package com.vandieu_manhdung.taskmanager.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceStatus;
import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceType;
import com.vandieu_manhdung.taskmanager.data.local.dao.UserDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.WorkspaceDao;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.User;
import com.vandieu_manhdung.taskmanager.model.Workspace;

import java.util.Locale;
import java.util.UUID;

public class WorkspaceRepository {

    private final TaskManagerDatabaseHelper databaseHelper;
    private final UserDao userDao;
    private final WorkspaceDao workspaceDao;

    public WorkspaceRepository(Context context) {
        Context applicationContext = context.getApplicationContext();

        databaseHelper =
                TaskManagerDatabaseHelper.getInstance(applicationContext);

        userDao = new UserDao(applicationContext);
        workspaceDao = new WorkspaceDao(applicationContext);
    }

    public Workspace initializePersonalWorkspace(User user) {
        validateUser(user);

        String stableWorkspaceId = personalWorkspaceId(user.getUserId());
        Workspace existingBeforeTransaction =
                workspaceDao.findPersonalWorkspace(user.getUserId());
        if (existingBeforeTransaction != null) {
            prepareUser(user);
            if (!userDao.save(user)) {
                throw new IllegalStateException("Không thể lưu người dùng cục bộ");
            }
            return workspaceDao.renameWorkspace(
                    existingBeforeTransaction,
                    stableWorkspaceId
            );
        }

        SQLiteDatabase database =
                databaseHelper.getWritableDatabase();

        database.beginTransaction();

        try {
            prepareUser(user);

            boolean saved = userDao.save(user);

            if (!saved) {
                throw new IllegalStateException(
                        "Không thể lưu người dùng cục bộ"
                );
            }

            Workspace newWorkspace = createPersonalWorkspace(user);
            newWorkspace.setWorkspaceId(stableWorkspaceId);

            boolean inserted =
                    workspaceDao.insert(newWorkspace);

            if (!inserted) {
                throw new IllegalStateException(
                        "Không thể tạo Workspace cá nhân"
                );
            }

            database.setTransactionSuccessful();
            return newWorkspace;

        } finally {
            database.endTransaction();
        }
    }

    private String personalWorkspaceId(String userId) {
        return "personal_" + userId;
    }

    public Workspace getPersonalWorkspace(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        return workspaceDao.findPersonalWorkspace(userId);
    }

    private void prepareUser(User user) {
        long currentTime = System.currentTimeMillis();

        if (user.getUserCode() == null ||
                user.getUserCode().isBlank()) {
            user.setUserCode(generateUniqueUserCode());
        }

        if (user.getCreatedAt() <= 0) {
            user.setCreatedAt(currentTime);
        }

        user.setUpdatedAt(currentTime);
    }

    private Workspace createPersonalWorkspace(User user) {
        long currentTime = System.currentTimeMillis();

        Workspace workspace = new Workspace();

        workspace.setWorkspaceId(
                UUID.randomUUID().toString()
        );

        workspace.setManagerId(user.getUserId());
        workspace.setName("Công việc cá nhân");
        workspace.setType(WorkspaceType.PERSONAL);

        workspace.setDescription(
                "Không gian quản lý công việc cá nhân"
        );

        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setCreatedAt(currentTime);
        workspace.setUpdatedAt(currentTime);

        return workspace;
    }

    private String generateUniqueUserCode() {
        String userCode;

        do {
            String randomPart = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase(Locale.ROOT);

            userCode = "USR-" + randomPart;

        } while (userDao.existsByUserCode(userCode));

        return userCode;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "Thông tin người dùng không được để trống"
            );
        }

        if (user.getUserId() == null ||
                user.getUserId().isBlank()) {
            throw new IllegalArgumentException(
                    "User ID không được để trống"
            );
        }

        if (user.getEmail() == null ||
                user.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Email không được để trống"
            );
        }

        if (user.getDisplayName() == null ||
                user.getDisplayName().isBlank()) {
            throw new IllegalArgumentException(
                    "Tên người dùng không được để trống"
            );
        }
    }
}
