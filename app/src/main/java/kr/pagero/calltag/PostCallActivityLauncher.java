package kr.pagero.calltag;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/** Opens the compact post-call popup automatically and verifies delivery. */
public final class PostCallActivityLauncher {
    private static final long DUPLICATE_WINDOW_MS = 12_000L;

    private static long lastCallId = Long.MIN_VALUE;
    private static long lastLaunchAt;

    private PostCallActivityLauncher() {}

    public static synchronized boolean launch(Context context, Intent source) {
        if (context == null || source == null) return false;

        long callId = source.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        long now = System.currentTimeMillis();
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

        if (callId == lastCallId
                && now - lastLaunchAt < DUPLICATE_WINDOW_MS
                && PostCallLaunchReceipt.wasVisible(context, callId)) {
            CrashTelemetryStore.record(context, "post_call_launcher",
                    "duplicate_visible", "call=" + callId);
            return true;
        }

        PostCallLaunchReceipt.closeStaleActivity(callId);
        Intent target = prepareTarget(source);
        PostCallLaunchReceipt.arm(context, target);
        CrashTelemetryStore.record(context, "post_call_launcher", "auto_popup_attempt",
                "call=" + callId);

        int requestCode = (int) (callId & 0x7fffffff);
        PendingIntent pending = createActivityPendingIntent(context, requestCode, target);
        boolean accepted = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions options = ActivityOptions.makeBasic();
                if (Build.VERSION.SDK_INT >= 36) {
                    options.setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS);
                } else {
                    options.setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                }
                pending.send(context, 0, null, null, null, null, options.toBundle());
            } else {
                pending.send();
            }
            accepted = true;
            CrashTelemetryStore.record(context, "post_call_launcher",
                    "auto_popup_pending_intent_accepted", "call=" + callId);
        } catch (PendingIntent.CanceledException | RuntimeException firstError) {
            CrashTelemetryStore.record(context, "post_call_launcher",
                    "auto_popup_pending_intent_failed", firstError.getClass().getSimpleName());
            try {
                context.startActivity(prepareTarget(source));
                accepted = true;
                CrashTelemetryStore.record(context, "post_call_launcher",
                        "auto_popup_direct_start_accepted", "call=" + callId);
            } catch (RuntimeException secondError) {
                CrashTelemetryStore.record(context, "post_call_launcher",
                        "auto_popup_direct_start_failed", secondError.getClass().getSimpleName());
            }
        }

        lastCallId = callId;
        lastLaunchAt = now;
        PostCallDeliveryGuard.schedule(context, target);
        return accepted;
    }

    private static PendingIntent createActivityPendingIntent(Context context, int requestCode,
                                                              Intent target) {
        int flags = PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return PendingIntent.getActivity(context, requestCode, target, flags);
        }

        // Android 14+ requires creator-side opt-in as well as sender-side opt-in for a
        // background activity launch. Android 16 adds ALLOW_ALWAYS.
        ActivityOptions creator = ActivityOptions.makeBasic();
        if (Build.VERSION.SDK_INT >= 36) {
            creator.setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS);
        } else {
            creator.setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
        }
        Bundle options = creator.toBundle();
        return PendingIntent.getActivity(context, requestCode, target, flags, options);
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
