package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public final class PageroAccountStatusStore {
    public static final String CONNECTED = "connected";
    public static final String NOT_CONNECTED = "not_connected";
    public static final String UNKNOWN = "unknown";

    private static final String PREFS = "calltag_pagero_account_status";
    private static final String KEY_STATUS = "status";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_PROJECT_COUNT = "project_count";
    private static final String KEY_CHECKED_AT = "checked_at";

    private PageroAccountStatusStore() {}

    public static void save(Context context, JSONObject response) {
        JSONObject value = response == null ? null : response.optJSONObject("connection");
        if (value == null && response != null) value = response.optJSONObject("pageroConnection");
        if (value == null) {
            saveUnknown(context, defaultMessage(UNKNOWN));
            return;
        }
        String status = normalize(value.optString("status", UNKNOWN));
        prefs(context).edit()
                .putString(KEY_STATUS, status)
                .putString(KEY_MESSAGE, defaultMessage(status))
                .putInt(KEY_PROJECT_COUNT, Math.max(0, value.optInt("projectCount", 0)))
                .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
                .apply();
    }

    public static void saveUnknown(Context context, String message) {
        prefs(context).edit()
                .putString(KEY_STATUS, UNKNOWN)
                .putString(KEY_MESSAGE, friendly(message, UNKNOWN))
                .putInt(KEY_PROJECT_COUNT, 0)
                .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
                .apply();
    }

    public static Snapshot read(Context context) {
        SharedPreferences value = prefs(context);
        String status = normalize(value.getString(KEY_STATUS, UNKNOWN));
        return new Snapshot(
                status,
                friendly(value.getString(KEY_MESSAGE, ""), status),
                value.getInt(KEY_PROJECT_COUNT, 0),
                value.getLong(KEY_CHECKED_AT, 0L));
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String normalize(String value) {
        if (CONNECTED.equals(value) || NOT_CONNECTED.equals(value)) return value;
        return UNKNOWN;
    }

    private static String defaultMessage(String status) {
        if (CONNECTED.equals(status)) return "페이지로 문의 연결이 완료됐어요.";
        if (NOT_CONNECTED.equals(status)) {
            return "페이지로에서 같은 이메일로 로그인하면 자동으로 연결돼요.";
        }
        return "연결 상태를 확인하고 있어요. 잠시 후 다시 확인해주세요.";
    }

    private static String friendly(String raw, String status) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()
                || value.contains("서버")
                || value.contains("API")
                || value.contains("status")
                || value.contains("error")
                || value.contains("code")) {
            return defaultMessage(status);
        }
        return value;
    }

    public static final class Snapshot {
        public final String status;
        public final String message;
        public final int projectCount;
        public final long checkedAt;

        Snapshot(String status, String message, int projectCount, long checkedAt) {
            this.status = status;
            this.message = message == null ? "" : message;
            this.projectCount = projectCount;
            this.checkedAt = checkedAt;
        }

        public boolean connected() {
            return CONNECTED.equals(status);
        }
    }
}
