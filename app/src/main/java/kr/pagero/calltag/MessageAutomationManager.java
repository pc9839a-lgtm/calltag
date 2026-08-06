package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.CallLog;

/** 통화 종료 이벤트를 텍스트 SMS 또는 사진 MMS 자동발송 작업으로 변환한다. */
public final class MessageAutomationManager {
    /** 구버전 발송내역 호환용. 신규 자동발송은 수신·발신 트리거를 분리한다. */
    public static final String TRIGGER_CONNECTED = "CALL_CONNECTED_END";
    public static final String TRIGGER_INCOMING = "CALL_INCOMING_END";
    public static final String TRIGGER_OUTGOING = "CALL_OUTGOING_END";
    public static final String TRIGGER_MISSED = "MISSED_OR_REJECTED_CALL";
    public static final String TRIGGER_DELAYED = "DELAY_AFTER_CALL";
    public static final String TRIGGER_MANUAL = "MANUAL_SEND";
    public static final String TRIGGER_CAMPAIGN = "CAMPAIGN_SEND";

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private MessageAutomationManager() {}

    public static void onCallResolved(Context context, CallRecord record, Customer customer) {
        if (record == null || !FeatureEntitlementStore.hasMessageAccess(context)) return;
        MessageAutomationStore.ensureDefaults(context);
        MessageTemplateStore.ensureDefaults(context);
        if (!MessageAutomationStore.isEnabled(context)) return;
        String phone = record.phone;
        if (PhoneNumberNormalizer.normalize(phone).length() < 8) return;

        boolean incomingConnected = record.durationSec > 0L
                && record.type == CallLog.Calls.INCOMING_TYPE;
        boolean outgoingConnected = record.durationSec > 0L
                && record.type == CallLog.Calls.OUTGOING_TYPE;
        boolean connected = incomingConnected || outgoingConnected;
        boolean missed = record.type == CallLog.Calls.MISSED_TYPE
                || record.type == CallLog.Calls.REJECTED_TYPE;

        MessageLogStore store = new MessageLogStore(context);
        try {
            if (connected) {
                int cancelled = store.cancelScheduledForPhone(
                        phone, TRIGGER_DELAYED, "새 통화가 감지돼 이전 후속 문자를 취소했습니다.");
                if (cancelled > 0) {
                    context.sendBroadcast(new android.content.Intent(MessageSectionView.ACTION_CHANGED)
                            .setPackage(context.getPackageName()));
                }
            }

            if (connected && MessageAutomationStore.connectedEnabled(context)) {
                String purpose = incomingConnected
                        ? MessageTemplateStore.PURPOSE_INCOMING
                        : MessageTemplateStore.PURPOSE_OUTGOING;
                String trigger = incomingConnected ? TRIGGER_INCOMING : TRIGGER_OUTGOING;
                MessageTemplateStore.Template template =
                        AutomationTemplateSelectionStore.template(context, purpose);
                sendImmediate(context, store, record, customer, trigger,
                        template == null ? "" : template.id,
                        template == null
                                ? MessageAutomationStore.DEFAULT_CONNECTED_TEMPLATE
                                : template.body);
            } else if (missed && MessageAutomationStore.missedEnabled(context)) {
                MessageTemplateStore.Template template = AutomationTemplateSelectionStore.template(
                        context, MessageTemplateStore.PURPOSE_MISSED);
                sendImmediate(context, store, record, customer, TRIGGER_MISSED,
                        template == null ? "" : template.id,
                        template == null
                                ? MessageAutomationStore.DEFAULT_MISSED_TEMPLATE
                                : template.body);
            }

            // 이전 버전의 단일 후속문자 설정도 호환한다. 신규 다중 규칙은 FollowUpAutomationManager가 처리한다.
            if (connected && MessageAutomationStore.delayedEnabled(context)) {
                scheduleDelayed(context, store, record, customer);
            }
        } finally {
            store.close();
        }
    }

