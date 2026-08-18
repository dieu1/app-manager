package com.vandieu_manhdung.taskmanager.ui.team.task;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.core.constant.TaskPriority;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TeamRules;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.TeamWorkspaceSnapshot;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeamTaskFormFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "workspace_id";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_TASK_ID = "task_id";
    private static final String STATE_DUE_DATE = "due_date";

    private String workspaceId;
    private String userId;
    private String taskId;
    private long dueDate;
    private TeamTaskFormViewModel viewModel;
    private TeamWorkspaceSnapshot snapshot;
    private TeamTaskItem editingItem;
    private List<Project> projects = List.of();
    private List<WorkspaceMember> members = List.of();
    private boolean populated;

    private EditText title;
    private EditText description;
    private EditText estimatedMinutes;
    private Spinner projectSpinner;
    private Spinner assigneeSpinner;
    private Spinner statusSpinner;
    private Spinner prioritySpinner;
    private SeekBar progressSeek;
    private TextView progressText;
    private TextView dueDateText;
    private View saveButton;
    private View deleteButton;
    private ProgressBar loadingView;

    public static TeamTaskFormFragment newInstance(
            String workspaceId,
            String userId,
            String taskId
    ) {
        TeamTaskFormFragment fragment = new TeamTaskFormFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_WORKSPACE_ID, workspaceId);
        arguments.putString(ARG_USER_ID, userId);
        arguments.putString(ARG_TASK_ID, taskId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_team_task_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        workspaceId = requireArguments().getString(ARG_WORKSPACE_ID);
        userId = requireArguments().getString(ARG_USER_ID);
        taskId = requireArguments().getString(ARG_TASK_ID);
        if (savedInstanceState != null) {
            dueDate = savedInstanceState.getLong(STATE_DUE_DATE);
        }
        bind(view);
        setupStaticSpinners();
        setupProgressRules();

        view.<TextView>findViewById(R.id.textTeamTaskFormTitle).setText(
                taskId == null ? R.string.add_team_task : R.string.edit_team_task);
        deleteButton.setVisibility(taskId == null ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.buttonCancelTeamTask).setOnClickListener(
                button -> getParentFragmentManager().popBackStack());
        view.findViewById(R.id.buttonTeamSelectDueDate).setOnClickListener(
                button -> showDatePicker());
        view.findViewById(R.id.buttonTeamClearDueDate).setOnClickListener(button -> {
            dueDate = 0;
            displayDueDate();
        });
        saveButton.setOnClickListener(button -> save());
        deleteButton.setOnClickListener(button -> confirmDelete());
        displayDueDate();

        viewModel = new ViewModelProvider(this).get(TeamTaskFormViewModel.class);
        observe();
        viewModel.initialize(workspaceId, userId, taskId);
    }

    private void bind(View view) {
        title = view.findViewById(R.id.editTeamTaskTitle);
        description = view.findViewById(R.id.editTeamTaskDescription);
        estimatedMinutes = view.findViewById(R.id.editTeamEstimatedMinutes);
        projectSpinner = view.findViewById(R.id.spinnerTeamTaskProject);
        assigneeSpinner = view.findViewById(R.id.spinnerTeamTaskAssignee);
        statusSpinner = view.findViewById(R.id.spinnerTeamTaskStatus);
        prioritySpinner = view.findViewById(R.id.spinnerTeamTaskPriority);
        progressSeek = view.findViewById(R.id.seekTeamTaskProgress);
        progressText = view.findViewById(R.id.textTeamTaskProgress);
        dueDateText = view.findViewById(R.id.textTeamSelectedDueDate);
        saveButton = view.findViewById(R.id.buttonSaveTeamTask);
        deleteButton = view.findViewById(R.id.buttonDeleteTeamTask);
        loadingView = view.findViewById(R.id.progressTeamTaskForm);
    }

    private void setupStaticSpinners() {
        setSpinner(statusSpinner, List.of(
                getString(R.string.status_todo),
                getString(R.string.status_in_progress),
                getString(R.string.status_completed),
                getString(R.string.status_cancelled)));
        setSpinner(prioritySpinner, List.of(
                getString(R.string.priority_low),
                getString(R.string.priority_medium),
                getString(R.string.priority_high),
                getString(R.string.priority_urgent)));
        prioritySpinner.setSelection(1);
    }

    private void setupProgressRules() {
        progressSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressText.setText(getString(R.string.progress_value, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    progressSeek.setProgress(0);
                    progressSeek.setEnabled(false);
                } else if (position == 2) {
                    progressSeek.setProgress(100);
                    progressSeek.setEnabled(false);
                } else {
                    progressSeek.setEnabled(true);
                    if (position == 1 && progressSeek.getProgress() == 0) {
                        progressSeek.setProgress(1);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void observe() {
        viewModel.getSnapshot().observe(getViewLifecycleOwner(), value -> {
            snapshot = value;
            projects = value.getProjects();
            members = value.getMembers();
            List<String> projectNames = new ArrayList<>();
            for (Project project : projects) projectNames.add(project.getName());
            List<String> memberNames = new ArrayList<>();
            for (WorkspaceMember member : members) memberNames.add(member.getDisplayName());
            setSpinner(projectSpinner, projectNames);
            setSpinner(assigneeSpinner, memberNames);
            tryPopulate();
        });
        viewModel.getEditingItem().observe(getViewLifecycleOwner(), item -> {
            editingItem = item;
            tryPopulate();
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean value = Boolean.TRUE.equals(loading);
            loadingView.setVisibility(value ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!value);
        });
        viewModel.getSaved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved)) {
                Toast.makeText(requireContext(), R.string.team_task_saved, Toast.LENGTH_SHORT).show();
                viewModel.clearResult();
                getParentFragmentManager().popBackStack();
            }
        });
        viewModel.getDeleted().observe(getViewLifecycleOwner(), deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                Toast.makeText(requireContext(), R.string.team_task_deleted, Toast.LENGTH_SHORT).show();
                viewModel.clearResult();
                getParentFragmentManager().popBackStack();
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    private void tryPopulate() {
        if (populated || snapshot == null || (taskId != null && editingItem == null)) {
            return;
        }
        if (taskId == null) {
            int currentMember = memberPosition(userId);
            if (currentMember >= 0) assigneeSpinner.setSelection(currentMember);
            populated = true;
            return;
        }
        title.setText(editingItem.getTask().getTitle());
        description.setText(editingItem.getTask().getDescription());
        estimatedMinutes.setText(String.valueOf(editingItem.getTask().getEstimatedMinutes()));
        projectSpinner.setSelection(projectPosition(editingItem.getTask().getProjectId()));
        assigneeSpinner.setSelection(memberPosition(editingItem.getAssigneeId()));
        statusSpinner.setSelection(statusPosition(editingItem.getTask().getStatus()));
        prioritySpinner.setSelection(priorityPosition(editingItem.getTask().getPriority()));
        progressSeek.setProgress(editingItem.getTask().getProgress());
        dueDate = editingItem.getTask().getDueDate();
        displayDueDate();

        boolean canEdit = TeamRules.canEditTask(
                snapshot.getCurrentRole(), userId, editingItem.getTask(), editingItem.getAssigneeId());
        boolean canDelete = TeamRules.canDeleteTask(
                snapshot.getCurrentRole(), userId, editingItem.getTask());
        saveButton.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        setFieldsEnabled(canEdit);
        populated = true;
    }

    private void setFieldsEnabled(boolean enabled) {
        title.setEnabled(enabled);
        description.setEnabled(enabled);
        estimatedMinutes.setEnabled(enabled);
        projectSpinner.setEnabled(enabled);
        assigneeSpinner.setEnabled(enabled);
        statusSpinner.setEnabled(enabled);
        prioritySpinner.setEnabled(enabled);
        progressSeek.setEnabled(enabled && statusSpinner.getSelectedItemPosition() != 0 &&
                statusSpinner.getSelectedItemPosition() != 2);
        requireView().findViewById(R.id.buttonTeamSelectDueDate).setEnabled(enabled);
        requireView().findViewById(R.id.buttonTeamClearDueDate).setEnabled(enabled);
    }

    private void save() {
        String cleanTitle = title.getText().toString().trim();
        if (cleanTitle.isEmpty()) {
            title.setError(getString(R.string.task_title_required));
            return;
        }
        if (projects.isEmpty() || members.isEmpty()) {
            Toast.makeText(requireContext(), R.string.team_task_missing_context,
                    Toast.LENGTH_LONG).show();
            return;
        }
        int minutes = 0;
        String minutesText = estimatedMinutes.getText().toString().trim();
        if (!minutesText.isEmpty()) {
            try {
                minutes = Integer.parseInt(minutesText);
            } catch (NumberFormatException exception) {
                estimatedMinutes.setError(getString(R.string.invalid_estimated_minutes));
                return;
            }
        }
        viewModel.save(
                cleanTitle,
                description.getText().toString(),
                statusValue(),
                priorityValue(),
                progressSeek.getProgress(),
                dueDate,
                minutes,
                projects.get(projectSpinner.getSelectedItemPosition()).getProjectId(),
                members.get(assigneeSpinner.getSelectedItemPosition()).getUserId());
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_task_title)
                .setMessage(R.string.delete_task_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.delete())
                .show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (dueDate > 0) calendar.setTimeInMillis(dueDate);
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (picker, year, month, day) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day, 23, 59, 59);
                    selected.set(Calendar.MILLISECOND, 999);
                    dueDate = selected.getTimeInMillis();
                    displayDueDate();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void displayDueDate() {
        dueDateText.setText(dueDate <= 0
                ? getString(R.string.no_due_date_selected)
                : getString(R.string.selected_due_date,
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(new Date(dueDate))));
    }

    private void setSpinner(Spinner spinner, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private int projectPosition(String projectId) {
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getProjectId().equals(projectId)) return i;
        }
        return 0;
    }

    private int memberPosition(String memberId) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getUserId().equals(memberId)) return i;
        }
        return 0;
    }

    private int statusPosition(String status) {
        if (TaskStatus.IN_PROGRESS.equals(status)) return 1;
        if (TaskStatus.COMPLETED.equals(status)) return 2;
        if (TaskStatus.CANCELLED.equals(status)) return 3;
        return 0;
    }

    private int priorityPosition(String priority) {
        if (TaskPriority.HIGH.equals(priority)) return 2;
        if (TaskPriority.URGENT.equals(priority)) return 3;
        if (TaskPriority.MEDIUM.equals(priority)) return 1;
        return 0;
    }

    private String statusValue() {
        return switch (statusSpinner.getSelectedItemPosition()) {
            case 1 -> TaskStatus.IN_PROGRESS;
            case 2 -> TaskStatus.COMPLETED;
            case 3 -> TaskStatus.CANCELLED;
            default -> TaskStatus.TODO;
        };
    }

    private String priorityValue() {
        return switch (prioritySpinner.getSelectedItemPosition()) {
            case 0 -> TaskPriority.LOW;
            case 2 -> TaskPriority.HIGH;
            case 3 -> TaskPriority.URGENT;
            default -> TaskPriority.MEDIUM;
        };
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(STATE_DUE_DATE, dueDate);
        super.onSaveInstanceState(outState);
    }
}
