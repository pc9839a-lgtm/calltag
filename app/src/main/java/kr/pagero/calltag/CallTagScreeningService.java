package kr.pagero.calltag;

import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

public final class CallTagScreeningService extends CallScreeningService {
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
            SettingsStore.setCallerScreeningStatus(this, "발신 통화라 수신 고객정보를 표시하지 않았습니다.");
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
                SettingsStore.setCallerScreeningStatus(this, "제외번호라 고객정보를 표시하지 않았습니다.");
                return;
            }
            Customer customer = db.findByPhone(phone);
            if (customer == null) {
                SettingsStore.setCallerScreeningStatus(this, "콜태그에 등록되지 않은 번호입니다.");
                return;
            }

            String memo = CustomerInsightResolver.latestMemo(db, customer);
            String stageColor = db.stageColor(customer.relationStatus);
            boolean overlayShown = CallerOverlayManager.show(this, customer, memo, stageColor);
            if (overlayShown) {
                CallerOverlayCallStateWatcher.start(this);
                SettingsStore.setCallerScreeningStatus(this,
                        "전화 화면 위에 고객정보 오버레이를 표시했습니다.");
                return;
            }

            boolean posted = CallPopupNotificationManager.showIncoming(
                    this, customer, memo, stageColor);
            SettingsStore.setCallerScreeningStatus(this,
                    posted
                            ? "오버레이 권한이 없어 수신 알림 팝업으로 대신 표시했습니다."
                            : "오버레이와 수신 알림을 모두 표시하지 못했습니다.");
        } finally {
            db.close();
        }
    }
}
