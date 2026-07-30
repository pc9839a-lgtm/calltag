package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQUEST_MONITOR_PERMISSIONS = 1201;

    private CallTagDbHelper db;
    private View sectionToday;
    private View sectionCustomers;
    private View sectionConsultations;
    private View sectionMore;
    private NavItemTextView navToday;
    private NavItemTextView navCustomers;
    private NavItemTextView navConsultations;
    private NavItemTextView navMore;
    private TextView todayDueCount;
    private TextView overdueCount;
    private TextView todayNewCount;
    private TextView todayEmpty;
    private TextView monitorStateText;
    private Switch enableMonitorButton;
    private LinearLayout todayTaskList;
    private LinearLayout customerList;
    private LinearLayout consultationSummary;
    private LinearLayout moreMenuList;
    private TextView customerFilterButton;
    private String activeCustomerFilter;
    private final Calendar visibleCalendarMonth = Calendar.getInstance();
    private final Calendar selectedCalendarDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new CallTagDbHelper(this);
        normalizeCalendarState();
        bindViews();
        bindActions();
        selectSection(sectionToday, navToday);
        if (hasMonitorPermissions() && SettingsStore.isMonitorEnabled(this)) startMonitorService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    private void normalizeCalendarState() {
        visibleCalendarMonth.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(visibleCalendarMonth);
        clearTime(selectedCalendarDate);
    }

    private void bindViews() {
        sectionToday = findViewById(R.id.sectionToday);
        sectionCustomers = findViewById(R.id.sectionCustomers);
        sectionConsultations = findViewById(R.id.sectionConsultations);
        sectionMore = findViewById(R.id.sectionMore);
        navToday = findViewById(R.id.navToday);
        navCustomers = findViewById(R.id.navCustomers);
        navConsultations = findViewById(R.id.navConsultations);
        navMore = findViewById(R.id.navMore);
        todayDueCount = findViewById(R.id.todayDueCount);
        overdueCount = findViewById(R.id.overdueCount);
        todayNewCount = findViewById(R.id.todayNewCount);
        todayEmpty = findViewById(R.id.todayEmpty);
        monitorStateText = findViewById(R.id.monitorStateText);
        enableMonitorButton = findViewById(R.id.enableMonitorButton);
        todayTaskList = findViewById(R.id.todayTaskList);
        customerList = findViewById(R.id.customerList);
        consultationSummary = findViewById(R.id.consultationSummary);
        moreMenuList = findViewById(R.id.moreMenuList);
        customerFilterButton = findViewById(R.id.customerFilterButton);
    }

    private void bindActions() {
        navToday.setOnClickListener(v -> selectSection(sectionToday, navToday));
        navCustomers.setOnClickListener(v -> selectSection(sectionCustomers, navCustomers));
        navConsultations.setOnClickListener(v -> selectSection(sectionConsultations, navConsultations));
        navMore.setOnClickListener(v -> selectSection(sectionMore, navMore));
        enableMonitorButton.setOnClickListener(v -> toggleMonitor());
        findViewById(R.id.quickAddCustomer).setOnClickListener(v -> showAddCustomerDialog());
        findViewById(R.id.quickCustomers).setOnClickListener(v -> selectSection(sectionCustomers, navCustomers));
        findViewById(R.id.quickConsultations).setOnClickListener(v -> selectSection(sectionConsultations, navConsultations));
        findViewById(R.id.addCustomerButton).setOnClickListener(v -> showAddCustomerDialog());
        customerFilterButton.setOnClickListener(v -> showCustomerFilterDialog());
        findViewById(R.id.stageSettingsButton).setOnClickListener(v -> openStageSettings());
    }

    private void selectSection(View section, NavItemTextView nav) {
        sectionToday.setVisibility(section == sectionToday ? View.VISIBLE : View.GONE);
        sectionCustomers.setVisibility(section == sectionCustomers ? View.VISIBLE : View.GONE);
        sectionConsultations.setVisibility(section == sectionConsultations ? View.VISIBLE : View.GONE);
        sectionMore.setVisibility(section == sectionMore ? View.VISIBLE : View.GONE);
        int active = getColor(R.color.primary);
        int inactive = getColor(R.color.nav_inactive);
        navToday.setTextColor(nav == navToday ? active : inactive);
        navCustomers.setTextColor(nav == navCustomers ? active : inactive);
        navConsultations.setTextColor(nav == navConsultations ? active : inactive);
        navMore.setTextColor(nav == navMore ? active : inactive);
        refreshAll();
    }

    private void refreshAll() {
        todayDueCount.setText(db.countDueTodayTasks() + "\n오늘 연락");
        overdueCount.setText(db.countOverdueTasks() + "\n기한 지남");
        todayNewCount.setText(db.countCustomersByStatus(db.firstStage()) + "\n첫 상태");
        renderMonitorState();
        renderTasks();
        renderCustomers();
        renderConsultations();
        renderMoreMenu();
    }

    private void toggleMonitor() {
        if (!hasMonitorPermissions()) {
            requestMonitorPermissions();
            enableMonitorButton.setChecked(false);
            return;
        }
        if (SettingsStore.isMonitorEnabled(this)) stopMonitorService();
        else startMonitorService();
        renderMonitorState();
    }

    private void requestMonitorPermissions() {
        List<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.READ_CALL_LOG);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (missing.isEmpty()) {
            startMonitorService();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("권한 허용")
                .setNegativeButton("취소", null)
                .setPositiveButton("허용", (dialog, which) -> requestPermissions(
                        missing.toArray(new String[0]), REQUEST_MONITOR_PERMISSIONS))
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_MONITOR_PERMISSIONS) return;
        if (hasMonitorPermissions()) {
            startMonitorService();
            Toast.makeText(this, "통화 감지를 켰습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
        renderMonitorState();
    }

    private boolean hasMonitorPermissions() {
        boolean granted = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted = granted
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return granted;
    }

    private void startMonitorService() {
        SettingsStore.setMonitorEnabled(this, true);
        Intent intent = new Intent(this, CallMonitorService.class).setAction(CallMonitorService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
            else startService(intent);
        } catch (RuntimeException e) {
            SettingsStore.setMonitorEnabled(this, false);
            Toast.makeText(this, "시작하지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMonitorService() {
        SettingsStore.setMonitorEnabled(this, false);
        stopService(new Intent(this, CallMonitorService.class));
    }

    private void renderMonitorState() {
        boolean enabled = hasMonitorPermissions() && SettingsStore.isMonitorEnabled(this);
        monitorStateText.setText(enabled ? "통화 감지가 작동 중이에요" : "통화 감지를 켜주세요");
        enableMonitorButton.setChecked(enabled);
    }

    private void renderTasks() {
        todayTaskList.removeAllViews();
        List<FollowUpTask> tasks = db.listPendingTasks();
        todayEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        for (FollowUpTask task : tasks) {
            LinearLayout card = verticalCard();
            card.setOnClickListener(v -> openCustomer(task.customerId));
            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(titleText(task.customerName, 17f), new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView due = mutedText(formatter.format(new Date(task.dueAt)));
            if (task.isOverdue()) due.setTextColor(getColor(R.color.danger));
            header.addView(due);
            card.addView(header, matchWrap());
            card.addView(bodyText(task.title), topMargin(7));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button call = smallButton("전화", false);
            call.setOnClickListener(v -> dial(task.phone));
            actions.addView(call, new LinearLayout.LayoutParams(0, dp(44), 1f));
            Button done = smallButton("완료", true);
            done.setOnClickListener(v -> completeCalendarTask(task));
            LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
            doneParams.leftMargin = dp(9);
            actions.addView(done, doneParams);
            card.addView(actions, topMargin(14));

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            todayTaskList.addView(card, params);
        }
    }

    private void showCustomerFilterDialog() {
        List<StageOption> stages = db.listStages();
        String[] labels = new String[stages.size() + 1];
        labels[0] = "전체 상태";
        int selected = 0;
        for (int i = 0; i < stages.size(); i++) {
            labels[i + 1] = stages.get(i).name;
            if (stages.get(i).name.equals(activeCustomerFilter)) selected = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle("고객 상태 필터")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    activeCustomerFilter = which == 0 ? null : stages.get(which - 1).name;
                    dialog.dismiss();
                    renderCustomers();
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void renderCustomers() {
        customerList.removeAllViews();
        customerFilterButton.setText((activeCustomerFilter == null ? "전체 상태" : activeCustomerFilter) + "  ▾");
        if (activeCustomerFilter == null) {
            customerFilterButton.setBackgroundResource(R.drawable.bg_secondary_button);
            customerFilterButton.setTextColor(getColor(R.color.text_primary));
        } else {
            String filterColor = db.stageColor(activeCustomerFilter);
            customerFilterButton.setBackground(stageTagBackground(filterColor));
            customerFilterButton.setTextColor(contrastTextColor(parseColor(filterColor)));
        }

        List<Customer> customers = db.listCustomers(activeCustomerFilter);
        if (customers.isEmpty()) {
            TextView empty = bodyText("해당 상태의 고객이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(22), dp(24), dp(22));
            empty.setBackgroundResource(R.drawable.bg_card);
            customerList.addView(empty, matchWrap());
            return;
        }

        DateFormat formatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
        for (Customer customer : customers) {
            String colorHex = db.stageColor(customer.relationStatus);
            LinearLayout card = verticalCard();
            card.setClickable(false);
            card.setFocusable(false);
            card.setBackground(customerCardBackground(colorHex));

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(titleText(customer.displayName, 17f), new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button stage = smallButton(customer.relationStatus + "  ▾", false);
            stage.setTextSize(12f);
            stage.setMinWidth(0);
            stage.setPadding(dp(12), 0, dp(12), 0);
            stage.setBackground(stageTagBackground(colorHex));
            stage.setTextColor(contrastTextColor(parseColor(colorHex)));
            stage.setOnClickListener(v -> showStagePicker(customer));
            header.addView(stage, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(40)));
            card.addView(header, matchWrap());

            card.addView(bodyText(customer.primaryPhone), topMargin(10));
            card.addView(mutedText("최근 연락  " + formatter.format(new Date(customer.lastContactAt))), topMargin(5));

            Button open = smallButton("고객 상세 보기", false);
            open.setOnClickListener(v -> openCustomer(customer.id));
            card.addView(open, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(44)) {{ topMargin = dp(12); }});

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            customerList.addView(card, params);
        }
    }

    private void showStagePicker(Customer customer) {
        List<StageOption> stages = db.listStages();
        String[] labels = new String[stages.size()];
        int selected = 0;
        for (int i = 0; i < stages.size(); i++) {
            labels[i] = stages.get(i).name;
            if (labels[i].equals(customer.relationStatus)) selected = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(customer.displayName + " 상태 변경")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    String oldStage = customer.relationStatus;
                    String newStage = stages.get(which).name;
                    if (!oldStage.equals(newStage)) {
                        db.updateCustomerStage(customer.id, newStage);
                        long now = System.currentTimeMillis();
                        db.insertInteraction(customer.id, "STATUS_CHANGE", now, now, 0L,
                                "STATUS_" + newStage, oldStage + " → " + newStage);
                        Toast.makeText(this, newStage + "으로 변경했습니다.", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                    refreshAll();
                })
                .setNeutralButton("상태·색상 편집", (dialog, which) -> openStageSettings())
                .setNegativeButton("닫기", null)
                .show();
    }

    private void openStageSettings() {
        startActivity(new Intent(this, StageSettingsActivity.class));
    }

    private void renderConsultations() {
        consultationSummary.removeAllViews();

        Calendar monthStart = (Calendar) visibleCalendarMonth.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(monthStart);
        Calendar monthEnd = (Calendar) monthStart.clone();
        monthEnd.add(Calendar.MONTH, 1);
        List<FollowUpTask> monthTasks = db.listTasksBetween(
                monthStart.getTimeInMillis(), monthEnd.getTimeInMillis());

        consultationSummary.addView(buildMonthHeader(), matchWrap());
        consultationSummary.addView(buildWeekdayHeader(), topMargin(12));
        consultationSummary.addView(buildCalendarGrid(monthStart, monthTasks), topMargin(5));
        consultationSummary.addView(buildSelectedDateHeader(), topMargin(24));
        renderSelectedDateTasks(monthTasks);
    }

    private View buildMonthHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView prev = calendarArrow("‹");
        prev.setOnClickListener(v -> changeCalendarMonth(-1));
        row.addView(prev, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = titleText(new SimpleDateFormat("yyyy년 M월", Locale.KOREA)
                .format(visibleCalendarMonth.getTime()), 20f);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView next = calendarArrow("›");
        next.setOnClickListener(v -> changeCalendarMonth(1));
        row.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return row;
    }

    private TextView calendarArrow(String label) {
        TextView view = titleText(label, 30f);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(R.drawable.bg_secondary_button);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private View buildWeekdayHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        for (int i = 0; i < labels.length; i++) {
            TextView day = mutedText(labels[i]);
            day.setGravity(Gravity.CENTER);
            if (i == 0) day.setTextColor(getColor(R.color.danger));
            if (i == 6) day.setTextColor(getColor(R.color.primary));
            row.addView(day, new LinearLayout.LayoutParams(0, dp(34), 1f));
        }
        return row;
    }

    private View buildCalendarGrid(Calendar monthStart, List<FollowUpTask> monthTasks) {
        LinearLayout calendar = new LinearLayout(this);
        calendar.setOrientation(LinearLayout.VERTICAL);
        calendar.setPadding(dp(4), dp(6), dp(4), dp(6));
        calendar.setBackgroundResource(R.drawable.bg_card);

        int offset = monthStart.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        int maxDay = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();
        clearTime(today);

        for (int week = 0; week < 6; week++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int column = 0; column < 7; column++) {
                int index = week * 7 + column;
                int dayNumber = index - offset + 1;

                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.TOP);
                cell.setPadding(dp(5), dp(5), dp(5), dp(4));
                cell.setBackground(calendarCellBackground(false, false));

                if (dayNumber >= 1 && dayNumber <= maxDay) {
                    Calendar date = (Calendar) monthStart.clone();
                    date.set(Calendar.DAY_OF_MONTH, dayNumber);
                    clearTime(date);

                    boolean selected = sameDay(date, selectedCalendarDate);
                    boolean isToday = sameDay(date, today);
                    cell.setBackground(calendarCellBackground(selected, isToday));

                    TextView day = new TextView(this);
                    day.setText(String.valueOf(dayNumber));
                    day.setTextSize(12f);
                    day.setTypeface(Typeface.DEFAULT, selected || isToday ? Typeface.BOLD : Typeface.NORMAL);
                    day.setIncludeFontPadding(false);
                    day.setTextColor(getColor(column == 0 ? R.color.danger
                            : column == 6 ? R.color.primary : R.color.text_primary));
                    cell.addView(day, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(18)));

                    List<FollowUpTask> dateTasks = tasksForDate(monthTasks, date);
                    int maxVisible = Math.min(2, dateTasks.size());
                    for (int taskIndex = 0; taskIndex < maxVisible; taskIndex++) {
                        FollowUpTask task = dateTasks.get(taskIndex);
                        TextView pill = calendarTaskPill(task,
                                taskIndex == 1 && dateTasks.size() > 2
                                        ? "+" + (dateTasks.size() - 1) + "개 일정"
                                        : task.customerName + " " + task.title);
                        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, dp(18));
                        pillParams.topMargin = dp(3);
                        cell.addView(pill, pillParams);
                        if (taskIndex == 1 && dateTasks.size() > 2) break;
                    }

                    cell.setOnClickListener(v -> {
                        selectedCalendarDate.setTimeInMillis(date.getTimeInMillis());
                        renderConsultations();
                    });
                    cell.setClickable(true);
                    cell.setFocusable(true);
                }

                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, dp(82), 1f);
                cellParams.setMargins(dp(2), dp(2), dp(2), dp(2));
                row.addView(cell, cellParams);
            }
            calendar.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        return calendar;
    }

    private List<FollowUpTask> tasksForDate(List<FollowUpTask> tasks, Calendar date) {
        List<FollowUpTask> rows = new ArrayList<>();
        for (FollowUpTask task : tasks) {
            if (sameDay(task.dueAt, date)) rows.add(task);
        }
        return rows;
    }

    private TextView calendarTaskPill(FollowUpTask task, String label) {
        Customer customer = db.findCustomerById(task.customerId);
        String colorHex = customer == null ? "#4389FF" : db.stageColor(customer.relationStatus);
        int color = task.isCompleted() ? getColor(R.color.text_muted) : parseColor(colorHex);

        TextView pill = new TextView(this);
        pill.setText(label);
        pill.setSingleLine(true);
        pill.setEllipsize(TextUtils.TruncateAt.END);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setTextSize(8f);
        pill.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pill.setTextColor(color);
        pill.setPadding(dp(4), 0, dp(4), 0);
        pill.setBackground(calendarPillBackground(color));
        return pill;
    }

    private View buildSelectedDateHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = titleText(new SimpleDateFormat("M월 d일 EEEE", Locale.KOREA)
                .format(selectedCalendarDate.getTime()), 18f);
        row.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button add = smallButton("+ 일정 추가", false);
        add.setTextColor(getColor(R.color.primary));
        add.setOnClickListener(v -> showAddScheduleFlow());
        row.addView(add, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)));
        return row;
    }

    private void renderSelectedDateTasks(List<FollowUpTask> monthTasks) {
        List<FollowUpTask> selectedTasks = new ArrayList<>();
        for (FollowUpTask task : monthTasks) {
            if (sameDay(task.dueAt, selectedCalendarDate)) selectedTasks.add(task);
        }

        if (selectedTasks.isEmpty()) {
            TextView empty = bodyText("등록된 일정이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(24), dp(20), dp(24));
            empty.setBackgroundResource(R.drawable.bg_card);
            consultationSummary.addView(empty, topMargin(12));
            return;
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm", Locale.KOREA);
        for (FollowUpTask task : selectedTasks) {
            LinearLayout card = verticalCard();
            card.setOnClickListener(v -> openCustomer(task.customerId));

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView customerName = titleText(task.customerName, 16f);
            header.addView(customerName, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            Customer customer = db.findCustomerById(task.customerId);
            String stageName = customer == null ? "" : customer.relationStatus;
            String stageColor = customer == null ? "#4389FF" : db.stageColor(stageName);
            TextView status = titleText(stageName, 12f);
            status.setTextColor(contrastTextColor(parseColor(stageColor)));
            status.setPadding(dp(10), dp(5), dp(10), dp(5));
            status.setBackground(stageTagBackground(stageColor));
            header.addView(status);
            card.addView(header, matchWrap());

            TextView taskTitle = bodyText((task.isCompleted() ? "완료 · " : "") + task.title);
            if (task.isCompleted()) taskTitle.setTextColor(getColor(R.color.text_muted));
            card.addView(taskTitle, topMargin(9));
            card.addView(mutedText(timeFormat.format(new Date(task.dueAt))), topMargin(5));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button change = smallButton(task.isCompleted() ? "다시 열기" : "변경", false);
            change.setOnClickListener(v -> {
                if (task.isCompleted()) {
                    db.reopenTask(task.id);
                    refreshAll();
                } else {
                    showTaskOptions(task);
                }
            });
            actions.addView(change, new LinearLayout.LayoutParams(0, dp(42), 1f));

            Button action = smallButton(task.isCompleted() ? "삭제" : "완료", !task.isCompleted());
            action.setOnClickListener(v -> {
                if (task.isCompleted()) confirmDeleteTask(task);
                else completeCalendarTask(task);
            });
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
            actionParams.leftMargin = dp(8);
            actions.addView(action, actionParams);
            card.addView(actions, topMargin(12));

            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.topMargin = dp(10);
            consultationSummary.addView(card, cardParams);
        }
    }

    private void changeCalendarMonth(int delta) {
        visibleCalendarMonth.add(Calendar.MONTH, delta);
        visibleCalendarMonth.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(visibleCalendarMonth);
        selectedCalendarDate.setTimeInMillis(visibleCalendarMonth.getTimeInMillis());
        renderConsultations();
    }

    private void showAddScheduleFlow() {
        List<Customer> customers = db.listCustomers(null);
        if (customers.isEmpty()) {
            Toast.makeText(this, "먼저 고객을 추가해주세요.", Toast.LENGTH_SHORT).show();
            selectSection(sectionCustomers, navCustomers);
            showAddCustomerDialog();
            return;
        }

        String[] labels = new String[customers.size()];
        for (int i = 0; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            labels[i] = customer.displayName + "  ·  " + customer.relationStatus;
        }
        new AlertDialog.Builder(this)
                .setTitle("고객 선택")
                .setItems(labels, (dialog, which) -> showScheduleTypeDialog(customers.get(which)))
                .setNegativeButton("취소", null)
                .show();
    }

    private void showScheduleTypeDialog(Customer customer) {
        String[] labels = {"전화하기", "방문·미팅", "자료 보내기", "직접 입력"};
        String[] types = {"CALL", "MEETING", "SEND", "CUSTOM"};
        new AlertDialog.Builder(this)
                .setTitle("일정 종류")
                .setItems(labels, (dialog, which) -> {
                    if (which == 3) showCustomScheduleTitle(customer);
                    else showNewTaskTimePicker(customer, labels[which], types[which]);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showCustomScheduleTitle(Customer customer) {
        EditText input = input("일정 내용", InputType.TYPE_CLASS_TEXT);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        wrap.addView(input, matchWrap());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("일정 내용")
                .setView(wrap)
                .setNegativeButton("취소", null)
                .setPositiveButton("다음", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "일정 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    showNewTaskTimePicker(customer, title, "CUSTOM");
                }));
        dialog.show();
    }

    private void showNewTaskTimePicker(Customer customer, String title, String taskType) {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar due = (Calendar) selectedCalendarDate.clone();
            due.set(Calendar.HOUR_OF_DAY, hourOfDay);
            due.set(Calendar.MINUTE, minute);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
            db.insertFollowUpTask(customer.id, 0L, taskType, title, due.getTimeInMillis());
            long now = System.currentTimeMillis();
            db.insertInteraction(customer.id, "SCHEDULE_CREATE", now, now, 0L,
                    "SCHEDULED", title);
            Toast.makeText(this, "일정을 추가했습니다.", Toast.LENGTH_SHORT).show();
            refreshAll();
        }, 10, 0, false).show();
    }

    private void showTaskOptions(FollowUpTask task) {
        String[] actions = {"날짜·시간 변경", "고객 보기", "삭제"};
        new AlertDialog.Builder(this)
                .setTitle(task.title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) showRescheduleDatePicker(task);
                    if (which == 1) openCustomer(task.customerId);
                    if (which == 2) confirmDeleteTask(task);
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void showRescheduleDatePicker(FollowUpTask task) {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(task.dueAt);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar changed = Calendar.getInstance();
            changed.set(year, month, dayOfMonth,
                    current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), 0);
            changed.set(Calendar.MILLISECOND, 0);
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                changed.set(Calendar.HOUR_OF_DAY, hourOfDay);
                changed.set(Calendar.MINUTE, minute);
                db.updateTaskDue(task.id, changed.getTimeInMillis());
                selectedCalendarDate.setTimeInMillis(changed.getTimeInMillis());
                visibleCalendarMonth.setTimeInMillis(changed.getTimeInMillis());
                visibleCalendarMonth.set(Calendar.DAY_OF_MONTH, 1);
                clearTime(visibleCalendarMonth);
                long now = System.currentTimeMillis();
                db.insertInteraction(task.customerId, "SCHEDULE_CHANGE", now, now, 0L,
                        "SCHEDULED", task.title + " 일정 변경");
                Toast.makeText(this, "일정을 변경했습니다.", Toast.LENGTH_SHORT).show();
                refreshAll();
            }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), false).show();
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void completeCalendarTask(FollowUpTask task) {
        db.completeTask(task.id);
        long now = System.currentTimeMillis();
        db.insertInteraction(task.customerId, "TASK_COMPLETE", now, now, 0L,
                "TASK_COMPLETED", task.title + " 완료");
        Toast.makeText(this, "일정을 완료했습니다.", Toast.LENGTH_SHORT).show();
        refreshAll();
    }

    private void confirmDeleteTask(FollowUpTask task) {
        new AlertDialog.Builder(this)
                .setTitle("일정 삭제")
                .setMessage(task.title)
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.deleteTask(task.id);
                    Toast.makeText(this, "삭제했습니다.", Toast.LENGTH_SHORT).show();
                    refreshAll();
                })
                .show();
    }

    private void renderMoreMenu() {
        moreMenuList.removeAllViews();
        addMenuRow("고객 상태 편집", "기본 3개와 사용자 상태의 이름·색상 변경", v -> openStageSettings());
    }

    private void addMenuRow(String title, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(12), dp(14), dp(12));
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        textWrap.addView(titleText(title, 16f), matchWrap());
        textWrap.addView(mutedText(subtitle), topMargin(4));
        row.addView(textWrap, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = titleText("›", 24f);
        arrow.setTextColor(getColor(R.color.text_muted));
        row.addView(arrow);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(72));
        params.bottomMargin = dp(8);
        moreMenuList.addView(row, params);
    }

    private void openCustomer(long customerId) {
        startActivity(new Intent(this, CustomerDetailActivity.class)
                .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customerId));
    }

    private void showAddCustomerDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(6), dp(20), 0);
        EditText name = input("고객명 또는 업체명", InputType.TYPE_CLASS_TEXT);
        EditText phone = input("전화번호", InputType.TYPE_CLASS_PHONE);
        form.addView(name, matchWrap());
        form.addView(phone, topMargin(12));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("고객 추가")
                .setView(form)
                .setNegativeButton("취소", null)
                .setPositiveButton("추가", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        long id = db.insertNewLead(name.getText().toString(), phone.getText().toString());
                        dialog.dismiss();
                        refreshAll();
                        openCustomer(id);
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }));
        dialog.show();
    }

    private Button smallButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(getColor(R.color.text_primary));
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout verticalCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackgroundResource(R.drawable.bg_clickable_card);
        card.setClickable(true);
        card.setFocusable(true);
        return card;
    }

    private GradientDrawable customerCardBackground(String colorHex) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(16));
        shape.setColor(getColor(R.color.surface));
        shape.setStroke(dp(2), parseColor(colorHex));
        return shape;
    }

    private GradientDrawable stageTagBackground(String colorHex) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(12));
        shape.setColor(parseColor(colorHex));
        return shape;
    }

    private GradientDrawable calendarCellBackground(boolean selected, boolean today) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(10));
        shape.setColor(getColor(R.color.surface_soft));
        if (selected) shape.setStroke(dp(2), getColor(R.color.primary));
        else if (today) shape.setStroke(dp(1), getColor(R.color.primary));
        else shape.setStroke(dp(1), getColor(R.color.border));
        return shape;
    }

    private GradientDrawable calendarPillBackground(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(6));
        shape.setColor(Color.argb(48, Color.red(color), Color.green(color), Color.blue(color)));
        shape.setStroke(dp(1), color);
        return shape;
    }

    private int parseColor(String value) {
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException ignored) {
            return getColor(R.color.primary);
        }
    }

    private int contrastTextColor(int backgroundColor) {
        double luminance = (0.299 * Color.red(backgroundColor)
                + 0.587 * Color.green(backgroundColor)
                + 0.114 * Color.blue(backgroundColor)) / 255d;
        return luminance > 0.66d ? Color.rgb(20, 22, 25) : Color.WHITE;
    }

    private TextView titleText(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView bodyText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(15f);
        return text;
    }

    private TextView mutedText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_muted));
        text.setTextSize(13f);
        return text;
    }

    private EditText input(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setTextSize(16f);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        return input;
    }

    private boolean sameDay(long millis, Calendar date) {
        Calendar other = Calendar.getInstance();
        other.setTimeInMillis(millis);
        return sameDay(other, date);
    }

    private boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void dial(String phone) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
        } catch (RuntimeException e) {
            Toast.makeText(this, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}
