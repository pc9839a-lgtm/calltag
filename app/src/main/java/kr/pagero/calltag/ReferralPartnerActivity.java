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

/** 더보기 > 친구 초대·파트너. 추천인 입력은 회원가입 화면에서만 허용한다. */
public final class ReferralPartnerActivity extends Activity {
    private TextView codeView;
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
        refresh(false);
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
        TextView title = text("친구 초대·파트너", 21f, R.color.text_primary, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        refreshButton = secondaryButton("새로고침");
        refreshButton.setOnClickListener(v -> refresh(true));
        header.addView(refreshButton, new LinearLayout.LayoutParams(dp(88), dp(40)));
        root.addView(header);

        LinearLayout invite = card();
        invite.addView(text("내 추천인 코드", 14f, R.color.text_secondary, true));
        codeView = text("확인 중", 28f, R.color.text_primary, true);
        codeView.setLetterSpacing(0.08f);
        invite.addView(codeView, top(9));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView copy = secondaryButton("복사");
        copy.setOnClickListener(v -> copyCode());
        actions.addView(copy, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView share = primaryButton("공유");
        share.setOnClickListener(v -> shareCode());
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        shareParams.leftMargin = dp(8);
        actions.addView(share, shareParams);
        invite.addView(actions, top(14));

        TextView benefit = text(
                "친구가 회원가입할 때 추천인 코드를 입력하면 통합권 무료체험이 7일 추가되어 총 14일 적용됩니다.",
                13f, R.color.text_secondary, false);
        benefit.setLineSpacing(0f, 1.2f);
        invite.addView(benefit, top(12));
        root.addView(invite, top(14));

        TextView signupOnly = text(
                "추천인 코드는 회원가입 시 1회만 입력할 수 있습니다.",
                13f, R.color.text_secondary, false);
        signupOnly.setBackgroundResource(R.drawable.bg_preview);
        signupOnly.setPadding(dp(14), dp(12), dp(14), dp(12));
        root.addView(signupOnly, top(10));

        root.addView(text("파트너 현황", 17f, R.color.text_primary, true), top(24));
        LinearLayout summary = card();
        referredCount = metric(summary, "추천 회원", true);
        activePaidCount = metric(summary, "유료 이용 중", false);
        estimatedRevenue = metric(summary, "이번 달 예상 수익", false);
        confirmedRevenue = metric(summary, "누적 확정 수익", false);
        root.addView(summary, top(10));

        TextView settlement = primaryButton("정산 페이지 열기");
        settlement.setOnClickListener(v -> openSettlement());
        root.addView(settlement, fixedTop(52, 12));
        return scroll;
    }

    private void refresh(boolean notify) {
        if (working) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            Toast.makeText(this, "로그인 정보를 다시 확인해주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        setWorking(true);
        new Thread(() -> {
            boolean success = false;
            try {
                JSONObject me = AuthApiClient.referralMe(session);
                ReferralStateStore.saveMe(this, me);
                success = true;
            } catch (Exception ignored) {}
            try {
                JSONObject summary = AuthApiClient.referralSummary(session);
                ReferralStateStore.saveSummary(this, summary);
                success = true;
            } catch (Exception ignored) {}
            boolean loaded = success;
            runOnUiThread(() -> {
                setWorking(false);
                render();
                if (notify) {
                    Toast.makeText(this,
                            loaded ? "최신 추천 현황을 확인했습니다."
                                    : "추천 현황을 확인하지 못했습니다.",
                            loaded ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                }
            });
        }, "calltag-referral-refresh").start();
    }

    private void render() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        codeView.setText(value.code.isEmpty() ? "확인 필요" : value.code);
        referredCount.setText(number(value.referredCount) + "명");
        activePaidCount.setText(number(value.activePaidCount) + "명");
        estimatedRevenue.setText(currency(value.estimatedRevenueKrw));
        confirmedRevenue.setText(currency(value.confirmedRevenueKrw));
    }

    private void copyCode() {
        String code = ReferralStateStore.snapshot(this).code;
        if (code.isEmpty()) {
            Toast.makeText(this, "추천인 코드를 먼저 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("콜태그 추천인 코드", code));
            Toast.makeText(this, "추천인 코드를 복사했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCode() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        if (value.code.isEmpty()) {
            Toast.makeText(this, "추천인 코드를 먼저 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder message = new StringBuilder()
                .append("콜태그 가입할 때 추천인 코드를 입력하면 통합권을 총 14일 무료로 이용할 수 있어요.\n")
                .append("추천인 코드: ").append(value.code);
        if (!value.shareUrl.isEmpty()) message.append("\n").append(value.shareUrl);
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, message.toString()), "친구에게 공유"));
    }

    private void openSettlement() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(PartnerSettlementActivity.SETTLEMENT_URL)));
        } catch (RuntimeException error) {
            Toast.makeText(this, "정산 페이지를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setWorking(boolean value) {
        working = value;
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
