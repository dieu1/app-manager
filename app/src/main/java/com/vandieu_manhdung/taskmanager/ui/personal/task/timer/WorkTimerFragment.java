package com.vandieu_manhdung.taskmanager.ui.personal.task.timer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.WorkTimerState;

import java.util.Locale;

public class WorkTimerFragment extends Fragment {

    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_TASK_TITLE = "task_title";

    private WorkTimerViewModel viewModel;
    private TextView elapsed;
    private TextView total;
    private TextView status;
    private Button startButton;
    private Button stopButton;
    private ProgressBar loading;

    public static WorkTimerFragment newInstance(
            String taskId,
            String userId,
            String taskTitle
    ) {
        WorkTimerFragment fragment = new WorkTimerFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TASK_ID, taskId);
        arguments.putString(ARG_USER_ID, userId);
        arguments.putString(ARG_TASK_TITLE, taskTitle);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_work_timer, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = requireArguments();

        ((TextView) view.findViewById(R.id.textTimerTaskTitle)).setText(
                arguments.getString(ARG_TASK_TITLE)
        );
        elapsed = view.findViewById(R.id.textTimerElapsed);
        total = view.findViewById(R.id.textTimerTotal);
        status = view.findViewById(R.id.textTimerStatus);
        startButton = view.findViewById(R.id.buttonStartTimer);
        stopButton = view.findViewById(R.id.buttonStopTimer);
        loading = view.findViewById(R.id.progressTimerLoading);

        viewModel = new ViewModelProvider(this).get(WorkTimerViewModel.class);
        viewModel.configure(
                arguments.getString(ARG_TASK_ID),
                arguments.getString(ARG_USER_ID)
        );

        view.findViewById(R.id.buttonBackTimer)
                .setOnClickListener(ignored ->
                        getParentFragmentManager().popBackStack());
        startButton.setOnClickListener(ignored -> viewModel.start());
        stopButton.setOnClickListener(ignored -> viewModel.stop());

        viewModel.getState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getElapsedSeconds().observe(
                getViewLifecycleOwner(),
                seconds -> elapsed.setText(formatDuration(seconds == null ? 0 : seconds))
        );
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean busy = Boolean.TRUE.equals(isLoading);
            loading.setVisibility(busy ? View.VISIBLE : View.GONE);
            startButton.setEnabled(!busy);
            stopButton.setEnabled(!busy);
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                return;
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            viewModel.clearError();
        });

        viewModel.load();
    }

    private void renderState(WorkTimerState value) {
        if (value == null) {
            return;
        }

        boolean running = value.isRunning();
        status.setText(running
                ? R.string.timer_running
                : R.string.timer_stopped);
        total.setText(getString(
                R.string.timer_total_minutes,
                value.getTotalMinutes()
        ));
        startButton.setVisibility(running ? View.GONE : View.VISIBLE);
        stopButton.setVisibility(running ? View.VISIBLE : View.GONE);
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
        );
    }
}