    private static void sendImmediate(Context context, MessageLogStore store,
                                      CallRecord record, Customer customer,
                                      String trigger, String templateId,
                                      String templateBody) {
        long now = System.currentTimeMillis();
        MessageTemplateEngine.RenderResult rendered = MessageAutomationStore.renderMessage(
                context, templateBody, customer, record);
        long customerId = customer == null ? 0L : customer.id;
        int subscriptionId = MessageAutomationStore.selectedSubscriptionId(context);
        if (!rendered.isReady()) {
            long id = store.createJobAdvanced(customerId, record.id, 0L, "", templateId,
                    record.phone, rendered.body, trigger, MessageLogStore.STATUS_FAILED,
                    now, subscriptionId, false);
            store.markFailed(id, renderFailureMessage(rendered));
            return;
        }

        MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                context, customerId, record.phone, trigger);
        if (exclusion.blocked) {
            long id = store.createJobAdvanced(customerId, record.id, 0L, "", templateId,
                    record.phone, rendered.body, trigger, MessageLogStore.STATUS_SKIPPED,
                    now, subscriptionId, false);
            store.markSkipped(id, exclusion.reason);
            return;
        }

        if (!MessageAutomationStore.isWithinBusinessHours(context, now)) {
            long id = store.createJobAdvanced(customerId, record.id, 0L, "", templateId,
                    record.phone, rendered.body, trigger, MessageLogStore.STATUS_SKIPPED,
                    now, subscriptionId, false);
            store.markSkipped(id, "설정한 업무시간 밖이라 발송하지 않았습니다.");
            return;
        }

        if (context.checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            long id = store.createJobAdvanced(customerId, record.id, 0L, "", templateId,
                    record.phone, rendered.body, trigger, MessageLogStore.STATUS_FAILED,
                    now, subscriptionId, false);
            store.markFailed(id, "문자 발송 권한이 필요합니다.");
            return;
        }

        SmsSender.queueAndSend(context, customerId, record.id, 0L, "", templateId,
                record.phone, rendered.body, trigger, subscriptionId, false);
    }

    private static void scheduleDelayed(Context context, MessageLogStore store,
                                        CallRecord record, Customer customer) {
        long now = System.currentTimeMillis();
        MessageTemplateStore.Template template = AutomationTemplateSelectionStore.template(
                context, MessageTemplateStore.PURPOSE_FOLLOW_UP);
        String templateId = template == null ? "" : template.id;
        String templateBody = template == null
                ? MessageAutomationStore.DEFAULT_DELAYED_TEMPLATE : template.body;
        MessageTemplateEngine.RenderResult rendered = MessageAutomationStore.renderMessage(
                context, templateBody, customer, record);
        long when = now + MessageAutomationStore.delayDays(context) * DAY_MS;
        long customerId = customer == null ? 0L : customer.id;
        int subscriptionId = MessageAutomationStore.selectedSubscriptionId(context);
        if (!rendered.isReady()) {
            long id = store.createJobAdvanced(customerId, record.id, 0L, "", templateId,
                    record.phone, rendered.body, TRIGGER_DELAYED,
                    MessageLogStore.STATUS_FAILED, now, subscriptionId, false);
            store.markFailed(id, renderFailureMessage(rendered));
            return;
        }

        MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                context, customerId, record.phone, TRIGGER_DELAYED);
        if (exclusion.blocked) {
            long id = store.createJobAdvanced(customerId, record.id, 0L, "", templateId,
                    record.phone, rendered.body, TRIGGER_DELAYED,
                    MessageLogStore.STATUS_SKIPPED, now, subscriptionId, false);
            store.markSkipped(id, exclusion.reason);
            return;
        }

        long id = store.createJobAdvanced(customerId, record.id, 0L, "", templateId,
                record.phone, rendered.body, TRIGGER_DELAYED,
                MessageLogStore.STATUS_SCHEDULED, when, subscriptionId, false);
        MessageRecord created = store.find(id);
        if (created != null && MessageLogStore.STATUS_SCHEDULED.equals(created.status)) {
            SmsSender.bindTemplateAttachment(context, id, templateId);
            MessageScheduler.schedule(context, id, when);
        }
    }

    private static String renderFailureMessage(MessageTemplateEngine.RenderResult rendered) {
        if (!rendered.unsupportedVariables.isEmpty()) {
            return "지원하지 않는 변수가 있습니다: "
                    + MessageTemplateEngine.describeVariables(rendered.unsupportedVariables);
        }
        if (!rendered.unresolvedVariables.isEmpty()) {
            return "치환되지 않은 변수가 있습니다: "
                    + MessageTemplateEngine.describeVariables(rendered.unresolvedVariables)
                    + ". 고객·계정·일정 정보를 확인해주세요.";
        }
        return "문자 내용을 확인해주세요.";
    }
}
