package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;

import java.util.List;

/** 연결된 통화 한 건에 대해 활성화된 모든 후속문자 규칙을 예약한다. */
public final class FollowUpAutomationManager {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private FollowUpAutomationManager() {}

    public static void onConnectedCall(Context context, CallRecord record, Customer customer) {
        if (context == null || record == null || record.durationSec <= 0L) return;
        if (!FeatureEntitlementStore.hasMessageAccess(context)) return;
        FollowUpRuleStore.ensureMigrated(context);

        MessageLogStore store = new MessageLogStore(context);
        try {
            int cancelled = store.cancelScheduledForPhone(
                    record.phone,
                    MessageAutomationManager.TRIGGER_DELAYED,
                    "새 통화가 감지돼 이전 후속 문자를 취소했습니다.");
            if (cancelled > 0) {
                context.sendBroadcast(new Intent(MessageSectionView.ACTION_CHANGED)
                        .setPackage(context.getPackageName()));
            }
            List<FollowUpRuleStore.Rule> rules = FollowUpRuleStore.enabledRules(context);
            for (FollowUpRuleStore.Rule rule : rules) {
                schedule(context, store, record, customer, rule);
            }
        } finally {
            store.close();
        }
    }

    private static void schedule(Context context, MessageLogStore store,
                                 CallRecord record, Customer customer,
                                 FollowUpRuleStore.Rule rule) {
        MessageTemplateStore.Template template = MessageTemplateStore.get(
                context, rule.templateId);
        if (template != null && template.imageRef != null && !template.imageRef.trim().isEmpty()) {
            template = null;
        }
        if (template == null) {
            template = MessageTemplateStore.defaultTemplate(
                    context, MessageTemplateStore.PURPOSE_FOLLOW_UP);
        }
        String templateId = template == null ? "" : template.id;
        String templateBody = template == null
                ? MessageAutomationStore.DEFAULT_DELAYED_TEMPLATE : template.body;
        MessageTemplateEngine.RenderResult rendered = MessageAutomationStore.renderMessage(
                context, templateBody, customer, record);
        long now = System.currentTimeMillis();
        long when = now + Math.max(1, Math.min(30, rule.delayDays)) * DAY_MS;
        long customerId = customer == null ? 0L : customer.id;
        int subscriptionId = MessageAutomationStore.selectedSubscriptionId(context);
        String ruleLabel = "후속 · " + (rule.name == null || rule.name.trim().isEmpty()
                ? "후속 안내" : rule.name.trim());

        if (!rendered.isReady()) {
            long id = store.createJobAdvanced(customerId, record.id, 0L, ruleLabel, templateId,
                    record.phone, rendered.body, MessageAutomationManager.TRIGGER_DELAYED,
                    MessageLogStore.STATUS_FAILED, now, subscriptionId, false);
            store.markFailed(id, renderFailureMessage(rendered));
            return;
        }

        MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                context, customerId, record.phone, MessageAutomationManager.TRIGGER_DELAYED);
        if (exclusion.blocked) {
            long id = store.createJobAdvanced(customerId, record.id, 0L, ruleLabel, templateId,
                    record.phone, rendered.body, MessageAutomationManager.TRIGGER_DELAYED,
                    MessageLogStore.STATUS_SKIPPED, now, subscriptionId, false);
            store.markSkipped(id, exclusion.reason);
            return;
        }

        long id = store.createJobAdvanced(customerId, record.id, 0L, ruleLabel, templateId,
                record.phone, rendered.body, MessageAutomationManager.TRIGGER_DELAYED,
                MessageLogStore.STATUS_SCHEDULED, when, subscriptionId, false);
        MessageRecord created = store.find(id);
        if (created != null && MessageLogStore.STATUS_SCHEDULED.equals(created.status)) {
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
