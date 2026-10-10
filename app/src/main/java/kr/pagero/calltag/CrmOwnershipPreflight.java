package kr.pagero.calltag;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

/**
 * Read-only ownership preflight for the OLD shared calltag.db.
 *
 * A historical SQLite file has no per-row owner_id. The receipt and cloud-sync
 * journals can prove that multiple accounts used it, but cannot safely assign
 * unattributed rows. This gate must never auto-reparent, delete or migrate rows.
 */
public final class CrmOwnershipPreflight {
    private static final String SHARED_CRM = "calltag.db";
    private static final String SYNC_JOURNAL = "calltag_sync_local.db";

    private CrmOwnershipPreflight() {}

    public static Snapshot inspect(Context context, String accountKey) {
        if (context == null || accountKey == null || accountKey.isEmpty()) {
            throw new IllegalStateException("고객 데이터를 확인할 로그인 계정이 없습니다.");
        }
        File crmFile = context.getApplicationContext().getDatabasePath(SHARED_CRM);
        if (!crmFile.isFile()) return new Snapshot(0L, 0L, 0L, 0L, false, false);

        long customers;
        long interactions;
        long tasks;
        long foreignLeadJournal = 0L;
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                crmFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
            customers = count(db, "customers");
            interactions = count(db, "interactions");
            tasks = count(db, "follow_up_tasks");
            if (hasTable(db, "universal_lead_import_events")) {
                String ownerId = AuthSessionStore.ownerId(context).trim();
                if (ownerId.isEmpty()) {
                    throw new IllegalStateException("기존 CRM을 확인할 계정 ID가 없습니다.");
                }
                foreignLeadJournal = countQuery(db,
                        "SELECT COUNT(*) FROM universal_lead_import_events WHERE owner_id<>?",
                        new String[]{ownerId});
            }
        } catch (RuntimeException error) {
            throw new IllegalStateException("기존 고객 DB 소유 이력을 확인하지 못했습니다.", error);
        }

        long ownMappings = 0L;
        long foreignMappings = 0L;
        File syncFile = context.getApplicationContext().getDatabasePath(SYNC_JOURNAL);
        if (syncFile.isFile()) {
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    syncFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
                if (hasTable(db, "entity_map")) {
                    String sql = "SELECT COUNT(*) FROM entity_map WHERE account_key=? "
                            + "AND entity_type IN ('customer','interaction','task') AND deleted=0";
                    ownMappings = countQuery(db, sql, new String[]{accountKey});
                    foreignMappings = countQuery(db,
                            "SELECT COUNT(*) FROM entity_map WHERE account_key<>? "
                                    + "AND entity_type IN ('customer','interaction','task') "
                                    + "AND deleted=0",
                            new String[]{accountKey});
                }
            } catch (RuntimeException error) {
                throw new IllegalStateException("기존 고객 동기화 소유 이력을 확인하지 못했습니다.", error);
            }
        }
        long liveRows = customers + interactions + tasks;
        return new Snapshot(liveRows, ownMappings, foreignMappings,
                foreignLeadJournal, liveRows > 0 && ownMappings == 0,
                liveRows > 0 && (foreignMappings > 0 || foreignLeadJournal > 0));
    }

    public static void requireSafeForSync(Context context, String accountKey) {
        Snapshot result = inspect(context, accountKey);
        if (result.foreignOwnerDetected) {
            throw new IllegalStateException(
                    "공용 CRM에서 다른 계정의 데이터가 확인되어 서버 동기화를 차단했습니다. "
                    + "기존 기록은 삭제되지 않았습니다.");
        }
        if (result.unattributedLegacyRecords) {
            throw new IllegalStateException(
                    "기존 고객 데이터의 소유 계정이 확인되지 않아 서버 동기화를 보류했습니다. "
                    + "기기 기록은 그대로 보존됩니다.");
        }
    }

    /**
     * Cloud sync v2 is attached to the account-specific CRM database.
     * The legacy shared file is never scanned and must never affect the new owner namespace.
     */
    public static void requireScopedForSync(Context context, String accountKey) {
        if (context == null || accountKey == null || !accountKey.endsWith("|crm:v2")
                || !accountKey.equals(CallTagSyncLocalStore.accountKey(context))) {
            throw new IllegalStateException("동기화 계정 식별자가 바뀌어 작업을 중지했습니다.");
        }
        AccountDataScope.currentCrmName(context);
    }

    private static long count(SQLiteDatabase db, String table) {
        if (!hasTable(db, table)) {
            throw new IllegalStateException("기존 CRM 테이블이 누락되었습니다: " + table);
        }
        return countQuery(db, "SELECT COUNT(*) FROM " + table, null);
    }

    private static boolean hasTable(SQLiteDatabase db, String table) {
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{table})) {
            return cursor.moveToFirst();
        }
    }

    private static long countQuery(SQLiteDatabase db, String sql, String[] args) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0L;
        }
    }

    public static final class Snapshot {
        public final long crmRows;
        public final long currentAccountMappings;
        public final long foreignAccountMappings;
        public final long foreignLeadImports;
        public final boolean unattributedLegacyRecords;
        public final boolean foreignOwnerDetected;

        Snapshot(long crmRows, long currentAccountMappings,
                 long foreignAccountMappings, long foreignLeadImports,
                 boolean unattributedLegacyRecords, boolean foreignOwnerDetected) {
            this.crmRows = crmRows;
            this.currentAccountMappings = currentAccountMappings;
            this.foreignAccountMappings = foreignAccountMappings;
            this.foreignLeadImports = foreignLeadImports;
            this.unattributedLegacyRecords = unattributedLegacyRecords;
            this.foreignOwnerDetected = foreignOwnerDetected;
        }
    }
}
