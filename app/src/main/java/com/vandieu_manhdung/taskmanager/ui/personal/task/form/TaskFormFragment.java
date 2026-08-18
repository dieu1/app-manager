package com.vandieu_manhdung.taskmanager.ui.personal.task.form;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskFormFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "workspace_id";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_TASK_ID = "task_id";
    private static final String STATE_DUE_DATE = "selected_due_date";

    private String workspaceId;
    private String userId;
    private String taskId;

    private long selectedDueDate;

    private EditText editTitle;
    private EditText editDescription;
    private EditText editEstimatedMinutes;
    private Spinner spinnerStatus;
    private Spinner spinnerPriority;
    private SeekBar seekProgress;
    private TextView textProgress;
    private TextView textDueDate;
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
            selectedDueDate = savedInstanceState.getLong(
                    STATE_DUE_DATE,
                    0
            );
        }

        shouldPopulateForm = savedInstanceState == null;

        bindViews(view);
        setupSpinners();
        setupProgress();
        setupViewModel();

        boolean editing = taskId != null && !taskId.isBlank();
        textFormTitle.setText(editing
                ? R.string.edit_task
                : R.string.add_task);
        buttonSave.setText(editing
                ? R.string.save_changes
                : R.string.save_task);

        if (selectedDueDate > 0) {
            displayDueDate();
        }

        view.findViewById(R.id.buttonSelectDueDate)
                .setOnClickListener(
                        button -> openDatePicker()
                );

        view.findViewById(R.id.buttonClearDueDate)
                .setOnClickListener(button -> {
                    selectedDueDate = 0;
                    textDueDate.setText(R.string.no_due_date_selected);
                });

        view.findViewById(R.id.buttonCancelTask)
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

        editEstimatedMinutes = view.findViewById(
                R.id.editEstimatedMinutes
        );

        spinnerStatus = view.findViewById(
                R.id.spinnerTaskStatus
        );

        spinnerPriority = view.findViewById(
                R.id.spinnerTaskPriority
        );

        seekProgress = view.findViewById(
                R.id.seekTaskProgress
        );

        textProgress = view.findViewById(
                R.id.textFormProgress
        );

        textDueDate = view.findViewById(
                R.id.textSelectedDueDate
        );

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

        spinnerStatus.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (position == 0) {
                            seekProgress.setProgress(0);
                            seekProgress.setEnabled(false);
                        } else if (position == 2) {
                            seekProgress.setProgress(100);
                            seekProgress.setEnabled(false);
                        } else {
                            seekProgress.setEnabled(true);
                            if (position == 1 && seekProgress.getProgress() == 0) {
                                seekProgress.setProgress(1);
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

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

    private void setupProgress() {
        seekProgress.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        textProgress.setText(
                                getString(
                                        R.string.progress_value,
                                        progress
                                )
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }
                }
        );
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

                    getParentFragmentManager()
                            .popBackStack();
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
        editEstimatedMinutes.setText(
                String.valueOf(task.getEstimatedMinutes())
        );
        spinnerStatus.setSelection(statusPosition(task.getStatus()));
        spinnerPriority.setSelection(priorityPosition(task.getPriority()));
        seekProgress.setProgress(task.getProgress());
        selectedDueDate = task.getDueDate();
        displayDueDate();
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

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        requireContext(),
                        (datePicker, year, month, day) -> {
                            Calendar selected =
                                    Calendar.getInstance();

                            selected.set(
                                    year,
                                    month,
                                    day,
                                    23,
                                    59,
                                    59
                            );

                            selected.set(
                                    Calendar.MILLISECOND,
                                    999
                            );

                            selectedDueDate =
                                    selected.getTimeInMillis();

                            displayDueDate();
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        dialog.getDatePicker().setMinDate(
                System.currentTimeMillis() - 1000
        );

        dialog.show();
    }

    private void displayDueDate() {
        if (selectedDueDate <= 0) {
            textDueDate.setText(R.string.no_due_date_selected);
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        );
        textDueDate.setText(getString(
                R.string.selected_due_date,
                formatter.format(new Date(selectedDueDate))
        ));
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

        int estimatedMinutes = 0;

        String estimatedValue =
                editEstimatedMinutes.getText()
                        .toString()
                        .trim();

        if (!estimatedValue.isEmpty()) {
            try {
                estimatedMinutes =
                        Integer.parseInt(estimatedValue);
            } catch (NumberFormatException exception) {
                editEstimatedMinutes.setError(
                        "Thời gian không hợp lệ"
                );

                return;
            }
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
                seekProgress.getProgress(),
                selectedDueDate,
                estimatedMinutes
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(STATE_DUE_DATE, selectedDueDate);
        super.onSaveInstanceState(outState);
    }
}
