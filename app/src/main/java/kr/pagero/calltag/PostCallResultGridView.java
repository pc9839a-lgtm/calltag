package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/** 기존 PostCallActivity의 2개 단위 버튼 생성을 3열 그리드로 재배치한다. */
public final class PostCallResultGridView extends LinearLayout {
    private final List<Button> buttons = new ArrayList<>();
    private boolean rebuilding;

    public PostCallResultGridView(Context context) {
        super(context);
        init();
    }

    public PostCallResultGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PostCallResultGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    @Override
    public void removeAllViews() {
        if (!rebuilding) buttons.clear();
        super.removeAllViews();
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (rebuilding) {
            super.addView(child, params);
            return;
        }
        if (child instanceof LinearLayout) {
            LinearLayout source = (LinearLayout) child;
            List<Button> incoming = new ArrayList<>();
            for (int i = 0; i < source.getChildCount(); i++) {
                if (source.getChildAt(i) instanceof Button) {
                    incoming.add((Button) source.getChildAt(i));
                }
            }
            if (!incoming.isEmpty()) {
                source.removeAllViews();
                buttons.addAll(incoming);
                rebuild();
                return;
            }
        }
        super.addView(child, params);
    }

    private void rebuild() {
        rebuilding = true;
        super.removeAllViews();
        for (int start = 0; start < buttons.size(); start += 3) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            for (int index = start; index < Math.min(start + 3, buttons.size()); index++) {
                Button button = buttons.get(index);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
                if (index > start) params.leftMargin = dp(6);
                row.addView(button, params);
            }
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            if (start > 0) rowParams.topMargin = dp(6);
            super.addView(row, rowParams);
        }
        rebuilding = false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
