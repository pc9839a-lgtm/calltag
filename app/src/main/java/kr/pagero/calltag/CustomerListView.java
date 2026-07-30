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
        if (child instanceof TextView) {
            TextView empty = (TextView) child;
            empty.setMinHeight(dp(78));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(14), dp(16), dp(14));
        }
        super.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
