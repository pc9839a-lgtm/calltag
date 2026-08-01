package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

/** 기존 4개 화면 중 하나가 열리면 통계 화면을 자동으로 닫는다. */
public final class StatsSectionScrollView extends ScrollView {
    public StatsSectionScrollView(Context context) {
        super(context);
        init();
    }

    public StatsSectionScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatsSectionScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getViewTreeObserver().addOnGlobalLayoutListener(this::closeWhenAnotherSectionOpens);
    }

    private void closeWhenAnotherSectionOpens() {
        if (getVisibility() != VISIBLE) return;
        View root = getRootView();
        if (isVisible(root, R.id.sectionToday)
                || isVisible(root, R.id.sectionCustomers)
                || isVisible(root, R.id.sectionConsultations)
                || isVisible(root, R.id.sectionMore)) {
            setVisibility(GONE);
        }
    }

    private boolean isVisible(View root, int id) {
        View view = root.findViewById(id);
        return view != null && view.getVisibility() == VISIBLE;
    }
}
