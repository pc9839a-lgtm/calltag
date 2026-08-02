package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 핵심 요약, 일별 추이, 통화 유형과 페이지로 성과를 기간별로 표시한다. */
public final class CustomerStatsView extends LinearLayout {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final int MAX_CUSTOM_DAYS = 365;

    private boolean internalRender;
    private boolean renderedCycle;
    private int selectedDays = 7;
    private long customStart;
    private long customEnd;

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
            long end = rangeEnd();
            int spanDays = rangeDays(start, end);
            List<Customer> customers = db.listCustomers(null);
            List<InteractionRecord> interactions = db.listRecentInteractions(5000);

            Set<Long> contacted = new HashSet<>();
            int calls = 0;
            int incoming = 0;
            int outgoing = 0;
            int missed = 0;
            int rejected = 0;
            int completedTasks = 0;

            int[] dailyCalls = spanDays >= 2 && spanDays <= 30 ? new int[spanDays] : null;
            int[] dailyPagero = dailyCalls == null ? null : new int[spanDays];
            String[] dailyLabels = dailyCalls == null ? null : dateLabels(start, spanDays);

            for (InteractionRecord row : interactions) {
                if (row.startedAt < start || row.startedAt > end) continue;
                if (isCall(row.type)) {
                    calls++;
                    contacted.add(row.customerId);
                    if ("INCOMING_CALL".equals(row.type)) incoming++;
                    else if ("OUTGOING_CALL".equals(row.type)) outgoing++;
                    else if ("MISSED_CALL".equals(row.type)) missed++;
                    else if ("REJECTED_CALL".equals(row.type)) rejected++;
                    if (dailyCalls != null) {
                        int index = dayIndex(row.startedAt, start, spanDays);
                        if (index >= 0) dailyCalls[index]++;
                    }
                } else if ("TASK_COMPLETE".equals(row.type)
                        || "TASK_AUTO_COMPLETE".equals(row.type)) {
                    completedTasks++;
                }
            }

            int newCustomers = 0;
            int pageroLeads = 0;
            int pageroContacted = 0;
            for (Customer customer : customers) {
                boolean createdInRange = customer.firstContactAt >= start
                        && customer.firstContactAt <= end;
                if (createdInRange) newCustomers++;
                if (createdInRange && CustomerSourceResolver.isPagero(customer)) {
                    pageroLeads++;
                    if (contacted.contains(customer.id)) pageroContacted++;
                    if (dailyPagero != null) {
                        int index = dayIndex(customer.firstContactAt, start, spanDays);
                        if (index >= 0) dailyPagero[index]++;
                    }
                }
            }

            int pageroUncontacted = Math.max(0, pageroLeads - pageroContacted);
            int pageroRate = pageroLeads == 0
                    ? 0 : Math.round(pageroContacted * 100f / pageroLeads);

            addFilterRow();
            addHeroCard(calls, contacted.size(), newCustomers, completedTasks);

            if (dailyCalls != null) {
                addSection("일별 추이");
                StatsTrendChartView chart = new StatsTrendChartView(getContext());
                chart.setData(dailyLabels, dailyCalls, dailyPagero);
                super.addView(chart, topMarginHeight(9, 205));
            }

            addSection("통화 유형");
            addGrid(new Metric[] {
                    callMetric("수신", incoming, calls),
                    callMetric("발신", outgoing, calls),
                    callMetric("부재중", missed, calls),
                    callMetric("거절", rejected, calls)
            }, 9);

            addSection("페이지로 유입");
            addGrid(new Metric[] {
                    metric("유입 고객", String.valueOf(pageroLeads), "명", true),
                    metric("연락률", String.valueOf(pageroRate), "%", true),
                    metric("연락 완료", String.valueOf(pageroContacted), "명", false),
                    metric("미연락", String.valueOf(pageroUncontacted), "명", false)
            }, 9);

