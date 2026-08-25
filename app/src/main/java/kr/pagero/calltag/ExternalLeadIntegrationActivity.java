package kr.pagero.calltag;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

import java.util.List;

/** Visible in-app hub for the Universal Lead receiver and external channel setup. */
public final class ExternalLeadIntegrationActivity extends Activity {
    private static final String CONNECT_URL = "https://calltag.pagero.kr/connect";

    private TextView receiverBadge;
    private TextView syncMessage;
    private TextView sourceCustomerCount;
    private Button syncButton;
    private boolean receiverRegistered;

    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !UniversalLeadSyncManager.ACTION_LEADS_UPDATED.equals(intent.getAction())) return;
            boolean success = intent.getBooleanExtra(UniversalLeadSyncManager.EXTRA_SUCCESS, false);
            int imported = intent.getIntExtra(UniversalLeadSyncManager.EXTRA_IMPORTED, 0);
            int updated = intent.getIntExtra(UniversalLeadSyncManager.EXTRA_UPDATED, 0);
            int rejected = intent.getIntExtra(UniversalLeadSyncManager.EXTRA_REJECTED, 0);
            String message = intent.getStringExtra(UniversalLeadSyncManager.EXTRA_MESSAGE);
            if (message == null || message.trim().isEmpty()) {
                message = success ? "외부 문의 확인을 완료했습니다." : "외부 문의를 확인하지 못했습니다.";
            }
            if (success && (imported > 0 || updated > 0 || rejected > 0)) {
                String counts = "신규 " + imported + "건 · 기존 고객 " + updated + "건";
                if (rejected > 0) counts += " · 확인 필요 " + rejected + "건";
                syncMessage.setText(counts + "\n" + message);
            } else {
                syncMessage.setText(message);
            }
            setReceiverBadge(success ? "확인 완료" : "확인 필요", success);
            finishSyncButton();
            refreshLocalStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("외부 문의 연동");
        setContentView(buildContent());
        refreshLocalStatus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(UniversalLeadSyncManager.ACTION_LEADS_UPDATED);
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
        refreshLocalStatus();
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
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(body, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = titleText("‹", 32f);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setContentDescription("뒤로가기");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));
        TextView title = titleText("외부 문의 연동", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(4);
        header.addView(title, titleParams);
        body.addView(header, matchWrap());

        LinearLayout hero = card();
        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        heroText.addView(titleText("외부 문의 수신", 18f), matchWrap());
        heroText.addView(mutedText("연결된 채널의 새 문의를 콜태그 고객으로 가져옵니다."), topMargin(5));
        heroTop.addView(heroText, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        receiverBadge = badge("확인 중", false);
        heroTop.addView(receiverBadge);
        hero.addView(heroTop, matchWrap());

        syncMessage = bodyText("앱 수신 상태를 확인하고 있습니다.");
        syncMessage.setLineSpacing(0f, 1.22f);
        hero.addView(syncMessage, topMargin(14));

        sourceCustomerCount = mutedText("외부 출처 고객 확인 중");
        hero.addView(sourceCustomerCount, topMargin(9));

        syncButton = actionButton("지금 문의 확인", true);
        syncButton.setOnClickListener(v -> requestLeadSync());
        hero.addView(syncButton, fixedTop(50, 15));
        body.addView(hero, topMargin(12));

        body.addView(sectionTitle("연동 채널"), topMargin(24));
        body.addView(mutedText("채널 연결은 웹에서 설정하고, 실제 문의는 이 앱이 안전하게 가져옵니다."), topMargin(5));

        addChannel(body, "PageRo", "기본 연동", true,
                "페이지로 랜딩 문의는 기존 전용 수신 경로와 문자 자동화를 그대로 사용합니다.");
        addChannel(body, "Meta Lead Ads", "웹 설정", false,
                "Facebook 페이지의 리드 광고 문의를 콜태그 고객 흐름으로 연결합니다.");
        addChannel(body, "Google Forms", "웹 설정", false,
                "Google Form 제출을 Apps Script 브리지로 받아 콜태그 문의로 변환합니다.");
        addChannel(body, "Generic Webhook", "웹 설정", false,
                "외부 폼·CRM·자동화 서비스의 JSON Webhook 문의를 받습니다.");
        addChannel(body, "Direct API", "개발자 연동", false,
                "외부 서버에서 CallTag Lead API로 문의를 직접 전송합니다.");

        Button connect = actionButton("웹에서 연동 설정 열기", true);
        connect.setOnClickListener(v -> openConnect());
        body.addView(connect, fixedTop(52, 16));

        LinearLayout safety = card();
        safety.addView(sectionTitle("앱 수신 방식"), matchWrap());
        safety.addView(bodyText("• 알림 신호에는 고객 이름·전화번호를 넣지 않습니다.\n"
                + "• 앱이 로그인 세션으로 서버에서 실제 문의를 가져옵니다.\n"
                + "• 고객 저장이 끝난 뒤에만 서버에 완료 처리합니다.\n"
                + "• 같은 문의는 eventId 영수증으로 중복 저장을 막습니다."), topMargin(11));
        body.addView(safety, topMargin(18));

        return scroll;
    }

    private void requestLeadSync() {
        if (!AuthSessionStore.hasSession(this)) {
            Toast.makeText(this, "콜태그 로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        syncButton.setEnabled(false);
        syncButton.setText("문의 확인 중...");
        setReceiverBadge("확인 중", false);
        syncMessage.setText("서버에서 새 외부 문의를 확인하고 있습니다.");
        boolean started = UniversalLeadSyncManager.requestSync(this, true);
        if (!started && !UniversalLeadSyncManager.isRunning()) {
            syncMessage.setText("동기화를 시작하지 못했습니다. 잠시 후 다시 시도해주세요.");
            setReceiverBadge("확인 필요", false);
            finishSyncButton();
        }
    }

    private void finishSyncButton() {
        syncButton.setEnabled(true);
        syncButton.setText("지금 문의 확인");
    }

    private void refreshLocalStatus() {
        boolean signedIn = AuthSessionStore.hasSession(this);
        if (!UniversalLeadSyncManager.isRunning()) {
            setReceiverBadge(signedIn ? "앱 수신 준비" : "로그인 필요", signedIn);
        }
        int withSource = 0;
        try (CallTagDbHelper db = new CallTagDbHelper(this)) {
            List<Customer> customers = db.listCustomers(null);
            for (Customer customer : customers) {
                String source = customer == null || customer.source == null ? "" : customer.source.trim();
                if (!source.isEmpty()) withSource++;
            }
        } catch (RuntimeException ignored) {
            // Status UI must never block the CRM screen when a local read fails.
        }
        sourceCustomerCount.setText("현재 출처가 기록된 고객 " + withSource + "명");
        if (!signedIn) {
            syncMessage.setText("외부 문의를 받으려면 콜태그 로그인이 필요합니다.");
        } else if (!UniversalLeadSyncManager.isRunning()
                && (syncMessage.getText() == null || syncMessage.getText().toString().contains("확인하고 있습니다"))) {
            syncMessage.setText("FCM 신호 수신 + 안전 동기화가 준비되어 있습니다.");
        }
    }

    private void setReceiverBadge(String label, boolean positive) {
        if (receiverBadge == null) return;
        receiverBadge.setText(label);
        receiverBadge.setTextColor(getColor(positive ? R.color.success : R.color.text_secondary));
        receiverBadge.setBackground(pillBackground(
                getColor(positive ? R.color.success_soft : R.color.surface_soft),
                getColor(positive ? R.color.success : R.color.border)));
    }

    private void addChannel(LinearLayout body, String name, String state, boolean positive, String description) {
        LinearLayout item = card();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(titleText(name, 16f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge(state, positive));
        item.addView(top, matchWrap());
        item.addView(mutedText(description), topMargin(8));
        body.addView(item, topMargin(10));
    }

    private void openConnect() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(CONNECT_URL)));
        } catch (RuntimeException error) {
            Toast.makeText(this, "연동 설정 페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(16), dp(17), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView badge(String value, boolean positive) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(11f);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(10), dp(6), dp(10), dp(6));
        text.setTextColor(getColor(positive ? R.color.success : R.color.text_secondary));
        text.setBackground(pillBackground(
                getColor(positive ? R.color.success_soft : R.color.surface_soft),
                getColor(positive ? R.color.success : R.color.border)));
        return text;
    }

    private GradientDrawable pillBackground(int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(999));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private TextView titleText(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView sectionTitle(String value) {
        TextView text = titleText(value, 15f);
        text.setTextColor(getColor(R.color.text_secondary));
        return text;
    }

    private TextView bodyText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(14f);
        return text;
    }

    private TextView mutedText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_muted));
        text.setTextSize(12f);
        text.setLineSpacing(0f, 1.18f);
        return text;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
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
