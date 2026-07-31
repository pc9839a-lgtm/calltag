package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
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
    private static final int DB_VERSION = 2;
    private static final String ACTIVE_STATUS_SQL = "('SCHEDULED','READY','SENDING','SENT')";

    private final Context appContext;

    public MessageLogStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
        appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE message_jobs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "customer_id INTEGER NOT NULL DEFAULT 0," +
                "call_log_id INTEGER NOT NULL DEFAULT 0," +
                "schedule_id INTEGER NOT NULL DEFAULT 0," +
                "campaign_id TEXT NOT NULL DEFAULT ''," +
                "template_id TEXT NOT NULL DEFAULT ''," +
                "phone TEXT NOT NULL," +
                "normalized_phone TEXT NOT NULL," +
                "body TEXT NOT NULL," +
                "body_hash TEXT NOT NULL DEFAULT ''," +
                "trigger_type TEXT NOT NULL," +
                "purpose_type TEXT NOT NULL DEFAULT ''," +
                "idempotency_key TEXT NOT NULL DEFAULT ''," +
                "active_dedupe_key TEXT," +
                "duplicate_of_id INTEGER NOT NULL DEFAULT 0," +
                "force_send INTEGER NOT NULL DEFAULT 0," +
                "status TEXT NOT NULL," +
                "scheduled_at INTEGER NOT NULL," +
                "sent_at INTEGER NOT NULL DEFAULT 0," +
                "error TEXT NOT NULL DEFAULT ''," +
                "subscription_id INTEGER NOT NULL DEFAULT -1," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)");
        createIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            addColumnIfMissing(db, "message_jobs", "schedule_id",
                    "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, "message_jobs", "campaign_id",
                    "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "message_jobs", "template_id",
                    "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "message_jobs", "body_hash",
                    "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "message_jobs", "purpose_type",
                    "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "message_jobs", "idempotency_key",
                    "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "message_jobs", "active_dedupe_key", "TEXT");
            addColumnIfMissing(db, "message_jobs", "duplicate_of_id",
                    "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, "message_jobs", "force_send",
                    "INTEGER NOT NULL DEFAULT 0");
            createIndexes(db);
        }
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_status_time " +
                "ON message_jobs(status, scheduled_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_phone_trigger " +
                "ON message_jobs(normalized_phone, trigger_type, created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_body_recent " +
                "ON message_jobs(normalized_phone, body_hash, created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_call_purpose " +
                "ON message_jobs(call_log_id, purpose_type, status)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_schedule_purpose " +
                "ON message_jobs(schedule_id, purpose_type, status)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_campaign_phone " +
                "ON message_jobs(campaign_id, normalized_phone, status)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_message_active_dedupe " +
                "ON message_jobs(active_dedupe_key) WHERE active_dedupe_key IS NOT NULL");
    }

    private void addColumnIfMissing(SQLiteDatabase db, String table, String column,
                                    String definition) {
        if (hasColumn(db, table, column)) return;
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    private boolean hasColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(nameIndex))) return true;
            }
        }
        return false;
    }

    public long createJob(long customerId, long callLogId, String phone,
                          String body, String triggerType, String status,
                          long scheduledAt, int subscriptionId) {
        return createJobAdvanced(customerId, callLogId, 0L, "", "", phone, body,
                triggerType, status, scheduledAt, subscriptionId, false);
    }

    public long createJobAdvanced(long customerId, long callLogId, long scheduleId,
                                  String campaignId, String templateId, String phone,
                                  String body, String triggerType, String status,
                                  long scheduledAt, int subscriptionId,
                                  boolean forceSend) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.length() < 8) throw new IllegalArgumentException("전화번호를 확인해주세요.");
        String message = body == null ? "" : body.trim();
        if (message.isEmpty()) throw new IllegalArgumentException("문자 내용을 입력해주세요.");

        long now = System.currentTimeMillis();
        long safeScheduledAt = Math.max(now, scheduledAt);
        long duplicateWindowMs = MessageAutomationStore.cooldownHours(appContext)
                * 60L * 60L * 1000L;
        MessageDedupeEngine.Metadata metadata = MessageDedupeEngine.metadata(
                callLogId, scheduleId, campaignId, phone, message, triggerType,
                safeScheduledAt, duplicateWindowMs);

        if (!isActiveCreationStatus(status)) {
            return insertJob(getWritableDatabase(), customerId, callLogId, scheduleId,
                    campaignId, templateId, phone, message, triggerType, status,
                    safeScheduledAt, subscriptionId, metadata, null, 0L,
                    forceSend, "", now);
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            DuplicateMatch duplicate = findDuplicate(db, customerId, callLogId, scheduleId,
                    campaignId, templateId, safeScheduledAt, metadata, now);
            if (duplicate != null && !forceSend) {
                long skippedId = insertDuplicateAttempt(db, customerId, callLogId,
                        scheduleId, campaignId, templateId, phone, message, triggerType,
                        safeScheduledAt, subscriptionId, metadata, duplicate, now);
                db.setTransactionSuccessful();
                return skippedId;
            }

            String activeKey = forceSend
                    ? MessageDedupeEngine.forceActiveKey(metadata.idempotencyKey, now)
                    : metadata.idempotencyKey;
            long id;
            try {
                id = insertJob(db, customerId, callLogId, scheduleId, campaignId,
                        templateId, phone, message, triggerType, status,
                        safeScheduledAt, subscriptionId, metadata, activeKey, 0L,
                        forceSend, "", now);
            } catch (SQLiteConstraintException race) {
                DuplicateMatch raced = findByActiveKey(db, metadata.idempotencyKey);
                if (raced == null || forceSend) throw race;
                id = insertDuplicateAttempt(db, customerId, callLogId, scheduleId,
                        campaignId, templateId, phone, message, triggerType,
                        safeScheduledAt, subscriptionId, metadata, raced, now);
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    private long insertDuplicateAttempt(SQLiteDatabase db, long customerId,
                                        long callLogId, long scheduleId,
                                        String campaignId, String templateId,
                                        String phone, String body, String triggerType,
                                        long scheduledAt, int subscriptionId,
                                        MessageDedupeEngine.Metadata metadata,
                                        DuplicateMatch duplicate, long now) {
        String reason = MessageDedupeEngine.duplicateReason(
                duplicate.reason, duplicate.id, duplicate.relevantTime(), duplicate.status);
        return insertJob(db, customerId, callLogId, scheduleId, campaignId,
                templateId, phone, body, triggerType, STATUS_SKIPPED,
                scheduledAt, subscriptionId, metadata, null, duplicate.id,
                false, reason, now);
    }

    private long insertJob(SQLiteDatabase db, long customerId, long callLogId,
                           long scheduleId, String campaignId, String templateId,
                           String phone, String body, String triggerType, String status,
                           long scheduledAt, int subscriptionId,
                           MessageDedupeEngine.Metadata metadata,
                           String activeDedupeKey, long duplicateOfId,
                           boolean forceSend, String error, long now) {
        ContentValues values = new ContentValues();
        values.put("customer_id", Math.max(0L, customerId));
        values.put("call_log_id", Math.max(0L, callLogId));
        values.put("schedule_id", Math.max(0L, scheduleId));
        values.put("campaign_id", safe(campaignId).trim());
        values.put("template_id", safe(templateId).trim());
        values.put("phone", phone == null ? metadata.normalizedPhone : phone.trim());
        values.put("normalized_phone", metadata.normalizedPhone);
        values.put("body", body.trim());
        values.put("body_hash", metadata.bodyHash);
        values.put("trigger_type", cleanTrigger(triggerType));
        values.put("purpose_type", metadata.purposeType);
        values.put("idempotency_key", metadata.idempotencyKey);
        if (activeDedupeKey == null || activeDedupeKey.trim().isEmpty()) {
            values.putNull("active_dedupe_key");
        } else {
            values.put("active_dedupe_key", activeDedupeKey);
        }
        values.put("duplicate_of_id", Math.max(0L, duplicateOfId));
        values.put("force_send", forceSend ? 1 : 0);
        values.put("status", status);
        values.put("scheduled_at", scheduledAt);
        values.put("sent_at", 0L);
        values.put("error", safe(error));
        values.put("subscription_id", subscriptionId);
        values.put("created_at", now);
        values.put("updated_at", now);
        return db.insertOrThrow("message_jobs", null, values);
    }

    private DuplicateMatch findDuplicate(SQLiteDatabase db, long customerId,
                                         long callLogId, long scheduleId,
                                         String campaignId, String templateId,
                                         long scheduledAt,
                                         MessageDedupeEngine.Metadata metadata,
                                         long now) {
        DuplicateMatch exact = findByActiveKey(db, metadata.idempotencyKey);
        if (exact != null) {
            exact.reason = "같은 발송 컨텍스트의 요청이 이미 처리되었습니다";
            return exact;
        }

        if (callLogId > 0L) {
            DuplicateMatch call = queryDuplicate(db,
                    "call_log_id=? AND purpose_type=? AND status IN " + ACTIVE_STATUS_SQL,
                    new String[]{String.valueOf(callLogId), metadata.purposeType},
                    "같은 통화에서 같은 목적의 문자가 이미 처리되었습니다");
            if (call != null) return call;
        }

        if (scheduleId > 0L) {
            DuplicateMatch schedule = queryDuplicate(db,
                    "schedule_id=? AND purpose_type=? AND status IN " + ACTIVE_STATUS_SQL,
                    new String[]{String.valueOf(scheduleId), metadata.purposeType},
                    "같은 일정에 연결된 후속문자가 이미 존재합니다");
            if (schedule != null) return schedule;
        }

        String safeCampaignId = safe(campaignId).trim();
        if (!safeCampaignId.isEmpty()) {
            DuplicateMatch campaign = queryDuplicate(db,
                    "campaign_id=? AND normalized_phone=? AND status IN " + ACTIVE_STATUS_SQL,
                    new String[]{safeCampaignId, metadata.normalizedPhone},
                    "같은 캠페인에서 동일 번호가 이미 발송 대상에 포함되었습니다");
            if (campaign != null) return campaign;
        }

        long since = now - metadata.duplicateWindowMs;
        String safeTemplateId = safe(templateId).trim();
        if (!safeTemplateId.isEmpty()) {
            DuplicateMatch template = queryDuplicate(db,
                    "normalized_phone=? AND template_id=? AND created_at>=? " +
                            "AND status IN " + ACTIVE_STATUS_SQL,
                    new String[]{metadata.normalizedPhone, safeTemplateId, String.valueOf(since)},
                    "같은 고객에게 동일 템플릿을 제한 시간 안에 이미 사용했습니다");
            if (template != null) return template;
        }

        DuplicateMatch body = queryDuplicate(db,
                "normalized_phone=? AND body_hash=? AND created_at>=? " +
                        "AND status IN " + ACTIVE_STATUS_SQL,
                new String[]{metadata.normalizedPhone, metadata.bodyHash, String.valueOf(since)},
                "같은 고객에게 동일한 문자 내용을 제한 시간 안에 이미 처리했습니다");
        if (body != null) return body;

        return queryDuplicate(db,
                "normalized_phone=? AND body_hash=? AND ABS(scheduled_at-?)<=? " +
                        "AND status IN " + ACTIVE_STATUS_SQL,
                new String[]{metadata.normalizedPhone, metadata.bodyHash,
                        String.valueOf(scheduledAt),
                        String.valueOf(MessageDedupeEngine.SCHEDULE_COLLISION_MS)},
                "예약문자와 즉시문자의 발송 시점이 겹칩니다");
    }

    private DuplicateMatch findByActiveKey(SQLiteDatabase db, String activeKey) {
        if (safe(activeKey).trim().isEmpty()) return null;
        return queryDuplicate(db,
                "active_dedupe_key=? AND status IN " + ACTIVE_STATUS_SQL,
                new String[]{activeKey}, "같은 멱등키 요청이 이미 처리되었습니다");
    }

    private DuplicateMatch queryDuplicate(SQLiteDatabase db, String where,
                                          String[] args, String reason) {
        String sql = "SELECT id,status,created_at,scheduled_at,sent_at FROM message_jobs " +
                "WHERE " + where + " ORDER BY id DESC LIMIT 1";
        try (Cursor cursor = db.rawQuery(sql, args)) {
            if (!cursor.moveToFirst()) return null;
            return new DuplicateMatch(cursor.getLong(0), cursor.getString(1),
                    cursor.getLong(2), cursor.getLong(3), cursor.getLong(4), reason);
        }
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
                "AND created_at>=? AND status IN " + ACTIVE_STATUS_SQL + " LIMIT 1";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql,
                new String[]{normalized, triggerType, String.valueOf(since)})) {
            return cursor.moveToFirst();
        }
    }

    public boolean hasActiveImmediateForCall(long callLogId) {
        if (callLogId <= 0L) return false;
        String sql = "SELECT 1 FROM message_jobs WHERE call_log_id=? " +
                "AND purpose_type=? AND status IN " + ACTIVE_STATUS_SQL + " LIMIT 1";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql,
                new String[]{String.valueOf(callLogId),
                        MessageDedupeEngine.PURPOSE_CALL_IMMEDIATE})) {
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
        updateStatus(id, STATUS_READY, "", 0L, false);
    }

    public void markSending(long id) {
        updateStatus(id, STATUS_SENDING, "", 0L, false);
    }

    public void markSent(long id) {
        updateStatus(id, STATUS_SENT, "", System.currentTimeMillis(), false);
    }

    public void markFailed(long id, String error) {
        updateStatus(id, STATUS_FAILED, error, 0L, true);
    }

    public void markSkipped(long id, String reason) {
        updateStatus(id, STATUS_SKIPPED, reason, 0L, true);
    }

    public void cancel(long id, String reason) {
        updateStatus(id, STATUS_CANCELLED, reason, 0L, true);
    }

    public int cancelScheduledForPhone(String phone, String triggerType, String reason) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        ContentValues values = new ContentValues();
        values.put("status", STATUS_CANCELLED);
        values.put("error", reason == null ? "" : reason);
        values.putNull("active_dedupe_key");
        values.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().update(
                "message_jobs",
                values,
                "normalized_phone=? AND trigger_type=? AND status=?",
                new String[]{normalized, triggerType, STATUS_SCHEDULED});
    }

    public boolean isSendable(long id) {
        MessageRecord record = find(id);
        return record != null && (STATUS_READY.equals(record.status)
                || STATUS_SCHEDULED.equals(record.status));
    }

    private void updateStatus(long id, String status, String error,
                              long sentAt, boolean clearDedupeKey) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("error", error == null ? "" : error);
        if (sentAt > 0L) values.put("sent_at", sentAt);
        if (clearDedupeKey) values.putNull("active_dedupe_key");
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

    private boolean isActiveCreationStatus(String status) {
        return STATUS_SCHEDULED.equals(status) || STATUS_READY.equals(status);
    }

    private String cleanTrigger(String triggerType) {
        String result = safe(triggerType).trim();
        return result.isEmpty() ? MessageAutomationManager.TRIGGER_MANUAL : result;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class DuplicateMatch {
        final long id;
        final String status;
        final long createdAt;
        final long scheduledAt;
        final long sentAt;
        String reason;

        DuplicateMatch(long id, String status, long createdAt,
                       long scheduledAt, long sentAt, String reason) {
            this.id = id;
            this.status = status;
            this.createdAt = createdAt;
            this.scheduledAt = scheduledAt;
            this.sentAt = sentAt;
            this.reason = reason;
        }

        long relevantTime() {
            if (sentAt > 0L) return sentAt;
            if (STATUS_SCHEDULED.equals(status) && scheduledAt > 0L) return scheduledAt;
            return createdAt;
        }
    }
}
