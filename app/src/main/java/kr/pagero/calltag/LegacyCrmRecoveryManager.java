package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * An explicit, one-time, non-destructive *claim* of a single-owner legacy CRM.
 * Historical rows have no owner_id: never silently assign ownership.
 * A second historic account marker, a non-pristine destination, or an active
 * synchronizer is a hard stop. The original calltag.db is never modified.
 */
public final class LegacyCrmRecoveryManager {
    private static final String SOURCE = "calltag.db";
    private static final String LEGACY_SYNC = "calltag_sync_local.db";
    private static final String MARKER = "legacy_crm_recovery";
    private static final Object LOCK = new Object();
    private static final String[] TABLES = {
            "customers", "crm_stages", "opportunities", "interactions",
            "follow_up_tasks", "phone_rules", "universal_lead_import_events",
            "post_call_save_receipts"
    };
    private static final String[] REQUIRED = {
            "customers", "crm_stages", "opportunities", "interactions",
            "follow_up_tasks", "phone_rules"
    };

    private LegacyCrmRecoveryManager() {}

    public static boolean hasLegacyData(Context context) {
        File source = context.getDatabasePath(SOURCE);
        if (!source.isFile()) return false;
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                source.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
            return tableExists(db, "customers") && count(db, "customers") > 0
                    || tableExists(db, "interactions") && count(db, "interactions") > 0
                    || tableExists(db, "follow_up_tasks") && count(db, "follow_up_tasks") > 0;
        } catch (RuntimeException failed) {
            // A damaged legacy file needs assisted recovery, not silent skipping.
            return true;
        }
    }

    public static Result recoverAfterOwnerConfirmation(Context context, String confirmedOwnerId)
            throws Exception {
        synchronized (LOCK) {
            Context app = context.getApplicationContext();
            String ownerId = AccountDataScope.requireOwner(app);
            String session = AuthSessionStore.session(app);
            if (!ownerId.equals(confirmedOwnerId) || session.isEmpty()) {
                throw new IllegalStateException("현재 로그인한 계정을 다시 확인해주세요.");
            }
            if (!CallTagSyncManager.beginMaintenance()) {
                throw new IllegalStateException("다른 데이터 작업이 진행 중입니다.");
            }
            try {
                if (PageroLeadSyncManager.isRunning() || UniversalLeadSyncManager.isRunning()) {
                    throw new IllegalStateException("문의 수신이 끝난 뒤 다시 시도해주세요.");
                }
                File sourceFile = app.getDatabasePath(SOURCE);
                if (!sourceFile.isFile()) throw new IllegalStateException("이전 고객 DB가 없습니다.");
                try (SQLiteDatabase source = SQLiteDatabase.openDatabase(
                        sourceFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                     CallTagDbHelper targetHelper = new CallTagDbHelper(app)) {
                    SQLiteDatabase target = targetHelper.getWritableDatabase();
                    ensureSingleOwner(app, source, ownerId);
                    for (String table : REQUIRED) {
                        if (!tableExists(source, table)) {
                            throw new IllegalStateException("기존 DB 필수 테이블 누락: " + table);
                        }
                    }
                    if (tableExists(target, MARKER)) {
                        try (Cursor cursor = target.rawQuery(
                                "SELECT owner_hash FROM " + MARKER + " LIMIT 1", null)) {
                            if (cursor.moveToFirst()) {
                                if (AccountDataScope.fingerprint(ownerId).equals(cursor.getString(0))) {
                                    disableAndResetOwnerSync(app, ownerId, session);
                                    return new Result(true, 0, 0, 0);
                                }
                                throw new IllegalStateException("이미 다른 계정의 복구 기록이 있습니다.");
                            }
                        }
                    }
                    ensureFreshTarget(target);
                    int customers = (int) count(source, "customers");
                    int interactions = (int) count(source, "interactions");
                    int tasks = (int) count(source, "follow_up_tasks");
                    if (customers + interactions + tasks == 0) {
                        throw new IllegalStateException("이전 고객 기록이 없습니다.");
                    }
                    target.beginTransaction();
                    try {
                        // Fresh owner DB contains only default stages, regenerated by onCreate.
                        target.delete("crm_stages", null, null);
                        for (String table : TABLES) {
                            if (!tableExists(source, table)) continue;
                            copyRows(source, target, table, ownerId);
                            if (count(target, table) != count(source, table)) {
                                throw new SQLiteException("복구 레코드 수가 다릅니다: " + table);
                            }
                        }
                        try (Cursor fk = target.rawQuery("PRAGMA foreign_key_check", null)) {
                            if (fk.moveToFirst()) {
                                throw new SQLiteException("복구 중 참조 관계가 손상됐습니다.");
                            }
                        }
                        if (!ownerId.equals(AuthSessionStore.ownerId(app))
                                || !session.equals(AuthSessionStore.session(app))) {
                            throw new IllegalStateException("복구 중 로그인 계정이 변경됐습니다.");
                        }
                        target.execSQL("CREATE TABLE IF NOT EXISTS " + MARKER + " (" +
                                "owner_hash TEXT PRIMARY KEY, recovered_at INTEGER NOT NULL," +
                                "customer_count INTEGER NOT NULL, interaction_count INTEGER NOT NULL," +
                                "task_count INTEGER NOT NULL)");
                        ContentValues done = new ContentValues();
                        done.put("owner_hash", AccountDataScope.fingerprint(ownerId));
                        done.put("recovered_at", System.currentTimeMillis());
                        done.put("customer_count", customers);
                        done.put("interaction_count", interactions);
                        done.put("task_count", tasks);
                        target.insertOrThrow(MARKER, null, done);
                        target.setTransactionSuccessful();
                    } finally {
                        target.endTransaction();
                    }
                    // Local v2 cloud mapping IDs are no longer guaranteed to refer
                    // to the same customers after a historical CRM copy.
                    disableAndResetOwnerSync(app, ownerId, session);
                    // Deliberately never alter, rename, or delete the legacy database.
                    return new Result(false, customers, interactions, tasks);
                }
            } finally {
                CallTagSyncManager.endMaintenance();
            }
        }
    }

    private static void disableAndResetOwnerSync(
            Context context, String ownerId, String session) {
        if (!ownerId.equals(AuthSessionStore.ownerId(context))
                || !session.equals(AuthSessionStore.session(context))) {
            throw new IllegalStateException("계정이 바뀌어 복구 동기화 정보를 변경하지 않았습니다.");
        }
        CallTagSyncPreferenceStore.setEnabled(context, false);
        try (CallTagSyncLocalStore store = new CallTagSyncLocalStore(context)) {
            String key = store.accountKey();
            if (!key.isEmpty()) {
                SQLiteDatabase db = store.getWritableDatabase();
                db.beginTransaction();
                try {
                    db.delete("entity_map", "account_key=?", new String[]{key});
                    db.delete("sync_meta", "account_key=?", new String[]{key});
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    private static void ensureSingleOwner(Context app, SQLiteDatabase source, String ownerId) {
        if (tableExists(source, "universal_lead_import_events")
                && queryCount(source,
                        "SELECT COUNT(*) FROM universal_lead_import_events WHERE owner_id<>?",
                        new String[]{ownerId}) > 0) {
            throw new IllegalStateException("이전 기록에 다른 계정의 문의가 포함돼 복구할 수 없습니다.");
        }
        if (tableExists(source, "post_call_save_receipts")
                && queryCount(source,
                        "SELECT COUNT(*) FROM post_call_save_receipts WHERE account_key<>?",
                        new String[]{"owner:" + ownerId}) > 0) {
            throw new IllegalStateException("이전 기록에 다른 계정의 상담 이력이 있어 복구할 수 없습니다.");
        }
        File journal = app.getDatabasePath(LEGACY_SYNC);
        if (journal.isFile()) {
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    journal.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
                if (tableExists(db, "entity_map") && queryCount(db,
                        "SELECT COUNT(*) FROM entity_map WHERE account_key<>? " +
                                "AND account_key NOT LIKE '%|crm:v2' " +
                                "AND entity_type IN ('customer','interaction','task','stage')",
                        new String[]{"owner:" + ownerId}) > 0) {
                    throw new IllegalStateException("이 기기에서 여러 계정이 사용된 흔적이 있습니다. " +
                            "자동 복구 대신 소유권 확인이 필요합니다.");
                }
            }
        }
    }

    private static void ensureFreshTarget(SQLiteDatabase db) {
        for (String name : new String[]{"customers", "opportunities", "interactions",
                "follow_up_tasks", "phone_rules", "universal_lead_import_events",
                "post_call_save_receipts"}) {
            if (tableExists(db, name) && count(db, name) != 0) {
                throw new IllegalStateException(
                        "현재 계정에 이미 데이터가 있어 기존 기록을 덮어쓰지 않습니다.");
            }
        }
        if (count(db, "crm_stages") != 3 || queryCount(db,
                "SELECT COUNT(*) FROM crm_stages WHERE " +
                        "(name='신규' AND position=0 AND color='#4389FF') OR " +
                        "(name='진행 중' AND position=1 AND color='#F5A524') OR " +
                        "(name='완료' AND position=2 AND color='#32D583')", null) != 3) {
            throw new IllegalStateException(
                    "현재 계정의 고객 단계 설정이 변경돼 자동 복구를 중단했습니다.");
        }
    }

    private static void copyRows(SQLiteDatabase source, SQLiteDatabase target,
                                 String table, String ownerId) {
        Set<String> destinationColumns = new HashSet<>();
        try (Cursor fields = target.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (fields.moveToNext()) {
                destinationColumns.add(fields.getString(fields.getColumnIndexOrThrow("name")));
            }
        }
        try (Cursor rows = source.rawQuery("SELECT * FROM " + table, null)) {
            String[] columns = rows.getColumnNames();
            for (String col : columns) {
                if (!destinationColumns.contains(col)) {
                    throw new IllegalStateException("이전 DB와 현재 앱 DB가 호환되지 않습니다: " + table);
                }
            }
            while (rows.moveToNext()) {
                ContentValues values = new ContentValues();
                for (int i = 0; i < columns.length; i++) {
                    switch (rows.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL: values.putNull(columns[i]); break;
                        case Cursor.FIELD_TYPE_INTEGER: values.put(columns[i], rows.getLong(i)); break;
                        case Cursor.FIELD_TYPE_FLOAT: values.put(columns[i], rows.getDouble(i)); break;
                        case Cursor.FIELD_TYPE_BLOB: values.put(columns[i], rows.getBlob(i)); break;
                        default: values.put(columns[i], rows.getString(i));
                    }
                }
                if ("post_call_save_receipts".equals(table)) {
                    // Preserve old post-call idempotence across the cloud-map v2 epoch.
                    values.put("account_key", "owner:" + ownerId + "|crm:v2");
                }
                target.insertOrThrow(table, null, values);
            }
        }
    }

    private static boolean tableExists(SQLiteDatabase db, String name) {
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{name})) {
            return cursor.moveToFirst();
        }
    }

    private static long count(SQLiteDatabase db, String table) {
        return queryCount(db, "SELECT COUNT(*) FROM " + table, null);
    }

    private static long queryCount(SQLiteDatabase db, String sql, String[] args) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    public static final class Result {
        public final boolean alreadyRecovered;
        public final int customers;
        public final int interactions;
        public final int tasks;
        private Result(boolean existing, int customerCount, int interactionCount, int taskCount) {
            alreadyRecovered = existing;
            customers = customerCount;
            interactions = interactionCount;
            tasks = taskCount;
        }
    }
}
