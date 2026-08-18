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

public class RegisterFragment extends Fragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AuthViewModel viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        EditText displayName = view.findViewById(R.id.editRegisterDisplayName);
        EditText email = view.findViewById(R.id.editRegisterEmail);
        EditText password = view.findViewById(R.id.editRegisterPassword);
        EditText confirmation = view.findViewById(R.id.editRegisterPasswordConfirmation);
        ProgressBar loading = view.findViewById(R.id.progressRegister);

        view.findViewById(R.id.buttonRegisterBack).setOnClickListener(button ->
                ((MainActivity) requireActivity()).showLogin());
        view.findViewById(R.id.buttonCreateAccount).setOnClickListener(button ->
                viewModel.register(
                        text(displayName),
                        text(email),
                        text(password),
                        text(confirmation)
                ));

        viewModel.getLoading().observe(getViewLifecycleOwner(), value -> {
            boolean isLoading = Boolean.TRUE.equals(value);
            loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.buttonCreateAccount).setEnabled(!isLoading);
            view.findViewById(R.id.buttonRegisterBack).setEnabled(!isLoading);
        });
        viewModel.getRegistered().observe(getViewLifecycleOwner(), registered -> {
            if (Boolean.TRUE.equals(registered)) {
                Toast.makeText(
                        requireContext(),
                        R.string.registration_verification_sent,
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

    private String text(EditText editText) {
        return editText.getText().toString().trim();
    }
}
