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
    private static final String CHANNEL_ID = "calltag_caller_info";

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
            showCustomerNotification(customer, CustomerInsightResolver.latestMemo(db, customer));
        } finally {
            db.close();
        }
    }

    private void showCustomerNotification(Customer customer, String memo) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "수신 고객정보", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("전화가 올 때 저장된 고객 단계와 최근 메모를 표시합니다.");
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);

        Intent detail = new Intent(this, CustomerDetailActivity.class)
                .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customer.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                this,
                (int) (customer.id & 0x7fffffff),
                detail,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String stage = customer.relationStatus == null || customer.relationStatus.trim().isEmpty()
                ? "영업 단계 미지정" : customer.relationStatus.trim();
        String body = memo.isEmpty() ? stage : stage + "\n최근 메모 · " + memo;

        Notification publicVersion = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(customer.displayName)
                .setContentText("등록된 고객 전화")
                .build();

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(customer.displayName + " 고객 전화")
                .setContentText(stage)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_CALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .build();
        try {
            manager.notify(6400 + Math.abs(customer.normalizedPhone.hashCode() % 1000), notification);
        } catch (RuntimeException ignored) {
            // Notification permission or OEM restrictions can block delivery.
        }
    }
}
