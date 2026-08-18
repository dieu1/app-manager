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

public class AuthFragment extends Fragment {

    private AuthViewModel viewModel;
    private EditText email;
    private EditText password;
    private ProgressBar loading;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        email = view.findViewById(R.id.editAuthEmail);
        password = view.findViewById(R.id.editAuthPassword);
        loading = view.findViewById(R.id.progressAuth);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        view.findViewById(R.id.buttonSignIn).setOnClickListener(button ->
                viewModel.signIn(text(email), text(password)));
        view.findViewById(R.id.buttonOpenRegister).setOnClickListener(button ->
                ((MainActivity) requireActivity()).showRegister());
        view.findViewById(R.id.buttonOpenForgot).setOnClickListener(button ->
                ((MainActivity) requireActivity()).showForgotPassword());

        viewModel.getLoading().observe(getViewLifecycleOwner(), value -> {
            boolean isLoading = Boolean.TRUE.equals(value);
            loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.buttonSignIn).setEnabled(!isLoading);
            view.findViewById(R.id.buttonOpenRegister).setEnabled(!isLoading);
            view.findViewById(R.id.buttonOpenForgot).setEnabled(!isLoading);
        });
        viewModel.getAuthenticatedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                viewModel.clearEvents();
                ((MainActivity) requireActivity()).onAuthenticated(user);
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
