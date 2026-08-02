package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class PageroLeadReceiptStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "calltag-pagero-sync.db";
    private static final int DB_VERSION = 1;

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
                "acked_at INTEGER" +
                ")");
        db.execSQL("CREATE INDEX idx_lead_receipts_ack ON lead_receipts(status, server_lead_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 최초 버전. 이후 변경 시 기존 수신 이력을 보존하는 마이그레이션만 추가한다.
    }

    public boolean isImported(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) return false;
        try (Cursor cursor = getReadableDatabase().query(
                "lead_receipts",
                new String[]{"event_id"},
                "event_id=? AND status IN ('IMPORTED','ACKED')",
                new String[]{eventId.trim()},
                null,
                null,
                null,
                "1")) {
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
        getWritableDatabase().insertWithOnConflict(
                "lead_receipts", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void markAcked(long serverLeadId) {
        ContentValues values = new ContentValues();
        values.put("status", "ACKED");
        values.put("acked_at", System.currentTimeMillis());
        getWritableDatabase().update(
                "lead_receipts",
                values,
                "server_lead_id=?",
                new String[]{String.valueOf(serverLeadId)});
    }
}
