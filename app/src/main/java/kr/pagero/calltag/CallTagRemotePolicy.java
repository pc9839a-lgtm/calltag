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
    private static final String LIMIT_PREFIX = "minimum_";
    public static final String MIN_COOLDOWN_HOURS = "minCooldownHours";
    public static final String MIN_LEAD_DELAY_MINUTES = "minLeadDelayMinutes";
    public static final String MIN_FOLLOWUP_DAYS = "minFollowupDays";
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

    /**
     * Server may only increase an automatic send's minimum delay/duplicate
     * window. A zero/default never overrides the user's own stricter values.
     */
    public static int minimumCooldownHours(Context context) {
        return cachedMinimum(context, MIN_COOLDOWN_HOURS, 720);
    }

    public static int minimumLeadDelayMinutes(Context context) {
        return cachedMinimum(context, MIN_LEAD_DELAY_MINUTES, 1440);
    }

    public static int minimumFollowupDays(Context context) {
        return cachedMinimum(context, MIN_FOLLOWUP_DAYS, 30);
    }

    private static int cachedMinimum(Context context, String key, int cap) {
        if (context == null) return 0;
        int value = prefs(context).getInt(LIMIT_PREFIX + key, 0);
        return Math.max(0, Math.min(cap, value));
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

    /**
     * WorkManager invokes this on a background worker thread even when no
     * Activity has been opened recently. A failed poll retains cached stops.
     */
    public static void refreshOnWorker(Context context) {
        if (context == null || !RUNNING.compareAndSet(false, true)) return;
        try {
            lastAttemptElapsedMs = SystemClock.elapsedRealtime();
            refreshNow(context.getApplicationContext());
        } catch (Exception ignored) {
            // Retain the last committed operational policy.
        } finally {
            RUNNING.set(false);
        }
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

            // Backwards-compatible v1 response: no limits => all zeros.
            // Reject the entire response if any limit is malformed/out of bounds.
            JSONObject limits = policy.optJSONObject("automationLimits");
            if (policy.has("automationLimits") && limits == null) return;
            int cooldown = checkedMinimum(limits, MIN_COOLDOWN_HOURS, 720);
            int leadDelay = checkedMinimum(limits, MIN_LEAD_DELAY_MINUTES, 1440);
            int followup = checkedMinimum(limits, MIN_FOLLOWUP_DAYS, 30);
            if (cooldown < 0 || leadDelay < 0 || followup < 0) return;

            SharedPreferences p = prefs(context);
            synchronized (CallTagRemotePolicy.class) {
                // Existing clients understand flags but not automationLimits. A
                // device upgrading from such a client may already have cached
                // this SAME revision without its limits. Apply it once after
                // upgrade, but never accept an older revision.
                long cachedRevision = p.getLong("revision", 0L);
                boolean limitsInitialized = p.getBoolean("limits_initialized", false);
                if (revision < cachedRevision
                        || (revision == cachedRevision && limitsInitialized)) return;
                SharedPreferences.Editor editor = p.edit();
                for (String key : new String[]{ SMS, AUTOMATIC_SMS, MMS, LEAD_IMPORT, CLOUD_SYNC }) {
                    editor.putBoolean(PREFIX + key, flags.getBoolean(key));
                }
                // All flags and revision commit atomically or previous restrictions survive.
                editor.putInt(LIMIT_PREFIX + MIN_COOLDOWN_HOURS, cooldown);
                editor.putInt(LIMIT_PREFIX + MIN_LEAD_DELAY_MINUTES, leadDelay);
                editor.putInt(LIMIT_PREFIX + MIN_FOLLOWUP_DAYS, followup);
                editor.putBoolean("limits_initialized", true);
                editor.putLong("revision", revision);
                editor.putLong("updated_at", System.currentTimeMillis());
                if (!editor.commit()) throw new IllegalStateException("policy_persist_failed");
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static int checkedMinimum(JSONObject limits, String key, int cap) {
        if (limits == null) return 0;
        Object raw = limits.opt(key);
        if (!(raw instanceof Number)) return -1;
        double number = ((Number) raw).doubleValue();
        long value = ((Number) raw).longValue();
        return value < 0 || value > cap || number != value ? -1 : (int) value;
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
