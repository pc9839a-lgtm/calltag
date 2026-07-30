package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TaskTypeStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "calltag_task_types.db";
    private static final int DB_VERSION = 1;

    public static final String TYPE_CALL = "CALL";
    public static final String TYPE_MEETING = "MEETING";
    public static final String TYPE_SEND = "SEND";
    public static final String TYPE_CUSTOM = "CUSTOM";

    public TaskTypeStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE task_types (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "code TEXT NOT NULL UNIQUE," +
                "name TEXT NOT NULL," +
                "color TEXT NOT NULL," +
                "position INTEGER NOT NULL," +
                "is_default INTEGER NOT NULL DEFAULT 0," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL" +
                ")");
        seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Initial schema.
    }

    private void seed(SQLiteDatabase db) {
        insertDefault(db, TYPE_CALL, "전화하기", "#4389FF", 0);
        insertDefault(db, TYPE_MEETING, "미팅", "#7A5AF8", 1);
        insertDefault(db, TYPE_SEND, "자료 보내기", "#F5A524", 2);
    }

    private void insertDefault(SQLiteDatabase db, String code, String name, String color, int position) {
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("code", code);
        values.put("name", name);
        values.put("color", color);
        values.put("position", position);
        values.put("is_default", 1);
        values.put("created_at", now);
        values.put("updated_at", now);
        db.insertWithOnConflict("task_types", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<TaskTypeOption> list() {
        List<TaskTypeOption> rows = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "task_types",
                new String[]{"id", "code", "name", "color", "is_default"},
                null, null, null, null, "position ASC,id ASC")) {
            while (cursor.moveToNext()) {
                rows.add(new TaskTypeOption(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getInt(4) == 1));
            }
        }
        if (rows.isEmpty()) {
            seed(getWritableDatabase());
            return list();
        }
        return rows;
    }

    public TaskTypeOption find(String code) {
        if (code == null || code.trim().isEmpty()) return fallback(TYPE_CUSTOM);
        try (Cursor cursor = getReadableDatabase().query(
                "task_types",
                new String[]{"id", "code", "name", "color", "is_default"},
                "code=?", new String[]{code.trim()}, null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                return new TaskTypeOption(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getInt(4) == 1);
            }
        }
        return fallback(code);
    }

    private TaskTypeOption fallback(String code) {
        String safe = code == null || code.trim().isEmpty() ? TYPE_CUSTOM : code.trim();
        return new TaskTypeOption(-1L, safe, "할 일", "#A7ABB2", false);
    }

    public long add(String rawName, String rawColor) {
        String name = cleanName(rawName);
        if (nameExists(name, -1L)) throw new IllegalArgumentException("이미 등록된 일정 종류입니다.");
        long now = System.currentTimeMillis();
        String code = "CUSTOM_" + now + "_" + Math.abs(name.hashCode());
        ContentValues values = new ContentValues();
        values.put("code", code.toUpperCase(Locale.US));
        values.put("name", name);
        values.put("color", cleanColor(rawColor));
        values.put("position", count());
        values.put("is_default", 0);
        values.put("created_at", now);
        values.put("updated_at", now);
        return getWritableDatabase().insertOrThrow("task_types", null, values);
    }

    public void update(TaskTypeOption option, String rawName, String rawColor) {
        if (option == null || option.id <= 0L) return;
        String name = cleanName(rawName);
        if (nameExists(name, option.id)) throw new IllegalArgumentException("이미 등록된 일정 종류입니다.");
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", cleanColor(rawColor));
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("task_types", values, "id=?", new String[]{String.valueOf(option.id)});
    }

    public void delete(TaskTypeOption option) {
        if (option == null || option.id <= 0L) return;
        if (option.defaultType) throw new IllegalArgumentException("기본 일정 종류는 삭제할 수 없습니다.");
        getWritableDatabase().delete("task_types", "id=?", new String[]{String.valueOf(option.id)});
    }

    private int count() {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM task_types", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private boolean nameExists(String name, long exceptId) {
        String selection = exceptId > 0L ? "name=? AND id<>?" : "name=?";
        String[] args = exceptId > 0L
                ? new String[]{name, String.valueOf(exceptId)} : new String[]{name};
        try (Cursor cursor = getReadableDatabase().query(
                "task_types", new String[]{"id"}, selection, args,
                null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    private String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) throw new IllegalArgumentException("일정 종류 이름을 입력해주세요.");
        if (name.length() > 18) throw new IllegalArgumentException("일정 종류는 18자 이하로 입력해주세요.");
        return name;
    }

    private String cleanColor(String raw) {
        String color = raw == null ? "" : raw.trim().toUpperCase(Locale.US);
        return color.matches("#[0-9A-F]{6}") ? color : "#4389FF";
    }
}
