package kr.pagero.calltag;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public final class PostCallActivity extends Activity {
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_CACHED_NAME = "cached_name";
    public static final String EXTRA_CALL_TYPE = "call_type";
    public static final String EXTRA_STARTED_AT = "started_at";
    public static final String EXTRA_ENDED_AT = "ended_at";
    public static final String EXTRA_DURATION_SEC = "duration_sec";

    private CallTagDbHelper db;
    private Customer existingCustomer;
    private String phone;
    private String selectedResult = "INTERESTED";

    private EditText nameInput;
    private EditText noteInput;
    private RadioGroup relationGroup;
    private RadioGroup followUpGroup;
    private LinearLayout resultButtons;

    private final String[] resultCodes = {
            "INTERESTED", "QUOTE", "CALLBACK", "CONTRACT", "HOLD", "CLOSED"
    };
    private final String[] resultLabels = {
            "관심 있음", "견적·자료 발송", "다시 연락", "계약·거래 완료", "보류", "상담 종료"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_call);
        db = new CallTagDbHelper(this);

        phone = getIntent().getStringExtra(EXTRA_PHONE);
        if (phone == null) phone = "";
        existingCustomer = db.findByPhone(phone);

        bindViews();
        renderHeader();
        renderResultButtons();
        bindActions();
    }

    private void bindViews() {
        nameInput = findViewById(R.id.postCallName);
        noteInput = findViewById(R.id.postCallNote);
        relationGroup = findViewById(R.id.relationGroup);
        followUpGroup = findViewById(R.id.followUpGroup);
        resultButtons = findViewById(R.id.postCallResultButtons);

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

        phoneView.setText(phone.isEmpty() ? "번호를 확인할 수 없음" : phone);
        int type = getIntent().getIntExtra(EXTRA_CALL_TYPE, CallLog.Calls.INCOMING_TYPE);
        long durationSec = getIntent().getLongExtra(EXTRA_DURATION_SEC, 0L);
        long startedAt = getIntent().getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis());
        String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(startedAt));
        metaView.setText(callTypeLabel(type) + " · " + formatDuration(durationSec) + " · " + time);

        if (existingCustomer == null) {
            existingInfo.setText("등록되지 않은 번호입니다. 신규 또는 기존 고객으로 분류해주세요.");
        } else {
            existingInfo.setText("등록 고객 · 현재 상태: " + statusLabel(existingCustomer.relationStatus));
        }
    }

    private void renderResultButtons() {
        resultButtons.removeAllViews();
        for (int i = 0; i < resultCodes.length; i++) {
            String code = resultCodes[i];
            Button button = new Button(this);
            button.setText(resultLabels[i]);
            button.setTextAllCaps(false);
            button.setTextSize(15f);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setGravity(android.view.Gravity.CENTER_VERTICAL);
            button.setPadding(dp(16), 0, dp(16), 0);
            button.setOnClickListener(v -> {
                selectedResult = code;
                refreshResultButtonStyles();
                if ("CALLBACK".equals(code)) {
                    ((RadioButton) findViewById(R.id.followTomorrow)).setChecked(true);
                }
            });
            button.setTag(code);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
            params.bottomMargin = dp(9);
            resultButtons.addView(button, params);
        }
        refreshResultButtonStyles();
    }

    private void refreshResultButtonStyles() {
        for (int i = 0; i < resultButtons.getChildCount(); i++) {
            Button button = (Button) resultButtons.getChildAt(i);
            boolean selected = selectedResult.equals(button.getTag());
            button.setBackgroundResource(selected ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
            button.setTextColor(getColor(selected ? R.color.text_primary : R.color.text_secondary));
        }
    }

    private void bindActions() {
        findViewById(R.id.postCallClose).setOnClickListener(v -> finish());
        findViewById(R.id.postCallSave).setOnClickListener(v -> saveResult());
        relationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean excluded = checkedId == R.id.relationExcluded;
            nameInput.setEnabled(!excluded);
            noteInput.setEnabled(!excluded);
            resultButtons.setAlpha(excluded ? 0.45f : 1f);
            followUpGroup.setAlpha(excluded ? 0.45f : 1f);
        });
    }

    private void saveResult() {
        if (relationGroup.getCheckedRadioButtonId() == R.id.relationExcluded) {
            db.addPhoneRule(phone, "EXCLUDED", "통화 후 사용자가 제외 처리");
            Toast.makeText(this, "이 번호는 이후 고객 분류에서 제외합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String name = nameInput.getText().toString().trim();
        String relationStatus = relationGroup.getCheckedRadioButtonId() == R.id.relationExisting
                ? CallTagDbHelper.STATUS_EXISTING
                : CallTagDbHelper.STATUS_NEW;

        try {
            long customerId;
            if (existingCustomer == null) {
                customerId = db.insertCustomer(name, phone, relationStatus, "CALL");
            } else {
                customerId = existingCustomer.id;
                String nextStatus = existingCustomer.relationStatus;
                if (CallTagDbHelper.STATUS_NEW.equals(nextStatus) && "INTERESTED".equals(selectedResult)) {
                    nextStatus = CallTagDbHelper.STATUS_CONSULTING;
                }
                if (relationGroup.getCheckedRadioButtonId() == R.id.relationExisting) {
                    nextStatus = CallTagDbHelper.STATUS_EXISTING;
                }
                db.updateCustomer(customerId, name, nextStatus);
            }

            if ("CONTRACT".equals(selectedResult)) {
                db.markTransactionCompleted(customerId);
            } else if (existingCustomer == null && "INTERESTED".equals(selectedResult)) {
                db.updateCustomer(customerId, name, CallTagDbHelper.STATUS_CONSULTING);
            }

            long startedAt = getIntent().getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis());
            long endedAt = getIntent().getLongExtra(EXTRA_ENDED_AT, System.currentTimeMillis());
            long durationSec = getIntent().getLongExtra(EXTRA_DURATION_SEC, 0L);
            int type = getIntent().getIntExtra(EXTRA_CALL_TYPE, CallLog.Calls.INCOMING_TYPE);
            long interactionId = db.insertInteraction(
                    customerId,
                    callTypeCode(type),
                    startedAt,
                    endedAt,
                    durationSec,
                    selectedResult,
                    noteInput.getText().toString());

            long dueAt = selectedFollowUpTime();
            if (dueAt > 0L) {
                db.insertFollowUpTask(
                        customerId,
                        interactionId,
                        "CALL",
                        "다시 연락하기",
                        dueAt);
            }

            Toast.makeText(this, dueAt > 0L
                    ? "고객과 다음 연락을 저장했습니다."
                    : "상담 결과를 저장했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            Toast.makeText(this, "저장하지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show();
        }
    }

    private long selectedFollowUpTime() {
        int checked = followUpGroup.getCheckedRadioButtonId();
        if (checked == R.id.followNone) return -1L;
        long days = checked == R.id.followTomorrow ? 1L : checked == R.id.followThreeDays ? 3L : 7L;
        long base = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L;
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(base);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 10);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String callTypeCode(int type) {
        if (type == CallLog.Calls.OUTGOING_TYPE) return "OUTGOING_CALL";
        if (type == CallLog.Calls.MISSED_TYPE) return "MISSED_CALL";
        if (type == CallLog.Calls.REJECTED_TYPE) return "REJECTED_CALL";
        return "INCOMING_CALL";
    }

    private String callTypeLabel(int type) {
        if (type == CallLog.Calls.OUTGOING_TYPE) return "발신 통화";
        if (type == CallLog.Calls.MISSED_TYPE) return "부재중 전화";
        if (type == CallLog.Calls.REJECTED_TYPE) return "거절한 전화";
        return "수신 통화";
    }

    private String statusLabel(String status) {
        if (CallTagDbHelper.STATUS_EXISTING.equals(status)) return "기존 고객";
        if (CallTagDbHelper.STATUS_CONSULTING.equals(status)) return "상담 중";
        if (CallTagDbHelper.STATUS_VIP.equals(status)) return "VIP";
        if (CallTagDbHelper.STATUS_DORMANT.equals(status)) return "휴면";
        return "신규 고객";
    }

    private String formatDuration(long seconds) {
        long minutes = seconds / 60L;
        long remain = seconds % 60L;
        return minutes > 0 ? minutes + "분 " + remain + "초" : remain + "초";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
