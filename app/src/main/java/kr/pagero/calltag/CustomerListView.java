package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 고객 카드에는 상태/문자/삭제 3개 액션만 작게 노출하고, 나머지는 상세에서 처리한다. */
public final class CustomerListView extends LinearLayout {
    public CustomerListView(Context context) { super(context); }
    public CustomerListView(Context context, AttributeSet attrs) { super(context, attrs); }
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
        card.setBackgroundResource(R.drawable.bg_card);
        card.setMinimumHeight(dp(128));
        decorateCustomerCard(card);
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).bottomMargin = dp(8);
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
        if (customer == null) return;

        final long customerId = customer.id;
        final String customerPhone = phone;
        final boolean pagero = CustomerSourceResolver.isPagero(customer);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openCustomer(customerId));

        removeOldSourceBadges(header);
        if (phoneView.getParent() == card) card.removeView(phoneView);
        if (recentView != null && recentView.getParent() == card) card.removeView(recentView);

        LinearLayout contactRow = new LinearLayout(getContext());
        contactRow.setOrientation(HORIZONTAL);
        contactRow.setGravity(Gravity.CENTER_VERTICAL);
        phoneView.setSingleLine(true);
        phoneView.setEllipsize(TextUtils.TruncateAt.END);
        phoneView.setTextSize(14f);
        contactRow.addView(phoneView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        if (pagero) {
            TextView source = CustomerSourceBadge.create(getContext(), "페이지로");
            LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, dp(26));
            sourceParams.leftMargin = dp(8);
            contactRow.addView(source, sourceParams);
        }
        card.addView(contactRow, Math.min(1, card.getChildCount()), topMargin(8));

        TextView memoView = new TextView(getContext());
        String compactMemo = compact(memo);
        memoView.setText(compactMemo.isEmpty() ? "메모 없음" : compactMemo);
        memoView.setTextColor(getContext().getColor(compactMemo.isEmpty()
                ? R.color.text_muted : R.color.text_secondary));
        memoView.setTextSize(13f);
        memoView.setSingleLine(true);
        memoView.setEllipsize(TextUtils.TruncateAt.END);
        memoView.setIncludeFontPadding(false);
        card.addView(memoView, Math.min(2, card.getChildCount()), topMargin(6));

        int contentIndex = 3;
        if (pagero) {
            PageroLeadSmsStatusResolver.State sms = PageroLeadSmsStatusResolver.latest(
                    getContext(), customerId);
            if (sms != null) {
                TextView state = new TextView(getContext());
                state.setText(sms.label + (sms.reason.isEmpty() ? "" : " · " + compact(sms.reason)));
                state.setTextSize(12f);
                state.setSingleLine(true);
                state.setEllipsize(TextUtils.TruncateAt.END);
                state.setIncludeFontPadding(false);
                state.setTextColor(getContext().getColor(
                        PageroLeadReceiptStore.SMS_FAILED.equals(sms.code)
                                ? R.color.danger : R.color.text_secondary));
                card.addView(state, Math.min(contentIndex++, card.getChildCount()), topMargin(5));
            }
        }

        if (recentView != null) {
            recentView.setSingleLine(true);
            recentView.setEllipsize(TextUtils.TruncateAt.END);
            recentView.setTextSize(12f);
            card.addView(recentView, Math.min(contentIndex, card.getChildCount()), topMargin(5));
        }

        if (actions == null) return;
        Button status = findActionButton(actions, "상태 변경");
        if (status == null && actions.getChildCount() > 0 && actions.getChildAt(0) instanceof Button) {
            status = (Button) actions.getChildAt(0);
        }
        if (status == null) return;

        actions.removeAllViews();
        actions.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

        View spacer = new View(getContext());
        actions.addView(spacer, new LinearLayout.LayoutParams(0, dp(40), 1f));

        configureIconButton(status, R.drawable.ic_customer_status, "상태 변경");
        actions.addView(status, iconParams(0));

        Button message = iconButton(R.drawable.ic_customer_message, "문자 보내기");
        message.setOnClickListener(v -> openCustomerMessage(customerPhone));
        actions.addView(message, iconParams(7));

        Button delete = iconButton(R.drawable.ic_customer_delete, "고객 삭제");
        delete.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), CustomerDeleteActivity.class)
                        .putExtra(CustomerDeleteActivity.EXTRA_CUSTOMER_ID, customerId)));
        actions.addView(delete, iconParams(7));

        ViewGroup.LayoutParams raw = actions.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) raw;
            lp.height = dp(44);
            lp.topMargin = dp(9);
            actions.setLayoutParams(lp);
        }
    }

    private Button findActionButton(LinearLayout row, String text) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child instanceof Button && text.contentEquals(((Button) child).getText())) {
                return (Button) child;
            }
        }
        return null;
    }

    private Button iconButton(int drawable, String description) {
        Button button = new Button(getContext());
        configureIconButton(button, drawable, description);
        return button;
    }

    private void configureIconButton(Button button, int drawable, String description) {
        button.setText("");
        button.setContentDescription(description);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(0, 0, 0, 0);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_clickable_row);
        button.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
    }

    private LinearLayout.LayoutParams iconParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(40));
        params.leftMargin = dp(leftMargin);
        return params;
    }

    private void removeOldSourceBadges(LinearLayout row) {
        if (row == null) return;
        for (int i = row.getChildCount() - 1; i >= 0; i--) {
            if ("customer_source_badge".equals(row.getChildAt(i).getTag())) row.removeViewAt(i);
        }
    }

    private String compact(String value) {
        if (value == null) return "";
        String safe = value.trim().replaceAll("\\s+", " ");
        return safe.length() <= 52 ? safe : safe.substring(0, 49) + "…";
    }

    private void openCustomer(long customerId) {
        getContext().startActivity(new Intent(getContext(), CustomerDetailActivity.class)
                .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customerId));
    }

    private void openCustomerMessage(String phone) {
        if (!FeatureAccessGate.require(getContext(), FeatureAccessGate.MESSAGE)) return;
        long customerId = 0L;
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            Customer customer = db.findByPhone(phone);
            if (customer != null) customerId = customer.id;
        } finally {
            db.close();
        }
        getContext().startActivity(new Intent(getContext(), ManualMessageActivity.class)
                .putExtra(ManualMessageActivity.EXTRA_PHONE, phone)
                .putExtra(ManualMessageActivity.EXTRA_CUSTOMER_ID, customerId));
    }

    private LayoutParams topMargin(int value) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(value);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
