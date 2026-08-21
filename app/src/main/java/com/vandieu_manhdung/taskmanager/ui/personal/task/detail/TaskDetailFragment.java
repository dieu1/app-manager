package com.vandieu_manhdung.taskmanager.ui.personal.task.detail;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.core.constant.TaskPriority;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;
import com.vandieu_manhdung.taskmanager.core.util.TaskSubtaskRules;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;
import com.vandieu_manhdung.taskmanager.ui.personal.task.form.TaskFormFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class TaskDetailFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "workspace_id";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_TASK_ID = "task_id";

    private String workspaceId;
    private String userId;
    private String taskId;
    private TaskDetailViewModel viewModel;

    private View content;
    private ProgressBar loading;
    private ProgressBar progress;
    private TextView title;
    private TextView description;
    private TextView status;
    private TextView priority;
    private TextView progressText;
    private TextView subtasksSummary;
    private TextView subtasksEmpty;
    private TextView startDate;
    private TextView dueDate;
    private TextView estimatedTime;
    private TextView updatedAt;
    private Button editButton;
    private Button deleteButton;
    private Button addSubtaskButton;
    private RecyclerView subtasksRecycler;
    private TaskSubtaskAdapter subtaskAdapter;
    private List<TaskSubtask> currentSubtasks = java.util.Collections.emptyList();
    private Task currentTask;

    public static TaskDetailFragment newInstance(
            String workspaceId,
            String userId,
            String taskId
    ) {
        TaskDetailFragment fragment = new TaskDetailFragment();
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
        return inflater.inflate(R.layout.fragment_task_detail, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        Bundle arguments = requireArguments();
        workspaceId = arguments.getString(ARG_WORKSPACE_ID);
        userId = arguments.getString(ARG_USER_ID);
        taskId = arguments.getString(ARG_TASK_ID);

        bindViews(view);
        setupViewModel();

        view.findViewById(R.id.buttonBackTaskDetail)
                .setOnClickListener(ignored ->
                        getParentFragmentManager().popBackStack());
        editButton.setOnClickListener(ignored -> openEditForm());
        deleteButton.setOnClickListener(ignored -> confirmDelete());
        addSubtaskButton.setOnClickListener(ignored -> showAddSubtaskDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && taskId != null) {
            viewModel.loadDetails(taskId, userId);
        }
    }

    private void bindViews(View view) {
        content = view.findViewById(R.id.layoutTaskDetailContent);
        loading = view.findViewById(R.id.progressLoadingTaskDetail);
        progress = view.findViewById(R.id.progressTaskDetail);
        title = view.findViewById(R.id.textTaskDetailTitle);
        description = view.findViewById(R.id.textTaskDetailDescription);
        status = view.findViewById(R.id.textTaskDetailStatus);
        priority = view.findViewById(R.id.textTaskDetailPriority);
        progressText = view.findViewById(R.id.textTaskDetailProgress);
        subtasksSummary = view.findViewById(R.id.textTaskDetailSubtasksSummary);
        subtasksEmpty = view.findViewById(R.id.textTaskSubtasksEmpty);
        startDate = view.findViewById(R.id.textTaskDetailStartDate);
        dueDate = view.findViewById(R.id.textTaskDetailDueDate);
        estimatedTime = view.findViewById(R.id.textTaskDetailEstimated);
        updatedAt = view.findViewById(R.id.textTaskDetailUpdatedAt);
        editButton = view.findViewById(R.id.buttonEditTask);
        deleteButton = view.findViewById(R.id.buttonDeleteTask);
        addSubtaskButton = view.findViewById(R.id.buttonAddSubtask);
        subtasksRecycler = view.findViewById(R.id.recyclerTaskSubtasks);
        subtaskAdapter = new TaskSubtaskAdapter(new TaskSubtaskAdapter.Listener() {
            @Override
            public void onToggle(TaskSubtask subtask, boolean completed) {
                viewModel.toggleSubtask(taskId, userId, subtask.getSubtaskId(), completed);
            }

            @Override
            public void onDelete(TaskSubtask subtask) {
                confirmDeleteSubtask(subtask);
            }
        });
        subtasksRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        subtasksRecycler.setAdapter(subtaskAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskDetailViewModel.class);

        viewModel.getTask().observe(getViewLifecycleOwner(), this::renderTask);
        viewModel.getSubtasks().observe(getViewLifecycleOwner(), this::renderSubtasks);
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean visible = Boolean.TRUE.equals(isLoading);
            loading.setVisibility(visible ? View.VISIBLE : View.GONE);
            content.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
        });
        viewModel.getDeleting().observe(getViewLifecycleOwner(), isDeleting -> {
            boolean disabled = Boolean.TRUE.equals(isDeleting);
            editButton.setEnabled(!disabled);
            deleteButton.setEnabled(!disabled);
        });
        viewModel.getDeleted().observe(getViewLifecycleOwner(), isDeleted -> {
            if (!Boolean.TRUE.equals(isDeleted)) {
                return;
            }
            Toast.makeText(
                    requireContext(),
                    R.string.task_deleted_successfully,
                    Toast.LENGTH_SHORT
            ).show();
            viewModel.clearDeleted();
            getParentFragmentManager().popBackStack();
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                return;
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            viewModel.clearError();
        });
    }

    private void renderTask(Task task) {
        if (task == null) {
            return;
        }

        currentTask = task;

        title.setText(task.getTitle());
        description.setText(task.getDescription() == null || task.getDescription().isBlank()
                ? getString(R.string.no_description)
                : task.getDescription());
        status.setText(getString(
                R.string.task_detail_status,
                formatStatus(task.getStatus())
        ));
        priority.setText(getString(
                R.string.task_detail_priority,
                formatPriority(task.getPriority())
        ));
        progress.setProgress(task.getProgress());
        progressText.setText(getString(
                R.string.task_detail_progress,
                task.getProgress()
        ));
        startDate.setText(getString(
                R.string.task_detail_start_date,
                formatDateTime(task.getStartDate())
        ));

        boolean overdue = TaskRules.isOverdue(task, System.currentTimeMillis());
        String dueDateValue = formatDateTime(task.getDueDate());
        dueDate.setText(overdue
                ? getString(R.string.task_detail_overdue, dueDateValue)
                : getString(R.string.task_detail_due_date, dueDateValue));
        dueDate.setTextColor(ContextCompat.getColor(
                requireContext(),
                overdue ? R.color.task_overdue : R.color.task_text_primary
        ));

        estimatedTime.setText(getString(
                R.string.task_detail_estimated,
                formatDuration(task.getEstimatedMinutes())
        ));
        updatedAt.setText(getString(
                R.string.task_detail_updated_at,
                formatDateTime(task.getUpdatedAt())
        ));
        renderSubtaskProgress();
    }

    private void openEditForm() {
        getParentFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.main,
                        TaskFormFragment.newEditInstance(
                                workspaceId,
                                userId,
                                taskId
                        )
                )
                .addToBackStack("task_edit")
                .commit();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_task_title)
                .setMessage(R.string.delete_task_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) -> viewModel.deleteTask(taskId)
                )
                .show();
    }

    private void renderSubtasks(List<TaskSubtask> value) {
        currentSubtasks = value == null ? java.util.Collections.emptyList() : value;
        subtaskAdapter.submitList(currentSubtasks);
        subtasksEmpty.setVisibility(currentSubtasks.isEmpty() ? View.VISIBLE : View.GONE);
        renderSubtaskProgress();
    }

    private void renderSubtaskProgress() {
        if (currentTask == null) {
            return;
        }
        if (currentSubtasks.isEmpty()) {
            progress.setProgress(currentTask.getProgress());
            progressText.setText(getString(
                    R.string.task_detail_progress,
                    currentTask.getProgress()
            ));
            subtasksSummary.setText(getString(
                    R.string.task_detail_manual_progress,
                    currentTask.getProgress()
            ));
            return;
        }
        int completed = TaskSubtaskRules.completedCount(currentSubtasks);
        int autoProgress = TaskSubtaskRules.calculateProgress(currentSubtasks);
        progress.setProgress(autoProgress);
        progressText.setText(getString(R.string.task_detail_auto_progress, autoProgress));
        subtasksSummary.setText(getString(
                R.string.task_detail_subtasks_summary,
                completed,
                currentSubtasks.size()
        ));
    }

    private void showAddSubtaskDialog() {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, 0, padding, 0);

        EditText titleInput = new EditText(requireContext());
        titleInput.setHint(R.string.subtask_title_hint);
        form.addView(titleInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        EditText estimateInput = new EditText(requireContext());
        estimateInput.setHint(R.string.subtask_estimated_hint);
        estimateInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(estimateInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.subtask_add_title)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String titleValue = titleInput.getText().toString().trim();
                    if (titleValue.isEmpty()) {
                        titleInput.setError(getString(R.string.task_title_required));
                        return;
                    }
                    int estimate = 0;
                    String estimateValue = estimateInput.getText().toString().trim();
                    if (!estimateValue.isEmpty()) {
                        try {
                            estimate = Integer.parseInt(estimateValue);
                        } catch (NumberFormatException exception) {
                            estimateInput.setError(getString(R.string.invalid_estimated_minutes));
                            return;
                        }
                    }
                    viewModel.createSubtask(taskId, userId, titleValue, estimate);
                    Toast.makeText(requireContext(), R.string.subtask_saved, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void confirmDeleteSubtask(TaskSubtask subtask) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.subtask_delete_title)
                .setMessage(R.string.subtask_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteSubtask(taskId, userId, subtask.getSubtaskId());
                    Toast.makeText(requireContext(), R.string.subtask_deleted, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String formatDateTime(long value) {
        if (value <= 0) {
            return "-";
        }
        return new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
        ).format(new Date(value));
    }

    private String formatDuration(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "-";
        }
        int days = totalMinutes / (24 * 60);
        int hours = (totalMinutes % (24 * 60)) / 60;
        int minutes = totalMinutes % 60;
        StringBuilder value = new StringBuilder();
        if (days > 0) value.append(days).append(" ngày ");
        if (hours > 0) value.append(hours).append(" giờ ");
        if (minutes > 0 || value.length() == 0) value.append(minutes).append(" phút");
        return value.toString().trim();
    }

    private String formatStatus(String value) {
        if (TaskStatus.TODO.equals(value)) {
            return getString(R.string.status_todo);
        }
        if (TaskStatus.IN_PROGRESS.equals(value)) {
            return getString(R.string.status_in_progress);
        }
        if (TaskStatus.COMPLETED.equals(value)) {
            return getString(R.string.status_completed);
        }
        if (TaskStatus.CANCELLED.equals(value)) {
            return getString(R.string.status_cancelled);
        }
        return value;
    }

    private String formatPriority(String value) {
        if (TaskPriority.LOW.equals(value)) {
            return getString(R.string.priority_low);
        }
        if (TaskPriority.MEDIUM.equals(value)) {
            return getString(R.string.priority_medium);
        }
        if (TaskPriority.HIGH.equals(value)) {
            return getString(R.string.priority_high);
        }
        if (TaskPriority.URGENT.equals(value)) {
            return getString(R.string.priority_urgent);
        }
        return value;
    }
}
