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
            SettingsStore.setCallerScreeningStatus(this, "발신 통화라 수신 알림을 표시하지 않았습니다.");
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
                SettingsStore.setCallerScreeningStatus(this, "제외번호라 수신 알림을 표시하지 않았습니다.");
                return;
            }
            Customer customer = db.findByPhone(phone);
            if (customer == null) {
                SettingsStore.setCallerScreeningStatus(this, "콜태그에 등록되지 않은 번호입니다.");
                return;
            }

            String memo = CustomerInsightResolver.latestMemo(db, customer);
            boolean shown = CallerPopupNotification.post(
                    this, customer, memo, db.stageColor(customer.relationStatus), false);
            SettingsStore.setCallerScreeningStatus(this,
                    shown
                            ? "등록 고객을 확인해 수신 알림 팝업을 표시했습니다."
                            : "수신 알림 채널의 팝업 표시가 꺼져 있거나 알림이 차단되었습니다.");
        } finally {
            db.close();
        }
    }
}
