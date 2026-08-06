package kr.pagero.calltag;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/** Android 14~16의 PendingIntent 백그라운드 Activity 시작 opt-in을 한 곳에서 처리한다. */
public final class BackgroundActivityLaunchCompat {
    private BackgroundActivityLaunchCompat() {}

    public static PendingIntent activity(Context context, int requestCode,
                                         Intent intent, int flags) {
        Bundle options = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions creator = ActivityOptions.makeBasic();
            if (Build.VERSION.SDK_INT >= 36) {
                creator.setPendingIntentCreatorBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS);
            } else {
                creator.setPendingIntentCreatorBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            }
            options = creator.toBundle();
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags, options);
    }

    public static boolean send(Context context, PendingIntent pendingIntent) {
        if (context == null || pendingIntent == null) return false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions sender = ActivityOptions.makeBasic();
                if (Build.VERSION.SDK_INT >= 36) {
                    sender.setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS);
                } else {
                    sender.setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                }
                pendingIntent.send(context, 0, null, null, null, null, sender.toBundle());
            } else {
                pendingIntent.send();
            }
            return true;
        } catch (PendingIntent.CanceledException | RuntimeException ignored) {
            return false;
        }
    }
}
