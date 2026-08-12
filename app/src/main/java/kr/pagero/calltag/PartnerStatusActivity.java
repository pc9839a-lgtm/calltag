package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

/** 더보기 > 파트너 현황. 파트너 코드와 별도 화면으로 운영한다. */
public final class PartnerStatusActivity extends Activity {
    private static final long SUMMARY_REFRESH_MS = 5L * 60L * 1000L;

    private TextView referredCount;
    private TextView activePaidCount;
    private TextView estimatedRevenue;
    private TextView confirmedRevenue;
    private TextView refreshButton;
    private boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
        render();
        refreshIfNeeded();
    }

    private ScrollView buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, R.color.text_primary, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));
        TextView title = text("파트너 현황", 21f, R.color.text_primary, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        refreshButton = secondaryButton("새로고침");
        refreshButton.setOnClickListener(v -> refresh(true));
        header.addView(refreshButton, new LinearLayout.LayoutParams(dp(88), dp(40)));
        root.addView(header);

        LinearLayout summary = card();
        referredCount = metric(summary, "추천 회원", true);
        activePaidCount = metric(summary, "유료 이용 중", false);
        estimatedRevenue = metric(summary, "이번 달 예상 수익", false);
        confirmedRevenue = metric(summary, "누적 확정 수익", false);
        root.addView(summary, top(16));

        TextView settlement = primaryButton("정산 페이지 열기");
        settlement.setOnClickListener(v -> openSettlement());
        root.addView(settlement, fixedTop(52, 12));
        return scroll;
    }

    private void refreshIfNeeded() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        long age = System.currentTimeMillis() - value.summaryCheckedAt;
        if (value.summaryCheckedAt <= 0L || age >= SUMMARY_REFRESH_MS) {
            refresh(false);
        }
    }

    private void refresh(boolean notify) {
        if (working) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            if (notify) Toast.makeText(this, "로그인 정보를 다시 확인해주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        working = true;
        if (notify) setManualRefreshState(true);
        new Thread(() -> {
            boolean success = false;
            try {
                JSONObject summary = AuthApiClient.referralSummary(session);
                ReferralStateStore.saveSummary(this, summary);
                success = true;
            } catch (Exception ignored) {}
            boolean loaded = success;
            runOnUiThread(() -> {
                working = false;
                if (notify) setManualRefreshState(false);
                render();
                if (notify) {
                    Toast.makeText(this,
                            loaded ? "파트너 현황을 새로 확인했습니다."
                                    : "파트너 현황을 확인하지 못했습니다.",
                            loaded ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                }
            });
        }, "calltag-partner-status-refresh").start();
    }

    private void render() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        referredCount.setText(number(value.referredCount) + "명");
        activePaidCount.setText(number(value.activePaidCount) + "명");
        estimatedRevenue.setText(currency(value.estimatedRevenueKrw));
        confirmedRevenue.setText(currency(value.confirmedRevenueKrw));
    }

    private void openSettlement() {
        String dynamicUrl = ReferralStateStore.snapshot(this).partnerUrl;
        String target = dynamicUrl.isEmpty() ? PartnerSettlementActivity.SETTLEMENT_URL : dynamicUrl;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target)));
        } catch (RuntimeException error) {
            Toast.makeText(this, "정산 페이지를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setManualRefreshState(boolean value) {
        refreshButton.setEnabled(!value);
        refreshButton.setAlpha(value ? 0.55f : 1f);
        refreshButton.setText(value ? "확인 중…" : "새로고침");
    }

    private TextView metric(LinearLayout parent, String label, boolean first) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView labelView = text(label, 14f, R.color.text_secondary, false);
        row.addView(labelView, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = text("0", 17f, R.color.text_primary, true);
        row.addView(value);
        parent.addView(row, first ? wrap() : top(15));
        return value;
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(18), dp(18), dp(18), dp(18));
        view.setBackgroundResource(R.drawable.bg_card);
        return view;
    }

    private TextView primaryButton(String value) {
        TextView view = text(value, 14f, android.R.color.white, true);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(R.drawable.bg_primary_button);
        return view;
    }

    private TextView secondaryButton(String value) {
        TextView view = text(value, 14f, R.color.primary, true);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(R.drawable.bg_secondary_button);
        return view;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(color));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = wrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private String number(int value) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(Math.max(0, value));
    }

    private String currency(long value) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(Math.max(0L, value)) + "원";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
