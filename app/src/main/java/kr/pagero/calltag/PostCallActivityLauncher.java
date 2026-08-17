package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;

/**
 * Post-call delivery gate.
 *
 * Automatic Activity launches are intentionally disabled. A finished call must never bring the
 * CallTag app/task to the foreground. CallMonitorService and recovery callers interpret false as
 * "use fallback delivery", which routes to PostCallOverlayManager first and then to a notification
 * only when the overlay cannot be shown. PostCallActivity remains available only for an explicit
 * user action such as tapping a fallback notification.
 */
public final class PostCallActivityLauncher {
    private PostCallActivityLauncher() {}

    public static synchronized boolean launch(Context context, Intent source) {
        return handleWithoutActivity(context, source, "initial");
    }

    static synchronized boolean retryOnce(Context context, Intent source) {
        return handleWithoutActivity(context, source, "visibility_retry");
    }

    private static boolean handleWithoutActivity(Context context, Intent source, String sourceLabel) {
        if (context == null || source == null) return false;

        long callId = source.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        if (callId < 0L) {
            CrashTelemetryStore.record(context, "post_call_launcher", "invalid_call_id", "");
            return false;
        }

        String phone = source.getStringExtra(PostCallActivity.EXTRA_PHONE);
        if (PostCallExclusionStore.contains(context, phone)) {
            PostCallRecoveryStore.markDelivered(context, callId);
            CrashTelemetryStore.record(context, "post_call_launcher", "excluded",
                    "call=" + callId);
            return true;
        }

        // Deliberately return false so the caller uses the compact overlay/notification path.
        // Do not arm PostCallLaunchReceipt and do not schedule an Activity visibility retry.
        CrashTelemetryStore.record(context, "post_call_launcher", "auto_activity_disabled",
                "call=" + callId + ",source=" + sourceLabel);
        return false;
    }

    /**
     * Builds the Activity target used only after an explicit user action (for example, tapping a
     * fallback notification). It is never started automatically by this class.
     */
    public static Intent prepareTarget(Intent source) {
        Intent target = new Intent(source);
        int flags = target.getFlags();
        flags &= ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
        flags &= ~Intent.FLAG_ACTIVITY_NO_USER_ACTION;
        target.setFlags(flags);
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return target;
    }
}
