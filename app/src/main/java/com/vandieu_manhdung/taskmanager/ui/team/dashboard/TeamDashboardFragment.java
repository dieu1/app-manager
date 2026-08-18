package com.vandieu_manhdung.taskmanager.ui.team.dashboard;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.TeamDashboardData;
import com.vandieu_manhdung.taskmanager.model.TeamDashboardSummary;
import com.vandieu_manhdung.taskmanager.ui.team.workspace.TeamMemberAdapter;

public class TeamDashboardFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "workspace_id";
    private static final String ARG_USER_ID = "user_id";

    private TeamDashboardViewModel viewModel;
    private TeamProjectProgressAdapter projectAdapter;
    private TeamMemberAdapter memberAdapter;
    private ProgressBar loading;

    public static TeamDashboardFragment newInstance(String workspaceId, String userId) {
        TeamDashboardFragment fragment = new TeamDashboardFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_WORKSPACE_ID, workspaceId);
        arguments.putString(ARG_USER_ID, userId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_team_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String workspaceId = requireArguments().getString(ARG_WORKSPACE_ID);
        String userId = requireArguments().getString(ARG_USER_ID);
        loading = view.findViewById(R.id.progressLoadingTeamDashboard);

        RecyclerView projectList = view.findViewById(R.id.recyclerDashboardProjects);
        RecyclerView memberList = view.findViewById(R.id.recyclerDashboardMembers);
        projectAdapter = new TeamProjectProgressAdapter();
        memberAdapter = new TeamMemberAdapter(member -> {
        });
        projectList.setLayoutManager(new LinearLayoutManager(requireContext()));
        memberList.setLayoutManager(new LinearLayoutManager(requireContext()));
        projectList.setAdapter(projectAdapter);
        memberList.setAdapter(memberAdapter);
        projectList.setNestedScrollingEnabled(false);
        memberList.setNestedScrollingEnabled(false);

        view.findViewById(R.id.buttonBackTeamDashboard).setOnClickListener(
                button -> getParentFragmentManager().popBackStack());
        view.findViewById(R.id.buttonRefreshTeamDashboard).setOnClickListener(
                button -> viewModel.load());

        viewModel = new ViewModelProvider(this).get(TeamDashboardViewModel.class);
        viewModel.getData().observe(getViewLifecycleOwner(), data -> display(view, data));
        viewModel.getLoading().observe(getViewLifecycleOwner(), value ->
                loading.setVisibility(Boolean.TRUE.equals(value) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
        viewModel.initialize(workspaceId, userId);
    }

    private void display(View view, TeamDashboardData data) {
        TeamDashboardSummary summary = data.getSummary();
        view.<TextView>findViewById(R.id.textTeamDashboardTitle).setText(getString(
                R.string.team_dashboard_title,
                data.getSnapshot().getWorkspace().getName()));
        view.<TextView>findViewById(R.id.textTeamDashboardRate).setText(getString(
                R.string.team_completion_rate,
                summary.getCompletionRate()));
        view.<ProgressBar>findViewById(R.id.progressTeamDashboardRate)
                .setProgress(summary.getCompletionRate());
        setCount(view, R.id.textTeamDashboardTotal, R.string.dashboard_total_label, summary.getTotal());
        setCount(view, R.id.textTeamDashboardTodo, R.string.status_todo, summary.getTodo());
        setCount(view, R.id.textTeamDashboardInProgress, R.string.status_in_progress, summary.getInProgress());
        setCount(view, R.id.textTeamDashboardCompleted, R.string.status_completed, summary.getCompleted());
        setCount(view, R.id.textTeamDashboardCancelled, R.string.status_cancelled, summary.getCancelled());
        setCount(view, R.id.textTeamDashboardOverdue, R.string.overdue_tasks, summary.getOverdue());

        projectAdapter.submitList(data.getProjectProgress());
        memberAdapter.submitList(data.getSnapshot().getMembers());
        view.findViewById(R.id.textDashboardNoProjects).setVisibility(
                data.getProjectProgress().isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setCount(View view, int viewId, int labelId, int value) {
        view.<TextView>findViewById(viewId).setText(getString(
                R.string.dashboard_count_value,
                getString(labelId),
                value));
    }
}
