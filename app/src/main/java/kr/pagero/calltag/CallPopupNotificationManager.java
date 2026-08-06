package kr.pagero.calltag;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.provider.CallLog;
import android.provider.Settings;

public final class CallPopupNotificationManager {
    public static final String INCOMING_CHANNEL_ID = "calltag_incoming_popup_v5";
    public static final String POST_CALL_CHANNEL_ID = "calltag_post_call_popup_v5";

    private static final String[] LEGACY_CHANNEL_IDS = {
            "calltag_caller_info_v2",
            "calltag_caller_info_v3",
            "calltag_caller_info_v4",
            "calltag_incoming_customer_popup_v5",
            "calltag_post_call",
            "calltag_post_call_popup_v2",
            "calltag_post_call_popup_v3",
            "calltag_post_call_popup_v4"
    };

    private CallPopupNotificationManager() {}

    public static void ensureChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel incoming = new NotificationChannel(
                INCOMING_CHANNEL_ID,
                "전화 수신 고객정보 팝업",
                NotificationManager.IMPORTANCE_HIGH);
        incoming.setDescription("전화가 올 때 콜태그 고객명과 최근 메모를 표시합니다.");
        incoming.enableVibration(true);
        incoming.setVibrationPattern(new long[]{0L, 120L, 80L, 120L});
        incoming.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audio);
        incoming.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        incoming.setShowBadge(false);
        manager.createNotificationChannel(incoming);

        NotificationChannel postCall = new NotificationChannel(
                POST_CALL_CHANNEL_ID,
                "통화 종료 큰 정리 화면",
                NotificationManager.IMPORTANCE_HIGH);
        postCall.setDescription("통화가 끝난 뒤 메모와 다음 할 일을 남기는 큰 정리 화면을 표시합니다.");
        postCall.enableVibration(true);
        postCall.setVibrationPattern(new long[]{0L, 140L, 80L, 140L});
        postCall.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audio);
        postCall.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        postCall.setShowBadge(true);
        manager.createNotificationChannel(postCall);

        for (String legacyId : LEGACY_CHANNEL_IDS) {
            try {
                manager.deleteNotificationChannel(legacyId);
            } catch (RuntimeException ignored) {
                // Fresh installations do not have legacy channels.
            }
        }
    }

    public static boolean isPopupReady(Context context, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        ensureChannels(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || !manager.areNotificationsEnabled()) return false;
        NotificationChannel channel = manager.getNotificationChannel(channelId);
        return channel != null && channel.getImportance() >= NotificationManager.IMPORTANCE_HIGH;
    }

    public static boolean canUsePostCallFullScreen(Context context) {
        if (!isPopupReady(context, POST_CALL_CHANNEL_ID)) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager != null && manager.canUseFullScreenIntent();
    }

    public static void openChannelSettings(Context context, String channelId) {
        try {
            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (RuntimeException ignored) {
            try {
                Intent fallback = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            } catch (RuntimeException ignoredAgain) {
                // Notification settings are not exposed by this device.
            }
        }
    }

    public static boolean showIncoming(Context context, Customer customer,
                                       String memo, String stageColor) {
        if (customer == null) return false;
        ensureChannels(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return false;

        String stage = safe(customer.relationStatus, "상태 미지정");
        String compactMemo = compactFirstLine(memo);
        int notificationId = 6400 + Math.abs(customer.normalizedPhone.hashCode() % 1000);

        Intent open = new Intent(context, CustomerDetailActivity.class)
                .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customer.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = BackgroundActivityLaunchCompat.activity(
                context,
                notificationId,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String content = compactMemo.isEmpty() ? stage : "최근 메모 · " + compactMemo;
        String expanded = stage + "\n" + (compactMemo.isEmpty()
                ? customer.primaryPhone
                : "최근 메모 · " + safe(memo, compactMemo));

        Notification notification = new Notification.Builder(context, INCOMING_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(customer.displayName + " · " + stage)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(expanded))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setTimeoutAfter(45_000L)
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(buildIncomingPublic(
                        context, customer.displayName, stage, compactMemo))
                .setOnlyAlertOnce(false)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setColor(parseColor(stageColor))
                .setColorized(true)
                .build();
        try {
            manager.notify(notificationId, notification);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * 통화 종료 화면이 Android의 백그라운드 실행 제한으로 표시되지 않으면,
     * 권한이 있는 기기에서는 full-screen intent로 큰 정리 화면을 즉시 표시한다.
     * 권한이 없으면 동일 알림이 heads-up으로 남아 사용자가 바로 열 수 있다.
     */
    public static boolean showPostCall(Context context, CallRecord record, Customer customer,
                                       Intent reviewIntent, String memo) {
        if (record == null || reviewIntent == null) return false;
        ensureChannels(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return false;

        int notificationId = 5000 + (int) (record.id % 100000L);
        PendingIntent pending = BackgroundActivityLaunchCompat.activity(
                context,
                (int) (record.id & 0x7fffffff),
                reviewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String callLabel = callTypeLabel(record);
        String person = customer == null
                ? safe(record.cachedName, record.phone)
                : customer.displayName;
        String compactMemo = compactFirstLine(memo);
        String title = callLabel + " · " + person;
        String content;
        String expanded;
        if (customer == null) {
            content = needsDeferredHandling(record)
                    ? "다시 전화하거나 할 일을 등록하세요."
                    : "통화 메모와 다음 할 일을 남기세요.";
            expanded = record.phone + "\n" + content;
        } else {
            String stage = safe(customer.relationStatus, "상태 미지정");
            content = compactMemo.isEmpty() ? stage : "최근 메모 · " + compactMemo;
            expanded = stage + (compactMemo.isEmpty()
                    ? "" : "\n최근 메모 · " + safe(memo, compactMemo));
        }

        Notification.Builder builder = new Notification.Builder(context, POST_CALL_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(expanded))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setTimeoutAfter(180_000L)
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setOnlyAlertOnce(false)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setColor(context.getColor(R.color.primary))
                .setColorized(true);
        if (canUsePostCallFullScreen(context)) {
            builder.setFullScreenIntent(pending, true);
        }
        try {
            manager.notify(notificationId, builder.build());
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Notification buildIncomingPublic(Context context, String name,
                                                      String stage, String memo) {
        int privacy = SettingsStore.callerPrivacyMode(context);
        String text;
        if (privacy == SettingsStore.CALLER_PRIVACY_MEMO && !memo.isEmpty()) {
            text = "최근 메모 · " + memo;
        } else if (privacy >= SettingsStore.CALLER_PRIVACY_STAGE) {
            text = stage;
        } else {
            text = "등록된 고객 전화";
        }
        return new Notification.Builder(context, INCOMING_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(name)
                .setContentText(text)
                .build();
    }

    private static boolean needsDeferredHandling(CallRecord record) {
        return record.type == CallLog.Calls.MISSED_TYPE
                || record.type == CallLog.Calls.REJECTED_TYPE
                || (record.type == CallLog.Calls.OUTGOING_TYPE && record.durationSec == 0L);
    }

    private static String callTypeLabel(CallRecord record) {
        if (record.type == CallLog.Calls.MISSED_TYPE) return "부재중 전화";
        if (record.type == CallLog.Calls.REJECTED_TYPE) return "거절한 전화";
        if (record.type == CallLog.Calls.OUTGOING_TYPE && record.durationSec == 0L) {
            return "발신 · 연결 안 됨";
        }
        if (record.type == CallLog.Calls.OUTGOING_TYPE) return "발신 통화 종료";
        return "수신 통화 종료";
    }

    private static String compactFirstLine(String value) {
        if (value == null) return "";
        String compact = value.trim().replaceAll("\\s+", " ");
        return compact.length() <= 58 ? compact : compact.substring(0, 55) + "…";
    }

    private static String safe(String value, String fallback) {
        String safe = value == null ? "" : value.trim();
        return safe.isEmpty() ? fallback : safe;
    }

    private static int parseColor(String value) {
        try {
            return Color.parseColor(value);
        } catch (RuntimeException ignored) {
            return Color.parseColor("#4389FF");
        }
    }
}
