package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.TextUtils;
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
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setClickable(false);
        card.setFocusable(false);
        card.setMinimumHeight(dp(154));
        decorateCustomerCard(card);

        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).bottomMargin = dp(9);
        }
    }

    private void decorateCustomerCard(LinearLayout card) {
        String phone = "";
        TextView phoneView = null;
        TextView recentView = null;
        LinearLayout actions = null;
        LinearLayout header = null;

        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (i == 0 && child instanceof LinearLayout) header = (LinearLayout) child;
            if (child instanceof TextView) {
                TextView text = (TextView) child;
                String candidate = String.valueOf(text.getText()).trim();
                if (PhoneNumberNormalizer.normalize(candidate).length() >= 8) {
                    phone = candidate;
                    phoneView = text;
                } else if (candidate.startsWith("최근 연락")) {
                    recentView = text;
                }
            }
            if (child instanceof LinearLayout
                    && ((LinearLayout) child).getOrientation() == LinearLayout.HORIZONTAL) {
                LinearLayout row = (LinearLayout) child;
                if (row.getChildCount() >= 2 && row.getChildAt(0) instanceof Button) actions = row;
            }
        }
        if (phone.isEmpty() || phoneView == null) return;

        Customer customer;
        String memo = "";
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            customer = db.findByPhone(phone);
            if (customer != null) memo = CustomerInsightResolver.latestMemo(db, customer);
        } finally {
            db.close();
        }

        removeOldSourceBadges(header);
        if (phoneView.getParent() == card) card.removeView(phoneView);
        if (recentView != null && recentView.getParent() == card) card.removeView(recentView);

        LinearLayout contactRow = new LinearLayout(getContext());
        contactRow.setOrientation(HORIZONTAL);
        contactRow.setGravity(Gravity.CENTER_VERTICAL);
        phoneView.setSingleLine(true);
        phoneView.setEllipsize(TextUtils.TruncateAt.END);
        phoneView.setTextSize(14f);
        contactRow.addView(phoneView, new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f));

        if (customer != null && CustomerSourceResolver.isPagero(customer)) {
            TextView source = CustomerSourceBadge.create(getContext(), "페이지로");
            LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, dp(28));
            sourceParams.leftMargin = dp(8);
            contactRow.addView(source, sourceParams);
        }
        card.addView(contactRow, Math.min(1, card.getChildCount()), topMargin(9));

        TextView memoView = new TextView(getContext());
        String compactMemo = compact(memo);
        memoView.setText(compactMemo.isEmpty() ? "메모 없음" : "메모 · " + compactMemo);
        memoView.setTextColor(getContext().getColor(compactMemo.isEmpty()
                ? R.color.text_muted : R.color.text_secondary));
        memoView.setTextSize(13f);
        memoView.setSingleLine(true);
        memoView.setEllipsize(TextUtils.TruncateAt.END);
        memoView.setIncludeFontPadding(false);
        card.addView(memoView, Math.min(2, card.getChildCount()), topMargin(7));

        if (recentView != null) {
            recentView.setSingleLine(true);
            recentView.setEllipsize(TextUtils.TruncateAt.END);
            recentView.setTextSize(12.5f);
            card.addView(recentView, Math.min(3, card.getChildCount()), topMargin(6));
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

    private void removeOldSourceBadges(LinearLayout row) {
        if (row == null) return;
        for (int i = row.getChildCount() - 1; i >= 0; i--) {
            if ("customer_source_badge".equals(row.getChildAt(i).getTag())) {
                row.removeViewAt(i);
            }
        }
    }

    private String compact(String value) {
        if (value == null) return "";
        String safe = value.trim().replaceAll("\\s+", " ");
        return safe.length() <= 52 ? safe : safe.substring(0, 49) + "…";
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

    private LayoutParams topMargin(int value) {
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(value);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
