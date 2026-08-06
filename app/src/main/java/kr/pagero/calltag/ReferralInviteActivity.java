package kr.pagero.calltag;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/** 내 추천인 코드 확인·복사·친구 초대만 담당한다. */
public final class ReferralInviteActivity extends Activity {
    private TextView code;
    private TextView referred;
    private TextView paid;
    private Button refresh;
    private boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        render();
        refresh(false);
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = title("‹", 30f);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView screenTitle = title("내 추천인 코드", 22f);
        header.addView(screenTitle, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        refresh = button("새로고침", false);
        refresh.setOnClickListener(v -> refresh(true));
        header.addView(refresh, new LinearLayout.LayoutParams(dp(88), dp(42)));
        root.addView(header, matchWrap());

        LinearLayout codeCard = card();
        codeCard.addView(body("내 추천인 코드"), matchWrap());
        code = title("확인 중", 30f);
        code.setLetterSpacing(0.08f);
        codeCard.addView(code, top(10));
        LinearLayout actions = new LinearLayout(this);
        Button copy = button("코드 복사", false);
        copy.setOnClickListener(v -> copyCode());
        actions.addView(copy, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button share = button("친구에게 공유", true);
        share.setOnClickListener(v -> shareCode());
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        shareParams.leftMargin = dp(8);
        actions.addView(share, shareParams);
        codeCard.addView(actions, top(16));
        codeCard.addView(body("친구가 가입할 때 이 코드를 등록하면 무료 이용기간이 5일 늘어납니다."), top(12));
        root.addView(codeCard, top(18));

        LinearLayout summary = card();
        summary.addView(metricRow("추천 회원", true), matchWrap());
        summary.addView(metricRow("유료 이용 중", false), top(14));
        root.addView(summary, top(12));
        return scroll;
    }

    private LinearLayout metricRow(String label, boolean first) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(body(label), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = title("0명", 18f);
        row.addView(value);
        if (first) referred = value;
        else paid = value;
        return row;
    }

    private void refresh(boolean notify) {
        if (working) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        working = true;
        refresh.setEnabled(false);
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
                working = false;
                refresh.setEnabled(true);
                render();
                if (notify) Toast.makeText(this,
                        loaded ? "추천 현황을 갱신했습니다." : "추천 현황을 확인하지 못했습니다.",
                        loaded ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            });
        }, "calltag-referral-invite-refresh").start();
    }

    private void render() {
        if (code == null) return;
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        code.setText(value.code.isEmpty() ? "확인 필요" : value.code);
        referred.setText(value.referredCount + "명");
        paid.setText(value.activePaidCount + "명");
    }

    private void copyCode() {
        String value = ReferralStateStore.snapshot(this).code;
        if (value.isEmpty()) {
            Toast.makeText(this, "추천인 코드를 먼저 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("콜태그 추천인 코드", value));
            Toast.makeText(this, "추천인 코드를 복사했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCode() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        if (value.code.isEmpty()) {
            Toast.makeText(this, "추천인 코드를 먼저 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = "콜태그 또는 페이지로 가입 시 추천인 코드를 등록하면 무료 이용기간이 5일 늘어납니다.\n"
                + "추천인 코드: " + value.code
                + (value.shareUrl.isEmpty() ? "" : "\n" + value.shareUrl);
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text), "친구에게 공유"));
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView title(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView body(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(14f);
        view.setTextColor(getColor(R.color.text_secondary));
        view.setIncludeFontPadding(false);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
