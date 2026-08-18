package com.vandieu_manhdung.taskmanager.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;

public class BackendSetupFragment extends Fragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_backend_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.buttonRetryFirebase).setOnClickListener(
                button -> ((MainActivity) requireActivity()).retryFirebaseSetup());
    }
}
