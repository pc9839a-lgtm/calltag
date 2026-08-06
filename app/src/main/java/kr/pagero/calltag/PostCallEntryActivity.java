package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 통화 종료 알림과 백그라운드 실행을 먼저 받는 안전 진입 화면이다.
 * 오래된 PendingIntent·별도 task 재사용으로 정리 화면이 열리지 않는 경우에도
 * 캐시 삭제 안내 대신 재시도 또는 고객목록 진입 경로를 제공한다.
 */
public final class PostCallEntryActivity extends Activity {
    private static final long VERIFY_DELAY_MS = 1_400L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private Button retry;
    private Button openCustomers;
    private Intent reviewIntent;
    private long callId = -1L;
    private boolean opening;

    public static Intent createIntent(android.content.Context context, Intent review) {
        Intent entry = new Intent(context, PostCallEntryActivity.class);
        if (review != null && review.getExtras() != null) {
            entry.putExtras(review.getExtras());
        }
        long id = review == null
                ? -1L : review.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        entry.setData(android.net.Uri.parse(
                "calltag://post-call/open/" + Math.max(0L, id)
                        + "/" + System.currentTimeMillis()));
        entry.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return entry;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        reviewIntent = buildReviewIntent(getIntent());
        callId = reviewIntent.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        if (callId < 0L
                || safe(reviewIntent.getStringExtra(PostCallActivity.EXTRA_PHONE)).isEmpty()) {
            showRecovery("통화 정보를 불러오지 못했습니다. 고객목록에서 직접 확인해주세요.");
            return;
        }
        openReview();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handler.removeCallbacksAndMessages(null);
        opening = false;
        reviewIntent = buildReviewIntent(intent);
        callId = reviewIntent.getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L);
        openReview();
    }

    private Intent buildReviewIntent(Intent source) {
        Intent target = new Intent(this, PostCallActivity.class);
        if (source != null && source.getExtras() != null) {
            target.putExtras(source.getExtras());
        }
        target.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return target;
    }

    private void openReview() {
        if (opening || reviewIntent == null || callId < 0L) return;
        opening = true;
        status.setText("통화 정리 화면을 여는 중입니다.");
        retry.setVisibility(View.GONE);
        openCustomers.setVisibility(View.GONE);
        PostCallLaunchReceipt.arm(this, reviewIntent);
        try {
            startActivity(reviewIntent);
        } catch (RuntimeException error) {
            opening = false;
            DiagnosticEventStore.record(this, "통화 종료 화면 실행 실패", callId,
                    error.getClass().getSimpleName());
            showRecovery("통화 정리 화면을 열지 못했습니다. 다시 열거나 고객목록으로 이동해주세요.");
            return;
        }

        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (PostCallLaunchReceipt.wasVisible(this, callId)) {
                finish();
                return;
            }
            opening = false;
            DiagnosticEventStore.record(this, "통화 종료 화면 표시 실패", callId,
                    "안전 진입 화면에서 재시도 제공");
            showRecovery("통화 정리 화면이 표시되지 않았습니다. 캐시를 지우지 말고 아래 버튼으로 다시 열어주세요.");
        }, VERIFY_DELAY_MS);
    }

    private void showRecovery(String message) {
        status.setText(message);
        retry.setVisibility(callId >= 0L ? View.VISIBLE : View.GONE);
        openCustomers.setVisibility(View.VISIBLE);
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(42), dp(22), dp(32));
        root.setBackgroundColor(getColor(R.color.background));

        TextView title = new TextView(this);
        title.setText("통화 정리");
        title.setTextSize(24f);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, matchWrap());

        status = new TextView(this);
        status.setText("통화 정리 화면을 여는 중입니다.");
        status.setTextSize(15f);
        status.setTextColor(getColor(R.color.text_secondary));
        status.setGravity(Gravity.CENTER);
        status.setLineSpacing(0f, 1.25f);
        status.setPadding(dp(18), dp(22), dp(18), dp(22));
        status.setBackgroundResource(R.drawable.bg_card);
        root.addView(status, top(18));

        retry = button("통화 정리 다시 열기", true);
        retry.setVisibility(View.GONE);
        retry.setOnClickListener(v -> openReview());
        root.addView(retry, fixedTop(52, 18));

        openCustomers = button("고객목록 열기", false);
        openCustomers.setVisibility(View.GONE);
        openCustomers.setOnClickListener(v -> openMain());
        root.addView(openCustomers, fixedTop(52, 9));
        return root;
    }

    private void openMain() {
        Class<?> destination = AuthSessionStore.hasSession(this)
                ? MainActivity.class : AuthGateActivity.class;
        startActivity(new Intent(this, destination)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private Button button(String label, boolean primary) {
        Button value = new Button(this);
        value.setText(label);
        value.setAllCaps(false);
        value.setTextSize(15f);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setTextColor(getColor(primary
                ? android.R.color.white : R.color.text_primary));
        value.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return value;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
