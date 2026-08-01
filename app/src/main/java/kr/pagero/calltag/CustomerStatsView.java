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

/** 기간별 핵심 지표를 2×2 카드 규격으로 표시한다. */
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
                if (row.startedAt < start || row.startedAt > end) continue;
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
            int pageroLeads = 0;
            int pageroContacted = 0;
            for (Customer customer : customers) {
                boolean createdInRange = customer.firstContactAt >= start
                        && customer.firstContactAt <= end;
                if (createdInRange) newCustomers++;
                if (createdInRange && CustomerSourceResolver.isPagero(customer)) {
                    pageroLeads++;
                    if (contacted.contains(customer.id)) pageroContacted++;
                }
            }

            int pageroUncontacted = Math.max(0, pageroLeads - pageroContacted);
            int pageroRate = pageroLeads == 0
                    ? 0 : Math.round(pageroContacted * 100f / pageroLeads);

            addFilterRow();
            addGrid(new Metric[] {
                    metric("연락 고객", String.valueOf(contacted.size()), "명", false),
                    metric("전체 통화", String.valueOf(calls), "건", true),
                    metric("신규 고객", String.valueOf(newCustomers), "명", false),
                    metric("완료한 일", String.valueOf(completedTasks), "건", false)
            }, 14);

            addSection("통화 유형");
            addGrid(new Metric[] {
                    callMetric("수신", incoming, calls),
                    callMetric("발신", outgoing, calls),
                    callMetric("부재중", missed, calls),
                    callMetric("거절", rejected, calls)
            }, 9);

            addSection("페이지로 유입");
            addGrid(new Metric[] {
                    metric("유입 고객", String.valueOf(pageroLeads), "명", false),
                    metric("연락 완료", String.valueOf(pageroContacted), "명", true),
                    metric("미연락", String.valueOf(pageroUncontacted), "명", false),
                    metric("연락률", String.valueOf(pageroRate), "%", true)
            }, 9);

            addSection("지금 처리할 일");
            addGrid(new Metric[] {
                    metric("오늘 할 일", String.valueOf(db.countDueTodayTasks()), "건", true),
                    metric("기한 지남", String.valueOf(db.countOverdueTasks()), "건", false),
                    metric("예정된 할 일", String.valueOf(pendingTasks.size()), "건", false),
                    metric("확인할 통화", String.valueOf(pendingCalls.countPending()), "건", false)
            }, 9);
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
                if (days == -1) showCustomDatePicker();
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

    private void showCustomDatePicker() {
        Activity activity = getContext() instanceof Activity ? (Activity) getContext() : null;
        if (activity == null) return;
        Calendar initial = Calendar.getInstance();
        initial.add(Calendar.DAY_OF_MONTH, -6);
        new DatePickerDialog(activity, (view, year, month, day) -> {
            Calendar start = Calendar.getInstance();
            start.set(year, month, day, 0, 0, 0);
            start.set(Calendar.MILLISECOND, 0);

            Calendar endInitial = Calendar.getInstance();
            new DatePickerDialog(activity, (endView, endYear, endMonth, endDay) -> {
                Calendar end = Calendar.getInstance();
                end.set(endYear, endMonth, endDay, 23, 59, 59);
                end.set(Calendar.MILLISECOND, 999);
                if (end.before(start)) {
                    showRangeWarning("종료일은 시작일보다 빠를 수 없습니다.");
                    return;
                }
                long days = ((startOfDay(end.getTimeInMillis())
                        - startOfDay(start.getTimeInMillis())) / DAY_MS) + 1L;
                if (days > MAX_CUSTOM_DAYS) {
                    showRangeWarning("직접 선택은 최대 " + MAX_CUSTOM_DAYS
                            + "일까지 조회할 수 있습니다. 기간을 줄여주세요.");
                    return;
                }
                customStart = start.getTimeInMillis();
                customEnd = end.getTimeInMillis();
                selectedDays = -1;
                renderDashboard();
            }, endInitial.get(Calendar.YEAR), endInitial.get(Calendar.MONTH),
                    endInitial.get(Calendar.DAY_OF_MONTH)).show();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showRangeWarning(String message) {
        new AlertDialog.Builder(getContext())
                .setTitle("조회 기간 확인")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
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

    private View metricCard(Metric metric) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout valueRow = new LinearLayout(getContext());
        valueRow.setGravity(Gravity.BOTTOM);
        TextView number = title(metric.value, 23f);
        number.setTextColor(getContext().getColor(metric.primary
                ? R.color.primary : R.color.text_primary));
        valueRow.addView(number);
        TextView suffix = body(metric.suffix);
        suffix.setTextSize(12f);
        LayoutParams suffixParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        suffixParams.leftMargin = dp(3);
        suffixParams.bottomMargin = dp(2);
        valueRow.addView(suffix, suffixParams);
        card.addView(valueRow, matchWrap());

        TextView label = body(metric.label);
        label.setTextSize(12.5f);
        card.addView(label, topMargin(4));
        return card;
    }

    private Metric callMetric(String label, int count, int total) {
        int percent = total == 0 ? 0 : Math.round(count * 100f / total);
        return metric(label + " · " + percent + "%", String.valueOf(count), "건", false);
    }

    private Metric metric(String label, String value, String suffix, boolean primary) {
        return new Metric(label, value, suffix, primary);
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
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        return end.getTimeInMillis();
    }

    private String rangeLabel() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
        return format.format(rangeStart()) + " – " + format.format(rangeEnd());
    }

    private long startOfDay(long value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private LayoutParams matchWrap() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams topMargin(int value) {
        LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
