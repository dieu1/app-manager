package com.vandieu_manhdung.taskmanager.ui.team.workspace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

import java.util.Locale;
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
        private final TextView code;
        private final TextView initial;
        private final TextView role;
        private final TextView progress;
        private final ProgressBar progressBar;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textMemberName);
            code = itemView.findViewById(R.id.textMemberCode);
            initial = itemView.findViewById(R.id.textMemberInitial);
            role = itemView.findViewById(R.id.textMemberRole);
            progress = itemView.findViewById(R.id.textMemberProgress);
            progressBar = itemView.findViewById(R.id.progressMemberTasks);
        }

        void bind(WorkspaceMember member) {
            String displayName = member.getDisplayName();
            name.setText(displayName);
            code.setText(member.getUserCode());
            initial.setText(displayName == null || displayName.isBlank()
                    ? "T" : displayName.substring(0, 1).toUpperCase(Locale.getDefault()));
            role.setText(formatRole(member.getRole()));
            styleRole(member.getRole());
            progress.setText(itemView.getContext().getString(
                    R.string.member_task_progress,
                    member.getCompletedTasks(),
                    member.getTotalTasks()));
            int completion = member.getTotalTasks() <= 0 ? 0
                    : Math.round(member.getCompletedTasks() * 100f / member.getTotalTasks());
            progressBar.setProgress(completion);
            itemView.setOnClickListener(view -> listener.onMemberClicked(member));
        }

        private void styleRole(String value) {
            int background;
            int color;
            if ("OWNER".equals(value)) {
                background = R.drawable.bg_team_badge_primary;
                color = R.color.team_primary;
            } else if ("ADMIN".equals(value)) {
                background = R.drawable.bg_team_badge_info;
                color = R.color.team_info;
            } else {
                background = R.drawable.bg_team_badge_neutral;
                color = R.color.team_text_secondary;
            }
            role.setBackgroundResource(background);
            role.setTextColor(ContextCompat.getColor(itemView.getContext(), color));
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
