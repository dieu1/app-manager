package com.vandieu_manhdung.taskmanager.ui.team.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.ProjectProgress;

import java.util.Objects;

public class TeamProjectProgressAdapter extends
        ListAdapter<ProjectProgress, TeamProjectProgressAdapter.ProgressViewHolder> {

    public TeamProjectProgressAdapter() {
        super(new DiffUtil.ItemCallback<ProjectProgress>() {
            @Override
            public boolean areItemsTheSame(
                    @NonNull ProjectProgress oldItem,
                    @NonNull ProjectProgress newItem
            ) {
                return Objects.equals(oldItem.getProjectId(), newItem.getProjectId());
            }

            @Override
            public boolean areContentsTheSame(
                    @NonNull ProjectProgress oldItem,
                    @NonNull ProjectProgress newItem
            ) {
                return oldItem.getTotal() == newItem.getTotal() &&
                        oldItem.getCompleted() == newItem.getCompleted() &&
                        oldItem.getOverdue() == newItem.getOverdue();
            }
        });
    }

    @NonNull
    @Override
    public ProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProgressViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_project_progress, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView summary;
        private final ProgressBar progress;

        ProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textDashboardProjectName);
            summary = itemView.findViewById(R.id.textDashboardProjectSummary);
            progress = itemView.findViewById(R.id.progressDashboardProject);
        }

        void bind(ProjectProgress value) {
            name.setText(value.getProjectName());
            summary.setText(itemView.getContext().getString(
                    R.string.project_progress_summary,
                    value.getCompleted(),
                    value.getTotal(),
                    value.getOverdue(),
                    value.getCompletionRate()));
            progress.setProgress(value.getCompletionRate());
        }
    }
}
