package kr.pagero.calltag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 예약 시간이 된 텍스트·사진 문자를 자동 발송 큐로 전환한다. */
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
            if (!CampaignRuntimeManager.allowSend(context, record)) {
                DiagnosticEventStore.record(context, "예약 보류", messageId,
                        "캠페인 또는 SIM 안전 조건 불충족");
                return;
            }

            String lifecycleBlock = TaskMessageLifecycleManager.validateScheduledSend(
                    context, messageId);
            if (!lifecycleBlock.isEmpty()) {
                store.markSkipped(messageId, lifecycleBlock);
                MmsComposer.forget(context, messageId);
                CampaignRuntimeManager.onSendResult(context, messageId, false, Integer.MIN_VALUE);
                DiagnosticEventStore.record(context, "예약 건너뜀", messageId,
                        "일정 생명주기 조건 불충족");
                return;
            }

            if (!MessageAutomationStore.isWithinBusinessHours(context, System.currentTimeMillis())) {
                store.markSkipped(messageId, "설정한 업무시간 밖이라 발송하지 않았습니다.");
                MmsComposer.forget(context, messageId);
                CampaignRuntimeManager.onSendResult(context, messageId, false, Integer.MIN_VALUE);
                DiagnosticEventStore.record(context, "예약 건너뜀", messageId, "업무시간 밖");
                return;
            }

            boolean hasImage = MmsComposer.hasAttachment(context, messageId);
            store.markReady(messageId);
            DiagnosticEventStore.record(context,
                    hasImage ? "예약 MMS 준비" : "예약 SMS 준비",
                    messageId,
                    hasImage ? "사진 포함 자동발송 요청 전환" : "텍스트 자동발송 요청 전환");
        } finally {
            store.close();
        }
        SmsSender.sendExisting(context, messageId);
    }
}
