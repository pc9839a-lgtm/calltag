package kr.pagero.calltag;

import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

public final class CallTagScreeningService extends CallScreeningService {
    @Override
    public void onScreenCall(Call.Details callDetails) {
        String device = deviceSummary();
        boolean roleHeld = SetupRequirements.hasScreeningRole(this);
        CrashTelemetryStore.record(this, "call_screening", "invoked",
                device + ",role=" + roleHeld);
        SettingsStore.setCallerScreeningStatus(this,
                "수신정보 서비스가 호출되었습니다. · " + device);

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
            SettingsStore.setCallerScreeningStatus(this,
                    "발신 통화라 수신 고객정보를 표시하지 않았습니다. · " + device);
            return;
        }

        Uri handle = callDetails.getHandle();
        if (handle == null || handle.getSchemeSpecificPart() == null) {
            SettingsStore.setCallerScreeningStatus(this,
                    "전화번호를 전달받지 못했습니다. · " + device);
            CrashTelemetryStore.record(this, "call_screening", "missing_number", device);
            return;
        }
        String phone = handle.getSchemeSpecificPart();
        if (PhoneNumberNormalizer.normalize(phone).length() < 8) {
            SettingsStore.setCallerScreeningStatus(this,
                    "표시할 수 없는 발신번호입니다. · " + device);
            return;
        }

        CallTagDbHelper db = new CallTagDbHelper(this);
        try {
            if (db.isExcluded(phone)) {
                SettingsStore.setCallerScreeningStatus(this,
                        "제외번호라 고객정보를 표시하지 않았습니다. · " + device);
                return;
            }
            Customer customer = db.findByPhone(phone);
            if (customer == null) {
                SettingsStore.setCallerScreeningStatus(this,
                        "수신 서비스는 정상 호출됐지만 콜태그에 등록되지 않은 번호입니다: "
                                + phone + " · " + device);
                return;
            }

            String memo = CustomerInsightResolver.latestMemo(db, customer);
            String stageColor = db.stageColor(customer.relationStatus);
            ContextSnapshot snapshot = new ContextSnapshot(customer, memo, stageColor);
            SettingsStore.setCallerScreeningStatus(this,
                    "등록 고객을 찾았습니다. 고객명과 최근 메모 표시를 시도합니다: "
                            + customer.displayName + " · " + device);

            boolean requested = CallerOverlayManager.show(
                    getApplicationContext(), customer, memo, stageColor, shown -> {
                        if (shown) {
                            SettingsStore.setCallerScreeningStatus(getApplicationContext(),
                                    "전화 화면 위 고객명·최근 메모 표시 성공: "
                                            + snapshot.customer.displayName + " · " + device);
                            CrashTelemetryStore.record(getApplicationContext(),
                                    "call_screening", "overlay_shown", device);
                            return;
                        }
                        postFallback(snapshot, "오버레이 창 추가 실패", device);
                    });

            if (!requested) {
                postFallback(snapshot, "다른 앱 위 표시 권한 없음", device);
            }
        } catch (RuntimeException error) {
            SettingsStore.setCallerScreeningStatus(this,
                    "수신 고객정보 처리 중 오류가 발생했습니다. · " + device);
            CrashTelemetryStore.record(this, "call_screening", "failed",
                    device + "," + error.getClass().getSimpleName());
        } finally {
            db.close();
        }
    }

    private void postFallback(ContextSnapshot snapshot, String reason, String device) {
        boolean posted = CallPopupNotificationManager.showIncoming(
                getApplicationContext(), snapshot.customer, snapshot.memo, snapshot.stageColor);
        SettingsStore.setCallerScreeningStatus(getApplicationContext(),
                posted
                        ? reason + " · 수신 알림으로 고객명과 메모를 대신 표시했습니다. · " + device
                        : reason + " · 수신 알림도 표시하지 못했습니다. · " + device);
        CrashTelemetryStore.record(getApplicationContext(), "call_screening",
                posted ? "notification_fallback" : "fallback_failed", device);
    }

    private String deviceSummary() {
        String manufacturer = Build.MANUFACTURER == null ? "unknown" : Build.MANUFACTURER.trim();
        String model = Build.MODEL == null ? "unknown" : Build.MODEL.trim();
        return manufacturer + "/" + model + "/api" + Build.VERSION.SDK_INT;
    }

    private static final class ContextSnapshot {
        final Customer customer;
        final String memo;
        final String stageColor;

        ContextSnapshot(Customer customer, String memo, String stageColor) {
            this.customer = customer;
            this.memo = memo;
            this.stageColor = stageColor;
        }
    }
}
