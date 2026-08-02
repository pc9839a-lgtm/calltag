package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

/** 페이지로 문의 동기화의 마지막 실행 결과를 앱 화면에 표시하기 위한 로컬 상태 저장소. */
public final class PageroConnectionStatusStore {
    private static final String PREFS = "calltag_pagero_connection";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_LAST_ATTEMPT = "last_attempt";
    private static final String KEY_LAST_SUCCESS = "last_success";
    private static final String KEY_IMPORTED = "imported";
    private static final String KEY_UPDATED = "updated";
    private static final String KEY_REJECTED = "rejected";
    private static final String KEY_ERROR = "error";
    private static final String KEY_ERROR_CODE = "error_code";

    private PageroConnectionStatusStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void markRunning(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_RUNNING, true)
                .putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis())
                .putString(KEY_ERROR, "")
                .putString(KEY_ERROR_CODE, "")
                .apply();
    }

    public static void markSuccess(Context context, int imported, int updated, int rejected) {
        long now = System.currentTimeMillis();
        prefs(context).edit()
                .putBoolean(KEY_RUNNING, false)
                .putLong(KEY_LAST_ATTEMPT, now)
                .putLong(KEY_LAST_SUCCESS, now)
                .putInt(KEY_IMPORTED, Math.max(0, imported))
                .putInt(KEY_UPDATED, Math.max(0, updated))
                .putInt(KEY_REJECTED, Math.max(0, rejected))
                .putString(KEY_ERROR, "")
                .putString(KEY_ERROR_CODE, "")
                .apply();
    }

    public static void markFailure(Context context, String message, String code) {
        prefs(context).edit()
                .putBoolean(KEY_RUNNING, false)
                .putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis())
                .putString(KEY_ERROR, safe(message))
                .putString(KEY_ERROR_CODE, safe(code))
                .apply();
    }

    public static Snapshot read(Context context) {
        SharedPreferences values = prefs(context);
        return new Snapshot(
                values.getBoolean(KEY_RUNNING, false),
                values.getLong(KEY_LAST_ATTEMPT, 0L),
                values.getLong(KEY_LAST_SUCCESS, 0L),
                values.getInt(KEY_IMPORTED, 0),
                values.getInt(KEY_UPDATED, 0),
                values.getInt(KEY_REJECTED, 0),
                values.getString(KEY_ERROR, ""),
                values.getString(KEY_ERROR_CODE, ""));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Snapshot {
        public final boolean running;
        public final long lastAttemptAt;
        public final long lastSuccessAt;
        public final int imported;
        public final int updated;
        public final int rejected;
        public final String error;
        public final String errorCode;

        Snapshot(
                boolean running,
                long lastAttemptAt,
                long lastSuccessAt,
                int imported,
                int updated,
                int rejected,
                String error,
                String errorCode) {
            this.running = running;
            this.lastAttemptAt = lastAttemptAt;
            this.lastSuccessAt = lastSuccessAt;
            this.imported = imported;
            this.updated = updated;
            this.rejected = rejected;
            this.error = safe(error);
            this.errorCode = safe(errorCode);
        }
    }
}
