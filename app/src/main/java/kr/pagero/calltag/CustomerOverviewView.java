package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 고객 목록 위에 항상 보이는 핵심 수치 3개. */
public final class CustomerOverviewView extends LinearLayout {
    private final TextView total;
    private final TextView dueToday;
    private final TextView overdue;

    public CustomerOverviewView(Context context) {
        this(context, null);
    }

    public CustomerOverviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomerOverviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(6), dp(12), dp(6), dp(12));
        setBackgroundResource(R.drawable.bg_card);

        total = metric(R.color.text_primary);
        dueToday = metric(R.color.primary);
        overdue = metric(R.color.danger);
        addView(total, new LayoutParams(0, dp(64), 1f));
        addView(divider(), new LayoutParams(dp(1), dp(38)));
        addView(dueToday, new LayoutParams(0, dp(64), 1f));
        addView(divider(), new LayoutParams(dp(1), dp(38)));
        addView(overdue, new LayoutParams(0, dp(64), 1f));
        refresh();
    }

    public void refresh() {
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            total.setText(db.listCustomers(null).size() + "\n전체 고객");
            dueToday.setText(db.countDueTodayTasks() + "\n오늘 할 일");
            overdue.setText(db.countOverdueTasks() + "\n기한 지남");
        } finally {
            db.close();
        }
    }

    private TextView metric(int colorRes) {
        TextView view = new TextView(getContext());
        view.setGravity(Gravity.CENTER);
        view.setTextSize(14f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextColor(getContext().getColor(colorRes));
        view.setLineSpacing(0f, 1.2f);
        view.setIncludeFontPadding(false);
        return view;
    }

    private View divider() {
        View view = new View(getContext());
        view.setBackgroundColor(getContext().getColor(R.color.border));
        return view;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refresh();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) refresh();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
