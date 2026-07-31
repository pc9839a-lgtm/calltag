package kr.pagero.calltag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ScheduledMessageReceiver extends BroadcastReceiver {
    public static final String ACTION_SEND_SCHEDULED = "kr.pagero.calltag.SEND_SCHEDULED_MESSAGE";
    public static final String EXTRA_MESSAGE_ID = "message_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        long messageId = intent == null ? -1L : intent.getLongExtra(EXTRA_MESSAGE_ID, -1L);
        if (messageId <= 0L) return;

        MessageLogStore store = new MessageLogStore(context);
        try {
            MessageRecord record = store.find(messageId);
            if (record == null || !MessageLogStore.STATUS_SCHEDULED.equals(record.status)) return;

            String lifecycleBlock = TaskMessageLifecycleManager.validateScheduledSend(
                    context, messageId);
            if (!lifecycleBlock.isEmpty()) {
                store.markSkipped(messageId, lifecycleBlock);
                return;
            }

            if (!MessageAutomationStore.isWithinBusinessHours(context, System.currentTimeMillis())) {
                store.markSkipped(messageId, "설정한 업무시간 밖이라 발송하지 않았습니다.");
                return;
            }
            store.markReady(messageId);
        } finally {
            store.close();
        }
        SmsSender.sendExisting(context, messageId);
    }
}
