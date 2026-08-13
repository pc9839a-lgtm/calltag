package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.List;

/** 페이지로 문의 접수 직후 실행되는 전용 문자 트리거. 기본 OFF이며 eventId 기준 멱등 처리한다. */
public final class PageroLeadMessageAutomation {
    public static final String TRIGGER_PAGERO_LEAD_RECEIVED = "PAGERO_LEAD_RECEIVED";
    private static final String CAMPAIGN_PREFIX = "pagero-lead:";

    private PageroLeadMessageAutomation() {}

    public static void onImported(Context context, PageroLead lead, Customer customer) {
        if (lead == null || customer == null || customer.id <= 0L) return;
        PageroLeadReceiptStore receipts = new PageroLeadReceiptStore(context);
        try {
            if (!PageroLeadMessageSettings.enabled(context)) {
                receipts.markSms(lead.eventId, 0L, PageroLeadReceiptStore.SMS_NOT_SENT,
                        "페이지로 문의 자동문자가 꺼져 있습니다.");
                return;
            }
            if (!FeatureEntitlementStore.hasMessageAccess(context)) {
                receipts.markSms(lead.eventId, 0L, PageroLeadReceiptStore.SMS_NOT_SENT,
                        "문자자동화 요금제가 필요합니다.");
                return;
            }

            String body = render(context,
                    PageroLeadMessageSettings.templateFor(context, lead.siteId), lead, customer);
            List<String> unresolved = MessageTemplateEngine.findPlaceholders(body);
            if (body.trim().isEmpty() || !unresolved.isEmpty()) {
                receipts.markSms(lead.eventId, 0L, PageroLeadReceiptStore.SMS_FAILED,
                        unresolved.isEmpty() ? "문자 내용을 확인해주세요."
                                : "치환되지 않은 변수가 있습니다: "
                                + MessageTemplateEngine.describeVariables(unresolved));
                return;
            }

            MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                    context, customer.id, lead.phone, TRIGGER_PAGERO_LEAD_RECEIVED);
            if (exclusion.blocked) {
                long jobId = createTerminalJob(context, lead, customer, body,
                        MessageLogStore.STATUS_SKIPPED, exclusion.reason);
                receipts.markSms(lead.eventId, jobId, PageroLeadReceiptStore.SMS_NOT_SENT,
                        exclusion.reason);
                return;
            }

            if (context.checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                long jobId = createTerminalJob(context, lead, customer, body,
                        MessageLogStore.STATUS_FAILED, "문자 보내기 권한이 필요합니다.");
                receipts.markSms(lead.eventId, jobId, PageroLeadReceiptStore.SMS_FAILED,
                        "문자 보내기 권한이 필요합니다.");
                return;
            }

            int subscriptionId = MessageAutomationStore.selectedSubscriptionId(context);
            if (!SimProfileManager.isActive(context, subscriptionId)) {
                long jobId = createTerminalJob(context, lead, customer, body,
                        MessageLogStore.STATUS_FAILED, "문자 발송 회선을 선택해주세요.");
                receipts.markSms(lead.eventId, jobId, PageroLeadReceiptStore.SMS_FAILED,
                        "문자 발송 회선을 선택해주세요.");
                return;
            }

            int delayMinutes = PageroLeadMessageSettings.delayMinutes(context);
            long now = System.currentTimeMillis();
            if (delayMinutes == 0 && !MessageAutomationStore.isWithinBusinessHours(context, now)) {
                long jobId = createTerminalJob(context, lead, customer, body,
                        MessageLogStore.STATUS_SKIPPED, "설정한 업무시간 밖이라 발송하지 않았습니다.");
                receipts.markSms(lead.eventId, jobId, PageroLeadReceiptStore.SMS_NOT_SENT,
                        "업무시간 밖이라 발송하지 않았습니다.");
                return;
            }

            String campaignId = CAMPAIGN_PREFIX + lead.eventId;
            if (delayMinutes > 0) {
                long when = now + delayMinutes * 60_000L;
                MessageLogStore store = new MessageLogStore(context);
                long jobId;
                try {
                    jobId = store.createJobAdvanced(customer.id, 0L, 0L,
                            campaignId, "", lead.phone, body,
                            TRIGGER_PAGERO_LEAD_RECEIVED, MessageLogStore.STATUS_SCHEDULED,
                            when, subscriptionId, false);
                    MessageRecord created = store.find(jobId);
                    if (created != null && MessageLogStore.STATUS_SCHEDULED.equals(created.status)) {
                        MessageScheduler.schedule(context, jobId, when);
                        receipts.markSms(lead.eventId, jobId, PageroLeadReceiptStore.SMS_NOT_SENT,
                                delayMinutes + "분 후 발송 예정");
                    } else {
                        updateReceiptFromJob(receipts, lead.eventId, created, jobId);
                    }
                } finally {
                    store.close();
                }
                return;
            }

            long jobId = SmsSender.queueAndSend(context, customer.id, 0L, 0L,
                    campaignId, "", lead.phone, body, TRIGGER_PAGERO_LEAD_RECEIVED,
                    subscriptionId, false);
            MessageLogStore store = new MessageLogStore(context);
            try {
                updateReceiptFromJob(receipts, lead.eventId, store.find(jobId), jobId);
            } finally {
                store.close();
            }
        } finally {
            receipts.close();
        }
    }

    private static long createTerminalJob(Context context, PageroLead lead, Customer customer,
                                          String body, String status, String reason) {
        MessageLogStore store = new MessageLogStore(context);
        try {
            long id = store.createJobAdvanced(customer.id, 0L, 0L,
                    CAMPAIGN_PREFIX + lead.eventId, "", lead.phone, body,
                    TRIGGER_PAGERO_LEAD_RECEIVED, status, System.currentTimeMillis(),
                    MessageAutomationStore.selectedSubscriptionId(context), false);
            if (MessageLogStore.STATUS_FAILED.equals(status)) store.markFailed(id, reason);
            else if (MessageLogStore.STATUS_SKIPPED.equals(status)) store.markSkipped(id, reason);
            return id;
        } finally {
            store.close();
        }
    }

    private static void updateReceiptFromJob(PageroLeadReceiptStore receipts, String eventId,
                                             MessageRecord record, long jobId) {
        if (record == null) {
            receipts.markSms(eventId, jobId, PageroLeadReceiptStore.SMS_FAILED,
                    "문자 발송 상태를 확인하지 못했습니다.");
            return;
        }
        if (MessageLogStore.STATUS_SENT.equals(record.status)) {
            receipts.markSms(eventId, jobId, PageroLeadReceiptStore.SMS_SENT, "");
        } else if (MessageLogStore.STATUS_FAILED.equals(record.status)) {
            receipts.markSms(eventId, jobId, PageroLeadReceiptStore.SMS_FAILED, record.error);
        } else if (MessageLogStore.STATUS_SKIPPED.equals(record.status)
                || MessageLogStore.STATUS_CANCELLED.equals(record.status)
                || MessageLogStore.STATUS_SCHEDULED.equals(record.status)) {
            receipts.markSms(eventId, jobId, PageroLeadReceiptStore.SMS_NOT_SENT,
                    record.error.isEmpty() ? "발송 대기 또는 미발송" : record.error);
        } else {
            receipts.markSms(eventId, jobId, PageroLeadReceiptStore.SMS_SENDING, "");
        }
    }

    static String render(Context context, String template, PageroLead lead, Customer customer) {
        String customerName = customer == null ? lead.customerName : customer.displayName;
        if (customerName == null || customerName.trim().isEmpty()
                || "이름 없는 고객".equals(customerName.trim())) customerName = "고객";
        String pageName = lead.pageName().isEmpty() ? "페이지로" : lead.pageName();
        String inquiry = lead.inquiryContent;
        if (inquiry.isEmpty() && !lead.inquiryFields.isEmpty()) {
            inquiry = lead.inquiryFields.get(0).value;
        }
        String result = template == null ? "" : template;
        result = result.replace("{고객명}", customerName.trim());
        result = result.replace("{페이지명}", pageName);
        result = result.replace("{문의내용}", inquiry);
        result = result.replace("{회사명}", safe(AuthSessionStore.brand(context)));
        result = result.replace("{상호명}", safe(AuthSessionStore.brand(context)));
        result = result.replace("{담당자명}", safe(AuthSessionStore.name(context)));
        result = result.replace("{내이름}", safe(AuthSessionStore.name(context)));
        return result.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
