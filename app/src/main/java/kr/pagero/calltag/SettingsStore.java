package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsStore {
    private static final String PREFS = "calltag_settings";
    private static final String KEY_MONITOR_ENABLED = "monitor_enabled";
    private static final String KEY_LAST_CALL_ID = "last_call_id";

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

    public static long lastCallId(Context context) {
        return prefs(context).getLong(KEY_LAST_CALL_ID, -1L);
    }

    public static void setLastCallId(Context context, long callId) {
        prefs(context).edit().putLong(KEY_LAST_CALL_ID, callId).apply();
    }
}
