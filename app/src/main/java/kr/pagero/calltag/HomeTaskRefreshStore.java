package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

/** One-shot signal that forces MainActivity to rebuild after an isolated task editor save. */
public final class HomeTaskRefreshStore {
    private static final String PREFS = "calltag_home_task_refresh";
    private static final String KEY_PENDING = "pending";

    private HomeTaskRefreshStore() {}

    public static void mark(Context context) {
        prefs(context).edit().putBoolean(KEY_PENDING, true).apply();
    }

    public static boolean consume(Context context) {
        SharedPreferences value = prefs(context);
        if (!value.getBoolean(KEY_PENDING, false)) return false;
        value.edit().putBoolean(KEY_PENDING, false).apply();
        return true;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
