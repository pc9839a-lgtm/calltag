package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 날짜 필터와 핵심 지표만 보여주는 간결한 통계 화면. */
public final class CustomerStatsView extends LinearLayout {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private boolean internalRender;
    private boolean renderedCycle;
    private int selectedDays = 7;

    public CustomerStatsView(Context context) {
        super(context);
        init();
    }

    public CustomerStatsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomerStatsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    @Override
    public void removeAllViews() {
        if (internalRender) {
            super.removeAllViews();
            return;
        }
        super.removeAllViews();
        renderedCycle = false;
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (internalRender) {
            super.addView(child, params);
            return;
        }
        if (!renderedCycle) {
            renderedCycle = true;
            renderDashboard();
        }
    }

    private void renderDashboard() {
        internalRender = true;
        super.removeAllViews();

        CallTagDbHelper db = new CallTagDbHelper(getContext());
        PendingCallStore pendingCalls = new PendingCallStore(getContext());
        try {
            long start = rangeStart();
            List<Customer> customers = db.listCustomers(null);
            List<InteractionRecord> interactions = db.listRecentInteractions(5000);
            List<FollowUpTask> pendingTasks = db.listPendingTasks();

            Set<Long> contacted = new HashSet<>();
            int calls = 0;
            int incoming = 0;
            int outgoing = 0;
            int missed = 0;
            int rejected = 0;
            int completedTasks = 0;

            for (InteractionRecord row : interactions) {
                if (row.startedAt < start) continue;
                if (isCall(row.type)) {
                    calls++;
                    contacted.add(row.customerId);
                    if ("INCOMING_CALL".equals(row.type)) incoming++;
                    else if ("OUTGOING_CALL".equals(row.type)) outgoing++;
                    else if ("MISSED_CALL".equals(row.type)) missed++;
                    else if ("REJECTED_CALL".equals(row.type)) rejected++;
                } else if ("TASK_COMPLETE".equals(row.type)
                        || "TASK_AUTO_COMPLETE".equals(row.type)) {
                    completedTasks++;
                }
            }

            int newCustomers = 0;
            for (Customer customer : customers) {
                if (customer.firstContactAt >= start) newCustomers++;
            }

            addFilterRow();
            addKpiGrid(new Kpi[] {
                    new Kpi(String.valueOf(contacted.size()), "연락 고객", R.color.primary),
                    new Kpi(String.valueOf(calls), "통화", R.color.text_primary),
                    new Kpi(String.valueOf(newCustomers), "신규 고객", R.color.success),
                    new Kpi(String.valueOf(completedTasks), "완료한 일", R.color.warning)
            });

            super.addView(sectionTitle("통화 유형"), topMargin(24));
            LinearLayout callTypes = new LinearLayout(getContext());
            callTypes.setOrientation(HORIZONTAL);
            callTypes.setBackgroundResource(R.drawable.bg_card);
            callTypes.setPadding(dp(6), dp(12), dp(6), dp(12));
            callTypes.addView(compactMetric("수신", incoming, R.color.primary), weighted());
            callTypes.addView(compactMetric("발신", outgoing, R.color.success), weighted());
            callTypes.addView(compactMetric("부재중", missed, R.color.warning), weighted());
            callTypes.addView(compactMetric("거절", rejected, R.color.danger), weighted());
            super.addView(callTypes, topMargin(10));

            super.addView(sectionTitle("지금 처리할 일"), topMargin(24));
            LinearLayout attention = new LinearLayout(getContext());
            attention.setOrientation(VERTICAL);
            attention.setBackgroundResource(R.drawable.bg_card);
            attention.setPadding(dp(16), dp(6), dp(16), dp(6));
            attention.addView(metricRow("오늘 할 일", db.countDueTodayTasks() + "건", R.color.primary));
            attention.addView(divider(), matchHeight(1));
            attention.addView(metricRow("기한 지남", db.countOverdueTasks() + "건", R.color.danger));
            attention.addView(divider(), matchHeight(1));
            attention.addView(metricRow("예정된 할 일", pendingTasks.size() + "건", R.color.text_primary));
            attention.addView(divider(), matchHeight(1));
            attention.addView(metricRow("확인할 통화", pendingCalls.countPending() + "건", R.color.warning));
            super.addView(attention, topMargin(10));
        } finally {
            pendingCalls.close();
            db.close();
            internalRender = false;
        }
    }

