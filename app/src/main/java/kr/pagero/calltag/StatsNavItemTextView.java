package kr.pagero.calltag;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.View;
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
        setOnClickListener(v -> openStats());
        getViewTreeObserver().addOnGlobalLayoutListener(this::syncColor);
    }

    private void openStats() {
        View root = getRootView();
        hide(root, R.id.sectionToday);
        hide(root, R.id.sectionCustomers);
        hide(root, R.id.sectionConsultations);
        hide(root, R.id.sectionMore);
        View stats = root.findViewById(R.id.sectionStats);
        if (stats != null) stats.setVisibility(VISIBLE);
        setNavColor(root, R.id.navCustomers, false);
        setNavColor(root, R.id.navConsultations, false);
        setNavColor(root, R.id.navToday, false);
        setNavColor(root, R.id.navMore, false);
        syncColor();
    }

    private void syncColor() {
        View stats = getRootView().findViewById(R.id.sectionStats);
        boolean active = stats != null && stats.getVisibility() == VISIBLE;
        int color = getContext().getColor(active ? R.color.primary : R.color.nav_inactive);
        setTextColor(color);
        setCompoundDrawableTintList(ColorStateList.valueOf(color));
    }

    private void setNavColor(View root, int id, boolean active) {
        TextView view = root.findViewById(id);
        if (view != null) view.setTextColor(getContext().getColor(
                active ? R.color.primary : R.color.nav_inactive));
    }

    private void hide(View root, int id) {
        View view = root.findViewById(id);
        if (view != null) view.setVisibility(GONE);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        setCompoundDrawableTintList(ColorStateList.valueOf(color));
    }
}
