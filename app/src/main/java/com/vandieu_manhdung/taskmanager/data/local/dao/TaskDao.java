package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.core.constant.TaskSortOption;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.constant.SyncStatus;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskDao {

    private final TaskManagerDatabaseHelper databaseHelper;

    public TaskDao(Context context) {
        databaseHelper =
                TaskManagerDatabaseHelper.getInstance(context);
    }

    /*
     * =========================================================
     * THÊM, SỬA, XÓA
     * =========================================================
     */

    public boolean insert(Task task) {
        SQLiteDatabase database =
                databaseHelper.getWritableDatabase();

        long result = database.insert(
                TaskTable.TABLE_NAME,
                null,
                toContentValues(task)
        );

        return result != -1;
    }

    public boolean save(Task task) {
        return existsById(task.getTaskId())
                ? update(task) > 0
                : insert(task);
    }

    public int update(Task task) {
        SQLiteDatabase database =
                databaseHelper.getWritableDatabase();

        return database.update(
                TaskTable.TABLE_NAME,
                toContentValues(task),
                TaskTable.TASK_ID + " = ?",
                new String[]{task.getTaskId()}
        );
    }

    public int updateStatusAndProgress(
            String taskId,
            String status,
            int progress
    ) {
        SQLiteDatabase database =
                databaseHelper.getWritableDatabase();

        Task existing = findByIdIncludingDeleted(taskId);
        ContentValues values = new ContentValues();

        values.put(TaskTable.STATUS, status);
        values.put(TaskTable.PROGRESS, progress);
        values.put(
                TaskTable.UPDATED_AT,
                System.currentTimeMillis()
        );
        values.put(TaskTable.VERSION, existing == null ? 1 : existing.getVersion() + 1);
        values.put(TaskTable.SYNC_STATUS, SyncStatus.PENDING);

        return database.update(
                TaskTable.TABLE_NAME,
                values,
                TaskTable.TASK_ID + " = ?",
                new String[]{taskId}
        );
    }

    public int updateStatusAndProgressFromRemote(String taskId, String status, int progress) {
        ContentValues values = new ContentValues();
        values.put(TaskTable.STATUS, status);
        values.put(TaskTable.PROGRESS, progress);
        return databaseHelper.getWritableDatabase().update(
                TaskTable.TABLE_NAME, values, TaskTable.TASK_ID + " = ?",
                new String[]{taskId});
    }

    public int softDelete(String taskId, long deletedAt, int version) {
        ContentValues values = new ContentValues();
        values.put(TaskTable.DELETED_AT, deletedAt);
        values.put(TaskTable.UPDATED_AT, deletedAt);
        values.put(TaskTable.VERSION, Math.max(1, version));
        values.put(TaskTable.SYNC_STATUS, SyncStatus.PENDING);
        return databaseHelper.getWritableDatabase().update(
                TaskTable.TABLE_NAME, values, TaskTable.TASK_ID + " = ?",
                new String[]{taskId});
    }

    public int restore(String taskId, long updatedAt, int version) {
        ContentValues values = new ContentValues();
        values.putNull(TaskTable.DELETED_AT);
        values.put(TaskTable.UPDATED_AT, updatedAt);
        values.put(TaskTable.VERSION, Math.max(1, version));
        values.put(TaskTable.SYNC_STATUS, SyncStatus.PENDING);
        return databaseHelper.getWritableDatabase().update(
                TaskTable.TABLE_NAME, values, TaskTable.TASK_ID + " = ?",
                new String[]{taskId});
    }

    public void markSyncStatus(String taskId, int version, String status) {
        ContentValues values = new ContentValues();
        values.put(TaskTable.SYNC_STATUS, status);
        databaseHelper.getWritableDatabase().update(
                TaskTable.TABLE_NAME, values,
                TaskTable.TASK_ID + " = ? AND " + TaskTable.VERSION + " = ?",
                new String[]{taskId, String.valueOf(version)});
    }

    public int delete(String taskId) {
        SQLiteDatabase database =
                databaseHelper.getWritableDatabase();

        return database.delete(
                TaskTable.TABLE_NAME,
                TaskTable.TASK_ID + " = ?",
                new String[]{taskId}
        );
    }

    /*
     * =========================================================
     * TÌM THEO ID
     * =========================================================
     */

    public Task findById(String taskId) {
        Task task = findByIdIncludingDeleted(taskId);
        return task != null && task.getDeletedAt() <= 0 ? task : null;
    }

    public Task findByIdIncludingDeleted(String taskId) {
        SQLiteDatabase database =
                databaseHelper.getReadableDatabase();

        try (Cursor cursor = database.query(
                TaskTable.TABLE_NAME,
                null,
                TaskTable.TASK_ID + " = ?",
                new String[]{taskId},
                null,
                null,
                null,
                "1"
        )) {
            if (cursor.moveToFirst()) {
                return mapCursorToTask(cursor);
            }
        }

        return null;
    }

    public boolean existsById(String taskId) {
        SQLiteDatabase database =
                databaseHelper.getReadableDatabase();

        String sql =
                "SELECT 1 FROM " + TaskTable.TABLE_NAME +
                        " WHERE " + TaskTable.TASK_ID +
                        " = ? LIMIT 1";

        try (Cursor cursor = database.rawQuery(
                sql,
                new String[]{taskId}
        )) {
            return cursor.moveToFirst();
        }
    }

    /*
     * =========================================================
     * DANH SÁCH CÔNG VIỆC CÁ NHÂN
     * =========================================================
     */

    public List<Task> findAllPersonalTasks(
            String workspaceId
    ) {
        return queryPersonalTasks(
                workspaceId,
                null,
                null,
                null,
                TaskSortOption.CREATED_AT,
                false
        );
    }

    public List<Task> searchPersonalTasks(
            String workspaceId,
            String keyword
    ) {
        return queryPersonalTasks(
                workspaceId,
                keyword,
                null,
                null,
                TaskSortOption.CREATED_AT,
                false
        );
    }

    public List<Task> filterPersonalTasks(
            String workspaceId,
            String status,
            String priority,
            String sortOption,
            boolean ascending
    ) {
        return queryPersonalTasks(
                workspaceId,
                null,
                status,
                priority,
                sortOption,
                ascending
        );
    }

    /*
     * Đây là phương thức chính để tìm kiếm, lọc và sắp xếp.
     *
     * keyword  = null: không tìm kiếm
     * status   = null: lấy tất cả trạng thái
     * priority = null: lấy tất cả độ ưu tiên
     */
    public List<Task> queryPersonalTasks(
            String workspaceId,
            String keyword,
            String status,
            String priority,
            String sortOption,
            boolean ascending
    ) {
        List<Task> tasks = new ArrayList<>();

        SQLiteDatabase database =
                databaseHelper.getReadableDatabase();

        StringBuilder selection = new StringBuilder();

        List<String> arguments = new ArrayList<>();

        // Chỉ lấy công việc thuộc Workspace hiện tại.
        selection.append(TaskTable.WORKSPACE_ID)
                .append(" = ?");

        arguments.add(workspaceId);

        selection.append(" AND (")
                .append(TaskTable.DELETED_AT)
                .append(" IS NULL OR ")
                .append(TaskTable.DELETED_AT)
                .append(" = 0)");

        // Công việc cá nhân không thuộc dự án Team.
        selection.append(" AND ")
                .append(TaskTable.PROJECT_ID)
                .append(" IS NULL");

        if (keyword != null && !keyword.trim().isEmpty()) {
            selection.append(" AND (")
                    .append(TaskTable.TITLE)
                    .append(" LIKE ? OR ")
                    .append(TaskTable.DESCRIPTION)
                    .append(" LIKE ?)");

            String searchValue =
                    "%" + keyword.trim() + "%";

            arguments.add(searchValue);
            arguments.add(searchValue);
        }

        if (status != null &&
                !status.trim().isEmpty() &&
                !"ALL".equalsIgnoreCase(status)) {
            selection.append(" AND ")
                    .append(TaskTable.STATUS)
                    .append(" = ?");

            arguments.add(status);
        }

        if (priority != null &&
                !priority.trim().isEmpty() &&
                !"ALL".equalsIgnoreCase(priority)) {
            selection.append(" AND ")
                    .append(TaskTable.PRIORITY)
                    .append(" = ?");

            arguments.add(priority);
        }

        String orderBy =
                buildOrderBy(sortOption, ascending);

        try (Cursor cursor = database.query(
                TaskTable.TABLE_NAME,
                null,
                selection.toString(),
                arguments.toArray(new String[0]),
                null,
                null,
                orderBy
        )) {
            while (cursor.moveToNext()) {
                tasks.add(mapCursorToTask(cursor));
            }
        }

        return tasks;
    }

    /*
     * =========================================================
     * CÔNG VIỆC QUÁ HẠN
     * =========================================================
     */

    public List<Task> findOverduePersonalTasks(
            String workspaceId,
            long currentTime
    ) {
        List<Task> tasks = new ArrayList<>();

        SQLiteDatabase database =
                databaseHelper.getReadableDatabase();

        String selection =
                TaskTable.WORKSPACE_ID + " = ? AND " +
                        "(" + TaskTable.DELETED_AT + " IS NULL OR " +
                        TaskTable.DELETED_AT + " = 0) AND " +
                        TaskTable.PROJECT_ID + " IS NULL AND " +
                        TaskTable.DUE_DATE + " IS NOT NULL AND " +
                        TaskTable.DUE_DATE + " > 0 AND " +
                        TaskTable.DUE_DATE + " < ? AND " +
                        TaskTable.STATUS + " != ? AND " +
                        TaskTable.STATUS + " != ?";

        String[] selectionArgs = {
                workspaceId,
                String.valueOf(currentTime),
                TaskStatus.COMPLETED,
                TaskStatus.CANCELLED
        };

        try (Cursor cursor = database.query(
                TaskTable.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                TaskTable.DUE_DATE + " ASC"
        )) {
            while (cursor.moveToNext()) {
                tasks.add(mapCursorToTask(cursor));
            }
        }

        return tasks;
    }

    /*
     * =========================================================
     * THỐNG KÊ
     * =========================================================
     */

    public int countPersonalTasks(String workspaceId) {
        SQLiteDatabase database =
                databaseHelper.getReadableDatabase();

        String sql =
                "SELECT COUNT(*) FROM " +
                        TaskTable.TABLE_NAME +
                        " WHERE " + TaskTable.WORKSPACE_ID +
                        " = ? AND (" + TaskTable.DELETED_AT + " IS NULL OR " +
                        TaskTable.DELETED_AT + " = 0) AND " + TaskTable.PROJECT_ID +
                        " IS NULL";

        try (Cursor cursor = database.rawQuery(
                sql,
                new String[]{workspaceId}
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    public int countPersonalTasksByStatus(
            String workspaceId,
            String status
    ) {
        SQLiteDatabase database =
                databaseHelper.getReadableDatabase();

        String sql =
                "SELECT COUNT(*) FROM " +
                        TaskTable.TABLE_NAME +
                        " WHERE " + TaskTable.WORKSPACE_ID +
                        " = ? AND " +
                        "(" + TaskTable.DELETED_AT + " IS NULL OR " +
                        TaskTable.DELETED_AT + " = 0) AND " +
                        TaskTable.PROJECT_ID +
                        " IS NULL AND " +
                        TaskTable.STATUS + " = ?";

        try (Cursor cursor = database.rawQuery(
                sql,
                new String[]{workspaceId, status}
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    public int countOverduePersonalTasks(
            String workspaceId,
            long currentTime
    ) {
        SQLiteDatabase database =
                databaseHelper.getReadableDatabase();

        String sql =
                "SELECT COUNT(*) FROM " +
                        TaskTable.TABLE_NAME +
                        " WHERE " +
                        TaskTable.WORKSPACE_ID + " = ? AND " +
                        "(" + TaskTable.DELETED_AT + " IS NULL OR " +
                        TaskTable.DELETED_AT + " = 0) AND " +
                        TaskTable.PROJECT_ID + " IS NULL AND " +
                        TaskTable.DUE_DATE + " IS NOT NULL AND " +
                        TaskTable.DUE_DATE + " > 0 AND " +
                        TaskTable.DUE_DATE + " < ? AND " +
                        TaskTable.STATUS + " != ? AND " +
                        TaskTable.STATUS + " != ?";

        String[] arguments = {
                workspaceId,
                String.valueOf(currentTime),
                TaskStatus.COMPLETED,
                TaskStatus.CANCELLED
        };

        try (Cursor cursor = database.rawQuery(
                sql,
                arguments
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    public List<Task> findAllScheduledTasks(long now) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        List<Task> result = new ArrayList<>();
        String selection = "(" + TaskTable.PROJECT_ID + " IS NULL OR " +
                TaskTable.PROJECT_ID + " = '') AND (" +
                TaskTable.START_DATE + " > ? OR " +
                TaskTable.DUE_DATE + " > ?) AND " +
                "(" + TaskTable.DELETED_AT + " IS NULL OR " +
                TaskTable.DELETED_AT + " = 0) AND " +
                TaskTable.STATUS + " NOT IN (?, ?)";
        try (Cursor cursor = database.query(
                TaskTable.TABLE_NAME,
                null,
                selection,
                new String[]{
                        String.valueOf(now),
                        String.valueOf(now),
                        TaskStatus.COMPLETED,
                        TaskStatus.CANCELLED
                },
                null,
                null,
                TaskTable.START_DATE + " ASC"
        )) {
            while (cursor.moveToNext()) {
                result.add(mapCursorToTask(cursor));
            }
        }
        return result;
    }

    public List<Task> findAllReminderTasks() {
        List<Task> result = new ArrayList<>();
        String selection = "(" + TaskTable.START_DATE + " > 0 OR " +
                TaskTable.DUE_DATE + " > 0) AND (" + TaskTable.DELETED_AT + " IS NULL OR " +
                TaskTable.DELETED_AT + " = 0) AND " + TaskTable.STATUS + " NOT IN (?, ?)";
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TaskTable.TABLE_NAME, null, selection,
                new String[]{TaskStatus.COMPLETED, TaskStatus.CANCELLED},
                null, null, TaskTable.DUE_DATE + " ASC")) {
            while (cursor.moveToNext()) result.add(mapCursorToTask(cursor));
        }
        return result;
    }

    public int averagePersonalProgress(String workspaceId) {
        String sql = "SELECT COALESCE(ROUND(AVG(" + TaskTable.PROGRESS + ")), 0) " +
                "FROM " + TaskTable.TABLE_NAME +
                " WHERE " + TaskTable.WORKSPACE_ID + " = ? AND " +
                "(" + TaskTable.DELETED_AT + " IS NULL OR " +
                TaskTable.DELETED_AT + " = 0) AND " +
                TaskTable.PROJECT_ID + " IS NULL AND " +
                TaskTable.STATUS + " != ?";
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql,
                new String[]{workspaceId, TaskStatus.CANCELLED}
        )) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public List<Task> findDeletedPersonalTasks(String workspaceId) {
        List<Task> result = new ArrayList<>();
        String selection = TaskTable.WORKSPACE_ID + " = ? AND " +
                TaskTable.PROJECT_ID + " IS NULL AND " + TaskTable.DELETED_AT + " > 0";
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TaskTable.TABLE_NAME, null, selection, new String[]{workspaceId},
                null, null, TaskTable.DELETED_AT + " DESC")) {
            while (cursor.moveToNext()) result.add(mapCursorToTask(cursor));
        }
        return result;
    }

    /*
     * =========================================================
     * CHUYỂN TASK THÀNH CONTENT VALUES
     * =========================================================
     */

    private ContentValues toContentValues(Task task) {
        ContentValues values = new ContentValues();

        values.put(TaskTable.TASK_ID, task.getTaskId());
        values.put(
                TaskTable.WORKSPACE_ID,
                task.getWorkspaceId()
        );

        if (task.getProjectId() == null) {
            values.putNull(TaskTable.PROJECT_ID);
        } else {
            values.put(
                    TaskTable.PROJECT_ID,
                    task.getProjectId()
            );
        }

        values.put(
                TaskTable.CREATED_BY,
                task.getCreatedBy()
        );

        values.put(TaskTable.TITLE, task.getTitle());

        values.put(
                TaskTable.DESCRIPTION,
                task.getDescription()
        );

        values.put(TaskTable.STATUS, task.getStatus());
        values.put(TaskTable.PRIORITY, task.getPriority());
        values.put(TaskTable.PROGRESS, task.getProgress());

        if (task.getStartDate() > 0) {
            values.put(
                    TaskTable.START_DATE,
                    task.getStartDate()
            );
        } else {
            values.putNull(TaskTable.START_DATE);
        }

        if (task.getDueDate() > 0) {
            values.put(
                    TaskTable.DUE_DATE,
                    task.getDueDate()
            );
        } else {
            values.putNull(TaskTable.DUE_DATE);
        }

        values.put(
                TaskTable.ESTIMATED_MINUTES,
                task.getEstimatedMinutes()
        );

        if (task.getCompletedAt() > 0) {
            values.put(TaskTable.COMPLETED_AT, task.getCompletedAt());
        } else {
            values.putNull(TaskTable.COMPLETED_AT);
        }
        if (task.getDeletedAt() > 0) {
            values.put(TaskTable.DELETED_AT, task.getDeletedAt());
        } else {
            values.putNull(TaskTable.DELETED_AT);
        }
        values.put(TaskTable.VERSION, Math.max(1, task.getVersion()));
        values.put(TaskTable.SYNC_STATUS,
                task.getSyncStatus() == null ? SyncStatus.SYNCED : task.getSyncStatus());

        values.put(
                TaskTable.CREATED_AT,
                task.getCreatedAt()
        );

        values.put(
                TaskTable.UPDATED_AT,
                task.getUpdatedAt()
        );

        return values;
    }

    /*
     * =========================================================
     * CHUYỂN CURSOR THÀNH TASK
     * =========================================================
     */

    private Task mapCursorToTask(Cursor cursor) {
        Task task = new Task();

        task.setTaskId(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TaskTable.TASK_ID
                )
        ));

        task.setWorkspaceId(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TaskTable.WORKSPACE_ID
                )
        ));

        int projectColumn = cursor.getColumnIndexOrThrow(
                TaskTable.PROJECT_ID
        );

        if (cursor.isNull(projectColumn)) {
            task.setProjectId(null);
        } else {
            task.setProjectId(
                    cursor.getString(projectColumn)
            );
        }

        task.setCreatedBy(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TaskTable.CREATED_BY
                )
        ));

        task.setTitle(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TaskTable.TITLE
                )
        ));

        task.setDescription(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TaskTable.DESCRIPTION
                )
        ));

        task.setStatus(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TaskTable.STATUS
                )
        ));

        task.setPriority(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TaskTable.PRIORITY
                )
        ));

        task.setProgress(cursor.getInt(
                cursor.getColumnIndexOrThrow(
                        TaskTable.PROGRESS
                )
        ));

        int startDateColumn = cursor.getColumnIndexOrThrow(
                TaskTable.START_DATE
        );

        task.setStartDate(
                cursor.isNull(startDateColumn)
                        ? 0
                        : cursor.getLong(startDateColumn)
        );

        int dueDateColumn = cursor.getColumnIndexOrThrow(
                TaskTable.DUE_DATE
        );

        task.setDueDate(
                cursor.isNull(dueDateColumn)
                        ? 0
                        : cursor.getLong(dueDateColumn)
        );

        task.setEstimatedMinutes(cursor.getInt(
                cursor.getColumnIndexOrThrow(
                        TaskTable.ESTIMATED_MINUTES
                )
        ));

        int completedAtColumn = cursor.getColumnIndexOrThrow(TaskTable.COMPLETED_AT);
        task.setCompletedAt(cursor.isNull(completedAtColumn) ? 0 : cursor.getLong(completedAtColumn));
        int deletedAtColumn = cursor.getColumnIndexOrThrow(TaskTable.DELETED_AT);
        task.setDeletedAt(cursor.isNull(deletedAtColumn) ? 0 : cursor.getLong(deletedAtColumn));
        task.setVersion(cursor.getInt(cursor.getColumnIndexOrThrow(TaskTable.VERSION)));
        task.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.SYNC_STATUS)));

        task.setCreatedAt(cursor.getLong(
                cursor.getColumnIndexOrThrow(
                        TaskTable.CREATED_AT
                )
        ));

        task.setUpdatedAt(cursor.getLong(
                cursor.getColumnIndexOrThrow(
                        TaskTable.UPDATED_AT
                )
        ));

        return task;
    }

    /*
     * =========================================================
     * TẠO CÂU SẮP XẾP AN TOÀN
     * =========================================================
     */

    private String buildOrderBy(
            String sortOption,
            boolean ascending
    ) {
        String direction = ascending ? " ASC" : " DESC";

        if (TaskSortOption.TITLE.equals(sortOption)) {
            return TaskTable.TITLE +
                    " COLLATE NOCASE" + direction;
        }

        if (TaskSortOption.PROGRESS.equals(sortOption)) {
            return TaskTable.PROGRESS + direction +
                    ", " + TaskTable.CREATED_AT + " DESC";
        }

        if (TaskSortOption.DUE_DATE.equals(sortOption)) {
            return "CASE WHEN " +
                    TaskTable.DUE_DATE +
                    " IS NULL OR " +
                    TaskTable.DUE_DATE +
                    " = 0 THEN 1 ELSE 0 END ASC, " +
                    TaskTable.DUE_DATE + direction;
        }

        if (TaskSortOption.PRIORITY.equals(sortOption)) {
            return "CASE " + TaskTable.PRIORITY +
                    " WHEN 'URGENT' THEN 4" +
                    " WHEN 'HIGH' THEN 3" +
                    " WHEN 'MEDIUM' THEN 2" +
                    " WHEN 'LOW' THEN 1" +
                    " ELSE 0 END" + direction +
                    ", " + TaskTable.CREATED_AT + " DESC";
        }

        return TaskTable.CREATED_AT + direction;
    }
}
