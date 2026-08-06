package kr.pagero.calltag;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Opens the post-call screen through a fresh, crash-safe task entry. */
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
        Intent review = new Intent(source)
                .setClass(context, PostCallActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        PostCallLaunchReceipt.arm(context, review);

        Intent entry = PostCallEntryActivity.createIntent(context, review);
        int requestCode = (int) ((callId ^ 0x4A770000L) & 0x7fffffff);
        PendingIntent pending = BackgroundActivityLaunchCompat.activity(
                context,
                requestCode,
                entry,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean accepted = BackgroundActivityLaunchCompat.send(context, pending);
        if (!accepted) {
            try {
                context.startActivity(entry);
                accepted = true;
            } catch (RuntimeException ignored) {
                accepted = false;
            }
        }

        lastCallId = callId;
        lastLaunchAt = now;
        PostCallDeliveryGuard.schedule(context, review);
        return accepted;
    }
}
