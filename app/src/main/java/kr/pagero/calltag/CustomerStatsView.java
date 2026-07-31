package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CustomerStatsView extends LinearLayout {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private boolean internalRender;
    private boolean renderedCycle;

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
        TaskTypeStore taskTypes = new TaskTypeStore(getContext());
        PendingCallStore pendingCalls = new PendingCallStore(getContext());
        try {
            List<Customer> customers = db.listCustomers(null);
            List<FollowUpTask> pendingTasks = db.listPendingTasks();
            List<InteractionRecord> interactions = db.listRecentInteractions(1500);

            long todayStart = startOfDay(System.currentTimeMillis());
            long tomorrowStart = todayStart + DAY_MS;
            long sevenDayStart = todayStart - 6L * DAY_MS;
            long thirtyDayStart = todayStart - 29L * DAY_MS;

            Set<Long> contactedTodayIds = new HashSet<>();
            Set<Long> contactedSevenDayIds = new HashSet<>();
            int[] dailyCalls = new int[7];
            int incomingCalls = 0;
            int outgoingCalls = 0;
            int missedCalls = 0;
            int rejectedCalls = 0;

            for (InteractionRecord record : interactions) {
                if (!isCall(record.type)) continue;
                if (record.startedAt >= todayStart) contactedTodayIds.add(record.customerId);
                if (record.startedAt >= sevenDayStart) {
                    contactedSevenDayIds.add(record.customerId);
                    int dayIndex = (int) ((startOfDay(record.startedAt) - sevenDayStart) / DAY_MS);
                    if (dayIndex >= 0 && dayIndex < dailyCalls.length) dailyCalls[dayIndex]++;
                    if ("INCOMING_CALL".equals(record.type)) incomingCalls++;
                    else if ("OUTGOING_CALL".equals(record.type)) outgoingCalls++;
                    else if ("MISSED_CALL".equals(record.type)) missedCalls++;
                    else if ("REJECTED_CALL".equals(record.type)) rejectedCalls++;
                }
            }

            Set<Long> customersWithPendingTask = new HashSet<>();
            Map<String, Integer> pendingTypeCounts = new LinkedHashMap<>();
            for (FollowUpTask task : pendingTasks) {
                customersWithPendingTask.add(task.customerId);
                pendingTypeCounts.put(task.taskType,
                        pendingTypeCounts.getOrDefault(task.taskType, 0) + 1);
            }

            int noContact30 = 0;
            int noPendingTask = 0;
            int firstStageNoTask = 0;
            for (Customer customer : customers) {
                if (customer.lastContactAt < thirtyDayStart) noContact30++;
                if (!customersWithPendingTask.contains(customer.id)) {
                    noPendingTask++;
                    if (db.firstStage().equals(customer.relationStatus)) firstStageNoTask++;
                }
            }

            List<FollowUpTask> dueLast30 = db.listTasksBetween(thirtyDayStart, tomorrowStart);
            int completedLast30 = 0;
            for (FollowUpTask task : dueLast30) {
                if (task.isCompleted()) completedLast30++;
            }
            int completionRate = dueLast30.isEmpty()
                    ? 0 : Math.round(completedLast30 * 100f / dueLast30.size());

            addSectionHeader("업무 현황", "지금 바로 확인해야 할 숫자입니다.");
            addKpiGrid(new Kpi[] {
                    new Kpi(String.valueOf(customers.size()), "전체 고객", "등록 고객", R.color.text_primary),
                    new Kpi(String.valueOf(contactedTodayIds.size()), "오늘 연락", "실제 통화 고객", R.color.primary),
                    new Kpi(String.valueOf(db.countOverdueTasks()), "기한 초과", "처리되지 않은 할 일", R.color.danger),
                    new Kpi(String.valueOf(pendingCalls.countPending()), "확인 통화", "부재중·거절·미연결", R.color.warning)
            });

            addSectionHeader("관리 공백", "놓치기 쉬운 고객을 먼저 확인합니다.");
            LinearLayout attention = verticalCard();
            attention.addView(metricRow("30일 이상 미접촉", noContact30 + "명", R.color.danger), matchWrap());
            attention.addView(divider(), matchWrapHeight(1));
            attention.addView(metricRow("예정된 할 일이 없는 고객", noPendingTask + "명", R.color.warning), matchWrap());
            attention.addView(divider(), matchWrapHeight(1));
            attention.addView(metricRow("첫 상태지만 할 일이 없는 고객", firstStageNoTask + "명", R.color.primary), matchWrap());
            super.addView(attention, topMargin(10));

            addSectionHeader("최근 30일 처리율", "기한이 도래한 할 일 기준입니다.");
            LinearLayout completion = verticalCard();
            LinearLayout completionHeader = new LinearLayout(getContext());
            completionHeader.setGravity(Gravity.CENTER_VERTICAL);
            completionHeader.addView(title("완료한 할 일", 16f), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            completionHeader.addView(title(dueLast30.isEmpty()
                    ? "기록 없음" : completionRate + "%", 22f));
            completion.addView(completionHeader, matchWrap());
            completion.addView(body(completedLast30 + "건 완료 · 전체 " + dueLast30.size() + "건"), topMargin(6));
            completion.addView(progressBar(completionRate, getColor(R.color.primary)), topMargin(13));
            super.addView(completion, topMargin(10));

            addSectionHeader("최근 7일 고객 접촉", contactedSevenDayIds.size() + "명의 고객과 통화했습니다.");
            LinearLayout trend = verticalCard();
            int maxDaily = 1;
            for (int count : dailyCalls) maxDaily = Math.max(maxDaily, count);
            SimpleDateFormat dayFormat = new SimpleDateFormat("M/d E", Locale.KOREA);
            for (int i = 0; i < dailyCalls.length; i++) {
                long day = sevenDayStart + i * DAY_MS;
                trend.addView(barRow(dayFormat.format(day), dailyCalls[i], maxDaily,
                        getColor(R.color.primary)), i == 0 ? matchWrap() : topMargin(11));
            }
            super.addView(trend, topMargin(10));

            addSectionHeader("최근 7일 통화 유형", "연결·미연결 흐름을 구분합니다.");
            addKpiGrid(new Kpi[] {
                    new Kpi(String.valueOf(incomingCalls), "수신", "받은 전화", R.color.primary),
                    new Kpi(String.valueOf(outgoingCalls), "발신", "내가 건 전화", R.color.success),
                    new Kpi(String.valueOf(missedCalls), "부재중", "못 받은 전화", R.color.warning),
                    new Kpi(String.valueOf(rejectedCalls), "거절", "거절된 전화", R.color.danger)
            });

            addSectionHeader("고객 상태 분포", "상태별 고객 비중입니다.");
            LinearLayout stagesCard = verticalCard();
            int totalCustomers = Math.max(1, customers.size());
            List<StageOption> stages = db.listStages();
            Map<String, Integer> stageCounts = new HashMap<>();
            for (Customer customer : customers) {
                stageCounts.put(customer.relationStatus,
                        stageCounts.getOrDefault(customer.relationStatus, 0) + 1);
            }
            for (int i = 0; i < stages.size(); i++) {
                StageOption stage = stages.get(i);
                int count = stageCounts.getOrDefault(stage.name, 0);
                int percent = Math.round(count * 100f / totalCustomers);
                stagesCard.addView(distributionRow(stage.name, count, percent, parseColor(stage.color)),
                        i == 0 ? matchWrap() : topMargin(13));
            }
            super.addView(stagesCard, topMargin(10));

            addSectionHeader("예정된 할 일", "현재 남아 있는 할 일 종류입니다.");
            LinearLayout taskCard = verticalCard();
            List<TaskTypeOption> types = taskTypes.list();
            int maxType = 1;
            for (int count : pendingTypeCounts.values()) maxType = Math.max(maxType, count);
            boolean hasTypes = false;
            for (TaskTypeOption type : types) {
                int count = pendingTypeCounts.getOrDefault(type.code, 0);
                if (count == 0) continue;
                hasTypes = true;
                taskCard.addView(barRow(type.name, count, maxType, parseColor(type.color)),
                        taskCard.getChildCount() == 0 ? matchWrap() : topMargin(12));
            }
            if (!hasTypes) {
                TextView empty = body("예정된 할 일이 없습니다.");
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, dp(16), 0, dp(16));
                taskCard.addView(empty, matchWrap());
            }
            super.addView(taskCard, topMargin(10));
        } finally {
            pendingCalls.close();
            taskTypes.close();
            db.close();
            internalRender = false;
        }
    }

    private boolean isCall(String type) {
        return "INCOMING_CALL".equals(type)
                || "OUTGOING_CALL".equals(type)
                || "MISSED_CALL".equals(type)
                || "REJECTED_CALL".equals(type);
    }

    private void addSectionHeader(String title, String subtitle) {
        TextView titleView = title(title, 18f);
        LayoutParams titleParams = topMargin(getChildCount() == 0 ? 2 : 24);
        super.addView(titleView, titleParams);
        TextView subtitleView = body(subtitle);
        subtitleView.setTextSize(13f);
        subtitleView.setTextColor(getColor(R.color.text_muted));
        super.addView(subtitleView, topMargin(5));
    }

    private void addKpiGrid(Kpi[] items) {
        for (int i = 0; i < items.length; i += 2) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            row.addView(kpiCard(items[i]), new LayoutParams(0, dp(104), 1f));
            if (i + 1 < items.length) {
                LayoutParams rightParams = new LayoutParams(0, dp(104), 1f);
                rightParams.leftMargin = dp(8);
                row.addView(kpiCard(items[i + 1]), rightParams);
            }
            super.addView(row, topMargin(i == 0 ? 10 : 8));
        }
    }

    private View kpiCard(Kpi item) {
        LinearLayout card = verticalCard();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(15), dp(13), dp(15), dp(13));
        TextView number = title(item.number, 25f);
        number.setTextColor(getColor(item.color));
        card.addView(number, matchWrap());
        card.addView(title(item.label, 14f), topMargin(5));
        TextView note = body(item.note);
        note.setTextSize(12f);
        note.setTextColor(getColor(R.color.text_muted));
        card.addView(note, topMargin(3));
        return card;
    }

    private View metricRow(String label, String value, int color) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(13), 0, dp(13));
        row.addView(body(label), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView valueView = title(value, 16f);
        valueView.setTextColor(getColor(color));
        row.addView(valueView);
        return row;
    }

    private View distributionRow(String label, int count, int percent, int color) {
        LinearLayout wrap = new LinearLayout(getContext());
        wrap.setOrientation(VERTICAL);
        LinearLayout header = new LinearLayout(getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        View swatch = new View(getContext());
        swatch.setBackground(circle(color));
        header.addView(swatch, new LayoutParams(dp(10), dp(10)));
        TextView name = title(label, 14f);
        LayoutParams nameParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = dp(8);
        header.addView(name, nameParams);
        header.addView(title(count + "명 · " + percent + "%", 14f));
        wrap.addView(header, matchWrap());
        wrap.addView(progressBar(percent, color), topMargin(8));
        return wrap;
    }

    private View barRow(String label, int value, int max, int color) {
        LinearLayout wrap = new LinearLayout(getContext());
        wrap.setOrientation(VERTICAL);
        LinearLayout header = new LinearLayout(getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(body(label), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title(value + "건", 14f));
        wrap.addView(header, matchWrap());
        int percent = max <= 0 ? 0 : Math.round(value * 100f / max);
        wrap.addView(progressBar(percent, color), topMargin(7));
        return wrap;
    }

    private View progressBar(int percent, int color) {
        LinearLayout track = new LinearLayout(getContext());
        track.setOrientation(HORIZONTAL);
        track.setBackground(roundRect(getColor(R.color.surface_soft), 5));
        int safe = Math.max(0, Math.min(100, percent));
        if (safe > 0) {
            View fill = new View(getContext());
            fill.setBackground(roundRect(color, 5));
            track.addView(fill, new LayoutParams(0, dp(8), safe));
        }
        View rest = new View(getContext());
        track.addView(rest, new LayoutParams(0, dp(8), Math.max(1, 100 - safe)));
        return track;
    }

    private LinearLayout verticalCard() {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private View divider() {
        View divider = new View(getContext());
        divider.setBackgroundColor(getColor(R.color.border));
        return divider;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radiusDp));
        return shape;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        return shape;
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(14f);
        text.setIncludeFontPadding(false);
        return text;
    }

    private int getColor(int colorRes) {
        return getContext().getColor(colorRes);
    }

    private int parseColor(String value) {
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException ignored) {
            return getColor(R.color.primary);
        }
    }

    private long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private LayoutParams matchWrap() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams matchWrapHeight(int heightDp) {
        return new LayoutParams(LayoutParams.MATCH_PARENT, dp(heightDp));
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
        final String note;
        final int color;

        Kpi(String number, String label, String note, int color) {
            this.number = number;
            this.label = label;
            this.note = note;
            this.color = color;
        }
    }
}
