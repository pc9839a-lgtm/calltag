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
    private static final String CHANNEL_ID = "calltag_caller_info_v3";

    @Override
    public void onScreenCall(Call.Details callDetails) {
        SettingsStore.setCallerScreeningStatus(this, "수신정보 서비스가 호출되었습니다.");

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

        if (!incoming) {
            SettingsStore.setCallerScreeningStatus(this, "발신 통화라 수신 팝업을 표시하지 않았습니다.");
            return;
        }

        Uri handle = callDetails.getHandle();
        if (handle == null || handle.getSchemeSpecificPart() == null) {
            SettingsStore.setCallerScreeningStatus(this, "전화번호를 전달받지 못했습니다.");
            return;
        }
        String phone = handle.getSchemeSpecificPart();
        if (PhoneNumberNormalizer.normalize(phone).length() < 8) {
            SettingsStore.setCallerScreeningStatus(this, "표시할 수 없는 발신번호입니다.");
            return;
        }

        CallTagDbHelper db = new CallTagDbHelper(this);
        try {
            if (db.isExcluded(phone)) {
                SettingsStore.setCallerScreeningStatus(this, "제외번호라 팝업을 표시하지 않았습니다.");
                return;
            }
            Customer customer = db.findByPhone(phone);
            if (customer == null) {
                SettingsStore.setCallerScreeningStatus(this, "콜태그에 등록되지 않은 번호입니다.");
                return;
            }
            String memo = CustomerInsightResolver.latestMemo(db, customer);
            showCustomerInfo(customer, memo, db.stageColor(customer.relationStatus));
        } finally {
            db.close();
        }
    }

    private void showCustomerInfo(Customer customer, String memo, String stageColor) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            SettingsStore.setCallerScreeningStatus(this, "알림 서비스를 사용할 수 없습니다.");
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "수신 고객정보", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("전화가 올 때 저장된 고객 상태와 최근 메모를 표시합니다.");
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
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        boolean directLaunchRequested = false;
        try {
            startActivity(callerInfo);
            directLaunchRequested = true;
        } catch (RuntimeException ignored) {
            // Full-screen notification below remains as the fallback path.
        }

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
            SettingsStore.setCallerScreeningStatus(this,
                    directLaunchRequested
                            ? "등록 고객을 확인해 전용 화면과 알림을 요청했습니다."
                            : "전용 화면 실행이 차단되어 알림으로 표시를 요청했습니다.");
        } catch (RuntimeException error) {
            SettingsStore.setCallerScreeningStatus(this, "시스템이 고객정보 알림을 차단했습니다.");
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
