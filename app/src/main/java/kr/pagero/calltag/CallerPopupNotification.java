package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

public final class CallerPopupNotification {
    public static final String CHANNEL_ID = "calltag_incoming_customer_popup_v5";

    private static final String[] OLD_CHANNEL_IDS = {
            "calltag_caller_info_v2",
            "calltag_caller_info_v3",
            "calltag_caller_info_v4"
    };

    private CallerPopupNotification() {}

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "전화 수신 고객정보 팝업",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("전화가 올 때 고객 상태와 최근 메모를 상단 팝업으로 표시합니다.");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0L, 120L});
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);

        for (String oldChannelId : OLD_CHANNEL_IDS) {
            try {
                manager.deleteNotificationChannel(oldChannelId);
            } catch (RuntimeException ignored) {
                // Old channels may not exist on a fresh installation.
            }
        }
    }

    public static boolean isPopupEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;

        ensureChannel(context);
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = manager == null ? null : manager.getNotificationChannel(CHANNEL_ID);
        return channel != null && channel.getImportance() >= NotificationManager.IMPORTANCE_HIGH;
    }

    public static void openChannelSettings(Context context) {
        ensureChannel(context);
        try {
            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID);
            if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (RuntimeException error) {
            Intent fallback = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            if (!(context instanceof Activity)) fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallback);
        }
    }

    public static boolean post(Context context, Customer customer, String memo,
                               String stageColor, boolean test) {
        if (customer == null) return false;
        ensureChannel(context);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !isPopupEnabled(context)) return false;

        String stage = customer.relationStatus == null || customer.relationStatus.trim().isEmpty()
                ? "상태 미지정" : customer.relationStatus.trim();
        String compactMemo = compactFirstLine(memo);
        int notificationId = (test ? 7600 : 6400)
                + Math.abs(customer.normalizedPhone.hashCode() % 1000);

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
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                detail,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = test ? "콜태그 알림 팝업 테스트" : customer.displayName + " 고객 전화";
        String defaultText = compactMemo.isEmpty() ? stage : stage + " · " + compactMemo;
        String expandedText = compactMemo.isEmpty()
                ? stage
                : stage + "\n최근 메모 · " + memo;

        Notification publicVersion = buildPublicVersion(
                context, customer.displayName, stage, compactMemo);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(title)
                .setContentText(defaultText)
                .setStyle(new Notification.BigTextStyle().bigText(expandedText))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(test ? 12_000L : 45_000L)
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .setColor(context.getColor(R.color.primary))
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setDefaults(Notification.DEFAULT_ALL);
        }

        try {
            manager.notify(notificationId, builder.build());
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Notification buildPublicVersion(Context context, String name,
                                                   String stage, String memo) {
        int privacy = SettingsStore.callerPrivacyMode(context);
        String text;
        if (privacy == SettingsStore.CALLER_PRIVACY_MEMO && !memo.isEmpty()) {
            text = "메모 · " + memo;
        } else if (privacy >= SettingsStore.CALLER_PRIVACY_STAGE) {
            text = stage;
        } else {
            text = "등록된 고객 전화";
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        return builder.setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(name)
                .setContentText(text)
                .build();
    }

    private static String compactFirstLine(String value) {
        if (value == null) return "";
        String compact = value.trim().replaceAll("\\s+", " ");
        if (compact.length() <= 58) return compact;
        return compact.substring(0, 55) + "…";
    }
}
