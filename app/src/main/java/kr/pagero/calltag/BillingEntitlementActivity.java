package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.ProductDetails;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 더보기 > 이용권. Play 상품조회와 서버 이용권 확인은 서로 기다리지 않는다. */
public final class BillingEntitlementActivity extends Activity
        implements PlayBillingManager.Listener {
    private static final int BLUE = Color.rgb(67, 137, 255);
    private static final int TEXT = Color.rgb(244, 245, 247);
    private static final int SUBTEXT = Color.rgb(168, 173, 181);
    private static final int MUTED = Color.rgb(116, 122, 132);
    private static final int SURFACE = Color.rgb(28, 30, 34);
    private static final int BACKGROUND = Color.rgb(16, 17, 19);
    private static final int BORDER = Color.rgb(41, 44, 49);
    private static final long PLAY_LOAD_TIMEOUT_MS = 6000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView stateTitle;
    private TextView stateDetail;
    private TextView stateMeta;
    private TextView refreshButton;
    private TextView phoneButton;
    private TextView messageButton;
    private TextView restoreButton;
    private TextView manageButton;
    private PlayBillingManager billing;
    private Map<String, ProductDetails> playProducts = Collections.emptyMap();
    private boolean working;
    private boolean refreshing;
    private boolean productQueryCompleted;
    private boolean billingLoadFailed;
    private String billingError = "";

    private final Runnable billingTimeout = () -> {
        if (isFinishing() || isDestroyed() || productQueryCompleted) return;
        productQueryCompleted = true;
        billingLoadFailed = true;
        billingError = "Google Play 응답이 늦습니다. 다시 시도해주세요.";
        render();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
        billing = new PlayBillingManager(this, this);

        render();
        startPlayLoad();
        refreshEntitlement(false);
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(billingTimeout);
        if (billing != null) billing.close();
        super.onDestroy();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = button("‹", false);
        back.setTextSize(30f);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView title = text("이용권", 21f, TEXT, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        titleParams.leftMargin = dp(8);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, titleParams);

        refreshButton = button("새로고침", false);
        refreshButton.setOnClickListener(v -> {
            startPlayLoad();
            refreshEntitlement(true);
        });
        header.addView(refreshButton, new LinearLayout.LayoutParams(dp(86), dp(40)));
        root.addView(header, full());

        LinearLayout statusCard = card();
        stateTitle = text("이용권 확인 중", 20f, TEXT, true);
        stateDetail = text("잠시만 기다려주세요.", 14f, SUBTEXT, false);
        stateMeta = text("", 13f, MUTED, false);
        statusCard.addView(stateTitle, full());
        statusCard.addView(stateDetail, top(9));
        statusCard.addView(stateMeta, top(7));
        root.addView(statusCard, top(18));

        root.addView(sectionTitle("이용권 선택"), top(24));

        LinearLayout phone = productCard(
                "전화관리",
                "월 1,900원",
                "수신 고객 표시 · 통화 후 고객관리");
        phoneButton = productButton("월 1,900원 시작", FeatureEntitlementStore.PLAN_PHONE);
        phone.addView(phoneButton, fixedTop(48, 14));
        root.addView(phone, top(9));

        LinearLayout message = productCard(
                "문자자동화",
                "월 990원",
                "통화 후 자동문자 · 템플릿 · 발송관리");
        messageButton = productButton("월 990원 시작", FeatureEntitlementStore.PLAN_MESSAGE);
        message.addView(messageButton, fixedTop(48, 14));
        root.addView(message, top(10));

        restoreButton = button("구매 복원", false);
        restoreButton.setOnClickListener(v -> {
            if (billing != null && billing.isReady()) {
                billing.restore();
            } else {
                startPlayLoad();
                Toast.makeText(this, "Google Play에 다시 연결합니다.", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(restoreButton, fixedTop(50, 16));

        manageButton = button("구독 관리", false);
        manageButton.setOnClickListener(v -> openPlaySubscriptions());
        root.addView(manageButton, fixedTop(50, 8));

        LinearLayout pagero = card();
        pagero.addView(text("페이지로 이용권", 15f, TEXT, true), full());
        pagero.addView(text(
                "페이지로에서 이용 중인 통합권은 웹에서 관리할 수 있습니다.",
                13.5f,
                SUBTEXT,
                false), top(7));
        root.addView(pagero, top(20));

        root.addView(text(
                "신규 가입은 7일 무료이며 추천인 코드 입력 시 7일이 추가됩니다. 무료 이용이 끝나도 자동 결제되지 않습니다.",
                13f,
                MUTED,
                false), top(16));
        return scroll;
    }

    private void startPlayLoad() {
        if (billing == null) return;
        productQueryCompleted = false;
        billingLoadFailed = false;
        billingError = "";
        mainHandler.removeCallbacks(billingTimeout);
        mainHandler.postDelayed(billingTimeout, PLAY_LOAD_TIMEOUT_MS);
        billing.connectAndLoad();
        render();
    }

    private void refreshEntitlement(boolean notify) {
        if (refreshing) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            if (notify) Toast.makeText(this, "로그인 정보를 다시 확인해주세요.", Toast.LENGTH_LONG).show();
            return;
        }

        refreshing = true;
        if (notify) setWorking(true);
        new Thread(() -> {
            try {
                JSONObject response = AuthApiClient.billingEntitlements(session);
                FeatureEntitlementStore.saveServerEntitlement(this, response);
                runOnUiThread(() -> {
                    refreshing = false;
                    if (notify) setWorking(false);
                    render();
                    if (notify) Toast.makeText(this, "이용권을 새로 확인했습니다.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    refreshing = false;
                    if (notify) setWorking(false);
                    render();
                    if (notify) Toast.makeText(this,
                            "이용권 상태 확인에 실패했습니다. 결제 상품은 별도로 다시 불러올 수 있습니다.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "calltag-entitlement-refresh").start();
    }

    private void verifyThenPurchase(String productId) {
        if (working) return;
        if (AuthSessionStore.session(this).isEmpty()) {
            Toast.makeText(this, "로그인 정보를 다시 확인해주세요.", Toast.LENGTH_LONG).show();
            return;
        }

        FeatureEntitlementStore.Snapshot snapshot = FeatureEntitlementStore.snapshot(this);
        if (snapshot.serverChecked && snapshot.isWebSubscription()) {
            showBlocked("페이지로에서 이용 중입니다.", "현재 이용권은 페이지로에서 관리해주세요.");
            return;
        }
        if (snapshot.isProductSubscribed(productId)) {
            showBlocked("이미 이용 중입니다.", productName(productId) + " 이용권을 이미 사용하고 있습니다.");
            return;
        }
        if (snapshot.serverChecked && snapshot.purchaseBlocked) {
            String reason = snapshot.blockReason == null || snapshot.blockReason.trim().isEmpty()
                    ? "현재 계정에서는 새 결제를 시작할 수 없습니다." : snapshot.blockReason;
            showBlocked("결제를 시작할 수 없습니다.", reason);
            return;
        }
        if (!playProducts.containsKey(productId)) {
            startPlayLoad();
            Toast.makeText(this,
                    billingError.isEmpty() ? "결제 정보를 다시 불러옵니다." : billingError,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        billing.purchase(productId);
    }

    private void render() {
        FeatureEntitlementStore.Snapshot value = FeatureEntitlementStore.snapshot(this);
        renderCurrentPlan(value);
        updateProductButton(phoneButton, "월 1,900원 시작", FeatureEntitlementStore.PLAN_PHONE, value);
        updateProductButton(messageButton, "월 990원 시작", FeatureEntitlementStore.PLAN_MESSAGE, value);

        setEnabled(restoreButton, !working);
        boolean hasPaid = value.phoneSubscribed || value.messageSubscribed;
        manageButton.setVisibility(hasPaid ? View.VISIBLE : View.GONE);
        setEnabled(manageButton, !working && hasPaid);
    }

    private void renderCurrentPlan(FeatureEntitlementStore.Snapshot value) {
        stateMeta.setVisibility(View.VISIBLE);

        if (!value.serverChecked) {
            stateTitle.setText("이용권 확인 중");
            stateDetail.setText("저장된 상태를 먼저 표시하고 서버에서 갱신합니다.");
            stateMeta.setVisibility(View.GONE);
            return;
        }

        if (value.isTrial()) {
            stateTitle.setText("무료 이용 중");
            stateDetail.setText("전화관리 · 문자자동화");
            if (value.remainingDays >= 0) {
                stateMeta.setText(value.remainingDays + "일 남음"
                        + (value.endsAt.isEmpty() ? "" : " · " + displayDate(value.endsAt) + "까지"));
            } else {
                stateMeta.setText(value.endsAt.isEmpty() ? "" : displayDate(value.endsAt) + "까지");
            }
            return;
        }

        if (value.isWebSubscription()) {
            stateTitle.setText("페이지로 통합 이용 중");
            stateDetail.setText("전화관리 · 문자자동화");
            stateMeta.setText(value.nextBillingAt.isEmpty()
                    ? "페이지로에서 구독 관리"
                    : "다음 결제일 " + displayDate(value.nextBillingAt));
            return;
        }

        if (value.phoneSubscribed && value.messageSubscribed) {
            stateTitle.setText("전화관리 · 문자자동화 이용 중");
            stateDetail.setText(joinBillingDates(value));
            stateMeta.setText("두 이용권을 모두 사용하고 있습니다.");
            return;
        }

        if (value.phoneSubscribed) {
            stateTitle.setText("전화관리 이용 중");
            stateDetail.setText(nextBillingText(value.phoneNextBillingAt));
            stateMeta.setText("문자자동화는 이용하지 않고 있습니다.");
            return;
        }

        if (value.messageSubscribed) {
            stateTitle.setText("문자자동화 이용 중");
            stateDetail.setText(nextBillingText(value.messageNextBillingAt));
            stateMeta.setText("전화관리는 이용하지 않고 있습니다.");
            return;
        }

        stateTitle.setText("이용 중인 상품이 없습니다");
        stateDetail.setText("필요한 이용권을 선택해주세요.");
        stateMeta.setText("원하는 상품만 각각 이용할 수 있습니다.");
    }

    private String joinBillingDates(FeatureEntitlementStore.Snapshot value) {
        String phoneDate = displayDate(value.phoneNextBillingAt);
        String messageDate = displayDate(value.messageNextBillingAt);
        if (!phoneDate.isEmpty() && !messageDate.isEmpty()) {
            if (phoneDate.equals(messageDate)) return "다음 결제일 " + phoneDate;
            return "전화관리 " + phoneDate + " · 문자자동화 " + messageDate;
        }
        if (!phoneDate.isEmpty()) return "전화관리 다음 결제일 " + phoneDate;
        if (!messageDate.isEmpty()) return "문자자동화 다음 결제일 " + messageDate;
        return "현재 이용 중";
    }

    private String nextBillingText(String raw) {
        String date = displayDate(raw);
        return date.isEmpty() ? "현재 이용 중" : "다음 결제일 " + date;
    }

    private void updateProductButton(
            TextView view,
            String normalLabel,
            String productId,
            FeatureEntitlementStore.Snapshot snapshot) {
        if (snapshot.isProductSubscribed(productId)) {
            view.setText("이용 중");
            setEnabled(view, false);
            return;
        }
        if (snapshot.serverChecked && snapshot.isWebSubscription()) {
            view.setText("통합 이용 중");
            setEnabled(view, false);
            return;
        }
        if (snapshot.serverChecked && snapshot.purchaseBlocked) {
            view.setText("구매 불가");
            setEnabled(view, false);
            return;
        }
        if (playProducts.containsKey(productId)) {
            view.setText(normalLabel);
            setEnabled(view, !working);
            return;
        }
        if (billingLoadFailed || productQueryCompleted) {
            view.setText("다시 시도");
            setEnabled(view, !working);
            return;
        }
        view.setText("불러오는 중…");
        setEnabled(view, false);
    }

    @Override
    public void onBillingReady(Map<String, ProductDetails> products) {
        runOnUiThread(() -> {
            mainHandler.removeCallbacks(billingTimeout);
            productQueryCompleted = true;
            billingLoadFailed = false;
            billingError = "";
            playProducts = products == null
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(products));
            render();
        });
    }

    @Override
    public void onBillingUnavailable(String message) {
        runOnUiThread(() -> {
            mainHandler.removeCallbacks(billingTimeout);
            productQueryCompleted = true;
            billingLoadFailed = true;
            billingError = message == null ? "Google Play 결제 정보를 불러오지 못했습니다." : message;
            playProducts = Collections.emptyMap();
            render();
            Toast.makeText(this, billingError, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onBillingMessage(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onServerVerified() {
        runOnUiThread(() -> {
            render();
            Toast.makeText(this, "결제 확인 완료", Toast.LENGTH_SHORT).show();
        });
    }

    private TextView productButton(String label, String productId) {
        TextView view = button(label, true);
        view.setOnClickListener(v -> verifyThenPurchase(productId));
        return view;
    }

    private void setWorking(boolean value) {
        working = value;
        refreshButton.setEnabled(!value);
        refreshButton.setAlpha(value ? 0.55f : 1f);
        refreshButton.setText(value ? "확인 중…" : "새로고침");
        render();
    }

    private void setEnabled(TextView view, boolean enabled) {
        if (view == null) return;
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.52f);
    }

    private void showBlocked(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", null)
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private void openPlaySubscriptions() {
        try {
            Uri uri = Uri.parse("https://play.google.com/store/account/subscriptions?package="
                    + getPackageName());
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception error) {
            Toast.makeText(this, "구독 관리 화면을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout productCard(String name, String price, String detail) {
        LinearLayout value = card();
        value.addView(text(name, 17f, TEXT, true), full());
        value.addView(text(price, 23f, BLUE, true), top(8));
        value.addView(text(detail, 13.5f, SUBTEXT, false), top(6));
        return value;
    }

    private LinearLayout card() {
        LinearLayout value = new LinearLayout(this);
        value.setOrientation(LinearLayout.VERTICAL);
        value.setPadding(dp(16), dp(15), dp(16), dp(15));
        value.setBackground(round(SURFACE, BORDER, 16));
        return value;
    }

    private TextView sectionTitle(String value) {
        return text(value, 15f, TEXT, true);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.18f);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String productName(String productId) {
        if (FeatureEntitlementStore.PLAN_PHONE.equals(productId)) return "전화관리";
        if (FeatureEntitlementStore.PLAN_MESSAGE.equals(productId)) return "문자자동화";
        return "이용권";
    }

    private String displayDate(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.length() >= 10) return value.substring(0, 10).replace('-', '.');
        return value;
    }
}
