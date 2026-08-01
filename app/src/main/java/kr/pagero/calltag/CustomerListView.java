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
        card.setMinimumHeight(dp(126));
        decorateCustomerCard(card);

        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).bottomMargin = dp(8);
        }
    }

    private void decorateCustomerCard(LinearLayout card) {
        String phone = "";
        LinearLayout actions = null;
        LinearLayout header = null;
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (i == 0 && child instanceof LinearLayout) header = (LinearLayout) child;
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
        if (phone.isEmpty()) return;

        Customer customer = null;
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            customer = db.findByPhone(phone);
        } finally {
            db.close();
        }

        if (header != null && customer != null && !hasSourceBadge(header)) {
            TextView source = CustomerSourceBadge.create(
                    getContext(), CustomerSourceResolver.label(getContext(), customer));
            LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(28));
            sourceParams.leftMargin = dp(6);
            int index = Math.max(1, header.getChildCount() - 1);
            header.addView(source, index, sourceParams);
        }

        if (actions == null || hasMessageButton(actions)) return;
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
        message.setTag("customer_message_button");
        message.setOnClickListener(v -> openCustomerMessage(customerPhone));

        actions.addView(message, 0, new LinearLayout.LayoutParams(0, dp(44), 1f));
        for (int i = 1; i < actions.getChildCount(); i++) {
            View child = actions.getChildAt(i);
            if (child.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams item = (LinearLayout.LayoutParams) child.getLayoutParams();
                item.leftMargin = dp(6);
                child.setLayoutParams(item);
            }
        }
    }

    private boolean hasSourceBadge(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            if ("customer_source_badge".equals(row.getChildAt(i).getTag())) return true;
        }
        return false;
    }

    private boolean hasMessageButton(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            if ("customer_message_button".equals(row.getChildAt(i).getTag())) return true;
        }
        return false;
    }

    private void openCustomerMessage(String phone) {
        if (!FeatureEntitlementStore.hasMessageAccess(getContext())) {
            Toast.makeText(getContext(), "문자 기능 이용 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
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
