package kr.pagero.calltag;

import android.os.SystemClock;

import java.util.concurrent.ConcurrentHashMap;

/** Process-wide debounce for Activity and popup launches. */
public final class UiLaunchGuard {
    private static final ConcurrentHashMap<String, Long> LAST = new ConcurrentHashMap<>();

    private UiLaunchGuard() {}

    public static boolean tryAcquire(String rawKey, long windowMs) {
        String key = rawKey == null ? "" : rawKey.trim();
        if (key.isEmpty()) return true;
        long now = SystemClock.elapsedRealtime();
        long safeWindow = Math.max(250L, windowMs);
        Long previous = LAST.put(key, now);
        return previous == null || now - previous >= safeWindow;
    }

    public static void release(String rawKey) {
        if (rawKey == null) return;
        LAST.remove(rawKey.trim());
    }
}
