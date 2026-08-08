package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MessageGroupStore extends SQLiteOpenHelper {
    public static final String TYPE_MANUAL = "MANUAL";
    public static final String TYPE_SMART = "SMART";
    public static final String TRANSACTION_ANY = "ANY";
    public static final String TRANSACTION_HAS = "HAS";
    public static final String TRANSACTION_NONE = "NONE";

    private static final String DB_NAME = "calltag_groups.db";
    private static final int DB_VERSION = 1;

    public MessageGroupStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE message_groups (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "status_filter TEXT NOT NULL DEFAULT ''," +
                "inactive_days INTEGER NOT NULL DEFAULT 0," +
                "pending_only INTEGER NOT NULL DEFAULT 0," +
                "transaction_mode TEXT NOT NULL DEFAULT 'ANY'," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE message_group_members (" +
                "group_id TEXT NOT NULL," +
                "customer_id INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "PRIMARY KEY(group_id,customer_id)," +
                "FOREIGN KEY(group_id) REFERENCES message_groups(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX idx_group_type_name ON message_groups(type,name)");
        db.execSQL("CREATE INDEX idx_group_members_customer ON message_group_members(customer_id)");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public Group saveManual(String groupId, String rawName, List<Long> customerIds) {
        String id = cleanId(groupId);
        long now = System.currentTimeMillis();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = baseValues(rawName, TYPE_MANUAL, now, id);
            values.put("status_filter", "");
            values.put("inactive_days", 0);
            values.put("pending_only", 0);
            values.put("transaction_mode", TRANSACTION_ANY);
            db.insertWithOnConflict("message_groups", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.delete("message_group_members", "group_id=?", new String[]{id});
            if (customerIds != null) {
                Set<Long> unique = new HashSet<>(customerIds);
                for (Long customerId : unique) {
                    if (customerId == null || customerId <= 0L) continue;
                    ContentValues member = new ContentValues();
                    member.put("group_id", id);
                    member.put("customer_id", customerId);
                    member.put("created_at", now);
                    db.insertWithOnConflict("message_group_members", null, member,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return find(id);
    }

    public Group saveSmart(String groupId, String rawName, String statusFilter,
                           int inactiveDays, boolean pendingOnly, String ignoredTransactionMode) {
        String id = cleanId(groupId);
        long now = System.currentTimeMillis();
        ContentValues values = baseValues(rawName, TYPE_SMART, now, id);
        values.put("status_filter", safe(statusFilter).trim());
        values.put("inactive_days", Math.max(0, inactiveDays));
        values.put("pending_only", pendingOnly ? 1 : 0);
        // 거래/미거래는 콜태그의 스마트그룹 조건으로 사용하지 않는다.
        // 기존 DB 컬럼은 호환성을 위해 남기되 모든 저장값을 ANY로 정규화한다.
        values.put("transaction_mode", TRANSACTION_ANY);
        getWritableDatabase().insertWithOnConflict("message_groups", null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        getWritableDatabase().delete("message_group_members", "group_id=?", new String[]{id});
        return find(id);
    }

    private ContentValues baseValues(String rawName, String type, long now, String id) {
        String name = safe(rawName).trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) throw new IllegalArgumentException("그룹 이름을 입력해주세요.");
        if (name.length() > 30) throw new IllegalArgumentException("그룹 이름은 30자 이하로 입력해주세요.");
        Group existing = find(id);
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("name", name);
        values.put("type", type);
        values.put("created_at", existing == null ? now : existing.createdAt);
        values.put("updated_at", now);
        return values;
    }

    public Group find(String id) {
        if (safe(id).trim().isEmpty()) return null;
        try (Cursor cursor = getReadableDatabase().query("message_groups", null,
                "id=?", new String[]{id}, null, null, null, "1")) {
            return cursor.moveToFirst() ? readGroup(cursor) : null;
        }
    }

    public List<Group> list() {
        List<Group> rows = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("message_groups", null,
                null, null, null, null, "updated_at DESC,name ASC")) {
            while (cursor.moveToNext()) rows.add(readGroup(cursor));
        }
        return rows;
    }

    public List<Long> manualMemberIds(String groupId) {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("message_group_members",
                new String[]{"customer_id"}, "group_id=?", new String[]{groupId},
                null, null, "created_at ASC,customer_id ASC")) {
            while (cursor.moveToNext()) ids.add(cursor.getLong(0));
        }
        return ids;
    }

    public List<Customer> resolveMembers(Context context, Group group) {
        List<Customer> result = new ArrayList<>();
        if (group == null) return result;
        CallTagDbHelper crm = new CallTagDbHelper(context);
        try {
            List<Customer> all = crm.listCustomers(null);
            if (TYPE_MANUAL.equals(group.type)) {
                Set<Long> selected = new HashSet<>(manualMemberIds(group.id));
                for (Customer customer : all) {
                    if (selected.contains(customer.id)) result.add(customer);
                }
                return result;
            }

            Set<Long> pendingCustomers = new HashSet<>();
            if (group.pendingOnly) {
                for (FollowUpTask task : crm.listPendingTasks()) {
                    pendingCustomers.add(task.customerId);
                }
            }
            long inactiveCutoff = group.inactiveDays <= 0 ? Long.MIN_VALUE
                    : System.currentTimeMillis() - group.inactiveDays * 24L * 60L * 60L * 1000L;
            for (Customer customer : all) {
                if (!group.statusFilter.isEmpty()
                        && !group.statusFilter.equals(customer.relationStatus)) continue;
                if (group.inactiveDays > 0 && customer.lastContactAt > inactiveCutoff) continue;
                if (group.pendingOnly && !pendingCustomers.contains(customer.id)) continue;
                result.add(customer);
            }
            return result;
        } finally {
            crm.close();
        }
    }

    public int countMembers(Context context, Group group) {
        return resolveMembers(context, group).size();
    }

    public boolean delete(String id) {
        return getWritableDatabase().delete("message_groups", "id=?", new String[]{id}) > 0;
    }

    public static String describe(Group group) {
        if (group == null) return "";
        if (TYPE_MANUAL.equals(group.type)) return "직접 선택한 고객";
        List<String> parts = new ArrayList<>();
        if (!group.statusFilter.isEmpty()) parts.add("상태 " + group.statusFilter);
        if (group.inactiveDays > 0) parts.add(group.inactiveDays + "일 이상 미접촉");
        if (group.pendingOnly) parts.add("미완료 일정 있음");
        return parts.isEmpty() ? "전체 고객 자동 포함" : String.join(" · ", parts);
    }

    private Group readGroup(Cursor cursor) {
        return new Group(
                cursor.getString(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("type")),
                cursor.getString(cursor.getColumnIndexOrThrow("status_filter")),
                cursor.getInt(cursor.getColumnIndexOrThrow("inactive_days")),
                cursor.getInt(cursor.getColumnIndexOrThrow("pending_only")) == 1,
                TRANSACTION_ANY,
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")));
    }

    private String cleanId(String id) {
        String value = safe(id).trim();
        return value.isEmpty() ? UUID.randomUUID().toString() : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Group {
        public final String id;
        public final String name;
        public final String type;
        public final String statusFilter;
        public final int inactiveDays;
        public final boolean pendingOnly;
        public final String transactionMode;
        public final long createdAt;
        public final long updatedAt;

        Group(String id, String name, String type, String statusFilter, int inactiveDays,
              boolean pendingOnly, String transactionMode, long createdAt, long updatedAt) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.statusFilter = statusFilter;
            this.inactiveDays = inactiveDays;
            this.pendingOnly = pendingOnly;
            this.transactionMode = transactionMode;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
}
