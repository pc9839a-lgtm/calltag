package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Universal Lead receipts are account-scoped; legacy v1 records remain untouched. */
public final class UniversalLeadReceiptStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "calltag-universal-lead-sync.db";
    private static final int DB_VERSION = 2;
    private static final String ACCOUNT_TABLE = "lead_receipts_by_owner";

    public UniversalLeadReceiptStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAccountTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // v1 lead_receipts has no owner information. It is unsafe to attribute those
            // records to whichever account happens to be logged in during an upgrade.
            // Keep the v1 table and its data intact for recovery/auditing; new writes
            // use the scoped v2 table only.
            createAccountTable(db);
        }
    }

    private static void createAccountTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ACCOUNT_TABLE + " (" +
                "owner_id TEXT NOT NULL," +
                "event_id TEXT NOT NULL," +
                "server_lead_id INTEGER NOT NULL," +
                "customer_id INTEGER NOT NULL," +
                "status TEXT NOT NULL," +
                "received_at INTEGER NOT NULL," +
                "acked_at INTEGER," +
                "PRIMARY KEY(owner_id, event_id)" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lead_owner_ack " +
                "ON " + ACCOUNT_TABLE + "(owner_id, status, server_lead_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lead_owner_customer " +
                "ON " + ACCOUNT_TABLE + "(owner_id, customer_id, received_at DESC)");
    }

    public boolean isImported(String ownerId, String eventId) {
        String owner = requireOwner(ownerId);
        if (eventId == null || eventId.trim().isEmpty()) return false;
        try (Cursor cursor = getReadableDatabase().query(
                ACCOUNT_TABLE, new String[]{"event_id"},
                "owner_id=? AND event_id=? AND status IN ('IMPORTED','ACKED')",
                new String[]{owner, eventId.trim()}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    public void markImported(String ownerId, String eventId, long serverLeadId, long customerId) {
        String owner = requireOwner(ownerId);
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("외부 문의 eventId가 없습니다.");
        }
        ContentValues values = new ContentValues();
        values.put("owner_id", owner);
        values.put("event_id", eventId.trim());
        values.put("server_lead_id", serverLeadId);
        values.put("customer_id", customerId);
        values.put("status", "IMPORTED");
        values.put("received_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(
                ACCOUNT_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void markAcked(String ownerId, long serverLeadId) {
        String owner = requireOwner(ownerId);
        ContentValues values = new ContentValues();
        values.put("status", "ACKED");
        values.put("acked_at", System.currentTimeMillis());
        getWritableDatabase().update(
                ACCOUNT_TABLE, values, "owner_id=? AND server_lead_id=?",
                new String[]{owner, String.valueOf(serverLeadId)});
    }

    private static String requireOwner(String ownerId) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("로그인 계정을 확인해주세요.");
        }
        return ownerId.trim();
    }
}
