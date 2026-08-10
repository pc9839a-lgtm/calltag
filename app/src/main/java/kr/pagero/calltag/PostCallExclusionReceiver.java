package kr.pagero.calltag;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Handles the one-tap "팝업 제외" action from the post-call notification. */
public final class PostCallExclusionReceiver extends BroadcastReceiver {
    public static final String ACTION_EXCLUDE = "kr.pagero.calltag.POST_CALL_EXCLUDE";
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_CALL_ID = "call_id";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !ACTION_EXCLUDE.equals(intent.getAction())) return;

        String phone = safe(intent.getStringExtra(EXTRA_PHONE));
        String name = safe(intent.getStringExtra(EXTRA_NAME));
        long callId = intent.getLongExtra(EXTRA_CALL_ID, -1L);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);

        try {
            PostCallExclusionStore.add(context, name, phone);
            if (callId > 0L) PostCallRecoveryStore.markDelivered(context, callId);
            if (notificationId > 0) {
                NotificationManager manager = context.getSystemService(NotificationManager.class);
                if (manager != null) manager.cancel(notificationId);
            }
            CrashTelemetryStore.record(context, "post_call_exclusion", "quick_added",
                    "call=" + callId);
            Toast.makeText(context, "이 번호는 앞으로 통화 후 팝업이 뜨지 않습니다.",
                    Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException error) {
            Toast.makeText(context, "전화번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
