package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class PostCallActivity extends Activity {
    public static final String EXTRA_PENDING_CALL_ID = "pending_call_id";
    public static final String EXTRA_CALL_LOG_ID = "call_log_id";
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_CACHED_NAME = "cached_name";
    public static final String EXTRA_CALL_TYPE = "call_type";
    public static final String EXTRA_STARTED_AT = "started_at";
    public static final String EXTRA_ENDED_AT = "ended_at";
    public static final String EXTRA_DURATION_SEC = "duration_sec";

    private static final int REQUEST_TEMPLATE = 8201;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

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
    private String selectedStage;
    private int delayedMessageDays;

    private String selectedMessagePurpose = MessageTemplateStore.PURPOSE_INCOMING;
    private String selectedTemplateId = "";
    private String selectedTemplateName = "";
    private String selectedTemplateBody = "";
    private boolean applyingTemplate;
    private boolean messageEditedByUser;
    private String lastSendNotice = "";
    private String lastScheduleNotice = "";

    private EditText nameInput;
    private EditText noteInput;
    private EditText messageInput;
    private LinearLayout resultButtons;
    private View messageSection;
    private View rootView;
    private Button stageButton;
    private Button taskTypeButton;
    private Button delayedMessageButton;
    private Button templateChangeButton;
    private Button saveOnlyButton;
    private Button saveAndSendButton;
    private TextView templateNameView;
    private TextView templateMetaView;
    private TextView messageGuideView;
    private RadioGroup followUpGroup;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_call);
        db = new CallTagDbHelper(this);
        taskTypes = new TaskTypeStore(this);
        MessageAutomationStore.ensureDefaults(this);
        MessageTemplateStore.ensureDefaults(this);

        phone = safe(getIntent().getStringExtra(EXTRA_PHONE));
        existingCustomer = db.findByPhone(phone);
        selectedStage = existingCustomer == null
                ? db.firstStage() : existingCustomer.relationStatus;
        callFingerprint = buildCallFingerprint();

        bindViews();
        bindTextWatchers();
        renderHeader();
        renderResultButtons();
        renderStage();
        renderTaskType();
        renderDelayedMessage();
        renderMessageSection();
        bindActions();
        rootView.requestFocus();
    }

    private void bindViews() {
        rootView = findViewById(R.id.postCallRoot);
        nameInput = findViewById(R.id.postCallName);
        noteInput = findViewById(R.id.postCallNote);
        messageInput = findViewById(R.id.postCallMessageBody);
        resultButtons = findViewById(R.id.postCallResultButtons);
        messageSection = findViewById(R.id.postCallMessageSection);
        stageButton = findViewById(R.id.postCallStage);
        taskTypeButton = findViewById(R.id.postCallTaskType);
        delayedMessageButton = findViewById(R.id.postCallDelayedMessage);
        templateChangeButton = findViewById(R.id.postCallTemplateChange);
        templateNameView = findViewById(R.id.postCallTemplateName);
        templateMetaView = findViewById(R.id.postCallTemplateMeta);
        messageGuideView = findViewById(R.id.postCallMessageGuide);
        saveOnlyButton = findViewById(R.id.postCallSaveOnly);
        saveAndSendButton = findViewById(R.id.postCallSaveAndSend);
        followUpGroup = findViewById(R.id.followUpGroup);

        String cachedName = safe(getIntent().getStringExtra(EXTRA_CACHED_NAME)).trim();
        if (existingCustomer != null) {
            nameInput.setText(existingCustomer.displayName);
        } else if (!cachedName.isEmpty() && !"이름없는고객".equals(cachedName)) {
            nameInput.setText(cachedName);
        } else {
            nameInput.setText(defaultCustomerName(phone));
        }
    }

    private void bindTextWatchers() {
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!saving && !messageEditedByUser && !selectedTemplateBody.isEmpty()) {
                    renderSelectedTemplate();
                }
            }
        });
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!applyingTemplate) {
                    messageEditedByUser = true;
                    messageGuideView.setText("직접 수정한 발송 문구입니다. 원본 템플릿은 변경되지 않습니다.");
                }
            }
        });
    }

    private void bindActions() {
        findViewById(R.id.postCallClose).setOnClickListener(v -> {
            if (!saving) finish();
        });
        findViewById(R.id.postCallExclude).setOnClickListener(v -> confirmExclude());
        stageButton.setOnClickListener(v -> showStageDialog());
        taskTypeButton.setOnClickListener(v -> showTaskTypeDialog());
        delayedMessageButton.setOnClickListener(v -> showDelayedMessageDialog());
        templateChangeButton.setOnClickListener(v -> openTemplateLibrary());
        saveOnlyButton.setOnClickListener(v -> saveResult(false));
        saveAndSendButton.setOnClickListener(v -> saveResult(true));
    }

    private void renderHeader() {
        TextView phoneView = findViewById(R.id.postCallPhone);
        TextView metaView = findViewById(R.id.postCallMeta);
        TextView existingInfo = findViewById(R.id.postCallExistingInfo);
        TextView existingMemo = findViewById(R.id.postCallExistingMemo);

        phoneView.setText(phone.isEmpty() ? "번호 없음" : phone);
        int type = callType();
        long durationSec = durationSec();
        metaView.setText(callTypeLabel(type, durationSec) + " · " + formatDuration(durationSec));
        existingInfo.setText(existingCustomer == null
                ? "새 고객으로 저장됩니다."
                : "현재 상태 · " + statusLabel(existingCustomer.relationStatus));

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
                    if (saving) return;
                    selectedResult = code;
                    applyResultDefaults(code);
                    refreshResultButtonStyles();
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

    private void applyResultDefaults(String code) {
        if ("CONTRACT".equals(code) || "CLOSED".equals(code)) {
            selectedStage = db.completedStage();
            followUpGroup.check(R.id.followNone);
        } else if ("QUOTE".equals(code) || "CALLBACK".equals(code) || "INTERESTED".equals(code)) {
            if (db.firstStage().equals(selectedStage)) selectedStage = progressStage();
            if ("CALLBACK".equals(code)) {
                selectedTaskType = TaskTypeStore.TYPE_CALL;
                followUpGroup.check(R.id.followTomorrow);
                renderTaskType();
            }
        }
        renderStage();
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

    private void showStageDialog() {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        for (StageOption stage : db.listStages()) {
            options.add(new ActionChoiceDialog.Option(stage.name, stage.name,
                    stage.name.equals(selectedStage) ? "현재 선택" : "이 상태로 저장",
                    stage.color));
        }
        ActionChoiceDialog.show(this, "고객 상태", null, options, option -> {
            selectedStage = option.key;
            renderStage();
        }, "고객 상태 편집", v -> startActivity(new Intent(this, StageSettingsActivity.class)));
    }

    private void renderStage() {
        stageButton.setText(selectedStage + "  ▾");
    }

    private void showTaskTypeDialog() {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        for (TaskTypeOption type : taskTypes.list()) {
            options.add(new ActionChoiceDialog.Option(type.code, type.name,
                    type.code.equals(selectedTaskType) ? "현재 선택" : "다음 할 일로 선택",
                    type.color));
        }
        ActionChoiceDialog.show(this, "다음 할 일 종류", null, options, option -> {
            selectedTaskType = option.key;
            renderTaskType();
        }, "일정 종류 편집", v -> startActivity(new Intent(this, TaskTypeSettingsActivity.class)));
    }

    private void renderTaskType() {
        TaskTypeOption type = taskTypes.find(selectedTaskType);
        taskTypeButton.setText(type.name + "  ▾");
    }

    private void renderMessageSection() {
        boolean enabled = FeatureEntitlementStore.hasMessageAccess(this);
        messageSection.setVisibility(enabled ? View.VISIBLE : View.GONE);
        saveAndSendButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) return;

        selectedMessagePurpose = messagePurposeForCall();
        MessageTemplateStore.Template template =
                MessageTemplateStore.defaultTemplate(this, selectedMessagePurpose);
        if (template == null) {
            String fallback = isMissedOrRejected()
                    ? MessageAutomationStore.missedTemplate(this)
                    : MessageAutomationStore.connectedTemplate(this);
            selectTemplate("", "기본 템플릿", fallback);
        } else {
            selectTemplate(template.id, template.name, template.body);
        }
    }

    private String messagePurposeForCall() {
        if (isMissedOrRejected()) return MessageTemplateStore.PURPOSE_MISSED;
        if (callType() == CallLog.Calls.OUTGOING_TYPE) return MessageTemplateStore.PURPOSE_OUTGOING;
        return MessageTemplateStore.PURPOSE_INCOMING;
    }

    private void openTemplateLibrary() {
        if (saving) return;
        startActivityForResult(new Intent(this, MessageTemplateLibraryActivity.class)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_SELECT_MODE, true)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_PURPOSE_FILTER,
                        selectedMessagePurpose), REQUEST_TEMPLATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TEMPLATE || resultCode != RESULT_OK || data == null) return;
        selectTemplate(
                safe(data.getStringExtra(MessageTemplateLibraryActivity.EXTRA_TEMPLATE_ID)),
                safe(data.getStringExtra(MessageTemplateLibraryActivity.EXTRA_TEMPLATE_NAME)),
                safe(data.getStringExtra(MessageTemplateLibraryActivity.EXTRA_TEMPLATE_BODY)));
    }

    private void selectTemplate(String id, String name, String body) {
        selectedTemplateId = safe(id).trim();
        selectedTemplateName = safe(name).trim();
        selectedTemplateBody = safe(body);
        messageEditedByUser = false;
        templateNameView.setText(selectedTemplateName.isEmpty() ? "선택한 템플릿" : selectedTemplateName);
        boolean isDefault = !selectedTemplateId.isEmpty()
                && selectedTemplateId.equals(MessageTemplateStore.defaultId(this, selectedMessagePurpose));
        templateMetaView.setText(MessageTemplateStore.purposeLabel(selectedMessagePurpose)
                + (isDefault ? " 기본" : " 템플릿") + " · 현재 통화 정보로 치환");
        renderSelectedTemplate();
    }

    private void renderSelectedTemplate() {
        if (selectedTemplateBody.isEmpty()) return;
        Customer previewCustomer = existingCustomer != null
                ? previewCustomer(nameInput.getText().toString().trim(), existingCustomer.id)
                : previewCustomer(nameInput.getText().toString().trim(), 0L);
        MessageTemplateEngine.RenderResult rendered = MessageTemplateEngine.render(
                this, selectedTemplateBody, previewCustomer, buildCallRecord());
        applyingTemplate = true;
        messageInput.setText(rendered.body);
        messageInput.setSelection(messageInput.getText().length());
        applyingTemplate = false;
        messageEditedByUser = false;

        if (!rendered.unsupportedVariables.isEmpty()) {
            messageGuideView.setText("지원하지 않는 변수: "
                    + MessageTemplateEngine.describeVariables(rendered.unsupportedVariables));
        } else if (!rendered.unresolvedVariables.isEmpty()) {
            messageGuideView.setText("아직 치환되지 않은 변수: "
                    + MessageTemplateEngine.describeVariables(rendered.unresolvedVariables)
                    + " · 고객·계정·일정 정보를 확인해주세요.");
        } else {
            messageGuideView.setText("고객명·전화번호·통화정보가 치환된 미리보기입니다. 내용을 수정해도 원본 템플릿은 바뀌지 않습니다.");
        }
    }

    private void showDelayedMessageDialog() {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        options.add(new ActionChoiceDialog.Option("0", "예약 안 함", "후속문자를 만들지 않음", ""));
        options.add(new ActionChoiceDialog.Option("1", "1일 후", "내일 같은 시간에 자동 발송", "#4389FF"));
        options.add(new ActionChoiceDialog.Option("3", "3일 후", "상담 후 검토 여부 확인", "#7A5AF8"));
        options.add(new ActionChoiceDialog.Option("7", "7일 후", "일주일 후 후속 안내", "#F5A524"));
        ActionChoiceDialog.show(this, "후속문자 예약",
                "고객 상태가 완료되거나 다시 통화하면 자동 취소됩니다.",
                options, option -> {
                    try {
                        delayedMessageDays = Integer.parseInt(option.key);
                    } catch (NumberFormatException ignored) {
                        delayedMessageDays = 0;
                    }
                    renderDelayedMessage();
                });
    }

    private void renderDelayedMessage() {
        delayedMessageButton.setText(delayedMessageDays <= 0
                ? "예약 안 함  ▾" : delayedMessageDays + "일 후 자동 발송  ▾");
    }

    private void saveResult(boolean sendNow) {
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
        if (sendNow && !FeatureEntitlementStore.hasMessageAccess(this)) {
            Toast.makeText(this, "문자자동화 구독이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sendNow && checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "문자 발송 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        if (sendNow && !validateImmediateMessage()) return;

        setSaving(true);
        lastSendNotice = "";
        lastScheduleNotice = "";
        try {
            Customer customer = saveCustomerAndCall();
            boolean immediateQueued = sendNow && queueImmediateMessage(customer);
            boolean delayedScheduled = delayedMessageDays > 0
                    && FeatureEntitlementStore.hasMessageAccess(this)
                    && scheduleDelayedMessage(customer, delayedMessageDays);

            SettingsStore.markCallProcessed(this, callFingerprint);
            markPendingHandled();
            ContactNameSyncManager.requestSyncAll(this);

            String result = "저장했습니다.";
            if (immediateQueued && delayedScheduled) result = "저장하고 문자 발송·후속문자 예약을 등록했습니다.";
            else if (immediateQueued) result = "저장하고 문자를 발송했습니다.";
            else if (delayedScheduled) result = "저장하고 후속문자를 예약했습니다.";
            else if (!lastSendNotice.isEmpty()) result = "저장했습니다. 즉시문자는 중복방지로 건너뛰었습니다.";
            else if (!lastScheduleNotice.isEmpty()) result = "저장했습니다. 후속문자는 중복방지 또는 제외 설정으로 예약하지 않았습니다.";
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            finish();
        } catch (IllegalArgumentException e) {
            setSaving(false);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            setSaving(false);
            Toast.makeText(this, "저장하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean validateImmediateMessage() {
        String body = messageInput.getText().toString().trim();
        if (body.isEmpty()) {
            Toast.makeText(this, "보낼 문자 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        List<String> unresolved = MessageTemplateEngine.findPlaceholders(body);
        if (!unresolved.isEmpty()) {
            Toast.makeText(this, "치환되지 않은 변수가 있습니다: "
                    + MessageTemplateEngine.describeVariables(unresolved), Toast.LENGTH_LONG).show();
            return false;
        }
        long customerId = existingCustomer == null ? 0L : existingCustomer.id;
        MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                this, customerId, phone, MessageAutomationManager.TRIGGER_MANUAL);
        if (exclusion.blocked) {
            Toast.makeText(this, exclusion.reason, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private Customer saveCustomerAndCall() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) name = defaultCustomerName(phone);

        Customer latestCustomer = db.findByPhone(phone);
        long customerId;
        if (latestCustomer == null) {
            customerId = db.insertCustomer(name, phone, selectedStage, "");
        } else {
            customerId = latestCustomer.id;
            db.updateCustomer(customerId, name, selectedStage);
        }
        if ("CONTRACT".equals(selectedResult)) db.markTransactionCompleted(customerId);

        long startedAt = startedAt();
        long endedAt = Math.max(startedAt, endedAt());
        long interactionId = db.insertInteraction(customerId, callTypeCode(callType()),
                startedAt, endedAt, durationSec(), selectedResult,
                noteInput.getText().toString().trim());

        long dueAt = selectedFollowUpTime();
        if (dueAt > 0L) {
            TaskTypeOption taskType = taskTypes.find(selectedTaskType);
            db.insertFollowUpTask(customerId, interactionId,
                    taskType.code, taskType.name, dueAt);
            if (db.firstStage().equals(selectedStage)) {
                selectedStage = progressStage();
                db.updateCustomerStage(customerId, selectedStage);
                db.insertInteraction(customerId, "STATUS_CHANGE",
                        System.currentTimeMillis(), System.currentTimeMillis(), 0L,
                        "STATUS_" + selectedStage, db.firstStage() + " → " + selectedStage);
            }
        }
        return db.findCustomerById(customerId);
    }

    private boolean queueImmediateMessage(Customer customer) {
        String body = messageInput.getText().toString().trim();
        if (body.isEmpty()) return false;
        long messageId = SmsSender.queueAndSend(this, customer.id, callLogId(),
                0L, "", selectedTemplateId, phone, body,
                MessageAutomationManager.TRIGGER_MANUAL,
                MessageAutomationStore.selectedSubscriptionId(this), false);
        MessageRecord record;
        MessageLogStore messages = new MessageLogStore(this);
        try {
            record = messages.find(messageId);
        } finally {
            messages.close();
        }
        boolean queued = record != null
                && !MessageLogStore.STATUS_SKIPPED.equals(record.status)
                && !MessageLogStore.STATUS_FAILED.equals(record.status)
                && !MessageLogStore.STATUS_CANCELLED.equals(record.status);
        if (!queued && record != null) lastSendNotice = safe(record.error);
        long now = System.currentTimeMillis();
        db.insertInteraction(customer.id, "MESSAGE_SEND_REQUEST", now, now, 0L,
                queued ? "QUEUED" : "SKIPPED", body);
        sendBroadcast(new Intent(MessageSectionView.ACTION_CHANGED).setPackage(getPackageName()));
        return queued;
    }

    private boolean scheduleDelayedMessage(Customer customer, int days) {
        MessageTemplateStore.Template followUp = MessageTemplateStore.defaultTemplate(
                this, MessageTemplateStore.PURPOSE_FOLLOW_UP);
        String templateId = followUp == null ? "" : followUp.id;
        String templateBody = followUp == null
                ? MessageAutomationStore.delayedTemplate(this) : followUp.body;
        MessageTemplateEngine.RenderResult rendered = MessageTemplateEngine.render(
                this, templateBody, customer, buildCallRecord());
        if (!rendered.isReady()) {
            throw new IllegalArgumentException("후속문자 템플릿의 고객·계정·일정 정보를 확인해주세요.");
        }
        MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                this, customer.id, phone, MessageAutomationManager.TRIGGER_DELAYED);
        if (exclusion.blocked) {
            lastScheduleNotice = exclusion.reason;
            return false;
        }

        long when = System.currentTimeMillis() + Math.max(1, days) * DAY_MS;
        MessageLogStore store = new MessageLogStore(this);
        try {
            store.cancelScheduledForPhone(phone, MessageAutomationManager.TRIGGER_DELAYED,
                    "통화 정리 화면에서 새 후속문자를 예약했습니다.");
            long messageId = store.createJobAdvanced(customer.id, callLogId(), 0L,
                    "", templateId, phone, rendered.body,
                    MessageAutomationManager.TRIGGER_DELAYED,
                    MessageLogStore.STATUS_SCHEDULED, when,
                    MessageAutomationStore.selectedSubscriptionId(this), false);
            MessageRecord record = store.find(messageId);
            if (record == null || !MessageLogStore.STATUS_SCHEDULED.equals(record.status)) {
                lastScheduleNotice = record == null ? "후속문자를 예약하지 못했습니다." : safe(record.error);
                return false;
            }
            MessageScheduler.schedule(this, messageId, when);
            long now = System.currentTimeMillis();
            db.insertInteraction(customer.id, "MESSAGE_SCHEDULED", now, now, 0L,
                    "SCHEDULED", days + "일 후 · " + rendered.body);
            sendBroadcast(new Intent(MessageSectionView.ACTION_CHANGED).setPackage(getPackageName()));
            return true;
        } finally {
            store.close();
        }
    }

    private void confirmExclude() {
        if (saving) return;
        new AlertDialog.Builder(this)
                .setTitle("이 번호를 고객관리에서 제외할까요?")
                .setMessage("앞으로 통화 정리 대상에서 제외됩니다. 문자 발송 제외는 고객 상세에서 따로 설정합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("제외", (dialog, which) -> excludePhone())
                .show();
    }

    private void excludePhone() {
        try {
            db.addPhoneRule(phone, "EXCLUDED", "사용자 제외");
            SettingsStore.markCallProcessed(this, callFingerprint);
            markPendingHandled();
            Toast.makeText(this, "고객관리에서 제외했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (RuntimeException e) {
            Toast.makeText(this, "제외하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void setSaving(boolean value) {
        saving = value;
        saveOnlyButton.setEnabled(!value);
        saveAndSendButton.setEnabled(!value);
        templateChangeButton.setEnabled(!value);
        saveOnlyButton.setAlpha(value ? 0.55f : 1f);
        saveAndSendButton.setAlpha(value ? 0.55f : 1f);
        templateChangeButton.setAlpha(value ? 0.55f : 1f);
        saveOnlyButton.setText(value ? "저장 중" : "저장만 하기");
        saveAndSendButton.setText(value ? "처리 중" : "저장하고 문자 보내기");
        stageButton.setEnabled(!value);
        taskTypeButton.setEnabled(!value);
        delayedMessageButton.setEnabled(!value);
        nameInput.setEnabled(!value);
        noteInput.setEnabled(!value);
        messageInput.setEnabled(!value);
    }

    private void markPendingHandled() {
        long pendingId = getIntent().getLongExtra(EXTRA_PENDING_CALL_ID, -1L);
        if (pendingId <= 0L) return;
        PendingCallStore store = new PendingCallStore(this);
        try {
            store.markHandled(pendingId);
        } finally {
            store.close();
        }
        sendBroadcast(new Intent(PendingCallSectionView.ACTION_CHANGED).setPackage(getPackageName()));
    }

    private CallRecord buildCallRecord() {
        return new CallRecord(callLogId(), phone,
                safe(getIntent().getStringExtra(EXTRA_CACHED_NAME)),
                callType(), startedAt(), durationSec());
    }

    private Customer previewCustomer(String name, long id) {
        long now = System.currentTimeMillis();
        String displayName = name == null || name.trim().isEmpty()
                ? defaultCustomerName(phone) : name.trim();
        return new Customer(id, displayName, phone,
                PhoneNumberNormalizer.normalize(phone), selectedStage,
                "", existingCustomer == null ? "" : existingCustomer.memo,
                now, now, existingCustomer == null ? null : existingCustomer.firstTransactionAt);
    }

    private String buildCallFingerprint() {
        return PhoneNumberNormalizer.normalize(phone) + ":" + startedAt() + ":"
                + durationSec() + ":" + callType();
    }

    private long selectedFollowUpTime() {
        int checked = followUpGroup.getCheckedRadioButtonId();
        if (checked == R.id.followNone) return -1L;
        int days = checked == R.id.followTomorrow ? 1
                : checked == R.id.followThreeDays ? 3 : 7;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String progressStage() {
        List<StageOption> stages = db.listStages();
        return stages.size() > 1 ? stages.get(1).name : db.firstStage();
    }

    private long callLogId() {
        long id = getIntent().getLongExtra(EXTRA_CALL_LOG_ID, -1L);
        if (id > 0L) return id;
        return Math.max(0L, getIntent().getLongExtra(EXTRA_PENDING_CALL_ID, 0L));
    }

    private int callType() {
        return getIntent().getIntExtra(EXTRA_CALL_TYPE, CallLog.Calls.INCOMING_TYPE);
    }

    private long startedAt() {
        return getIntent().getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis());
    }

    private long endedAt() {
        return getIntent().getLongExtra(EXTRA_ENDED_AT, System.currentTimeMillis());
    }

    private long durationSec() {
        return Math.max(0L, getIntent().getLongExtra(EXTRA_DURATION_SEC, 0L));
    }

    private boolean isMissedOrRejected() {
        int type = callType();
        return type == CallLog.Calls.MISSED_TYPE || type == CallLog.Calls.REJECTED_TYPE;
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

    private String defaultCustomerName(String rawPhone) {
        String normalized = PhoneNumberNormalizer.normalize(rawPhone);
        String suffix = normalized.length() >= 4
                ? normalized.substring(normalized.length() - 4) : normalized;
        return suffix.isEmpty() ? "새 고객" : "고객 " + suffix;
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
