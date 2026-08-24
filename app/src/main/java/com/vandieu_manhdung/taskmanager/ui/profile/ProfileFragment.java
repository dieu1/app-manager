package com.vandieu_manhdung.taskmanager.ui.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.repository.AuthRepository;
import com.vandieu_manhdung.taskmanager.model.User;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String ARG_NAME = "name";
    private static final String ARG_EMAIL = "email";
    private static final String ARG_USER_CODE = "user_code";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_AVATAR_URL = "avatar_url";
    private String currentName;
    private TextView avatarInitials;
    private TextView nameView;

    public static ProfileFragment newInstance(
            String userId, String name, String email, String userCode, String avatarUrl) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_NAME, name);
        arguments.putString(ARG_EMAIL, email);
        arguments.putString(ARG_USER_CODE, userCode);
        arguments.putString(ARG_USER_ID, userId);
        arguments.putString(ARG_AVATAR_URL, avatarUrl);
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
        currentName = arguments.getString(ARG_NAME, "");
        nameView = view.findViewById(R.id.textProfileName);
        nameView.setText(currentName);
        avatarInitials = view.findViewById(R.id.textProfileAvatarInitials);
        renderInitials();
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
        view.findViewById(R.id.buttonProfileEdit).setOnClickListener(button -> showEditDialog());
        view.findViewById(R.id.buttonProfileNotifications).setOnClickListener(button ->
                activity.openNotifications());
        view.findViewById(R.id.buttonProfileTrash).setOnClickListener(button ->
                activity.openTaskTrash());
        view.findViewById(R.id.buttonProfileSignOut).setOnClickListener(button ->
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.sign_out_question)
                        .setMessage(R.string.sign_out_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.sign_out, (dialog, which) -> activity.signOut())
                        .show());
    }

    private void showEditDialog() {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, 0);
        EditText nameInput = new EditText(requireContext());
        nameInput.setHint(R.string.display_name);
        nameInput.setText(currentName);
        content.addView(nameInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_profile)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    new AuthRepository(requireContext()).updateProfile(
                            nameInput.getText().toString(), null,
                            new RepositoryCallback<User>() {
                                @Override public void onSuccess(User user) {
                                    currentName = user.getDisplayName();
                                    nameView.setText(currentName);
                                    renderInitials();
                                    ((MainActivity) requireActivity()).onProfileUpdated(user);
                                    dialog.dismiss();
                                    Toast.makeText(requireContext(), R.string.profile_updated,
                                            Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onError(Exception exception) {
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    Toast.makeText(requireContext(), exception.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }));
        dialog.show();
    }

    private void renderInitials() {
        if (avatarInitials == null) return;
        String clean = currentName == null ? "" : currentName.trim();
        if (clean.isEmpty()) {
            avatarInitials.setText(R.string.profile_fallback_initials);
            return;
        }
        String[] parts = clean.split("\\s+");
        String value = parts[0].substring(0, 1);
        if (parts.length > 1) value += parts[parts.length - 1].substring(0, 1);
        avatarInitials.setText(value.toUpperCase(Locale.getDefault()));
    }
}
