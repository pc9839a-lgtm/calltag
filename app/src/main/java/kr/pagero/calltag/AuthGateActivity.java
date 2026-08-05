package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 앱 시작 시 로그인, 추천 링크, 이용권과 필수 설정을 확인하는 사용자용 로딩 화면이다. */
public final class AuthGateActivity extends Activity {
    private static final long MIN_LOADING_MS = 750L;
    private static final long MAX_LOADING_MS = 8_000L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long loadingStartedAt;
    private boolean routed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PendingReferralStore.capture(this, getIntent());
        loadingStartedAt = System.currentTimeMillis();
        setContentView(R.layout.activity_auth_gate);
        handler.postDelayed(this::routeFromCachedState, MAX_LOADING_MS);
        checkAccount();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        PendingReferralStore.capture(this, intent);
        if (!routed) checkAccount();
    }

    private void checkAccount() {
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            routeAfterLoading(this::openLogin);
            return;
        }
        executor.execute(() -> {
            try {
                JSONObject response = AuthApiClient.refresh(session);
                AuthSessionStore.save(this, response);
                applyPendingReferral(session);
                refreshEntitlement(session);
                CallTagSyncManager.request(this, false);
                runOnUiThread(() -> routeAfterLoading(this::openDestination));
            } catch (Exception error) {
                runOnUiThread(this::routeFromCachedState);
            }
        });
    }

    private void applyPendingReferral(String session) {
        String code = PendingReferralStore.peek(this);
        if (code.isEmpty()) return;
        try {
            JSONObject response = AuthApiClient.applyReferral(session, code);
            ReferralStateStore.saveMe(this, response);
            FeatureEntitlementStore.saveServerEntitlement(this, response);
            PendingReferralStore.clear(this);
        } catch (AuthApiClient.ApiException error) {
            if (isPermanentReferralError(error.code)) PendingReferralStore.clear(this);
        } catch (Exception ignored) {
            // 일시적인 연결 실패는 다음 앱 실행에서 다시 시도한다.
        }
    }

    private void refreshEntitlement(String session) {
        try {
            FeatureEntitlementStore.saveServerEntitlement(
                    this,
                    AuthApiClient.billingEntitlements(session));
        } catch (Exception ignored) {
            // 마지막 서버 시각과 이용권 캐시로 안전하게 판정한다.
        }
    }

    private boolean isPermanentReferralError(String code) {
        return "SELF_REFERRAL".equals(code)
                || "REFERRAL_ALREADY_APPLIED".equals(code)
                || "PAID_CONVERSION_COMPLETED".equals(code)
                || "REFERRAL_CODE_NOT_FOUND".equals(code);
    }

    private void routeFromCachedState() {
        if (routed || isFinishing()) return;
        routeAfterLoading(AuthSessionStore.hasSession(this)
                ? this::openDestination : this::openLogin);
    }

    private void routeAfterLoading(Runnable action) {
        if (routed || isFinishing()) return;
        long elapsed = System.currentTimeMillis() - loadingStartedAt;
        long delay = Math.max(0L, MIN_LOADING_MS - elapsed);
        handler.postDelayed(() -> {
            if (routed || isFinishing()) return;
            routed = true;
            handler.removeCallbacksAndMessages(null);
            action.run();
        }, delay);
    }

    private void openDestination() {
        if (!SetupRequirements.isReady(this)) {
            startActivity(SetupRequirements.requiredSetupIntent(this)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }
        Class<?> destination = EntitlementNoticeActivity.shouldOpen(this)
                ? EntitlementNoticeActivity.class : MainActivity.class;
        startActivity(new Intent(this, destination)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }
}
