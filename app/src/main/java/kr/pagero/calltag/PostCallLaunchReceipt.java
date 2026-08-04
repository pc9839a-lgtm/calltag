package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.lang.ref.WeakReference;

/** 통화 종료 정리 화면이 실제로 사용자에게 표시됐는지 확인한다. */
public final class PostCallLaunchReceipt {
    private static final String PREFS = "calltag_post_call_launch_receipt";
    private static final String KEY_PENDING_CALL_ID = "pending_call_id";
    private static final String KEY_ARMED_AT = "armed_at";
    private static final String KEY_VISIBLE_CALL_ID = "visible_call_id";
    private static final String KEY_VISIBLE_AT = "visible_at";

    private static WeakReference<Activity> visibleActivity = new WeakReference<>(null);

    private PostCallLaunchReceipt() {}

    public static synchronized void arm(Context context, Intent intent) {
        long callId = callId(intent);
        if (callId < 0L) return;
        prefs(context).edit()
                .putLong(KEY_PENDING_CALL_ID, callId)
                .putLong(KEY_ARMED_AT, System.currentTimeMillis())
                .remove(KEY_VISIBLE_CALL_ID)
                .remove(KEY_VISIBLE_AT)
                .apply();
    }

    public static synchronized void markVisible(Activity activity) {
        if (activity == null || !(activity instanceof PostCallActivity)) return;
        long callId = callId(activity.getIntent());
        if (callId < 0L) return;
        visibleActivity = new WeakReference<>(activity);
        prefs(activity).edit()
                .putLong(KEY_VISIBLE_CALL_ID, callId)
                .putLong(KEY_VISIBLE_AT, System.currentTimeMillis())
                .apply();
    }

    public static synchronized boolean wasVisible(Context context, long callId) {
        if (callId < 0L) return false;
        SharedPreferences value = prefs(context);
        long armedAt = value.getLong(KEY_ARMED_AT, 0L);
        long visibleAt = value.getLong(KEY_VISIBLE_AT, 0L);
        return value.getLong(KEY_PENDING_CALL_ID, Long.MIN_VALUE) == callId
                && value.getLong(KEY_VISIBLE_CALL_ID, Long.MIN_VALUE) == callId
                && visibleAt >= armedAt
                && armedAt > 0L;
    }

    public static synchronized void closeStaleActivity(long nextCallId) {
        Activity activity = visibleActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        long visibleCallId = callId(activity.getIntent());
        if (visibleCallId == nextCallId) return;
        activity.runOnUiThread(activity::finish);
        visibleActivity.clear();
    }

    private static long callId(Intent intent) {
        return intent == null ? -1L
                : intent.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
