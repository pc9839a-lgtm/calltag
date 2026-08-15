package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * Verifies that the compact post-call popup was actually visible.
 * Some OEM phone apps keep the call UI in front for a few seconds after IDLE, so an accepted
 * PendingIntent is not treated as delivery. Retry the popup once after the phone UI has had time
 * to settle, then fall back to a notification only when that channel can really be shown.
 */
public final class PostCallDeliveryGuard {
    private static final long VERIFY_DELAY_MS = 2_400L;
    private static final long RETRY_VERIFY_DELAY_MS = 2_200L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private PostCallDeliveryGuard() {}

    public static void schedule(Context context, Intent source) {
        if (context == null || source == null) return;
        Context app = context.getApplicationContext();
        Intent review = PostCallActivityLauncher.prepareTarget(source);
        long callId = review.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        if (callId < 0L) return;

        MAIN.postDelayed(() -> {
            if (PostCallLaunchReceipt.wasVisible(app, callId)) return;

            CrashTelemetryStore.record(app, "post_call_delivery",
                    "activity_not_visible_retry", "call=" + callId);
            boolean retryAccepted = PostCallActivityLauncher.retryOnce(app, review);
            if (!retryAccepted) {
                CrashTelemetryStore.record(app, "post_call_delivery",
                        "activity_retry_rejected", "call=" + callId);
                showFallback(app, review);
                return;
            }

            MAIN.postDelayed(() -> {
                if (PostCallLaunchReceipt.wasVisible(app, callId)) return;
                CrashTelemetryStore.record(app, "post_call_delivery",
                        "activity_retry_not_visible", "call=" + callId);
                showFallback(app, review);
            }, RETRY_VERIFY_DELAY_MS);
        }, VERIFY_DELAY_MS);
    }

    private static void showFallback(Context context, Intent review) {
        long callId = review.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        String phone = safe(review.getStringExtra(PostCallActivity.EXTRA_PHONE));
        String cachedName = safe(review.getStringExtra(PostCallActivity.EXTRA_CACHED_NAME));
        int type = review.getIntExtra(PostCallActivity.EXTRA_CALL_TYPE, 0);
        long startedAt = review.getLongExtra(
                PostCallActivity.EXTRA_STARTED_AT, System.currentTimeMillis());
        long durationSec = Math.max(0L, review.getLongExtra(
                PostCallActivity.EXTRA_DURATION_SEC, 0L));
        if (callId < 0L || phone.isEmpty()) return;

        if (!CallPopupNotificationManager.isPopupReady(
                context, CallPopupNotificationManager.POST_CALL_CHANNEL_ID)) {
            CrashTelemetryStore.record(context, "post_call_delivery",
                    "fallback_notification_unavailable", "call=" + callId);
            // Do not mark this review delivered. PostCallRecoveryStore keeps it so the next
            // foreground/service recovery can try again instead of silently losing the memo UI.
            return;
        }

        CallRecord record = new CallRecord(callId, phone, cachedName, type, startedAt, durationSec);
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            Customer customer = db.findByPhone(phone);
            String memo = customer == null
                    ? "" : CustomerInsightResolver.latestMemo(db, customer);
            boolean posted = CallPopupNotificationManager.showPostCall(
                    context, record, customer, review, memo);
            if (posted) PostCallRecoveryStore.markDelivered(context, callId);
        } finally {
            db.close();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
