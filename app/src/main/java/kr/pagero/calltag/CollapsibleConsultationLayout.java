package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 월간 캘린더 본문만 접고, 선택 날짜/일정 추가/일정 목록은 그대로 유지한다.
 * MainActivity가 consultationSummary를 다시 그려도 사용자가 마지막으로 선택한 상태를 보존한다.
 */
public final class CollapsibleConsultationLayout extends LinearLayout {
    private static final String PREFS = "calltag_calendar_ui";
    private static final String KEY_EXPANDED = "month_calendar_expanded";

    private boolean waitingForMonthCard = true;
    private boolean wrappingMonthCard;
    private boolean expanded;

    public CollapsibleConsultationLayout(Context context) {
        super(context);
        init(context);
    }

    public CollapsibleConsultationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CollapsibleConsultationLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        expanded = prefs(context).getBoolean(KEY_EXPANDED, true);
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        waitingForMonthCard = true;
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (!wrappingMonthCard && waitingForMonthCard && child != null) {
            waitingForMonthCard = false;
            wrappingMonthCard = true;
            try {
                super.addView(wrapMonthCard(child), params);
            } finally {
                wrappingMonthCard = false;
            }
            return;
        }
        super.addView(child, params);
    }

    private View wrapMonthCard(View monthCard) {
        LinearLayout wrapper = new LinearLayout(getContext());
        wrapper.setOrientation(VERTICAL);

        LinearLayout toggleRow = new LinearLayout(getContext());
        toggleRow.setOrientation(HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);
        toggleRow.setPadding(dp(14), 0, dp(12), 0);
        toggleRow.setBackgroundResource(R.drawable.bg_clickable_card);
        toggleRow.setClickable(true);
        toggleRow.setFocusable(true);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.ic_nav_consultations);
        icon.setColorFilter(getContext().getColor(R.color.text_secondary));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconParams.rightMargin = dp(10);
        toggleRow.addView(icon, iconParams);

        TextView label = new TextView(getContext());
        label.setText("월간 캘린더");
        label.setTextSize(15f);
        label.setTextColor(getContext().getColor(R.color.text_primary));
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setIncludeFontPadding(false);
        toggleRow.addView(label, new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f));

        TextView toggle = new TextView(getContext());
        toggle.setTextSize(20f);
        toggle.setTextColor(getContext().getColor(R.color.text_secondary));
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setGravity(Gravity.CENTER);
        toggle.setIncludeFontPadding(false);
        toggle.setBackground(roundButton());
        toggle.setClickable(false);
        toggle.setFocusable(false);
        toggleRow.addView(toggle, new LinearLayout.LayoutParams(dp(36), dp(36)));

        wrapper.addView(toggleRow, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(60)));

        LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        monthParams.topMargin = dp(8);
        wrapper.addView(monthCard, monthParams);

        Runnable render = () -> {
            monthCard.setVisibility(expanded ? View.VISIBLE : View.GONE);
            toggle.setText(expanded ? "⌃" : "⌄");
            toggleRow.setContentDescription(expanded
                    ? "월간 캘린더 접기" : "월간 캘린더 펼치기");
        };
        toggleRow.setOnClickListener(v -> {
            expanded = !expanded;
            prefs(getContext()).edit().putBoolean(KEY_EXPANDED, expanded).apply();
            render.run();
        });
        render.run();
        return wrapper;
    }

    private GradientDrawable roundButton() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getContext().getColor(R.color.surface));
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), getContext().getColor(R.color.border));
        return drawable;
    }

    private SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
