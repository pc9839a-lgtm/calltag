package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;

/**
 * Post-call policy gate.
 *
 * Automatic background Activity launches are intentionally disabled. The call monitor receives
 * false and posts the compact post-call notification instead. PostCallActivity opens only after
 * an explicit user tap on that notification.
 */
public final class PostCallActivityLauncher {
    private PostCallActivityLauncher() {}

    public static synchronized boolean launch(Context context, Intent source) {
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

        CrashTelemetryStore.record(context, "post_call_launcher", "notification_only",
                "call=" + callId);
        return false;
    }

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
