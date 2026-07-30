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
        if (child instanceof TextView && !(child instanceof LinearLayout)) {
            TextView empty = (TextView) child;
            empty.setText("등록된 고객이 없습니다");
            empty.setMinHeight(dp(78));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(14), dp(16), dp(14));
            return;
        }

        if (!(child instanceof LinearLayout)) return;
        LinearLayout card = (LinearLayout) child;
        card.setBackgroundResource(R.drawable.bg_clickable_card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setMinimumHeight(dp(96));

        if (card.getChildCount() == 0 || !(card.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout header = (LinearLayout) card.getChildAt(0);
        TextView chevron = new TextView(getContext());
        chevron.setText("›");
        chevron.setTextColor(getContext().getColor(R.color.text_muted));
        chevron.setTextSize(24f);
        chevron.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dp(22), dp(34));
        chevronParams.leftMargin = dp(8);
        header.addView(chevron, chevronParams);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}