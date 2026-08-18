package com.vandieu_manhdung.taskmanager.ui.team.workspace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.Project;

import java.util.Objects;

public class TeamProjectAdapter extends
        ListAdapter<Project, TeamProjectAdapter.ProjectViewHolder> {

    public TeamProjectAdapter() {
        super(new DiffUtil.ItemCallback<Project>() {
            @Override
            public boolean areItemsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
                return Objects.equals(oldItem.getProjectId(), newItem.getProjectId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
                return Objects.equals(oldItem.getName(), newItem.getName()) &&
                        Objects.equals(oldItem.getDescription(), newItem.getDescription());
            }
        });
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

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView description;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textProjectName);
            description = itemView.findViewById(R.id.textProjectDescription);
        }

        void bind(Project project) {
            name.setText(project.getName());
            String value = project.getDescription();
            description.setText(value == null || value.isBlank()
                    ? itemView.getContext().getString(R.string.no_project_description)
                    : value);
        }
    }
}
