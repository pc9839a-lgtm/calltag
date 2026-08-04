package kr.pagero.calltag;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.Uri;
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

/** 페이지로 문의 연결 상태를 일반 사용자가 쉽게 확인하는 화면. */
public final class PageroConnectionActivity extends Activity {
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
            int imported = Math.max(0,
                    intent.getIntExtra(PageroLeadSyncManager.EXTRA_IMPORTED, 0));
            int updated = Math.max(0,
                    intent.getIntExtra(PageroLeadSyncManager.EXTRA_UPDATED, 0));
            int rejected = Math.max(0,
                    intent.getIntExtra(PageroLeadSyncManager.EXTRA_REJECTED, 0));

            String message;
            if (!success) {
                message = "문의를 확인하지 못했어요. 잠시 후 다시 시도해주세요.";
            } else if (imported + updated == 0 && rejected == 0) {
                message = "새로 들어온 문의가 없어요.";
            } else if (rejected > 0) {
                message = "문의 확인을 마쳤어요. 확인이 필요한 문의가 " + rejected + "건 있어요.";
            } else {
                message = "새 문의 " + (imported + updated) + "건을 콜태그에 반영했어요.";
            }
            Toast.makeText(PageroConnectionActivity.this, message, Toast.LENGTH_SHORT).show();
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
        header.setPadding(dp(12), 0, dp(16), 0);
        header.setBackgroundColor(getColor(R.color.surface));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(34f);
        back.setTextColor(getColor(R.color.text_primary));
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("뒤로가기");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(56)));

        TextView title = text("페이지로 문의 연결", 20f, true, R.color.text_primary);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));
        screen.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        content.addView(text("문의가 들어오면\n콜태그에 바로 등록됩니다.",
                23f, true, R.color.text_primary), wrap());
        content.addView(text(
                "고객 이름·연락처·문의 내용을 자동으로 정리하고 바로 알려드려요.",
                14f, false, R.color.text_secondary), top(9));

        LinearLayout accountCard = card();
        accountCard.addView(label("연결 계정"), wrap());
        accountValue = text("", 17f, true, R.color.text_primary);
        accountCard.addView(accountValue, top(8));
        content.addView(accountCard, top(20));

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
        alertCard.addView(lastCheck, top(12));
        content.addView(alertCard, top(10));

        checkButton = button("새 문의 확인", true);
        checkButton.setOnClickListener(v -> checkNewLeads());
        content.addView(checkButton, fixedTop(50, 14));

        Button openPagero = button("페이지로 관리화면 열기", false);
        openPagero.setOnClickListener(v -> openPagero());
        content.addView(openPagero, fixedTop(48, 8));

        content.addView(text("이렇게 사용하세요", 16f, true, R.color.text_primary), top(26));

        LinearLayout guide = card();
        guide.addView(step("1", "페이지로에서 랜딩페이지를 만들고 공개합니다."), wrap());
        guide.addView(divider(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        guide.addView(step("2", "고객이 랜딩페이지에서 문의를 남깁니다."), wrap());
        guide.addView(divider(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        guide.addView(step("3", "콜태그가 고객을 등록하고 새 문의를 알려드립니다."), wrap());
        content.addView(guide, top(10));

        content.addView(text(
                "페이지로를 아직 사용하지 않아도 콜태그의 전화·문자 기능은 그대로 사용할 수 있어요.",
                13f, false, R.color.text_muted), top(14));

        Button done = button("완료", false);
        done.setOnClickListener(v -> finish());
        content.addView(done, fixedTop(48, 18));

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
            renderState();
            return;
        }
        if (!PageroLeadSyncManager.requestSync(this, true)) {
            Toast.makeText(this, "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }
        renderState();
    }

    private void openPagero() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://pagero.kr/app")));
        } catch (Exception error) {
            Toast.makeText(this, "페이지로 관리화면을 열지 못했어요.", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderState() {
        if (accountValue == null) return;

        boolean hasSession = AuthSessionStore.hasSession(this);
        String email = AuthSessionStore.email(this).trim().toLowerCase();
        accountValue.setText(email.isEmpty() ? "로그인 후 확인할 수 있어요" : email);

        PageroAccountStatusStore.Snapshot account = PageroAccountStatusStore.read(this);
        if (!hasSession) {
            connectionTitle.setText("로그인이 필요해요");
            connectionBody.setText("콜태그에 로그인하면 페이지로 연결을 자동으로 확인합니다.");
        } else if (email.isEmpty()) {
            connectionTitle.setText("계정을 다시 확인해주세요");
            connectionBody.setText("로그아웃한 뒤 페이지로와 같은 계정으로 다시 로그인해주세요.");
        } else if (account.connected()) {
            connectionTitle.setText("페이지로 연결 완료");
            connectionBody.setText(account.projectCount > 0
                    ? "공개한 페이지 " + account.projectCount + "개에서 들어오는 문의를 받고 있어요."
                    : "페이지로에서 들어오는 문의를 콜태그에 자동으로 등록해요.");
        } else if (PageroAccountStatusStore.NOT_CONNECTED.equals(account.status)) {
            connectionTitle.setText("아직 연결된 페이지가 없어요");
            connectionBody.setText("페이지로에서 같은 이메일로 로그인해 랜딩페이지를 만들면 자동으로 연결됩니다.");
        } else {
            connectionTitle.setText("연결 상태를 확인하고 있어요");
            connectionBody.setText("잠시 후 자동으로 다시 확인합니다.");
        }

        CallTagPushStatusStore.Snapshot realtime = CallTagPushStatusStore.read(this);
        if (realtime.realtime) {
            alertTitle.setText("새 문의 알림 켜짐");
            alertBody.setText("앱을 닫거나 화면을 잠가도 새 문의가 들어오면 바로 알려드려요.");
        } else if (hasSession) {
            alertTitle.setText("새 문의 알림 준비 중");
            alertBody.setText("앱을 열면 새 문의를 자동으로 확인합니다. 잠시 후 다시 확인해주세요.");
        } else {
            alertTitle.setText("로그인 후 알림을 받을 수 있어요");
            alertBody.setText("콜태그에 로그인하면 새 문의 알림이 자동으로 준비됩니다.");
        }

        PageroConnectionStatusStore.Snapshot status = PageroConnectionStatusStore.read(this);
        boolean running = PageroLeadSyncManager.isRunning() || status.running;
        checkButton.setEnabled(!running && hasSession);
        checkButton.setText(running ? "문의 확인 중…" : "새 문의 확인");

        if (running) {
            lastCheck.setText("페이지로에서 새 문의를 찾고 있어요.");
            return;
        }
        if (!status.error.isEmpty()) {
            lastCheck.setText("마지막 확인을 마치지 못했어요. 잠시 후 다시 시도해주세요.");
            return;
        }
        if (status.lastSuccessAt <= 0L) {
            lastCheck.setText("연결되면 새 문의가 자동으로 여기에 표시됩니다.");
            return;
        }

        String when = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT).format(new Date(status.lastSuccessAt));
        StringBuilder summary = new StringBuilder(when).append(" 확인");
        if (status.imported > 0) summary.append(" · 새 고객 ").append(status.imported).append("명");
        if (status.updated > 0) summary.append(" · 기존 고객 문의 ").append(status.updated).append("건");
        if (status.rejected > 0) summary.append(" · 확인 필요 ").append(status.rejected).append("건");
        if (status.imported == 0 && status.updated == 0 && status.rejected == 0) {
            summary.append(" · 새 문의 없음");
        }
        lastCheck.setText(summary.toString());
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private View step(String number, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        TextView badge = text(number, 13f, true, R.color.primary);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.bg_soft_panel);
        row.addView(badge, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView copy = text(value, 14f, true, R.color.text_primary);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        copyParams.leftMargin = dp(12);
        row.addView(copy, copyParams);
        return row;
    }

    private View divider() {
        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.border));
        return divider;
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
        view.setLineSpacing(0f, 1.15f);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
