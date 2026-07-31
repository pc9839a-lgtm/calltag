package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class TaskAutomation {
    private TaskAutomation() {}

    public static boolean completeNextCallTask(Context context, String phone) {
        if (context == null || PhoneNumberNormalizer.normalize(phone).length() < 8) return false;
        CallTagDbHelper helper = new CallTagDbHelper(context);
        try {
            Customer customer = helper.findByPhone(phone);
            if (customer == null) return false;
            SQLiteDatabase db = helper.getWritableDatabase();
            long taskId = -1L;
            String title = "전화하기";
            String taskType = TaskTypeStore.TYPE_CALL;
            long dueAt = System.currentTimeMillis();
            try (Cursor cursor = db.rawQuery(
                    "SELECT id,title,task_type,due_at FROM follow_up_tasks " +
                            "WHERE customer_id=? AND task_type='CALL' AND status='PENDING' " +
                            "ORDER BY due_at ASC,id ASC LIMIT 1",
                    new String[]{String.valueOf(customer.id)})) {
                if (cursor.moveToFirst()) {
                    taskId = cursor.getLong(0);
                    title = cursor.getString(1);
                    taskType = cursor.getString(2);
                    dueAt = cursor.getLong(3);
                }
            }
            if (taskId <= 0L) return false;

            long now = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put("status", "COMPLETED");
            values.put("completed_at", now);
            db.update("follow_up_tasks", values, "id=?", new String[]{String.valueOf(taskId)});

            FollowUpTask completedTask = new FollowUpTask(
                    taskId, customer.id, customer.displayName, customer.primaryPhone,
                    title, taskType, dueAt, "PENDING");
            TaskMessageLifecycleManager.onTaskCompleted(context, completedTask);

            helper.insertInteraction(customer.id, "TASK_AUTO_COMPLETE", now, now, 0L,
                    "CALL_COMPLETED", title + " · 연결된 발신 통화로 자동 완료");
            return true;
        } finally {
            helper.close();
        }
    }
}
