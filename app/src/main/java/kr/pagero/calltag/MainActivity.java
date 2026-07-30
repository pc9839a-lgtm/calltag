package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.InputType;
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
        findViewById(R.id.filterAll).setOnClickListener(v -> setCustomerFilter(null));
        findViewById(R.id.filterNew).setOnClickListener(v -> setCustomerFilter(CallTagDbHelper.STATUS_NEW));
        findViewById(R.id.filterConsulting).setOnClickListener(v -> setCustomerFilter(CallTagDbHelper.STATUS_CONSULTING));
        findViewById(R.id.filterExisting).setOnClickListener(v -> setCustomerFilter(CallTagDbHelper.STATUS_EXISTING));
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
        todayNewCount.setText(db.countCustomersByStatus(CallTagDbHelper.STATUS_NEW) + "\n신규 고객");
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

    private void setCustomerFilter(String status) {
        activeCustomerFilter = status;
        renderCustomers();
        setFilterColor(R.id.filterAll, status == null);
        setFilterColor(R.id.filterNew, CallTagDbHelper.STATUS_NEW.equals(status));
        setFilterColor(R.id.filterConsulting, CallTagDbHelper.STATUS_CONSULTING.equals(status));
        setFilterColor(R.id.filterExisting, CallTagDbHelper.STATUS_EXISTING.equals(status));
    }

    private void setFilterColor(int id, boolean selected) {
        Button button = findViewById(id);
        button.setTextColor(getColor(selected ? R.color.primary : R.color.text_secondary));
    }

    private void renderCustomers() {
        customerList.removeAllViews();
        List<Customer> customers = db.listCustomers(activeCustomerFilter);
        if (customers.isEmpty()) {
            TextView empty = bodyText("고객이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(22), dp(24), dp(22));
            empty.setBackgroundResource(R.drawable.bg_card);
            customerList.addView(empty, matchWrap());
            return;
        }

        DateFormat formatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
        for (Customer customer : customers) {
            LinearLayout card = verticalCard();
            card.setOnClickListener(v -> openCustomer(customer.id));
            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(titleText(customer.displayName, 17f), new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView badge = titleText(statusLabel(customer.relationStatus), 12f);
            badge.setTextColor(statusColor(customer.relationStatus));
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(10), dp(6), dp(10), dp(6));
            badge.setBackgroundResource(R.drawable.bg_badge);
            header.addView(badge);
            TextView arrow = titleText("  ›", 22f);
            arrow.setTextColor(getColor(R.color.text_muted));
            header.addView(arrow);
            card.addView(header, matchWrap());
            card.addView(bodyText(customer.primaryPhone), topMargin(10));
            card.addView(mutedText(formatter.format(new Date(customer.lastContactAt))), topMargin(5));
            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            customerList.addView(card, params);
        }
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
        calendar.setPadding(dp(6), dp(8), dp(6), dp(8));
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
                TextView cell = new TextView(this);
                cell.setGravity(Gravity.CENTER);
                cell.setIncludeFontPadding(false);
                cell.setTextSize(13f);
                cell.setLineSpacing(0f, 1.2f);
                cell.setBackgroundResource(R.drawable.bg_calendar_day);

                if (dayNumber >= 1 && dayNumber <= maxDay) {
                    Calendar date = (Calendar) monthStart.clone();
                    date.set(Calendar.DAY_OF_MONTH, dayNumber);
                    clearTime(date);
                    int total = countTasksForDate(monthTasks, date, false);
                    int completed = countTasksForDate(monthTasks, date, true);
                    String secondLine = total == 0 ? "" : (completed == total ? "\n✓ " + total : "\n" + total + "건");
                    cell.setText(dayNumber + secondLine);
                    cell.setTextColor(getColor(column == 0 ? R.color.danger
                            : column == 6 ? R.color.primary : R.color.text_primary));

                    if (sameDay(date, selectedCalendarDate)) {
                        cell.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                        cell.setTextColor(getColor(R.color.text_primary));
                        cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    } else if (sameDay(date, today)) {
                        cell.setBackgroundResource(R.drawable.bg_calendar_day_today);
                        cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    }
                    cell.setOnClickListener(v -> {
                        selectedCalendarDate.setTimeInMillis(date.getTimeInMillis());
                        renderConsultations();
                    });
                    cell.setClickable(true);
                    cell.setFocusable(true);
                }
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, dp(58), 1f);
                cellParams.setMargins(dp(2), dp(2), dp(2), dp(2));
                row.addView(cell, cellParams);
            }
            calendar.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        return calendar;
    }

    private int countTasksForDate(List<FollowUpTask> tasks, Calendar date, boolean completedOnly) {
        int count = 0;
        for (FollowUpTask task : tasks) {
            if (sameDay(task.dueAt, date) && (!completedOnly || task.isCompleted())) count++;
        }
        return count;
    }

    private View buildSelectedDateHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = titleText(new SimpleDateFormat("M월 d일 EEEE", Locale.KOREA)
                .format(selectedCalendarDate.getTime()), 18f);
        row.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView add = titleText("+ 일정 추가", 14f);
        add.setTextColor(getColor(R.color.primary));
        add.setGravity(Gravity.CENTER);
        add.setPadding(dp(14), 0, dp(14), 0);
        add.setBackgroundResource(R.drawable.bg_secondary_button);
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
            TextView status = titleText(customer == null ? "" : statusLabel(customer.relationStatus), 12f);
            status.setTextColor(customer == null ? getColor(R.color.text_muted) : statusColor(customer.relationStatus));
            status.setPadding(dp(10), dp(5), dp(10), dp(5));
            status.setBackgroundResource(R.drawable.bg_badge);
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
            labels[i] = customer.displayName + "  ·  " + statusLabel(customer.relationStatus);
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
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
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
        int defaultHour = 10;
        int defaultMinute = 0;
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar due = (Calendar) selectedCalendarDate.clone();
            due.set(Calendar.HOUR_OF_DAY, hourOfDay);
            due.set(Calendar.MINUTE, minute);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
            db.insertFollowUpTask(customer.id, 0L, taskType, title, due.getTimeInMillis());

            String statusNote = "";
            if (CallTagDbHelper.STATUS_NEW.equals(customer.relationStatus)) {
                db.updateCustomer(customer.id, customer.displayName, CallTagDbHelper.STATUS_CONSULTING);
                statusNote = " · 신규 → 상담 중";
            }
            long now = System.currentTimeMillis();
            db.insertInteraction(customer.id, "SCHEDULE_CREATE", now, now, 0L,
                    "SCHEDULED", title + statusNote);
            Toast.makeText(this, "일정을 추가했습니다.", Toast.LENGTH_SHORT).show();
            refreshAll();
        }, defaultHour, defaultMinute, false).show();
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
        addMenuRow("통화 후 처리 테스트", v -> openPostCallTest());
        addMenuRow(SettingsStore.isMonitorEnabled(this) ? "통화 감지 끄기" : "통화 감지 켜기",
                v -> toggleMonitor());
        addMenuRow("제외번호", v -> Toast.makeText(this,
                "통화 후 정리에서 제외를 선택할 수 있습니다.", Toast.LENGTH_SHORT).show());
    }

    private void addMenuRow(String label, View.OnClickListener listener) {
        TextView row = new TextView(this);
        row.setText(label + "                                      ›");
        row.setTextColor(getColor(R.color.text_primary));
        row.setTextSize(16f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(14), 0);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        params.bottomMargin = dp(1);
        moreMenuList.addView(row, params);
    }

    private void openCustomer(long customerId) {
        startActivity(new Intent(this, CustomerDetailActivity.class)
                .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customerId));
    }

    private void openPostCallTest() {
        startActivity(new Intent(this, PostCallActivity.class)
                .putExtra(PostCallActivity.EXTRA_PHONE, "010-0000-1234")
                .putExtra(PostCallActivity.EXTRA_CACHED_NAME, "")
                .putExtra(PostCallActivity.EXTRA_CALL_TYPE, CallLog.Calls.INCOMING_TYPE)
                .putExtra(PostCallActivity.EXTRA_STARTED_AT, System.currentTimeMillis() - 185_000L)
                .putExtra(PostCallActivity.EXTRA_ENDED_AT, System.currentTimeMillis())
                .putExtra(PostCallActivity.EXTRA_DURATION_SEC, 185L));
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
        card.setBackgroundResource(R.drawable.bg_card);
        card.setClickable(true);
        card.setFocusable(true);
        return card;
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

    private String statusLabel(String status) {
        if (CallTagDbHelper.STATUS_CONSULTING.equals(status)) return "상담 중";
        if (CallTagDbHelper.STATUS_EXISTING.equals(status)) return "기존";
        if (CallTagDbHelper.STATUS_VIP.equals(status)) return "VIP";
        if (CallTagDbHelper.STATUS_DORMANT.equals(status)) return "휴면";
        return "신규";
    }

    private int statusColor(String status) {
        if (CallTagDbHelper.STATUS_NEW.equals(status)) return getColor(R.color.primary);
        if (CallTagDbHelper.STATUS_EXISTING.equals(status)) return getColor(R.color.text_primary);
        return getColor(R.color.text_secondary);
    }

    private String resultLabel(String result) {
        if ("QUOTE".equals(result)) return "견적·자료 발송";
        if ("CALLBACK".equals(result)) return "다시 연락";
        if ("CONTRACT".equals(result)) return "계약·거래 완료";
        if ("HOLD".equals(result)) return "보류";
        if ("CLOSED".equals(result)) return "상담 종료";
        if ("SCHEDULED".equals(result)) return "일정 등록·변경";
        if ("TASK_COMPLETED".equals(result)) return "일정 완료";
        if (result != null && result.startsWith("STATUS_")) {
            return "상태 변경 · " + statusLabel(result.substring("STATUS_".length()));
        }
        return "관심 있음";
    }

    private void dial(String phone) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
        } catch (RuntimeException e) {
            Toast.makeText(this, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
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
