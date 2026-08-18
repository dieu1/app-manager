package com.vandieu_manhdung.taskmanager.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;

public class HomeFragment extends Fragment {

    private static final String ARG_NAME = "name";
    private static final String ARG_USER_CODE = "user_code";

    public static HomeFragment newInstance(
            String name,
            String userCode
    ) {
        HomeFragment fragment = new HomeFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_NAME, name);
        arguments.putString(ARG_USER_CODE, userCode);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = requireArguments();
        String name = arguments.getString(ARG_NAME, "");
        String userCode = arguments.getString(ARG_USER_CODE, "");
        ((TextView) view.findViewById(R.id.textHomeName)).setText(name);
        ((TextView) view.findViewById(R.id.textHomeUserCode)).setText(userCode);

        MainActivity activity = (MainActivity) requireActivity();
        view.findViewById(R.id.cardOpenPersonal).setOnClickListener(button ->
                activity.openPersonalTasks());
        view.findViewById(R.id.cardOpenTeams).setOnClickListener(button ->
                activity.openTeams());
        view.findViewById(R.id.buttonHomeProfile).setOnClickListener(button ->
                activity.openProfile());
        view.findViewById(R.id.buttonCopyUserCode).setOnClickListener(button -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Task Manager ID", userCode));
            Toast.makeText(requireContext(), R.string.user_code_copied, Toast.LENGTH_SHORT).show();
        });
    }
}
