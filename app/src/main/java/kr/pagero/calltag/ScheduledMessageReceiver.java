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
        DiagnosticEventStore.record(context, "예약 수신", messageId, "예약 리시버 실행");

        MessageLogStore store = new MessageLogStore(context);
        try {
            MessageRecord record = store.find(messageId);
            if (record == null) {
                DiagnosticEventStore.record(context, "예약 무시", messageId, "문자 작업 없음");
                return;
            }
            if (!MessageLogStore.STATUS_SCHEDULED.equals(record.status)) {
                DiagnosticEventStore.record(context, "예약 무시", messageId,
                        "현재 상태 " + record.status);
                return;
            }

            String lifecycleBlock = TaskMessageLifecycleManager.validateScheduledSend(
                    context, messageId);
            if (!lifecycleBlock.isEmpty()) {
                store.markSkipped(messageId, lifecycleBlock);
                MmsComposer.forget(context, messageId);
                DiagnosticEventStore.record(context, "예약 건너뜀", messageId,
                        "일정 생명주기 조건 불충족");
                return;
            }

            if (!MessageAutomationStore.isWithinBusinessHours(context, System.currentTimeMillis())) {
                store.markSkipped(messageId, "설정한 업무시간 밖이라 발송하지 않았습니다.");
                MmsComposer.forget(context, messageId);
                DiagnosticEventStore.record(context, "예약 건너뜀", messageId, "업무시간 밖");
                return;
            }

            if (MmsComposer.hasAttachment(context, messageId)) {
                MmsComposer.postComposeNotification(context, messageId);
                DiagnosticEventStore.record(context, "이미지 문자 알림", messageId,
                        "사용자 확인 필요");
                return;
            }
            store.markReady(messageId);
            DiagnosticEventStore.record(context, "예약 발송 준비", messageId, "SMS 발송 요청 전환");
        } finally {
            store.close();
        }
        SmsSender.sendExisting(context, messageId);
    }
}
