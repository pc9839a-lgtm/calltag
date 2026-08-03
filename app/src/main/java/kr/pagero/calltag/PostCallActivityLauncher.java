package kr.pagero.calltag;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Opens the compact post-call popup once in its own task. */
public final class PostCallActivityLauncher {
    private static final long DUPLICATE_WINDOW_MS = 8_000L;

    private static long lastCallId = Long.MIN_VALUE;
    private static long lastLaunchAt;

    private PostCallActivityLauncher() {}

    public static synchronized boolean launch(Context context, Intent source) {
        if (context == null || source == null) return false;

        long callId = source.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID,
                System.currentTimeMillis());
        long now = System.currentTimeMillis();
        if (callId == lastCallId && now - lastLaunchAt < DUPLICATE_WINDOW_MS) {
            return true;
        }

        Intent target = new Intent(source)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        int requestCode = (int) (callId & 0x7fffffff);
        PendingIntent pending = PendingIntent.getActivity(
                context,
                requestCode,
                target,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                pending.send(context, 0, null, null, null, null, options.toBundle());
            } else {
                pending.send();
            }
            lastCallId = callId;
            lastLaunchAt = now;
            return true;
        } catch (PendingIntent.CanceledException | RuntimeException ignored) {
            try {
                context.startActivity(target);
                lastCallId = callId;
                lastLaunchAt = now;
                return true;
            } catch (RuntimeException ignoredAgain) {
                return false;
            }
        }
    }
}
