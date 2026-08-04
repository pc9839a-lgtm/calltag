package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

/** 더보기 > 친구 초대·파트너. */
public final class ReferralPartnerActivity extends Activity {
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int TEXT = Color.rgb(15, 23, 42);
    private static final int SUBTEXT = Color.rgb(71, 85, 105);
    private static final int MUTED = Color.rgb(148, 163, 184);
    private static final int SURFACE = Color.WHITE;
    private static final int BACKGROUND = Color.rgb(248, 250, 252);
    private static final int BORDER = Color.rgb(226, 232, 240);

    private TextView codeView;
    private TextView benefitView;
    private EditText referralInput;
    private TextView applyButton;
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

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = text("친구 초대·파트너", 22f, TEXT, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1f));
        refreshButton = button("새로고침", false);
        refreshButton.setOnClickListener(v -> refresh(true));
        header.addView(refreshButton, new LinearLayout.LayoutParams(dp(86), dp(40)));
        root.addView(header, full());

        LinearLayout inviteCard = card();
        inviteCard.addView(text("내 추천인 코드", 15f, SUBTEXT, true), full());
        codeView = text("확인 중", 30f, TEXT, true);
        codeView.setLetterSpacing(0.08f);
        inviteCard.addView(codeView, top(10));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView copy = button("복사", false);
        copy.setOnClickListener(v -> copyCode());
        actions.addView(copy, weighted(1f));
        TextView share = button("공유", true);
        share.setOnClickListener(v -> shareCode());
        LinearLayout.LayoutParams shareParams = weighted(1f);
        shareParams.leftMargin = dp(10);
        actions.addView(share, shareParams);
        inviteCard.addView(actions, fixedTop(48, 16));
        inviteCard.addView(text(
                "친구가 가입할 때 코드를 등록하면 무료 이용기간이 5일 더 늘어납니다.",
                13f,
                MUTED,
                false), top(12));
        root.addView(inviteCard, top(22));

        LinearLayout applyCard = card();
        applyCard.addView(text("추천인 코드가 있나요?", 18f, TEXT, true), full());
        benefitView = text("등록하면 무료 이용기간이 5일 더 늘어납니다.", 14f, SUBTEXT, false);
        applyCard.addView(benefitView, top(7));
        referralInput = new EditText(this);
        referralInput.setSingleLine(true);
        referralInput.setHint("추천인 코드 입력");
        referralInput.setTextSize(16f);
        referralInput.setTextColor(TEXT);
        referralInput.setHintTextColor(MUTED);
        referralInput.setAllCaps(true);
        referralInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        referralInput.setPadding(dp(14), 0, dp(14), 0);
        referralInput.setBackground(round(SURFACE, BORDER, 14));
        applyCard.addView(referralInput, fixedTop(50, 16));
        applyButton = button("추천 혜택 받기", true);
        applyButton.setOnClickListener(v -> applyReferral());
        applyCard.addView(applyButton, fixedTop(50, 10));
        root.addView(applyCard, top(14));

        root.addView(sectionTitle("파트너 현황"), top(28));
        LinearLayout summary = card();
        referredCount = metric(summary, "추천 회원", true);
        activePaidCount = metric(summary, "유료 이용 중", false);
        estimatedRevenue = metric(summary, "이번 달 예상 수익", false);
        confirmedRevenue = metric(summary, "누적 확정 수익", false);
        summary.addView(text(
                "직접 추천한 회원의 확정 결제금액을 기준으로 서버에서 계산합니다. 환불·할인·부가세는 제외됩니다.",
                12f,
                MUTED,
                false), top(14));
        root.addView(summary, top(10));
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
            } catch (Exception ignored) {
                // Summary can still load even if the code endpoint is temporarily unavailable.
            }
            try {
                JSONObject summary = AuthApiClient.referralSummary(session);
                ReferralStateStore.saveSummary(this, summary);
                success = true;
            } catch (Exception ignored) {
                // Keep the last cached partner summary.
            }
            boolean loaded = success;
            runOnUiThread(() -> {
                setWorking(false);
                render();
                if (notify) Toast.makeText(this,
                        loaded ? "최신 추천 현황을 확인했어요."
                                : "추천 현황을 확인하지 못했어요. 인터넷 연결을 확인해주세요.",
                        loaded ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            });
        }, "calltag-referral-refresh").start();
    }

    private void applyReferral() {
        if (working) return;
        String code = referralInput.getText() == null
                ? "" : referralInput.getText().toString().trim().toUpperCase(Locale.KOREA);
        if (!code.matches("[A-Z0-9]{4,20}")) {
            Toast.makeText(this, "추천인 코드를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        ReferralStateStore.Snapshot cached = ReferralStateStore.snapshot(this);
        if (!cached.code.isEmpty() && cached.code.equalsIgnoreCase(code)) {
            Toast.makeText(this, "본인 추천인 코드는 등록할 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            Toast.makeText(this, "로그인 정보를 다시 확인해주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        hideKeyboard();
        setWorking(true);
        new Thread(() -> {
            try {
                JSONObject response = AuthApiClient.applyReferral(session, code);
                ReferralStateStore.saveMe(this, response);
                JSONObject entitlement = response.optJSONObject("entitlement");
                if (entitlement != null) {
                    FeatureEntitlementStore.saveServerEntitlement(this, response);
                } else {
                    try {
                        FeatureEntitlementStore.saveServerEntitlement(
                                this,
                                AuthApiClient.billingEntitlements(session));
                    } catch (Exception ignored) {
                        // Referral success remains valid; entitlement refresh can retry later.
                    }
                }
                runOnUiThread(() -> {
                    setWorking(false);
                    render();
                    new AlertDialog.Builder(this)
                            .setTitle("추천 혜택이 적용되었습니다")
                            .setMessage("무료 이용기간이 5일 늘어났습니다.")
                            .setPositiveButton("확인", null)
                            .show();
                });
            } catch (AuthApiClient.ApiException error) {
                runOnUiThread(() -> {
                    setWorking(false);
                    showApplyError(error.code);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setWorking(false);
                    Toast.makeText(this,
                            "추천인 코드를 등록하지 못했어요. 잠시 후 다시 시도해주세요.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "calltag-referral-apply").start();
    }

    private void render() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        codeView.setText(value.code.isEmpty() ? "확인 필요" : value.code);
        if (value.applied) {
            benefitView.setText("추천인 등록 완료 · 추천 혜택 +" + Math.max(5, value.bonusDays) + "일 적용");
            referralInput.setVisibility(View.GONE);
            applyButton.setVisibility(View.GONE);
        } else {
            benefitView.setText("등록하면 무료 이용기간이 5일 더 늘어납니다.");
            referralInput.setVisibility(View.VISIBLE);
            applyButton.setVisibility(View.VISIBLE);
        }
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
                .append("콜태그 또는 페이지로 가입 시 추천인 코드를 등록하면 무료 이용기간이 5일 더 늘어나요.\n")
                .append("추천인 코드: ").append(value.code);
        if (!value.shareUrl.isEmpty()) message.append("\n").append(value.shareUrl);
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, message.toString());
        startActivity(Intent.createChooser(intent, "친구에게 공유"));
    }

    private void showApplyError(String code) {
        String message;
        if ("SELF_REFERRAL".equals(code)) {
            message = "본인 추천인 코드는 등록할 수 없습니다.";
        } else if ("REFERRAL_ALREADY_APPLIED".equals(code)) {
            message = "이미 추천인 등록을 완료했습니다.";
        } else if ("PAID_CONVERSION_COMPLETED".equals(code)) {
            message = "첫 유료 결제 이후에는 추천인 코드를 등록할 수 없습니다.";
        } else if ("REFERRAL_CODE_NOT_FOUND".equals(code)) {
            message = "존재하지 않는 추천인 코드입니다.";
        } else {
            message = "추천인 코드를 등록하지 못했어요. 입력한 코드를 확인해주세요.";
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setWorking(boolean value) {
        working = value;
        refreshButton.setEnabled(!value);
        refreshButton.setAlpha(value ? 0.55f : 1f);
        refreshButton.setText(value ? "확인 중…" : "새로고침");
        applyButton.setEnabled(!value);
        applyButton.setAlpha(value ? 0.55f : 1f);
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null && referralInput != null) {
            manager.hideSoftInputFromWindow(referralInput.getWindowToken(), 0);
        }
    }

    private TextView metric(LinearLayout parent, String label, boolean first) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView labelView = text(label, 14f, SUBTEXT, false);
        row.addView(labelView, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = text("0", 17f, TEXT, true);
        row.addView(value, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        parent.addView(row, first ? full() : top(15));
        return value;
    }

    private LinearLayout card() {
        LinearLayout value = new LinearLayout(this);
        value.setOrientation(LinearLayout.VERTICAL);
        value.setPadding(dp(18), dp(18), dp(18), dp(18));
        value.setBackground(round(SURFACE, BORDER, 18));
        return value;
    }

    private TextView sectionTitle(String value) {
        return text(value, 16f, TEXT, true);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.2f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView button(String value, boolean primary) {
        TextView view = text(value, 14f, primary ? Color.WHITE : TEXT, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(round(primary ? BLUE : SURFACE,
                primary ? BLUE : BORDER,
                14));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private GradientDrawable round(int fill, int stroke, int radiusDp) {
        GradientDrawable value = new GradientDrawable();
        value.setColor(fill);
        value.setCornerRadius(dp(radiusDp));
        value.setStroke(dp(1), stroke);
        return value;
    }

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams value = full();
        value.topMargin = dp(margin);
        return value;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams value = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(height));
        value.topMargin = dp(margin);
        return value;
    }

    private LinearLayout.LayoutParams weighted(float weight) {
        return new LinearLayout.LayoutParams(0, dp(48), weight);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(Math.max(0L, value));
    }

    private String currency(long value) {
        return number(value) + "원";
    }
}
