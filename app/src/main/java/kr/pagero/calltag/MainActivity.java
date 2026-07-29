package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new CallTagDbHelper(this);
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
            done.setOnClickListener(v -> {
                db.completeTask(task.id);
                refreshAll();
            });
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
            empty.setPadding(dp(24), dp(30), dp(24), dp(30));
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

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(0, dp(18), 0, dp(18));
        stats.setBackgroundResource(R.drawable.bg_card);
        addMetricColumn(stats, db.countCustomersByStatus(CallTagDbHelper.STATUS_NEW), "신규");
        addDivider(stats);
        addMetricColumn(stats, db.countCustomersByStatus(CallTagDbHelper.STATUS_CONSULTING), "상담 중");
        addDivider(stats);
        addMetricColumn(stats, db.countCustomersByStatus(CallTagDbHelper.STATUS_EXISTING), "기존");
        consultationSummary.addView(stats, matchWrap());

        TextView title = titleText("최근 상담", 18f);
        consultationSummary.addView(title, topMargin(26));
        List<InteractionRecord> records = db.listRecentInteractions(30);
        if (records.isEmpty()) {
            TextView empty = bodyText("상담 기록이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(28), dp(20), dp(28));
            empty.setBackgroundResource(R.drawable.bg_card);
            consultationSummary.addView(empty, topMargin(12));
            return;
        }

        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (InteractionRecord record : records) {
            LinearLayout card = verticalCard();
            card.setOnClickListener(v -> openCustomer(record.customerId));
            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(titleText(record.customerName, 16f), new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            header.addView(mutedText(formatter.format(new Date(record.startedAt))));
            card.addView(header, matchWrap());
            card.addView(bodyText(resultLabel(record.result)), topMargin(7));
            if (record.note != null && !record.note.trim().isEmpty()) {
                card.addView(mutedText(record.note.trim()), topMargin(6));
            }
            LinearLayout.LayoutParams params = matchWrap();
            params.topMargin = dp(10);
            consultationSummary.addView(card, params);
        }
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

    private void addMetricColumn(LinearLayout parent, int value, String label) {
        TextView text = new TextView(this);
        text.setText(value + "\n" + label);
        text.setGravity(Gravity.CENTER);
        text.setIncludeFontPadding(false);
        text.setLineSpacing(0f, 1.35f);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(15f);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        parent.addView(text, new LinearLayout.LayoutParams(0, dp(76), 1f));
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.border));
        parent.addView(divider, new LinearLayout.LayoutParams(dp(1), dp(44)));
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
        return "관심 있음";
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
