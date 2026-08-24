package com.vandieu_manhdung.taskmanager.ui.main;

import android.Manifest;
import android.app.AlarmManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.Log;
import android.widget.Toast;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.notification.TaskNotificationManager;
import com.vandieu_manhdung.taskmanager.core.notification.TaskReminderScheduler;
import com.vandieu_manhdung.taskmanager.data.repository.AuthRepository;
import com.vandieu_manhdung.taskmanager.data.repository.WorkspaceRepository;
import com.vandieu_manhdung.taskmanager.data.remote.CloudSyncManager;
import com.vandieu_manhdung.taskmanager.model.User;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.ui.personal.task.PersonalTaskListFragment;
import com.vandieu_manhdung.taskmanager.ui.personal.task.detail.TaskDetailFragment;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.ui.auth.AuthFragment;
import com.vandieu_manhdung.taskmanager.ui.auth.BackendSetupFragment;
import com.vandieu_manhdung.taskmanager.ui.auth.ForgotPasswordFragment;
import com.vandieu_manhdung.taskmanager.ui.auth.RegisterFragment;
import com.vandieu_manhdung.taskmanager.ui.home.HomeFragment;
import com.vandieu_manhdung.taskmanager.ui.profile.ProfileFragment;
import com.vandieu_manhdung.taskmanager.ui.notification.NotificationFragment;
import com.vandieu_manhdung.taskmanager.ui.personal.trash.TaskTrashFragment;
import com.vandieu_manhdung.taskmanager.ui.team.TeamListFragment;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATIONS = 2001;
    public static final String EXTRA_OPEN_TASK_ID = "open_task_id";

    private AuthRepository authRepository;
    private User currentUser;
    private Workspace personalWorkspace;
    private boolean exactAlarmPromptShown;
    private boolean hadExactAlarmAccess;
    private String pendingTaskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        pendingTaskId = getIntent().getStringExtra(EXTRA_OPEN_TASK_ID);
        TaskNotificationManager.createChannel(this);
        hadExactAlarmAccess = canScheduleExactAlarms();
        authRepository = new AuthRepository(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        startAuthenticationFlow();
    }

    private void startAuthenticationFlow() {
        if (!authRepository.isConfigured()) {
            showRootFragment(new BackendSetupFragment());
            return;
        }
        if (!authRepository.isSignedIn()) {
            showRootFragment(new AuthFragment());
            return;
        }
        authRepository.getCurrentUser(new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User user) {
                onAuthenticated(user);
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(MainActivity.this,
                        exception.getMessage(), Toast.LENGTH_LONG).show();
                showRootFragment(new AuthFragment());
            }
        });
    }

    public void onAuthenticated(User user) {
        if (!requestNotificationPermissionIfNeeded()) {
            requestExactAlarmAccessIfNeeded();
        }
        initializeApplication(user);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingTaskId = intent.getStringExtra(EXTRA_OPEN_TASK_ID);
        if (currentUser != null && personalWorkspace != null) {
            openPendingTaskOrHome();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean hasExactAlarmAccess = canScheduleExactAlarms();
        if (hasExactAlarmAccess && !hadExactAlarmAccess) {
            new Thread(() -> new TaskReminderScheduler(this).rescheduleAll()).start();
        }
        hadExactAlarmAccess = hasExactAlarmAccess;
    }

    private boolean requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS
            );
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            requestExactAlarmAccessIfNeeded();
        }
    }

    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        return manager != null && manager.canScheduleExactAlarms();
    }

    private void requestExactAlarmAccessIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                canScheduleExactAlarms() || exactAlarmPromptShown) {
            return;
        }
        exactAlarmPromptShown = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.exact_alarm_permission_title)
                .setMessage(R.string.exact_alarm_permission_message)
                .setPositiveButton(
                        R.string.open_settings,
                        (dialog, which) -> openExactAlarmSettings()
                )
                .setNegativeButton(R.string.later, null)
                .show();
    }

    private void openExactAlarmSettings() {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + getPackageName())));
        }
    }

    public void signOut() {
        CloudSyncManager.getInstance(this).stop();
        authRepository.signOut();
        currentUser = null;
        personalWorkspace = null;
        showRootFragment(new AuthFragment());
    }

    public void retryFirebaseSetup() {
        startAuthenticationFlow();
    }

    public void showLogin() {
        showRootFragment(new AuthFragment());
    }

    public void showRegister() {
        navigate(new RegisterFragment());
    }

    public void showForgotPassword() {
        navigate(new ForgotPasswordFragment());
    }

    public void openHome() {
        if (currentUser == null || personalWorkspace == null) return;
        showRootFragment(HomeFragment.newInstance(
                currentUser.getDisplayName(),
                currentUser.getUserCode()
        ));
    }

    public void openPersonalTasks() {
        if (currentUser == null || personalWorkspace == null) return;
        navigate(PersonalTaskListFragment.newInstance(
                personalWorkspace.getWorkspaceId(),
                currentUser.getUserId()
        ));
    }

    public void openTeams() {
        if (currentUser == null) return;
        navigate(TeamListFragment.newInstance(currentUser.getUserId()));
    }

    public void openProfile() {
        if (currentUser == null) return;
        navigate(ProfileFragment.newInstance(
                currentUser.getUserId(),
                currentUser.getDisplayName(),
                currentUser.getEmail(),
                currentUser.getUserCode(),
                currentUser.getAvatarUrl()
            )
        );
    }

    public void openNotifications() {
        if (currentUser == null) return;
        navigate(NotificationFragment.newInstance(currentUser.getUserId()));
    }

    public void openTaskTrash() {
        if (personalWorkspace == null) return;
        navigate(TaskTrashFragment.newInstance(personalWorkspace.getWorkspaceId()));
    }

    public void openTaskDetail(String taskId) {
        if (currentUser == null || personalWorkspace == null || taskId == null) return;
        navigate(taskDetailFragment(taskId));
    }

    public void onProfileUpdated(User user) {
        currentUser = user;
    }

    private void openPendingTaskOrHome() {
        if (pendingTaskId == null || pendingTaskId.isBlank()) {
            openHome();
            return;
        }
        String taskId = pendingTaskId;
        pendingTaskId = null;
        showRootFragment(taskDetailFragment(taskId));
    }

    private TaskDetailFragment taskDetailFragment(String taskId) {
        Task task = new TaskDao(this).findByIdIncludingDeleted(taskId);
        if (task != null && task.getProjectId() != null && !task.getProjectId().isBlank()) {
            return TaskDetailFragment.newTeamInstance(
                    task.getWorkspaceId(), currentUser.getUserId(), taskId);
        }
        return TaskDetailFragment.newInstance(
                personalWorkspace.getWorkspaceId(), currentUser.getUserId(), taskId);
    }

    private void showRootFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().popBackStackImmediate(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        );
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, fragment)
                .commit();
    }

    private void navigate(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void initializeApplication(User user) {
        new Thread(() -> {
            try {
                WorkspaceRepository repository =
                        new WorkspaceRepository(this);

                Workspace workspace =
                        repository
                                .initializePersonalWorkspace(user);

                currentUser = user;
                personalWorkspace = workspace;
                new TaskReminderScheduler(this).rescheduleAll();

                runOnUiThread(() -> CloudSyncManager.getInstance(this).start(
                        user.getUserId(),
                        new RepositoryCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean ignored) {
                                openPendingTaskOrHome();
                            }

                            @Override
                            public void onError(Exception exception) {
                                Log.e("MAIN_ACTIVITY", "Không thể khởi động đồng bộ", exception);
                                Toast.makeText(
                                        MainActivity.this,
                                        "Không thể đồng bộ dữ liệu: " + exception.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                                openPendingTaskOrHome();
                            }
                        }
                ));

            } catch (Exception exception) {
                Log.e(
                        "MAIN_ACTIVITY",
                        "Không thể mở danh sách công việc",
                        exception
                );
                runOnUiThread(() -> Toast.makeText(
                        this,
                        R.string.personal_workspace_load_error,
                        Toast.LENGTH_LONG
                ).show());
            }
        }).start();
    }
}
