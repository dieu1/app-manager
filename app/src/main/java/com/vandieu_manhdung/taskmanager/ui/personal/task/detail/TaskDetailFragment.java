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
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.BuildConfig;
import com.vandieu_manhdung.taskmanager.core.constant.TaskPriority;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;
import com.vandieu_manhdung.taskmanager.core.util.TaskSubtaskRules;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;
import com.vandieu_manhdung.taskmanager.model.TaskHistory;
import com.vandieu_manhdung.taskmanager.model.TaskComment;
import com.vandieu_manhdung.taskmanager.model.TaskAttachment;
import com.vandieu_manhdung.taskmanager.model.TaskDependency;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;
import com.vandieu_manhdung.taskmanager.ui.personal.task.form.TaskFormFragment;
import com.vandieu_manhdung.taskmanager.ui.team.task.TeamTaskFormFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class TaskDetailFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "workspace_id";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_TEAM_MODE = "team_mode";

    private String workspaceId;
    private String userId;
    private String taskId;
    private boolean teamMode;
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
    private TextView historyText;
    private Button editButton;
    private Button deleteButton;
    private Button addSubtaskButton;
    private RecyclerView subtasksRecycler;
    private TaskSubtaskAdapter subtaskAdapter;
    private List<TaskSubtask> currentSubtasks = java.util.Collections.emptyList();
    private Task currentTask;
    private TextView commentsText;
    private TextView attachmentsText;
    private TextView dependenciesText;
    private List<TaskComment> currentComments = java.util.Collections.emptyList();
    private List<TaskDependency> currentDependencies = java.util.Collections.emptyList();
    private List<TeamTaskItem> dependencyCandidates = java.util.Collections.emptyList();
    private final ActivityResultLauncher<String[]> attachmentPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null || viewModel == null) return;
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) { }
                viewModel.addAttachment(taskId, userId, uri);
            });

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

    public static TaskDetailFragment newTeamInstance(
            String workspaceId, String userId, String taskId
    ) {
        TaskDetailFragment fragment = newInstance(workspaceId, userId, taskId);
        fragment.requireArguments().putBoolean(ARG_TEAM_MODE, true);
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
        teamMode = arguments.getBoolean(ARG_TEAM_MODE, false);

        bindViews(view);
        setupViewModel();

        view.findViewById(R.id.buttonBackTaskDetail)
                .setOnClickListener(ignored ->
                        getParentFragmentManager().popBackStack());
        View notificationButton = view.findViewById(R.id.buttonTaskDetailNotifications);
        notificationButton.setVisibility(teamMode ? View.VISIBLE : View.GONE);
        notificationButton.setOnClickListener(ignored ->
                ((MainActivity) requireActivity()).openNotifications());
        editButton.setOnClickListener(ignored -> openEditForm());
        deleteButton.setOnClickListener(ignored -> confirmDelete());
        addSubtaskButton.setOnClickListener(ignored -> showAddSubtaskDialog());
        view.findViewById(R.id.layoutTeamCollaboration)
                .setVisibility(teamMode ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.buttonAddTaskComment)
                .setOnClickListener(ignored -> showAddCommentDialog());
        View attachmentButton = view.findViewById(R.id.buttonAddTaskAttachment);
        attachmentButton.setVisibility(
                teamMode && BuildConfig.CLOUD_STORAGE_ENABLED ? View.VISIBLE : View.GONE);
        attachmentsText.setVisibility(
                teamMode && BuildConfig.CLOUD_STORAGE_ENABLED ? View.VISIBLE : View.GONE);
        attachmentButton.setOnClickListener(
                ignored -> attachmentPicker.launch(new String[]{"*/*"}));
        view.findViewById(R.id.buttonAddTaskDependency)
                .setOnClickListener(ignored -> showAddDependencyDialog());
        commentsText.setOnClickListener(ignored -> showManageCommentsDialog());
        dependenciesText.setOnClickListener(ignored -> showManageDependenciesDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && taskId != null) {
            viewModel.loadDetails(taskId, userId, teamMode);
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
        historyText = view.findViewById(R.id.textTaskHistory);
        editButton = view.findViewById(R.id.buttonEditTask);
        deleteButton = view.findViewById(R.id.buttonDeleteTask);
        addSubtaskButton = view.findViewById(R.id.buttonAddSubtask);
        subtasksRecycler = view.findViewById(R.id.recyclerTaskSubtasks);
        commentsText = view.findViewById(R.id.textTaskComments);
        attachmentsText = view.findViewById(R.id.textTaskAttachments);
        dependenciesText = view.findViewById(R.id.textTaskDependencies);
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
        viewModel.getHistory().observe(getViewLifecycleOwner(), this::renderHistory);
        viewModel.getComments().observe(getViewLifecycleOwner(), this::renderComments);
        viewModel.getAttachments().observe(getViewLifecycleOwner(), this::renderAttachments);
        viewModel.getDependencies().observe(getViewLifecycleOwner(), this::renderDependencies);
        viewModel.getDependencyCandidates().observe(getViewLifecycleOwner(), values ->
                dependencyCandidates = values == null
                        ? java.util.Collections.emptyList() : values);
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

    private void renderHistory(List<TaskHistory> values) {
        if (values == null || values.isEmpty()) {
            historyText.setText(R.string.task_history_empty);
            return;
        }
        StringBuilder text = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        int count = Math.min(values.size(), 20);
        for (int index = 0; index < count; index++) {
            TaskHistory item = values.get(index);
            if (index > 0) text.append("\n\n");
            text.append("• ").append(item.getDetail())
                    .append("\n  ").append(format.format(new Date(item.getCreatedAt())));
        }
        historyText.setText(text.toString());
    }

    private void renderComments(List<TaskComment> values) {
        if (commentsText == null) return;
        currentComments = values == null ? java.util.Collections.emptyList() : values;
        if (values == null || values.isEmpty()) {
            commentsText.setText(R.string.no_comments);
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        StringBuilder result = new StringBuilder();
        for (TaskComment item : values) {
            if (result.length() > 0) result.append("\n\n");
            String author = item.getUserDisplayName();
            result.append(author == null || author.isBlank() ? "Thành viên" : author)
                    .append(" · ").append(format.format(new Date(item.getCreatedAt())))
                    .append("\n").append(item.getMessage());
        }
        commentsText.setText(result.toString());
    }

    private void renderAttachments(List<TaskAttachment> values) {
        if (attachmentsText == null) return;
        if (values == null || values.isEmpty()) {
            attachmentsText.setText(R.string.no_attachments);
            return;
        }
        StringBuilder result = new StringBuilder();
        for (TaskAttachment item : values) {
            if (result.length() > 0) result.append("\n");
            result.append("• ").append(item.getDisplayName());
            if (item.getRemoteUrl() != null && !item.getRemoteUrl().isBlank()) {
                result.append("\n  ").append(item.getRemoteUrl());
            }
        }
        attachmentsText.setText(result.toString());
    }

    private void renderDependencies(List<TaskDependency> values) {
        if (dependenciesText == null) return;
        currentDependencies = values == null ? java.util.Collections.emptyList() : values;
        if (values == null || values.isEmpty()) {
            dependenciesText.setText(R.string.no_dependencies);
            return;
        }
        StringBuilder result = new StringBuilder();
        for (TaskDependency item : values) {
            if (result.length() > 0) result.append("\n");
            result.append("• ").append(item.getDependsOnTitle());
        }
        dependenciesText.setText(result.toString());
    }

    private void showAddCommentDialog() {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.comment_hint);
        input.setMinLines(3);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_comment)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, (dialog, which) ->
                        viewModel.addComment(taskId, userId, input.getText().toString()))
                .show();
    }

    private void showManageCommentsDialog() {
        if (currentComments.isEmpty()) return;
        String[] labels = new String[currentComments.size()];
        for (int index = 0; index < currentComments.size(); index++) {
            TaskComment item = currentComments.get(index);
            String author = item.getUserDisplayName();
            labels[index] = (author == null || author.isBlank() ? "Thành viên" : author) +
                    ": " + item.getMessage();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.manage_comments)
                .setItems(labels, (dialog, which) -> showCommentActions(currentComments.get(which)))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void showCommentActions(TaskComment item) {
        boolean ownComment = userId.equals(item.getUserId());
        String[] actions = ownComment
                ? new String[]{getString(R.string.edit_comment), getString(R.string.delete_comment)}
                : new String[]{getString(R.string.delete_comment)};
        new AlertDialog.Builder(requireContext())
                .setTitle(item.getMessage())
                .setItems(actions, (dialog, which) -> {
                    if (ownComment && which == 0) showEditCommentDialog(item);
                    else confirmDeleteComment(item);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditCommentDialog(TaskComment item) {
        EditText input = new EditText(requireContext());
        input.setText(item.getMessage());
        input.setSelection(input.length());
        input.setMinLines(3);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_comment)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> viewModel.editComment(
                        taskId, item.getCommentId(), userId, input.getText().toString()))
                .show();
    }

    private void confirmDeleteComment(TaskComment item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_comment)
                .setMessage(R.string.delete_comment_question)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.deleteComment(
                        taskId, item.getCommentId(), userId))
                .show();
    }

    private void showAddDependencyDialog() {
        if (dependencyCandidates.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_dependency_candidates,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[dependencyCandidates.size()];
        for (int index = 0; index < dependencyCandidates.size(); index++) {
            labels[index] = dependencyCandidates.get(index).getTask().getTitle();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_dependency)
                .setItems(labels, (dialog, which) -> viewModel.addDependency(
                        taskId,
                        dependencyCandidates.get(which).getTask().getTaskId(),
                        userId))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showManageDependenciesDialog() {
        if (currentDependencies.isEmpty()) return;
        String[] labels = new String[currentDependencies.size()];
        for (int index = 0; index < currentDependencies.size(); index++) {
            labels[index] = currentDependencies.get(index).getDependsOnTitle();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.manage_dependencies)
                .setItems(labels, (dialog, which) -> {
                    TaskDependency item = currentDependencies.get(which);
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.delete_dependency)
                            .setMessage(getString(R.string.delete_dependency_question,
                                    item.getDependsOnTitle()))
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.delete, (confirm, button) ->
                                    viewModel.deleteDependency(taskId,
                                            item.getDependsOnTaskId(), userId))
                            .show();
                })
                .setNegativeButton(R.string.close, null)
                .show();
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
        if (teamMode) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main, TeamTaskFormFragment.newInstance(
                            workspaceId, userId, taskId))
                    .addToBackStack("team_task_edit")
                    .commit();
            return;
        }
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
                        (dialog, which) -> viewModel.deleteTask(taskId, userId, teamMode)
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
