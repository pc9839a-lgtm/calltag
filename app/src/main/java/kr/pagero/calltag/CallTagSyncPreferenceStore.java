package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

public final class CallTagSyncPreferenceStore {
    private static final String PREFS = "calltag_secure_sync_preferences";
    private static final String KEY_ENABLED = "enabled";
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
        if (key.isEmpty()) return false;
        return prefs(context).getBoolean(key,
                prefs(context).getBoolean(KEY_ENABLED, false));
    }

    public static void setEnabled(Context context, boolean enabled) {
        Context app = context.getApplicationContext();
        String key = key(app);
        if (key.isEmpty()) return;
        boolean saved = prefs(app).edit().putBoolean(key, enabled).commit();
        if (!saved) return;
        if (enabled) {
            CallTagSyncWorkScheduler.reconcile(app);
            CallTagSyncWorkScheduler.enqueueImmediate(app, "enabled");
        } else {
            CallTagSyncWorkScheduler.cancel(app);
        }
    }
}
