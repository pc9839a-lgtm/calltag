package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CallTagSyncLocalStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "calltag_sync_local.db";
    private static final int DB_VERSION = 1;

    private static final String META_CURSOR = "cursor";
    private static final String META_INITIALIZED = "initialized";
    private static final String META_LAST_ATTEMPT = "last_attempt";
    private static final String META_LAST_SUCCESS = "last_success";
    private static final String META_STATUS = "status";
    private static final String META_MESSAGE = "message";
    private static final String META_SERVER_RECORDS = "server_records";

    private final String accountKey;

    public CallTagSyncLocalStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
        accountKey = accountKey(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE entity_map (" +
                "account_key TEXT NOT NULL," +
                "entity_type TEXT NOT NULL," +
                "sync_id TEXT NOT NULL," +
                "local_id INTEGER NOT NULL DEFAULT 0," +
                "version INTEGER NOT NULL DEFAULT 1," +
                "payload_hash TEXT NOT NULL DEFAULT ''," +
                "payload_json TEXT NOT NULL DEFAULT '{}'," +
                "pending INTEGER NOT NULL DEFAULT 0," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "updated_at INTEGER NOT NULL," +
                "PRIMARY KEY(account_key, entity_type, sync_id)," +
                "UNIQUE(account_key, entity_type, local_id)" +
                ")");
        db.execSQL("CREATE INDEX idx_sync_pending ON entity_map(account_key,pending,entity_type,updated_at)");
        db.execSQL("CREATE TABLE sync_meta (" +
                "account_key TEXT NOT NULL," +
                "meta_key TEXT NOT NULL," +
                "meta_value TEXT NOT NULL DEFAULT ''," +
                "updated_at INTEGER NOT NULL," +
                "PRIMARY KEY(account_key,meta_key)" +
                ")");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public boolean isReady() {
        return !accountKey.isEmpty();
    }

    public String accountKey() {
        return accountKey;
    }

    public synchronized Mapping ensureMapping(String entityType, long localId) {
        Mapping existing = mappingByLocal(entityType, localId);
        if (existing != null) return existing;
        String syncId = entityType + ":" + UUID.randomUUID();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("account_key", accountKey);
        values.put("entity_type", entityType);
        values.put("sync_id", syncId);
        values.put("local_id", localId);
        values.put("version", 1);
        values.put("payload_hash", "");
        values.put("payload_json", "{}");
        values.put("pending", 0);
        values.put("deleted", 0);
        values.put("updated_at", now);
        getWritableDatabase().insertOrThrow("entity_map", null, values);
        return mappingBySync(entityType, syncId);
    }

    public synchronized Mapping markLocal(
            String entityType,
            long localId,
            String payloadHash,
            String payloadJson,
            boolean deleted) {
        Mapping mapping = ensureMapping(entityType, localId);
        String hash = payloadHash == null ? "" : payloadHash;
        String json = payloadJson == null ? "{}" : payloadJson;
        if (mapping.deleted == deleted && mapping.payloadHash.equals(hash)) return mapping;

        ContentValues values = new ContentValues();
        values.put("local_id", localId);
        values.put("version", Math.max(1, mapping.version + 1));
        values.put("payload_hash", hash);
        values.put("payload_json", json);
        values.put("pending", 1);
        values.put("deleted", deleted ? 1 : 0);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("entity_map", values,
                "account_key=? AND entity_type=? AND sync_id=?",
                new String[]{accountKey, entityType, mapping.syncId});
        return mappingBySync(entityType, mapping.syncId);
    }

    public synchronized void markAccepted(String entityType, String syncId, int version) {
        ContentValues values = new ContentValues();
        values.put("version", Math.max(1, version));
        values.put("pending", 0);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("entity_map", values,
                "account_key=? AND entity_type=? AND sync_id=?",
                new String[]{accountKey, entityType, syncId});
    }

    public synchronized void markRemote(
            String entityType,
            String syncId,
            long localId,
            int version,
            String payloadHash,
            String payloadJson,
            boolean deleted) {
        ContentValues values = new ContentValues();
        values.put("account_key", accountKey);
        values.put("entity_type", entityType);
        values.put("sync_id", syncId);
        values.put("local_id", Math.max(0L, localId));
        values.put("version", Math.max(1, version));
        values.put("payload_hash", payloadHash == null ? "" : payloadHash);
        values.put("payload_json", payloadJson == null ? "{}" : payloadJson);
        values.put("pending", 0);
        values.put("deleted", deleted ? 1 : 0);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(
                "entity_map", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized Mapping mappingByLocal(String entityType, long localId) {
        if (accountKey.isEmpty() || localId <= 0L) return null;
        try (Cursor cursor = getReadableDatabase().query("entity_map", null,
                "account_key=? AND entity_type=? AND local_id=?",
                new String[]{accountKey, entityType, String.valueOf(localId)},
                null, null, null, "1")) {
            return cursor.moveToFirst() ? readMapping(cursor) : null;
        }
    }

    public synchronized Mapping mappingBySync(String entityType, String syncId) {
        if (accountKey.isEmpty() || syncId == null || syncId.isEmpty()) return null;
        try (Cursor cursor = getReadableDatabase().query("entity_map", null,
                "account_key=? AND entity_type=? AND sync_id=?",
                new String[]{accountKey, entityType, syncId},
                null, null, null, "1")) {
            return cursor.moveToFirst() ? readMapping(cursor) : null;
        }
    }

    public synchronized List<Mapping> listMappings() {
        List<Mapping> rows = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("entity_map", null,
                "account_key=?", new String[]{accountKey}, null, null,
                "entity_type ASC,local_id ASC")) {
            while (cursor.moveToNext()) rows.add(readMapping(cursor));
        }
        return rows;
    }

    public synchronized List<PendingItem> listPending(int limit) {
        List<PendingItem> rows = new ArrayList<>();
        String order = "CASE entity_type " +
                "WHEN 'customer' THEN 1 WHEN 'stage' THEN 2 " +
                "WHEN 'interaction' THEN 3 WHEN 'task' THEN 4 ELSE 9 END," +
                "updated_at ASC";
        try (Cursor cursor = getReadableDatabase().query("entity_map",
                new String[]{"entity_type", "sync_id", "version", "payload_json", "deleted"},
                "account_key=? AND pending=1", new String[]{accountKey},
                null, null, order, String.valueOf(Math.max(1, Math.min(100, limit))))) {
            while (cursor.moveToNext()) {
                rows.add(new PendingItem(
                        cursor.getString(0), cursor.getString(1), cursor.getInt(2),
                        cursor.getString(3), cursor.getInt(4) != 0));
            }
        }
        return rows;
    }

    public synchronized int pendingCount() {
        return count("SELECT COUNT(*) FROM entity_map WHERE account_key=? AND pending=1",
                new String[]{accountKey});
    }

    public synchronized boolean initialized() {
        return "1".equals(meta(META_INITIALIZED, "0"));
    }

    public synchronized void setInitialized(boolean initialized) {
        putMeta(META_INITIALIZED, initialized ? "1" : "0");
    }

    public synchronized long cursor() {
        return parseLong(meta(META_CURSOR, "0"));
    }

    public synchronized void setCursor(long cursor) {
        putMeta(META_CURSOR, String.valueOf(Math.max(0L, cursor)));
    }

    public synchronized long lastAttemptAt() {
        return parseLong(meta(META_LAST_ATTEMPT, "0"));
    }

    public synchronized void setLastAttemptAt(long value) {
        putMeta(META_LAST_ATTEMPT, String.valueOf(Math.max(0L, value)));
    }

    public synchronized void markSuccess(long serverRecords) {
        putMeta(META_LAST_SUCCESS, String.valueOf(System.currentTimeMillis()));
        putMeta(META_SERVER_RECORDS, String.valueOf(Math.max(0L, serverRecords)));
        putMeta(META_STATUS, "SYNCED");
        putMeta(META_MESSAGE, "안전하게 동기화되었습니다.");
    }

    public synchronized void markStatus(String status, String message) {
        putMeta(META_STATUS, status == null ? "" : status);
        putMeta(META_MESSAGE, message == null ? "" : message);
    }

    public synchronized StatusSnapshot status() {
        return new StatusSnapshot(
                meta(META_STATUS, "IDLE"),
                meta(META_MESSAGE, "동기화를 켜면 앱 삭제 후에도 복구할 수 있습니다."),
                parseLong(meta(META_LAST_ATTEMPT, "0")),
                parseLong(meta(META_LAST_SUCCESS, "0")),
                cursor(),
                parseLong(meta(META_SERVER_RECORDS, "0")),
                pendingCount(),
                initialized());
    }

    private String meta(String key, String fallback) {
        try (Cursor cursor = getReadableDatabase().query("sync_meta",
                new String[]{"meta_value"}, "account_key=? AND meta_key=?",
                new String[]{accountKey, key}, null, null, null, "1")) {
            return cursor.moveToFirst() ? cursor.getString(0) : fallback;
        }
    }

    private void putMeta(String key, String value) {
        ContentValues values = new ContentValues();
        values.put("account_key", accountKey);
        values.put("meta_key", key);
        values.put("meta_value", value == null ? "" : value);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(
                "sync_meta", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private int count(String sql, String[] args) {
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private Mapping readMapping(Cursor cursor) {
        return new Mapping(
                cursor.getString(cursor.getColumnIndexOrThrow("entity_type")),
                cursor.getString(cursor.getColumnIndexOrThrow("sync_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("local_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("version")),
                cursor.getString(cursor.getColumnIndexOrThrow("payload_hash")),
                cursor.getString(cursor.getColumnIndexOrThrow("payload_json")),
                cursor.getInt(cursor.getColumnIndexOrThrow("pending")) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow("deleted")) != 0);
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value == null ? "0" : value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public static String accountKey(Context context) {
        String ownerId = AuthSessionStore.ownerId(context).trim();
        if (!ownerId.isEmpty()) return "owner:" + ownerId;
        String email = AuthSessionStore.email(context).trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(email.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("email:");
            for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value));
            return result.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static final class Mapping {
        public final String entityType;
        public final String syncId;
        public final long localId;
        public final int version;
        public final String payloadHash;
        public final String payloadJson;
        public final boolean pending;
        public final boolean deleted;

        Mapping(String entityType, String syncId, long localId, int version,
                String payloadHash, String payloadJson, boolean pending, boolean deleted) {
            this.entityType = entityType;
            this.syncId = syncId;
            this.localId = localId;
            this.version = version;
            this.payloadHash = payloadHash == null ? "" : payloadHash;
            this.payloadJson = payloadJson == null ? "{}" : payloadJson;
            this.pending = pending;
            this.deleted = deleted;
        }
    }

    public static final class PendingItem {
        public final String entityType;
        public final String syncId;
        public final int version;
        public final String payloadJson;
        public final boolean deleted;

        PendingItem(String entityType, String syncId, int version,
                    String payloadJson, boolean deleted) {
            this.entityType = entityType;
            this.syncId = syncId;
            this.version = version;
            this.payloadJson = payloadJson == null ? "{}" : payloadJson;
            this.deleted = deleted;
        }
    }

    public static final class StatusSnapshot {
        public final String status;
        public final String message;
        public final long lastAttemptAt;
        public final long lastSuccessAt;
        public final long cursor;
        public final long serverRecords;
        public final int pendingCount;
        public final boolean initialized;

        StatusSnapshot(String status, String message, long lastAttemptAt, long lastSuccessAt,
                       long cursor, long serverRecords, int pendingCount, boolean initialized) {
            this.status = status;
            this.message = message;
            this.lastAttemptAt = lastAttemptAt;
            this.lastSuccessAt = lastSuccessAt;
            this.cursor = cursor;
            this.serverRecords = serverRecords;
            this.pendingCount = pendingCount;
            this.initialized = initialized;
        }
    }
}