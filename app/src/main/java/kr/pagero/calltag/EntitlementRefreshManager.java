package kr.pagero.calltag;

import android.content.Context;

import java.util.concurrent.atomic.AtomicBoolean;

/** 앱 전면 진입 시 이용권과 서버 시각을 조용히 갱신한다. */
public final class EntitlementRefreshManager {
    private static final long MIN_INTERVAL_MS = 5L * 60L * 1000L;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static volatile long lastAttemptAt;

    private EntitlementRefreshManager() {}

    public static void request(Context context, boolean force) {
        Context app = context.getApplicationContext();
        String session = AuthSessionStore.session(app);
        if (session.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastAttemptAt < MIN_INTERVAL_MS) return;
        if (!RUNNING.compareAndSet(false, true)) return;
        lastAttemptAt = now;

        new Thread(() -> {
            try {
                FeatureEntitlementStore.saveServerEntitlement(
                        app,
                        AuthApiClient.billingEntitlements(session));
            } catch (Exception ignored) {
                // 마지막 서버 시각 기반 캐시를 유지한다.
            } finally {
                RUNNING.set(false);
            }
        }, "calltag-entitlement-refresh").start();
    }
}
