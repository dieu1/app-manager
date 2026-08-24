package com.vandieu_manhdung.taskmanager.ui.team;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.model.TeamInvite;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;
import com.vandieu_manhdung.taskmanager.ui.team.workspace.TeamWorkspaceFragment;

import java.util.List;

public class TeamListFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";

    private String userId;
    private TeamListViewModel viewModel;
    private TeamWorkspaceAdapter adapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private View invitesButton;
    private List<TeamInvite> pendingInvites = List.of();

    public static TeamListFragment newInstance(String userId) {
        TeamListFragment fragment = new TeamListFragment();
        Bundle arguments = new Bundle();
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
        return inflater.inflate(R.layout.fragment_team_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userId = requireArguments().getString(ARG_USER_ID);
        recyclerView = view.findViewById(R.id.recyclerTeams);
        progressBar = view.findViewById(R.id.progressTeams);
        emptyView = view.findViewById(R.id.textEmptyTeams);
        invitesButton = view.findViewById(R.id.buttonTeamInvites);

        adapter = new TeamWorkspaceAdapter(this::openTeam);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(TeamListViewModel.class);
        observeViewModel();

        view.findViewById(R.id.buttonBackToPersonal).setOnClickListener(
                button -> ((MainActivity) requireActivity()).openHome());
        view.findViewById(R.id.buttonAddTeam).setOnClickListener(
                button -> showCreateTeamDialog());
        view.findViewById(R.id.buttonRefreshTeams).setOnClickListener(
                button -> viewModel.loadTeams());
        view.findViewById(R.id.buttonTeamNotifications).setOnClickListener(
                button -> ((MainActivity) requireActivity()).openNotifications());
        invitesButton.setOnClickListener(button -> showInvites());

        viewModel.setUserId(userId);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadTeams();
        }
    }

    private void observeViewModel() {
        viewModel.getTeams().observe(getViewLifecycleOwner(), this::displayTeams);
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                progressBar.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
        viewModel.getCreatedTeam().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) {
                viewModel.clearCreatedTeam();
                openTeam(workspace);
            }
        });
        viewModel.getInvites().observe(getViewLifecycleOwner(), values -> {
            pendingInvites = values == null ? List.of() : values;
            invitesButton.setVisibility(pendingInvites.isEmpty() ? View.GONE : View.VISIBLE);
            if (!pendingInvites.isEmpty()) {
                ((TextView) invitesButton).setText(getString(
                        R.string.team_invites_count, pendingInvites.size()));
            }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }

    private void showInvites() {
        if (pendingInvites.isEmpty()) return;
        String[] labels = new String[pendingInvites.size()];
        for (int index = 0; index < pendingInvites.size(); index++) {
            TeamInvite invite = pendingInvites.get(index);
            labels[index] = invite.getWorkspaceName() + " · " + invite.getRole();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.team_invites_title)
                .setItems(labels, (dialog, which) -> confirmInvite(pendingInvites.get(which)))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void confirmInvite(TeamInvite invite) {
        new AlertDialog.Builder(requireContext())
                .setTitle(invite.getWorkspaceName())
                .setMessage(getString(R.string.team_invite_question, invite.getRole()))
                .setNegativeButton(R.string.reject, (dialog, which) ->
                        viewModel.respondToInvite(invite, false))
                .setPositiveButton(R.string.accept, (dialog, which) ->
                        viewModel.respondToInvite(invite, true))
                .show();
    }

    private void displayTeams(List<Workspace> teams) {
        adapter.submitList(teams);
        boolean empty = teams == null || teams.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showCreateTeamDialog() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_team_form, null, false);
        EditText name = content.findViewById(R.id.editTeamName);
        EditText description = content.findViewById(R.id.editTeamDescription);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.create_team)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.create, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    if (name.getText().toString().trim().isEmpty()) {
                        name.setError(getString(R.string.team_name_required));
                        return;
                    }
                    viewModel.createTeam(
                            name.getText().toString(),
                            description.getText().toString());
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void openTeam(Workspace workspace) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main, TeamWorkspaceFragment.newInstance(
                        workspace.getWorkspaceId(), userId))
                .addToBackStack("team_workspace")
                .commit();
    }
}
