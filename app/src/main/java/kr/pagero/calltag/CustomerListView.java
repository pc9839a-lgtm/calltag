package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class CustomerListView extends LinearLayout {
    public CustomerListView(Context context) {
        super(context);
    }

    public CustomerListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomerListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        styleChild(child, params);
        super.addView(child, params);
    }

    private void styleChild(View child, ViewGroup.LayoutParams params) {
        if (child instanceof TextView) {
            TextView empty = (TextView) child;
            empty.setMinHeight(dp(72));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(14), dp(12), dp(14), dp(12));
            return;
        }

        if (!(child instanceof LinearLayout)) return;
        LinearLayout card = (LinearLayout) child;
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setClickable(false);
        card.setFocusable(false);
        card.setMinimumHeight(dp(112));
        attachCustomerMessageAction(card);

        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).bottomMargin = dp(8);
        }
    }

    private void attachCustomerMessageAction(LinearLayout card) {
        String phone = "";
        LinearLayout actions = null;
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof TextView) {
                String candidate = String.valueOf(((TextView) child).getText()).trim();
                if (PhoneNumberNormalizer.normalize(candidate).length() >= 8) phone = candidate;
            }
            if (child instanceof LinearLayout
                    && ((LinearLayout) child).getOrientation() == LinearLayout.HORIZONTAL) {
                LinearLayout row = (LinearLayout) child;
                if (row.getChildCount() >= 2 && row.getChildAt(0) instanceof Button) actions = row;
            }
        }
        if (phone.isEmpty() || actions == null) return;

        final String customerPhone = phone;
        Button message = new Button(getContext());
        message.setText("문자");
        message.setAllCaps(false);
        message.setTextSize(13f);
        message.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        message.setTextColor(getContext().getColor(android.R.color.white));
        message.setMinWidth(0);
        message.setPadding(dp(4), 0, dp(4), 0);
        message.setBackgroundResource(R.drawable.bg_primary_button);
        message.setOnClickListener(v -> openCustomerMessage(customerPhone));

        if (actions.getChildCount() > 0
                && actions.getChildAt(0).getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams first =
                    (LinearLayout.LayoutParams) actions.getChildAt(0).getLayoutParams();
            first.leftMargin = dp(6);
            actions.getChildAt(0).setLayoutParams(first);
        }
        actions.addView(message, 0, new LinearLayout.LayoutParams(0, dp(44), 1f));
    }

    private void openCustomerMessage(String phone) {
        if (!FeatureEntitlementStore.hasMessageAccess(getContext())) {
            Toast.makeText(getContext(), "문자자동화 이용권이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        long customerId = 0L;
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            Customer customer = db.findByPhone(phone);
            if (customer != null) customerId = customer.id;
        } finally {
            db.close();
        }
        Intent intent = new Intent(getContext(), ManualMessageActivity.class)
                .putExtra(ManualMessageActivity.EXTRA_PHONE, phone)
                .putExtra(ManualMessageActivity.EXTRA_CUSTOMER_ID, customerId);
        getContext().startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
