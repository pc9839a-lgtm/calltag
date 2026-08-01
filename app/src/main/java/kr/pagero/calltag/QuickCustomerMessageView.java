package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/** MainActivity의 기존 캘린더 클릭 바인딩을 무시하고 고객 문자 탭을 여는 홈 버튼. */
public final class QuickCustomerMessageView extends TextView {
    private OnClickListener internalClick;
    private boolean ready;

    public QuickCustomerMessageView(Context context) {
        super(context);
        init();
    }

    public QuickCustomerMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QuickCustomerMessageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        internalClick = v -> openCustomerMessages();
        ready = true;
        super.setOnClickListener(internalClick);
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        if (ready) super.setOnClickListener(internalClick);
        else super.setOnClickListener(listener);
    }

    private void openCustomerMessages() {
        View root = getRootView();
        View customerNav = root.findViewById(R.id.navCustomers);
        if (customerNav != null) customerNav.performClick();
        post(() -> {
            View tabs = root.findViewById(R.id.customerHubTabs);
            if (tabs instanceof CustomerHubTabsView) {
                ((CustomerHubTabsView) tabs).showMessages();
            }
        });
    }
}