    private void addFilterRow() {
        TextView label = sectionTitle("기간");
        super.addView(label, topMargin(2));

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        int[] values = {1, 7, 30, 0};
        String[] labels = {"오늘", "7일", "30일", "전체"};
        for (int i = 0; i < values.length; i++) {
            final int days = values[i];
            TextView chip = new TextView(getContext());
            chip.setText(labels[i]);
            chip.setTextSize(14f);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setIncludeFontPadding(false);
            chip.setTextColor(getContext().getColor(days == selectedDays
                    ? android.R.color.white : R.color.text_primary));
            chip.setBackgroundResource(days == selectedDays
                    ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                selectedDays = days;
                renderDashboard();
            });
            LayoutParams params = new LayoutParams(0, dp(46), 1f);
            if (i > 0) params.leftMargin = dp(7);
            row.addView(chip, params);
        }
        super.addView(row, topMargin(10));
    }

    private void addKpiGrid(Kpi[] values) {
        for (int i = 0; i < values.length; i += 2) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            row.addView(kpiCard(values[i]), new LayoutParams(0, dp(96), 1f));
            LayoutParams right = new LayoutParams(0, dp(96), 1f);
            right.leftMargin = dp(8);
            row.addView(kpiCard(values[i + 1]), right);
            super.addView(row, topMargin(i == 0 ? 16 : 8));
        }
    }

    private View kpiCard(Kpi value) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setBackgroundResource(R.drawable.bg_card);
        TextView number = title(value.number, 25f);
        number.setTextColor(getContext().getColor(value.color));
        card.addView(number, matchWrap());
        TextView label = body(value.label);
        label.setTextColor(getContext().getColor(R.color.text_secondary));
        card.addView(label, topMargin(5));
        return card;
    }

    private View compactMetric(String label, int value, int color) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(VERTICAL);
        box.setGravity(Gravity.CENTER);
        TextView number = title(String.valueOf(value), 20f);
        number.setTextColor(getContext().getColor(color));
        box.addView(number, matchWrap());
        TextView name = body(label);
        name.setTextSize(12f);
        box.addView(name, topMargin(4));
        return box;
    }

    private View metricRow(String label, String value, int color) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.addView(body(label), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView result = title(value, 16f);
        result.setTextColor(getContext().getColor(color));
        row.addView(result);
        return row;
    }

    private View divider() {
        View line = new View(getContext());
        line.setBackgroundColor(getContext().getColor(R.color.border));
        return line;
    }

    private TextView sectionTitle(String value) {
        return title(value, 17f);
    }

    private TextView title(String value, float size) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextColor(getContext().getColor(R.color.text_primary));
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView body(String value) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextColor(getContext().getColor(R.color.text_secondary));
        view.setTextSize(14f);
        view.setIncludeFontPadding(false);
        return view;
    }

    private boolean isCall(String type) {
        return "INCOMING_CALL".equals(type)
                || "OUTGOING_CALL".equals(type)
                || "MISSED_CALL".equals(type)
                || "REJECTED_CALL".equals(type);
    }

    private long rangeStart() {
        if (selectedDays <= 0) return 0L;
        long today = startOfDay(System.currentTimeMillis());
        return today - Math.max(0, selectedDays - 1) * DAY_MS;
    }

    private long startOfDay(long value) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(value);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private LayoutParams weighted() {
        return new LayoutParams(0, dp(72), 1f);
    }

    private LayoutParams matchWrap() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams matchHeight(int height) {
        return new LayoutParams(LayoutParams.MATCH_PARENT, dp(height));
    }

    private LayoutParams topMargin(int value) {
        LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Kpi {
        final String number;
        final String label;
        final int color;

        Kpi(String number, String label, int color) {
            this.number = number;
            this.label = label;
            this.color = color;
        }
    }
}
