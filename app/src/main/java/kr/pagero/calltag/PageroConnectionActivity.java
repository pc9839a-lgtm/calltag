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
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

/** 일반 사용자가 PageRo 연결 계정과 문의 동기화 결과를 확인하는 화면. */
public final class PageroConnectionActivity extends Activity {
    private TextView accountValue;
    private TextView connectionTitle;
    private TextView connectionBody;
    private TextView syncResult;
    private Button syncButton;
    private boolean receiverRegistered;

    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!PageroLeadSyncManager.ACTION_LEADS_UPDATED.equals(intent.getAction())) return;
            renderState();
            boolean success = intent.getBooleanExtra(PageroLeadSyncManager.EXTRA_SUCCESS, false);
            String message = intent.getStringExtra(PageroLeadSyncManager.EXTRA_MESSAGE);
            if (message == null || message.trim().isEmpty()) {
                message = success ? "동기화를 완료했습니다." : "동기화하지 못했습니다.";
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
        renderState();
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
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(56)));

        TextView title = new TextView(this);
        title.setText("페이지로 연결");
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(getColor(R.color.text_primary));
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));
        screen.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(30));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView intro = text("페이지로 문의를 콜태그 고객으로 자동 등록합니다.", 22f, true,
                R.color.text_primary);
        content.addView(intro, wrap());

        TextView introSub = text("두 서비스에 같은 계정으로 로그인하면 별도 키 입력 없이 연결됩니다.",
                14f, false, R.color.text_secondary);
        content.addView(introSub, top(8));

        LinearLayout accountCard = card();
        accountCard.addView(label("현재 연결 계정"), wrap());
        accountValue = text("", 17f, true, R.color.text_primary);
        accountCard.addView(accountValue, top(8));
        content.addView(accountCard, top(20));

        LinearLayout stateCard = card();
        connectionTitle = text("", 17f, true, R.color.text_primary);
        stateCard.addView(connectionTitle, wrap());
        connectionBody = text("", 14f, false, R.color.text_secondary);
        stateCard.addView(connectionBody, top(7));
        syncResult = text("", 13f, false, R.color.text_muted);
        stateCard.addView(syncResult, top(12));
        content.addView(stateCard, top(10));

        syncButton = button("지금 동기화", true);
        syncButton.setOnClickListener(v -> startSync());
        content.addView(syncButton, fixedTop(50, 14));

        Button openPagero = button("페이지로 열기", false);
        openPagero.setOnClickListener(v -> openPagero());
        content.addView(openPagero, fixedTop(48, 8));

        TextView guideTitle = text("연결 방법", 16f, true, R.color.text_primary);
        content.addView(guideTitle, top(26));

        LinearLayout guide = card();
        guide.addView(step("1", "페이지로와 콜태그에 같은 이메일로 로그인"), wrap());
        guide.addView(divider(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        guide.addView(step("2", "페이지로에서 랜딩페이지를 만들고 공개"), wrap());
        guide.addView(divider(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        guide.addView(step("3", "문의 접수 후 콜태그에서 지금 동기화"), wrap());
        content.addView(guide, top(10));

        TextView note = text(
                "계정이 다르면 문의가 보이지 않습니다. 연결 계정을 바꾸려면 콜태그에서 로그아웃한 뒤 페이지로와 같은 계정으로 다시 로그인하세요.",
                13f, false, R.color.text_muted);
        content.addView(note, top(14));

        Button done = button("고객목록에서 확인", false);
        done.setOnClickListener(v -> finish());
        content.addView(done, fixedTop(48, 18));

        screen.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return screen;
    }

    private void startSync() {
        if (!AuthSessionStore.hasSession(this)) {
            Toast.makeText(this, "콜태그에 먼저 로그인하세요.", Toast.LENGTH_SHORT).show();
            renderState();
            return;
        }
        if (PageroLeadSyncManager.isRunning()) {
            Toast.makeText(this, "이미 동기화 중입니다.", Toast.LENGTH_SHORT).show();
            renderState();
            return;
        }
        boolean started = PageroLeadSyncManager.requestSync(this, true);
        if (!started) {
            Toast.makeText(this, "동기화를 시작하지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
        renderState();
    }

    private void openPagero() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://pagero.kr/app")));
        } catch (Exception error) {
            Toast.makeText(this, "페이지로를 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderState() {
        if (accountValue == null) return;
        boolean hasSession = AuthSessionStore.hasSession(this);
        String email = AuthSessionStore.email(this).trim().toLowerCase();
        accountValue.setText(email.isEmpty() ? "로그인 계정 확인 필요" : email);

        if (!hasSession) {
            connectionTitle.setText("로그인이 필요합니다");
            connectionBody.setText("콜태그에 로그인한 뒤 페이지로 연결을 확인하세요.");
        } else if (email.isEmpty()) {
            connectionTitle.setText("계정 이메일을 확인할 수 없습니다");
            connectionBody.setText("로그아웃 후 페이지로와 같은 이메일로 다시 로그인하세요.");
        } else {
            connectionTitle.setText("자동 연결 준비 완료");
            connectionBody.setText(email + " 계정의 페이지로 문의만 가져옵니다.");
        }

        PageroConnectionStatusStore.Snapshot status = PageroConnectionStatusStore.read(this);
        boolean running = PageroLeadSyncManager.isRunning() || status.running;
        syncButton.setEnabled(!running && hasSession);
        syncButton.setText(running ? "동기화 중..." : "지금 동기화");

        if (running) {
            syncResult.setText("페이지로의 새 문의를 확인하고 있습니다.");
            return;
        }
        if (!status.error.isEmpty()) {
            String code = status.errorCode.isEmpty() ? "" : " (" + status.errorCode + ")";
            syncResult.setText("마지막 동기화 실패: " + status.error + code);
            return;
        }
        if (status.lastSuccessAt <= 0L) {
            syncResult.setText("아직 동기화한 기록이 없습니다.");
            return;
        }
        String when = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT).format(new Date(status.lastSuccessAt));
        syncResult.setText(when + " · 신규 " + status.imported + "건 · 갱신 "
                + status.updated + "건 · 확인 필요 " + status.rejected + "건");
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
        button.setTextColor(getColor(R.color.text_primary));
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
