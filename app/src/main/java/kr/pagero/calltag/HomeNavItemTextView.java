package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/** Gives the selected home tab a distinct button surface. */
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
        setBackgroundResource(R.drawable.bg_home_tab);
        setElevation(0f);
        setTranslationY(0f);
        setTextSize(13f);
        setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        post(this::enlargeIcon);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        setTextColor(getContext().getColor(selected ? android.R.color.white : R.color.nav_inactive));
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
