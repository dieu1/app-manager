package com.vandieu_manhdung.taskmanager.ui.personal.task;

import android.content.Context;
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
import com.vandieu_manhdung.taskmanager.core.constant.TaskPriority;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;
import com.vandieu_manhdung.taskmanager.model.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class PersonalTaskAdapter extends
        ListAdapter<Task, PersonalTaskAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private final OnTaskClickListener listener;

    public PersonalTaskAdapter(
            OnTaskClickListener listener
    ) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Task>
            DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Task>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull Task oldItem,
                        @NonNull Task newItem
                ) {
                    return Objects.equals(
                            oldItem.getTaskId(),
                            newItem.getTaskId()
                    );
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Task oldItem,
                        @NonNull Task newItem
                ) {
                    return Objects.equals(
                            oldItem.getTitle(),
                            newItem.getTitle()
                    ) &&
                            Objects.equals(
                                    oldItem.getDescription(),
                                    newItem.getDescription()
                            ) &&
                            Objects.equals(
                                    oldItem.getStatus(),
                                    newItem.getStatus()
                            ) &&
                            Objects.equals(
                                    oldItem.getPriority(),
                                    newItem.getPriority()
                            ) &&
                            oldItem.getProgress() ==
                                    newItem.getProgress() &&
                            oldItem.getDueDate() ==
                                    newItem.getDueDate();
                }
            };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_task,
                        parent,
                        false
                );

        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TaskViewHolder holder,
            int position
    ) {
        Task task = getItem(position);
        holder.bind(task);
    }

    class TaskViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView textTitle;
        private final TextView textDescription;
        private final TextView textStatus;
        private final TextView textPriority;
        private final TextView textDueDate;
        private final TextView textProgress;
        private final ProgressBar progressBar;

        public TaskViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            textTitle =
                    itemView.findViewById(
                            R.id.textTaskTitle
                    );

            textDescription =
                    itemView.findViewById(
                            R.id.textTaskDescription
                    );

            textStatus =
                    itemView.findViewById(
                            R.id.textTaskStatus
                    );

            textPriority =
                    itemView.findViewById(
                            R.id.textTaskPriority
                    );

            textDueDate =
                    itemView.findViewById(
                            R.id.textTaskDueDate
                    );

            textProgress =
                    itemView.findViewById(
                            R.id.textTaskProgress
                    );

            progressBar =
                    itemView.findViewById(
                            R.id.progressTask
                    );
        }

        void bind(Task task) {
            textTitle.setText(task.getTitle());

            if (task.getDescription() == null ||
                    task.getDescription().isBlank()) {
                textDescription.setVisibility(View.GONE);
            } else {
                textDescription.setVisibility(View.VISIBLE);
                textDescription.setText(
                        task.getDescription()
                );
            }

            textStatus.setText(
                    formatStatus(
                            task.getStatus(),
                            itemView.getContext()
                    )
            );

            textPriority.setText(
                    formatPriority(
                            task.getPriority(),
                            itemView.getContext()
                    )
            );

            if (task.getDueDate() > 0) {
                SimpleDateFormat formatter =
                        new SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault()
                        );

                boolean overdue = TaskRules.isOverdue(
                        task,
                        System.currentTimeMillis()
                );
                String formattedDate = formatter.format(
                        new Date(task.getDueDate())
                );
                textDueDate.setText(itemView.getContext().getString(
                        overdue
                                ? R.string.task_overdue_value
                                : R.string.task_due_date_value,
                        formattedDate
                ));
                textDueDate.setTextColor(ContextCompat.getColor(
                        itemView.getContext(),
                        overdue
                                ? R.color.task_overdue
                                : R.color.task_text_primary
                ));
            } else {
                textDueDate.setText(R.string.no_due_date);
                textDueDate.setTextColor(ContextCompat.getColor(
                        itemView.getContext(),
                        R.color.task_text_primary
                ));
            }

            progressBar.setProgress(
                    task.getProgress()
            );

            textProgress.setText(
                    itemView.getContext().getString(
                            R.string.progress_percent,
                            task.getProgress()
                    )
            );

            itemView.setOnClickListener(
                    view -> {
                        if (listener != null) {
                            listener.onTaskClick(task);
                        }
                    }
            );
        }
    }

    private String formatStatus(String status, Context context) {
        if (TaskStatus.TODO.equals(status)) {
            return context.getString(R.string.status_todo);
        }

        if (TaskStatus.IN_PROGRESS.equals(status)) {
            return context.getString(R.string.status_in_progress);
        }

        if (TaskStatus.COMPLETED.equals(status)) {
            return context.getString(R.string.status_completed);
        }

        if (TaskStatus.CANCELLED.equals(status)) {
            return context.getString(R.string.status_cancelled);
        }

        return status;
    }

    private String formatPriority(String priority, Context context) {
        if (TaskPriority.LOW.equals(priority)) {
            return context.getString(R.string.priority_low);
        }

        if (TaskPriority.MEDIUM.equals(priority)) {
            return context.getString(R.string.priority_medium);
        }

        if (TaskPriority.HIGH.equals(priority)) {
            return context.getString(R.string.priority_high);
        }

        if (TaskPriority.URGENT.equals(priority)) {
            return context.getString(R.string.priority_urgent);
        }

        return priority;
    }

}
