package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** MainActivity가 추가하는 오늘 할 일 카드에 고객 유입 경로 배지를 붙인다. */
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
        if (hasBadge(header)) return;

        String customerName = String.valueOf(((TextView) header.getChildAt(0)).getText()).trim();
        Customer customer = findCustomer(customerName);
        if (customer == null) return;

        TextView badge = CustomerSourceBadge.create(
                getContext(), CustomerSourceResolver.label(getContext(), customer));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(28));
        badgeParams.leftMargin = dp(6);
        int index = Math.max(1, header.getChildCount() - 1);
        header.addView(badge, index, badgeParams);
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

    private boolean hasBadge(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            if ("customer_source_badge".equals(row.getChildAt(i).getTag())) return true;
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
