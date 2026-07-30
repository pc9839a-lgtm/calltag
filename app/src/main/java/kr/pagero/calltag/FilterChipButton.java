package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public final class FilterChipButton extends Button {
    public FilterChipButton(Context context) {
        super(context);
    }

    public FilterChipButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FilterChipButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        if (!isInEditMode()) {
            boolean selected = color == getContext().getColor(R.color.primary);
            setBackgroundResource(selected
                    ? R.drawable.bg_filter_selected
                    : R.drawable.bg_secondary_button);
        }
    }
}