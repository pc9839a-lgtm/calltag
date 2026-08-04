package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
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

/** 더보기 > 이용권·결제. */
public final class BillingEntitlementActivity extends Activity
        implements PlayBillingManager.Listener {
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int TEXT = Color.rgb(15, 23, 42);
    private static final int SUBTEXT = Color.rgb(71, 85, 105);
    private static final int MUTED = Color.rgb(148, 163, 184);
    private static final int SURFACE = Color.WHITE;
    private static final int BACKGROUND = Color.rgb(248, 250, 252);
    private static final int BORDER = Color.rgb(226, 232, 240);

    private TextView stateTitle;
    private TextView stateDetail;
    private TextView stateMeta;
    private TextView billingNoticeTitle;
    private TextView billingNoticeDetail;
    private TextView refreshButton;
    private TextView bundleButton;
    private TextView phoneButton;
    private TextView messageButton;
    private TextView restoreButton;
    private TextView manageButton;
    private PlayBillingManager billing;
    private Map<String, ProductDetails> playProducts = Collections.emptyMap();
    private boolean working;
    private boolean productQueryCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
        billing = new PlayBillingManager(this, this);
        render();
        refreshEntitlement(false);
    }

    @Override
    protected void onDestroy() {
        if (billing != null) billing.close();
        super.onDestroy();
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
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = text("이용권·결제", 22f, TEXT, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, titleParams);
        refreshButton = button("새로고침", false);
        refreshButton.setOnClickListener(v -> refreshEntitlement(true));
        header.addView(refreshButton, new LinearLayout.LayoutParams(dp(86), dp(40)));
        root.addView(header, full());

        LinearLayout statusCard = card();
        stateTitle = text("이용권 확인 중", 20f, TEXT, true);
        stateDetail = text("현재 이용 상태를 확인하고 있어요.", 15f, SUBTEXT, false);
        stateMeta = text("", 13f, MUTED, false);
        statusCard.addView(stateTitle, full());
        statusCard.addView(stateDetail, top(9));
        statusCard.addView(stateMeta, top(8));
        root.addView(statusCard, top(22));

        LinearLayout billingNotice = card();
        billingNoticeTitle = text("앱 결제 준비 상태", 16f, TEXT, true);
        billingNoticeDetail = text("결제 가능 여부를 확인하고 있어요.", 14f, SUBTEXT, false);
        billingNotice.addView(billingNoticeTitle, full());
        billingNotice.addView(billingNoticeDetail, top(8));
        root.addView(billingNotice, top(12));

        root.addView(sectionTitle("이용할 상품"), top(28));
        LinearLayout bundle = productCard(
                "통합권",
                "월 6,000원",
                "전화관리 · 문자자동화 · 페이지로",
                true);
        bundleButton = productButton("통합권 결제", FeatureEntitlementStore.PLAN_BUNDLE);
        bundle.addView(bundleButton, fixedTop(48, 16));
        root.addView(bundle, top(10));

        LinearLayout phone = productCard(
                "전화관리",
                "월 1,900원",
                "수신 고객 표시 · 통화 후 고객관리",
                false);
        phoneButton = productButton("전화관리 결제", FeatureEntitlementStore.PLAN_PHONE);
        phone.addView(phoneButton, fixedTop(48, 16));
        root.addView(phone, top(12));

        LinearLayout message = productCard(
                "문자자동화",
                "월 990원",
                "통화 후 자동문자 · 템플릿 · 발송관리",
                false);
        messageButton = productButton("문자자동화 결제", FeatureEntitlementStore.PLAN_MESSAGE);
        message.addView(messageButton, fixedTop(48, 16));
        root.addView(message, top(12));

        restoreButton = button("Google Play 구매 복원", false);
        restoreButton.setOnClickListener(v -> {
            FeatureEntitlementStore.Snapshot snapshot = FeatureEntitlementStore.snapshot(this);
            if (!snapshot.playBillingAvailable) {
                showPlayPreparing(snapshot);
                return;
            }
            billing.restore();
        });
        root.addView(restoreButton, fixedTop(50, 18));

        manageButton = button("Google Play 구독 관리", false);
        manageButton.setOnClickListener(v -> openPlaySubscriptions());
        root.addView(manageButton, fixedTop(50, 10));

        LinearLayout pagero = card();
        pagero.addView(text("랜딩페이지만 필요하신가요?", 16f, TEXT, true), full());
        pagero.addView(text(
                "페이지로 단독 이용권은 페이지로 웹에서 관리합니다. 이미 웹에서 통합권을 이용 중이면 앱 결제는 자동으로 차단됩니다.",
                14f,
                SUBTEXT,
                false), top(8));
        root.addView(pagero, top(24));

        root.addView(text(
                "무료 이용은 기본 3일이며, 추천인 코드를 등록하면 5일이 추가됩니다. 무료기간 종료 후 자동 결제되지 않습니다.",
                13f,
                MUTED,
                false), top(18));
        return scroll;
    }

    private void refreshEntitlement(boolean notify) {
        if (working) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            Toast.makeText(this, "로그인 정보를 다시 확인해주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        setWorking(true);
        new Thread(() -> {
            try {
                JSONObject response = AuthApiClient.billingEntitlements(session);
                FeatureEntitlementStore.saveServerEntitlement(this, response);
                runOnUiThread(() -> {
                    setWorking(false);
                    render();
                    maybeLoadPlayProducts();
                    if (notify) {
                        Toast.makeText(this, "최신 이용권을 확인했어요.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setWorking(false);
                    render();
                    if (notify) {
                        Toast.makeText(this,
                                "이용권을 확인하지 못했어요. 인터넷 연결을 확인해주세요.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, "calltag-entitlement-refresh").start();
    }

    private void maybeLoadPlayProducts() {
        FeatureEntitlementStore.Snapshot snapshot = FeatureEntitlementStore.snapshot(this);
        if (!snapshot.playBillingAvailable) {
            productQueryCompleted = false;
            playProducts = Collections.emptyMap();
            render();
            return;
        }
        billing.connectAndLoad();
    }

    private void verifyThenPurchase(String productId) {
        if (working) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            Toast.makeText(this, "로그인 정보를 다시 확인해주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        setWorking(true);
        new Thread(() -> {
            try {
                JSONObject response = AuthApiClient.billingEntitlements(session);
                FeatureEntitlementStore.saveServerEntitlement(this, response);
                FeatureEntitlementStore.Snapshot snapshot = FeatureEntitlementStore.snapshot(this);
                runOnUiThread(() -> {
                    setWorking(false);
                    render();
                    if (!snapshot.playBillingAvailable) {
                        showPlayPreparing(snapshot);
                    } else if (snapshot.isWebSubscription()) {
                        showBlocked("페이지로에서 구독 중입니다.",
                                "현재 앱에서 다시 결제하지 않아도 됩니다.");
                    } else if (!snapshot.canStartPlayPurchase()) {
                        showBlocked("이미 이용 중인 구독이 있습니다.",
                                "현재 결제정보를 확인한 뒤 다시 시도해주세요.");
                    } else {
                        billing.purchase(productId);
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setWorking(false);
                    Toast.makeText(this,
                            "중복 결제 여부를 확인하지 못해 결제를 시작하지 않았어요.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "calltag-prepurchase-gate").start();
    }

    private void render() {
        FeatureEntitlementStore.Snapshot value = FeatureEntitlementStore.snapshot(this);
        if (!value.serverChecked) {
            stateTitle.setText("이용권 확인 필요");
            stateDetail.setText("현재 이용권을 확인하고 있습니다.");
            stateMeta.setText("확인 전에는 결제를 시작하지 않습니다.");
        } else if (value.isTrial() && value.active) {
            stateTitle.setText("무료 이용 중");
            stateDetail.setText(value.remainingDays >= 0
                    ? "통합 기능을 " + value.remainingDays + "일 더 사용할 수 있어요."
                    : "통합 기능을 무료로 이용하고 있어요.");
            stateMeta.setText(value.endsAt.isEmpty()
                    ? "기본 3일 · 추천 등록 시 +5일"
                    : "무료 이용 종료일  " + displayDate(value.endsAt));
        } else if (value.isWebSubscription()) {
            stateTitle.setText("페이지로에서 통합권 이용 중");
            stateDetail.setText("현재 앱에서 추가 결제할 필요가 없습니다.");
            stateMeta.setText(value.nextBillingAt.isEmpty()
                    ? "결제 경로  페이지로 웹"
                    : "다음 결제일  " + displayDate(value.nextBillingAt));
        } else if (value.active) {
            stateTitle.setText(planName(value.plan) + " 이용 중");
            stateDetail.setText(value.nextBillingAt.isEmpty()
                    ? "Google Play에서 결제 중"
                    : "다음 결제일  " + displayDate(value.nextBillingAt));
            stateMeta.setText("결제 경로  " + channelName(value.channel));
        } else {
            stateTitle.setText("이용권이 없습니다");
            stateDetail.setText("원하는 상품을 선택해 이용할 수 있어요.");
            stateMeta.setText("무료 이용 종료 후 자동 결제되지 않습니다.");
        }

        if (!value.serverChecked) {
            billingNoticeTitle.setText("결제 가능 여부 확인 중");
            billingNoticeDetail.setText("확인이 끝나기 전에는 결제되지 않습니다.");
        } else if (!value.playBillingAvailable) {
            billingNoticeTitle.setText("Google Play 결제 준비 중");
            billingNoticeDetail.setText(value.playBillingMessage);
        } else if (!productQueryCompleted) {
            billingNoticeTitle.setText("Google Play 상품 확인 중");
            billingNoticeDetail.setText("등록된 결제 상품을 불러오고 있습니다.");
        } else if (playProducts.isEmpty()) {
            billingNoticeTitle.setText("Google Play 상품 확인 필요");
            billingNoticeDetail.setText("결제 상품을 불러오지 못했습니다. 잠시 후 다시 확인해주세요.");
        } else {
            billingNoticeTitle.setText("Google Play 결제 사용 가능");
            billingNoticeDetail.setText("결제 직전 기존 구독을 다시 확인해 중복 결제를 막습니다.");
        }

        updateProductButton(bundleButton, "통합권 결제", FeatureEntitlementStore.PLAN_BUNDLE, value);
        updateProductButton(phoneButton, "전화관리 결제", FeatureEntitlementStore.PLAN_PHONE, value);
        updateProductButton(messageButton, "문자자동화 결제", FeatureEntitlementStore.PLAN_MESSAGE, value);

        boolean playEnabled = value.serverChecked && value.playBillingAvailable && !working;
        setEnabled(restoreButton, playEnabled);
        boolean showManage = value.playBillingAvailable
                || FeatureEntitlementStore.CHANNEL_GOOGLE_PLAY.equals(value.channel);
        manageButton.setVisibility(showManage ? View.VISIBLE : View.GONE);
        setEnabled(manageButton, showManage && !working);
    }

    private void updateProductButton(
            TextView view,
            String normalLabel,
            String productId,
            FeatureEntitlementStore.Snapshot snapshot) {
        if (!snapshot.serverChecked || working) {
            view.setText("확인 중…");
            setEnabled(view, false);
            return;
        }
        if (!snapshot.playBillingAvailable) {
            view.setText("출시 준비 중");
            setEnabled(view, false);
            return;
        }
        if (!productQueryCompleted || !playProducts.containsKey(productId)) {
            view.setText("상품 확인 중");
            setEnabled(view, false);
            return;
        }
        view.setText(normalLabel);
        setEnabled(view, snapshot.canStartPlayPurchase());
    }

    @Override
    public void onBillingReady(Map<String, ProductDetails> products) {
        runOnUiThread(() -> {
            productQueryCompleted = FeatureEntitlementStore.isPlayBillingAvailable(this);
            playProducts = products == null
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(products));
            render();
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
            Toast.makeText(this, "이용권이 반영되었습니다.", Toast.LENGTH_LONG).show();
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

    private void showPlayPreparing(FeatureEntitlementStore.Snapshot snapshot) {
        String message = snapshot.playBillingMessage == null
                ? "앱 결제 기능을 준비하고 있습니다."
                : snapshot.playBillingMessage;
        showBlocked("Google Play 결제 준비 중",
                message + " 준비가 끝나기 전에는 결제가 진행되지 않습니다.");
    }

    private void showBlocked(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }

    private void openPlaySubscriptions() {
        try {
            Uri uri = Uri.parse("https://play.google.com/store/account/subscriptions?package="
                    + getPackageName());
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception error) {
            Toast.makeText(this, "Google Play 구독 관리를 열지 못했어요.", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout productCard(
            String name,
            String price,
            String detail,
            boolean recommended) {
        LinearLayout value = card();
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView nameView = text(name, 18f, TEXT, true);
        line.addView(nameView, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (recommended) {
            TextView badge = text("추천", 12f, Color.WHITE, true);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(round(BLUE, BLUE, 999));
            line.addView(badge, new LinearLayout.LayoutParams(dp(48), dp(28)));
        }
        value.addView(line, full());
        value.addView(text(price, 24f, BLUE, true), top(9));
        value.addView(text(detail, 14f, SUBTEXT, false), top(7));
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String planName(String plan) {
        if (FeatureEntitlementStore.PLAN_PHONE.equals(plan)) return "전화관리";
        if (FeatureEntitlementStore.PLAN_MESSAGE.equals(plan)) return "문자자동화";
        return "통합권";
    }

    private String channelName(String channel) {
        if (FeatureEntitlementStore.CHANNEL_GOOGLE_PLAY.equals(channel)) return "Google Play";
        if (FeatureEntitlementStore.CHANNEL_WEB.equals(channel)) return "페이지로 웹";
        return "확인 중";
    }

    private String displayDate(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.length() >= 10) return value.substring(0, 10).replace('-', '.');
        return value;
    }
}
