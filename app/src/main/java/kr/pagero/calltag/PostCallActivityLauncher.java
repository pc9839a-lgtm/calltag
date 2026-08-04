package kr.pagero.calltag;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Opens the post-call screen and verifies that Android actually displayed it. */
public final class PostCallActivityLauncher {
    private static final long DUPLICATE_WINDOW_MS = 12_000L;

    private static long lastCallId = Long.MIN_VALUE;
    private static long lastLaunchAt;

    private PostCallActivityLauncher() {}

    public static synchronized boolean launch(Context context, Intent source) {
        if (context == null || source == null) return false;

        long callId = source.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        long now = System.currentTimeMillis();
        if (callId < 0L) return false;
        if (callId == lastCallId
                && now - lastLaunchAt < DUPLICATE_WINDOW_MS
                && PostCallLaunchReceipt.wasVisible(context, callId)) {
            return true;
        }

        PostCallLaunchReceipt.closeStaleActivity(callId);
        Intent target = new Intent(source)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PostCallLaunchReceipt.arm(context, target);

        int requestCode = (int) (callId & 0x7fffffff);
        PendingIntent pending = PendingIntent.getActivity(
                context,
                requestCode,
                target,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean accepted = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                pending.send(context, 0, null, null, null, null, options.toBundle());
            } else {
                pending.send();
            }
            accepted = true;
        } catch (PendingIntent.CanceledException | RuntimeException ignored) {
            try {
                context.startActivity(target);
                accepted = true;
            } catch (RuntimeException ignoredAgain) {
                accepted = false;
            }
        }

        lastCallId = callId;
        lastLaunchAt = now;
        PostCallDeliveryGuard.schedule(context, target);
        return accepted;
    }
}
