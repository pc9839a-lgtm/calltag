package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsStore {
    private static final String PREFS = "calltag_settings";
    private static final String KEY_MONITOR_ENABLED = "monitor_enabled";
    private static final String KEY_CALLER_INFO_DISPLAY_ENABLED = "caller_info_display_enabled";
    private static final String KEY_LAST_CALL_ID = "last_call_id";
    private static final String KEY_LAST_PROCESSED_CALL = "last_processed_call";
    private static final String KEY_CALL_RECOVERY_CURSOR_AT = "call_recovery_cursor_at";
    private static final String KEY_CALLER_PRIVACY_MODE = "caller_privacy_mode";
    private static final String KEY_LAST_SCREENING_STATUS = "last_screening_status";
    private static final String KEY_LAST_SCREENING_AT = "last_screening_at";
    private static final String KEY_SCREENING_ROLE_KNOWN = "screening_role_known";
    private static final String KEY_SCREENING_ROLE_HELD = "screening_role_held";
    private static final String KEY_SCREENING_ROLE_CHANGED_AT = "screening_role_changed_at";
    private static final String KEY_CONTACT_NAME_SYNC_ENABLED = "contact_name_sync_enabled";
    private static final String KEY_CONTACT_NAME_SYNC_STATUS = "contact_name_sync_status";

    public static final int CALLER_PRIVACY_NAME = 0;
    public static final int CALLER_PRIVACY_STAGE = 1;
    public static final int CALLER_PRIVACY_MEMO = 2;

    private SettingsStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isMonitorEnabled(Context context) {
        return prefs(context).getBoolean(KEY_MONITOR_ENABLED, false);
    }

    public static void setMonitorEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_MONITOR_ENABLED, enabled).apply();
    }

    public static boolean isCallerInfoDisplayEnabled(Context context) {
        return prefs(context).getBoolean(KEY_CALLER_INFO_DISPLAY_ENABLED, true);
    }

    public static void setCallerInfoDisplayEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_CALLER_INFO_DISPLAY_ENABLED, enabled).apply();
    }

    public static long lastCallId(Context context) {
        return prefs(context).getLong(KEY_LAST_CALL_ID, -1L);
    }

    public static void setLastCallId(Context context, long callId) {
        prefs(context).edit().putLong(KEY_LAST_CALL_ID, callId).apply();
    }

    public static long callRecoveryCursorAt(Context context) {
        return prefs(context).getLong(KEY_CALL_RECOVERY_CURSOR_AT, 0L);
    }

    public static void setCallRecoveryCursorAt(Context context, long timestamp) {
        prefs(context).edit()
                .putLong(KEY_CALL_RECOVERY_CURSOR_AT, Math.max(0L, timestamp))
                .apply();
    }

    public static void advanceCallRecoveryCursor(Context context, long timestamp) {
        if (timestamp <= 0L) return;
        SharedPreferences values = prefs(context);
        long current = values.getLong(KEY_CALL_RECOVERY_CURSOR_AT, 0L);
        if (timestamp <= current) return;
        values.edit().putLong(KEY_CALL_RECOVERY_CURSOR_AT, timestamp).apply();
    }

    public static boolean isCallProcessed(Context context, String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) return false;
        return fingerprint.equals(prefs(context).getString(KEY_LAST_PROCESSED_CALL, ""));
    }

    public static void markCallProcessed(Context context, String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) return;
        prefs(context).edit().putString(KEY_LAST_PROCESSED_CALL, fingerprint).apply();
    }

    public static int callerPrivacyMode(Context context) {
        int value = prefs(context).getInt(KEY_CALLER_PRIVACY_MODE, CALLER_PRIVACY_MEMO);
        if (value < CALLER_PRIVACY_NAME || value > CALLER_PRIVACY_MEMO) {
            return CALLER_PRIVACY_MEMO;
        }
        return value;
    }

    public static void setCallerPrivacyMode(Context context, int mode) {
        int safe = Math.max(CALLER_PRIVACY_NAME, Math.min(CALLER_PRIVACY_MEMO, mode));
        prefs(context).edit().putInt(KEY_CALLER_PRIVACY_MODE, safe).apply();
    }

    public static boolean updateScreeningRoleState(Context context, boolean held) {
        SharedPreferences values = prefs(context);
        boolean known = values.getBoolean(KEY_SCREENING_ROLE_KNOWN, false);
        boolean previous = values.getBoolean(KEY_SCREENING_ROLE_HELD, false);
        boolean changed = known && previous != held;
        SharedPreferences.Editor editor = values.edit()
                .putBoolean(KEY_SCREENING_ROLE_KNOWN, true)
                .putBoolean(KEY_SCREENING_ROLE_HELD, held);
        if (!known || changed) {
            editor.putLong(KEY_SCREENING_ROLE_CHANGED_AT, System.currentTimeMillis());
        }
        editor.apply();
        return changed;
    }

    public static boolean lastScreeningRoleHeld(Context context) {
        return prefs(context).getBoolean(KEY_SCREENING_ROLE_HELD, false);
    }

    public static long screeningRoleChangedAt(Context context) {
        return prefs(context).getLong(KEY_SCREENING_ROLE_CHANGED_AT, 0L);
    }

    public static boolean isContactNameSyncEnabled(Context context) {
        return prefs(context).getBoolean(KEY_CONTACT_NAME_SYNC_ENABLED, false);
    }

    public static void setContactNameSyncEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_CONTACT_NAME_SYNC_ENABLED, enabled).apply();
    }

    public static void setContactNameSyncStatus(Context context, String status) {
        prefs(context).edit().putString(KEY_CONTACT_NAME_SYNC_STATUS,
                status == null ? "" : status.trim()).apply();
    }

    public static String contactNameSyncStatus(Context context) {
        return prefs(context).getString(KEY_CONTACT_NAME_SYNC_STATUS,
                "아직 연락처 이름을 동기화하지 않았습니다.");
    }

    public static void setCallerScreeningStatus(Context context, String status) {
        prefs(context).edit()
                .putString(KEY_LAST_SCREENING_STATUS, status == null ? "" : status.trim())
                .putLong(KEY_LAST_SCREENING_AT, System.currentTimeMillis())
                .apply();
    }

    public static String lastCallerScreeningStatus(Context context) {
        return prefs(context).getString(KEY_LAST_SCREENING_STATUS, "아직 수신 감지 기록이 없습니다.");
    }

    public static long lastCallerScreeningAt(Context context) {
        return prefs(context).getLong(KEY_LAST_SCREENING_AT, 0L);
    }
}
