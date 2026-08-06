package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CallTagSyncDataAdapter {
    private static final Set<String> SUPPORTED = Set.of(
            "customer", "interaction", "task", "stage");

    private CallTagSyncDataAdapter() {}

    public static int localEntityCount(Context context) {
        CallTagDbHelper helper = new CallTagDbHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        return count(db, "customers")
                + count(db, "interactions")
                + count(db, "follow_up_tasks")
                + count(db, "crm_stages");
    }

    public static ScanResult scanLocal(Context context, CallTagSyncLocalStore store) throws Exception {
        if (!store.isReady()) return new ScanResult(0, 0);
        CallTagDbHelper helper = new CallTagDbHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        Set<String> seen = new HashSet<>();
        int scanned = 0;

        scanned += scanCustomers(db, store, seen);
        scanned += scanStages(db, store, seen);
        scanned += scanInteractions(db, store, seen);
        scanned += scanTasks(db, store, seen);

        int deleted = 0;
        for (CallTagSyncLocalStore.Mapping mapping : store.listMappings()) {
            if (!SUPPORTED.contains(mapping.entityType) || mapping.deleted) continue;
            String key = key(mapping.entityType, mapping.localId);
            if (seen.contains(key)) continue;
            store.markLocal(mapping.entityType, mapping.localId,
                    "deleted", "{}", true);
            deleted++;
        }
        return new ScanResult(scanned, deleted);
    }

    public static ApplyResult applyRemote(
            Context context,
            JSONArray rawItems,
            CallTagSyncLocalStore store) throws Exception {
        List<JSONObject> items = new ArrayList<>();
        for (int index = 0; index < rawItems.length(); index++) {
            JSONObject item = rawItems.optJSONObject(index);
            if (item != null && SUPPORTED.contains(item.optString("entityType", ""))) {
                items.add(item);
            }
        }
        items.sort(Comparator.comparingInt(item -> priority(item.optString("entityType", ""))));

        CallTagDbHelper helper = new CallTagDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        int applied = 0;
        int conflicts = 0;
        List<JSONObject> deferred = new ArrayList<>();

        for (JSONObject item : items) {
            ApplyOutcome outcome = applyOne(db, store, item);
            if (outcome == ApplyOutcome.APPLIED) applied++;
            else if (outcome == ApplyOutcome.CONFLICT) conflicts++;
            else if (outcome == ApplyOutcome.DEFERRED) deferred.add(item);
        }
        for (JSONObject item : deferred) {
            ApplyOutcome outcome = applyOne(db, store, item);
            if (outcome == ApplyOutcome.APPLIED) applied++;
            else if (outcome == ApplyOutcome.CONFLICT) conflicts++;
            else throw new IllegalStateException("연결된 고객 데이터를 먼저 복구하지 못했습니다.");
        }
        return new ApplyResult(applied, conflicts);
    }

    private static int scanCustomers(SQLiteDatabase db, CallTagSyncLocalStore store,
                                     Set<String> seen) throws Exception {
        int count = 0;
        try (Cursor cursor = db.query("customers", new String[]{
                "id", "display_name", "primary_phone", "relation_status", "source", "memo",
                "first_contact_at", "last_contact_at", "created_at", "updated_at"
        }, null, null, null, null, "id ASC")) {
            while (cursor.moveToNext()) {
                long localId = cursor.getLong(0);
                CallTagSyncLocalStore.Mapping mapping = store.ensureMapping("customer", localId);
                JSONObject payload = new JSONObject();
                payload.put("displayName", cursor.getString(1));
                payload.put("primaryPhone", cursor.getString(2));
                payload.put("relationStatus", cursor.getString(3));
                payload.put("source", safe(cursor.getString(4)));
                payload.put("memo", safe(cursor.getString(5)));
                payload.put("firstContactAt", cursor.getLong(6));
                payload.put("lastContactAt", cursor.getLong(7));
                payload.put("createdAt", cursor.getLong(8));
                payload.put("updatedAt", cursor.getLong(9));
                store.markLocal("customer", localId, hash(payload), payload.toString(), false);
                seen.add(key("customer", localId));
                count++;
            }
        }
        return count;
    }

    private static int scanStages(SQLiteDatabase db, CallTagSyncLocalStore store,
                                  Set<String> seen) throws Exception {
        int count = 0;
        try (Cursor cursor = db.query("crm_stages", new String[]{
                "id", "name", "position", "color", "created_at", "updated_at"
        }, null, null, null, null, "position ASC,id ASC")) {
            while (cursor.moveToNext()) {
                long localId = cursor.getLong(0);
                store.ensureMapping("stage", localId);
                JSONObject payload = new JSONObject();
                payload.put("name", cursor.getString(1));
                payload.put("position", cursor.getInt(2));
                payload.put("color", cursor.getString(3));
                payload.put("createdAt", cursor.getLong(4));
                payload.put("updatedAt", cursor.getLong(5));
                store.markLocal("stage", localId, hash(payload), payload.toString(), false);
                seen.add(key("stage", localId));
                count++;
            }
        }
        return count;
    }

    private static int scanInteractions(SQLiteDatabase db, CallTagSyncLocalStore store,
                                        Set<String> seen) throws Exception {
        int count = 0;
        try (Cursor cursor = db.query("interactions", new String[]{
                "id", "customer_id", "opportunity_id", "type", "started_at", "ended_at",
                "duration_sec", "result", "note", "created_at"
        }, null, null, null, null, "id ASC")) {
            while (cursor.moveToNext()) {
                long localId = cursor.getLong(0);
                long customerLocalId = cursor.getLong(1);
                CallTagSyncLocalStore.Mapping customer =
                        store.ensureMapping("customer", customerLocalId);
                store.ensureMapping("interaction", localId);
                JSONObject payload = new JSONObject();
                payload.put("customerId", customer.syncId);
                if (!cursor.isNull(2)) payload.put("opportunityId", cursor.getLong(2));
                payload.put("type", cursor.getString(3));
                payload.put("startedAt", cursor.getLong(4));
                payload.put("endedAt", cursor.isNull(5) ? JSONObject.NULL : cursor.getLong(5));
                payload.put("durationSec", cursor.getLong(6));
                payload.put("result", safe(cursor.getString(7)));
                payload.put("note", safe(cursor.getString(8)));
                payload.put("createdAt", cursor.getLong(9));
                payload.put("updatedAt", cursor.getLong(9));
                store.markLocal("interaction", localId,
                        hash(payload), payload.toString(), false);
                seen.add(key("interaction", localId));
                count++;
            }
        }
        return count;
    }

    private static int scanTasks(SQLiteDatabase db, CallTagSyncLocalStore store,
                                 Set<String> seen) throws Exception {
        int count = 0;
        try (Cursor cursor = db.query("follow_up_tasks", new String[]{
                "id", "customer_id", "opportunity_id", "interaction_id", "task_type", "title",
                "due_at", "status", "completed_at", "created_at"
        }, null, null, null, null, "id ASC")) {
            while (cursor.moveToNext()) {
                long localId = cursor.getLong(0);
                CallTagSyncLocalStore.Mapping customer =
                        store.ensureMapping("customer", cursor.getLong(1));
                store.ensureMapping("task", localId);
                JSONObject payload = new JSONObject();
                payload.put("customerId", customer.syncId);
                if (!cursor.isNull(2)) payload.put("opportunityId", cursor.getLong(2));
                if (!cursor.isNull(3)) {
                    CallTagSyncLocalStore.Mapping interaction =
                            store.ensureMapping("interaction", cursor.getLong(3));
                    payload.put("interactionId", interaction.syncId);
                }
                payload.put("taskType", cursor.getString(4));
                payload.put("title", cursor.getString(5));
                payload.put("dueAt", cursor.getLong(6));
                payload.put("status", cursor.getString(7));
                payload.put("completedAt", cursor.isNull(8) ? JSONObject.NULL : cursor.getLong(8));
                payload.put("createdAt", cursor.getLong(9));
                payload.put("updatedAt", cursor.isNull(8)
                        ? Math.max(cursor.getLong(6), cursor.getLong(9))
                        : Math.max(cursor.getLong(8), cursor.getLong(9)));
                store.markLocal("task", localId, hash(payload), payload.toString(), false);
                seen.add(key("task", localId));
                count++;
            }
        }
        return count;
    }

    private static ApplyOutcome applyOne(
            SQLiteDatabase db,
            CallTagSyncLocalStore store,
            JSONObject item) throws Exception {
        String type = item.optString("entityType", "");
        String syncId = item.optString("entityId", "");
        int version = item.optInt("version", 0);
        boolean deleted = item.optBoolean("deleted", false);
        if (!SUPPORTED.contains(type) || syncId.isEmpty() || version < 1) {
            return ApplyOutcome.SKIPPED;
        }

        CallTagSyncLocalStore.Mapping mapping = store.mappingBySync(type, syncId);
        if (mapping != null && mapping.pending && mapping.version >= version) {
            return ApplyOutcome.CONFLICT;
        }
        if (mapping != null && mapping.version > version) return ApplyOutcome.SKIPPED;

        if (deleted) {
            long localId = mapping == null ? negativeLocalId(syncId) : mapping.localId;
            if (mapping != null && mapping.localId > 0L) deleteLocal(db, type, mapping.localId);
            store.markRemote(type, syncId, localId, version, "deleted", "{}", true);
            return ApplyOutcome.APPLIED;
        }

        JSONObject payload = item.optJSONObject("payload");
        if (payload == null) return ApplyOutcome.SKIPPED;
        long localId;
        if ("customer".equals(type)) {
            localId = upsertCustomer(db, mapping, payload);
        } else if ("stage".equals(type)) {
            localId = upsertStage(db, mapping, payload);
        } else if ("interaction".equals(type)) {
            localId = upsertInteraction(db, store, mapping, payload);
            if (localId <= 0L) return ApplyOutcome.DEFERRED;
        } else if ("task".equals(type)) {
            localId = upsertTask(db, store, mapping, payload);
            if (localId <= 0L) return ApplyOutcome.DEFERRED;
        } else {
            return ApplyOutcome.SKIPPED;
        }
        store.markRemote(type, syncId, localId, version,
                hash(payload), payload.toString(), false);
        return ApplyOutcome.APPLIED;
    }

    private static long upsertCustomer(SQLiteDatabase db,
                                       CallTagSyncLocalStore.Mapping mapping,
                                       JSONObject payload) {
        long localId = validLocalId(db, "customers", mapping);
        String phone = payload.optString("primaryPhone", "").trim();
        if (localId <= 0L && !phone.isEmpty()) {
            String normalized = PhoneNumberNormalizer.normalize(phone);
            try (Cursor cursor = db.query("customers", new String[]{"id"},
                    "normalized_phone=?", new String[]{normalized},
                    null, null, null, "1")) {
                if (cursor.moveToFirst()) localId = cursor.getLong(0);
            }
        }
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("display_name", fallback(payload.optString("displayName", ""), "이름 없음"));
        values.put("primary_phone", phone);
        values.put("normalized_phone", PhoneNumberNormalizer.normalize(phone));
        values.put("relation_status", fallback(payload.optString("relationStatus", ""), CallTagDbHelper.STATUS_NEW));
        values.put("source", payload.optString("source", ""));
        values.put("memo", payload.optString("memo", ""));
        values.put("first_contact_at", payload.optLong("firstContactAt", now));
        values.put("last_contact_at", payload.optLong("lastContactAt", now));
        values.put("created_at", payload.optLong("createdAt", now));
        values.put("updated_at", payload.optLong("updatedAt", now));
        if (localId > 0L) {
            db.update("customers", values, "id=?", new String[]{String.valueOf(localId)});
            return localId;
        }
        return db.insertOrThrow("customers", null, values);
    }

    private static long upsertStage(SQLiteDatabase db,
                                    CallTagSyncLocalStore.Mapping mapping,
                                    JSONObject payload) {
        long localId = validLocalId(db, "crm_stages", mapping);
        String name = fallback(payload.optString("name", ""), "고객 상태");
        if (localId <= 0L) {
            try (Cursor cursor = db.query("crm_stages", new String[]{"id"},
                    "name=?", new String[]{name}, null, null, null, "1")) {
                if (cursor.moveToFirst()) localId = cursor.getLong(0);
            }
        }
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("position", payload.optInt("position", 0));
        values.put("color", fallback(payload.optString("color", ""), "#4389FF"));
        values.put("created_at", payload.optLong("createdAt", now));
        values.put("updated_at", payload.optLong("updatedAt", now));
        if (localId > 0L) {
            db.update("crm_stages", values, "id=?", new String[]{String.valueOf(localId)});
            return localId;
        }
        return db.insertOrThrow("crm_stages", null, values);
    }

    private static long upsertInteraction(SQLiteDatabase db,
                                          CallTagSyncLocalStore store,
                                          CallTagSyncLocalStore.Mapping mapping,
                                          JSONObject payload) {
        CallTagSyncLocalStore.Mapping customer =
                store.mappingBySync("customer", payload.optString("customerId", ""));
        if (customer == null || customer.localId <= 0L || !exists(db, "customers", customer.localId)) {
            return 0L;
        }
        long localId = validLocalId(db, "interactions", mapping);
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("customer_id", customer.localId);
        values.put("type", fallback(payload.optString("type", ""), "CALL"));
        values.put("started_at", payload.optLong("startedAt", now));
        if (payload.isNull("endedAt")) values.putNull("ended_at");
        else values.put("ended_at", payload.optLong("endedAt", now));
        values.put("duration_sec", payload.optLong("durationSec", 0L));
        values.put("result", payload.optString("result", ""));
        values.put("note", payload.optString("note", ""));
        values.put("created_at", payload.optLong("createdAt", now));
        if (localId > 0L) {
            db.update("interactions", values, "id=?", new String[]{String.valueOf(localId)});
            return localId;
        }
        return db.insertOrThrow("interactions", null, values);
    }

    private static long upsertTask(SQLiteDatabase db,
                                   CallTagSyncLocalStore store,
                                   CallTagSyncLocalStore.Mapping mapping,
                                   JSONObject payload) {
        CallTagSyncLocalStore.Mapping customer =
                store.mappingBySync("customer", payload.optString("customerId", ""));
        if (customer == null || customer.localId <= 0L || !exists(db, "customers", customer.localId)) {
            return 0L;
        }
        long localId = validLocalId(db, "follow_up_tasks", mapping);
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("customer_id", customer.localId);
        String interactionSyncId = payload.optString("interactionId", "");
        if (!interactionSyncId.isEmpty()) {
            CallTagSyncLocalStore.Mapping interaction =
                    store.mappingBySync("interaction", interactionSyncId);
            if (interaction == null || interaction.localId <= 0L) return 0L;
            values.put("interaction_id", interaction.localId);
        }
        values.put("task_type", fallback(payload.optString("taskType", ""), "CALL"));
        values.put("title", fallback(payload.optString("title", ""), "다시 연락"));
        values.put("due_at", payload.optLong("dueAt", now));
        values.put("status", fallback(payload.optString("status", ""), "PENDING"));
        if (payload.isNull("completedAt")) values.putNull("completed_at");
        else values.put("completed_at", payload.optLong("completedAt", now));
        values.put("created_at", payload.optLong("createdAt", now));
        if (localId > 0L) {
            db.update("follow_up_tasks", values, "id=?", new String[]{String.valueOf(localId)});
            return localId;
        }
        return db.insertOrThrow("follow_up_tasks", null, values);
    }

    private static void deleteLocal(SQLiteDatabase db, String type, long localId) {
        String table = table(type);
        if (!table.isEmpty()) db.delete(table, "id=?", new String[]{String.valueOf(localId)});
    }

    private static long validLocalId(SQLiteDatabase db, String table,
                                     CallTagSyncLocalStore.Mapping mapping) {
        if (mapping == null || mapping.localId <= 0L) return 0L;
        return exists(db, table, mapping.localId) ? mapping.localId : 0L;
    }

    private static boolean exists(SQLiteDatabase db, String table, long localId) {
        try (Cursor cursor = db.query(table, new String[]{"id"}, "id=?",
                new String[]{String.valueOf(localId)}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    private static int count(SQLiteDatabase db, String table) {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private static String table(String type) {
        if ("customer".equals(type)) return "customers";
        if ("interaction".equals(type)) return "interactions";
        if ("task".equals(type)) return "follow_up_tasks";
        if ("stage".equals(type)) return "crm_stages";
        return "";
    }

    private static int priority(String type) {
        if ("customer".equals(type)) return 1;
        if ("stage".equals(type)) return 2;
        if ("interaction".equals(type)) return 3;
        if ("task".equals(type)) return 4;
        return 9;
    }

    private static String hash(JSONObject payload) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder value = new StringBuilder();
        for (byte item : bytes) value.append(String.format(Locale.ROOT, "%02x", item & 0xff));
        return value.toString();
    }

    private static long negativeLocalId(String syncId) {
        long value = Math.abs((long) syncId.hashCode()) + 1L;
        return -value;
    }

    private static String key(String type, long localId) {
        return type + ":" + localId;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private enum ApplyOutcome { APPLIED, CONFLICT, DEFERRED, SKIPPED }

    public static final class ScanResult {
        public final int scanned;
        public final int deleted;

        ScanResult(int scanned, int deleted) {
            this.scanned = scanned;
            this.deleted = deleted;
        }
    }

    public static final class ApplyResult {
        public final int applied;
        public final int conflicts;

        ApplyResult(int applied, int conflicts) {
            this.applied = applied;
            this.conflicts = conflicts;
        }
    }
}