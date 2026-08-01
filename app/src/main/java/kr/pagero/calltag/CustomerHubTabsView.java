package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/** 0.35.0부터 제거된 고객/문자 상단 탭의 레이아웃 호환용 클래스. */
public final class CustomerHubTabsView extends LinearLayout {
    public CustomerHubTabsView(Context context) {
        super(context);
        init();
    }

    public CustomerHubTabsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomerHubTabsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVisibility(View.GONE);
    }
}
