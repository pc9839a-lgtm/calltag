package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

/** Hands a prepared SMS or MMS to the device messaging app for user confirmation. */
public final class MmsComposer {
    public static final String REQUIRED_PREFIX = "메시지 앱에서 전송 필요";
    public static final String OPENED_PREFIX = "메시지 앱 작성창 열림";

    private static final String PREFS = "calltag_mms_compose_v1";
    private static final String KEY_PREFIX = "attachment_";
    private static final String CHANNEL_ID = "calltag_mms_follow_up";

    private MmsComposer() {}

    public static boolean remember(Context context, long messageId, String imageRef) {
        if (context == null || messageId <= 0L
                || !MessageAttachmentStore.exists(context, imageRef)) return false;
        String snapshot = MessageAttachmentStore.duplicate(
                context, imageRef, "message-" + messageId);
        String stored = snapshot.isEmpty() ? imageRef.trim() : snapshot;
        prefs(context).edit().putString(KEY_PREFIX + messageId, stored).apply();
        if (!snapshot.isEmpty() && imageRef.startsWith("message_images/draft-")) {
            MessageAttachmentStore.delete(context, imageRef);
        }
        return MessageAttachmentStore.exists(context, stored);
    }

    public static boolean hasAttachment(Context context, long messageId) {
        return MessageAttachmentStore.exists(context, attachmentRef(context, messageId));
    }

    public static String attachmentRef(Context context, long messageId) {
        return prefs(context).getString(KEY_PREFIX + messageId, "");
    }

    public static void forget(Context context, long messageId) {
        if (context == null || messageId <= 0L) return;
        String imageRef = attachmentRef(context, messageId);
        prefs(context).edit().remove(KEY_PREFIX + messageId).apply();
        if (imageRef.startsWith("message_images/message-")) {
            MessageAttachmentStore.delete(context, imageRef);
        }
    }

    public static boolean isComposeRequired(String reason) {
        return reason != null && (reason.startsWith(REQUIRED_PREFIX)
                || reason.startsWith("이미지 문자 전송 필요"));
    }

    public static boolean isComposerOpened(String reason) {
        return reason != null && (reason.startsWith(OPENED_PREFIX)
                || reason.startsWith("이미지 문자 작성창 열림"));
    }

    static boolean canOpen(MessageRecord record) {
        return record != null && !MessageAutomationStore.isCallTriggeredMessage(record)
                && (MessageLogStore.STATUS_READY.equals(record.status)
                || MessageLogStore.STATUS_SCHEDULED.equals(record.status)
                || isComposeRequired(record.error) || isComposerOpened(record.error));
    }

    static boolean openManualCopy(Activity activity, MessageRecord source) {
        if (!FeatureEntitlementStore.hasMessageAccess(activity)) return false;
        if (MessageExclusionStore.evaluate(activity, source.customerId, source.phone,
                source.triggerType).blocked) return false;
        MessageLogStore store = new MessageLogStore(activity);
        long draftId;
        try {
            draftId = store.createJobAdvanced(source.customerId, 0L, 0L, "", "",
                    source.phone, source.body, MessageAutomationManager.TRIGGER_MANUAL,
                    MessageLogStore.STATUS_READY, System.currentTimeMillis(), -1, true);
        } finally {
            store.close();
        }
        return openComposer(activity, draftId);
    }

