package kr.pagero.calltag;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/** 페이지로 문의가 실제 고객 데이터로 반영된 뒤에만 사용자 알림을 표시한다. */
public final class PageroLeadNotificationManager {
    private static final String CHANNEL_ID = "pagero_realtime_leads";
    private static final int NOTIFICATION_ID = 4201;

    private PageroLeadNotificationManager() {}

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "페이지로 문의 알림",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("페이지로 문의가 콜태그 고객목록에 등록되면 알려드립니다.");
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    public static void showImported(Context context, int imported, int updated) {
        int total = Math.max(0, imported) + Math.max(0, updated);
        if (total <= 0 || !canNotify(context)) return;

        ensureChannel(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        Intent destination = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                destination,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text;
        if (imported > 0 && updated > 0) {
            text = "신규 문의 " + imported + "건과 기존 고객 문의 " + updated + "건이 반영되었습니다.";
        } else if (imported > 0) {
            text = "신규 문의 " + imported + "건이 고객목록에 등록되었습니다.";
        } else {
            text = "기존 고객 문의 " + updated + "건이 상담이력에 반영되었습니다.";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("페이지로 문의 접수")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(Notification.DEFAULT_ALL)
                .setNumber(total)
                .setAutoCancel(true)
                .setContentIntent(pending);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private static boolean canNotify(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
