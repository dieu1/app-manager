package com.vandieu_manhdung.taskmanager.ui.personal.trash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.repository.TaskRepository;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.ui.personal.task.PersonalTaskAdapter;

import java.util.List;

public class TaskTrashFragment extends Fragment {
    private static final String ARG_WORKSPACE_ID = "workspace_id";
    private String workspaceId;
    private TaskRepository repository;
    private PersonalTaskAdapter adapter;
    private TextView empty;

    public static TaskTrashFragment newInstance(String workspaceId) {
        TaskTrashFragment fragment = new TaskTrashFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WORKSPACE_ID, workspaceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                      Bundle state) {
        return inflater.inflate(R.layout.fragment_task_trash, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        workspaceId = requireArguments().getString(ARG_WORKSPACE_ID, "");
        repository = new TaskRepository(requireContext());
        empty = view.findViewById(R.id.textTrashEmpty);
        RecyclerView recycler = view.findViewById(R.id.recyclerTrash);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PersonalTaskAdapter(this::confirmRestore);
        recycler.setAdapter(adapter);
        view.findViewById(R.id.buttonTrashBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        load();
    }

    private void load() {
        repository.getDeletedPersonalTasks(workspaceId, new RepositoryCallback<List<Task>>() {
            @Override public void onSuccess(List<Task> tasks) {
                adapter.submitList(tasks);
                empty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onError(Exception exception) { showError(exception); }
        });
    }

    private void confirmRestore(Task task) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.restore_task)
                .setMessage(getString(R.string.restore_task_question, task.getTitle()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore, (dialog, which) ->
                        repository.restorePersonalTask(task.getTaskId(),
                                new RepositoryCallback<Task>() {
                                    @Override public void onSuccess(Task ignored) {
                                        Toast.makeText(requireContext(), R.string.task_restored,
                                                Toast.LENGTH_SHORT).show();
                                        load();
                                    }
                                    @Override public void onError(Exception exception) {
                                        showError(exception);
                                    }
                                })).show();
    }

    private void showError(Exception exception) {
        Toast.makeText(requireContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
    }
}
