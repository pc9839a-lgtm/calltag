package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/** 하단 중앙 홈은 배경 원 없이 아이콘과 라벨만 한 단계 크게 표시한다. */
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
        setBackground(null);
        setElevation(0f);
        setTranslationY(0f);
        setTextSize(13f);
        setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        post(this::enlargeIcon);
    }

    private void enlargeIcon() {
        Drawable[] drawables = getCompoundDrawables();
        Drawable top = drawables[1];
        if (top == null) return;
        int size = dp(29);
        top.setBounds(0, 0, size, size);
        setCompoundDrawables(drawables[0], top, drawables[2], drawables[3]);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
