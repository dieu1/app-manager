package com.vandieu_manhdung.taskmanager.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;

public class ForgotPasswordFragment extends Fragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AuthViewModel viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        EditText email = view.findViewById(R.id.editForgotEmail);
        ProgressBar loading = view.findViewById(R.id.progressForgot);

        view.findViewById(R.id.buttonForgotBack).setOnClickListener(button ->
                ((MainActivity) requireActivity()).showLogin());
        view.findViewById(R.id.buttonSendReset).setOnClickListener(button ->
                viewModel.resetPassword(email.getText().toString().trim()));

        viewModel.getLoading().observe(getViewLifecycleOwner(), value -> {
            boolean isLoading = Boolean.TRUE.equals(value);
            loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.buttonSendReset).setEnabled(!isLoading);
            view.findViewById(R.id.buttonForgotBack).setEnabled(!isLoading);
        });
        viewModel.getResetSent().observe(getViewLifecycleOwner(), sent -> {
            if (Boolean.TRUE.equals(sent)) {
                Toast.makeText(
                        requireContext(),
                        R.string.password_reset_sent,
                        Toast.LENGTH_LONG
                ).show();
                viewModel.clearEvents();
                ((MainActivity) requireActivity()).showLogin();
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                viewModel.clearEvents();
            }
        });
    }
}
