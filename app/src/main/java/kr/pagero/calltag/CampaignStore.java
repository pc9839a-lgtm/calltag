package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CampaignStore extends SQLiteOpenHelper {
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final String DB_NAME = "calltag_campaigns.db";
    private static final int DB_VERSION = 2;

    public CampaignStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE campaigns (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "group_id TEXT NOT NULL," +
                "group_name TEXT NOT NULL," +
                "template_id TEXT NOT NULL DEFAULT ''," +
                "template_name TEXT NOT NULL DEFAULT ''," +
                "body_template TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "subscription_id INTEGER NOT NULL DEFAULT -1," +
                "pause_reason TEXT NOT NULL DEFAULT ''," +
                "consecutive_failures INTEGER NOT NULL DEFAULT 0," +
                "scheduled_at INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE campaign_recipients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "campaign_id TEXT NOT NULL," +
                "customer_id INTEGER NOT NULL DEFAULT 0," +
                "customer_name TEXT NOT NULL DEFAULT ''," +
                "phone TEXT NOT NULL," +
                "normalized_phone TEXT NOT NULL," +
                "body TEXT NOT NULL," +
                "message_id INTEGER NOT NULL DEFAULT 0," +
                "status TEXT NOT NULL," +
                "reason TEXT NOT NULL DEFAULT ''," +
                "scheduled_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL," +
                "UNIQUE(campaign_id,normalized_phone)," +
                "FOREIGN KEY(campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE)");
        createIndexes(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            addColumnIfMissing(db, "campaigns", "subscription_id",
                    "INTEGER NOT NULL DEFAULT -1");
            addColumnIfMissing(db, "campaigns", "pause_reason",
                    "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "campaigns", "consecutive_failures",
                    "INTEGER NOT NULL DEFAULT 0");
            createIndexes(db);
        }
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_created ON campaigns(created_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_status ON campaigns(status,updated_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_recipient_status ON campaign_recipients(campaign_id,status)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_recipient_message ON campaign_recipients(message_id)");
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

    public Campaign create(String name, MessageGroupStore.Group group,
                           String templateId, String templateName,
                           String bodyTemplate, long scheduledAt,
                           int subscriptionId) {
        String safeName = clean(name, "단체문자 캠페인");
        if (group == null) throw new IllegalArgumentException("수신자 그룹을 선택해주세요.");
        if (safe(bodyTemplate).trim().isEmpty()) {
            throw new IllegalArgumentException("문자 내용을 입력해주세요.");
        }
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("name", safeName);
        values.put("group_id", group.id);
        values.put("group_name", group.name);
        values.put("template_id", safe(templateId));
        values.put("template_name", safe(templateName));
        values.put("body_template", bodyTemplate.trim());
        values.put("status", STATUS_SCHEDULED);
        values.put("subscription_id", subscriptionId);
        values.put("pause_reason", "");
        values.put("consecutive_failures", 0);
        values.put("scheduled_at", Math.max(now, scheduledAt));
        values.put("created_at", now);
        values.put("updated_at", now);
        getWritableDatabase().insertOrThrow("campaigns", null, values);
        return find(id);
    }

    public long addRecipient(String campaignId, Customer customer, String phone, String body,
                             long messageId, String status, String reason, long scheduledAt) {
        ContentValues values = new ContentValues();
        values.put("campaign_id", campaignId);
        values.put("customer_id", customer == null ? 0L : customer.id);
        values.put("customer_name", customer == null ? "" : customer.displayName);
        values.put("phone", safe(phone).trim());
        values.put("normalized_phone", PhoneNumberNormalizer.normalize(phone));
        values.put("body", safe(body));
        values.put("message_id", Math.max(0L, messageId));
        values.put("status", safe(status));
        values.put("reason", safe(reason));
        values.put("scheduled_at", scheduledAt);
        values.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().insertWithOnConflict("campaign_recipients", null,
                values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void replaceRecipientJob(long recipientId, long messageId, String status,
                                    String reason, long scheduledAt) {
        ContentValues values = new ContentValues();
        values.put("message_id", Math.max(0L, messageId));
        values.put("status", safe(status));
        values.put("reason", safe(reason));
        values.put("scheduled_at", scheduledAt);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("campaign_recipients", values, "id=?",
                new String[]{String.valueOf(recipientId)});
    }

    public Campaign find(String id) {
        if (safe(id).trim().isEmpty()) return null;
        try (Cursor cursor = getReadableDatabase().query("campaigns", null,
                "id=?", new String[]{id}, null, null, null, "1")) {
            return cursor.moveToFirst() ? readCampaign(cursor) : null;
        }
    }

    public List<Campaign> list() {
        List<Campaign> rows = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("campaigns", null,
                null, null, null, null, "created_at DESC")) {
            while (cursor.moveToNext()) rows.add(readCampaign(cursor));
        }
        return rows;
    }

    public List<Recipient> recipients(String campaignId) {
        List<Recipient> rows = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("campaign_recipients", null,
                "campaign_id=?", new String[]{campaignId}, null, null,
                "scheduled_at ASC,id ASC")) {
            while (cursor.moveToNext()) rows.add(readRecipient(cursor));
        }
        return rows;
    }

    public Campaign findBlockingCampaign(String exceptCampaignId) {
        String except = safe(exceptCampaignId).trim();
        for (Campaign campaign : list()) {
            if (campaign.id.equals(except)) continue;
            if (!STATUS_SCHEDULED.equals(campaign.status)
                    && !STATUS_RUNNING.equals(campaign.status)
                    && !STATUS_PAUSED.equals(campaign.status)) continue;
            if (counts(campaign.id).active > 0) return campaign;
        }
        return null;
    }

    public Campaign findRunningCampaign(String exceptCampaignId) {
        String except = safe(exceptCampaignId).trim();
        for (Campaign campaign : list()) {
            if (campaign.id.equals(except)) continue;
            if (STATUS_RUNNING.equals(campaign.status) && counts(campaign.id).active > 0) {
                return campaign;
            }
        }
        return null;
    }

    public Campaign sync(Context context, String campaignId) {
        MessageLogStore messages = new MessageLogStore(context);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Recipient recipient : recipients(campaignId)) {
                if (recipient.messageId <= 0L) continue;
                MessageRecord record = messages.find(recipient.messageId);
                if (record == null) continue;
                if (record.status.equals(recipient.status)
                        && safe(record.error).equals(recipient.reason)) continue;
                ContentValues values = new ContentValues();
                values.put("status", record.status);
                values.put("reason", safe(record.error));
                values.put("scheduled_at", record.scheduledAt);
                values.put("updated_at", System.currentTimeMillis());
                db.update("campaign_recipients", values, "id=?",
                        new String[]{String.valueOf(recipient.id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            messages.close();
        }
        updateCampaignStatus(campaignId);
        return find(campaignId);
    }

    public void updateCampaignStatus(String campaignId) {
        Campaign current = find(campaignId);
        if (current == null) return;
        Counts counts = counts(campaignId);
        if (STATUS_PAUSED.equals(current.status) && counts.active > 0) return;

        String status;
        if (counts.total == 0) status = STATUS_PARTIAL;
        else if (counts.active > 0) status = counts.sent > 0 ? STATUS_RUNNING : STATUS_SCHEDULED;
        else if (counts.sent == counts.total) status = STATUS_COMPLETED;
        else if (counts.cancelled == counts.total) status = STATUS_CANCELLED;
        else status = STATUS_PARTIAL;
        ContentValues values = new ContentValues();
        values.put("status", status);
        if (!STATUS_PAUSED.equals(status)) values.put("pause_reason", "");
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("campaigns", values, "id=?", new String[]{campaignId});
    }

    public void setPaused(String campaignId, String reason) {
        ContentValues values = new ContentValues();
        values.put("status", STATUS_PAUSED);
        values.put("pause_reason", safe(reason));
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("campaigns", values, "id=?", new String[]{campaignId});
    }

    public void setResumed(String campaignId, int subscriptionId, long scheduledAt) {
        ContentValues values = new ContentValues();
        values.put("status", STATUS_SCHEDULED);
        values.put("subscription_id", subscriptionId);
        values.put("pause_reason", "");
        values.put("consecutive_failures", 0);
        values.put("scheduled_at", scheduledAt);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("campaigns", values, "id=?", new String[]{campaignId});
    }

    public void setRunning(String campaignId) {
        ContentValues values = new ContentValues();
        values.put("status", STATUS_RUNNING);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("campaigns", values,
                "id=? AND status=?", new String[]{campaignId, STATUS_SCHEDULED});
    }

    public void updateSubscription(String campaignId, int subscriptionId) {
        ContentValues values = new ContentValues();
        values.put("subscription_id", subscriptionId);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("campaigns", values, "id=?", new String[]{campaignId});
    }

    public int incrementConsecutiveFailures(String campaignId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE campaigns SET consecutive_failures=consecutive_failures+1,updated_at=? WHERE id=?",
                new Object[]{System.currentTimeMillis(), campaignId});
        Campaign campaign = find(campaignId);
        return campaign == null ? 0 : campaign.consecutiveFailures;
    }

    public void resetConsecutiveFailures(String campaignId) {
        ContentValues values = new ContentValues();
        values.put("consecutive_failures", 0);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("campaigns", values,
                "id=? AND consecutive_failures<>0", new String[]{campaignId});
    }

    public Counts counts(String campaignId) {
        Counts counts = new Counts();
        String sql = "SELECT status,COUNT(*) FROM campaign_recipients WHERE campaign_id=? GROUP BY status";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, new String[]{campaignId})) {
            while (cursor.moveToNext()) {
                String status = cursor.getString(0);
                int count = cursor.getInt(1);
                counts.total += count;
                if (MessageLogStore.STATUS_SENT.equals(status)) counts.sent += count;
                else if (MessageLogStore.STATUS_FAILED.equals(status)) counts.failed += count;
                else if (MessageLogStore.STATUS_SKIPPED.equals(status)) counts.skipped += count;
                else if (MessageLogStore.STATUS_CANCELLED.equals(status)) counts.cancelled += count;
                else counts.active += count;
            }
        }
        return counts;
    }

    public boolean delete(String campaignId) {
        return getWritableDatabase().delete("campaigns", "id=?", new String[]{campaignId}) > 0;
    }

    private Campaign readCampaign(Cursor cursor) {
        return new Campaign(
                cursor.getString(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("group_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("group_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("template_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("template_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("body_template")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getInt(cursor.getColumnIndexOrThrow("subscription_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("pause_reason")),
                cursor.getInt(cursor.getColumnIndexOrThrow("consecutive_failures")),
                cursor.getLong(cursor.getColumnIndexOrThrow("scheduled_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")));
    }

    private Recipient readRecipient(Cursor cursor) {
        return new Recipient(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("campaign_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("customer_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("customer_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("body")),
                cursor.getLong(cursor.getColumnIndexOrThrow("message_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getString(cursor.getColumnIndexOrThrow("reason")),
                cursor.getLong(cursor.getColumnIndexOrThrow("scheduled_at")));
    }

    private static String clean(String value, String fallback) {
        String result = safe(value).trim().replaceAll("\\s+", " ");
        return result.isEmpty() ? fallback : result;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Campaign {
        public final String id;
        public final String name;
        public final String groupId;
        public final String groupName;
        public final String templateId;
        public final String templateName;
        public final String bodyTemplate;
        public final String status;
        public final int subscriptionId;
        public final String pauseReason;
        public final int consecutiveFailures;
        public final long scheduledAt;
        public final long createdAt;
        public final long updatedAt;

        Campaign(String id, String name, String groupId, String groupName,
                 String templateId, String templateName, String bodyTemplate,
                 String status, int subscriptionId, String pauseReason,
                 int consecutiveFailures, long scheduledAt, long createdAt,
                 long updatedAt) {
            this.id = id;
            this.name = name;
            this.groupId = groupId;
            this.groupName = groupName;
            this.templateId = templateId;
            this.templateName = templateName;
            this.bodyTemplate = bodyTemplate;
            this.status = status;
            this.subscriptionId = subscriptionId;
            this.pauseReason = pauseReason;
            this.consecutiveFailures = consecutiveFailures;
            this.scheduledAt = scheduledAt;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    public static final class Recipient {
        public final long id;
        public final String campaignId;
        public final long customerId;
        public final String customerName;
        public final String phone;
        public final String body;
        public final long messageId;
        public final String status;
        public final String reason;
        public final long scheduledAt;

        Recipient(long id, String campaignId, long customerId, String customerName,
                  String phone, String body, long messageId, String status,
                  String reason, long scheduledAt) {
            this.id = id;
            this.campaignId = campaignId;
            this.customerId = customerId;
            this.customerName = customerName;
            this.phone = phone;
            this.body = body;
            this.messageId = messageId;
            this.status = status;
            this.reason = reason;
            this.scheduledAt = scheduledAt;
        }
    }

    public static final class Counts {
        public int total;
        public int active;
        public int sent;
        public int failed;
        public int skipped;
        public int cancelled;
    }
}
