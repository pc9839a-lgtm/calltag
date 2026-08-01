package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 고객 목록과 고객 연결 문자를 전환하는 compact 세그먼트 탭. */
public final class CustomerHubTabsView extends LinearLayout {
    private TextView customersTab;
    private TextView messagesTab;

    public CustomerHubTabsView(Context context) {
        super(context);
        init();
    }

    public CustomerHubTabsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomerHubTabsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setPadding(dp(3), dp(3), dp(3), dp(3));
        setBackgroundResource(R.drawable.bg_soft_panel);

        customersTab = tab("고객");
        messagesTab = tab("문자");
        addView(customersTab, new LayoutParams(0, dp(38), 1f));
        LayoutParams messageParams = new LayoutParams(0, dp(38), 1f);
        messageParams.leftMargin = dp(4);
        addView(messagesTab, messageParams);

        customersTab.setOnClickListener(v -> showCustomers());
        messagesTab.setOnClickListener(v -> showMessages());
        post(this::showCustomers);
    }

    public void showCustomers() {
        View customers = getRootView().findViewById(R.id.customerListPanel);
        View messages = getRootView().findViewById(R.id.customerMessagesPanel);
        if (customers == null || messages == null) return;
        customers.setVisibility(VISIBLE);
        messages.setVisibility(GONE);
        style(customersTab, true);
        style(messagesTab, false);
    }

    public void showMessages() {
        View customers = getRootView().findViewById(R.id.customerListPanel);
        View messages = getRootView().findViewById(R.id.customerMessagesPanel);
        if (customers == null || messages == null) return;
        customers.setVisibility(GONE);
        messages.setVisibility(VISIBLE);
        style(customersTab, false);
        style(messagesTab, true);
    }

    private TextView tab(String label) {
        TextView view = new TextView(getContext());
        view.setText(label);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(14f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(false);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private void style(TextView tab, boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(10));
        background.setColor(getContext().getColor(selected
                ? R.color.primary_soft : android.R.color.transparent));
        tab.setBackground(background);
        tab.setTextColor(getContext().getColor(selected
                ? R.color.primary : R.color.text_secondary));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
