package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** 페이지로 eventId 멱등 처리 + 해당 문의의 문자 처리 상태 연결. */
public final class PageroLeadReceiptStore extends SQLiteOpenHelper {
    public static final String SMS_NOT_SENT = "NOT_SENT";
    public static final String SMS_SENDING = "SENDING";
    public static final String SMS_SENT = "SENT";
    public static final String SMS_FAILED = "FAILED";

    private static final String DB_NAME = "calltag-pagero-sync.db";
    private static final int DB_VERSION = 2;

    public PageroLeadReceiptStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE lead_receipts (" +
                "event_id TEXT PRIMARY KEY," +
                "server_lead_id INTEGER NOT NULL," +
                "customer_id INTEGER NOT NULL," +
                "status TEXT NOT NULL," +
                "received_at INTEGER NOT NULL," +
                "acked_at INTEGER," +
                "sms_job_id INTEGER NOT NULL DEFAULT 0," +
                "sms_status TEXT NOT NULL DEFAULT 'NOT_SENT'," +
                "sms_reason TEXT NOT NULL DEFAULT ''," +
                "sms_updated_at INTEGER NOT NULL DEFAULT 0" +
                ")");
        createIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            addColumn(db, "sms_job_id", "INTEGER NOT NULL DEFAULT 0");
            addColumn(db, "sms_status", "TEXT NOT NULL DEFAULT 'NOT_SENT'");
            addColumn(db, "sms_reason", "TEXT NOT NULL DEFAULT ''");
            addColumn(db, "sms_updated_at", "INTEGER NOT NULL DEFAULT 0");
            createIndexes(db);
        }
    }

    private void addColumn(SQLiteDatabase db, String name, String definition) {
        try {
            db.execSQL("ALTER TABLE lead_receipts ADD COLUMN " + name + " " + definition);
        } catch (RuntimeException ignored) {
            // 이미 마이그레이션된 기기 호환.
        }
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lead_receipts_ack " +
                "ON lead_receipts(status, server_lead_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lead_receipts_customer " +
                "ON lead_receipts(customer_id, received_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lead_receipts_sms_job " +
                "ON lead_receipts(sms_job_id)");
    }

    public boolean isImported(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) return false;
        try (Cursor cursor = getReadableDatabase().query(
                "lead_receipts", new String[]{"event_id"},
                "event_id=? AND status IN ('IMPORTED','ACKED')",
                new String[]{eventId.trim()}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    public void markImported(String eventId, long serverLeadId, long customerId) {
        ContentValues values = new ContentValues();
        values.put("event_id", eventId.trim());
        values.put("server_lead_id", serverLeadId);
        values.put("customer_id", customerId);
        values.put("status", "IMPORTED");
        values.put("received_at", System.currentTimeMillis());
        values.put("sms_status", SMS_NOT_SENT);
        values.put("sms_reason", "");
        values.put("sms_updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(
                "lead_receipts", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void markAcked(long serverLeadId) {
        ContentValues values = new ContentValues();
        values.put("status", "ACKED");
        values.put("acked_at", System.currentTimeMillis());
        getWritableDatabase().update("lead_receipts", values, "server_lead_id=?",
                new String[]{String.valueOf(serverLeadId)});
    }

    public void markSms(String eventId, long jobId, String status, String reason) {
        if (eventId == null || eventId.trim().isEmpty()) return;
        ContentValues values = smsValues(jobId, status, reason, true);
        getWritableDatabase().update("lead_receipts", values, "event_id=?",
                new String[]{eventId.trim()});
    }

    /** SmsStatusReceiver가 실제 통신사 callback을 받은 시점의 최종 상태를 receipt에도 영구 반영한다. */
    public void markSmsByJobId(long jobId, String status, String reason) {
        if (jobId <= 0L) return;
        ContentValues values = smsValues(jobId, status, reason, false);
        getWritableDatabase().update("lead_receipts", values, "sms_job_id=?",
                new String[]{String.valueOf(jobId)});
    }

    private ContentValues smsValues(long jobId, String status, String reason, boolean writeJobId) {
        ContentValues values = new ContentValues();
        if (writeJobId) values.put("sms_job_id", Math.max(0L, jobId));
        values.put("sms_status", normalizeSmsStatus(status));
        values.put("sms_reason", reason == null ? "" : reason.trim());
        values.put("sms_updated_at", System.currentTimeMillis());
        return values;
    }

    public SmsSnapshot latestSmsForCustomer(long customerId) {
        try (Cursor cursor = getReadableDatabase().query(
                "lead_receipts",
                new String[]{"event_id", "sms_job_id", "sms_status", "sms_reason", "sms_updated_at"},
                "customer_id=?",
                new String[]{String.valueOf(customerId)}, null, null,
                "received_at DESC", "1")) {
            if (!cursor.moveToFirst()) return null;
            return new SmsSnapshot(cursor.getString(0), cursor.getLong(1), cursor.getString(2),
                    cursor.getString(3), cursor.getLong(4));
        }
    }

    private String normalizeSmsStatus(String status) {
        if (SMS_SENDING.equals(status) || SMS_SENT.equals(status) || SMS_FAILED.equals(status)) {
            return status;
        }
        return SMS_NOT_SENT;
    }

    public static final class SmsSnapshot {
        public final String eventId;
        public final long jobId;
        public final String status;
        public final String reason;
        public final long updatedAt;

        SmsSnapshot(String eventId, long jobId, String status, String reason, long updatedAt) {
            this.eventId = eventId == null ? "" : eventId;
            this.jobId = jobId;
            this.status = status == null ? SMS_NOT_SENT : status;
            this.reason = reason == null ? "" : reason;
            this.updatedAt = updatedAt;
        }
    }
}
