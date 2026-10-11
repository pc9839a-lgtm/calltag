package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read-only operational kill switches served over HTTPS by the CallTag Pages worker.
 *
 * Server switches only further RESTRICT an existing Android capability. They cannot
 * enable SMS automation, grant permissions, change user opt-outs, alter subscriptions,
 * run code, or move CRM records. A previous restrictive policy remains cached across
 * network failures, restarts and logout. Only strictly newer revisions can replace it.
 */
public final class CallTagRemotePolicy {
    public static final String SMS = "smsSending";
    public static final String AUTOMATIC_SMS = "automaticSms";
    public static final String MMS = "mmsCompose";
    public static final String LEAD_IMPORT = "leadImport";
    public static final String CLOUD_SYNC = "cloudSync";

    private static final String URL = "https://calltag.pagero.kr/api/mobile-config/v1";
    private static final String PREFS = "calltag_remote_safety_policy_v1";
    private static final String PREFIX = "flag_";
    private static final long MIN_REFRESH_MS = 60_000L;
    private static final int MAX_RESPONSE_BYTES = 8192;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static volatile long lastAttemptElapsedMs;

    private CallTagRemotePolicy() {}

    public static boolean allowed(Context context, String flag) {
        if (context == null || !isKnownFlag(flag)) return false;
        // Never default to disabling a previously available feature when the
        // endpoint has not been deployed. A cached remote off-state is durable.
        return prefs(context).getBoolean(PREFIX + flag, true);
    }

    public static void refreshAsync(Context context, boolean force) {
        if (context == null) return;
        long now = SystemClock.elapsedRealtime();
        if (!force && now - lastAttemptElapsedMs < MIN_REFRESH_MS) return;
        if (!RUNNING.compareAndSet(false, true)) return;
        lastAttemptElapsedMs = now;
        Context app = context.getApplicationContext();
        Thread thread = new Thread(() -> {
            try {
                refreshNow(app);
            } catch (Exception ignored) {
                // Fail closed to the last verified revision. Never clear a kill switch.
            } finally {
                RUNNING.set(false);
            }
        }, "calltag-remote-safety-policy");
        thread.setDaemon(true);
        thread.start();
    }

    private static void refreshNow(Context context) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(4500);
            connection.setReadTimeout(4500);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("X-CallTag-App-Version", BuildConfig.VERSION_NAME);
            if (connection.getResponseCode() != 200) return;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[2048];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (output.size() + read > MAX_RESPONSE_BYTES) return;
                    output.write(buffer, 0, read);
                }
            }
            JSONObject policy = new JSONObject(
                    new String(output.toByteArray(), StandardCharsets.UTF_8));
            if (policy.optInt("schemaVersion", -1) != 1) return;
            Object revisionValue = policy.opt("revision");
            if (!(revisionValue instanceof Number)) return;
            long revision = ((Number) revisionValue).longValue();
            if (revision < 1L || ((Number) revisionValue).doubleValue() != revision) return;

            JSONObject flags = policy.optJSONObject("flags");
            if (flags == null) return;
            for (String key : new String[]{ SMS, AUTOMATIC_SMS, MMS, LEAD_IMPORT, CLOUD_SYNC }) {
                if (!(flags.opt(key) instanceof Boolean)) return;
            }

            SharedPreferences p = prefs(context);
            synchronized (CallTagRemotePolicy.class) {
                if (revision <= p.getLong("revision", 0L)) return;
                SharedPreferences.Editor editor = p.edit();
                for (String key : new String[]{ SMS, AUTOMATIC_SMS, MMS, LEAD_IMPORT, CLOUD_SYNC }) {
                    editor.putBoolean(PREFIX + key, flags.getBoolean(key));
                }
                // All flags and revision commit atomically or previous restrictions survive.
                editor.putLong("revision", revision);
                editor.putLong("updated_at", System.currentTimeMillis());
                if (!editor.commit()) throw new IllegalStateException("policy_persist_failed");
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean isKnownFlag(String flag) {
        return SMS.equals(flag) || AUTOMATIC_SMS.equals(flag) || MMS.equals(flag)
                || LEAD_IMPORT.equals(flag) || CLOUD_SYNC.equals(flag);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
