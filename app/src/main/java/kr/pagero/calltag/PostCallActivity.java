package kr.pagero.calltag;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public final class PostCallActivity extends Activity {
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
    private Customer existingCustomer;
    private String phone;
    private String selectedResult = "INTERESTED";
    private EditText nameInput;
    private EditText noteInput;
    private RadioGroup relationGroup;
    private RadioGroup followUpGroup;
    private LinearLayout resultButtons;

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
            boolean isExisting = CallTagDbHelper.STATUS_EXISTING.equals(existingCustomer.relationStatus)
                    || CallTagDbHelper.STATUS_VIP.equals(existingCustomer.relationStatus);
            ((RadioButton) findViewById(isExisting ? R.id.relationExisting : R.id.relationNew)).setChecked(true);
        } else {
            nameInput.setText(cachedName == null ? "" : cachedName);
            ((RadioButton) findViewById(R.id.relationNew)).setChecked(true);
        }
    }

    private void renderHeader() {
        TextView phoneView = findViewById(R.id.postCallPhone);
        TextView metaView = findViewById(R.id.postCallMeta);
        TextView existingInfo = findViewById(R.id.postCallExistingInfo);
        phoneView.setText(phone.isEmpty() ? "번호 없음" : phone);
        int type = getIntent().getIntExtra(EXTRA_CALL_TYPE, CallLog.Calls.INCOMING_TYPE);
        long durationSec = getIntent().getLongExtra(EXTRA_DURATION_SEC, 0L);
        metaView.setText(callTypeLabel(type) + " · " + formatDuration(durationSec));
        existingInfo.setText(existingCustomer == null ? "신규 번호" : statusLabel(existingCustomer.relationStatus));
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
                    selectedResult = code;
                    refreshResultButtonStyles();
                    if ("CALLBACK".equals(code)) {
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
        findViewById(R.id.postCallClose).setOnClickListener(v -> finish());
        findViewById(R.id.postCallSave).setOnClickListener(v -> saveResult());
        relationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean excluded = checkedId == R.id.relationExcluded;
            nameInput.setEnabled(!excluded);
            noteInput.setEnabled(!excluded);
            resultButtons.setAlpha(excluded ? 0.35f : 1f);
            followUpGroup.setAlpha(excluded ? 0.35f : 1f);
        });
    }

    private void saveResult() {
        if (relationGroup.getCheckedRadioButtonId() == R.id.relationExcluded) {
            db.addPhoneRule(phone, "EXCLUDED", "사용자 제외");
            Toast.makeText(this, "제외했습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String name = nameInput.getText().toString().trim();
        boolean selectedExisting = relationGroup.getCheckedRadioButtonId() == R.id.relationExisting;
        String initialStatus = selectedExisting
                ? CallTagDbHelper.STATUS_EXISTING
                : CallTagDbHelper.STATUS_NEW;

        try {
            long customerId;
            if (existingCustomer == null) {
                customerId = db.insertCustomer(name, phone, initialStatus, "CALL");
            } else {
                customerId = existingCustomer.id;
                String nextStatus = existingCustomer.relationStatus;
                if (selectedExisting) {
                    nextStatus = CallTagDbHelper.STATUS_EXISTING;
                } else if (!CallTagDbHelper.STATUS_EXISTING.equals(nextStatus)
                        && !CallTagDbHelper.STATUS_VIP.equals(nextStatus)
                        && isActiveResult(selectedResult)) {
                    nextStatus = CallTagDbHelper.STATUS_CONSULTING;
                }
                db.updateCustomer(customerId, name, nextStatus);
            }

            if ("CONTRACT".equals(selectedResult)) {
                db.markTransactionCompleted(customerId);
            } else if (existingCustomer == null && isActiveResult(selectedResult)) {
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
                    noteInput.getText().toString().trim());

            long dueAt = selectedFollowUpTime();
            if (dueAt > 0L) {
                db.insertFollowUpTask(customerId, interactionId, "CALL", "다시 연락", dueAt);
            }
            Toast.makeText(this, "저장했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            Toast.makeText(this, "저장하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isActiveResult(String result) {
        return "INTERESTED".equals(result)
                || "QUOTE".equals(result)
                || "CALLBACK".equals(result);
    }

    private long selectedFollowUpTime() {
        int checked = followUpGroup.getCheckedRadioButtonId();
        if (checked == R.id.followNone) return -1L;
        long days = checked == R.id.followTomorrow ? 1L
                : checked == R.id.followThreeDays ? 3L : 7L;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L);
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

    private String callTypeLabel(int type) {
        if (type == CallLog.Calls.OUTGOING_TYPE) return "발신";
        if (type == CallLog.Calls.MISSED_TYPE) return "부재중";
        if (type == CallLog.Calls.REJECTED_TYPE) return "거절";
        return "수신";
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

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}