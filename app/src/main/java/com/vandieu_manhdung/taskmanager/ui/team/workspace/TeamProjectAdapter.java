package com.vandieu_manhdung.taskmanager.ui.team.workspace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.Project;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class TeamProjectAdapter extends
        ListAdapter<Project, TeamProjectAdapter.ProjectViewHolder> {

    public interface Listener { void onProjectClicked(Project project); }
    private final Listener listener;

    public TeamProjectAdapter() { this(project -> { }); }

    public TeamProjectAdapter(Listener listener) {
        super(new DiffUtil.ItemCallback<Project>() {
            @Override
            public boolean areItemsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
                return Objects.equals(oldItem.getProjectId(), newItem.getProjectId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
                return Objects.equals(oldItem.getName(), newItem.getName()) &&
                        Objects.equals(oldItem.getDescription(), newItem.getDescription()) &&
                        Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                        oldItem.getDueDate() == newItem.getDueDate();
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProjectViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_project, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView description;
        private final TextView initial;
        private final TextView status;
        private final TextView dueDate;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textProjectName);
            description = itemView.findViewById(R.id.textProjectDescription);
            initial = itemView.findViewById(R.id.textProjectInitial);
            status = itemView.findViewById(R.id.textProjectStatus);
            dueDate = itemView.findViewById(R.id.textProjectDueDate);
        }

        void bind(Project project) {
            name.setText(project.getName());
            String projectName = project.getName();
            initial.setText(projectName == null || projectName.isBlank()
                    ? "D" : projectName.substring(0, 1).toUpperCase(Locale.getDefault()));
            String value = project.getDescription();
            description.setText(value == null || value.isBlank()
                    ? itemView.getContext().getString(R.string.no_project_description)
                    : value);
            status.setText(formatStatus(project.getStatus()));
            styleStatus(project.getStatus());
            dueDate.setText(project.getDueDate() > 0
                    ? itemView.getContext().getString(R.string.task_due_date_value,
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(new Date(project.getDueDate())))
                    : itemView.getContext().getString(R.string.no_due_date));
            boolean overdue = project.getDueDate() > 0
                    && project.getDueDate() < System.currentTimeMillis()
                    && !"COMPLETED".equals(project.getStatus())
                    && !"ARCHIVED".equals(project.getStatus());
            dueDate.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    overdue ? R.color.team_danger : R.color.team_text_secondary));
            itemView.setOnClickListener(view -> listener.onProjectClicked(project));
        }

        private void styleStatus(String value) {
            int background;
            int color;
            if ("COMPLETED".equals(value)) {
                background = R.drawable.bg_team_badge_success;
                color = R.color.team_success;
            } else if ("ARCHIVED".equals(value)) {
                background = R.drawable.bg_team_badge_neutral;
                color = R.color.team_text_secondary;
            } else {
                background = R.drawable.bg_team_badge_info;
                color = R.color.team_info;
            }
            status.setBackgroundResource(background);
            status.setTextColor(ContextCompat.getColor(itemView.getContext(), color));
        }

        private String formatStatus(String value) {
            if ("COMPLETED".equals(value)) {
                return itemView.getContext().getString(R.string.team_ui_project_completed);
            }
            if ("ARCHIVED".equals(value)) {
                return itemView.getContext().getString(R.string.team_ui_project_archived);
            }
            return itemView.getContext().getString(R.string.team_ui_project_active);
        }
    }
}
