package kr.pagero.calltag;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

public final class CallPopupNotifications {
    public static final String INCOMING_CHANNEL = "calltag_incoming_popup_v5";
    public static final String POST_CALL_CHANNEL = "calltag_post_call_popup_v2";

    private CallPopupNotifications() {}

    public static void ensureChannels(Context context) {
        NotificationManager manager = manager(context);
        if (manager == null) return;

        removeLegacyChannels(manager);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();

        NotificationChannel incoming = new NotificationChannel(
                INCOMING_CHANNEL, "전화 수신 팝업", NotificationManager.IMPORTANCE_HIGH);
        incoming.setDescription("등록 고객에게 전화가 올 때 고객 상태와 최근 메모를 상단 팝업으로 표시합니다.");
        incoming.enableVibration(true);
        incoming.setVibrationPattern(new long[]{0L, 180L, 100L, 180L});
        incoming.setSound(sound, audio);
        incoming.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        incoming.setShowBadge(true);
        manager.createNotificationChannel(incoming);

        NotificationChannel postCall = new NotificationChannel(
                POST_CALL_CHANNEL, "통화 종료 팝업", NotificationManager.IMPORTANCE_HIGH);
        postCall.setDescription("통화가 끝나면 통화 정리와 다음 할 일을 상단 팝업으로 표시합니다.");
        postCall.enableVibration(true);
        postCall.setVibrationPattern(new long[]{0L, 220L});
        postCall.setSound(sound, audio);
        postCall.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        postCall.setShowBadge(true);
        manager.createNotificationChannel(postCall);
    }

    public static boolean incomingPopupEnabled(Context context) {
        return channelEnabled(context, INCOMING_CHANNEL);
    }

    public static boolean postCallPopupEnabled(Context context) {
        return channelEnabled(context, POST_CALL_CHANNEL);
    }

    private static boolean channelEnabled(Context context, String channelId) {
        NotificationManager manager = manager(context);
        if (manager == null || !manager.areNotificationsEnabled()) return false;
        ensureChannels(context);
        NotificationChannel channel = manager.getNotificationChannel(channelId);
        return channel != null && channel.getImportance() >= NotificationManager.IMPORTANCE_HIGH;
    }

    public static void openChannelSettings(Context context, String channelId) {
        try {
            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (RuntimeException error) {
            try {
                Intent fallback = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            } catch (RuntimeException ignored) {
                // Settings screen is not available on this device.
            }
        }
    }

    public static boolean showIncoming(Context context, Customer customer, String memo,
                                       String stageColor, boolean test) {
        NotificationManager manager = manager(context);
        if (manager == null || customer == null) return false;
        ensureChannels(context);

        int notificationId = test
                ? 6499
                : 6400 + Math.abs(customer.normalizedPhone.hashCode() % 1000);
        String stage = safe(customer.relationStatus, "상태 미지정");
        String compactMemo = compactFirstLine(memo);
        String compactText = compactMemo.isEmpty() ? stage : stage + " · " + compactMemo;
        String expandedText = compactMemo.isEmpty()
                ? stage
                : stage + "\n최근 메모 · " + memo.trim();

        Intent detail = new Intent(context, CallerInfoActivity.class)
                .putExtra(CallerInfoActivity.EXTRA_CUSTOMER_ID, customer.id)
                .putExtra(CallerInfoActivity.EXTRA_NAME, customer.displayName)
                .putExtra(CallerInfoActivity.EXTRA_PHONE, customer.primaryPhone)
                .putExtra(CallerInfoActivity.EXTRA_STAGE, stage)
                .putExtra(CallerInfoActivity.EXTRA_STAGE_COLOR, stageColor)
                .putExtra(CallerInfoActivity.EXTRA_MEMO, memo)
                .putExtra(CallerInfoActivity.EXTRA_LAST_CONTACT_AT, customer.lastContactAt)
                .putExtra(CallerInfoActivity.EXTRA_NOTIFICATION_ID, notificationId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, notificationId, detail,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, INCOMING_CHANNEL)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle((test ? "팝업 테스트 · " : "") + customer.displayName + " 고객 전화")
                .setContentText(compactText)
                .setStyle(new Notification.BigTextStyle().bigText(expandedText))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(test ? 15_000L : 55_000L)
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setColor(parseColor(stageColor, context.getColor(R.color.primary)))
                .setColorized(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(buildPublicVersion(context, customer.displayName, stage, compactMemo))
                .build();
        try {
            manager.notify(notificationId, notification);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    public static boolean showPostCall(Context context, int notificationId,
                                       PendingIntent contentIntent, String title,
                                       String compactText, String expandedText) {
        NotificationManager manager = manager(context);
        if (manager == null) return false;
        ensureChannels(context);

        Notification notification = new Notification.Builder(context, POST_CALL_CHANNEL)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(title)
                .setContentText(compactText)
                .setStyle(new Notification.BigTextStyle().bigText(expandedText))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setColor(context.getColor(R.color.primary))
                .setColorized(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .build();
        try {
            manager.notify(notificationId, notification);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static Notification buildPublicVersion(Context context, String name,
                                                    String stage, String memo) {
        int privacy = SettingsStore.callerPrivacyMode(context);
        String text;
        if (privacy == SettingsStore.CALLER_PRIVACY_MEMO && !memo.isEmpty()) {
            text = stage + " · " + memo;
        } else if (privacy >= SettingsStore.CALLER_PRIVACY_STAGE) {
            text = stage;
        } else {
            text = "등록된 고객 전화";
        }
        return new Notification.Builder(context, INCOMING_CHANNEL)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(name)
                .setContentText(text)
                .build();
    }

    private static NotificationManager manager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static void removeLegacyChannels(NotificationManager manager) {
        manager.deleteNotificationChannel("calltag_caller_info_v3");
        manager.deleteNotificationChannel("calltag_caller_info_v4");
        manager.deleteNotificationChannel("calltag_post_call");
    }

    private static String compactFirstLine(String value) {
        if (value == null) return "";
        String compact = value.trim().replaceAll("\\s+", " ");
        if (compact.length() <= 62) return compact;
        return compact.substring(0, 59) + "…";
    }

    private static String safe(String value, String fallback) {
        String safe = value == null ? "" : value.trim();
        return safe.isEmpty() ? fallback : safe;
    }

    private static int parseColor(String value, int fallback) {
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
