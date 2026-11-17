package kr.pagero.calltag;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.TextView;

/** MainActivity 수정 없이 독립 통계 화면을 여는 다섯 번째 하단 메뉴. */
public final class StatsNavItemTextView extends TextView {
    public StatsNavItemTextView(Context context) {
        super(context);
        init();
    }

    public StatsNavItemTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatsNavItemTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnClickListener(v -> MainSectionRouter.select(getRootView(), R.id.sectionStats, R.id.navStats));
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        setCompoundDrawableTintList(ColorStateList.valueOf(color));
    }
}
