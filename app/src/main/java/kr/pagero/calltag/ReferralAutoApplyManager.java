package kr.pagero.calltag;

import android.content.Context;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/** 로그인 완료 후 보관된 추천코드를 서버에 최초 한 번 자동 등록한다. */
public final class ReferralAutoApplyManager {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private ReferralAutoApplyManager() {}

    public static void applyIfNeeded(Context context) {
        Context app = context.getApplicationContext();
        String session = AuthSessionStore.session(app);
        String code = PendingReferralStore.peek(app);
        if (session.isEmpty() || code.isEmpty() || !RUNNING.compareAndSet(false, true)) return;

        new Thread(() -> {
            try {
                JSONObject response = AuthApiClient.applyReferral(session, code);
                ReferralStateStore.saveMe(app, response);
                FeatureEntitlementStore.saveServerEntitlement(app, response);
                PendingReferralStore.clear(app);
            } catch (AuthApiClient.ApiException error) {
                if (isPermanent(error.code)) PendingReferralStore.clear(app);
            } catch (Exception ignored) {
                // 네트워크 오류는 다음 전면 실행에서 다시 시도한다.
            } finally {
                RUNNING.set(false);
            }
        }, "calltag-referral-auto-apply").start();
    }

    private static boolean isPermanent(String code) {
        return "SELF_REFERRAL".equals(code)
                || "REFERRAL_ALREADY_APPLIED".equals(code)
                || "PAID_CONVERSION_COMPLETED".equals(code)
                || "REFERRAL_CODE_NOT_FOUND".equals(code);
    }
}
