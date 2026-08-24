package com.vandieu_manhdung.taskmanager.ui.team.workspace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class TeamTaskAdapter extends
        ListAdapter<TeamTaskItem, TeamTaskAdapter.TaskViewHolder> {

    public interface Listener {
        void onTaskClicked(TeamTaskItem item);
    }

    private final Listener listener;

    public TeamTaskAdapter(Listener listener) {
        super(new DiffUtil.ItemCallback<TeamTaskItem>() {
            @Override
            public boolean areItemsTheSame(
                    @NonNull TeamTaskItem oldItem,
                    @NonNull TeamTaskItem newItem
            ) {
                return Objects.equals(
                        oldItem.getTask().getTaskId(),
                        newItem.getTask().getTaskId());
            }

            @Override
            public boolean areContentsTheSame(
                    @NonNull TeamTaskItem oldItem,
                    @NonNull TeamTaskItem newItem
            ) {
                Task oldTask = oldItem.getTask();
                Task newTask = newItem.getTask();
                return Objects.equals(oldTask.getTitle(), newTask.getTitle()) &&
                        Objects.equals(oldTask.getStatus(), newTask.getStatus()) &&
                        Objects.equals(oldTask.getPriority(), newTask.getPriority()) &&
                        Objects.equals(oldItem.getAssigneeIds(), newItem.getAssigneeIds()) &&
                        oldTask.getProgress() == newTask.getProgress() &&
                        oldTask.getDueDate() == newTask.getDueDate();
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TaskViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView meta;
        private final TextView status;
        private final TextView dueDate;
        private final TextView progressLabel;
        private final ProgressBar progress;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTeamTaskTitle);
            meta = itemView.findViewById(R.id.textTeamTaskMeta);
            status = itemView.findViewById(R.id.textTeamTaskStatus);
            dueDate = itemView.findViewById(R.id.textTeamTaskDueDate);
            progressLabel = itemView.findViewById(R.id.textTeamTaskProgressLabel);
            progress = itemView.findViewById(R.id.progressTeamTask);
        }

        void bind(TeamTaskItem item) {
            Task task = item.getTask();
            title.setText(task.getTitle());
            meta.setText(itemView.getContext().getString(
                    R.string.team_task_meta,
                    item.getProjectName(),
                    item.getAssigneeName()));
            status.setText(formatStatus(task.getStatus()));
            styleStatus(task.getStatus());
            progressLabel.setText(itemView.getContext().getString(
                    R.string.team_ui_progress_label, task.getProgress()));
            dueDate.setText(task.getDueDate() > 0
                    ? itemView.getContext().getString(
                            R.string.task_due_date_value,
                            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    .format(new Date(task.getDueDate())))
                    : itemView.getContext().getString(R.string.no_due_date));
            boolean overdue = task.getDueDate() > 0 && task.getDueDate() < System.currentTimeMillis()
                    && !"COMPLETED".equals(task.getStatus())
                    && !"CANCELLED".equals(task.getStatus());
            dueDate.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    overdue ? R.color.team_danger : R.color.team_text_secondary));
            progress.setProgress(task.getProgress());
            itemView.setOnClickListener(view -> listener.onTaskClicked(item));
        }

        private void styleStatus(String value) {
            int background;
            int color;
            if ("IN_PROGRESS".equals(value)) {
                background = R.drawable.bg_team_badge_warning;
                color = R.color.team_warning;
            } else if ("COMPLETED".equals(value)) {
                background = R.drawable.bg_team_badge_success;
                color = R.color.team_success;
            } else if ("CANCELLED".equals(value)) {
                background = R.drawable.bg_team_badge_danger;
                color = R.color.team_danger;
            } else {
                background = R.drawable.bg_team_badge_info;
                color = R.color.team_info;
            }
            status.setBackgroundResource(background);
            status.setTextColor(ContextCompat.getColor(itemView.getContext(), color));
        }

        private String formatStatus(String value) {
            return switch (value) {
                case "IN_PROGRESS" -> itemView.getContext().getString(R.string.status_in_progress);
                case "COMPLETED" -> itemView.getContext().getString(R.string.status_completed);
                case "CANCELLED" -> itemView.getContext().getString(R.string.status_cancelled);
                default -> itemView.getContext().getString(R.string.status_todo);
            };
        }
    }
}
