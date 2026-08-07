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
        if (!(getContext() instanceof MainActivity)) return;
        MainActivity activity = (MainActivity) getContext();
        if (!UiLaunchGuard.tryAcquire("home_customer_messages", 500L)) return;
        MainSectionRouter.showCustomers(activity);
        post(() -> {
            View tabs = getRootView().findViewById(R.id.customerHubTabs);
            if (tabs instanceof CustomerHubTabsView) {
                ((CustomerHubTabsView) tabs).showMessages();
                CrashTelemetryStore.record(activity, "home_customer_messages", "shown", "");
            }
        });
    }
}
