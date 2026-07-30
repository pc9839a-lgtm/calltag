package kr.pagero.calltag;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class MessageScheduler {
    private MessageScheduler() {}

    public static void schedule(Context context, long messageId, long when) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null || messageId <= 0L) return;
        long triggerAt = Math.max(System.currentTimeMillis() + 1_000L, when);
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt,
                pendingIntent(context, messageId));
    }

    public static void cancel(Context context, long messageId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pendingIntent(context, messageId));
    }

    public static void rescheduleAll(Context context) {
        if (!FeatureEntitlementStore.hasMessageAccess(context)) return;
        MessageLogStore store = new MessageLogStore(context);
        try {
            for (MessageRecord record : store.listScheduled()) {
                schedule(context, record.id, record.scheduledAt);
            }
        } finally {
            store.close();
        }
    }

    private static PendingIntent pendingIntent(Context context, long messageId) {
        Intent intent = new Intent(context, ScheduledMessageReceiver.class)
                .setAction(ScheduledMessageReceiver.ACTION_SEND_SCHEDULED)
                .setData(Uri.parse("calltag://scheduled-message/" + messageId))
                .putExtra(ScheduledMessageReceiver.EXTRA_MESSAGE_ID, messageId);
        int requestCode = (int) (messageId ^ (messageId >>> 32));
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
