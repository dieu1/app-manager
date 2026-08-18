package com.vandieu_manhdung.taskmanager.ui.team.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.constant.TeamRole;
import com.vandieu_manhdung.taskmanager.core.util.TeamRules;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.TeamWorkspaceSnapshot;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;
import com.vandieu_manhdung.taskmanager.ui.team.task.TeamTaskFormFragment;
import com.vandieu_manhdung.taskmanager.ui.team.dashboard.TeamDashboardFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TeamWorkspaceFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "workspace_id";
    private static final String ARG_USER_ID = "user_id";

    private String workspaceId;
    private String userId;
    private TeamWorkspaceViewModel viewModel;
    private TeamWorkspaceSnapshot currentSnapshot;
    private TeamMemberAdapter memberAdapter;
    private TeamProjectAdapter projectAdapter;
    private TeamTaskAdapter taskAdapter;
    private ProgressBar progressBar;
    private TextView noProjects;
    private TextView noTasks;
    private Spinner projectFilterSpinner;
    private Spinner assigneeFilterSpinner;
    private Spinner statusFilterSpinner;
    private boolean updatingFilters;
    private String selectedProjectId;
    private String selectedAssigneeId;
    private String selectedStatus;
    private List<Project> projects = List.of();
    private List<WorkspaceMember> members = List.of();

    public static TeamWorkspaceFragment newInstance(String workspaceId, String userId) {
        TeamWorkspaceFragment fragment = new TeamWorkspaceFragment();
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
        return inflater.inflate(R.layout.fragment_team_workspace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        workspaceId = requireArguments().getString(ARG_WORKSPACE_ID);
        userId = requireArguments().getString(ARG_USER_ID);
        bindLists(view);
        bindFilters();

        progressBar = view.findViewById(R.id.progressTeamWorkspace);
        noProjects = view.findViewById(R.id.textNoProjects);
        noTasks = view.findViewById(R.id.textNoTeamTasks);
        viewModel = new ViewModelProvider(this).get(TeamWorkspaceViewModel.class);
        observe(view);

        view.findViewById(R.id.buttonBackTeams).setOnClickListener(
                button -> getParentFragmentManager().popBackStack());
        view.findViewById(R.id.buttonEditTeam).setOnClickListener(
                button -> showEditTeamDialog());
        view.findViewById(R.id.buttonDeleteTeam).setOnClickListener(
                button -> confirmDeleteTeam());
        view.findViewById(R.id.buttonAddMember).setOnClickListener(
                button -> showAddMemberDialog());
        view.findViewById(R.id.buttonAddProject).setOnClickListener(
                button -> showAddProjectDialog());
        view.findViewById(R.id.buttonAddTeamTask).setOnClickListener(
                button -> openTaskForm(null));
        view.findViewById(R.id.buttonTeamDashboard).setOnClickListener(
                button -> openDashboard());

        viewModel.initialize(workspaceId, userId);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.load();
        }
    }

    private void bindLists(View view) {
        RecyclerView memberList = view.findViewById(R.id.recyclerTeamMembers);
        RecyclerView projectList = view.findViewById(R.id.recyclerTeamProjects);
        RecyclerView taskList = view.findViewById(R.id.recyclerTeamTasks);
        memberAdapter = new TeamMemberAdapter(this::showMemberActions);
        projectAdapter = new TeamProjectAdapter();
        taskAdapter = new TeamTaskAdapter(this::openTask);
        memberList.setLayoutManager(new LinearLayoutManager(requireContext()));
        projectList.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskList.setLayoutManager(new LinearLayoutManager(requireContext()));
        memberList.setAdapter(memberAdapter);
        projectList.setAdapter(projectAdapter);
        taskList.setAdapter(taskAdapter);
        memberList.setNestedScrollingEnabled(false);
        projectList.setNestedScrollingEnabled(false);
        taskList.setNestedScrollingEnabled(false);

        projectFilterSpinner = view.findViewById(R.id.spinnerProjectFilter);
        assigneeFilterSpinner = view.findViewById(R.id.spinnerAssigneeFilter);
        statusFilterSpinner = view.findViewById(R.id.spinnerTeamStatusFilter);
    }

    private void bindFilters() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (updatingFilters) {
                    return;
                }
                String nextProjectId = selectedProjectId;
                String nextAssigneeId = selectedAssigneeId;
                String nextStatus = selectedStatus;
                if (parent.getId() == R.id.spinnerProjectFilter) {
                    nextProjectId = position == 0 ? null : projects.get(position - 1).getProjectId();
                } else if (parent.getId() == R.id.spinnerAssigneeFilter) {
                    nextAssigneeId = position == 0 ? null : members.get(position - 1).getUserId();
                } else {
                    nextStatus = switch (position) {
                        case 1 -> TaskStatus.TODO;
                        case 2 -> TaskStatus.IN_PROGRESS;
                        case 3 -> TaskStatus.COMPLETED;
                        case 4 -> TaskStatus.CANCELLED;
                        default -> null;
                    };
                }
                if (Objects.equals(selectedProjectId, nextProjectId) &&
                        Objects.equals(selectedAssigneeId, nextAssigneeId) &&
                        Objects.equals(selectedStatus, nextStatus)) {
                    return;
                }
                selectedProjectId = nextProjectId;
                selectedAssigneeId = nextAssigneeId;
                selectedStatus = nextStatus;
                viewModel.setFilters(selectedProjectId, selectedAssigneeId, selectedStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        projectFilterSpinner.setOnItemSelectedListener(listener);
        assigneeFilterSpinner.setOnItemSelectedListener(listener);
        statusFilterSpinner.setOnItemSelectedListener(listener);
    }

    private void observe(View root) {
        viewModel.getSnapshot().observe(getViewLifecycleOwner(), snapshot -> display(root, snapshot));
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
        viewModel.getDeleted().observe(getViewLifecycleOwner(), deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                viewModel.clearDeleted();
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void display(View root, TeamWorkspaceSnapshot snapshot) {
        currentSnapshot = snapshot;
        projects = snapshot.getProjects();
        members = snapshot.getMembers();
        root.<TextView>findViewById(R.id.textWorkspaceName)
                .setText(snapshot.getWorkspace().getName());
        root.<TextView>findViewById(R.id.textWorkspaceDescription)
                .setText(snapshot.getWorkspace().getDescription());
        root.<TextView>findViewById(R.id.textCurrentTeamRole).setText(getString(
                R.string.current_role,
                formatRole(snapshot.getCurrentRole())));
        root.<TextView>findViewById(R.id.textTeamSummary).setText(getString(
                R.string.team_summary,
                snapshot.getTotalTasks(),
                snapshot.getCompletedTasks(),
                snapshot.getOverdueTasks()));

        memberAdapter.submitList(snapshot.getMembers());
        projectAdapter.submitList(snapshot.getProjects());
        taskAdapter.submitList(snapshot.getTasks());
        noProjects.setVisibility(projects.isEmpty() ? View.VISIBLE : View.GONE);
        noTasks.setVisibility(snapshot.getTasks().isEmpty() ? View.VISIBLE : View.GONE);

        boolean owner = TeamRules.canManageWorkspace(snapshot.getCurrentRole());
        boolean manager = TeamRules.canManageMembers(snapshot.getCurrentRole());
        root.findViewById(R.id.buttonEditTeam).setVisibility(owner ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.buttonDeleteTeam).setVisibility(owner ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.buttonAddMember).setVisibility(manager ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.buttonAddProject).setVisibility(manager ? View.VISIBLE : View.GONE);
        setupFilterOptions();
    }

    private void setupFilterOptions() {
        updatingFilters = true;
        List<String> projectNames = new ArrayList<>();
        projectNames.add(getString(R.string.all_projects));
        for (Project project : projects) {
            projectNames.add(project.getName());
        }
        List<String> memberNames = new ArrayList<>();
        memberNames.add(getString(R.string.all_members));
        for (WorkspaceMember member : members) {
            memberNames.add(member.getDisplayName());
        }
        setSpinner(projectFilterSpinner, projectNames);
        setSpinner(assigneeFilterSpinner, memberNames);
        setSpinner(statusFilterSpinner, List.of(
                getString(R.string.all_statuses),
                getString(R.string.status_todo),
                getString(R.string.status_in_progress),
                getString(R.string.status_completed),
                getString(R.string.status_cancelled)));
        projectFilterSpinner.setSelection(findProjectPosition(selectedProjectId));
        assigneeFilterSpinner.setSelection(findMemberPosition(selectedAssigneeId));
        statusFilterSpinner.setSelection(statusPosition(selectedStatus));
        updatingFilters = false;
    }

    private void setSpinner(Spinner spinner, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private int findProjectPosition(String projectId) {
        if (projectId == null) return 0;
        for (int i = 0; i < projects.size(); i++) {
            if (projectId.equals(projects.get(i).getProjectId())) return i + 1;
        }
        return 0;
    }

    private int findMemberPosition(String memberId) {
        if (memberId == null) return 0;
        for (int i = 0; i < members.size(); i++) {
            if (memberId.equals(members.get(i).getUserId())) return i + 1;
        }
        return 0;
    }

    private int statusPosition(String status) {
        if (TaskStatus.TODO.equals(status)) return 1;
        if (TaskStatus.IN_PROGRESS.equals(status)) return 2;
        if (TaskStatus.COMPLETED.equals(status)) return 3;
        if (TaskStatus.CANCELLED.equals(status)) return 4;
        return 0;
    }

    private void showEditTeamDialog() {
        if (currentSnapshot == null) return;
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_team_form, null, false);
        EditText name = content.findViewById(R.id.editTeamName);
        EditText description = content.findViewById(R.id.editTeamDescription);
        name.setText(currentSnapshot.getWorkspace().getName());
        description.setText(currentSnapshot.getWorkspace().getDescription());
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_team)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save_changes, (dialog, which) ->
                        viewModel.updateTeam(name.getText().toString(),
                                description.getText().toString()))
                .show();
    }

    private void confirmDeleteTeam() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.disband_team_question)
                .setMessage(R.string.disband_team_warning)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.disband_team, (dialog, which) -> viewModel.deleteTeam())
                .show();
    }

    private void showAddMemberDialog() {
        if (currentSnapshot == null) return;
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_team_member, null, false);
        EditText userCode = content.findViewById(R.id.editMemberCode);
        Spinner roleSpinner = content.findViewById(R.id.spinnerMemberRole);
        boolean owner = TeamRole.OWNER.equals(currentSnapshot.getCurrentRole());
        List<String> roles = owner
                ? List.of(getString(R.string.role_member), getString(R.string.role_admin))
                : List.of(getString(R.string.role_member));
        setSpinner(roleSpinner, roles);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_member)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    if (userCode.getText().toString().trim().isEmpty()) {
                        userCode.setError(getString(R.string.member_code_required));
                        return;
                    }
                    String role = owner && roleSpinner.getSelectedItemPosition() == 1
                            ? TeamRole.ADMIN : TeamRole.MEMBER;
                    viewModel.addMember(userCode.getText().toString(), role);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showMemberActions(WorkspaceMember member) {
        if (currentSnapshot == null || userId.equals(member.getUserId()) ||
                TeamRole.OWNER.equals(member.getRole())) {
            return;
        }
        String currentRole = currentSnapshot.getCurrentRole();
        if (TeamRole.OWNER.equals(currentRole)) {
            String roleAction = TeamRole.ADMIN.equals(member.getRole())
                    ? getString(R.string.set_as_member)
                    : getString(R.string.set_as_admin);
            new AlertDialog.Builder(requireContext())
                    .setTitle(member.getDisplayName())
                    .setItems(new String[]{roleAction, getString(R.string.remove_member)},
                            (dialog, which) -> {
                                if (which == 0) {
                                    viewModel.changeMemberRole(
                                            member.getUserId(),
                                            TeamRole.ADMIN.equals(member.getRole())
                                                    ? TeamRole.MEMBER : TeamRole.ADMIN);
                                } else {
                                    confirmRemoveMember(member);
                                }
                            })
                    .show();
        } else if (TeamRole.ADMIN.equals(currentRole) &&
                TeamRole.MEMBER.equals(member.getRole())) {
            confirmRemoveMember(member);
        }
    }

    private void confirmRemoveMember(WorkspaceMember member) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.remove_member)
                .setMessage(getString(R.string.remove_member_question, member.getDisplayName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, (dialog, which) ->
                        viewModel.removeMember(member.getUserId()))
                .show();
    }

    private void showAddProjectDialog() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_project_form, null, false);
        EditText name = content.findViewById(R.id.editProjectName);
        EditText description = content.findViewById(R.id.editProjectDescription);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_project)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.create, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    if (name.getText().toString().trim().isEmpty()) {
                        name.setError(getString(R.string.project_name_required));
                        return;
                    }
                    viewModel.createProject(name.getText().toString(),
                            description.getText().toString());
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void openTask(TeamTaskItem item) {
        openTaskForm(item.getTask().getTaskId());
    }

    private void openTaskForm(String taskId) {
        if (projects.isEmpty()) {
            Toast.makeText(requireContext(), R.string.create_project_first,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main, TeamTaskFormFragment.newInstance(
                        workspaceId, userId, taskId))
                .addToBackStack("team_task_form")
                .commit();
    }

    private void openDashboard() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main, TeamDashboardFragment.newInstance(
                        workspaceId, userId))
                .addToBackStack("team_dashboard")
                .commit();
    }

    private String formatRole(String role) {
        if (TeamRole.OWNER.equals(role)) return getString(R.string.role_owner);
        if (TeamRole.ADMIN.equals(role)) return getString(R.string.role_admin);
        return getString(R.string.role_member);
    }
}
