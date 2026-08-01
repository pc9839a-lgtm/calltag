package kr.pagero.calltag;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;

/** 하단 중앙에서 항상 크게 보이는 홈 주 행동 버튼. */
public final class HomeNavItemTextView extends NavItemTextView {
    public HomeNavItemTextView(Context context) {
        super(context);
        init();
    }

    public HomeNavItemTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HomeNavItemTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundResource(R.drawable.bg_nav_home);
        setElevation(dp(8));
        setTranslationY(-dp(10));
        setTextColor(Color.WHITE);
        setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));
    }

    @Override
    public void setTextColor(int ignored) {
        super.setTextColor(Color.WHITE);
        setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
