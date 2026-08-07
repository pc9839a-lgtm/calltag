package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.CallLog;

import java.util.ArrayList;
import java.util.List;

public final class PendingCallStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "calltag_pending.db";
    private static final int DB_VERSION = 1;
    private static final long KEEP_HANDLED_MS = 30L * 24L * 60L * 60L * 1000L;

    public PendingCallStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pending_calls (" +
                "call_log_id INTEGER PRIMARY KEY," +
                "phone TEXT NOT NULL," +
                "normalized_phone TEXT NOT NULL," +
                "cached_name TEXT NOT NULL DEFAULT ''," +
                "call_type INTEGER NOT NULL," +
                "started_at INTEGER NOT NULL," +
                "duration_sec INTEGER NOT NULL DEFAULT 0," +
                "handled INTEGER NOT NULL DEFAULT 0," +
                "handled_at INTEGER," +
                "created_at INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX idx_pending_calls_handled ON pending_calls(handled, started_at DESC)");
        db.execSQL("CREATE INDEX idx_pending_calls_phone ON pending_calls(normalized_phone, started_at DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // First production schema.
    }

    public void upsert(CallRecord record) {
        if (record == null) return;
        String normalized = PhoneNumberNormalizer.normalize(record.phone);
        if (normalized.length() < 8) return;

        ContentValues values = new ContentValues();
        values.put("call_log_id", record.id);
        values.put("phone", record.phone == null ? "" : record.phone.trim());
        values.put("normalized_phone", normalized);
        values.put("cached_name", record.cachedName == null ? "" : record.cachedName.trim());
        values.put("call_type", record.type);
        values.put("started_at", record.startedAt);
        values.put("duration_sec", Math.max(0L, record.durationSec));
        values.put("handled", 0);
        values.putNull("handled_at");
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(
                "pending_calls", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        prune();
    }

    public List<PendingCallRecord> listPending(int limit) {
        List<PendingCallRecord> rows = new ArrayList<>();
        int safeLimit = Math.max(1, Math.min(limit, 100));
        try (Cursor cursor = getReadableDatabase().query(
                "pending_calls",
                new String[]{"call_log_id", "phone", "cached_name", "call_type", "started_at", "duration_sec"},
                "handled=0",
                null,
                null,
                null,
                "started_at DESC,call_log_id DESC",
                String.valueOf(safeLimit))) {
            while (cursor.moveToNext()) {
                rows.add(new PendingCallRecord(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getLong(4),
                        cursor.getLong(5)));
            }
        }
        return rows;
    }

    public int countPending() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM pending_calls WHERE handled=0", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void markHandled(long callLogId) {
        if (callLogId <= 0L) return;
        ContentValues values = handledValues();
        getWritableDatabase().update(
                "pending_calls", values, "call_log_id=?", new String[]{String.valueOf(callLogId)});
        prune();
    }

    /** Removes only CallTag's local review item. It never deletes the Android system call log. */
    public boolean deletePending(long callLogId) {
        if (callLogId <= 0L) return false;
        int deleted = getWritableDatabase().delete(
                "pending_calls", "call_log_id=?", new String[]{String.valueOf(callLogId)});
        return deleted > 0;
    }

    public int markUnansweredHandledByPhone(String phone, long beforeStartedAt) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.length() < 8) return 0;
        ContentValues values = handledValues();
        String selection = "handled=0 AND normalized_phone=? AND started_at<? AND (" +
                "call_type=? OR call_type=? OR (call_type=? AND duration_sec=0))";
        String[] args = {
                normalized,
                String.valueOf(beforeStartedAt),
                String.valueOf(CallLog.Calls.MISSED_TYPE),
                String.valueOf(CallLog.Calls.REJECTED_TYPE),
                String.valueOf(CallLog.Calls.OUTGOING_TYPE)
        };
        int changed = getWritableDatabase().update("pending_calls", values, selection, args);
        prune();
        return changed;
    }

    private ContentValues handledValues() {
        ContentValues values = new ContentValues();
        values.put("handled", 1);
        values.put("handled_at", System.currentTimeMillis());
        return values;
    }

    private void prune() {
        long cutoff = System.currentTimeMillis() - KEEP_HANDLED_MS;
        getWritableDatabase().delete(
                "pending_calls", "handled=1 AND handled_at<?", new String[]{String.valueOf(cutoff)});
    }
}
