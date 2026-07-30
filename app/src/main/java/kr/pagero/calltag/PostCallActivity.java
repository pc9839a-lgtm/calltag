package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class PostCallActivity extends Activity {
    public static final String EXTRA_PENDING_CALL_ID = "pending_call_id";
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_CACHED_NAME = "cached_name";
    public static final String EXTRA_CALL_TYPE = "call_type";
    public static final String EXTRA_STARTED_AT = "started_at";
    public static final String EXTRA_ENDED_AT = "ended_at";
    public static final String EXTRA_DURATION_SEC = "duration_sec";

    private final String[] resultCodes = {
            "INTERESTED", "QUOTE", "CALLBACK", "CONTRACT", "HOLD", "CLOSED"
    };
    private final String[] resultLabels = {
            "관심 있음", "견적 발송", "다시 연락", "거래 완료", "보류", "종료"
    };

    private CallTagDbHelper db;
    private TaskTypeStore taskTypes;
    private Customer existingCustomer;
    private String phone;
    private String callFingerprint;
    private String selectedResult = "INTERESTED";
    private String selectedTaskType = TaskTypeStore.TYPE_CALL;
    private EditText nameInput;
    private EditText noteInput;
    private RadioGroup relationGroup;
    private RadioGroup followUpGroup;
    private LinearLayout resultButtons;
    private Button taskTypeButton;
    private Button saveButton;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_call);
        db = new CallTagDbHelper(this);
        taskTypes = new TaskTypeStore(this);
        phone = getIntent().getStringExtra(EXTRA_PHONE);
        if (phone == null) phone = "";
        existingCustomer = db.findByPhone(phone);
        callFingerprint = buildCallFingerprint();
        bindViews();
        renderHeader();
        renderResultButtons();
        bindActions();
        renderTaskType();
    }

    private void bindViews() {
        nameInput = findViewById(R.id.postCallName);
        noteInput = findViewById(R.id.postCallNote);
        relationGroup = findViewById(R.id.relationGroup);
        followUpGroup = findViewById(R.id.followUpGroup);
        resultButtons = findViewById(R.id.postCallResultButtons);
        taskTypeButton = findViewById(R.id.postCallTaskType);
        saveButton = findViewById(R.id.postCallSave);

        String cachedName = getIntent().getStringExtra(EXTRA_CACHED_NAME);
        if (existingCustomer != null) {
            nameInput.setText(existingCustomer.displayName);
            ((RadioButton) findViewById(R.id.relationExisting)).setChecked(true);
        } else {
            nameInput.setText(cachedName == null ? "" : cachedName);
            ((RadioButton) findViewById(R.id.relationNew)).setChecked(true);
        }
    }

    private void renderHeader() {
        TextView phoneView = findViewById(R.id.postCallPhone);
        TextView metaView = findViewById(R.id.postCallMeta);
        TextView existingInfo = findViewById(R.id.postCallExistingInfo);
        TextView existingMemo = findViewById(R.id.postCallExistingMemo);
        phoneView.setText(phone.isEmpty() ? "번호 없음" : phone);
        int type = getIntent().getIntExtra(EXTRA_CALL_TYPE, CallLog.Calls.INCOMING_TYPE);
        long durationSec = getIntent().getLongExtra(EXTRA_DURATION_SEC, 0L);
        metaView.setText(callTypeLabel(type, durationSec) + " · " + formatDuration(durationSec));
        existingInfo.setText(existingCustomer == null
                ? "등록되지 않은 번호" : statusLabel(existingCustomer.relationStatus));

        if (existingCustomer == null) {
            existingMemo.setVisibility(View.GONE);
        } else {
            String memo = CustomerInsightResolver.latestMemo(db, existingCustomer);
            existingMemo.setVisibility(View.VISIBLE);
            existingMemo.setText(memo.isEmpty()
                    ? "저장된 최근 메모가 없습니다."
                    : "최근 메모 · " + memo);
        }
    }

    private void renderResultButtons() {
        resultButtons.removeAllViews();
        for (int i = 0; i < resultCodes.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            for (int j = 0; j < 2 && i + j < resultCodes.length; j++) {
                int index = i + j;
                String code = resultCodes[index];
                Button button = new Button(this);
                button.setText(resultLabels[index]);
                button.setAllCaps(false);
                button.setTextSize(14f);
                button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                button.setGravity(Gravity.CENTER);
                button.setPadding(dp(8), 0, dp(8), 0);
                button.setTag(code);
                button.setOnClickListener(v -> {
                    if (saving || !v.isEnabled()) return;
                    selectedResult = code;
                    refreshResultButtonStyles();
                    if ("CALLBACK".equals(code)) {
                        selectedTaskType = TaskTypeStore.TYPE_CALL;
                        renderTaskType();
                        ((RadioButton) findViewById(R.id.followTomorrow)).setChecked(true);
                    }
                });

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
                if (j == 1) params.leftMargin = dp(8);
                row.addView(button, params);
            }

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(8);
            resultButtons.addView(row, rowParams);
        }
        refreshResultButtonStyles();
    }

    private void refreshResultButtonStyles() {
        for (int i = 0; i < resultButtons.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) resultButtons.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                Button button = (Button) row.getChildAt(j);
                boolean selected = selectedResult.equals(button.getTag());
                button.setBackgroundResource(selected
                        ? R.drawable.bg_primary_button
                        : R.drawable.bg_secondary_button);
                button.setTextColor(getColor(selected
                        ? R.color.text_primary
                        : R.color.text_secondary));
            }
        }
    }

    private void bindActions() {
        findViewById(R.id.postCallClose).setOnClickListener(v -> {
            if (!saving) finish();
        });
        taskTypeButton.setOnClickListener(v -> showTaskTypeDialog());
        saveButton.setOnClickListener(v -> saveResult());
        relationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean excluded = checkedId == R.id.relationExcluded;
            nameInput.setEnabled(!excluded);
            noteInput.setEnabled(!excluded);
            taskTypeButton.setEnabled(!excluded);
            setEnabledRecursively(resultButtons, !excluded);
            setEnabledRecursively(followUpGroup, !excluded);
            resultButtons.setAlpha(excluded ? 0.35f : 1f);
            followUpGroup.setAlpha(excluded ? 0.35f : 1f);
            taskTypeButton.setAlpha(excluded ? 0.35f : 1f);
        });
    }

    private void showTaskTypeDialog() {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        for (TaskTypeOption type : taskTypes.list()) {
            String subtitle = type.code.equals(selectedTaskType) ? "현재 선택" : "다음 할 일로 선택";
            options.add(new ActionChoiceDialog.Option(type.code, type.name, subtitle, type.color));
        }
        ActionChoiceDialog.show(this, "다음 할 일 종류", "통화 후 해야 할 일을 선택합니다.",
                options, option -> {
                    selectedTaskType = option.key;
                    renderTaskType();
                }, "일정 종류 편집", v -> startActivity(new Intent(this, TaskTypeSettingsActivity.class)));
    }

    private void renderTaskType() {
        TaskTypeOption type = taskTypes.find(selectedTaskType);
        taskTypeButton.setText(type.name + "  ▾");
    }

    private void saveResult() {
        if (saving) return;
        if (PhoneNumberNormalizer.normalize(phone).length() < 8) {
            Toast.makeText(this, "전화번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (SettingsStore.isCallProcessed(this, callFingerprint)) {
            markPendingHandled();
            Toast.makeText(this, "이미 처리한 통화입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setSaving(true);
        if (relationGroup.getCheckedRadioButtonId() == R.id.relationExcluded) {
            try {
                db.addPhoneRule(phone, "EXCLUDED", "사용자 제외");
                SettingsStore.markCallProcessed(this, callFingerprint);
                markPendingHandled();
                Toast.makeText(this, "제외했습니다.", Toast.LENGTH_SHORT).show();
                finish();
            } catch (RuntimeException e) {
                setSaving(false);
                Toast.makeText(this, "저장하지 못했습니다.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        String name = nameInput.getText().toString().trim();
        boolean selectedExisting = relationGroup.getCheckedRadioButtonId() == R.id.relationExisting;
        String initialStage = selectedExisting ? db.completedStage() : db.firstStage();

        try {
            Customer latestCustomer = db.findByPhone(phone);
            long customerId;
            if (latestCustomer == null) {
                customerId = db.insertCustomer(name, phone, initialStage, "");
            } else {
                customerId = latestCustomer.id;
                db.updateCustomer(customerId, name, latestCustomer.relationStatus);
            }

            if ("CONTRACT".equals(selectedResult)) {
                db.markTransactionCompleted(customerId);
            }

            long startedAt = getIntent().getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis());
            long endedAt = getIntent().getLongExtra(EXTRA_ENDED_AT, System.currentTimeMillis());
            long durationSec = Math.max(0L, getIntent().getLongExtra(EXTRA_DURATION_SEC, 0L));
            int type = getIntent().getIntExtra(EXTRA_CALL_TYPE, CallLog.Calls.INCOMING_TYPE);
            long interactionId = db.insertInteraction(
                    customerId,
                    callTypeCode(type),
                    startedAt,
                    Math.max(startedAt, endedAt),
                    durationSec,
                    selectedResult,
                    noteInput.getText().toString().trim());

            long dueAt = selectedFollowUpTime();
            if (dueAt > 0L) {
                TaskTypeOption taskType = taskTypes.find(selectedTaskType);
                db.insertFollowUpTask(customerId, interactionId,
                        taskType.code, taskType.name, dueAt);
            }
            SettingsStore.markCallProcessed(this, callFingerprint);
            markPendingHandled();
            Toast.makeText(this, "저장했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IllegalArgumentException e) {
            setSaving(false);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            setSaving(false);
            Toast.makeText(this, "저장하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void markPendingHandled() {
        long callLogId = getIntent().getLongExtra(EXTRA_PENDING_CALL_ID, -1L);
        if (callLogId <= 0L) return;
        PendingCallStore store = new PendingCallStore(this);
        try {
            store.markHandled(callLogId);
        } finally {
            store.close();
        }
        sendBroadcast(new Intent(PendingCallSectionView.ACTION_CHANGED).setPackage(getPackageName()));
    }

    private void setSaving(boolean value) {
        saving = value;
        saveButton.setEnabled(!value);
        saveButton.setAlpha(value ? 0.55f : 1f);
        saveButton.setText(value ? "저장 중" : "저장");
        relationGroup.setEnabled(!value);
        taskTypeButton.setEnabled(!value);
        nameInput.setEnabled(!value && relationGroup.getCheckedRadioButtonId() != R.id.relationExcluded);
        noteInput.setEnabled(!value && relationGroup.getCheckedRadioButtonId() != R.id.relationExcluded);
    }

    private void setEnabledRecursively(View view, boolean enabled) {
        view.setEnabled(enabled && !saving);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabledRecursively(group.getChildAt(i), enabled);
            }
        }
    }

    private String buildCallFingerprint() {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        long startedAt = getIntent().getLongExtra(EXTRA_STARTED_AT, 0L);
        long durationSec = getIntent().getLongExtra(EXTRA_DURATION_SEC, 0L);
        int type = getIntent().getIntExtra(EXTRA_CALL_TYPE, CallLog.Calls.INCOMING_TYPE);
        return normalized + ":" + startedAt + ":" + durationSec + ":" + type;
    }

    private long selectedFollowUpTime() {
        int checked = followUpGroup.getCheckedRadioButtonId();
        if (checked == R.id.followNone) return -1L;
        long days = checked == R.id.followTomorrow ? 1L
                : checked == R.id.followThreeDays ? 3L : 7L;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, (int) days);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String callTypeCode(int type) {
        if (type == CallLog.Calls.OUTGOING_TYPE) return "OUTGOING_CALL";
        if (type == CallLog.Calls.MISSED_TYPE) return "MISSED_CALL";
        if (type == CallLog.Calls.REJECTED_TYPE) return "REJECTED_CALL";
        return "INCOMING_CALL";
    }

    private String callTypeLabel(int type, long durationSec) {
        if (type == CallLog.Calls.OUTGOING_TYPE && durationSec == 0L) return "발신 · 연결 안 됨";
        if (type == CallLog.Calls.OUTGOING_TYPE) return "발신 통화 완료";
        if (type == CallLog.Calls.MISSED_TYPE) return "부재중";
        if (type == CallLog.Calls.REJECTED_TYPE) return "거절";
        return "수신 통화 완료";
    }

    private String statusLabel(String status) {
        return status == null || status.trim().isEmpty() ? db.firstStage() : status.trim();
    }

    private String formatDuration(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        long minutes = safeSeconds / 60L;
        long remain = safeSeconds % 60L;
        return minutes > 0 ? minutes + "분 " + remain + "초" : remain + "초";
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
