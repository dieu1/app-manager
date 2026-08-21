package com.vandieu_manhdung.taskmanager.ui.personal.task.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;

import java.util.ArrayList;
import java.util.List;

public class TaskSubtaskAdapter extends RecyclerView.Adapter<TaskSubtaskAdapter.SubtaskViewHolder> {

    public interface Listener {
        void onToggle(TaskSubtask subtask, boolean completed);

        void onDelete(TaskSubtask subtask);
    }

    private final Listener listener;
    private final List<TaskSubtask> items = new ArrayList<>();

    public TaskSubtaskAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<TaskSubtask> subtasks) {
        items.clear();
        if (subtasks != null) {
            items.addAll(subtasks);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubtaskViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        return new SubtaskViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_subtask, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull SubtaskViewHolder holder,
            int position
    ) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class SubtaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox completed;
        private final TextView title;
        private final TextView estimate;
        private final TextView delete;

        SubtaskViewHolder(@NonNull View itemView) {
            super(itemView);
            completed = itemView.findViewById(R.id.checkSubtaskCompleted);
            title = itemView.findViewById(R.id.textSubtaskTitle);
            estimate = itemView.findViewById(R.id.textSubtaskEstimate);
            delete = itemView.findViewById(R.id.buttonDeleteSubtask);
        }

        void bind(TaskSubtask subtask) {
            title.setText(subtask.getTitle());
            estimate.setText(itemView.getContext().getString(
                    R.string.subtask_estimated,
                    subtask.getEstimatedMinutes()
            ));
            completed.setOnCheckedChangeListener(null);
            completed.setChecked(subtask.isCompleted());
            completed.setOnCheckedChangeListener((button, checked) ->
                    listener.onToggle(subtask, checked));
            delete.setOnClickListener(ignored -> listener.onDelete(subtask));
        }
    }
}
