package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 무료기간 종료 전·후 안내. 고객 데이터는 지우지 않고 자동화만 제한한다. */
public final class EntitlementNoticeActivity extends Activity {
    private static final String PREFS = "calltag_entitlement_notices";
    private static final String KEY_LAST_CODE = "last_code";
    private static final String KEY_LAST_DATE = "last_date";
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int TEXT = Color.rgb(15, 23, 42);
    private static final int SUBTEXT = Color.rgb(71, 85, 105);
    private static final int BORDER = Color.rgb(226, 232, 240);

    public static boolean shouldOpen(Context context) {
        FeatureEntitlementStore.Snapshot value = FeatureEntitlementStore.snapshot(context);
        String code = noticeCode(value);
        if (code.isEmpty()) return false;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String date = serverDate(value.estimatedServerNow);
        return !code.equals(prefs.getString(KEY_LAST_CODE, ""))
                || !date.equals(prefs.getString(KEY_LAST_DATE, ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FeatureEntitlementStore.Snapshot value = FeatureEntitlementStore.snapshot(this);
        String code = noticeCode(value);
        if (code.isEmpty()) {
            openMain();
            return;
        }
        markShown(code, value.estimatedServerNow);
        setContentView(buildScreen(value, code));
    }

    private LinearLayout buildScreen(FeatureEntitlementStore.Snapshot value, String code) {
        boolean expired = "TRIAL_EXPIRED".equals(code);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(32));
        root.setBackgroundColor(Color.rgb(248, 250, 252));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(26), dp(24), dp(24));
        card.setBackground(round(Color.WHITE, BORDER, 24));
        root.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = text(expired ? "무료 이용 종료" : "무료 이용 안내", 13f,
                expired ? Color.rgb(220, 38, 38) : BLUE, true);
        card.addView(eyebrow);

        String title = expired ? "무료 이용이 종료되었습니다."
                : "무료 이용이 곧 종료됩니다.";
        TextView titleView = text(title, 25f, TEXT, true);
        LinearLayout.LayoutParams titleParams = wrap();
        titleParams.topMargin = dp(10);
        card.addView(titleView, titleParams);

        String detail = expired
                ? "고객·상담 기록은 그대로 보관됩니다. 통화 후 정리와 문자 자동화는 이용권을 시작하면 다시 사용할 수 있습니다."
                : "종료 후에도 고객·상담 기록은 그대로 보관됩니다. 통화 후 정리와 문자 자동화만 일시 중지됩니다.";
        TextView detailView = text(detail, 15f, SUBTEXT, false);
        detailView.setLineSpacing(0f, 1.35f);
        LinearLayout.LayoutParams detailParams = wrap();
        detailParams.topMargin = dp(14);
        card.addView(detailView, detailParams);

        if (!value.endsAt.isEmpty()) {
            TextView end = text("종료일  " + value.endsAt.substring(0, Math.min(10, value.endsAt.length()))
                    .replace('-', '.'), 13f, SUBTEXT, false);
            LinearLayout.LayoutParams endParams = wrap();
            endParams.topMargin = dp(12);
            card.addView(end, endParams);
        }

        TextView billing = button("이용권 확인", true);
        billing.setOnClickListener(v -> {
            startActivity(new Intent(this, BillingEntitlementActivity.class));
            finish();
        });
        LinearLayout.LayoutParams billingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        billingParams.topMargin = dp(24);
        card.addView(billing, billingParams);

        TextView continueButton = button(expired ? "고객 기록 보기" : "계속 사용", false);
        continueButton.setOnClickListener(v -> openMain());
        LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        continueParams.topMargin = dp(10);
        card.addView(continueButton, continueParams);
        return root;
    }

    private static String noticeCode(FeatureEntitlementStore.Snapshot value) {
        if (!value.serverChecked) return "";
        if (value.isTrialEndingSoon()) return "TRIAL_ENDING_24H";
        if (value.isExpired()) return "TRIAL_EXPIRED";
        return "";
    }

    private void markShown(String code, long serverNow) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_LAST_CODE, code)
                .putString(KEY_LAST_DATE, serverDate(serverNow))
                .apply();
    }

    private static String serverDate(long millis) {
        long value = millis > 0L ? millis : System.currentTimeMillis();
        return DateTimeFormatter.ISO_LOCAL_DATE.format(
                Instant.ofEpochMilli(value).atZone(ZoneId.of("Asia/Seoul")));
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView button(String value, boolean primary) {
        TextView view = text(value, 15f, primary ? Color.WHITE : TEXT, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(round(primary ? BLUE : Color.WHITE,
                primary ? BLUE : BORDER, 14));
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

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
