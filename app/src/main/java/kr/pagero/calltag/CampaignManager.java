package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CampaignManager {
    private static final long FIRST_DELAY_MS = 1500L;
    private static final long RECIPIENT_INTERVAL_MS = 4000L;
    private static final int MAX_RECIPIENTS = 500;

    private CampaignManager() {}

    public static String create(Context context, String name, String groupId,
                                String templateId, String templateName,
                                String bodyTemplate, long startAt) {
        requireAvailable(context);
        MessageGroupStore groups = new MessageGroupStore(context);
        CampaignStore campaigns = new CampaignStore(context);
        try {
            MessageGroupStore.Group group = groups.find(groupId);
            if (group == null) throw new IllegalArgumentException("수신자 그룹을 선택해주세요.");
            List<Customer> members = groups.resolveMembers(context, group);
            if (members.isEmpty()) throw new IllegalArgumentException("그룹에 발송 가능한 고객이 없습니다.");
            if (members.size() > MAX_RECIPIENTS) {
                throw new IllegalArgumentException("한 캠페인은 최대 " + MAX_RECIPIENTS + "명까지 발송할 수 있습니다.");
            }
            MessageTemplateStore.Template template = MessageTemplateStore.get(context, templateId);
            if (template != null && MessageAttachmentStore.exists(context, template.imageRef)) {
                throw new IllegalArgumentException("단체문자는 텍스트 템플릿만 사용할 수 있습니다.");
            }

            long baseAt = Math.max(System.currentTimeMillis() + FIRST_DELAY_MS, startAt);
            CampaignStore.Campaign campaign = campaigns.create(name, group, templateId,
                    templateName, bodyTemplate, baseAt);
            Set<String> phones = new HashSet<>();
            int activeIndex = 0;
            for (Customer customer : members) {
                String normalized = PhoneNumberNormalizer.normalize(customer.primaryPhone);
                if (normalized.length() < 8 || !phones.add(normalized)) {
                    campaigns.addRecipient(campaign.id, customer, customer.primaryPhone, "",
                            0L, MessageLogStore.STATUS_SKIPPED,
                            normalized.length() < 8 ? "전화번호를 확인해주세요."
                                    : "그룹 안에서 중복된 번호입니다.", baseAt);
                    continue;
                }

                MessageTemplateEngine.RenderResult rendered = MessageTemplateEngine.render(
                        context, bodyTemplate, customer, previewCall(customer));
                if (!rendered.isReady()) {
                    campaigns.addRecipient(campaign.id, customer, customer.primaryPhone,
                            rendered.body, 0L, MessageLogStore.STATUS_SKIPPED,
                            "고객별 치환이 완료되지 않았습니다: "
                                    + MessageTemplateEngine.describeVariables(
                                    rendered.unresolvedVariables.isEmpty()
                                            ? rendered.unsupportedVariables : rendered.unresolvedVariables),
                            baseAt);
                    continue;
                }

                MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                        context, customer.id, customer.primaryPhone,
                        MessageAutomationManager.TRIGGER_CAMPAIGN);
                if (exclusion.blocked) {
                    campaigns.addRecipient(campaign.id, customer, customer.primaryPhone,
                            rendered.body, 0L, MessageLogStore.STATUS_SKIPPED,
                            exclusion.reason, baseAt);
                    continue;
                }

                long when = baseAt + activeIndex++ * RECIPIENT_INTERVAL_MS;
                addScheduledRecipient(context, campaigns, campaign, customer,
                        rendered.body, when, false, 0L);
            }
            campaigns.updateCampaignStatus(campaign.id);
            return campaign.id;
        } finally {
            campaigns.close();
            groups.close();
        }
    }

    public static void cancel(Context context, String campaignId) {
        CampaignStore campaigns = new CampaignStore(context);
        MessageLogStore messages = new MessageLogStore(context);
        try {
            for (CampaignStore.Recipient recipient : campaigns.recipients(campaignId)) {
                if (recipient.messageId <= 0L || !isActive(recipient.status)) continue;
                MessageScheduler.cancel(context, recipient.messageId);
                messages.cancel(recipient.messageId, "캠페인 발송을 취소했습니다.");
                campaigns.replaceRecipientJob(recipient.id, recipient.messageId,
                        MessageLogStore.STATUS_CANCELLED, "캠페인 발송을 취소했습니다.",
                        recipient.scheduledAt);
            }
            campaigns.updateCampaignStatus(campaignId);
        } finally {
            messages.close();
            campaigns.close();
        }
    }

    public static int retryFailed(Context context, String campaignId) {
        requireAvailable(context);
        CampaignStore campaigns = new CampaignStore(context);
        CallTagDbHelper crm = new CallTagDbHelper(context);
        try {
            CampaignStore.Campaign campaign = campaigns.find(campaignId);
            if (campaign == null) return 0;
            int retryIndex = 0;
            long baseAt = System.currentTimeMillis() + FIRST_DELAY_MS;
            for (CampaignStore.Recipient recipient : campaigns.recipients(campaignId)) {
                if (MessageLogStore.STATUS_SENT.equals(recipient.status)
                        || isActive(recipient.status)) continue;
                Customer customer = crm.findCustomerById(recipient.customerId);
                if (customer == null) {
                    campaigns.replaceRecipientJob(recipient.id, 0L,
                            MessageLogStore.STATUS_SKIPPED, "고객 정보를 찾을 수 없습니다.", baseAt);
                    continue;
                }
                MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                        context, customer.id, customer.primaryPhone,
                        MessageAutomationManager.TRIGGER_CAMPAIGN);
                if (exclusion.blocked) {
                    campaigns.replaceRecipientJob(recipient.id, 0L,
                            MessageLogStore.STATUS_SKIPPED, exclusion.reason, baseAt);
                    continue;
                }
                long when = baseAt + retryIndex * RECIPIENT_INTERVAL_MS;
                long messageId = createScheduledJob(context, campaign, customer,
                        recipient.body, when, false);
                MessageLogStore messageStore = new MessageLogStore(context);
                try {
                    MessageRecord record = messageStore.find(messageId);
                    String status = record == null ? MessageLogStore.STATUS_FAILED : record.status;
                    String reason = record == null ? "재시도 작업을 만들지 못했습니다." : record.error;
                    campaigns.replaceRecipientJob(recipient.id, messageId, status, reason, when);
                    if (record != null && MessageLogStore.STATUS_SCHEDULED.equals(record.status)) {
                        MessageScheduler.schedule(context, messageId, when);
                        retryIndex++;
                    }
                } finally {
                    messageStore.close();
                }
            }
            campaigns.updateCampaignStatus(campaignId);
            return retryIndex;
        } finally {
            crm.close();
            campaigns.close();
        }
    }

    private static void addScheduledRecipient(Context context, CampaignStore campaigns,
                                              CampaignStore.Campaign campaign, Customer customer,
                                              String body, long when, boolean force,
                                              long existingRecipientId) {
        long messageId = createScheduledJob(context, campaign, customer, body, when, force);
        MessageLogStore messages = new MessageLogStore(context);
        try {
            MessageRecord record = messages.find(messageId);
            String status = record == null ? MessageLogStore.STATUS_FAILED : record.status;
            String reason = record == null ? "발송 작업을 만들지 못했습니다." : record.error;
            if (existingRecipientId > 0L) {
                campaigns.replaceRecipientJob(existingRecipientId, messageId, status, reason, when);
            } else {
                campaigns.addRecipient(campaign.id, customer, customer.primaryPhone, body,
                        messageId, status, reason, when);
            }
            if (record != null && MessageLogStore.STATUS_SCHEDULED.equals(record.status)) {
                MessageScheduler.schedule(context, messageId, when);
            }
        } finally {
            messages.close();
        }
    }

    private static long createScheduledJob(Context context, CampaignStore.Campaign campaign,
                                           Customer customer, String body, long when,
                                           boolean force) {
        MessageLogStore messages = new MessageLogStore(context);
        try {
            return messages.createJobAdvanced(customer.id, 0L, 0L, campaign.id,
                    campaign.templateId, customer.primaryPhone, body,
                    MessageAutomationManager.TRIGGER_CAMPAIGN,
                    MessageLogStore.STATUS_SCHEDULED, when,
                    MessageAutomationStore.selectedSubscriptionId(context), force);
        } finally {
            messages.close();
        }
    }

    private static CallRecord previewCall(Customer customer) {
        return new CallRecord(0L, customer.primaryPhone, customer.displayName,
                0, System.currentTimeMillis(), 0L);
    }

    private static boolean isActive(String status) {
        return MessageLogStore.STATUS_SCHEDULED.equals(status)
                || MessageLogStore.STATUS_READY.equals(status)
                || MessageLogStore.STATUS_SENDING.equals(status);
    }

    private static void requireAvailable(Context context) {
        if (!FeatureEntitlementStore.hasMessageAccess(context)) {
            throw new IllegalArgumentException("문자자동화 이용권이 필요합니다.");
        }
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            throw new IllegalArgumentException("단체문자 발송 권한이 필요합니다.");
        }
    }
}
