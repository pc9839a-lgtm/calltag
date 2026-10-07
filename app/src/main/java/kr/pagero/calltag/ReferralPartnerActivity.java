package kr.pagero.calltag;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

/** 더보기 > 친구 추천 수익. 추천코드, 20% 수익 현황, 보안 정산센터 진입을 제공한다. */
public final class ReferralPartnerActivity extends Activity {
    private static final long CODE_REFRESH_MS = 24L * 60L * 60L * 1000L;
    private static final String DEFAULT_PARTNER_CENTER =
            "https://pagero.kr/partner?service=CALLTAG";

    private TextView codeView;
    private TextView refreshButton;
    private TextView referredCountView;
    private TextView paidCountView;
    private TextView estimatedRevenueView;
    private TextView confirmedRevenueView;
    private TextView friendBenefitView;
    private TextView myBenefitView;
    private TextView recurringView;
    private TextView partnerCenterButton;

    private JSONObject summary = new JSONObject();
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

        TextView title = text("친구 추천 수익", 21f, R.color.text_primary, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));

        refreshButton = secondaryButton("새로고침");
        refreshButton.setOnClickListener(v -> refresh(true));
        header.addView(refreshButton, new LinearLayout.LayoutParams(dp(88), dp(40)));
        root.addView(header);

        LinearLayout invite = card();
        invite.addView(text("내 추천인 코드", 14f, R.color.text_secondary, true));

        codeView = text("불러오는 중…", 28f, R.color.text_primary, true);
        codeView.setLetterSpacing(0.08f);
        invite.addView(codeView, top(9));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        TextView copy = secondaryButton("코드 복사");
        copy.setOnClickListener(v -> copyCode());
        actions.addView(copy, new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView share = primaryButton("친구에게 공유");
        share.setOnClickListener(v -> shareCode());
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        shareParams.leftMargin = dp(8);
        actions.addView(share, shareParams);
        invite.addView(actions, top(14));

        friendBenefitView = text(
                "친구 혜택 · 회원가입할 때 추천인 코드를 입력하면 무료체험 +5일",
                13f, R.color.text_secondary, false);
        friendBenefitView.setLineSpacing(0f, 1.2f);
        invite.addView(friendBenefitView, top(12));

        myBenefitView = text(
                "내 수익 · 추천 회원의 콜태그 유료 결제액의 20%",
                13f, R.color.text_primary, true);
        myBenefitView.setLineSpacing(0f, 1.2f);
        invite.addView(myBenefitView, top(8));

        recurringView = text(
                "추천 회원이 유료 구독을 유지해 새 결제가 확인될 때마다 같은 비율로 적립됩니다.",
                13f, R.color.text_secondary, false);
        recurringView.setLineSpacing(0f, 1.2f);
        invite.addView(recurringView, top(8));

        root.addView(invite, top(14));

        LinearLayout revenue = card();
        revenue.addView(text("추천 수익 현황", 17f, R.color.text_primary, true));

        LinearLayout countRow = new LinearLayout(this);
        countRow.setOrientation(LinearLayout.HORIZONTAL);
        referredCountView = stat("추천 가입", "0명");
        paidCountView = stat("유료 전환", "0명");
        countRow.addView(referredCountView, statParams(false));
        countRow.addView(paidCountView, statParams(true));
        revenue.addView(countRow, top(14));

        LinearLayout revenueRow = new LinearLayout(this);
        revenueRow.setOrientation(LinearLayout.HORIZONTAL);
        estimatedRevenueView = stat("이번 달 수익", "0원");
        confirmedRevenueView = stat("누적 확정", "0원");
        revenueRow.addView(estimatedRevenueView, statParams(false));
        revenueRow.addView(confirmedRevenueView, statParams(true));
        revenue.addView(revenueRow, top(8));

        partnerCenterButton = primaryButton("정산센터 열기");
        partnerCenterButton.setOnClickListener(v -> openPartnerCenter());
        revenue.addView(partnerCenterButton, fixedTop(50, 14));

        TextView payoutNotice = text(
                "정산정보 등록과 지급 요청은 보안 확인이 적용되는 정산센터에서 처리합니다.",
                12.5f, R.color.text_muted, false);
        payoutNotice.setLineSpacing(0f, 1.2f);
        revenue.addView(payoutNotice, top(10));
        root.addView(revenue, top(12));

        TextView signupOnly = text(
                "추천인 코드는 회원가입할 때 1회만 입력할 수 있습니다. 자기추천·중복가입·환불 등 부정 또는 취소 결제는 수익 대상에서 제외될 수 있습니다.",
                13f, R.color.text_secondary, false);
        signupOnly.setBackgroundResource(R.drawable.bg_preview);
        signupOnly.setPadding(dp(14), dp(12), dp(14), dp(12));
        signupOnly.setLineSpacing(0f, 1.2f);
        root.addView(signupOnly, top(10));

        return scroll;
    }

    private void refreshIfNeeded() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        long age = System.currentTimeMillis() - value.codeCheckedAt;
        if (value.code.isEmpty() || value.codeCheckedAt <= 0L || age >= CODE_REFRESH_MS) {
            refresh(false);
            return;
        }
        refresh(false);
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
            boolean codeLoaded = false;
            boolean summaryLoaded = false;
            JSONObject loadedSummary = null;
            try {
                JSONObject me = AuthApiClient.referralMe(session);
                ReferralStateStore.saveMe(this, me);
                codeLoaded = true;
            } catch (Exception error) {
                CrashTelemetryStore.record(this, "referral_cash", "code_load_failed",
                        error.getClass().getSimpleName());
            }
            try {
                JSONObject response = AuthApiClient.referralSummary(session);
                loadedSummary = response.optJSONObject("summary");
                summaryLoaded = loadedSummary != null;
            } catch (Exception error) {
                CrashTelemetryStore.record(this, "referral_cash", "summary_load_failed",
                        error.getClass().getSimpleName());
            }

            final boolean finalCodeLoaded = codeLoaded;
            final boolean finalSummaryLoaded = summaryLoaded;
            final JSONObject finalSummary = loadedSummary;
            runOnUiThread(() -> {
                working = false;
                if (notify) setManualRefreshState(false);
                if (finalSummary != null) summary = finalSummary;
                render();
                if (notify) {
                    String message;
                    if (finalCodeLoaded && finalSummaryLoaded) {
                        message = "추천 수익 현황을 새로 확인했습니다.";
                    } else if (finalCodeLoaded) {
                        message = "추천코드는 확인했지만 수익 현황을 불러오지 못했습니다.";
                    } else {
                        message = "추천 정보를 확인하지 못했습니다.";
                    }
                    Toast.makeText(this, message,
                            finalCodeLoaded ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                }
            });
        }, "calltag-referral-cash-refresh").start();
    }

    private void render() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        codeView.setText(value.code.isEmpty() ? "불러오는 중…" : value.code);

        referredCountView.setText(statText(
                "추천 가입", summary.optInt("referredCount", 0) + "명"));
        paidCountView.setText(statText(
                "유료 전환", summary.optInt("activePaidCount", 0) + "명"));
        estimatedRevenueView.setText(statText(
                "이번 달 수익", money(summary.optLong("estimatedRevenueKrw", 0L))));
        confirmedRevenueView.setText(statText(
                "누적 확정", money(summary.optLong("confirmedRevenueKrw", 0L))));

        int bonusDays = Math.max(0, summary.optInt("friendBonusDays", 5));
        double rate = summary.optDouble("commissionRatePercent", 20d);
        String friendBenefit = summary.optString("friendBenefitMessage", "").trim();
        if (friendBenefit.isEmpty()) {
            friendBenefit = "친구 혜택 · 회원가입할 때 추천인 코드를 입력하면 무료체험 +"
                    + bonusDays + "일";
        }
        String myBenefit = summary.optString("benefitMessage", "").trim();
        if (myBenefit.isEmpty()) {
            myBenefit = "내 수익 · 추천 회원의 콜태그 유료 결제액의 "
                    + rateText(rate) + "%";
        }
        String recurring = summary.optString("recurringMessage", "").trim();
        if (recurring.isEmpty()) {
            recurring = "추천 회원이 유료 구독을 유지해 새 결제가 확인될 때마다 같은 비율로 적립됩니다.";
        }
        friendBenefitView.setText(friendBenefit);
        myBenefitView.setText(myBenefit);
        recurringView.setText(recurring);

        boolean available = summary.optBoolean("partnerCenterAvailable", true);
        partnerCenterButton.setEnabled(available);
        partnerCenterButton.setAlpha(available ? 1f : 0.55f);
    }

    private void copyCode() {
        String code = ReferralStateStore.snapshot(this).code;
        if (code.isEmpty()) {
            Toast.makeText(this, "추천인 코드를 불러오는 중입니다.", Toast.LENGTH_SHORT).show();
            refreshIfNeeded();
            return;
        }

        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("콜태그 추천인 코드", code));
            Toast.makeText(this, "추천인 코드를 복사했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCode() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        if (value.code.isEmpty()) {
            Toast.makeText(this, "추천인 코드를 불러오는 중입니다.", Toast.LENGTH_SHORT).show();
            refreshIfNeeded();
            return;
        }

        String shareIntro = summary.optString("shareMessage", "").trim();
        if (shareIntro.isEmpty()) {
            int bonusDays = Math.max(0, summary.optInt("friendBonusDays", 5));
            shareIntro = "콜태그 가입할 때 추천인 코드를 입력하면 무료체험이 "
                    + bonusDays + "일 추가돼요.";
        }
        StringBuilder message = new StringBuilder()
                .append(shareIntro).append("\n")
                .append("추천인 코드: ").append(value.code);
        if (!value.shareUrl.isEmpty()) message.append("\n").append(value.shareUrl);

        startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, message.toString()),
                "친구에게 공유"
        ));
    }

    private void openPartnerCenter() {
        String url = summary.optString("partnerCenterUrl", DEFAULT_PARTNER_CENTER).trim();
        if (url.isEmpty()) url = DEFAULT_PARTNER_CENTER;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException error) {
            Toast.makeText(this, "정산센터를 열지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void setManualRefreshState(boolean value) {
        refreshButton.setEnabled(!value);
        refreshButton.setAlpha(value ? 0.55f : 1f);
        refreshButton.setText(value ? "확인 중…" : "새로고침");
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(18), dp(18), dp(18), dp(18));
        view.setBackgroundResource(R.drawable.bg_card);
        return view;
    }

    private TextView stat(String label, String value) {
        TextView view = text(statText(label, value), 14f, R.color.text_primary, true);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setLineSpacing(0f, 1.18f);
        view.setBackgroundResource(R.drawable.bg_preview);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        return view;
    }

    private String statText(String label, String value) {
        return label + "\n" + value;
    }

    private LinearLayout.LayoutParams statParams(boolean withLeftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(68), 1f);
        if (withLeftMargin) params.leftMargin = dp(8);
        return params;
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

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private String money(long amount) {
        return NumberFormat.getNumberInstance(Locale.KOREA)
                .format(amount) + "원";
    }

    private String rateText(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001d) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.KOREA, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
