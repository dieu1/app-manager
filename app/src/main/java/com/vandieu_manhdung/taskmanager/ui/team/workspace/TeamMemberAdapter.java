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
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

import java.util.Objects;

public class TeamMemberAdapter extends
        ListAdapter<WorkspaceMember, TeamMemberAdapter.MemberViewHolder> {

    public interface Listener {
        void onMemberClicked(WorkspaceMember member);
    }

    private final Listener listener;

    public TeamMemberAdapter(Listener listener) {
        super(new DiffUtil.ItemCallback<WorkspaceMember>() {
            @Override
            public boolean areItemsTheSame(
                    @NonNull WorkspaceMember oldItem,
                    @NonNull WorkspaceMember newItem
            ) {
                return Objects.equals(oldItem.getUserId(), newItem.getUserId());
            }

            @Override
            public boolean areContentsTheSame(
                    @NonNull WorkspaceMember oldItem,
                    @NonNull WorkspaceMember newItem
            ) {
                return Objects.equals(oldItem.getRole(), newItem.getRole()) &&
                        Objects.equals(oldItem.getDisplayName(), newItem.getDisplayName()) &&
                        oldItem.getTotalTasks() == newItem.getTotalTasks() &&
                        oldItem.getCompletedTasks() == newItem.getCompletedTasks();
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MemberViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_member, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView role;
        private final TextView progress;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textMemberName);
            role = itemView.findViewById(R.id.textMemberRole);
            progress = itemView.findViewById(R.id.textMemberProgress);
        }

        void bind(WorkspaceMember member) {
            name.setText(itemView.getContext().getString(
                    R.string.member_name_code,
                    member.getDisplayName(),
                    member.getUserCode()));
            role.setText(itemView.getContext().getString(
                    R.string.member_role_value,
                    formatRole(member.getRole())));
            progress.setText(itemView.getContext().getString(
                    R.string.member_task_progress,
                    member.getCompletedTasks(),
                    member.getTotalTasks()));
            itemView.setOnClickListener(view -> listener.onMemberClicked(member));
        }

        private String formatRole(String value) {
            return switch (value) {
                case "OWNER" -> itemView.getContext().getString(R.string.role_owner);
                case "ADMIN" -> itemView.getContext().getString(R.string.role_admin);
                default -> itemView.getContext().getString(R.string.role_member);
            };
        }
    }
}
