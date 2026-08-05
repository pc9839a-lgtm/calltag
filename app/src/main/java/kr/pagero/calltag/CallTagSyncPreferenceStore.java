package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

public final class CallTagSyncPreferenceStore {
    private static final String PREFS = "calltag_secure_sync_preferences";
    private static final String KEY_ENABLED = "enabled";

    private CallTagSyncPreferenceStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}