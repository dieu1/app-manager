package com.vandieu_manhdung.taskmanager.ui.profile;

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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;

public class ProfileFragment extends Fragment {

    private static final String ARG_NAME = "name";
    private static final String ARG_EMAIL = "email";
    private static final String ARG_USER_CODE = "user_code";

    public static ProfileFragment newInstance(String name, String email, String userCode) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_NAME, name);
        arguments.putString(ARG_EMAIL, email);
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
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = requireArguments();
        String userCode = arguments.getString(ARG_USER_CODE, "");
        ((TextView) view.findViewById(R.id.textProfileName))
                .setText(arguments.getString(ARG_NAME, ""));
        ((TextView) view.findViewById(R.id.textProfileEmail))
                .setText(arguments.getString(ARG_EMAIL, ""));
        ((TextView) view.findViewById(R.id.textProfileUserCode)).setText(userCode);

        MainActivity activity = (MainActivity) requireActivity();
        view.findViewById(R.id.buttonProfileBack).setOnClickListener(button ->
                requireActivity().getSupportFragmentManager().popBackStack());
        view.findViewById(R.id.buttonProfileCopyCode).setOnClickListener(button -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Task Manager ID", userCode));
            Toast.makeText(requireContext(), R.string.user_code_copied, Toast.LENGTH_SHORT).show();
        });
        view.findViewById(R.id.buttonProfileSignOut).setOnClickListener(button ->
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.sign_out_question)
                        .setMessage(R.string.sign_out_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.sign_out, (dialog, which) -> activity.signOut())
                        .show());
    }
}
