package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class CallTagDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "calltag.db";
    private static final int DB_VERSION = 1;

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_CONSULTING = "CONSULTING";
    public static final String STATUS_EXISTING = "EXISTING";
    public static final String STATUS_VIP = "VIP";
    public static final String STATUS_DORMANT = "DORMANT";
    public static final String STATUS_OPT_OUT = "OPT_OUT";
    public static final String STATUS_EXCLUDED = "EXCLUDED";

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
        db.execSQL("CREATE INDEX idx_opportunities_customer ON opportunities(customer_id, updated_at DESC)");
        db.execSQL("CREATE INDEX idx_interactions_customer ON interactions(customer_id, started_at DESC)");
        db.execSQL("CREATE INDEX idx_tasks_due ON follow_up_tasks(status, due_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Schema migrations will be added before the first production release.
    }

    public long insertNewLead(String displayName, String phone) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.length() < 8) {
            throw new IllegalArgumentException("전화번호를 정확히 입력해주세요.");
        }
        if (findByPhone(phone) != null) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }

        long now = System.currentTimeMillis();
        String safeName = displayName == null ? "" : displayName.trim();

        ContentValues values = new ContentValues();
        values.put("display_name", safeName.isEmpty() ? "이름 없는 고객" : safeName);
        values.put("primary_phone", phone.trim());
        values.put("normalized_phone", normalized);
        values.put("relation_status", STATUS_NEW);
        values.put("first_contact_at", now);
        values.put("last_contact_at", now);
        values.put("created_at", now);
        values.put("updated_at", now);
        return getWritableDatabase().insertOrThrow("customers", null, values);
    }

    public Customer findByPhone(String phone) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.isEmpty()) return null;

        try (Cursor cursor = getReadableDatabase().query(
                "customers",
                null,
                "normalized_phone = ?",
                new String[]{normalized},
                null,
                null,
                null,
                "1")) {
            return cursor.moveToFirst() ? readCustomer(cursor) : null;
        }
    }

    public List<Customer> listCustomers(String status) {
        List<Customer> customers = new ArrayList<>();
        String selection = status == null ? null : "relation_status = ?";
        String[] args = status == null ? null : new String[]{status};

        try (Cursor cursor = getReadableDatabase().query(
                "customers",
                null,
                selection,
                args,
                null,
                null,
                "last_contact_at DESC, id DESC")) {
            while (cursor.moveToNext()) {
                customers.add(readCustomer(cursor));
            }
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
        long now = System.currentTimeMillis();
        long end = now + 24L * 60L * 60L * 1000L;
        return count(
                "SELECT COUNT(*) FROM follow_up_tasks WHERE status = 'PENDING' AND due_at >= ? AND due_at < ?",
                new String[]{String.valueOf(now), String.valueOf(end)});
    }

    public int countOverdueTasks() {
        return count(
                "SELECT COUNT(*) FROM follow_up_tasks WHERE status = 'PENDING' AND due_at < ?",
                new String[]{String.valueOf(System.currentTimeMillis())});
    }

    public int countOpenOpportunities() {
        return count(
                "SELECT COUNT(*) FROM opportunities WHERE stage NOT IN ('WON', 'LOST')",
                null);
    }

    private int count(String sql, String[] args) {
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
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
                cursor.getString(cursor.getColumnIndexOrThrow("source")),
                cursor.getString(cursor.getColumnIndexOrThrow("memo")),
                cursor.getLong(cursor.getColumnIndexOrThrow("first_contact_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("last_contact_at")),
                firstTransactionAt);
    }
}
