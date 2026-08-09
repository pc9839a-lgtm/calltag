package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private CallTagDbHelper db;
    private TaskTypeStore taskTypes;

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
    private LinearLayout customerStatsContent;
    private View customerListPanel;
    private View customerStatsPanel;
    private Button customerListTab;
    private Button customerStatsTab;
    private Button customerFilterButton;
    private Button customerDateFilterButton;
    private EditText customerSearchInput;
    private String activeCustomerFilter;
    private String customerSearchQuery = "";
    private int customerDateDays;

    private LinearLayout consultationSummary;
    private LinearLayout moreMenuList;
    private final Calendar visibleCalendarMonth = Calendar.getInstance();
    private final Calendar selectedCalendarDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new CallTagDbHelper(this);
        taskTypes = new TaskTypeStore(this);
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
        customerStatsContent = findViewById(R.id.customerStatsContent);
        customerListPanel = findViewById(R.id.customerListPanel);
        customerStatsPanel = findViewById(R.id.customerStatsPanel);
        customerListTab = findViewById(R.id.customerListTab);
        customerStatsTab = findViewById(R.id.customerStatsTab);
        customerFilterButton = findViewById(R.id.customerFilterButton);
        customerDateFilterButton = findViewById(R.id.customerDateFilterButton);
        customerSearchInput = findViewById(R.id.customerSearchInput);

        consultationSummary = findViewById(R.id.consultationSummary);
        moreMenuList = findViewById(R.id.moreMenuList);
    }

    private void bindActions() {
        navToday.setOnClickListener(v -> selectSection(sectionToday, navToday));
        navCustomers.setOnClickListener(v -> selectSection(sectionCustomers, navCustomers));
        navConsultations.setOnClickListener(v -> selectSection(sectionConsultations, navConsultations));
        navMore.setOnClickListener(v -> selectSection(sectionMore, navMore));
        enableMonitorButton.setOnClickListener(v -> toggleMonitor());

        findViewById(R.id.quickAddCustomer).setOnClickListener(v -> {});
        findViewById(R.id.addCustomerButton).setOnClickListener(v -> {});
        findViewById(R.id.quickCustomers).setOnClickListener(v -> selectSection(sectionCustomers, navCustomers));
        findViewById(R.id.quickConsultations).setOnClickListener(v -> selectSection(sectionConsultations, navConsultations));
        findViewById(R.id.stageSettingsButton).setOnClickListener(v -> openStageSettings());

        customerListTab.setOnClickListener(v -> showCustomerSubTab(false));
        customerStatsTab.setOnClickListener(v -> showCustomerSubTab(true));
        customerFilterButton.setOnClickListener(v -> showCustomerFilterDialog());
        customerDateFilterButton.setOnClickListener(v -> showCustomerDateFilterDialog());
        customerSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                customerSearchQuery = s == null ? "" : s.toString().trim();
                renderCustomers();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
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

    private void showCustomerSubTab(boolean statistics) {
        customerListPanel.setVisibility(statistics ? View.GONE : View.VISIBLE);
        customerStatsPanel.setVisibility(statistics ? View.VISIBLE : View.GONE);
        styleSegmentButton(customerListTab, !statistics);
        styleSegmentButton(customerStatsTab, statistics);
        if (statistics) renderCustomerStats();
        else renderCustomers();
    }

    private void styleSegmentButton(Button button, boolean selected) {
        button.setBackgroundResource(selected ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        button.setTextColor(getColor(selected ? R.color.text_primary : R.color.text_secondary));
    }

    private void refreshAll() {
        todayDueCount.setText(db.countDueTodayTasks() + "\n오늘 할 일");
        overdueCount.setText(db.countOverdueTasks() + "\n기한 지남");
        todayNewCount.setText(db.countCustomersByStatus(db.firstStage()) + "\n신규 고객");
        renderMonitorState();
        renderTasks();
        renderCustomers();
        renderCustomerStats();
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
                .setTitle("통화 감지 권한")
                .setMessage("발신·수신·부재중 통화를 할 일과 연결하는 데 필요합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("권한 허용", (dialog, which) -> requestPermissions(
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
            Toast.makeText(this, "통화 감지 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "통화 감지를 시작하지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMonitorService() {
        SettingsStore.setMonitorEnabled(this, false);
        stopService(new Intent(this, CallMonitorService.class));
    }

    private void renderMonitorState() {
        boolean enabled = hasMonitorPermissions() && SettingsStore.isMonitorEnabled(this);
        monitorStateText.setText(enabled ? "통화와 할 일을\n자동으로 연결해요" : "통화 감지를\n켜주세요");
        enableMonitorButton.setChecked(enabled);
    }

    private void renderTasks() {
        todayTaskList.removeAllViews();
        List<FollowUpTask> tasks = db.listPendingTasks();
        todayEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        for (FollowUpTask task : tasks) {
            TaskTypeOption type = taskTypes.find(task.taskType);
            LinearLayout card = verticalCard(false);

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(titleText(task.customerName, 17f), new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView badge = typeBadge(type);
            header.addView(badge);
            card.addView(header, matchWrap());

            card.addView(bodyText(task.title), topMargin(9));
            TextView due = mutedText(formatter.format(new Date(task.dueAt)));
            if (task.isOverdue()) due.setTextColor(getColor(R.color.danger));
            card.addView(due, topMargin(6));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button change = smallButton("일정 변경", false);
            change.setOnClickListener(v -> showTaskOptions(task));
            actions.addView(change, new LinearLayout.LayoutParams(0, dp(46), 1f));

            Button primary = smallButton(taskPrimaryLabel(type), true);
            primary.setOnClickListener(v -> {
                if (TaskTypeStore.TYPE_CALL.equals(type.code)) dial(task.phone);
                else completeCalendarTask(task, type.name + " 완료");
            });
            LinearLayout.LayoutParams primaryParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
            primaryParams.leftMargin = dp(8);
            actions.addView(primary, primaryParams);
            card.addView(actions, topMargin(14));

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            todayTaskList.addView(card, params);
        }
    }

    private String taskPrimaryLabel(TaskTypeOption type) {
        if (TaskTypeStore.TYPE_CALL.equals(type.code)) return "전화하기";
        if (TaskTypeStore.TYPE_SEND.equals(type.code)) return "자료 보냄";
        if (TaskTypeStore.TYPE_MEETING.equals(type.code)) return "미팅 완료";
        String label = type.name.trim();
        return label.length() > 8 ? "처리 완료" : label + " 완료";
    }

    private void showCustomerFilterDialog() {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        options.add(new ActionChoiceDialog.Option("", "전체 상태", "모든 고객 보기", ""));
        for (StageOption stage : db.listStages()) {
            options.add(new ActionChoiceDialog.Option(stage.name, stage.name,
                    "이 상태의 고객만 보기", stage.color));
        }
        ActionChoiceDialog.show(this, "고객 상태 필터", "선택한 상태만 고객 목록에 표시합니다.",
                options, option -> {
                    activeCustomerFilter = option.key.trim().isEmpty() ? null : option.key;
                    renderCustomers();
                }, "고객 상태·색상 편집", v -> openStageSettings());
    }

    private void showCustomerDateFilterDialog() {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        options.add(new ActionChoiceDialog.Option("0", "전체 기간", "최근 연락일 제한 없음", ""));
        options.add(new ActionChoiceDialog.Option("1", "오늘", "오늘 연락한 고객", "#4389FF"));
        options.add(new ActionChoiceDialog.Option("7", "최근 7일", "최근 일주일 내 연락한 고객", "#7A5AF8"));
        options.add(new ActionChoiceDialog.Option("30", "최근 30일", "최근 한 달 내 연락한 고객", "#F5A524"));
        ActionChoiceDialog.show(this, "최근 연락일 필터", "고객의 마지막 연락일을 기준으로 필터링합니다.",
                options, option -> {
                    try {
                        customerDateDays = Integer.parseInt(option.key);
                    } catch (NumberFormatException ignored) {
                        customerDateDays = 0;
                    }
                    renderCustomers();
                });
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
        customerDateFilterButton.setText(dateFilterLabel() + "  ▾");

        List<Customer> filtered = new ArrayList<>();
        String query = PhoneNumberNormalizer.normalize(customerSearchQuery);
        String textQuery = customerSearchQuery.toLowerCase(Locale.KOREA);
        long cutoff = customerDateCutoff();
        for (Customer customer : db.listCustomers(activeCustomerFilter)) {
            if (cutoff > 0L && customer.lastContactAt < cutoff) continue;
            if (!textQuery.isEmpty()) {
                boolean nameMatch = customer.displayName.toLowerCase(Locale.KOREA).contains(textQuery);
                boolean phoneMatch = !query.isEmpty()
                        && PhoneNumberNormalizer.normalize(customer.primaryPhone).contains(query);
                if (!nameMatch && !phoneMatch) continue;
            }
            filtered.add(customer);
        }

        if (filtered.isEmpty()) {
            TextView empty = bodyText(customerSearchQuery.isEmpty()
                    ? "조건에 맞는 고객이 없습니다."
                    : "검색 결과가 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(22), dp(24), dp(22));
            empty.setBackgroundResource(R.drawable.bg_card);
            customerList.addView(empty, matchWrap());
            return;
        }

        DateFormat formatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
        for (Customer customer : filtered) {
            String colorHex = db.stageColor(customer.relationStatus);
            LinearLayout card = verticalCard(false);
            card.setBackground(customerCardBackground(colorHex));

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(titleText(customer.displayName, 17f), new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView stage = titleText(customer.relationStatus, 12f);
            stage.setTextColor(contrastTextColor(parseColor(colorHex)));
            stage.setPadding(dp(11), dp(6), dp(11), dp(6));
            stage.setBackground(stageTagBackground(colorHex));
            header.addView(stage);
            card.addView(header, matchWrap());

            card.addView(bodyText(customer.primaryPhone), topMargin(10));
            card.addView(mutedText("최근 연락 · " + formatter.format(new Date(customer.lastContactAt))), topMargin(5));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button change = smallButton("상태 변경", false);
            change.setOnClickListener(v -> showStagePicker(customer));
            actions.addView(change, new LinearLayout.LayoutParams(0, dp(44), 1f));
            Button open = smallButton("고객 상세", true);
            open.setOnClickListener(v -> openCustomer(customer.id));
            LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
            openParams.leftMargin = dp(8);
            actions.addView(open, openParams);
            card.addView(actions, topMargin(12));

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            customerList.addView(card, params);
        }
    }

    private String dateFilterLabel() {
        if (customerDateDays == 1) return "오늘";
        if (customerDateDays == 7) return "최근 7일";
        if (customerDateDays == 30) return "최근 30일";
        return "전체 기간";
    }

    private long customerDateCutoff() {
        if (customerDateDays <= 0) return 0L;
        Calendar start = Calendar.getInstance();
        clearTime(start);
        start.add(Calendar.DAY_OF_MONTH, -(customerDateDays - 1));
        return start.getTimeInMillis();
    }

    private void showStagePicker(Customer customer) {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        for (StageOption stage : db.listStages()) {
            String subtitle = stage.name.equals(customer.relationStatus)
                    ? "현재 상태" : "이 상태로 변경";
            options.add(new ActionChoiceDialog.Option(stage.name, stage.name, subtitle, stage.color));
        }
        ActionChoiceDialog.show(this, customer.displayName + " 상태 변경", null,
                options, option -> {
                    String oldStage = customer.relationStatus;
                    String newStage = option.key;
                    if (!oldStage.equals(newStage)) {
                        db.updateCustomerStage(customer.id, newStage);
                        long now = System.currentTimeMillis();
                        db.insertInteraction(customer.id, "STATUS_CHANGE", now, now, 0L,
                                "STATUS_" + newStage, oldStage + " → " + newStage);
                        Toast.makeText(this, newStage + "으로 변경했습니다.", Toast.LENGTH_SHORT).show();
                    }
                    refreshAll();
                }, "고객 상태·색상 편집", v -> openStageSettings());
    }

    private void renderCustomerStats() {
        customerStatsContent.removeAllViews();
        List<Customer> customers = db.listCustomers(null);
        List<FollowUpTask> tasks = db.listPendingTasks();

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.setBackgroundResource(R.drawable.bg_card);
        summary.setPadding(dp(8), dp(16), dp(8), dp(16));
        summary.addView(statNumber(String.valueOf(customers.size()), "전체 고객", R.color.text_primary),
                new LinearLayout.LayoutParams(0, dp(76), 1f));
        summary.addView(statNumber(String.valueOf(db.countDueTodayTasks()), "오늘 할 일", R.color.primary),
                new LinearLayout.LayoutParams(0, dp(76), 1f));
        summary.addView(statNumber(String.valueOf(db.countOverdueTasks()), "기한 지남", R.color.danger),
                new LinearLayout.LayoutParams(0, dp(76), 1f));
        customerStatsContent.addView(summary, matchWrap());

        customerStatsContent.addView(sectionLabel("고객 상태"), topMargin(22));
        for (StageOption stage : db.listStages()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            row.setBackgroundResource(R.drawable.bg_card);
            View swatch = new View(this);
            swatch.setBackground(stageTagBackground(stage.color));
            row.addView(swatch, new LinearLayout.LayoutParams(dp(14), dp(38)));
            TextView name = titleText(stage.name, 15f);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            nameParams.leftMargin = dp(12);
            row.addView(name, nameParams);
            row.addView(titleText(String.valueOf(db.countCustomersByStatus(stage.name)), 20f));
            customerStatsContent.addView(row, topMargin(8));
        }

        int contactedToday = 0;
        int contactedSevenDays = 0;
        long todayStart = startOfToday();
        long sevenDays = todayStart - 6L * DAY_MS;
        for (Customer customer : customers) {
            if (customer.lastContactAt >= todayStart) contactedToday++;
            if (customer.lastContactAt >= sevenDays) contactedSevenDays++;
        }
        customerStatsContent.addView(sectionLabel("연락 현황"), topMargin(22));
        customerStatsContent.addView(infoRow("오늘 연락한 고객", contactedToday + "명"), topMargin(8));
        customerStatsContent.addView(infoRow("최근 7일 연락 고객", contactedSevenDays + "명"), topMargin(8));
        customerStatsContent.addView(infoRow("예정된 전체 할 일", tasks.size() + "건"), topMargin(8));
    }

    private TextView statNumber(String number, String label, int color) {
        TextView view = new TextView(this);
        view.setText(number + "\n" + label);
        view.setTextColor(getColor(color));
        view.setTextSize(15f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setLineSpacing(0f, 1.25f);
        return view;
    }

    private View infoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackgroundResource(R.drawable.bg_card);
        row.addView(bodyText(label), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(titleText(value, 16f));
        return row;
    }

    private void openStageSettings() {
        startActivity(new Intent(this, StageSettingsActivity.class));
    }

    private void openTaskTypeSettings() {
        startActivity(new Intent(this, TaskTypeSettingsActivity.class));
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

        LinearLayout calendarCard = verticalCard(false);
        calendarCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        calendarCard.addView(buildMonthHeader(), matchWrap());
        calendarCard.addView(buildWeekdayHeader(), topMargin(10));
        calendarCard.addView(buildCalendarGrid(monthStart, monthTasks), topMargin(2));
        consultationSummary.addView(calendarCard, matchWrap());

        consultationSummary.addView(buildSelectedDateTitle(), topMargin(22));
        Button add = smallButton("+ 일정 추가", true);
        add.setTextSize(16f);
        add.setOnClickListener(v -> showAddScheduleFlow());
        consultationSummary.addView(add, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)) {{ topMargin = dp(12); }});
        renderSelectedDateTasks(monthTasks);
    }

    private View buildMonthHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView prev = calendarArrow("‹");
        prev.setOnClickListener(v -> changeCalendarMonth(-1));
        row.addView(prev, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = titleText(new SimpleDateFormat("yyyy년 M월", Locale.KOREA)
                .format(visibleCalendarMonth.getTime()), 20f);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(0, dp(46), 1f));
        TextView next = calendarArrow("›");
        next.setOnClickListener(v -> changeCalendarMonth(1));
        row.addView(next, new LinearLayout.LayoutParams(dp(46), dp(46)));
        return row;
    }

    private TextView calendarArrow(String value) {
        TextView view = titleText(value, 28f);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(R.drawable.bg_secondary_button);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private View buildWeekdayHeader() {
        LinearLayout row = new LinearLayout(this);
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        for (int i = 0; i < labels.length; i++) {
            TextView day = mutedText(labels[i]);
            day.setGravity(Gravity.CENTER);
            if (i == 0) day.setTextColor(getColor(R.color.danger));
            if (i == 6) day.setTextColor(getColor(R.color.primary));
            row.addView(day, new LinearLayout.LayoutParams(0, dp(30), 1f));
        }
        return row;
    }

    private View buildCalendarGrid(Calendar monthStart, List<FollowUpTask> monthTasks) {
        LinearLayout calendar = new LinearLayout(this);
        calendar.setOrientation(LinearLayout.VERTICAL);
        int offset = monthStart.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        int maxDay = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();
        clearTime(today);

        for (int week = 0; week < 6; week++) {
            LinearLayout row = new LinearLayout(this);
            for (int column = 0; column < 7; column++) {
                int index = week * 7 + column;
                int dayNumber = index - offset + 1;
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dp(3), dp(5), dp(3), dp(3));

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
                    day.setGravity(Gravity.CENTER);
                    day.setIncludeFontPadding(false);
                    day.setTypeface(Typeface.DEFAULT, selected || isToday ? Typeface.BOLD : Typeface.NORMAL);
                    day.setTextColor(selected ? Color.WHITE : getColor(column == 0
                            ? R.color.danger : column == 6 ? R.color.primary : R.color.text_primary));
                    cell.addView(day, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(20)));

                    List<FollowUpTask> dateTasks = tasksForDate(monthTasks, date);
                    if (!dateTasks.isEmpty()) {
                        FollowUpTask first = dateTasks.get(0);
                        TaskTypeOption type = taskTypes.find(first.taskType);
                        TextView label = new TextView(this);
                        label.setText(first.title);
                        label.setSingleLine(true);
                        label.setEllipsize(TextUtils.TruncateAt.END);
                        label.setTextSize(8f);
                        label.setGravity(Gravity.CENTER);
                        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                        int color = parseColor(type.color);
                        label.setTextColor(selected ? Color.WHITE : color);
                        label.setBackground(selected ? null : calendarPillBackground(color));
                        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, dp(16));
                        labelParams.topMargin = dp(3);
                        cell.addView(label, labelParams);
                        if (dateTasks.size() > 1) {
                            TextView more = mutedText("+" + (dateTasks.size() - 1));
                            more.setTextSize(9f);
                            more.setGravity(Gravity.CENTER);
                            if (selected) more.setTextColor(Color.WHITE);
                            cell.addView(more, new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, dp(14)));
                        }
                    }

                    cell.setOnClickListener(v -> {
                        selectedCalendarDate.setTimeInMillis(date.getTimeInMillis());
                        renderConsultations();
                    });
                    cell.setClickable(true);
                    cell.setFocusable(true);
                }

                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, dp(66), 1f);
                cellParams.setMargins(dp(1), dp(2), dp(1), dp(2));
                row.addView(cell, cellParams);
            }
            calendar.addView(row, matchWrap());
        }
        return calendar;
    }

    private View buildSelectedDateTitle() {
        TextView title = titleText(new SimpleDateFormat("M월 d일 EEEE", Locale.KOREA)
                .format(selectedCalendarDate.getTime()), 19f);
        return title;
    }

    private List<FollowUpTask> tasksForDate(List<FollowUpTask> tasks, Calendar date) {
        List<FollowUpTask> rows = new ArrayList<>();
        for (FollowUpTask task : tasks) {
            if (sameDay(task.dueAt, date)) rows.add(task);
        }
        return rows;
    }

    private void renderSelectedDateTasks(List<FollowUpTask> monthTasks) {
        List<FollowUpTask> selectedTasks = tasksForDate(monthTasks, selectedCalendarDate);
        if (selectedTasks.isEmpty()) {
            TextView empty = bodyText("이 날짜에 등록된 할 일이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(22), dp(20), dp(22));
            empty.setBackgroundResource(R.drawable.bg_card);
            consultationSummary.addView(empty, topMargin(12));
            return;
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm", Locale.KOREA);
        for (FollowUpTask task : selectedTasks) {
            TaskTypeOption type = taskTypes.find(task.taskType);
            LinearLayout card = verticalCard(false);
            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(titleText(task.customerName, 16f), new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            header.addView(typeBadge(type));
            card.addView(header, matchWrap());

            TextView title = bodyText((task.isCompleted() ? "처리 완료 · " : "") + task.title);
            if (task.isCompleted()) title.setTextColor(getColor(R.color.text_muted));
            card.addView(title, topMargin(9));
            card.addView(mutedText(timeFormat.format(new Date(task.dueAt))), topMargin(5));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button change = smallButton(task.isCompleted() ? "다시 열기" : "일정 변경", false);
            change.setOnClickListener(v -> {
                if (task.isCompleted()) {
                    db.reopenTask(task.id);
                    refreshAll();
                } else {
                    showTaskOptions(task);
                }
            });
            actions.addView(change, new LinearLayout.LayoutParams(0, dp(44), 1f));

            Button primary;
            if (task.isCompleted()) {
                primary = smallButton("삭제", false);
                primary.setTextColor(getColor(R.color.danger));
                primary.setOnClickListener(v -> confirmDeleteTask(task));
            } else if (TaskTypeStore.TYPE_CALL.equals(type.code)) {
                primary = smallButton("전화하기", true);
                primary.setOnClickListener(v -> dial(task.phone));
            } else {
                primary = smallButton(taskPrimaryLabel(type), true);
                primary.setOnClickListener(v -> completeCalendarTask(task, type.name + " 완료"));
            }
            LinearLayout.LayoutParams primaryParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
            primaryParams.leftMargin = dp(8);
            actions.addView(primary, primaryParams);
            card.addView(actions, topMargin(12));

            consultationSummary.addView(card, topMargin(10));
        }
    }

    private TextView typeBadge(TaskTypeOption type) {
        TextView badge = titleText(type.name, 12f);
        int color = parseColor(type.color);
        badge.setTextColor(contrastTextColor(color));
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        badge.setBackground(stageTagBackground(type.color));
        return badge;
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
            return;
        }
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        for (Customer customer : customers) {
            options.add(new ActionChoiceDialog.Option(String.valueOf(customer.id), customer.displayName,
                    customer.primaryPhone + " · " + customer.relationStatus,
                    db.stageColor(customer.relationStatus)));
        }
        ActionChoiceDialog.show(this, "일정 고객 선택", "일정을 등록할 고객을 선택합니다.",
                options, option -> {
                    try {
                        Customer customer = db.findCustomerById(Long.parseLong(option.key));
                        if (customer != null) showScheduleTypeDialog(customer);
                    } catch (NumberFormatException ignored) {
                        Toast.makeText(this, "고객을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showScheduleTypeDialog(Customer customer) {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        for (TaskTypeOption type : taskTypes.list()) {
            options.add(new ActionChoiceDialog.Option(type.code, type.name,
                    "이 종류로 할 일 등록", type.color));
        }
        ActionChoiceDialog.show(this, "할 일 종류 선택", customer.displayName + " 고객의 할 일을 선택합니다.",
                options, option -> {
                    TaskTypeOption type = taskTypes.find(option.key);
                    showNewTaskTimePicker(customer, type.name, type.code);
                }, "일정 종류 편집", v -> openTaskTypeSettings());
    }

    private void showNewTaskTimePicker(Customer customer, String title, String taskType) {
        TaskTimeChoiceDialog.show(this, 10, 0, "이 시간으로 등록", (hourOfDay, minute) -> {
            Calendar due = (Calendar) selectedCalendarDate.clone();
            due.set(Calendar.HOUR_OF_DAY, hourOfDay);
            due.set(Calendar.MINUTE, minute);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
            db.insertFollowUpTask(customer.id, 0L, taskType, title, due.getTimeInMillis());
            long now = System.currentTimeMillis();
            db.insertInteraction(customer.id, "SCHEDULE_CREATE", now, now, 0L,
                    "SCHEDULED", title);
            Toast.makeText(this, "할 일을 추가했습니다.", Toast.LENGTH_SHORT).show();
            refreshAll();
        });
    }

    private void showTaskOptions(FollowUpTask task) {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        options.add(new ActionChoiceDialog.Option("RESCHEDULE", "날짜·시간 변경", "일정 시간을 다시 선택", "#4389FF"));
        options.add(new ActionChoiceDialog.Option("CUSTOMER", "고객 상세 보기", task.customerName, "#7A5AF8"));
        options.add(new ActionChoiceDialog.Option("DELETE", "일정 삭제", "이 할 일을 삭제", "#F97066"));
        ActionChoiceDialog.show(this, task.title, null, options, option -> {
            if ("RESCHEDULE".equals(option.key)) showRescheduleDatePicker(task);
            else if ("CUSTOMER".equals(option.key)) openCustomer(task.customerId);
            else if ("DELETE".equals(option.key)) confirmDeleteTask(task);
        });
    }

    private void showRescheduleDatePicker(FollowUpTask task) {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(task.dueAt);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar changed = Calendar.getInstance();
            changed.set(year, month, dayOfMonth,
                    current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), 0);
            changed.set(Calendar.MILLISECOND, 0);
            TaskTimeChoiceDialog.show(this,
                    current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE),
                    "이 시간으로 변경", (hourOfDay, minute) -> {
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
            });
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void completeCalendarTask(FollowUpTask task, String resultMessage) {
        db.completeTask(task.id);
        long now = System.currentTimeMillis();
        db.insertInteraction(task.customerId, "TASK_COMPLETE", now, now, 0L,
                "TASK_COMPLETED", resultMessage);
        Toast.makeText(this, resultMessage, Toast.LENGTH_SHORT).show();
        refreshAll();
    }

    private void confirmDeleteTask(FollowUpTask task) {
        new AlertDialog.Builder(this)
                .setTitle("할 일 삭제")
                .setMessage("‘" + task.title + "’ 일정을 삭제합니다.")
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
        addMenuRow("고객 상태 편집", "고객 상태 이름과 색상 변경", v -> openStageSettings());
        addMenuRow("일정 종류 편집", "전화·미팅·자료 보내기 이름과 색상 변경", v -> openTaskTypeSettings());
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

    private Button smallButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(getColor(R.color.text_primary));
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinWidth(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout verticalCard(boolean clickable) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackgroundResource(clickable ? R.drawable.bg_clickable_card : R.drawable.bg_card);
        card.setClickable(clickable);
        card.setFocusable(clickable);
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
        shape.setColor(selected ? getColor(R.color.primary) : Color.TRANSPARENT);
        if (!selected && today) shape.setStroke(dp(1), getColor(R.color.primary));
        return shape;
    }

    private GradientDrawable calendarPillBackground(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(5));
        shape.setColor(Color.argb(42, Color.red(color), Color.green(color), Color.blue(color)));
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

    private TextView sectionLabel(String value) {
        TextView text = titleText(value, 15f);
        text.setTextColor(getColor(R.color.text_secondary));
        return text;
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

    private long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        clearTime(calendar);
        return calendar.getTimeInMillis();
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
        if (taskTypes != null) taskTypes.close();
        if (db != null) db.close();
        super.onDestroy();
    }
}
