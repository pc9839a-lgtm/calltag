package kr.pagero.calltag;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

public final class CallTagScreeningService extends CallScreeningService {
    private static final String CHANNEL_ID = "calltag_caller_info_v2";

    @Override
    public void onScreenCall(Call.Details callDetails) {
        boolean incoming = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || callDetails.getCallDirection() == Call.Details.DIRECTION_INCOMING;

        if (incoming) {
            CallResponse response = new CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build();
            respondToCall(callDetails, response);
        }

        if (!incoming || !AuthSessionStore.hasSession(this)) return;
        Uri handle = callDetails.getHandle();
        if (handle == null || handle.getSchemeSpecificPart() == null) return;
        String phone = handle.getSchemeSpecificPart();
        if (PhoneNumberNormalizer.normalize(phone).length() < 8) return;

        CallTagDbHelper db = new CallTagDbHelper(this);
        try {
            if (db.isExcluded(phone)) return;
            Customer customer = db.findByPhone(phone);
            if (customer == null) return;
            String memo = CustomerInsightResolver.latestMemo(db, customer);
            showCustomerNotification(customer, memo, db.stageColor(customer.relationStatus));
        } finally {
            db.close();
        }
    }

    private void showCustomerNotification(Customer customer, String memo, String stageColor) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "수신 고객정보", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("전화가 올 때 저장된 고객 단계와 최근 메모를 표시합니다.");
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        manager.createNotificationChannel(channel);

        int notificationId = 6400 + Math.abs(customer.normalizedPhone.hashCode() % 1000);
        String stage = customer.relationStatus == null || customer.relationStatus.trim().isEmpty()
                ? "상태 미지정" : customer.relationStatus.trim();
        String compactMemo = compactFirstLine(memo);

        Intent callerInfo = new Intent(this, CallerInfoActivity.class)
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
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent fullScreen = PendingIntent.getActivity(
                this,
                notificationId,
                callerInfo,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String defaultText = compactMemo.isEmpty() ? stage : "메모 · " + compactMemo;
        String expandedText = compactMemo.isEmpty()
                ? stage
                : stage + "\n최근 메모 · " + memo;

        Notification publicVersion = buildPublicVersion(customer.displayName, stage, compactMemo);
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(customer.displayName + " 고객 전화")
                .setContentText(defaultText)
                .setStyle(new Notification.BigTextStyle().bigText(expandedText))
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .setAutoCancel(true)
                .setTimeoutAfter(45000L)
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .build();
        try {
            manager.notify(notificationId, notification);
        } catch (RuntimeException ignored) {
            // Notification permission or OEM restrictions can block delivery.
        }
    }

    private Notification buildPublicVersion(String name, String stage, String memo) {
        int privacy = SettingsStore.callerPrivacyMode(this);
        String text;
        if (privacy == SettingsStore.CALLER_PRIVACY_MEMO && !memo.isEmpty()) {
            text = "메모 · " + memo;
        } else if (privacy >= SettingsStore.CALLER_PRIVACY_STAGE) {
            text = stage;
        } else {
            text = "등록된 고객 전화";
        }
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(name)
                .setContentText(text)
                .build();
    }

    private String compactFirstLine(String value) {
        if (value == null) return "";
        String compact = value.trim().replaceAll("\\s+", " ");
        if (compact.length() <= 58) return compact;
        return compact.substring(0, 55) + "…";
    }
}
