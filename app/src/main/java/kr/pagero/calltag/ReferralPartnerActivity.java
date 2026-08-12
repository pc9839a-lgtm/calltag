package kr.pagero.calltag;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/** 더보기 > 파트너 코드. 파트너 현황과 완전히 분리한다. */
public final class ReferralPartnerActivity extends Activity {
    private static final long CODE_REFRESH_MS = 24L * 60L * 60L * 1000L;

    private TextView codeView;
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
        TextView title = text("파트너 코드", 21f, R.color.text_primary, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        refreshButton = secondaryButton("새로고침");
        refreshButton.setOnClickListener(v -> refresh(true));
        header.addView(refreshButton, new LinearLayout.LayoutParams(dp(88), dp(40)));
        root.addView(header);

        LinearLayout invite = card();
        invite.addView(text("내 파트너 코드", 14f, R.color.text_secondary, true));
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

        TextView benefit = text(
                "친구가 회원가입할 때 이 코드를 입력하면 무료 이용 기간이 7일 추가됩니다.",
                13f, R.color.text_secondary, false);
        benefit.setLineSpacing(0f, 1.2f);
        invite.addView(benefit, top(12));
        root.addView(invite, top(14));

        TextView signupOnly = text(
                "코드는 회원가입할 때 1회 입력할 수 있습니다.",
                13f, R.color.text_secondary, false);
        signupOnly.setBackgroundResource(R.drawable.bg_preview);
        signupOnly.setPadding(dp(14), dp(12), dp(14), dp(12));
        root.addView(signupOnly, top(10));
        return scroll;
    }

    private void refreshIfNeeded() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        long age = System.currentTimeMillis() - value.codeCheckedAt;
        if (value.code.isEmpty() || value.codeCheckedAt <= 0L || age >= CODE_REFRESH_MS) {
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
                JSONObject me = AuthApiClient.referralMe(session);
                ReferralStateStore.saveMe(this, me);
                success = true;
            } catch (Exception ignored) {}
            boolean loaded = success;
            runOnUiThread(() -> {
                working = false;
                if (notify) setManualRefreshState(false);
                render();
                if (notify) {
                    Toast.makeText(this,
                            loaded ? "파트너 코드를 새로 확인했습니다."
                                    : "파트너 코드를 확인하지 못했습니다.",
                            loaded ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                }
            });
        }, "calltag-partner-code-refresh").start();
    }

    private void render() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        codeView.setText(value.code.isEmpty() ? "불러오는 중…" : value.code);
    }

    private void copyCode() {
        String code = ReferralStateStore.snapshot(this).code;
        if (code.isEmpty()) {
            Toast.makeText(this, "파트너 코드를 불러오는 중입니다.", Toast.LENGTH_SHORT).show();
            refreshIfNeeded();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("콜태그 파트너 코드", code));
            Toast.makeText(this, "파트너 코드를 복사했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCode() {
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        if (value.code.isEmpty()) {
            Toast.makeText(this, "파트너 코드를 불러오는 중입니다.", Toast.LENGTH_SHORT).show();
            refreshIfNeeded();
            return;
        }
        StringBuilder message = new StringBuilder()
                .append("콜태그 가입할 때 아래 파트너 코드를 입력하면 무료 이용 기간이 7일 추가돼요.\n")
                .append("파트너 코드: ").append(value.code);
        if (!value.shareUrl.isEmpty()) message.append("\n").append(value.shareUrl);
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, message.toString()), "친구에게 공유"));
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
