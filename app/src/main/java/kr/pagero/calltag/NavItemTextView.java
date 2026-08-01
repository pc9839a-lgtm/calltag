package kr.pagero.calltag;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.TextView;

public class NavItemTextView extends TextView {
    public NavItemTextView(Context context) {
        super(context);
    }

    public NavItemTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavItemTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        setCompoundDrawableTintList(ColorStateList.valueOf(color));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setCompoundDrawableTintList(getTextColors());
    }
}
