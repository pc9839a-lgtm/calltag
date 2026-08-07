package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Persists post-call reviews until Android has actually shown the popup or a fallback notification.
 * This survives process death and foreground-service restarts.
 */
public final class PostCallRecoveryStore {
    private static final String PREFS = "calltag_post_call_recovery";
    private static final String KEY_QUEUE = "pending_reviews";
    private static final int MAX_QUEUE = 20;
    private static final long MAX_AGE_MS = 48L * 60L * 60L * 1000L;

    private PostCallRecoveryStore() {}

    public static synchronized void arm(Context context, CallRecord record, long pendingCallId) {
        if (!valid(record)) return;
        JSONArray existing = read(context);
        JSONArray next = new JSONArray();
        long now = System.currentTimeMillis();

        for (int i = 0; i < existing.length(); i++) {
            JSONObject row = existing.optJSONObject(i);
            if (row == null) continue;
            long id = row.optLong("call_id", -1L);
            long armedAt = row.optLong("armed_at", 0L);
            if (id == record.id || now - armedAt > MAX_AGE_MS) continue;
            next.put(row);
        }

        JSONObject row = new JSONObject();
        try {
            row.put("call_id", record.id);
            row.put("pending_call_id", pendingCallId);
            row.put("phone", safe(record.phone));
            row.put("cached_name", safe(record.cachedName));
            row.put("call_type", record.type);
            row.put("started_at", record.startedAt);
            row.put("duration_sec", Math.max(0L, record.durationSec));
            row.put("armed_at", now);
        } catch (JSONException ignored) {
            return;
        }
        next.put(row);
        write(context, trimToNewest(next));
        CrashTelemetryStore.record(context, "post_call_recovery", "armed",
                "call=" + record.id);
    }

    public static synchronized void markDelivered(Context context, long callLogId) {
        if (callLogId <= 0L) return;
        JSONArray existing = read(context);
        JSONArray next = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < existing.length(); i++) {
            JSONObject row = existing.optJSONObject(i);
            if (row == null) continue;
            if (row.optLong("call_id", -1L) == callLogId) {
                removed = true;
                continue;
            }
            next.put(row);
        }
        if (removed) {
            write(context, next);
            CrashTelemetryStore.record(context, "post_call_recovery", "delivered",
                    "call=" + callLogId);
        }
    }

    public static synchronized boolean hasPending(Context context, long callLogId) {
        JSONArray rows = read(context);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && row.optLong("call_id", -1L) == callLogId) return true;
        }
        return false;
    }

    public static synchronized int pendingCount(Context context) {
        return read(context).length();
    }

    /**
     * Re-delivers the newest undelivered review. In foreground we prefer the compact popup;
     * from a background/service context we leave a high-priority notification instead.
     */
    public static synchronized boolean recoverLatest(Context context, boolean preferActivity) {
        PendingReview pending = newestValid(context);
        if (pending == null) return false;

        Intent review = pending.reviewIntent(context);
        if (preferActivity && PostCallActivityLauncher.launch(context, review)) {
            CrashTelemetryStore.record(context, "post_call_recovery", "activity_retry",
                    "call=" + pending.record.id);
            return true;
        }

        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            Customer customer = db.findByPhone(pending.record.phone);
            String memo = customer == null
                    ? "" : CustomerInsightResolver.latestMemo(db, customer);
            boolean posted = CallPopupNotificationManager.showPostCall(
                    context, pending.record, customer, review, memo);
            if (posted) {
                markDelivered(context, pending.record.id);
                CrashTelemetryStore.record(context, "post_call_recovery", "notification_retry",
                        "call=" + pending.record.id);
            }
            return posted;
        } finally {
            db.close();
        }
    }

    static synchronized void clearForTests(Context context) {
        prefs(context).edit().remove(KEY_QUEUE).commit();
    }

    private static PendingReview newestValid(Context context) {
        JSONArray existing = read(context);
        JSONArray retained = new JSONArray();
        PendingReview newest = null;
        long newestAt = Long.MIN_VALUE;
        long now = System.currentTimeMillis();

        for (int i = 0; i < existing.length(); i++) {
            JSONObject row = existing.optJSONObject(i);
            if (row == null) continue;
            long armedAt = row.optLong("armed_at", 0L);
            CallRecord record = recordFrom(row);
            if (!valid(record) || armedAt <= 0L || now - armedAt > MAX_AGE_MS) continue;
            retained.put(row);
            if (armedAt >= newestAt) {
                newestAt = armedAt;
                newest = new PendingReview(
                        record,
                        row.optLong("pending_call_id", -1L));
            }
        }
        if (retained.length() != existing.length()) write(context, retained);
        return newest;
    }

    private static CallRecord recordFrom(JSONObject row) {
        if (row == null) return null;
        return new CallRecord(
                row.optLong("call_id", -1L),
                row.optString("phone", ""),
                row.optString("cached_name", ""),
                row.optInt("call_type", 0),
                row.optLong("started_at", 0L),
                Math.max(0L, row.optLong("duration_sec", 0L)));
    }

    private static boolean valid(CallRecord record) {
        return record != null
                && record.id > 0L
                && record.startedAt > 0L
                && PhoneNumberNormalizer.normalize(record.phone).length() >= 8;
    }

    private static JSONArray trimToNewest(JSONArray rows) {
        int start = Math.max(0, rows.length() - MAX_QUEUE);
        JSONArray result = new JSONArray();
        for (int i = start; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null) result.put(row);
        }
        return result;
    }

    private static JSONArray read(Context context) {
        String raw = prefs(context).getString(KEY_QUEUE, "[]");
        try {
            return new JSONArray(raw == null ? "[]" : raw);
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    private static void write(Context context, JSONArray rows) {
        prefs(context).edit().putString(KEY_QUEUE, rows.toString()).commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class PendingReview {
        final CallRecord record;
        final long pendingCallId;

        PendingReview(CallRecord record, long pendingCallId) {
            this.record = record;
            this.pendingCallId = pendingCallId;
        }

        Intent reviewIntent(Context context) {
            return new Intent(context, PostCallActivity.class)
                    .putExtra(PostCallActivity.EXTRA_PENDING_CALL_ID, pendingCallId)
                    .putExtra(PostCallActivity.EXTRA_CALL_LOG_ID, record.id)
                    .putExtra(PostCallActivity.EXTRA_PHONE, record.phone)
                    .putExtra(PostCallActivity.EXTRA_CACHED_NAME, record.cachedName)
                    .putExtra(PostCallActivity.EXTRA_CALL_TYPE, record.type)
                    .putExtra(PostCallActivity.EXTRA_STARTED_AT, record.startedAt)
                    .putExtra(PostCallActivity.EXTRA_ENDED_AT,
                            Math.max(record.endedAt(), record.startedAt))
                    .putExtra(PostCallActivity.EXTRA_DURATION_SEC,
                            Math.max(0L, record.durationSec))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        }
    }
}
