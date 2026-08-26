package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 고객 상세에서 외부 문의의 정규화 채널과 서버가 저장한 원본 source를 보여준다. */
public final class CustomerSourceDetailView extends LinearLayout {
    public CustomerSourceDetailView(Context context) {
        super(context);
        init();
    }

    public CustomerSourceDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomerSourceDetailView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(14), dp(12), dp(14), dp(12));
        setBackgroundResource(R.drawable.bg_card);
        setVisibility(GONE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        render();
    }

    private void render() {
        Context context = getContext();
        if (!(context instanceof Activity)) {
            setVisibility(GONE);
            return;
        }
        long customerId = ((Activity) context).getIntent().getLongExtra(
                CustomerDetailActivity.EXTRA_CUSTOMER_ID, -1L);
        if (customerId <= 0L) {
            setVisibility(GONE);
            return;
        }

        Customer customer;
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            customer = db.findCustomerById(customerId);
        } finally {
            db.close();
        }
        String raw = CustomerSourceResolver.rawSource(context, customer);
        String label = CustomerSourceResolver.label(context, customer);
        if (raw.isEmpty() || label.isEmpty()) {
            removeAllViews();
            setVisibility(GONE);
            return;
        }

        removeAllViews();
        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(VERTICAL);

        TextView title = text("유입 채널", 12f, R.color.text_muted, true);
        copy.addView(title, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView original = text("원본 · " + compact(raw), 12f, R.color.text_secondary, false);
        LinearLayout.LayoutParams originalParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        originalParams.topMargin = dp(4);
        copy.addView(original, originalParams);

        addView(copy, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView badge = CustomerSourceBadge.create(context, label);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, dp(28));
        badgeParams.leftMargin = dp(10);
        addView(badge, badgeParams);
        setVisibility(VISIBLE);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getContext().getColor(color));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private String compact(String value) {
        String safe = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return safe.length() <= 72 ? safe : safe.substring(0, 69) + "…";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
