package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

public final class CallTagSyncPreferenceStore {
    private static final String PREFS = "calltag_secure_sync_preferences";
    private static final String KEY_PREFIX = "enabled_";

    private CallTagSyncPreferenceStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(Context context) {
        String accountKey = CallTagSyncLocalStore.accountKey(context);
        return accountKey.isEmpty() ? "" : KEY_PREFIX + accountKey;
    }

    public static boolean isEnabled(Context context) {
        String key = key(context);
        return !key.isEmpty() && prefs(context).getBoolean(key, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        String key = key(context);
        if (key.isEmpty()) return;
        prefs(context).edit().putBoolean(key, enabled).apply();
    }
}