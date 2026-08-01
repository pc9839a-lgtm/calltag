package kr.pagero.calltag;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** 통화 종료 후 큰 정리 화면을 foreground service에서 가능한 강한 방식으로 연다. */
public final class PostCallActivityLauncher {
    private PostCallActivityLauncher() {}

    public static boolean launch(Context context, Intent source) {
        if (context == null || source == null) return false;
        Intent target = new Intent(source)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        long callId = target.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID,
                System.currentTimeMillis());
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
            return true;
        } catch (PendingIntent.CanceledException | RuntimeException ignored) {
            try {
                context.startActivity(target);
                return true;
            } catch (RuntimeException ignoredAgain) {
                return false;
            }
        }
    }
}
