package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 페이지로에서 실제 유입된 고객에게만 배지를 별도 줄로 표시한다. */
public final class TodayTaskListView extends LinearLayout {
    public TodayTaskListView(Context context) {
        super(context);
    }

    public TodayTaskListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TodayTaskListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        decorate(child);
        super.addView(child, params);
    }

    private void decorate(View child) {
        if (!(child instanceof LinearLayout)) return;
        LinearLayout card = (LinearLayout) child;
        if (card.getChildCount() == 0 || !(card.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout header = (LinearLayout) card.getChildAt(0);
        if (header.getChildCount() == 0 || !(header.getChildAt(0) instanceof TextView)) return;
        if (hasBadge(card)) return;

        String customerName = String.valueOf(((TextView) header.getChildAt(0)).getText()).trim();
        Customer customer = findCustomer(customerName);
        if (customer == null || !CustomerSourceResolver.isPagero(customer)) return;

        LinearLayout sourceRow = new LinearLayout(getContext());
        sourceRow.setOrientation(HORIZONTAL);
        sourceRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView badge = CustomerSourceBadge.create(getContext(), CustomerSourceResolver.PAGERO);
        sourceRow.addView(badge, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(26)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(6);
        card.addView(sourceRow, Math.min(1, card.getChildCount()), rowParams);
    }

    private Customer findCustomer(String name) {
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            for (Customer customer : db.listCustomers(null)) {
                if (customer.displayName.equals(name)) return customer;
            }
            return null;
        } finally {
            db.close();
        }
    }

    private boolean hasBadge(LinearLayout card) {
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    if ("customer_source_badge".equals(row.getChildAt(j).getTag())) return true;
                }
            }
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
