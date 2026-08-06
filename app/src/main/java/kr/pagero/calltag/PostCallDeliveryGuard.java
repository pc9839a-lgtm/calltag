package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * Android가 백그라운드 Activity 시작 요청을 조용히 무시하는 경우를 감지해
 * 안전 진입 화면으로 한 번 더 실행하고, 그래도 화면이 확인되지 않으면 알림으로 남긴다.
 */
public final class PostCallDeliveryGuard {
    private static final long FIRST_VERIFY_DELAY_MS = 2_400L;
    private static final long FINAL_VERIFY_DELAY_MS = 2_400L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private PostCallDeliveryGuard() {}

    public static void schedule(Context context, Intent source) {
        if (context == null || source == null) return;
        Context app = context.getApplicationContext();
        Intent review = new Intent(source)
                .setClass(app, PostCallActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        long callId = review.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        if (callId < 0L) return;

        MAIN.postDelayed(() -> {
            if (PostCallLaunchReceipt.wasVisible(app, callId)) return;
            try {
                app.startActivity(PostCallEntryActivity.createIntent(app, review));
            } catch (RuntimeException ignored) {
                // 다음 확인 단계에서 알림 fallback을 남긴다.
            }

            MAIN.postDelayed(() -> {
                if (PostCallLaunchReceipt.wasVisible(app, callId)) return;
                showFallback(app, review);
            }, FINAL_VERIFY_DELAY_MS);
        }, FIRST_VERIFY_DELAY_MS);
    }

    private static void showFallback(Context context, Intent review) {
        long callId = review.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        String phone = safe(review.getStringExtra(PostCallActivity.EXTRA_PHONE));
        String cachedName = safe(review.getStringExtra(PostCallActivity.EXTRA_CACHED_NAME));
        int type = review.getIntExtra(PostCallActivity.EXTRA_CALL_TYPE, 0);
        long startedAt = review.getLongExtra(
                PostCallActivity.EXTRA_STARTED_AT, System.currentTimeMillis());
        long durationSec = Math.max(0L, review.getLongExtra(
                PostCallActivity.EXTRA_DURATION_SEC, 0L));
        if (callId < 0L || phone.isEmpty()) return;

        CallRecord record = new CallRecord(
                callId,
                phone,
                cachedName,
                type,
                startedAt,
                durationSec);
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            Customer customer = db.findByPhone(phone);
            String memo = customer == null
                    ? "" : CustomerInsightResolver.latestMemo(db, customer);
            CallPopupNotificationManager.showPostCall(
                    context, record, customer, review, memo);
        } finally {
            db.close();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
