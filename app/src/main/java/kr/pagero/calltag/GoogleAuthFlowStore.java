package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

/** One-shot marker proving that an OAuth callback follows a recent user-initiated login attempt. */
public final class GoogleAuthFlowStore {
    private static final String PREFS = "calltag_google_auth_flow";
    private static final String KEY_MARKER = "marker";
    private static final String KEY_STARTED_AT = "started_at";
    private static final long MAX_AGE_MS = 10L * 60L * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GoogleAuthFlowStore() {}

    public static void begin(Context context) {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        StringBuilder marker = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) marker.append(String.format("%02x", value & 0xff));
        prefs(context).edit()
                .putString(KEY_MARKER, marker.toString())
                .putLong(KEY_STARTED_AT, System.currentTimeMillis())
                .commit();
    }

    public static boolean consumeIfActive(Context context) {
        SharedPreferences prefs = prefs(context);
        String marker = prefs.getString(KEY_MARKER, "");
        long startedAt = prefs.getLong(KEY_STARTED_AT, 0L);
        prefs.edit().clear().commit();
        long age = System.currentTimeMillis() - startedAt;
        return marker != null && marker.length() == 48 && startedAt > 0L
                && age >= 0L && age <= MAX_AGE_MS;
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
