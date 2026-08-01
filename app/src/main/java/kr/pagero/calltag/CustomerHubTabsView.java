package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 고객과 문자를 한 화면 안에서 전환하는 상단 탭. */
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
        addView(customersTab, new LayoutParams(0, dp(46), 1f));
        LayoutParams messageParams = new LayoutParams(0, dp(46), 1f);
        messageParams.leftMargin = dp(5);
        addView(messagesTab, messageParams);

        customersTab.setOnClickListener(v -> showCustomers());
        messagesTab.setOnClickListener(v -> showMessages());
        post(this::showCustomers);
    }

    private TextView tab(String label) {
        TextView view = new TextView(getContext());
        view.setText(label);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(16f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private void showCustomers() {
        View customers = getRootView().findViewById(R.id.customerListPanel);
        View messages = getRootView().findViewById(R.id.customerMessagesPanel);
        if (customers == null || messages == null) return;
        customers.setVisibility(VISIBLE);
        messages.setVisibility(GONE);
        style(customersTab, true);
        style(messagesTab, false);
        View overview = getRootView().findViewById(R.id.customerOverview);
        if (overview instanceof CustomerOverviewView) {
            ((CustomerOverviewView) overview).refresh();
        }
    }

    private void showMessages() {
        View customers = getRootView().findViewById(R.id.customerListPanel);
        View messages = getRootView().findViewById(R.id.customerMessagesPanel);
        if (customers == null || messages == null) return;
        customers.setVisibility(GONE);
        messages.setVisibility(VISIBLE);
        style(customersTab, false);
        style(messagesTab, true);
    }

    private void style(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        tab.setTextColor(getContext().getColor(selected
                ? android.R.color.white : R.color.text_secondary));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
