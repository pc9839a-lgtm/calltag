package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class CallTagDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "calltag.db";
    private static final int DB_VERSION = 2;

    public static final String STATUS_NEW = "신규 문의";
    public static final String STATUS_CONSULTING = "요구 확인";
    public static final String STATUS_QUOTE = "견적·자료 발송";
    public static final String STATUS_REVIEW = "검토 중";
    public static final String STATUS_EXPECTED = "계약 예정";
    public static final String STATUS_EXISTING = "계약 완료";

    // 구버전 코드와 기존 호출부 호환용. 화면에는 노출하지 않는다.
    public static final String STATUS_VIP = STATUS_EXISTING;
    public static final String STATUS_DORMANT = STATUS_REVIEW;
    public static final String STATUS_OPT_OUT = STATUS_REVIEW;
    public static final String STATUS_EXCLUDED = STATUS_REVIEW;

    private static final String[] DEFAULT_STAGES = {
            STATUS_NEW,
            STATUS_CONSULTING,
            STATUS_QUOTE,
            STATUS_REVIEW,
            STATUS_EXPECTED,
            STATUS_EXISTING
    };

    public CallTagDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "display_name TEXT NOT NULL," +
                "primary_phone TEXT NOT NULL," +
                "normalized_phone TEXT NOT NULL UNIQUE," +
                "relation_status TEXT NOT NULL," +
                "source TEXT NOT NULL DEFAULT ''," +
                "memo TEXT NOT NULL DEFAULT ''," +
                "first_contact_at INTEGER NOT NULL," +
                "last_contact_at INTEGER NOT NULL," +
                "first_transaction_at INTEGER," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE crm_stages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "position INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE opportunities (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "customer_id INTEGER NOT NULL," +
                "title TEXT NOT NULL," +
                "category TEXT NOT NULL DEFAULT ''," +
                "stage TEXT NOT NULL," +
                "expected_amount INTEGER," +
                "confirmed_amount INTEGER," +
                "summary TEXT NOT NULL DEFAULT ''," +
                "opened_at INTEGER NOT NULL," +
                "closed_at INTEGER," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL," +
                "FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE interactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "customer_id INTEGER NOT NULL," +
                "opportunity_id INTEGER," +
                "type TEXT NOT NULL," +
                "started_at INTEGER NOT NULL," +
                "ended_at INTEGER," +
                "duration_sec INTEGER NOT NULL DEFAULT 0," +
                "result TEXT NOT NULL DEFAULT ''," +
                "note TEXT NOT NULL DEFAULT ''," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE," +
                "FOREIGN KEY(opportunity_id) REFERENCES opportunities(id) ON DELETE SET NULL" +
                ")");

        db.execSQL("CREATE TABLE follow_up_tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "customer_id INTEGER NOT NULL," +
                "opportunity_id INTEGER," +
                "interaction_id INTEGER," +
                "task_type TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "due_at INTEGER NOT NULL," +
                "status TEXT NOT NULL," +
                "completed_at INTEGER," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE," +
                "FOREIGN KEY(opportunity_id) REFERENCES opportunities(id) ON DELETE SET NULL," +
                "FOREIGN KEY(interaction_id) REFERENCES interactions(id) ON DELETE SET NULL" +
                ")");

        db.execSQL("CREATE TABLE phone_rules (" +
                "normalized_phone TEXT PRIMARY KEY," +
                "rule_type TEXT NOT NULL," +
                "reason TEXT NOT NULL DEFAULT ''," +
                "created_at INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE INDEX idx_customers_status ON customers(relation_status, last_contact_at DESC)");
        db.execSQL("CREATE INDEX idx_stages_position ON crm_stages(position, id)");
        db.execSQL("CREATE INDEX idx_opportunities_customer ON opportunities(customer_id, updated_at DESC)");
        db.execSQL("CREATE INDEX idx_interactions_customer ON interactions(customer_id, started_at DESC)");
        db.execSQL("CREATE INDEX idx_tasks_due ON follow_up_tasks(status, due_at)");
        seedStages(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS crm_stages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL UNIQUE," +
                    "position INTEGER NOT NULL," +
                    "created_at INTEGER NOT NULL," +
                    "updated_at INTEGER NOT NULL" +
                    ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_stages_position ON crm_stages(position, id)");
            seedStages(db);
            migrateLegacyStatuses(db);
        }
    }

    private void seedStages(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < DEFAULT_STAGES.length; i++) {
            ContentValues values = new ContentValues();
            values.put("name", DEFAULT_STAGES[i]);
            values.put("position", i);
            values.put("created_at", now);
            values.put("updated_at", now);
            db.insertWithOnConflict("crm_stages", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private void migrateLegacyStatuses(SQLiteDatabase db) {
        db.execSQL("UPDATE customers SET relation_status=? WHERE relation_status IN ('NEW','신규')",
                new Object[]{STATUS_NEW});
        db.execSQL("UPDATE customers SET relation_status=? WHERE relation_status IN ('CONSULTING','상담 중')",
                new Object[]{STATUS_CONSULTING});
        db.execSQL("UPDATE customers SET relation_status=? WHERE relation_status IN ('EXISTING','VIP','기존','거래 고객')",
                new Object[]{STATUS_EXISTING});
        db.execSQL("UPDATE customers SET relation_status=? WHERE relation_status IN ('DORMANT','OPT_OUT','EXCLUDED','휴면')",
                new Object[]{STATUS_REVIEW});
    }

    public List<StageOption> listStages() {
        List<StageOption> stages = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("crm_stages",
                new String[]{"id", "name", "position"}, null, null,
                null, null, "position ASC,id ASC")) {
            while (cursor.moveToNext()) {
                stages.add(new StageOption(cursor.getLong(0), cursor.getString(1), cursor.getInt(2)));
            }
        }
        if (stages.isEmpty()) {
            seedStages(getWritableDatabase());
            return listStages();
        }
        return stages;
    }

    public String firstStage() {
        List<StageOption> stages = listStages();
        return stages.isEmpty() ? STATUS_NEW : stages.get(0).name;
    }

    public String completedStage() {
        List<StageOption> stages = listStages();
        return stages.isEmpty() ? STATUS_EXISTING : stages.get(stages.size() - 1).name;
    }

    public long addStage(String rawName) {
        String name = cleanStageName(rawName);
        if (stageExists(name, -1L)) throw new IllegalArgumentException("이미 등록된 단계입니다.");
        int position = count("SELECT COUNT(*) FROM crm_stages", null);
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("position", position);
        values.put("created_at", now);
        values.put("updated_at", now);
        return getWritableDatabase().insertOrThrow("crm_stages", null, values);
    }

    public void renameStage(long stageId, String oldName, String rawName) {
        String name = cleanStageName(rawName);
        if (stageExists(name, stageId)) throw new IllegalArgumentException("이미 등록된 단계입니다.");
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            ContentValues stage = new ContentValues();
            stage.put("name", name);
            stage.put("updated_at", System.currentTimeMillis());
            database.update("crm_stages", stage, "id=?", new String[]{String.valueOf(stageId)});

            ContentValues customer = new ContentValues();
            customer.put("relation_status", name);
            customer.put("updated_at", System.currentTimeMillis());
            database.update("customers", customer, "relation_status=?", new String[]{oldName});
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void deleteStage(long stageId, String stageName) {
        List<StageOption> stages = listStages();
        if (stages.size() <= 1) throw new IllegalArgumentException("영업 단계는 최소 1개가 필요합니다.");
        String replacement = null;
        for (StageOption stage : stages) {
            if (stage.id != stageId) {
                replacement = stage.name;
                break;
            }
        }
        if (replacement == null) throw new IllegalStateException("대체 단계를 찾지 못했습니다.");

        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            ContentValues customer = new ContentValues();
            customer.put("relation_status", replacement);
            customer.put("updated_at", System.currentTimeMillis());
            database.update("customers", customer, "relation_status=?", new String[]{stageName});
            database.delete("crm_stages", "id=?", new String[]{String.valueOf(stageId)});
            compactStagePositions(database);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void moveStage(long stageId, int direction) {
        List<StageOption> stages = listStages();
        int index = -1;
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).id == stageId) {
                index = i;
                break;
            }
        }
        int target = index + direction;
        if (index < 0 || target < 0 || target >= stages.size()) return;
        StageOption current = stages.get(index);
        StageOption other = stages.get(target);
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            ContentValues first = new ContentValues();
            first.put("position", other.position);
            first.put("updated_at", System.currentTimeMillis());
            database.update("crm_stages", first, "id=?", new String[]{String.valueOf(current.id)});
            ContentValues second = new ContentValues();
            second.put("position", current.position);
            second.put("updated_at", System.currentTimeMillis());
            database.update("crm_stages", second, "id=?", new String[]{String.valueOf(other.id)});
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void compactStagePositions(SQLiteDatabase db) {
        try (Cursor cursor = db.query("crm_stages", new String[]{"id"}, null, null,
                null, null, "position ASC,id ASC")) {
            int position = 0;
            while (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                values.put("position", position++);
                db.update("crm_stages", values, "id=?", new String[]{String.valueOf(cursor.getLong(0))});
            }
        }
    }

    private boolean stageExists(String name, long exceptId) {
        String selection = exceptId > 0L ? "name=? AND id<>?" : "name=?";
        String[] args = exceptId > 0L
                ? new String[]{name, String.valueOf(exceptId)} : new String[]{name};
        try (Cursor cursor = getReadableDatabase().query("crm_stages", new String[]{"id"},
                selection, args, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    private String cleanStageName(String rawName) {
        String name = rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) throw new IllegalArgumentException("단계명을 입력해주세요.");
        if (name.length() > 18) throw new IllegalArgumentException("단계명은 18자 이하로 입력해주세요.");
        return name;
    }

    public long insertNewLead(String displayName, String phone) {
        return insertCustomer(displayName, phone, firstStage(), "");
    }

    public long insertCustomer(String displayName, String phone, String status, String source) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.length() < 8) throw new IllegalArgumentException("전화번호를 정확히 입력해주세요.");
        if (findByPhone(phone) != null) throw new IllegalArgumentException("이미 등록된 전화번호입니다.");

        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("display_name", safeName(displayName));
        values.put("primary_phone", phone.trim());
        values.put("normalized_phone", normalized);
        values.put("relation_status", normalizeLegacyStatus(status));
        values.put("source", "");
        values.put("first_contact_at", now);
        values.put("last_contact_at", now);
        values.put("created_at", now);
        values.put("updated_at", now);
        return getWritableDatabase().insertOrThrow("customers", null, values);
    }

    private String normalizeLegacyStatus(String status) {
        if (status == null || status.trim().isEmpty()) return firstStage();
        if ("NEW".equals(status)) return STATUS_NEW;
        if ("CONSULTING".equals(status)) return STATUS_CONSULTING;
        if ("EXISTING".equals(status) || "VIP".equals(status)) return completedStage();
        if ("DORMANT".equals(status) || "OPT_OUT".equals(status) || "EXCLUDED".equals(status)) {
            return STATUS_REVIEW;
        }
        return status.trim();
    }

    public void updateCustomer(long customerId, String displayName, String status) {
        Customer customer = findCustomerById(customerId);
        updateCustomerProfile(customerId, displayName, status, customer == null ? "" : customer.memo);
    }

    public void updateCustomerStage(long customerId, String stage) {
        Customer customer = findCustomerById(customerId);
        if (customer == null) return;
        updateCustomerProfile(customerId, customer.displayName, stage, customer.memo);
    }

    public void updateCustomerProfile(long customerId, String displayName, String status, String memo) {
        ContentValues values = new ContentValues();
        values.put("display_name", safeName(displayName));
        values.put("relation_status", normalizeLegacyStatus(status));
        values.put("memo", memo == null ? "" : memo.trim());
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("customers", values, "id = ?", new String[]{String.valueOf(customerId)});
    }

    public void markTransactionCompleted(long customerId) {
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("relation_status", completedStage());
        values.put("first_transaction_at", now);
        values.put("updated_at", now);
        getWritableDatabase().update("customers", values, "id = ?", new String[]{String.valueOf(customerId)});
    }

    public long insertInteraction(long customerId, String type, long startedAt, long endedAt,
                                  long durationSec, String result, String note) {
        ContentValues values = new ContentValues();
        values.put("customer_id", customerId);
        values.put("type", type);
        values.put("started_at", startedAt);
        values.put("ended_at", endedAt);
        values.put("duration_sec", durationSec);
        values.put("result", result == null ? "" : result);
        values.put("note", note == null ? "" : note.trim());
        values.put("created_at", System.currentTimeMillis());
        long id = getWritableDatabase().insertOrThrow("interactions", null, values);

        ContentValues customer = new ContentValues();
        customer.put("last_contact_at", endedAt);
        customer.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("customers", customer, "id = ?", new String[]{String.valueOf(customerId)});
        return id;
    }

    public long insertFollowUpTask(long customerId, long interactionId, String taskType,
                                   String title, long dueAt) {
        ContentValues values = new ContentValues();
        values.put("customer_id", customerId);
        if (interactionId > 0L) values.put("interaction_id", interactionId);
        values.put("task_type", taskType);
        values.put("title", title == null || title.trim().isEmpty() ? "다시 연락" : title.trim());
        values.put("due_at", dueAt);
        values.put("status", "PENDING");
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("follow_up_tasks", null, values);
    }

    public void completeTask(long taskId) {
        ContentValues values = new ContentValues();
        values.put("status", "COMPLETED");
        values.put("completed_at", System.currentTimeMillis());
        getWritableDatabase().update("follow_up_tasks", values, "id = ?", new String[]{String.valueOf(taskId)});
    }

    public void reopenTask(long taskId) {
        ContentValues values = new ContentValues();
        values.put("status", "PENDING");
        values.putNull("completed_at");
        getWritableDatabase().update("follow_up_tasks", values, "id = ?", new String[]{String.valueOf(taskId)});
    }

    public void updateTaskDue(long taskId, long dueAt) {
        ContentValues values = new ContentValues();
        values.put("due_at", dueAt);
        values.put("status", "PENDING");
        values.putNull("completed_at");
        getWritableDatabase().update("follow_up_tasks", values, "id = ?", new String[]{String.valueOf(taskId)});
    }

    public void deleteTask(long taskId) {
        getWritableDatabase().delete("follow_up_tasks", "id = ?", new String[]{String.valueOf(taskId)});
    }

    public List<FollowUpTask> listPendingTasks() {
        return listTasks("WHERE t.status='PENDING'", null, "ORDER BY t.due_at ASC,t.id ASC");
    }

    public List<FollowUpTask> listTasksBetween(long startAt, long endAt) {
        return listTasks("WHERE t.due_at>=? AND t.due_at<?",
                new String[]{String.valueOf(startAt), String.valueOf(endAt)},
                "ORDER BY t.due_at ASC,t.id ASC");
    }

    public List<FollowUpTask> listTasksForCustomer(long customerId) {
        return listTasks("WHERE t.customer_id=?", new String[]{String.valueOf(customerId)},
                "ORDER BY t.due_at DESC,t.id DESC");
    }

    private List<FollowUpTask> listTasks(String where, String[] args, String orderBy) {
        List<FollowUpTask> tasks = new ArrayList<>();
        String sql = "SELECT t.id,t.customer_id,c.display_name,c.primary_phone,t.title,t.task_type,t.due_at,t.status " +
                "FROM follow_up_tasks t JOIN customers c ON c.id=t.customer_id " + where + " " + orderBy;
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                tasks.add(new FollowUpTask(
                        cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3),
                        cursor.getString(4), cursor.getString(5), cursor.getLong(6), cursor.getString(7)));
            }
        }
        return tasks;
    }

    public List<InteractionRecord> listInteractionsForCustomer(long customerId) {
        return listInteractions("WHERE i.customer_id = ?", new String[]{String.valueOf(customerId)}, 100);
    }

    public List<InteractionRecord> listRecentInteractions(int limit) {
        return listInteractions("", null, Math.max(1, limit));
    }

    private List<InteractionRecord> listInteractions(String where, String[] args, int limit) {
        List<InteractionRecord> rows = new ArrayList<>();
        String sql = "SELECT i.id,i.customer_id,c.display_name,c.primary_phone,i.type,i.started_at," +
                "i.duration_sec,i.result,i.note FROM interactions i " +
                "JOIN customers c ON c.id=i.customer_id " + where +
                " ORDER BY i.started_at DESC,i.id DESC LIMIT " + limit;
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                rows.add(new InteractionRecord(
                        cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3),
                        cursor.getString(4), cursor.getLong(5), cursor.getLong(6),
                        cursor.getString(7), cursor.getString(8)));
            }
        }
        return rows;
    }

    public void addPhoneRule(String phone, String ruleType, String reason) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.isEmpty()) return;
        ContentValues values = new ContentValues();
        values.put("normalized_phone", normalized);
        values.put("rule_type", ruleType);
        values.put("reason", reason == null ? "" : reason);
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("phone_rules", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public boolean isExcluded(String phone) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        try (Cursor cursor = getReadableDatabase().query("phone_rules", new String[]{"normalized_phone"},
                "normalized_phone = ?", new String[]{normalized}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    public Customer findByPhone(String phone) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.isEmpty()) return null;
        try (Cursor cursor = getReadableDatabase().query("customers", null, "normalized_phone = ?",
                new String[]{normalized}, null, null, null, "1")) {
            return cursor.moveToFirst() ? readCustomer(cursor) : null;
        }
    }

    public Customer findCustomerById(long customerId) {
        try (Cursor cursor = getReadableDatabase().query("customers", null, "id = ?",
                new String[]{String.valueOf(customerId)}, null, null, null, "1")) {
            return cursor.moveToFirst() ? readCustomer(cursor) : null;
        }
    }

    public List<Customer> listCustomers(String status) {
        List<Customer> customers = new ArrayList<>();
        String selection = status == null ? null : "relation_status = ?";
        String[] args = status == null ? null : new String[]{status};
        try (Cursor cursor = getReadableDatabase().query("customers", null, selection, args,
                null, null, "last_contact_at DESC, id DESC")) {
            while (cursor.moveToNext()) customers.add(readCustomer(cursor));
        }
        return customers;
    }

    public int countCustomers() {
        return count("SELECT COUNT(*) FROM customers", null);
    }

    public int countCustomersByStatus(String status) {
        return count("SELECT COUNT(*) FROM customers WHERE relation_status = ?", new String[]{status});
    }

    public int countPendingTasks() {
        return count("SELECT COUNT(*) FROM follow_up_tasks WHERE status = 'PENDING'", null);
    }

    public int countDueTodayTasks() {
        long start = startOfToday();
        long end = start + 24L * 60L * 60L * 1000L;
        return count("SELECT COUNT(*) FROM follow_up_tasks WHERE status='PENDING' AND due_at>=? AND due_at<?",
                new String[]{String.valueOf(start), String.valueOf(end)});
    }

    public int countOverdueTasks() {
        return count("SELECT COUNT(*) FROM follow_up_tasks WHERE status='PENDING' AND due_at<?",
                new String[]{String.valueOf(startOfToday())});
    }

    public int countOpenOpportunities() {
        return count("SELECT COUNT(*) FROM opportunities WHERE stage NOT IN ('WON','LOST')", null);
    }

    private int count(String sql, String[] args) {
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String safeName(String value) {
        String safe = value == null ? "" : value.trim();
        return safe.isEmpty() ? "이름 없는 고객" : safe;
    }

    private Customer readCustomer(Cursor cursor) {
        int transactionIndex = cursor.getColumnIndexOrThrow("first_transaction_at");
        Long firstTransactionAt = cursor.isNull(transactionIndex) ? null : cursor.getLong(transactionIndex);
        return new Customer(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("display_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("primary_phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("normalized_phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("relation_status")),
                "",
                cursor.getString(cursor.getColumnIndexOrThrow("memo")),
                cursor.getLong(cursor.getColumnIndexOrThrow("first_contact_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("last_contact_at")),
                firstTransactionAt);
    }
}
