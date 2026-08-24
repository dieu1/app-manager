package com.vandieu_manhdung.taskmanager.ui.team;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.Workspace;

import java.util.Locale;
import java.util.Objects;

public class TeamWorkspaceAdapter extends
        ListAdapter<Workspace, TeamWorkspaceAdapter.WorkspaceViewHolder> {

    public interface Listener {
        void onWorkspaceClicked(Workspace workspace);
    }

    private final Listener listener;

    public TeamWorkspaceAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Workspace> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Workspace>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull Workspace oldItem,
                        @NonNull Workspace newItem
                ) {
                    return Objects.equals(oldItem.getWorkspaceId(), newItem.getWorkspaceId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Workspace oldItem,
                        @NonNull Workspace newItem
                ) {
                    return Objects.equals(oldItem.getName(), newItem.getName()) &&
                            Objects.equals(oldItem.getDescription(), newItem.getDescription()) &&
                            oldItem.getUpdatedAt() == newItem.getUpdatedAt();
                }
            };

    @NonNull
    @Override
    public WorkspaceViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        return new WorkspaceViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_workspace, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull WorkspaceViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class WorkspaceViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView description;
        private final TextView initial;

        WorkspaceViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textTeamName);
            description = itemView.findViewById(R.id.textTeamDescription);
            initial = itemView.findViewById(R.id.textTeamInitial);
        }

        void bind(Workspace workspace) {
            name.setText(workspace.getName());
            String teamName = workspace.getName();
            initial.setText(teamName == null || teamName.isBlank()
                    ? "T" : teamName.substring(0, 1).toUpperCase(Locale.getDefault()));
            String value = workspace.getDescription();
            description.setText(value == null || value.isBlank()
                    ? itemView.getContext().getString(R.string.no_team_description)
                    : value);
            itemView.setOnClickListener(view -> listener.onWorkspaceClicked(workspace));
        }
    }
}
