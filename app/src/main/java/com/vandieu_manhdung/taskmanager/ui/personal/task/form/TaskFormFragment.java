package com.vandieu_manhdung.taskmanager.ui.personal.task.form;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.core.constant.TaskPriority;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskScheduleRules;
import com.vandieu_manhdung.taskmanager.ui.personal.task.detail.TaskDetailFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskFormFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "workspace_id";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_TASK_ID = "task_id";
    private static final String STATE_START_DATE = "selected_start_date";
    private static final String STATE_DUE_DATE = "selected_due_date";

    private String workspaceId;
    private String userId;
    private String taskId;

    private long selectedStartDate;
    private long selectedDueDate;

    private EditText editTitle;
    private EditText editDescription;
    private Spinner spinnerStatus;
    private Spinner spinnerPriority;
    private TextView textStartDate;
    private TextView textDueDate;
    private TextView textDuration;
    private TextView textFormTitle;
    private Button buttonSave;
    private ProgressBar progressSaving;

    private TaskFormViewModel viewModel;
    private boolean shouldPopulateForm;

    public static TaskFormFragment newInstance(
            String workspaceId,
            String userId
    ) {
        TaskFormFragment fragment =
                new TaskFormFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_WORKSPACE_ID, workspaceId);
        arguments.putString(ARG_USER_ID, userId);

        fragment.setArguments(arguments);
        return fragment;
    }

    public static TaskFormFragment newEditInstance(
            String workspaceId,
            String userId,
            String taskId
    ) {
        TaskFormFragment fragment = newInstance(workspaceId, userId);
        requireArguments(fragment).putString(ARG_TASK_ID, taskId);
        return fragment;
    }

    private static Bundle requireArguments(TaskFormFragment fragment) {
        Bundle arguments = fragment.getArguments();
        if (arguments == null) {
            arguments = new Bundle();
            fragment.setArguments(arguments);
        }
        return arguments;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_task_form,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        workspaceId = requireArguments().getString(
                ARG_WORKSPACE_ID
        );

        userId = requireArguments().getString(
                ARG_USER_ID
        );

        taskId = requireArguments().getString(ARG_TASK_ID);

        if (savedInstanceState != null) {
            selectedStartDate = savedInstanceState.getLong(
                    STATE_START_DATE,
                    0
            );
            selectedDueDate = savedInstanceState.getLong(
                    STATE_DUE_DATE,
                    0
            );
        }

        shouldPopulateForm = savedInstanceState == null;

        bindViews(view);
        setupSpinners();
        setupViewModel();

        boolean editing = taskId != null && !taskId.isBlank();
        textFormTitle.setText(editing
                ? R.string.edit_task
                : R.string.add_task);
        buttonSave.setText(editing
                ? R.string.save_changes
                : R.string.save_task_and_add_steps);

        if (!editing && savedInstanceState == null) {
            initializeDefaultSchedule();
        }
        displaySchedule();

        view.findViewById(R.id.buttonSelectStartDateTime)
                .setOnClickListener(
                        button -> openDateTimePicker(true)
                );

        view.findViewById(R.id.buttonSelectDueDateTime)
                .setOnClickListener(
                        button -> openDateTimePicker(false)
                );

        view.findViewById(R.id.buttonCancelTask)
                .setOnClickListener(
                        button -> getParentFragmentManager()
                                .popBackStack()
                );

        view.findViewById(R.id.buttonCancelTaskTop)
                .setOnClickListener(
                        button -> getParentFragmentManager()
                                .popBackStack()
                );

        buttonSave.setOnClickListener(
                button -> saveTask()
        );
    }

    private void bindViews(View view) {
        editTitle = view.findViewById(
                R.id.editTaskTitle
        );

        editDescription = view.findViewById(
                R.id.editTaskDescription
        );

        spinnerStatus = view.findViewById(
                R.id.spinnerTaskStatus
        );

        spinnerPriority = view.findViewById(
                R.id.spinnerTaskPriority
        );

        textStartDate = view.findViewById(R.id.textSelectedStartDateTime);
        textDueDate = view.findViewById(R.id.textSelectedDueDateTime);
        textDuration = view.findViewById(R.id.textCalculatedDuration);

        buttonSave = view.findViewById(
                R.id.buttonSaveTask
        );

        progressSaving = view.findViewById(
                R.id.progressSavingTask
        );

        textFormTitle = view.findViewById(
                R.id.textTaskFormTitle
        );
    }

    private void setupSpinners() {
        String[] statuses = {
                "Chưa xử lý",
                "Đang xử lý",
                "Đã xử lý",
                "Không xử lý"
        };

        String[] priorities = {
                "Thấp",
                "Vừa",
                "Cao",
                "Khẩn cấp"
        };

        ArrayAdapter<String> statusAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        statuses
                );

        statusAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerStatus.setAdapter(statusAdapter);

        ArrayAdapter<String> priorityAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        priorities
                );

        priorityAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerPriority.setAdapter(priorityAdapter);

        // Mặc định độ ưu tiên vừa.
        spinnerPriority.setSelection(1);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(TaskFormViewModel.class);

        viewModel.getEditingTask().observe(
                getViewLifecycleOwner(),
                task -> {
                    if (task == null || !shouldPopulateForm) {
                        return;
                    }
                    populateForm(task);
                    shouldPopulateForm = false;
                }
        );

        viewModel.getSaving().observe(
                getViewLifecycleOwner(),
                saving -> {
                    boolean isSaving =
                            Boolean.TRUE.equals(saving);

                    progressSaving.setVisibility(
                            isSaving
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    buttonSave.setEnabled(!isSaving);
                }
        );

        viewModel.getSavedTask().observe(
                getViewLifecycleOwner(),
                task -> {
                    if (task == null) {
                        return;
                    }

                    Toast.makeText(
                            requireContext(),
                            taskId == null
                                    ? R.string.task_created_successfully
                                    : R.string.task_updated_successfully,
                            Toast.LENGTH_SHORT
                    ).show();

                    viewModel.clearSavedTask();

                    if (taskId == null || taskId.isBlank()) {
                        getParentFragmentManager().popBackStackImmediate();
                        getParentFragmentManager()
                                .beginTransaction()
                                .replace(
                                        R.id.main,
                                        TaskDetailFragment.newInstance(
                                                workspaceId,
                                                userId,
                                                task.getTaskId()
                                        )
                                )
                                .addToBackStack("task_detail")
                                .commit();
                    } else {
                        getParentFragmentManager().popBackStack();
                    }
                }
        );

        viewModel.getError().observe(
                getViewLifecycleOwner(),
                message -> {
                    if (message == null) {
                        return;
                    }

                    Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_LONG
                    ).show();

                    viewModel.clearError();
                }
        );

        if (taskId != null && !taskId.isBlank() && shouldPopulateForm) {
            viewModel.loadTask(taskId);
        }
    }

    private void populateForm(com.vandieu_manhdung.taskmanager.model.Task task) {
        editTitle.setText(task.getTitle());
        editDescription.setText(task.getDescription());
        spinnerStatus.setSelection(statusPosition(task.getStatus()));
        spinnerPriority.setSelection(priorityPosition(task.getPriority()));
        selectedStartDate = task.getStartDate();
        selectedDueDate = task.getDueDate();
        displaySchedule();
    }

    private int statusPosition(String status) {
        if (TaskStatus.IN_PROGRESS.equals(status)) {
            return 1;
        }
        if (TaskStatus.COMPLETED.equals(status)) {
            return 2;
        }
        if (TaskStatus.CANCELLED.equals(status)) {
            return 3;
        }
        return 0;
    }

    private int priorityPosition(String priority) {
        if (TaskPriority.LOW.equals(priority)) {
            return 0;
        }
        if (TaskPriority.HIGH.equals(priority)) {
            return 2;
        }
        if (TaskPriority.URGENT.equals(priority)) {
            return 3;
        }
        return 1;
    }

    private void initializeDefaultSchedule() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 15);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        selectedStartDate = calendar.getTimeInMillis();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        selectedDueDate = calendar.getTimeInMillis();
    }

    private void openDateTimePicker(boolean selectingStart) {
        long currentValue = selectingStart ? selectedStartDate : selectedDueDate;
        Calendar calendar = Calendar.getInstance();
        if (currentValue > 0) {
            calendar.setTimeInMillis(currentValue);
        }

        DatePickerDialog dialog =
                new DatePickerDialog(
                        requireContext(),
                        (datePicker, year, month, day) -> {
                            new TimePickerDialog(
                                    requireContext(),
                                    (timePicker, hour, minute) -> {
                                        Calendar selected = Calendar.getInstance();
                                        selected.set(year, month, day, hour, minute, 0);
                                        selected.set(Calendar.MILLISECOND, 0);
                                        if (selectingStart) {
                                            selectedStartDate = selected.getTimeInMillis();
                                            if (selectedDueDate <= selectedStartDate) {
                                                selectedDueDate = selectedStartDate + 60 * 60 * 1000L;
                                            }
                                        } else {
                                            selectedDueDate = selected.getTimeInMillis();
                                        }
                                        displaySchedule();
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                            ).show();
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        dialog.show();
    }

    private void displaySchedule() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
        );
        textStartDate.setText(selectedStartDate <= 0
                ? getString(R.string.no_start_datetime_selected)
                : getString(R.string.selected_start_datetime,
                formatter.format(new Date(selectedStartDate))));
        textDueDate.setText(selectedDueDate <= 0
                ? getString(R.string.no_due_date_selected)
                : getString(R.string.selected_due_datetime,
                formatter.format(new Date(selectedDueDate))));
        int minutes = TaskScheduleRules.calculateEstimatedMinutes(
                selectedStartDate,
                selectedDueDate
        );
        textDuration.setText(minutes <= 0
                ? getString(R.string.calculated_duration_empty)
                : getString(R.string.calculated_duration, formatDuration(minutes)));
    }

    private String formatDuration(int totalMinutes) {
        int days = totalMinutes / (24 * 60);
        int hours = (totalMinutes % (24 * 60)) / 60;
        int minutes = totalMinutes % 60;
        StringBuilder value = new StringBuilder();
        if (days > 0) value.append(days).append(" ngày ");
        if (hours > 0) value.append(hours).append(" giờ ");
        if (minutes > 0 || value.length() == 0) value.append(minutes).append(" phút");
        return value.toString().trim();
    }

    private void saveTask() {
        String title =
                editTitle.getText().toString().trim();

        String description =
                editDescription.getText()
                        .toString()
                        .trim();

        if (title.isEmpty()) {
            editTitle.setError(
                    "Vui lòng nhập tên công việc"
            );

            editTitle.requestFocus();
            return;
        }

        if (selectedStartDate <= 0 || selectedDueDate <= selectedStartDate) {
            Toast.makeText(
                    requireContext(),
                    R.string.invalid_task_schedule,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String status = switch (
                spinnerStatus.getSelectedItemPosition()
                ) {
            case 1 -> TaskStatus.IN_PROGRESS;
            case 2 -> TaskStatus.COMPLETED;
            case 3 -> TaskStatus.CANCELLED;
            default -> TaskStatus.TODO;
        };

        String priority = switch (
                spinnerPriority.getSelectedItemPosition()
                ) {
            case 0 -> TaskPriority.LOW;
            case 2 -> TaskPriority.HIGH;
            case 3 -> TaskPriority.URGENT;
            default -> TaskPriority.MEDIUM;
        };

        viewModel.savePersonalTask(
                taskId,
                workspaceId,
                userId,
                title,
                description,
                status,
                priority,
                selectedStartDate,
                selectedDueDate
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(STATE_START_DATE, selectedStartDate);
        outState.putLong(STATE_DUE_DATE, selectedDueDate);
        super.onSaveInstanceState(outState);
    }
}
