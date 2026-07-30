package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CustomerDetailActivity extends Activity {
    public static final String EXTRA_CUSTOMER_ID = "customer_id";

    private CallTagDbHelper db;
    private long customerId;
    private Customer customer;
    private TextView nameView;
    private TextView phoneView;
    private TextView statusView;
    private EditText memoInput;
    private LinearLayout taskList;
    private TextView taskEmpty;
    private LinearLayout interactionList;
    private TextView interactionEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);
        db = new CallTagDbHelper(this);
        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, -1L);
        bindViews();
        bindActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomer();
    }

    private void bindViews() {
        nameView = findViewById(R.id.detailName);
        phoneView = findViewById(R.id.detailPhone);
        statusView = findViewById(R.id.detailStatus);
        memoInput = findViewById(R.id.detailMemo);
        taskList = findViewById(R.id.detailTaskList);
        taskEmpty = findViewById(R.id.detailTaskEmpty);
        interactionList = findViewById(R.id.detailInteractionList);
        interactionEmpty = findViewById(R.id.detailInteractionEmpty);
    }

    private void bindActions() {
        findViewById(R.id.detailBack).setOnClickListener(v -> finish());
        findViewById(R.id.detailCall).setOnClickListener(v -> dial());
        findViewById(R.id.detailSchedule).setOnClickListener(v -> showScheduleTypeDialog());
        findViewById(R.id.detailScheduleShortcut).setOnClickListener(v -> showScheduleTypeDialog());
        findViewById(R.id.detailChangeStatus).setOnClickListener(v -> showStatusDialog());
        statusView.setOnClickListener(v -> showStatusDialog());
        findViewById(R.id.detailSaveMemo).setOnClickListener(v -> saveMemo());
    }

    private void loadCustomer() {
        customer = db.findCustomerById(customerId);
        if (customer == null) {
            Toast.makeText(this, "고객 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        nameView.setText(customer.displayName);
        phoneView.setText(customer.primaryPhone);
        statusView.setText(statusLabel(customer.relationStatus) + "  ›");
        statusView.setTextColor(getColor(statusColor(customer.relationStatus)));
        if (!memoInput.hasFocus()) memoInput.setText(customer.memo);
        renderTasks();
        renderInteractions();
    }

    private void renderTasks() {
        taskList.removeAllViews();
        List<FollowUpTask> tasks = db.listTasksForCustomer(customerId);
        taskEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        SimpleDateFormat formatter = new SimpleDateFormat("M월 d일 E a h:mm", Locale.KOREA);

        for (FollowUpTask task : tasks) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(15), dp(18), dp(15));
            card.setBackgroundResource(R.drawable.bg_card);

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = text(task.title, 16f, task.isCompleted()
                    ? R.color.text_muted : R.color.text_primary, true);
            header.addView(title, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView state = text(task.isCompleted() ? "완료" : "예정", 12f,
                    task.isCompleted() ? R.color.text_muted : R.color.primary, true);
            state.setPadding(dp(10), dp(5), dp(10), dp(5));
            state.setBackgroundResource(R.drawable.bg_badge);
            header.addView(state);
            card.addView(header, matchWrap());
            card.addView(text(formatter.format(new Date(task.dueAt)), 13f,
                    task.isOverdue() ? R.color.danger : R.color.text_secondary, false), topMargin(7));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button first = actionButton(task.isCompleted() ? "다시 열기" : "변경", false);
            first.setOnClickListener(v -> {
                if (task.isCompleted()) {
                    db.reopenTask(task.id);
                    loadCustomer();
                } else {
                    showTaskOptions(task);
                }
            });
            actions.addView(first, new LinearLayout.LayoutParams(0, dp(42), 1f));

            Button second = actionButton(task.isCompleted() ? "삭제" : "완료", !task.isCompleted());
            second.setOnClickListener(v -> {
                if (task.isCompleted()) confirmDeleteTask(task);
                else completeTask(task);
            });
            LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
            secondParams.leftMargin = dp(8);
            actions.addView(second, secondParams);
            card.addView(actions, topMargin(12));

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            taskList.addView(card, params);
        }
    }

    private void renderInteractions() {
        interactionList.removeAllViews();
        List<InteractionRecord> records = db.listInteractionsForCustomer(customerId);
        interactionEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        for (InteractionRecord record : records) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            card.setBackgroundResource(R.drawable.bg_card);

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView result = text(resultLabel(record.result), 16f, R.color.text_primary, true);
            header.addView(result, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView date = text(formatter.format(new Date(record.startedAt)), 12f, R.color.text_muted, false);
            header.addView(date);
            card.addView(header, matchWrap());

            TextView meta = text(typeLabel(record.type) + durationSuffix(record),
                    13f, R.color.text_secondary, false);
            card.addView(meta, topMargin(7));

            if (record.note != null && !record.note.trim().isEmpty()) {
                TextView note = text(record.note.trim(), 14f, R.color.text_primary, false);
                note.setLineSpacing(0f, 1.25f);
                card.addView(note, topMargin(10));
            }

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            interactionList.addView(card, params);
        }
    }

    private String durationSuffix(InteractionRecord record) {
        if ("STATUS_CHANGE".equals(record.type)
                || record.type.startsWith("SCHEDULE_")
                || "TASK_COMPLETE".equals(record.type)) return "";
        return " · " + durationLabel(record.durationSec);
    }

    private void saveMemo() {
        if (customer == null) return;
        db.updateCustomerProfile(customer.id, customer.displayName,
                customer.relationStatus, memoInput.getText().toString());
        Toast.makeText(this, "저장했습니다.", Toast.LENGTH_SHORT).show();
        memoInput.clearFocus();
        loadCustomer();
    }

    private void showStatusDialog() {
        if (customer == null) return;
        String[] labels = {"신규", "상담 중", "기존", "VIP", "휴면"};
        String[] values = {
                CallTagDbHelper.STATUS_NEW,
                CallTagDbHelper.STATUS_CONSULTING,
                CallTagDbHelper.STATUS_EXISTING,
                CallTagDbHelper.STATUS_VIP,
                CallTagDbHelper.STATUS_DORMANT
        };
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(customer.relationStatus)) selected = i;
        }
        new AlertDialog.Builder(this)
                .setTitle("고객 상태 변경")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    String oldStatus = customer.relationStatus;
                    String newStatus = values[which];
                    if (!oldStatus.equals(newStatus)) {
                        db.updateCustomerProfile(customer.id, customer.displayName,
                                newStatus, memoInput.getText().toString());
                        long now = System.currentTimeMillis();
                        db.insertInteraction(customer.id, "STATUS_CHANGE", now, now, 0L,
                                "STATUS_" + newStatus,
                                statusLabel(oldStatus) + " → " + statusLabel(newStatus));
                        Toast.makeText(this, statusLabel(newStatus) + "으로 변경했습니다.",
                                Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                    loadCustomer();
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void showScheduleTypeDialog() {
        if (customer == null) return;
        String[] labels = {"전화하기", "방문·미팅", "자료 보내기", "직접 입력"};
        String[] types = {"CALL", "MEETING", "SEND", "CUSTOM"};
        new AlertDialog.Builder(this)
                .setTitle("일정 종류")
                .setItems(labels, (dialog, which) -> {
                    if (which == 3) showCustomScheduleTitle();
                    else showNewScheduleDatePicker(labels[which], types[which]);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showCustomScheduleTitle() {
        EditText input = new EditText(this);
        input.setHint("일정 내용");
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        input.setBackgroundResource(R.drawable.bg_input);
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
                    showNewScheduleDatePicker(title, "CUSTOM");
                }));
        dialog.show();
    }

    private void showNewScheduleDatePicker(String title, String taskType) {
        Calendar defaultDate = Calendar.getInstance();
        defaultDate.add(Calendar.DAY_OF_MONTH, 1);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar due = Calendar.getInstance();
            due.set(year, month, dayOfMonth, 10, 0, 0);
            due.set(Calendar.MILLISECOND, 0);
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                due.set(Calendar.HOUR_OF_DAY, hourOfDay);
                due.set(Calendar.MINUTE, minute);
                createSchedule(title, taskType, due.getTimeInMillis());
            }, 10, 0, false).show();
        }, defaultDate.get(Calendar.YEAR), defaultDate.get(Calendar.MONTH),
                defaultDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void createSchedule(String title, String taskType, long dueAt) {
        if (customer == null) return;
        db.insertFollowUpTask(customer.id, 0L, taskType, title, dueAt);
        String statusNote = "";
        if (CallTagDbHelper.STATUS_NEW.equals(customer.relationStatus)) {
            db.updateCustomer(customer.id, customer.displayName, CallTagDbHelper.STATUS_CONSULTING);
            statusNote = " · 신규 → 상담 중";
        }
        long now = System.currentTimeMillis();
        db.insertInteraction(customer.id, "SCHEDULE_CREATE", now, now, 0L,
                "SCHEDULED", title + statusNote);
        Toast.makeText(this, "일정을 추가했습니다.", Toast.LENGTH_SHORT).show();
        loadCustomer();
    }

    private void showTaskOptions(FollowUpTask task) {
        String[] actions = {"날짜·시간 변경", "완료", "삭제"};
        new AlertDialog.Builder(this)
                .setTitle(task.title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) showRescheduleDatePicker(task);
                    if (which == 1) completeTask(task);
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
                long now = System.currentTimeMillis();
                db.insertInteraction(customerId, "SCHEDULE_CHANGE", now, now, 0L,
                        "SCHEDULED", task.title + " 일정 변경");
                Toast.makeText(this, "일정을 변경했습니다.", Toast.LENGTH_SHORT).show();
                loadCustomer();
            }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), false).show();
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void completeTask(FollowUpTask task) {
        db.completeTask(task.id);
        long now = System.currentTimeMillis();
        db.insertInteraction(customerId, "TASK_COMPLETE", now, now, 0L,
                "TASK_COMPLETED", task.title + " 완료");
        Toast.makeText(this, "일정을 완료했습니다.", Toast.LENGTH_SHORT).show();
        loadCustomer();
    }

    private void confirmDeleteTask(FollowUpTask task) {
        new AlertDialog.Builder(this)
                .setTitle("일정 삭제")
                .setMessage(task.title)
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.deleteTask(task.id);
                    Toast.makeText(this, "삭제했습니다.", Toast.LENGTH_SHORT).show();
                    loadCustomer();
                })
                .show();
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(getColor(R.color.text_primary));
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private void dial() {
        if (customer == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.primaryPhone)));
        } catch (RuntimeException e) {
            Toast.makeText(this, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String statusLabel(String status) {
        if (CallTagDbHelper.STATUS_CONSULTING.equals(status)) return "상담 중";
        if (CallTagDbHelper.STATUS_EXISTING.equals(status)) return "기존";
        if (CallTagDbHelper.STATUS_VIP.equals(status)) return "VIP";
        if (CallTagDbHelper.STATUS_DORMANT.equals(status)) return "휴면";
        return "신규";
    }

    private int statusColor(String status) {
        if (CallTagDbHelper.STATUS_NEW.equals(status)) return R.color.primary;
        if (CallTagDbHelper.STATUS_EXISTING.equals(status)) return R.color.text_primary;
        return R.color.text_secondary;
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

    private String typeLabel(String type) {
        if ("OUTGOING_CALL".equals(type)) return "발신 통화";
        if ("MISSED_CALL".equals(type)) return "부재중";
        if ("REJECTED_CALL".equals(type)) return "거절";
        if ("STATUS_CHANGE".equals(type)) return "고객 상태";
        if ("SCHEDULE_CREATE".equals(type)) return "일정 등록";
        if ("SCHEDULE_CHANGE".equals(type)) return "일정 변경";
        if ("TASK_COMPLETE".equals(type)) return "일정 처리";
        return "수신 통화";
    }

    private String durationLabel(long seconds) {
        long minutes = seconds / 60L;
        long remain = seconds % 60L;
        return minutes > 0 ? minutes + "분 " + remain + "초" : remain + "초";
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
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
