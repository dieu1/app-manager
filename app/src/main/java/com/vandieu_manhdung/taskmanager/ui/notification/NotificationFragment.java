package com.vandieu_manhdung.taskmanager.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.data.local.dao.NotificationDao;
import com.vandieu_manhdung.taskmanager.model.AppNotification;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;

import java.util.List;

public class NotificationFragment extends Fragment {
    private static final String ARG_USER_ID = "user_id";
    private String userId;
    private NotificationAdapter adapter;
    private TextView empty;

    public static NotificationFragment newInstance(String userId) {
        NotificationFragment fragment = new NotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                      Bundle state) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        userId = requireArguments().getString(ARG_USER_ID, "");
        empty = view.findViewById(R.id.textNotificationsEmpty);
        RecyclerView recycler = view.findViewById(R.id.recyclerNotifications);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationAdapter(this::openNotification);
        recycler.setAdapter(adapter);
        view.findViewById(R.id.buttonNotificationsBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        view.findViewById(R.id.buttonMarkAllRead).setOnClickListener(v -> new Thread(() -> {
            new NotificationDao(requireContext()).markAllRead(userId);
            requireActivity().runOnUiThread(this::load);
        }).start());
        load();
    }

    private void load() {
        new Thread(() -> {
            List<AppNotification> values = new NotificationDao(requireContext()).findByUser(userId);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                adapter.submit(values);
                empty.setVisibility(values.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void openNotification(AppNotification item) {
        new Thread(() -> new NotificationDao(requireContext()).markRead(item.getNotificationId())).start();
        if (item.getTaskId() != null && !item.getTaskId().isBlank()) {
            ((MainActivity) requireActivity()).openTaskDetail(item.getTaskId());
        } else {
            load();
        }
    }
}