    public static boolean openComposer(Context context, long messageId) {
        if (context == null || messageId <= 0L) return false;
        MessageLogStore store = new MessageLogStore(context);
        try {
            MessageRecord record = store.find(messageId);
            if (!canOpen(record)) return false;
            String campaignId = CampaignRuntimeManager.campaignIdForMessage(context, messageId);
            if (!campaignId.isEmpty()) {
                CampaignStore campaigns = new CampaignStore(context);
                try {
                    CampaignStore.Campaign campaign = campaigns.find(campaignId);
                    if (campaign == null || CampaignStore.STATUS_PAUSED.equals(campaign.status)
                            || CampaignStore.STATUS_CANCELLED.equals(campaign.status)) return false;
                } finally {
                    campaigns.close();
                }
            }
            if (!FeatureEntitlementStore.hasMessageAccess(context)) {
                store.markFailed(messageId, "문자 기능 이용 권한이 필요합니다.");
                return false;
            }
            if (!MessageAutomationStore.allowsQueuedMessage(context, record)) {
                store.markSkipped(messageId, "문의 문자 알림 설정이 꺼져 있습니다.");
                return false;
            }
            String lifecycleBlock = TaskMessageLifecycleManager.validateScheduledSend(context, messageId);
            if (!lifecycleBlock.isEmpty()) {
                store.markSkipped(messageId, lifecycleBlock);
                return false;
            }
            MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                    context, record.customerId, record.phone, record.triggerType);
            if (exclusion.blocked) {
                store.markSkipped(messageId, exclusion.reason);
                return false;
            }
            String imageRef = attachmentRef(context, messageId);
            Uri image = MessageAttachmentStore.shareUri(context, imageRef);
            if (!imageRef.isEmpty() && image == null) {
                store.markFailed(messageId,
                        "첨부 이미지를 찾을 수 없습니다. 템플릿에서 이미지를 다시 선택해주세요.");
                return false;
            }

            String phone = PhoneNumberNormalizer.normalize(record.phone);
            if (phone.length() < 8 || record.body.trim().isEmpty()
                    || !MessageTemplateEngine.findPlaceholders(record.body).isEmpty()) {
                store.markFailed(messageId, "받는 번호와 문자 내용을 확인해주세요.");
                return false;
            }
            Uri address = Uri.fromParts("smsto", phone, null);
            Intent intent = new Intent(Intent.ACTION_SENDTO, address);
            intent.putExtra("sms_body", record.body);
            if (image != null) {
                intent.setAction(Intent.ACTION_SEND);
                intent.setDataAndType(address, "image/jpeg");
                intent.putExtra("address", phone);
                intent.putExtra(Intent.EXTRA_TEXT, record.body);
                intent.putExtra(Intent.EXTRA_STREAM, image);
                intent.setClipData(ClipData.newUri(context.getContentResolver(),
                        "콜태그 MMS 이미지", image));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            store.markSkipped(messageId, OPENED_PREFIX
                    + " · 최종 전송 여부는 기본 메시지 앱에서 확인해주세요.");
            MessageScheduler.cancel(context, messageId);
            cancelNotification(context, messageId);
            CampaignRuntimeManager.onSendResult(context, messageId, false, Integer.MIN_VALUE);
            return true;
        } catch (RuntimeException error) {
            store.markFailed(messageId, "메시지 앱을 열지 못했습니다.");
            return false;
        } finally {
            store.close();
        }
    }

    public static boolean postComposeNotification(Context context, long messageId) {
        if (context == null || messageId <= 0L) return false;
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.areNotificationsEnabled() || (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)) {
            MessageLogStore store = new MessageLogStore(context);
            try {
                store.markFailed(messageId,
                        REQUIRED_PREFIX + " · 발송 내역에서 문자를 열어 전송해주세요.");
            } finally {
                store.close();
            }
            return false;
        }
        ensureChannel(manager);

        Intent open = new Intent(context, MmsComposeActivity.class)
                .putExtra(MmsComposeActivity.EXTRA_MESSAGE_ID, messageId)
                .setData(Uri.parse("calltag://compose-mms/" + messageId));
        PendingIntent action = PendingIntent.getActivity(context,
                (int) (messageId ^ (messageId >>> 32)), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        MessageLogStore store = new MessageLogStore(context);
        try {
            MessageRecord record = store.find(messageId);
            if (record == null) return false;
            store.markSkipped(messageId, REQUIRED_PREFIX
                    + " · 예약 시간이 됐습니다. 알림을 눌러 메시지 앱에서 전송해주세요.");
        } finally {
            store.close();
        }

        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_messages)
                .setContentTitle("보낼 문자가 준비됐습니다")
                .setContentText("눌러서 메시지 앱에서 확인하고 전송하세요.")
                .setVisibility(android.app.Notification.VISIBILITY_PRIVATE)
                .setContentIntent(action)
                .setAutoCancel(true)
                .setCategory(android.app.Notification.CATEGORY_REMINDER)
                .build();
        manager.notify(notificationId(messageId), notification);
        return true;
    }

    public static void cancelNotification(Context context, long messageId) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(notificationId(messageId));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void ensureChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "문자 전송 알림", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("준비된 문자를 메시지 앱에서 전송하도록 알려줍니다.");
        manager.createNotificationChannel(channel);
    }

    private static int notificationId(long messageId) {
        return 0x4D4D0000 | ((int) messageId & 0xFFFF);
    }
}
