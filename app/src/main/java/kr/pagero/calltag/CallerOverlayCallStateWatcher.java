package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telecom.TelecomManager;

public final class CallerOverlayCallStateWatcher {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final long POLL_INTERVAL_MS = 400L;
    private static final long INITIAL_GRACE_MS = 5_000L;
    private static final long MAX_WATCH_MS = 10L * 60L * 1000L;
    private static final int IDLE_CONFIRM_COUNT = 3;

    private static Context appContext;
    private static TelecomManager telecomManager;
    private static Runnable pollRunnable;
    private static long startedAt;
    private static boolean seenInCall;
    private static int consecutiveIdle;

    private CallerOverlayCallStateWatcher() {}

    public static synchronized void start(Context context) {
        stop(context);
        Context app = context.getApplicationContext();
        if (app.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) return;

        TelecomManager manager = (TelecomManager) app.getSystemService(Context.TELECOM_SERVICE);
        if (manager == null) return;

        appContext = app;
        telecomManager = manager;
        startedAt = SystemClock.elapsedRealtime();
        seenInCall = false;
        consecutiveIdle = 0;
        pollRunnable = CallerOverlayCallStateWatcher::poll;
        HANDLER.post(pollRunnable);
    }

    private static void poll() {
        Context context;
        TelecomManager manager;
        synchronized (CallerOverlayCallStateWatcher.class) {
            context = appContext;
            manager = telecomManager;
        }
        if (context == null || manager == null) return;

        boolean inCall = false;
        try {
            inCall = manager.isInCall();
        } catch (RuntimeException ignored) {
            // 권한이나 제조사 Telecom 구현 문제로 상태 조회가 실패해도 오버레이는 유지한다.
        }

        long elapsed = SystemClock.elapsedRealtime() - startedAt;
        if (inCall) {
            seenInCall = true;
            consecutiveIdle = 0;
        } else if (seenInCall) {
            consecutiveIdle++;
            if (consecutiveIdle >= IDLE_CONFIRM_COUNT) {
                CallerOverlayManager.hide(context);
                stop(context);
                return;
            }
        } else if (elapsed < INITIAL_GRACE_MS) {
            // CallScreeningService는 전화 앱보다 먼저 호출될 수 있다. 초기 IDLE은 무시한다.
            consecutiveIdle = 0;
        } else if (elapsed >= MAX_WATCH_MS) {
            // 제조사 상태 API가 끝까지 갱신되지 않는 경우를 위한 안전 종료다.
            CallerOverlayManager.hide(context);
            stop(context);
            return;
        }

        synchronized (CallerOverlayCallStateWatcher.class) {
            if (pollRunnable != null) HANDLER.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        }
    }

    public static synchronized void stop(Context context) {
        if (pollRunnable != null) HANDLER.removeCallbacks(pollRunnable);
        pollRunnable = null;
        appContext = null;
        telecomManager = null;
        startedAt = 0L;
        seenInCall = false;
        consecutiveIdle = 0;
    }
}
