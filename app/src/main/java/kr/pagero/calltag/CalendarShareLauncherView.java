package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.widget.TextView;

/** 캘린더 탭에서 외부 일정 공유 목록을 연다. */
public final class CalendarShareLauncherView extends TextView {
    public CalendarShareLauncherView(Context context) {
        super(context);
        init();
    }

    public CalendarShareLauncherView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CalendarShareLauncherView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), CalendarSharePickerActivity.class)));
    }
}
