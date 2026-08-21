package com.vandieu_manhdung.taskmanager.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
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
import com.vandieu_manhdung.taskmanager.data.reponsitory.AuthRepository;
import com.vandieu_manhdung.taskmanager.data.reponsitory.WorkspaceRepository;
import com.vandieu_manhdung.taskmanager.data.remote.CloudSyncManager;
import com.vandieu_manhdung.taskmanager.model.User;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.ui.personal.task.PersonalTaskListFragment;
import com.vandieu_manhdung.taskmanager.ui.auth.AuthFragment;
import com.vandieu_manhdung.taskmanager.ui.auth.BackendSetupFragment;
import com.vandieu_manhdung.taskmanager.ui.auth.ForgotPasswordFragment;
import com.vandieu_manhdung.taskmanager.ui.auth.RegisterFragment;
import com.vandieu_manhdung.taskmanager.ui.home.HomeFragment;
import com.vandieu_manhdung.taskmanager.ui.profile.ProfileFragment;
import com.vandieu_manhdung.taskmanager.ui.team.TeamListFragment;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATIONS = 2001;

    private AuthRepository authRepository;
    private User currentUser;
    private Workspace personalWorkspace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        TaskNotificationManager.createChannel(this);
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
        requestNotificationPermissionIfNeeded();
        initializeApplication(user);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS
            );
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
                currentUser.getDisplayName(),
                currentUser.getEmail(),
                currentUser.getUserCode()
        ));
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
                                openHome();
                            }

                            @Override
                            public void onError(Exception exception) {
                                Log.e("MAIN_ACTIVITY", "Không thể khởi động đồng bộ", exception);
                                Toast.makeText(
                                        MainActivity.this,
                                        "Không thể đồng bộ dữ liệu: " + exception.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                                openHome();
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
