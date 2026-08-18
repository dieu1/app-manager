package com.vandieu_manhdung.taskmanager.ui.personal.task;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.core.constant.TaskPriority;
import com.vandieu_manhdung.taskmanager.core.constant.TaskSortOption;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.ui.personal.dashboard.PersonalDashboardFragment;
import com.vandieu_manhdung.taskmanager.ui.personal.task.detail.TaskDetailFragment;
import com.vandieu_manhdung.taskmanager.ui.personal.task.form.TaskFormFragment;
import com.vandieu_manhdung.taskmanager.ui.team.TeamListFragment;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;

import java.util.List;

public class PersonalTaskListFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "workspace_id";
    private static final String ARG_USER_ID = "user_id";

    private PersonalTaskListViewModel viewModel;
    private PersonalTaskAdapter adapter;

    private String workspaceId;
    private String currentUserId;

    private RecyclerView recyclerTasks;
    private ProgressBar progressLoading;
    private TextView textEmpty;

    private final Handler searchHandler =
            new Handler(Looper.getMainLooper());

    private Runnable searchRunnable;

    public static PersonalTaskListFragment newInstance(
            String workspaceId,
            String userId
    ) {
        PersonalTaskListFragment fragment =
                new PersonalTaskListFragment();

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
        return inflater.inflate(
                R.layout.fragment_personal_task_list,
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

        getArgumentsData();
        bindViews(view);
        setupRecyclerView();
        setupViewModel();
        setupSearch(view);
        setupFilters(view);
        observeViewModel();

        Button buttonRefresh =
                view.findViewById(R.id.buttonRefreshTasks);

        Button buttonAdd =
                view.findViewById(R.id.buttonAddTask);

        Button buttonStatistics =
                view.findViewById(R.id.buttonPersonalStatistics);

        Button buttonTeams =
                view.findViewById(R.id.buttonOpenTeams);

        buttonRefresh.setOnClickListener(
                button -> viewModel.loadTasks()
        );

        buttonAdd.setOnClickListener(
                button -> openTaskForm()
        );

        buttonStatistics.setOnClickListener(
                button -> openDashboard()
        );

        buttonTeams.setOnClickListener(
                button -> openTeams()
        );

        view.findViewById(R.id.buttonPersonalHome).setOnClickListener(
                button -> ((MainActivity) requireActivity()).openHome()
        );

        view.findViewById(R.id.buttonSignOut).setOnClickListener(
                button -> confirmSignOut()
        );

        viewModel.setWorkspaceId(workspaceId);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (viewModel != null) {
            viewModel.loadTasks();
        }
    }

    private void getArgumentsData() {
        if (getArguments() == null) {
            return;
        }

        workspaceId = getArguments().getString(
                ARG_WORKSPACE_ID
        );

        currentUserId = getArguments().getString(
                ARG_USER_ID
        );
    }

    private void bindViews(View view) {
        recyclerTasks = view.findViewById(
                R.id.recyclerPersonalTasks
        );

        progressLoading = view.findViewById(
                R.id.progressLoadingTasks
        );

        textEmpty = view.findViewById(
                R.id.textEmptyTasks
        );
    }

    private void setupRecyclerView() {
        adapter = new PersonalTaskAdapter(
                this::onTaskClicked
        );

        recyclerTasks.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerTasks.setAdapter(adapter);
        recyclerTasks.setHasFixedSize(true);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(PersonalTaskListViewModel.class);
    }

    private void setupSearch(View view) {
        SearchView searchView = view.findViewById(
                R.id.searchPersonalTasks
        );

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(
                            String query
                    ) {
                        viewModel.setKeyword(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {
                        if (searchRunnable != null) {
                            searchHandler.removeCallbacks(
                                    searchRunnable
                            );
                        }

                        searchRunnable = () ->
                                viewModel.setKeyword(newText);

                        searchHandler.postDelayed(
                                searchRunnable,
                                300
                        );

                        return true;
                    }
                }
        );
    }

    private void setupFilters(View view) {
        Spinner statusSpinner = view.findViewById(
                R.id.spinnerTaskStatusFilter
        );

        Spinner prioritySpinner = view.findViewById(
                R.id.spinnerTaskPriorityFilter
        );

        Spinner sortSpinner = view.findViewById(
                R.id.spinnerTaskSort
        );

        CheckBox ascendingCheckBox = view.findViewById(
                R.id.checkSortAscending
        );

        String[] statusItems = {
                "Tất cả trạng thái",
                "Chưa xử lý",
                "Đang xử lý",
                "Đã xử lý",
                "Không xử lý"
        };

        String[] priorityItems = {
                "Tất cả ưu tiên",
                "Thấp",
                "Vừa",
                "Cao",
                "Khẩn cấp"
        };

        String[] sortItems = {
                "Ngày tạo",
                "Hạn hoàn thành",
                "Độ ưu tiên",
                "Tiến độ",
                "Tên công việc"
        };

        setSpinnerAdapter(statusSpinner, statusItems);
        setSpinnerAdapter(prioritySpinner, priorityItems);
        setSpinnerAdapter(sortSpinner, sortItems);

        statusSpinner.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id
                    ) {
                        String status = switch (position) {
                            case 1 -> TaskStatus.TODO;
                            case 2 -> TaskStatus.IN_PROGRESS;
                            case 3 -> TaskStatus.COMPLETED;
                            case 4 -> TaskStatus.CANCELLED;
                            default -> null;
                        };

                        viewModel.setStatusFilter(status);
                    }
                }
        );

        prioritySpinner.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id
                    ) {
                        String priority = switch (position) {
                            case 1 -> TaskPriority.LOW;
                            case 2 -> TaskPriority.MEDIUM;
                            case 3 -> TaskPriority.HIGH;
                            case 4 -> TaskPriority.URGENT;
                            default -> null;
                        };

                        viewModel.setPriorityFilter(priority);
                    }
                }
        );

        sortSpinner.setOnItemSelectedListener(
                new SimpleItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id
                    ) {
                        String sortOption = switch (position) {
                            case 1 -> TaskSortOption.DUE_DATE;
                            case 2 -> TaskSortOption.PRIORITY;
                            case 3 -> TaskSortOption.PROGRESS;
                            case 4 -> TaskSortOption.TITLE;
                            default -> TaskSortOption.CREATED_AT;
                        };

                        viewModel.setSortOption(sortOption);
                    }
                }
        );

        ascendingCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        viewModel.setAscending(isChecked)
        );
    }

    private void setSpinnerAdapter(
            Spinner spinner,
            String[] items
    ) {
        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        items
                );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(spinnerAdapter);
    }

    private void observeViewModel() {
        viewModel.getTasks().observe(
                getViewLifecycleOwner(),
                this::displayTasks
        );

        viewModel.getLoading().observe(
                getViewLifecycleOwner(),
                loading -> progressLoading.setVisibility(
                        Boolean.TRUE.equals(loading)
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
    }

    private void displayTasks(List<Task> tasks) {
        adapter.submitList(tasks);

        boolean empty =
                tasks == null || tasks.isEmpty();

        textEmpty.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );

        recyclerTasks.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );
    }

    private void openTaskForm() {
        if (workspaceId == null ||
                currentUserId == null) {
            Toast.makeText(
                    requireContext(),
                    "Thiếu thông tin người dùng",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.main,
                        TaskFormFragment.newInstance(
                                workspaceId,
                                currentUserId
                        )
                )
                .addToBackStack("task_form")
                .commit();
    }

    private void onTaskClicked(Task task) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.main,
                        TaskDetailFragment.newInstance(
                                workspaceId,
                                currentUserId,
                                task.getTaskId()
                        )
                )
                .addToBackStack("task_detail")
                .commit();
    }

    private void openDashboard() {
        getParentFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.main,
                        PersonalDashboardFragment.newInstance(workspaceId)
                )
                .addToBackStack("personal_dashboard")
                .commit();
    }

    private void openTeams() {
        getParentFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.main,
                        TeamListFragment.newInstance(currentUserId)
                )
                .addToBackStack("team_list")
                .commit();
    }

    private void confirmSignOut() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out_question)
                .setMessage(R.string.sign_out_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.sign_out, (dialog, which) ->
                        ((MainActivity) requireActivity()).signOut())
                .show();
    }

    @Override
    public void onDestroyView() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        super.onDestroyView();
    }

    private abstract static class
    SimpleItemSelectedListener
            implements AdapterView.OnItemSelectedListener {

        @Override
        public void onNothingSelected(
                AdapterView<?> parent
        ) {
        }
    }
}
