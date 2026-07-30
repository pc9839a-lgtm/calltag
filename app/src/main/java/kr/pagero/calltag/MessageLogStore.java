package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class MessageLogStore extends SQLiteOpenHelper {
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_SENDING = "SENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final String DB_NAME = "calltag_messages.db";
    private static final int DB_VERSION = 1;

    public MessageLogStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE message_jobs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "customer_id INTEGER NOT NULL DEFAULT 0," +
                "call_log_id INTEGER NOT NULL DEFAULT 0," +
                "phone TEXT NOT NULL," +
                "normalized_phone TEXT NOT NULL," +
                "body TEXT NOT NULL," +
                "trigger_type TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "scheduled_at INTEGER NOT NULL," +
                "sent_at INTEGER NOT NULL DEFAULT 0," +
                "error TEXT NOT NULL DEFAULT ''," +
                "subscription_id INTEGER NOT NULL DEFAULT -1," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_message_status_time ON message_jobs(status, scheduled_at)");
        db.execSQL("CREATE INDEX idx_message_phone_trigger ON message_jobs(normalized_phone, trigger_type, created_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS message_jobs");
        onCreate(db);
    }

    public long createJob(long customerId, long callLogId, String phone,
                          String body, String triggerType, String status,
                          long scheduledAt, int subscriptionId) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.length() < 8) throw new IllegalArgumentException("전화번호를 확인해주세요.");
        String message = body == null ? "" : body.trim();
        if (message.isEmpty()) throw new IllegalArgumentException("문자 내용을 입력해주세요.");
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("customer_id", Math.max(0L, customerId));
        values.put("call_log_id", Math.max(0L, callLogId));
        values.put("phone", phone == null ? normalized : phone.trim());
        values.put("normalized_phone", normalized);
        values.put("body", message);
        values.put("trigger_type", triggerType == null ? "MANUAL" : triggerType);
        values.put("status", status);
        values.put("scheduled_at", Math.max(now, scheduledAt));
        values.put("subscription_id", subscriptionId);
        values.put("created_at", now);
        values.put("updated_at", now);
        return getWritableDatabase().insertOrThrow("message_jobs", null, values);
    }

    public MessageRecord find(long id) {
        try (Cursor cursor = getReadableDatabase().query(
                "message_jobs", null, "id=?", new String[]{String.valueOf(id)},
                null, null, null, "1")) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    public List<MessageRecord> listRecent(int limit) {
        List<MessageRecord> rows = new ArrayList<>();
        int safeLimit = Math.max(1, Math.min(300, limit));
        try (Cursor cursor = getReadableDatabase().query(
                "message_jobs", null, null, null, null, null,
                "created_at DESC,id DESC", String.valueOf(safeLimit))) {
            while (cursor.moveToNext()) rows.add(read(cursor));
        }
        return rows;
    }

    public List<MessageRecord> listScheduled() {
        List<MessageRecord> rows = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "message_jobs", null, "status=?",
                new String[]{STATUS_SCHEDULED}, null, null,
                "scheduled_at ASC,id ASC")) {
            while (cursor.moveToNext()) rows.add(read(cursor));
        }
        return rows;
    }

    public boolean hasRecentActive(String phone, String triggerType, long since) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        String sql = "SELECT 1 FROM message_jobs WHERE normalized_phone=? AND trigger_type=? " +
                "AND created_at>=? AND status IN (?,?,?,?) LIMIT 1";
        String[] args = {
                normalized,
                triggerType,
                String.valueOf(since),
                STATUS_SCHEDULED,
                STATUS_READY,
                STATUS_SENDING,
                STATUS_SENT
        };
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            return cursor.moveToFirst();
        }
    }

    public int countByStatus(String status) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM message_jobs WHERE status=?",
                new String[]{status})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void markReady(long id) {
        updateStatus(id, STATUS_READY, "", 0L);
    }

    public void markSending(long id) {
        updateStatus(id, STATUS_SENDING, "", 0L);
    }

    public void markSent(long id) {
        updateStatus(id, STATUS_SENT, "", System.currentTimeMillis());
    }

    public void markFailed(long id, String error) {
        updateStatus(id, STATUS_FAILED, error, 0L);
    }

    public void markSkipped(long id, String reason) {
        updateStatus(id, STATUS_SKIPPED, reason, 0L);
    }

    public void cancel(long id, String reason) {
        updateStatus(id, STATUS_CANCELLED, reason, 0L);
    }

    public int cancelScheduledForPhone(String phone, String triggerType, String reason) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        ContentValues values = new ContentValues();
        values.put("status", STATUS_CANCELLED);
        values.put("error", reason == null ? "" : reason);
        values.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().update(
                "message_jobs",
                values,
                "normalized_phone=? AND trigger_type=? AND status=?",
                new String[]{normalized, triggerType, STATUS_SCHEDULED});
    }

    private void updateStatus(long id, String status, String error, long sentAt) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("error", error == null ? "" : error);
        if (sentAt > 0L) values.put("sent_at", sentAt);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("message_jobs", values, "id=?",
                new String[]{String.valueOf(id)});
    }

    private MessageRecord read(Cursor cursor) {
        return new MessageRecord(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("customer_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("call_log_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("normalized_phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("body")),
                cursor.getString(cursor.getColumnIndexOrThrow("trigger_type")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getLong(cursor.getColumnIndexOrThrow("scheduled_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("sent_at")),
                cursor.getString(cursor.getColumnIndexOrThrow("error")),
                cursor.getInt(cursor.getColumnIndexOrThrow("subscription_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
    }
}
