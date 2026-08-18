package com.vandieu_manhdung.taskmanager.data.reponsitory;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.data.local.dao.UserDao;
import com.vandieu_manhdung.taskmanager.data.remote.FirebaseProvider;
import com.vandieu_manhdung.taskmanager.model.User;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AuthRepository {

    private final Context context;
    private final AppExecutors executors;
    private final UserDao userDao;
    private final SharedPreferences preferences;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        executors = AppExecutors.getInstance();
        userDao = new UserDao(this.context);
        preferences = this.context.getSharedPreferences(
                "authenticated_user_state",
                Context.MODE_PRIVATE
        );
    }

    public boolean isConfigured() {
        return FirebaseProvider.isConfigured(context);
    }

    public boolean isSignedIn() {
        return isConfigured() && FirebaseProvider.auth(context).getCurrentUser() != null;
    }

    public void getCurrentUser(RepositoryCallback<User> callback) {
        if (!isConfigured()) {
            callback.onError(new IllegalStateException(
                    "Firebase chưa được cấu hình. Hãy thêm app/google-services.json."));
            return;
        }
        FirebaseUser firebaseUser = FirebaseProvider.auth(context).getCurrentUser();
        if (firebaseUser == null) {
            callback.onError(new IllegalStateException("Bạn chưa đăng nhập"));
            return;
        }
        persistAuthenticatedUser(firebaseUser, callback);
    }

    public void register(
            String displayName,
            String email,
            String password,
            RepositoryCallback<User> callback
    ) {
        String cleanName = requireText(displayName, "Vui lòng nhập tên hiển thị");
        String cleanEmail = validateEmail(email);
        validatePassword(password);
        FirebaseAuth auth = FirebaseProvider.auth(context);
        auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError(new IllegalStateException("Không thể tạo tài khoản"));
                        return;
                    }
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build();
                    firebaseUser.updateProfile(profile)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException() == null
                                            ? new IllegalStateException("Không thể lưu tên người dùng")
                                            : task.getException();
                                }
                                return firebaseUser.sendEmailVerification();
                            })
                            .addOnSuccessListener(ignored -> persistAuthenticatedUser(
                                    firebaseUser,
                                    new RepositoryCallback<User>() {
                                        @Override
                                        public void onSuccess(User user) {
                                            auth.signOut();
                                            callback.onSuccess(user);
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            callback.onError(exception);
                                        }
                                    }))
                            .addOnFailureListener(error -> callback.onError(mapError(error)));
                })
                .addOnFailureListener(error -> callback.onError(mapError(error)));
    }

    public void signIn(
            String email,
            String password,
            RepositoryCallback<User> callback
    ) {
        String cleanEmail = validateEmail(email);
        requireText(password, "Vui lòng nhập mật khẩu");
        FirebaseAuth auth = FirebaseProvider.auth(context);
        auth.signInWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError(new IllegalStateException("Không thể đăng nhập"));
                        return;
                    }
                    if (!firebaseUser.isEmailVerified()) {
                        firebaseUser.sendEmailVerification();
                        auth.signOut();
                        callback.onError(new IllegalStateException(
                                "Email chưa được xác minh. Chúng tôi đã gửi lại thư xác minh."));
                        return;
                    }
                    persistAuthenticatedUser(firebaseUser, callback);
                })
                .addOnFailureListener(error -> callback.onError(mapError(error)));
    }

    public void sendPasswordReset(
            String email,
            RepositoryCallback<Boolean> callback
    ) {
        FirebaseProvider.auth(context)
                .sendPasswordResetEmail(validateEmail(email))
                .addOnSuccessListener(ignored -> callback.onSuccess(true))
                .addOnFailureListener(error -> callback.onError(mapError(error)));
    }

    public void signOut() {
        if (isConfigured()) {
            FirebaseProvider.auth(context).signOut();
        }
    }

    private void persistAuthenticatedUser(
            FirebaseUser firebaseUser,
            RepositoryCallback<User> callback
    ) {
        long now = System.currentTimeMillis();
        User user = new User();
        user.setUserId(firebaseUser.getUid());
        String firebaseEmail = firebaseUser.getEmail();
        String safeEmail = firebaseEmail == null ? "" : firebaseEmail.trim();
        String fallbackName = safeEmail.contains("@")
                ? safeEmail.substring(0, safeEmail.indexOf('@'))
                : "Người dùng";
        user.setEmail(safeEmail);
        user.setDisplayName(firebaseUser.getDisplayName() == null ||
                firebaseUser.getDisplayName().isBlank()
                ? fallbackName
                : firebaseUser.getDisplayName().trim());
        user.setAvatarUrl(firebaseUser.getPhotoUrl() == null
                ? null : firebaseUser.getPhotoUrl().toString());
        user.setCreatedAt(firebaseUser.getMetadata() == null
                ? now : firebaseUser.getMetadata().getCreationTimestamp());
        user.setUpdatedAt(now);

        FirebaseFirestore firestore = FirebaseProvider.firestore(context);
        executors.database().execute(() -> {
            User localUser = userDao.findById(user.getUserId());
            executors.mainThread().execute(() -> {
                if (localUser != null && localUser.getUserCode() != null &&
                        !localUser.getUserCode().isBlank()) {
                    user.setUserCode(localUser.getUserCode());
                    if (preferences.getBoolean(
                            reservedCodeKey(user.getUserId()), false)) {
                        persistProfile(firestore, user, callback);
                    } else {
                        reserveUserCode(firestore, user, callback, 0);
                    }
                    return;
                }
                firestore.collection("users")
                        .document(user.getUserId())
                        .get()
                        .addOnSuccessListener(document -> {
                            String existingCode = document.getString("userCode");
                            if (existingCode != null && !existingCode.isBlank()) {
                                user.setUserCode(existingCode);
                                preferences.edit().putBoolean(
                                        reservedCodeKey(user.getUserId()), true).apply();
                                persistProfile(firestore, user, callback);
                            } else {
                                reserveUserCode(firestore, user, callback, 0);
                            }
                        })
                        .addOnFailureListener(error -> callback.onError(mapError(error)));
            });
        });
    }

    private void reserveUserCode(
            FirebaseFirestore firestore,
            User user,
            RepositoryCallback<User> callback,
            int attempt
    ) {
        if (attempt >= 5) {
            callback.onError(new IllegalStateException(
                    "Không thể cấp mã người dùng duy nhất. Vui lòng thử lại."));
            return;
        }
        String candidate = user.getUserCode();
        if (candidate == null || candidate.isBlank() || attempt > 0) {
            candidate = generateUserCode();
        }
        String finalCandidate = candidate.toUpperCase(Locale.ROOT);
        firestore.runTransaction(transaction -> {
                    var codeReference = firestore.collection("user_codes")
                            .document(finalCandidate);
                    var existing = transaction.get(codeReference);
                    if (existing.exists() && !user.getUserId().equals(
                            existing.getString("userId"))) {
                        throw new UserCodeCollisionException();
                    }
                    user.setUserCode(finalCandidate);
                    transaction.set(codeReference, directoryMap(user), SetOptions.merge());
                    transaction.set(
                            firestore.collection("users").document(user.getUserId()),
                            profileMap(user),
                            SetOptions.merge()
                    );
                    return finalCandidate;
                })
                .addOnSuccessListener(code -> {
                    preferences.edit().putBoolean(
                            reservedCodeKey(user.getUserId()), true).apply();
                    persistLocalUser(user, callback);
                })
                .addOnFailureListener(error -> {
                    if (hasCodeCollision(error)) {
                        user.setUserCode(null);
                        reserveUserCode(firestore, user, callback, attempt + 1);
                    } else {
                        callback.onError(mapError(error));
                    }
                });
    }

    private void persistProfile(
            FirebaseFirestore firestore,
            User user,
            RepositoryCallback<User> callback
    ) {
        var batch = firestore.batch();
        batch.set(
                firestore.collection("user_codes").document(user.getUserCode()),
                directoryMap(user),
                SetOptions.merge()
        );
        batch.set(
                firestore.collection("users").document(user.getUserId()),
                profileMap(user),
                SetOptions.merge()
        );
        batch.commit()
                .addOnSuccessListener(ignored -> persistLocalUser(user, callback))
                .addOnFailureListener(error -> callback.onError(mapError(error)));
    }

    private void persistLocalUser(User user, RepositoryCallback<User> callback) {
        executors.database().execute(() -> {
            try {
                if (!userDao.saveAuthenticatedUser(user)) {
                    throw new IllegalStateException("Không thể lưu tài khoản cục bộ");
                }
                executors.mainThread().execute(() -> callback.onSuccess(user));
            } catch (Exception exception) {
                executors.mainThread().execute(() -> callback.onError(exception));
            }
        });
    }

    private Map<String, Object> profileMap(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getUserId());
        profile.put("userCode", user.getUserCode());
        profile.put("normalizedUserCode", user.getUserCode().toUpperCase(Locale.ROOT));
        profile.put("email", user.getEmail());
        profile.put("normalizedEmail", user.getEmail().toLowerCase(Locale.ROOT));
        profile.put("displayName", user.getDisplayName());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("updatedAt", user.getUpdatedAt());
        return profile;
    }

    private Map<String, Object> directoryMap(User user) {
        Map<String, Object> directory = new HashMap<>();
        directory.put("userCode", user.getUserCode());
        directory.put("userId", user.getUserId());
        directory.put("displayName", user.getDisplayName());
        directory.put("email", user.getEmail());
        directory.put("updatedAt", System.currentTimeMillis());
        return directory;
    }

    private String generateUserCode() {
        return "USR-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    }

    private String reservedCodeKey(String userId) {
        return "reserved_code_" + userId;
    }

    private boolean hasCodeCollision(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof UserCodeCollisionException) return true;
            current = current.getCause();
        }
        return false;
    }

    private String validateEmail(String email) {
        String value = requireText(email, "Vui lòng nhập email").toLowerCase(Locale.ROOT);
        if (!value.contains("@") || value.startsWith("@") || value.endsWith("@")) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        return value;
    }

    private void validatePassword(String password) {
        String value = requireText(password, "Vui lòng nhập mật khẩu");
        if (value.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự");
        }
        boolean hasLetter = false;
        boolean hasNumber = false;
        for (char character : value.toCharArray()) {
            hasLetter |= Character.isLetter(character);
            hasNumber |= Character.isDigit(character);
        }
        if (!hasLetter || !hasNumber) {
            throw new IllegalArgumentException("Mật khẩu phải có cả chữ và số");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private Exception mapError(Exception error) {
        if (error instanceof FirebaseAuthException authError) {
            return switch (authError.getErrorCode()) {
                case "ERROR_EMAIL_ALREADY_IN_USE" ->
                        new IllegalStateException("Email này đã được đăng ký");
                case "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
                        new IllegalStateException("Email hoặc mật khẩu không đúng");
                case "ERROR_TOO_MANY_REQUESTS" ->
                        new IllegalStateException("Bạn thao tác quá nhiều lần. Vui lòng thử lại sau.");
                case "ERROR_NETWORK_REQUEST_FAILED" ->
                        new IllegalStateException("Không thể kết nối máy chủ. Hãy kiểm tra Internet.");
                default -> new IllegalStateException(
                        error.getMessage() == null ? "Xác thực thất bại" : error.getMessage());
            };
        }
        return error;
    }

    private static final class UserCodeCollisionException extends RuntimeException {
    }
}
