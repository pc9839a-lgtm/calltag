package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

/** 페이지로 문의 계정 연결과 동기화 상태만 보여주는 전용 화면. */
public final class PageroSyncActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView account;
    private TextView connection;
    private TextView realtime;
    private TextView lastSync;
    private Button syncButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        PageroAccountConnectionManager.refresh(this, false);
        CallTagPushManager.registerIfAvailable(this);
        CallTagPushManager.refreshStatus(this);
        render();
        handler.postDelayed(this::render, 1200L);
        handler.postDelayed(this::render, 3200L);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        super.onPause();
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
        header.addView(title("페이지로 문의 동기화", 22f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header, matchWrap());

        account = stateCard(root, "연결 계정");
        connection = stateCard(root, "페이지로 연결");
        realtime = stateCard(root, "새 문의 알림");
        lastSync = stateCard(root, "마지막 문의 확인");

        syncButton = button("새 문의 지금 확인", true);
        syncButton.setOnClickListener(v -> syncNow());
        root.addView(syncButton, fixedTop(52, 16));

        Button guide = button("페이지로 사용 방법", false);
        guide.setOnClickListener(v -> startActivity(new Intent(this, PageroUseGuideActivity.class)));
        root.addView(guide, fixedTop(50, 8));
        return scroll;
    }

    private TextView stateCard(LinearLayout root, String label) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackgroundResource(R.drawable.bg_card);
        TextView labelView = body(label);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(labelView, matchWrap());
        TextView value = title("확인 중", 16f);
        card.addView(value, top(7));
        root.addView(card, top(10));
        return value;
    }

    private void syncNow() {
        if (!AuthSessionStore.hasSession(this)) {
            Toast.makeText(this, "콜태그에 먼저 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!PageroLeadSyncManager.requestSync(this, true)) {
            Toast.makeText(this, "이미 새 문의를 확인하고 있습니다.", Toast.LENGTH_SHORT).show();
        }
        render();
        handler.postDelayed(() -> {
            render();
            Toast.makeText(this, "페이지로 문의 확인을 마쳤습니다.", Toast.LENGTH_SHORT).show();
        }, 2200L);
    }

    private void render() {
        if (account == null) return;
        boolean hasSession = AuthSessionStore.hasSession(this);
        String email = AuthSessionStore.email(this).trim().toLowerCase();
        account.setText(email.isEmpty() ? "로그인 후 확인 가능" : email);

        PageroAccountStatusStore.Snapshot accountState = PageroAccountStatusStore.read(this);
        if (!hasSession) {
            connection.setText("로그인이 필요합니다.");
        } else if (accountState.connected()) {
            connection.setText(accountState.projectCount > 0
                    ? "연결 완료 · 공개 페이지 " + accountState.projectCount + "개"
                    : "연결 완료");
            connection.setTextColor(getColor(R.color.primary));
        } else if (PageroAccountStatusStore.NOT_CONNECTED.equals(accountState.status)) {
            connection.setText("같은 계정의 공개 페이지가 없습니다.");
            connection.setTextColor(getColor(R.color.text_secondary));
        } else {
            connection.setText("연결 상태 확인 중");
        }

        CallTagPushStatusStore.Snapshot push = CallTagPushStatusStore.read(this);
        realtime.setText(push.realtime
                ? "실시간 알림 사용 중"
                : hasSession ? "앱 실행 중 자동 확인" : "로그인 후 사용 가능");
        realtime.setTextColor(push.realtime
                ? getColor(R.color.primary) : getColor(R.color.text_secondary));

        PageroConnectionStatusStore.Snapshot status = PageroConnectionStatusStore.read(this);
        boolean running = PageroLeadSyncManager.isRunning() || status.running;
        syncButton.setEnabled(!running && hasSession);
        syncButton.setText(running ? "새 문의 확인 중…" : "새 문의 지금 확인");
        if (status.lastSuccessAt <= 0L) {
            lastSync.setText(status.error.isEmpty() ? "아직 확인 기록이 없습니다." : "마지막 확인 실패");
            return;
        }
        String when = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT).format(new Date(status.lastSuccessAt));
        int changed = status.imported + status.updated;
        lastSync.setText(when + " · 반영 " + changed + "건"
                + (status.rejected > 0 ? " · 확인 필요 " + status.rejected + "건" : ""));
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

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
