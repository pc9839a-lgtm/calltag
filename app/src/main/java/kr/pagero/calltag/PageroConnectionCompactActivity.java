package kr.pagero.calltag;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

/** PageRo connection status only. Product/service guidance lives in PageroUseGuideActivity. */
public final class PageroConnectionCompactActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable delayedRender = this::renderState;

    private TextView accountValue;
    private TextView connectionTitle;
    private TextView connectionBody;
    private TextView alertTitle;
    private TextView alertBody;
    private TextView lastCheck;
    private Button checkButton;
    private boolean receiverRegistered;

    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!PageroLeadSyncManager.ACTION_LEADS_UPDATED.equals(intent.getAction())) return;
            renderState();
            boolean success = intent.getBooleanExtra(PageroLeadSyncManager.EXTRA_SUCCESS, false);
            int imported = Math.max(0, intent.getIntExtra(PageroLeadSyncManager.EXTRA_IMPORTED, 0));
            int updated = Math.max(0, intent.getIntExtra(PageroLeadSyncManager.EXTRA_UPDATED, 0));
            int rejected = Math.max(0, intent.getIntExtra(PageroLeadSyncManager.EXTRA_REJECTED, 0));
            String message;
            if (!success) message = "문의를 확인하지 못했어요. 잠시 후 다시 시도해주세요.";
            else if (imported + updated == 0 && rejected == 0) message = "새로 들어온 문의가 없어요.";
            else if (rejected > 0) message = "확인이 필요한 문의가 " + rejected + "건 있어요.";
            else message = "새 문의 " + (imported + updated) + "건을 콜태그에 반영했어요.";
            Toast.makeText(PageroConnectionCompactActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        renderState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(PageroLeadSyncManager.ACTION_LEADS_UPDATED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(syncReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(syncReceiver, filter);
        }
        receiverRegistered = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        PageroAccountConnectionManager.refresh(this, false);
        CallTagPushManager.registerIfAvailable(this);
        CallTagPushManager.refreshStatus(this);
        renderState();
        handler.removeCallbacks(delayedRender);
        handler.postDelayed(delayedRender, 1200L);
        handler.postDelayed(delayedRender, 3200L);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(delayedRender);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(syncReceiver);
            receiverRegistered = false;
        }
        super.onStop();
    }

    private View buildContent() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(getColor(R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10), 0, dp(14), 0);
        header.setBackgroundColor(getColor(R.color.surface));

        TextView back = text("‹", 34f, true, R.color.text_primary);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("뒤로가기");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(52)));

        TextView title = text("페이지로 문의 연결", 20f, true, R.color.text_primary);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1f));
        screen.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(22));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout accountCard = card();
        accountCard.addView(label("연결 계정"), wrap());
        accountValue = text("", 17f, true, R.color.text_primary);
        accountCard.addView(accountValue, top(7));
        content.addView(accountCard, wrap());

        LinearLayout stateCard = card();
        connectionTitle = text("", 17f, true, R.color.text_primary);
        stateCard.addView(connectionTitle, wrap());
        connectionBody = text("", 14f, false, R.color.text_secondary);
        stateCard.addView(connectionBody, top(7));
        content.addView(stateCard, top(10));

        LinearLayout alertCard = card();
        alertTitle = text("", 17f, true, R.color.text_primary);
        alertCard.addView(alertTitle, wrap());
        alertBody = text("", 14f, false, R.color.text_secondary);
        alertCard.addView(alertBody, top(7));
        lastCheck = text("", 13f, false, R.color.text_muted);
        alertCard.addView(lastCheck, top(10));
        content.addView(alertCard, top(10));

        checkButton = button("새 문의 확인", true);
        checkButton.setOnClickListener(v -> checkNewLeads());
        content.addView(checkButton, fixedTop(50, 14));

        Button done = button("완료", false);
        done.setOnClickListener(v -> finish());
        content.addView(done, fixedTop(48, 10));

        screen.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return screen;
    }

    private void checkNewLeads() {
        if (!AuthSessionStore.hasSession(this)) {
            Toast.makeText(this, "콜태그에 먼저 로그인해주세요.", Toast.LENGTH_SHORT).show();
            renderState();
            return;
        }
        if (PageroLeadSyncManager.isRunning()) {
            Toast.makeText(this, "지금 새 문의를 확인하고 있어요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!PageroLeadSyncManager.requestSync(this, true)) {
            Toast.makeText(this, "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }
        renderState();
    }

    private void renderState() {
        if (accountValue == null) return;
        boolean hasSession = AuthSessionStore.hasSession(this);
        String email = AuthSessionStore.email(this).trim().toLowerCase();
        accountValue.setText(email.isEmpty() ? "로그인 후 확인할 수 있어요" : email);

        PageroAccountStatusStore.Snapshot account = PageroAccountStatusStore.read(this);
        if (!hasSession) {
            connectionTitle.setText("로그인이 필요해요");
            connectionBody.setText("콜태그에 로그인하면 연결 상태를 확인합니다.");
        } else if (account.connected()) {
            connectionTitle.setText("페이지로 연결 완료");
            connectionBody.setText(account.projectCount > 0
                    ? "연결된 공개 페이지 " + account.projectCount + "개"
                    : "페이지로 문의를 콜태그로 받고 있어요.");
        } else if (PageroAccountStatusStore.NOT_CONNECTED.equals(account.status)) {
            connectionTitle.setText("연결된 페이지 없음");
            connectionBody.setText("같은 계정의 페이지로 서비스가 확인되지 않았습니다.");
        } else {
            connectionTitle.setText("연결 상태 확인 중");
            connectionBody.setText("잠시 후 자동으로 다시 확인합니다.");
        }

        CallTagPushStatusStore.Snapshot realtime = CallTagPushStatusStore.read(this);
        if (realtime.realtime) {
            alertTitle.setText("새 문의 알림 켜짐");
            alertBody.setText("앱이 닫혀 있어도 새 문의 알림을 받을 수 있습니다.");
        } else if (hasSession) {
            alertTitle.setText("새 문의 알림 준비 중");
            alertBody.setText("연결 상태를 다시 확인하고 있습니다.");
        } else {
            alertTitle.setText("알림 대기");
            alertBody.setText("로그인 후 새 문의 알림을 사용할 수 있습니다.");
        }

        PageroConnectionStatusStore.Snapshot status = PageroConnectionStatusStore.read(this);
        boolean running = PageroLeadSyncManager.isRunning() || status.running;
        checkButton.setEnabled(!running && hasSession);
        checkButton.setText(running ? "문의 확인 중…" : "새 문의 확인");

        if (running) {
            lastCheck.setText("새 문의를 확인하고 있어요.");
        } else if (!status.error.isEmpty()) {
            lastCheck.setText("마지막 확인 실패 · 다시 시도해주세요.");
        } else if (status.lastSuccessAt <= 0L) {
            lastCheck.setText("아직 확인 기록이 없습니다.");
        } else {
            String when = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT).format(new Date(status.lastSuccessAt));
            StringBuilder summary = new StringBuilder(when).append(" 확인");
            if (status.imported > 0) summary.append(" · 새 고객 ").append(status.imported).append("명");
            if (status.updated > 0) summary.append(" · 문의 ").append(status.updated).append("건");
            if (status.rejected > 0) summary.append(" · 확인 필요 ").append(status.rejected).append("건");
            if (status.imported == 0 && status.updated == 0 && status.rejected == 0) summary.append(" · 새 문의 없음");
            lastCheck.setText(summary.toString());
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView label(String value) {
        return text(value, 13f, true, R.color.text_secondary);
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(color));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int value) {
        LinearLayout.LayoutParams params = wrap();
        params.topMargin = dp(value);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(top);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