            addSection("지금 처리할 일");
            addThreeGrid(new Metric[] {
                    metric("오늘 할 일", String.valueOf(db.countDueTodayTasks()), "건", true),
                    metric("기한 지남", String.valueOf(db.countOverdueTasks()), "건", false),
                    metric("확인할 통화", String.valueOf(pendingCalls.countPending()), "건", false)
            });
        } finally {
            pendingCalls.close();
            db.close();
            internalRender = false;
        }
    }

    private void addFilterRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        int[] values = {1, 7, 30, -1};
        String[] labels = {"오늘", "7일", "30일", "선택"};
        for (int i = 0; i < values.length; i++) {
            final int days = values[i];
            boolean selected = selectedDays == days;
            TextView chip = new TextView(getContext());
            chip.setText(labels[i]);
            chip.setTextSize(13f);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setIncludeFontPadding(false);
            chip.setTextColor(getContext().getColor(selected
                    ? android.R.color.white : R.color.text_primary));
            chip.setBackgroundResource(selected
                    ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                if (days == -1) showDateRangeDialog();
                else {
                    selectedDays = days;
                    customStart = 0L;
                    customEnd = 0L;
                    renderDashboard();
                }
            });
            LayoutParams params = new LayoutParams(0, dp(42), 1f);
            if (i > 0) params.leftMargin = dp(6);
            row.addView(chip, params);
        }
        super.addView(row, topMargin(2));

        TextView range = body(rangeLabel());
        range.setTextSize(12f);
        range.setGravity(Gravity.END);
        super.addView(range, topMargin(7));
    }

    private void showDateRangeDialog() {
        Activity activity = getContext() instanceof Activity ? (Activity) getContext() : null;
        if (activity == null) return;

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        if (selectedDays == -1 && customStart > 0L && customEnd > 0L) {
            start.setTimeInMillis(customStart);
            end.setTimeInMillis(customEnd);
        } else {
            start.add(Calendar.DAY_OF_MONTH, -6);
        }
        setStartOfDay(start);
        setEndOfDay(end);

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(VERTICAL);
        panel.setPadding(dp(20), dp(6), dp(20), dp(2));

        TextView guide = body("시작일과 종료일을 확인한 뒤 적용하세요. 최대 365일까지 조회됩니다.");
        guide.setLineSpacing(0f, 1.15f);
        panel.addView(guide, matchWrap());

        TextView startButton = dateRow("시작일", start.getTimeInMillis());
        TextView endButton = dateRow("종료일", end.getTimeInMillis());
        panel.addView(startButton, topMargin(14));
        panel.addView(endButton, topMargin(8));

        startButton.setOnClickListener(v -> openDatePicker(activity, start, selected -> {
            start.setTimeInMillis(selected);
            setStartOfDay(start);
            startButton.setText(dateRowText("시작일", start.getTimeInMillis()));
        }));
        endButton.setOnClickListener(v -> openDatePicker(activity, end, selected -> {
            end.setTimeInMillis(selected);
            setEndOfDay(end);
            endButton.setText(dateRowText("종료일", end.getTimeInMillis()));
        }));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("조회 기간 선택")
                .setView(panel)
                .setNegativeButton("취소", null)
                .setPositiveButton("적용", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (end.before(start)) {
                        showRangeWarning("종료일은 시작일보다 빠를 수 없습니다.");
                        return;
                    }
                    int days = rangeDays(start.getTimeInMillis(), end.getTimeInMillis());
                    if (days > MAX_CUSTOM_DAYS) {
                        showRangeWarning("직접 선택은 최대 " + MAX_CUSTOM_DAYS
                                + "일까지 조회할 수 있습니다. 기간을 줄여주세요.");
                        return;
                    }
                    customStart = start.getTimeInMillis();
                    customEnd = end.getTimeInMillis();
                    selectedDays = -1;
                    dialog.dismiss();
                    renderDashboard();
                }));
        dialog.show();
    }

    private TextView dateRow(String label, long time) {
        TextView row = title(dateRowText(label, time), 15f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(15), 0, dp(15), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setMinHeight(dp(52));
        return row;
    }

    private String dateRowText(String label, long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREA);
        return label + "    " + format.format(time) + "  ›";
    }

    private void openDatePicker(Activity activity, Calendar initial, DateSelected listener) {
        DatePickerDialog picker = new DatePickerDialog(activity, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            listener.onSelected(selected.getTimeInMillis());
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void showRangeWarning(String message) {
        new AlertDialog.Builder(getContext())
                .setTitle("조회 기간 확인")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }

    private void addHeroCard(int calls, int contacted, int newCustomers, int completed) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(17), dp(15), dp(17), dp(15));
        card.setBackgroundResource(R.drawable.bg_card);

        TextView label = body("선택 기간 전체 통화");
        label.setTextSize(12.5f);
        card.addView(label, matchWrap());

        LinearLayout value = new LinearLayout(getContext());
        value.setGravity(Gravity.BOTTOM);
        TextView number = title(String.valueOf(calls), 30f);
        number.setTextColor(getContext().getColor(R.color.primary));
        value.addView(number);
        TextView suffix = body("건");
        suffix.setTextSize(13f);
        LayoutParams suffixParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        suffixParams.leftMargin = dp(4);
        suffixParams.bottomMargin = dp(4);
        value.addView(suffix, suffixParams);
        card.addView(value, topMargin(5));

        TextView summary = body("연락 고객 " + contacted + "명   ·   신규 "
                + newCustomers + "명   ·   완료 " + completed + "건");
        summary.setTextColor(getContext().getColor(R.color.text_primary));
        summary.setTextSize(13f);
        card.addView(summary, topMargin(8));
        super.addView(card, topMargin(14));
    }

    private void addSection(String label) {
        super.addView(title(label, 17f), topMargin(21));
    }

    private void addGrid(Metric[] values, int firstTopMargin) {
        for (int i = 0; i < values.length; i += 2) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            row.addView(metricCard(values[i]), new LayoutParams(0, dp(82), 1f));
            LayoutParams right = new LayoutParams(0, dp(82), 1f);
            right.leftMargin = dp(8);
            row.addView(metricCard(values[i + 1]), right);
            super.addView(row, topMargin(i == 0 ? firstTopMargin : 8));
        }
    }

    private void addThreeGrid(Metric[] values) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        for (int i = 0; i < values.length; i++) {
            LayoutParams params = new LayoutParams(0, dp(78), 1f);
            if (i > 0) params.leftMargin = dp(7);
            row.addView(compactMetricCard(values[i]), params);
        }
        super.addView(row, topMargin(9));
    }

    private View metricCard(Metric metric) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackgroundResource(R.drawable.bg_card);
        card.addView(valueRow(metric, 23f), matchWrap());
        TextView label = body(metric.label);
        label.setTextSize(12.5f);
        card.addView(label, topMargin(4));
        return card;
    }

    private View compactMetricCard(Metric metric) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(7), dp(8), dp(7), dp(8));
        card.setBackgroundResource(R.drawable.bg_card);
        card.addView(valueRow(metric, 20f), matchWrap());
        TextView label = body(metric.label);
        label.setTextSize(11f);
        label.setGravity(Gravity.CENTER);
        card.addView(label, topMargin(4));
        return card;
    }

    private View valueRow(Metric metric, float size) {
        LinearLayout valueRow = new LinearLayout(getContext());
        valueRow.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        TextView number = title(metric.value, size);
        number.setTextColor(getContext().getColor(metric.primary
                ? R.color.primary : R.color.text_primary));
        valueRow.addView(number);
        TextView suffix = body(metric.suffix);
        suffix.setTextSize(11f);
        LayoutParams suffixParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        suffixParams.leftMargin = dp(3);
        suffixParams.bottomMargin = dp(2);
        valueRow.addView(suffix, suffixParams);
        return valueRow;
    }

    private Metric callMetric(String label, int count, int total) {
        int percent = total == 0 ? 0 : Math.round(count * 100f / total);
        return metric(label + " · " + percent + "%", String.valueOf(count), "건", false);
    }

    private Metric metric(String label, String value, String suffix, boolean primary) {
        return new Metric(label, value, suffix, primary);
    }

    private String[] dateLabels(long start, int days) {
        String[] labels = new String[days];
        SimpleDateFormat format = new SimpleDateFormat("M/d", Locale.KOREA);
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(start);
        for (int i = 0; i < days; i++) {
            labels[i] = format.format(date.getTime());
            date.add(Calendar.DAY_OF_MONTH, 1);
        }
        return labels;
    }

    private int dayIndex(long time, long start, int days) {
        int index = (int) ((startOfDay(time) - startOfDay(start)) / DAY_MS);
        return index >= 0 && index < days ? index : -1;
    }

    private int rangeDays(long start, long end) {
        return (int) (((startOfDay(end) - startOfDay(start)) / DAY_MS) + 1L);
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
        if (selectedDays == -1 && customStart > 0L) return customStart;
        long today = startOfDay(System.currentTimeMillis());
        return today - Math.max(0, selectedDays - 1) * DAY_MS;
    }

    private long rangeEnd() {
        if (selectedDays == -1 && customEnd > 0L) return customEnd;
        Calendar end = Calendar.getInstance();
        setEndOfDay(end);
        return end.getTimeInMillis();
    }

    private String rangeLabel() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
        return format.format(rangeStart()) + " – " + format.format(rangeEnd());
    }

    private long startOfDay(long value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value);
        setStartOfDay(calendar);
        return calendar.getTimeInMillis();
    }

    private void setStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void setEndOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
    }

    private LayoutParams matchWrap() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams topMargin(int value) {
        LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private LayoutParams topMarginHeight(int top, int height) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(top);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface DateSelected {
        void onSelected(long time);
    }

    private static final class Metric {
        final String label;
        final String value;
        final String suffix;
        final boolean primary;

        Metric(String label, String value, String suffix, boolean primary) {
            this.label = label;
            this.value = value;
            this.suffix = suffix;
            this.primary = primary;
        }
    }
}
