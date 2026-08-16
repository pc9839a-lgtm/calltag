package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Keeps the monthly calendar collapsible without changing the date-specific schedule list below it.
 * MainActivity rebuilds consultationSummary on refresh, so this container intercepts only the first
 * dynamic child (the month card) and keeps the user's expanded/collapsed preference across refreshes.
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
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);
        toggleRow.setPadding(dp(2), 0, dp(2), 0);

        TextView label = new TextView(getContext());
        label.setText("월간 캘린더");
        label.setTextSize(13f);
        label.setTextColor(getContext().getColor(R.color.text_muted));
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setIncludeFontPadding(false);
        toggleRow.addView(label, new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f));

        TextView toggle = new TextView(getContext());
        toggle.setTextSize(13f);
        toggle.setTextColor(getContext().getColor(R.color.primary));
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setGravity(Gravity.CENTER);
        toggle.setIncludeFontPadding(false);
        toggle.setBackgroundResource(R.drawable.bg_secondary_button);
        toggle.setClickable(true);
        toggle.setFocusable(true);
        toggleRow.addView(toggle, new LinearLayout.LayoutParams(dp(82), dp(38)));

        wrapper.addView(toggleRow, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(44)));

        LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        monthParams.topMargin = dp(6);
        wrapper.addView(monthCard, monthParams);

        Runnable render = () -> {
            monthCard.setVisibility(expanded ? View.VISIBLE : View.GONE);
            toggle.setText(expanded ? "접기 ︿" : "펼치기 ﹀");
            toggle.setContentDescription(expanded ? "월간 캘린더 접기" : "월간 캘린더 펼치기");
        };
        toggle.setOnClickListener(v -> {
            expanded = !expanded;
            prefs(getContext()).edit().putBoolean(KEY_EXPANDED, expanded).apply();
            render.run();
        });
        toggleRow.setOnClickListener(v -> toggle.performClick());
        toggleRow.setClickable(true);
        toggleRow.setFocusable(true);
        render.run();
        return wrapper;
    }

    private SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
