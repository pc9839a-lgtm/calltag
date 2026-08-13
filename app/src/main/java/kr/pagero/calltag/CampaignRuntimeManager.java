package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;

public final class CampaignRuntimeManager {
    private static final long FIRST_DELAY_MS = 1_500L;
    private static final long RECIPIENT_INTERVAL_MS = 4_000L;
    private static final int AUTO_PAUSE_FAILURE_THRESHOLD = 3;

    private CampaignRuntimeManager() {}

    public static int requireActiveSubscription(Context context) {
        int subscriptionId = selectedActiveSubscriptionId(context);
        if (!SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
            throw new IllegalArgumentException("활성 SIM을 확인할 수 없습니다. 문자 설정에서 발송 회선을 다시 선택해주세요.");
        }
        return subscriptionId;
    }

    public static int selectedActiveSubscriptionId(Context context) {
        int selected = MessageAutomationStore.selectedSubscriptionId(context);
        if (SimProfileManager.isActive(context, selected)) return selected;
        int fallback = SimProfileManager.selectedOrDefault(context);
        return SimProfileManager.isActive(context, fallback)
                ? fallback : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    public static void requireNoBlockingCampaign(Context context, String exceptCampaignId) {
        CampaignStore store = new CampaignStore(context);
        try {
            CampaignStore.Campaign blocking = store.findBlockingCampaign(exceptCampaignId);
            if (blocking != null) {
                throw new IllegalArgumentException("진행 중이거나 일시정지된 캠페인 ‘"
                        + blocking.name + "’을 먼저 완료·취소해주세요.");
            }
        } finally {
            store.close();
        }
    }

    public static boolean allowSend(Context context, MessageRecord record) {
        if (record == null) return false;
        String campaignId = campaignIdForMessage(context, record.id);
        if (campaignId.isEmpty()) {
            if (!SimProfileManager.isActive(context, record.subscriptionId)) {
                MessageLogStore messages = new MessageLogStore(context);
                try {
                    messages.markFailed(record.id,
                            "선택한 문자 SIM을 사용할 수 없습니다. 발송 회선을 다시 선택해주세요.");
                } finally {
                    messages.close();
                }
                DiagnosticEventStore.record(context, "SIM 발송 차단", record.id,
                        "활성 회선 불일치");
                return false;
            }
            return true;
        }

        CampaignStore campaigns = new CampaignStore(context);
        try {
            CampaignStore.Campaign campaign = campaigns.find(campaignId);
            if (campaign == null) {
                MessageLogStore messages = new MessageLogStore(context);
                try {
                    messages.markFailed(record.id,
                            "연결된 캠페인 정보를 찾을 수 없어 발송하지 않았습니다.");
                } finally {
                    messages.close();
                }
                DiagnosticEventStore.record(context, "캠페인 발송 차단", record.id,
                        "캠페인 정보 없음");
                return false;
            }
            if (CampaignStore.STATUS_PAUSED.equals(campaign.status)) return false;
            if (CampaignStore.STATUS_CANCELLED.equals(campaign.status)
                    || CampaignStore.STATUS_COMPLETED.equals(campaign.status)) {
                return false;
            }

            int selected = selectedActiveSubscriptionId(context);
            boolean campaignSimActive = SimProfileManager.isActive(context, campaign.subscriptionId);
            if (!campaignSimActive || selected != campaign.subscriptionId) {
                String reason = !campaignSimActive
                        ? "캠페인에 지정된 SIM이 제거되었거나 비활성화되었습니다."
                        : "문자 설정의 발송 회선이 캠페인 시작 때와 달라졌습니다.";
                pause(context, campaignId, reason + " 회선을 확인한 뒤 발송을 재개해주세요.");
                return false;
            }

            CampaignStore.Campaign running = campaigns.findRunningCampaign(campaignId);
            if (running != null) {
                pause(context, campaignId,
                        "다른 캠페인 ‘" + running.name + "’이 발송 중이라 충돌을 막기 위해 일시정지했습니다.");
                return false;
            }
            campaigns.setRunning(campaignId);
            return true;
        } finally {
            campaigns.close();
        }
    }

    public static int pause(Context context, String campaignId, String reason) {
        String safeReason = clean(reason, "사용자가 캠페인 발송을 일시정지했습니다.");
        CampaignStore campaigns = new CampaignStore(context);
        MessageLogStore messages = new MessageLogStore(context);
        int paused = 0;
        try {
            CampaignStore.Campaign campaign = campaigns.find(campaignId);
            if (campaign == null) return 0;
            campaigns.setPaused(campaignId, safeReason);
            for (CampaignStore.Recipient recipient : campaigns.recipients(campaignId)) {
                if (recipient.messageId <= 0L) continue;
                MessageRecord record = messages.find(recipient.messageId);
                if (record == null) continue;
                if (MessageLogStore.STATUS_SCHEDULED.equals(record.status)
                        || MessageLogStore.STATUS_READY.equals(record.status)) {
                    MessageScheduler.cancel(context, record.id);
                    resetToScheduled(messages, record.id, record.scheduledAt, record.subscriptionId,
                            safeReason);
                    campaigns.replaceRecipientJob(recipient.id, record.id,
                            MessageLogStore.STATUS_SCHEDULED, safeReason, record.scheduledAt);
                    paused++;
                }
            }
            campaigns.sync(context, campaignId);
        } finally {
            messages.close();
            campaigns.close();
        }
        DiagnosticEventStore.record(context, "캠페인 일시정지", 0L,
                "남은 작업 " + paused + "건");
        notifyChanged(context);
        return paused;
    }

    public static int resume(Context context, String campaignId) {
        int subscriptionId = requireActiveSubscription(context);
        requireNoBlockingCampaign(context, campaignId);

        CampaignStore campaigns = new CampaignStore(context);
        MessageLogStore messages = new MessageLogStore(context);
        int resumed = 0;
        try {
            CampaignStore.Campaign campaign = campaigns.find(campaignId);
            if (campaign == null) return 0;
            if (!CampaignStore.STATUS_PAUSED.equals(campaign.status)) {
                throw new IllegalArgumentException("일시정지된 캠페인만 재개할 수 있습니다.");
            }

            long baseAt = System.currentTimeMillis() + FIRST_DELAY_MS;
            for (CampaignStore.Recipient recipient : campaigns.recipients(campaignId)) {
                if (recipient.messageId <= 0L) continue;
                MessageRecord record = messages.find(recipient.messageId);
                if (record == null) continue;
                if (!MessageLogStore.STATUS_SCHEDULED.equals(record.status)
                        && !MessageLogStore.STATUS_READY.equals(record.status)) continue;

                long when = baseAt + resumed * RECIPIENT_INTERVAL_MS;
                MessageScheduler.cancel(context, record.id);
                resetToScheduled(messages, record.id, when, subscriptionId, "");
                campaigns.replaceRecipientJob(recipient.id, record.id,
                        MessageLogStore.STATUS_SCHEDULED, "", when);
                MessageScheduler.schedule(context, record.id, when);
                resumed++;
            }
            campaigns.setResumed(campaignId, subscriptionId, baseAt);
            campaigns.updateCampaignStatus(campaignId);
        } finally {
            messages.close();
            campaigns.close();
        }
        DiagnosticEventStore.record(context, "캠페인 재개", 0L,
                "재등록 " + resumed + "건");
        notifyChanged(context);
        return resumed;
    }

    public static void onSendResult(Context context, long messageId,
                                    boolean success, int resultCode) {
        String campaignId = campaignIdForMessage(context, messageId);
        if (campaignId.isEmpty()) return;

        CampaignStore campaigns = new CampaignStore(context);
        try {
            if (success) {
                campaigns.resetConsecutiveFailures(campaignId);
                campaigns.sync(context, campaignId);
                return;
            }

            if (isTransportFailure(resultCode)) {
                int failures = campaigns.incrementConsecutiveFailures(campaignId);
                campaigns.sync(context, campaignId);
                if (failures >= AUTO_PAUSE_FAILURE_THRESHOLD) {
                    pause(context, campaignId,
                            "문자 통신 오류가 " + failures
                                    + "회 연속 발생해 남은 발송을 자동 일시정지했습니다.");
                }
            } else {
                campaigns.sync(context, campaignId);
            }
        } finally {
            campaigns.close();
        }
    }

    /**
     * message_jobs.campaign_id is also used as an external-event idempotency namespace by
     * non-campaign automation (for example PageRo inquiries). Only CAMPAIGN_SEND jobs may enter
     * the campaign runtime state machine.
     */
    public static String campaignIdForMessage(Context context, long messageId) {
        if (messageId <= 0L) return "";
        MessageLogStore messages = new MessageLogStore(context);
        try (Cursor cursor = messages.getReadableDatabase().query(
                "message_jobs", new String[]{"campaign_id", "trigger_type"}, "id=?",
                new String[]{String.valueOf(messageId)}, null, null, null, "1")) {
            if (!cursor.moveToFirst()) return "";
            String trigger = cursor.getString(1);
            if (!MessageAutomationManager.TRIGGER_CAMPAIGN.equals(trigger)) return "";
            String value = cursor.getString(0);
            return value == null ? "" : value.trim();
        } finally {
            messages.close();
        }
    }

    private static void resetToScheduled(MessageLogStore store, long messageId,
                                         long scheduledAt, int subscriptionId,
                                         String reason) {
        ContentValues values = new ContentValues();
        values.put("status", MessageLogStore.STATUS_SCHEDULED);
        values.put("scheduled_at", scheduledAt);
        values.put("subscription_id", subscriptionId);
        values.put("error", reason == null ? "" : reason);
        values.put("updated_at", System.currentTimeMillis());
        store.getWritableDatabase().update("message_jobs", values, "id=?",
                new String[]{String.valueOf(messageId)});
    }

    private static boolean isTransportFailure(int resultCode) {
        return resultCode == SmsManager.RESULT_ERROR_NO_SERVICE
                || resultCode == SmsManager.RESULT_ERROR_RADIO_OFF
                || resultCode == SmsManager.RESULT_ERROR_GENERIC_FAILURE;
    }

    private static void notifyChanged(Context context) {
        context.sendBroadcast(new android.content.Intent(MessageSectionView.ACTION_CHANGED)
                .setPackage(context.getPackageName()));
    }

    private static String clean(String value, String fallback) {
        String result = value == null ? "" : value.trim();
        return result.isEmpty() ? fallback : result;
    }
}
