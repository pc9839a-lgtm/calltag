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
    private TextView todayEmpty;
    private TextView monitorStateText;
    private Button enableMonitorButton;
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

        if (hasMonitorPermissions() && SettingsStore.isMonitorEnabled(this)) {
            startMonitorService();
        }
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
        findViewById(R.id.addCustomerButton).setOnClickListener(v -> showAddCustomerDialog());
        findViewById(R.id.filterAll).setOnClickListener(v -> setCustomerFilter(null));
        findViewById(R.id.filterNew).setOnClickListener(v -> setCustomerFilter(CallTagDbHelper.STATUS_NEW));
        findViewById(R.id.filterConsulting).setOnClickListener(v -> setCustomerFilter(CallTagDbHelper.STATUS_CONSULTING));
        findViewById(R.id.filterExisting).setOnClickListener(v -> setCustomerFilter(CallTagDbHelper.STATUS_EXISTING));
    }

    private void selectSection(View selectedSection, NavItemTextView selectedNav) {
        sectionToday.setVisibility(selectedSection == sectionToday ? View.VISIBLE : View.GONE);
        sectionCustomers.setVisibility(selectedSection == sectionCustomers ? View.VISIBLE : View.GONE);
        sectionConsultations.setVisibility(selectedSection == sectionConsultations ? View.VISIBLE : View.GONE);
        sectionMore.setVisibility(selectedSection == sectionMore ? View.VISIBLE : View.GONE);

        int active = getColor(R.color.primary);
        int inactive = getColor(R.color.nav_inactive);
        navToday.setTextColor(selectedNav == navToday ? active : inactive);
        navCustomers.setTextColor(selectedNav == navCustomers ? active : inactive);
        navConsultations.setTextColor(selectedNav == navConsultations ? active : inactive);
        navMore.setTextColor(selectedNav == navMore ? active : inactive);
        refreshAll();
    }

    private void refreshAll() {
        todayDueCount.setText(String.valueOf(db.countDueTodayTasks()));
        overdueCount.setText(String.valueOf(db.countOverdueTasks()));
        renderMonitorState();
        renderTasks();
        renderCustomers();
        renderConsultationSummary();
        renderMoreMenu();
    }

    private void toggleMonitor() {
        if (!hasMonitorPermissions()) {
            requestMonitorPermissions();
            return;
        }
        if (SettingsStore.isMonitorEnabled(this)) stopMonitorService();
        else startMonitorService();
        renderMonitorState();
    }

    private void requestMonitorPermissions() {
        List<String> permissions = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CALL_LOG);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (permissions.isEmpty()) {
            startMonitorService();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("통화 감지 권한이 필요합니다")
                .setMessage("전화 상태와 최근 통화번호를 확인해야 통화가 끝난 뒤 신규·기존 고객 분류 화면을 표시할 수 있습니다. 통화 내용은 녹음하지 않습니다.")
                .setNegativeButton("나중에", null)
                .setPositiveButton("권한 허용", (dialog, which) -> requestPermissions(
                        permissions.toArray(new String[0]), REQUEST_MONITOR_PERMISSIONS))
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_MONITOR_PERMISSIONS) return;
        if (hasMonitorPermissions()) {
            startMonitorService();
            Toast.makeText(this, "통화 감지를 시작했습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "통화 감지에 필요한 권한이 허용되지 않았습니다.", Toast.LENGTH_LONG).show();
        }
        renderMonitorState();
    }

    private boolean hasMonitorPermissions() {
        boolean core = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            core = core && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return core;
    }

    private void startMonitorService() {
        SettingsStore.setMonitorEnabled(this, true);
        Intent service = new Intent(this, CallMonitorService.class).setAction(CallMonitorService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
            else startService(service);
        } catch (RuntimeException e) {
            SettingsStore.setMonitorEnabled(this, false);
            Toast.makeText(this, "통화 감지를 시작하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopMonitorService() {
        SettingsStore.setMonitorEnabled(this, false);
        stopService(new Intent(this, CallMonitorService.class));
        Toast.makeText(this, "통화 감지를 중지했습니다.", Toast.LENGTH_SHORT).show();
    }

    private void renderMonitorState() {
        if (!hasMonitorPermissions()) {
            monitorStateText.setText("권한을 허용하면 통화 종료 후 고객 분류가 시작됩니다.");
            enableMonitorButton.setText("권한 허용");
            return;
        }
        boolean enabled = SettingsStore.isMonitorEnabled(this);
        monitorStateText.setText(enabled
                ? "실행 중 · 통화가 끝나면 정리 알림이 표시됩니다."
                : "중지됨 · 통화 종료를 감지하지 않습니다.");
        enableMonitorButton.setText(enabled ? "끄기" : "켜기");
        enableMonitorButton.setBackgroundResource(enabled
                ? R.drawable.bg_secondary_button : R.drawable.bg_primary_button);
    }

    private void renderTasks() {
        todayTaskList.removeAllViews();
        List<FollowUpTask> tasks = db.listPendingTasks();
        todayEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        for (FollowUpTask task : tasks) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            card.setBackgroundResource(R.drawable.bg_card);

            TextView name = new TextView(this);
            name.setText(task.customerName);
            name.setTextColor(getColor(R.color.text_primary));
            name.setTextSize(17f);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(name, matchWrap());

            TextView title = createBodyText(task.title);
            LinearLayout.LayoutParams titleParams = matchWrap();
            titleParams.topMargin = dp(7);
            card.addView(title, titleParams);

            TextView due = createMutedText((task.isOverdue() ? "기한 지남 · " : "예정 · ")
                    + formatter.format(new Date(task.dueAt)));
            due.setTextColor(getColor(task.isOverdue() ? R.color.danger : R.color.text_muted));
            LinearLayout.LayoutParams dueParams = matchWrap();
            dueParams.topMargin = dp(6);
            card.addView(due, dueParams);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL);

            Button call = smallButton("전화");
            call.setOnClickListener(v -> dial(task.phone));
            actions.addView(call, new LinearLayout.LayoutParams(0, dp(44), 1f));

            Button complete = smallButton("완료");
            complete.setBackgroundResource(R.drawable.bg_primary_button);
            LinearLayout.LayoutParams completeParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
            completeParams.leftMargin = dp(9);
            actions.addView(complete, completeParams);
            complete.setOnClickListener(v -> {
                db.completeTask(task.id);
                refreshAll();
                Toast.makeText(this, "완료 처리했습니다.", Toast.LENGTH_SHORT).show();
            });

            LinearLayout.LayoutParams actionParams = matchWrap();
            actionParams.topMargin = dp(14);
            card.addView(actions, actionParams);

            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.bottomMargin = dp(12);
            todayTaskList.addView(card, cardParams);
        }
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextAllCaps(false);
        button.setTextColor(getColor(R.color.text_primary));
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_secondary_button);
        return button;
    }

    private void setCustomerFilter(String status) {
        activeCustomerFilter = status;
        renderCustomers();
        setFilterColor(R.id.filterAll, status == null);
        setFilterColor(R.id.filterNew, CallTagDbHelper.STATUS_NEW.equals(status));
        setFilterColor(R.id.filterConsulting, CallTagDbHelper.STATUS_CONSULTING.equals(status));
        setFilterColor(R.id.filterExisting, CallTagDbHelper.STATUS_EXISTING.equals(status));
    }

    private void setFilterColor(int viewId, boolean selected) {
        Button button = findViewById(viewId);
        button.setTextColor(getColor(selected ? R.color.primary : R.color.text_secondary));
    }

    private void renderCustomers() {
        customerList.removeAllViews();
        List<Customer> customers = db.listCustomers(activeCustomerFilter);
        if (customers.isEmpty()) {
            TextView empty = createBodyText(activeCustomerFilter == null
                    ? "아직 등록된 고객이 없습니다.\n통화가 끝난 뒤 신규 또는 기존 고객으로 저장할 수 있습니다."
                    : "해당 상태의 고객이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(30), dp(24), dp(30));
            empty.setBackgroundResource(R.drawable.bg_card);
            customerList.addView(empty, matchWrap());
            return;
        }

        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        for (Customer customer : customers) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(17), dp(18), dp(17));
            card.setBackgroundResource(R.drawable.bg_card);
            card.setOnClickListener(v -> showCustomerActions(customer));

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView name = new TextView(this);
            name.setText(customer.displayName);
            name.setTextColor(getColor(R.color.text_primary));
            name.setTextSize(17f);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            name.setIncludeFontPadding(false);
            header.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView badge = new TextView(this);
            badge.setText(statusLabel(customer.relationStatus));
            badge.setTextColor(statusColor(customer.relationStatus));
            badge.setTextSize(12f);
            badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(10), dp(6), dp(10), dp(6));
            badge.setBackgroundResource(R.drawable.bg_badge);
            header.addView(badge);
            card.addView(header, matchWrap());

            TextView phone = createBodyText(customer.primaryPhone);
            LinearLayout.LayoutParams phoneParams = matchWrap();
            phoneParams.topMargin = dp(11);
            card.addView(phone, phoneParams);

            TextView meta = createMutedText("최근 상담  " + dateFormat.format(new Date(customer.lastContactAt)));
            LinearLayout.LayoutParams metaParams = matchWrap();
            metaParams.topMargin = dp(6);
            card.addView(meta, metaParams);

            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.bottomMargin = dp(12);
            customerList.addView(card, cardParams);
        }
    }

    private void showCustomerActions(Customer customer) {
        String[] actions = {"전화 걸기", "상담 중으로 변경", "기존 고객으로 변경"};
        new AlertDialog.Builder(this)
                .setTitle(customer.displayName)
                .setMessage(customer.primaryPhone + "\n현재 상태: " + statusLabel(customer.relationStatus))
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) dial(customer.primaryPhone);
                    if (which == 1) {
                        db.updateCustomer(customer.id, customer.displayName, CallTagDbHelper.STATUS_CONSULTING);
                        refreshAll();
                    }
                    if (which == 2) {
                        db.updateCustomer(customer.id, customer.displayName, CallTagDbHelper.STATUS_EXISTING);
                        refreshAll();
                    }
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void renderConsultationSummary() {
        consultationSummary.removeAllViews();
        addMetricCard(consultationSummary, "미완료 다음 연락", db.countPendingTasks() + "건", false);
        addMetricCard(consultationSummary, "신규 고객", db.countCustomersByStatus(CallTagDbHelper.STATUS_NEW) + "명", false);
        addMetricCard(consultationSummary, "상담 중 고객", db.countCustomersByStatus(CallTagDbHelper.STATUS_CONSULTING) + "명", false);
        addMetricCard(consultationSummary, "기존 고객", db.countCustomersByStatus(CallTagDbHelper.STATUS_EXISTING) + "명", false);
    }

    private void renderMoreMenu() {
        moreMenuList.removeAllViews();
        addMenuCard("통화 후 처리 테스트", "실제 전화 없이 신규·기존 고객 분류 화면을 확인합니다.", v -> openPostCallTest());
        addMenuCard("통화 감지 설정", hasMonitorPermissions()
                ? (SettingsStore.isMonitorEnabled(this) ? "현재 실행 중입니다." : "현재 중지되어 있습니다.")
                : "전화·통화기록·알림 권한이 필요합니다.", v -> toggleMonitor());
        addMenuCard("고객 분류 설정", "신규·상담 중·기존 고객 상태는 고객 카드에서 변경합니다.", null);
        addMenuCard("제외번호", "통화 후 처리 화면에서 개인전화·거래처·스팸 번호를 제외할 수 있습니다.", null);
        addMenuCard("개인정보 및 데이터", "통화 내용은 녹음하지 않으며 고객 데이터는 휴대전화 내부에 저장합니다.", null);
    }

    private void openPostCallTest() {
        Intent intent = new Intent(this, PostCallActivity.class)
                .putExtra(PostCallActivity.EXTRA_PHONE, "010-0000-1234")
                .putExtra(PostCallActivity.EXTRA_CACHED_NAME, "")
                .putExtra(PostCallActivity.EXTRA_CALL_TYPE, CallLog.Calls.INCOMING_TYPE)
                .putExtra(PostCallActivity.EXTRA_STARTED_AT, System.currentTimeMillis() - 185_000L)
                .putExtra(PostCallActivity.EXTRA_ENDED_AT, System.currentTimeMillis())
                .putExtra(PostCallActivity.EXTRA_DURATION_SEC, 185L);
        startActivity(intent);
    }

    private void addMetricCard(LinearLayout parent, String label, String value, boolean danger) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundResource(R.drawable.bg_card);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(getColor(R.color.text_primary));
        labelView.setTextSize(16f);
        card.addView(labelView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(getColor(danger ? R.color.danger : R.color.text_primary));
        valueView.setTextSize(22f);
        valueView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(valueView);

        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(12);
        parent.addView(card, params);
    }

    private void addMenuCard(String title, String description, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackgroundResource(R.drawable.bg_card);
        if (listener != null) card.setOnClickListener(listener);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(getColor(R.color.text_primary));
        titleView.setTextSize(16f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(titleView, matchWrap());

        TextView descriptionView = createMutedText(description);
        LinearLayout.LayoutParams descriptionParams = matchWrap();
        descriptionParams.topMargin = dp(7);
        card.addView(descriptionView, descriptionParams);

        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(12);
        moreMenuList.addView(card, params);
    }

    private void showAddCustomerDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(6), dp(20), 0);
        EditText nameInput = createInput("고객명 또는 업체명", InputType.TYPE_CLASS_TEXT);
        form.addView(nameInput, matchWrap());
        EditText phoneInput = createInput("전화번호", InputType.TYPE_CLASS_PHONE);
        LinearLayout.LayoutParams phoneParams = matchWrap();
        phoneParams.topMargin = dp(12);
        form.addView(phoneInput, phoneParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("신규 고객 등록")
                .setMessage("거래 완료 전까지 신규 고객으로 분류합니다.")
                .setView(form)
                .setNegativeButton("취소", null)
                .setPositiveButton("등록", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                db.insertNewLead(nameInput.getText().toString(), phoneInput.getText().toString());
                Toast.makeText(this, "신규 고객으로 등록했습니다.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                setCustomerFilter(null);
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (RuntimeException e) {
                Toast.makeText(this, "고객을 등록하지 못했습니다.", Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private void dial(String phone) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
        } catch (RuntimeException e) {
            Toast.makeText(this, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private EditText createInput(String hint, int inputType) {
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

    private TextView createBodyText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(15f);
        text.setLineSpacing(0f, 1.3f);
        return text;
    }

    private TextView createMutedText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_muted));
        text.setTextSize(13f);
        return text;
    }

    private String statusLabel(String status) {
        switch (status) {
            case CallTagDbHelper.STATUS_NEW: return "신규";
            case CallTagDbHelper.STATUS_CONSULTING: return "상담 중";
            case CallTagDbHelper.STATUS_EXISTING: return "기존";
            case CallTagDbHelper.STATUS_VIP: return "VIP";
            case CallTagDbHelper.STATUS_DORMANT: return "휴면";
            case CallTagDbHelper.STATUS_OPT_OUT: return "수신거부";
            case CallTagDbHelper.STATUS_EXCLUDED: return "제외";
            default: return status;
        }
    }

    private int statusColor(String status) {
        if (CallTagDbHelper.STATUS_NEW.equals(status)) return getColor(R.color.primary);
        if (CallTagDbHelper.STATUS_EXISTING.equals(status)) return getColor(R.color.text_primary);
        return getColor(R.color.text_secondary);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
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
