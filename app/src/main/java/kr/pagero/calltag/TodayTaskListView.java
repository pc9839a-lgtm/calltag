package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * 오늘 할 일은 일정 자체에 집중한다.
 * 고객명으로 유입 경로를 역추적하면 동명이인에게 잘못된 페이지로 배지가 붙을 수 있어
 * 이 목록에서는 유입 배지를 표시하지 않는다.
 */
public final class TodayTaskListView extends LinearLayout {
    public TodayTaskListView(Context context) {
        super(context);
    }

    public TodayTaskListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TodayTaskListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
