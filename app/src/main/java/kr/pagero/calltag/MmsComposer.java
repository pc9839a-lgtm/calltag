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

/** Opens the device messaging app with an image, recipient and body prefilled. */
public final class MmsComposer {
    public static final String REQUIRED_PREFIX = "이미지 문자 전송 필요";
    public static final String OPENED_PREFIX = "이미지 문자 작성창 열림";

    private static final String PREFS = "calltag_mms_compose_v1";
    private static final String KEY_PREFIX = "attachment_";
    private static final String CHANNEL_ID = "calltag_mms_follow_up";

    private MmsComposer() {}

    public static void remember(Context context, long messageId, String imageRef) {
        if (context == null || messageId <= 0L) return;
        prefs(context).edit().putString(KEY_PREFIX + messageId,
                imageRef == null ? "" : imageRef.trim()).apply();
    }

    public static boolean hasAttachment(Context context, long messageId) {
        return MessageAttachmentStore.exists(context, attachmentRef(context, messageId));
    }

    public static String attachmentRef(Context context, long messageId) {
        return prefs(context).getString(KEY_PREFIX + messageId, "");
    }

    public static void forget(Context context, long messageId) {
        if (context == null || messageId <= 0L) return;
        prefs(context).edit().remove(KEY_PREFIX + messageId).apply();
    }

    public static boolean isComposeRequired(String reason) {
        return reason != null && reason.startsWith(REQUIRED_PREFIX);
    }

    public static boolean isComposerOpened(String reason) {
        return reason != null && reason.startsWith(OPENED_PREFIX);
    }

    public static boolean openComposer(Context context, long messageId) {
        if (context == null || messageId <= 0L) return false;
        MessageLogStore store = new MessageLogStore(context);
        try {
            MessageRecord record = store.find(messageId);
            String imageRef = attachmentRef(context, messageId);
            Uri image = MessageAttachmentStore.shareUri(context, imageRef);
            if (record == null || image == null) {
                if (record != null) store.markFailed(messageId,
                        "첨부 이미지를 찾을 수 없습니다. 템플릿에서 이미지를 다시 선택해주세요.");
                return false;
            }

            String phone = PhoneNumberNormalizer.normalize(record.phone);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setData(Uri.parse("smsto:" + phone));
            intent.setType("image/jpeg");
            intent.putExtra("address", phone);
            intent.putExtra("sms_body", record.body);
            intent.putExtra(Intent.EXTRA_TEXT, record.body);
            intent.putExtra(Intent.EXTRA_STREAM, image);
            intent.setClipData(ClipData.newUri(context.getContentResolver(),
                    "콜태그 MMS 이미지", image));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) == null) {
                store.markFailed(messageId, "이미지 문자를 작성할 메시지 앱을 찾을 수 없습니다.");
                return false;
            }
            context.startActivity(intent);
            store.markSkipped(messageId, OPENED_PREFIX
                    + " · 최종 전송 여부는 기본 메시지 앱에서 확인해주세요.");
            cancelNotification(context, messageId);
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
        if (manager == null) return false;
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            MessageLogStore store = new MessageLogStore(context);
            try {
                store.markFailed(messageId,
                        "예약 이미지 문자를 열려면 알림 권한이 필요합니다.");
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
        MessageRecord record;
        try {
            record = store.find(messageId);
            if (record == null) return false;
            store.markSkipped(messageId, REQUIRED_PREFIX
                    + " · 예약 시간이 됐습니다. 알림을 눌러 메시지 앱에서 전송해주세요.");
        } finally {
            store.close();
        }

        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_messages)
                .setContentTitle("이미지 문자 전송이 필요합니다")
                .setContentText(record.phone + " · 눌러서 메시지 앱 열기")
                .setStyle(new android.app.Notification.BigTextStyle()
                        .bigText(record.body + "\n\n눌러서 이미지가 첨부된 메시지 작성창을 여세요."))
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
                "예약 이미지 문자", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("예약된 이미지 문자를 메시지 앱에서 전송하도록 알려줍니다.");
        manager.createNotificationChannel(channel);
    }

    private static int notificationId(long messageId) {
        return 0x4D4D0000 | ((int) messageId & 0xFFFF);
    }
}
