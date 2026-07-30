package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CustomerDetailActivity extends Activity {
    public static final String EXTRA_CUSTOMER_ID = "customer_id";

    private CallTagDbHelper db;
    private TaskTypeStore taskTypes;
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
        taskTypes = new TaskTypeStore(this);
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
        statusView.setText(customer.relationStatus + "  ▾");
        String color = db.stageColor(customer.relationStatus);
        statusView.setBackground(stageBackground(color));
        statusView.setTextColor(contrastTextColor(parseColor(color)));
        if (!memoInput.hasFocus()) memoInput.setText(customer.memo);
        renderTasks();
        renderInteractions();
    }

    private void renderTasks() {
        taskList.removeAllViews();
        List<FollowUpTask> tasks = db.listTasksForCustomer(customerId);
        taskEmpty.setVisibility(tasks.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        SimpleDateFormat formatter = new SimpleDateFormat("M월 d일 E a h:mm", Locale.KOREA);

        for (FollowUpTask task : tasks) {
            TaskTypeOption type = taskTypes.find(task.taskType);
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
            header.addView(typeBadge(type, task.isCompleted()));
            card.addView(header, matchWrap());
            card.addView(text(formatter.format(new Date(task.dueAt)), 13f,
                    task.isOverdue() ? R.color.danger : R.color.text_secondary, false), topMargin(7));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button first = actionButton(task.isCompleted() ? "다시 열기" : "일정 변경", false);
            first.setOnClickListener(v -> {
                if (task.isCompleted()) {
                    db.reopenTask(task.id);
                    loadCustomer();
                } else {
                    showTaskOptions(task);
                }
            });
            actions.addView(first, new LinearLayout.LayoutParams(0, dp(44), 1f));

            Button second;
            if (task.isCompleted()) {
                second = actionButton("삭제", false);
                second.setTextColor(getColor(R.color.danger));
                second.setOnClickListener(v -> confirmDeleteTask(task));
            } else if (TaskTypeStore.TYPE_CALL.equals(type.code)) {
                second = actionButton("전화하기", true);
                second.setOnClickListener(v -> dial());
            } else {
                second = actionButton(completionLabel(type), true);
                second.setOnClickListener(v -> completeTask(task, type.name + " 완료"));
            }
            LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
            secondParams.leftMargin = dp(8);
            actions.addView(second, secondParams);
            card.addView(actions, topMargin(12));

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            taskList.addView(card, params);
        }
    }

    private String completionLabel(TaskTypeOption type) {
        if (TaskTypeStore.TYPE_SEND.equals(type.code)) return "자료 보냄";
        if (TaskTypeStore.TYPE_MEETING.equals(type.code)) return "미팅 완료";
        return type.name.length() > 8 ? "처리 완료" : type.name + " 완료";
    }

    private TextView typeBadge(TaskTypeOption type, boolean completed) {
        TextView badge = text(type.name, 12f, R.color.text_primary, true);
        int color = completed ? getColor(R.color.text_muted) : parseColor(type.color);
        badge.setTextColor(contrastTextColor(color));
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(11));
        badge.setBackground(shape);
        return badge;
    }

    private void renderInteractions() {
        interactionList.removeAllViews();
        List<InteractionRecord> records = db.listInteractionsForCustomer(customerId);
        interactionEmpty.setVisibility(records.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
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
                || "TASK_COMPLETE".equals(record.type)
                || "TASK_AUTO_COMPLETE".equals(record.type)) return "";
        return " · " + durationLabel(record.durationSec);
    }

    private void saveMemo() {
        if (customer == null) return;
        db.updateCustomerProfile(customer.id, customer.displayName,
                customer.relationStatus, memoInput.getText().toString());
        Toast.makeText(this, "메모를 저장했습니다.", Toast.LENGTH_SHORT).show();
        memoInput.clearFocus();
        loadCustomer();
    }

    private void showStatusDialog() {
        if (customer == null) return;
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        for (StageOption stage : db.listStages()) {
            options.add(new ActionChoiceDialog.Option(stage.name, stage.name,
                    stage.name.equals(customer.relationStatus) ? "현재 상태" : "이 상태로 변경",
                    stage.color));
        }
        ActionChoiceDialog.show(this, "고객 상태 변경", customer.displayName,
                options, option -> {
                    String oldStatus = customer.relationStatus;
                    String newStatus = option.key;
                    if (!oldStatus.equals(newStatus)) {
                        db.updateCustomerProfile(customer.id, customer.displayName,
                                newStatus, memoInput.getText().toString());
                        long now = System.currentTimeMillis();
                        db.insertInteraction(customer.id, "STATUS_CHANGE", now, now, 0L,
                                "STATUS_" + newStatus, oldStatus + " → " + newStatus);
                        Toast.makeText(this, newStatus + "으로 변경했습니다.", Toast.LENGTH_SHORT).show();
                    }
                    loadCustomer();
                }, "고객 상태·색상 편집", v ->
                        startActivity(new Intent(this, StageSettingsActivity.class)));
    }

    private void showScheduleTypeDialog() {
        if (customer == null) return;
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        for (TaskTypeOption type : taskTypes.list()) {
            options.add(new ActionChoiceDialog.Option(type.code, type.name,
                    "이 종류로 할 일 등록", type.color));
        }
        ActionChoiceDialog.show(this, "할 일 종류 선택", customer.displayName,
                options, option -> {
                    TaskTypeOption type = taskTypes.find(option.key);
                    showNewScheduleDatePicker(type.name, type.code);
                }, "일정 종류 편집", v ->
                        startActivity(new Intent(this, TaskTypeSettingsActivity.class)));
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
        long now = System.currentTimeMillis();
        db.insertInteraction(customer.id, "SCHEDULE_CREATE", now, now, 0L,
                "SCHEDULED", title);
        Toast.makeText(this, "할 일을 추가했습니다.", Toast.LENGTH_SHORT).show();
        loadCustomer();
    }

    private void showTaskOptions(FollowUpTask task) {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        options.add(new ActionChoiceDialog.Option("RESCHEDULE", "날짜·시간 변경",
                "일정 시간을 다시 선택", "#4389FF"));
        options.add(new ActionChoiceDialog.Option("DELETE", "일정 삭제",
                "이 할 일을 삭제", "#F97066"));
        ActionChoiceDialog.show(this, task.title, null, options, option -> {
            if ("RESCHEDULE".equals(option.key)) showRescheduleDatePicker(task);
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

    private void completeTask(FollowUpTask task, String message) {
        db.completeTask(task.id);
        long now = System.currentTimeMillis();
        db.insertInteraction(customerId, "TASK_COMPLETE", now, now, 0L,
                "TASK_COMPLETED", message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        loadCustomer();
    }

    private void confirmDeleteTask(FollowUpTask task) {
        new AlertDialog.Builder(this)
                .setTitle("할 일 삭제")
                .setMessage("‘" + task.title + "’ 일정을 삭제합니다.")
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

    private GradientDrawable stageBackground(String colorHex) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(12));
        shape.setColor(parseColor(colorHex));
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

    private String resultLabel(String result) {
        if ("QUOTE".equals(result)) return "견적·자료 발송";
        if ("CALLBACK".equals(result)) return "다시 연락";
        if ("CONTRACT".equals(result)) return "계약·거래 완료";
        if ("HOLD".equals(result)) return "보류";
        if ("CLOSED".equals(result)) return "상담 종료";
        if ("SCHEDULED".equals(result)) return "할 일 등록·변경";
        if ("TASK_COMPLETED".equals(result)) return "할 일 완료";
        if ("CALL_COMPLETED".equals(result)) return "전화 할 일 자동 완료";
        if (result != null && result.startsWith("STATUS_")) {
            return "상태 변경 · " + result.substring("STATUS_".length());
        }
        return "관심 있음";
    }

    private String typeLabel(String type) {
        if ("OUTGOING_CALL".equals(type)) return "발신 통화";
        if ("MISSED_CALL".equals(type)) return "부재중";
        if ("REJECTED_CALL".equals(type)) return "거절";
        if ("STATUS_CHANGE".equals(type)) return "고객 상태";
        if ("SCHEDULE_CREATE".equals(type)) return "할 일 등록";
        if ("SCHEDULE_CHANGE".equals(type)) return "할 일 변경";
        if ("TASK_COMPLETE".equals(type)) return "할 일 처리";
        if ("TASK_AUTO_COMPLETE".equals(type)) return "통화 자동 처리";
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
        if (taskTypes != null) taskTypes.close();
        if (db != null) db.close();
        super.onDestroy();
    }
}
