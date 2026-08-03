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
                SettingsStore.setCallerScreeningStatus(this,
                        "콜태그에 등록되지 않은 번호라 메모를 표시하지 않았습니다: " + phone);
                return;
            }

            String memo = CustomerInsightResolver.latestMemo(db, customer);
            String stageColor = db.stageColor(customer.relationStatus);
            String savedContactName = SystemContactLookup.displayName(this, phone);
            boolean savedContact = !savedContactName.isEmpty();
            SettingsStore.setCallerScreeningStatus(this,
                    savedContact
                            ? "저장된 연락처를 찾았습니다. 기존 이름은 유지하고 메모 오버레이를 표시합니다: "
                                    + savedContactName
                            : "미저장 번호를 찾았습니다. 고객명 없이 메모만 표시합니다.");

            ContextSnapshot snapshot = new ContextSnapshot(
                    customer, memo, stageColor, savedContactName, savedContact);
            boolean requested = CallerOverlayManager.show(
                    getApplicationContext(), customer, memo, stageColor, shown -> {
                        if (shown) {
                            CallerOverlayCallStateWatcher.start(getApplicationContext());
                            SettingsStore.setCallerScreeningStatus(getApplicationContext(),
                                    snapshot.savedContact
                                            ? "전화 화면 위 표시 성공 · 기존 연락처 이름 + 콜태그 메모: "
                                                    + snapshot.savedContactName
                                            : "전화 화면 위 표시 성공 · 미저장 번호는 콜태그 메모만 표시");
                            return;
                        }
                        postFallback(snapshot, "오버레이 창 추가 실패");
                    });

            if (!requested) {
                postFallback(snapshot, "다른 앱 위 표시 권한 없음");
            }
        } finally {
            db.close();
        }
    }

    private void postFallback(ContextSnapshot snapshot, String reason) {
        Customer notificationCustomer;
        if (snapshot.savedContact) {
            notificationCustomer = new Customer(
                    snapshot.customer.id,
                    snapshot.savedContactName,
                    snapshot.customer.primaryPhone,
                    snapshot.customer.normalizedPhone,
                    snapshot.customer.relationStatus,
                    snapshot.customer.source,
                    snapshot.customer.memo,
                    snapshot.customer.firstContactAt,
                    snapshot.customer.lastContactAt,
                    snapshot.customer.firstTransactionAt);
        } else {
            notificationCustomer = new Customer(
                    snapshot.customer.id,
                    "콜태그 메모",
                    "",
                    snapshot.customer.normalizedPhone,
                    "",
                    snapshot.customer.source,
                    snapshot.customer.memo,
                    snapshot.customer.firstContactAt,
                    snapshot.customer.lastContactAt,
                    snapshot.customer.firstTransactionAt);
        }
        boolean posted = CallPopupNotificationManager.showIncoming(
                getApplicationContext(), notificationCustomer, snapshot.memo, snapshot.stageColor);
        SettingsStore.setCallerScreeningStatus(getApplicationContext(),
                posted
                        ? reason + " · 수신 알림으로 대신 표시했습니다."
                        : reason + " · 수신 알림도 표시하지 못했습니다.");
    }

    private static final class ContextSnapshot {
        final Customer customer;
        final String memo;
        final String stageColor;
        final String savedContactName;
        final boolean savedContact;

        ContextSnapshot(Customer customer, String memo, String stageColor,
                        String savedContactName, boolean savedContact) {
            this.customer = customer;
            this.memo = memo;
            this.stageColor = stageColor;
            this.savedContactName = savedContactName;
            this.savedContact = savedContact;
        }
    }
}
