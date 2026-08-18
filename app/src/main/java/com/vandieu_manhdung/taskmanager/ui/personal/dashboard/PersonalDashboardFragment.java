package com.vandieu_manhdung.taskmanager.ui.personal.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.PersonalDashboardSummary;

public class PersonalDashboardFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "workspace_id";

    private PersonalDashboardViewModel viewModel;
    private ProgressBar loading;
    private ProgressBar completionProgress;
    private TextView total;
    private TextView todo;
    private TextView inProgress;
    private TextView completed;
    private TextView cancelled;
    private TextView overdue;
    private TextView completionRate;

    public static PersonalDashboardFragment newInstance(String workspaceId) {
        PersonalDashboardFragment fragment = new PersonalDashboardFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_WORKSPACE_ID, workspaceId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_personal_dashboard,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);

        viewModel = new ViewModelProvider(this)
                .get(PersonalDashboardViewModel.class);
        viewModel.setWorkspaceId(
                requireArguments().getString(ARG_WORKSPACE_ID)
        );

        view.findViewById(R.id.buttonBackDashboard)
                .setOnClickListener(ignored ->
                        getParentFragmentManager().popBackStack());
        view.findViewById(R.id.buttonRefreshDashboard)
                .setOnClickListener(ignored -> viewModel.loadSummary());

        viewModel.getSummary().observe(
                getViewLifecycleOwner(),
                this::renderSummary
        );
        viewModel.getLoading().observe(
                getViewLifecycleOwner(),
                isLoading -> loading.setVisibility(
                        Boolean.TRUE.equals(isLoading)
                                ? View.VISIBLE
                                : View.GONE
                )
        );
        viewModel.getError().observe(
                getViewLifecycleOwner(),
                message -> {
                    if (message == null) {
                        return;
                    }
                    Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                    viewModel.clearError();
                }
        );

        viewModel.loadSummary();
    }

    private void bindViews(View view) {
        loading = view.findViewById(R.id.progressLoadingDashboard);
        completionProgress = view.findViewById(R.id.progressCompletionRate);
        total = view.findViewById(R.id.textDashboardTotal);
        todo = view.findViewById(R.id.textDashboardTodo);
        inProgress = view.findViewById(R.id.textDashboardInProgress);
        completed = view.findViewById(R.id.textDashboardCompleted);
        cancelled = view.findViewById(R.id.textDashboardCancelled);
        overdue = view.findViewById(R.id.textDashboardOverdue);
        completionRate = view.findViewById(R.id.textDashboardCompletionRate);
    }

    private void renderSummary(PersonalDashboardSummary value) {
        if (value == null) {
            return;
        }

        total.setText(String.valueOf(value.getTotal()));
        todo.setText(String.valueOf(value.getTodo()));
        inProgress.setText(String.valueOf(value.getInProgress()));
        completed.setText(String.valueOf(value.getCompleted()));
        cancelled.setText(String.valueOf(value.getCancelled()));
        overdue.setText(String.valueOf(value.getOverdue()));
        completionProgress.setProgress(value.getCompletionRate());
        completionRate.setText(getString(
                R.string.completion_rate_value,
                value.getCompletionRate()
        ));
    }
}
