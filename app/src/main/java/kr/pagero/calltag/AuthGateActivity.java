package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 앱 시작 시 로그인과 필수 설정을 확인하는 사용자용 로딩 화면이다. */
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
        loadingStartedAt = System.currentTimeMillis();
        setContentView(R.layout.activity_auth_gate);
        handler.postDelayed(this::routeFromCachedState, MAX_LOADING_MS);
        checkAccount();
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
                runOnUiThread(() -> routeAfterLoading(this::openDestination));
            } catch (Exception error) {
                runOnUiThread(this::routeFromCachedState);
            }
        });
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
        startActivity(new Intent(this, MainActivity.class)
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
