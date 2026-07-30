package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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

        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).bottomMargin = dp(8);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
