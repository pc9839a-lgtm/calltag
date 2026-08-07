package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public final class PostCallActivity extends Activity {
    public static final String EXTRA_PENDING_CALL_ID = "pending_call_id";
    public static final String EXTRA_CALL_LOG_ID = "call_log_id";
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_CACHED_NAME = "cached_name";
    public static final String EXTRA_CALL_TYPE = "call_type";
    public static final String EXTRA_STARTED_AT = "started_at";
    public static final String EXTRA_ENDED_AT = "ended_at";
    public static final String EXTRA_DURATION_SEC = "duration_sec";

    private CallTagDbHelper db;
    private Customer existingCustomer;
    private EditText nameInput;
    private EditText noteInput;
    private Button saveButton;
    private String phone = "";
    private String callFingerprint = "";
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply compact window geometry before setContentView so there is never a full-screen frame
        // before the small post-call popup is measured/drawn.
        PostCallPopupWindowInstaller.install(this);
        setContentView(R.layout.activity_post_call);
        PostCallPopupWindowInstaller.install(this);
        db = new CallTagDbHelper(this);
        bindViews();
        bindIntent(getIntent());
        bindActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PostCallLaunchReceipt.markVisible(this);
        PostCallRecoveryStore.markDelivered(this, callLogId());
        PostCallPopupWindowInstaller.install(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bindIntent(intent);
        PostCallLaunchReceipt.markVisible(this);
        PostCallRecoveryStore.markDelivered(this, callLogId());
        PostCallPopupWindowInstaller.install(this);
        CrashTelemetryStore.record(this, "post_call", "new_intent_visible",
                "call=" + callLogId());
    }

    private void bindViews() {
        nameInput = findViewById(R.id.postCallName);
        noteInput = findViewById(R.id.postCallNote);
        saveButton = findViewById(R.id.postCallSaveOnly);
    }

    private void bindIntent(Intent intent) {
        if (intent == null) return;
        phone = safe(intent.getStringExtra(EXTRA_PHONE));
        existingCustomer = db.findByPhone(phone);
        callFingerprint = buildCallFingerprint();

        TextView phoneView = findViewById(R.id.postCallPhone);
        TextView metaView = findViewById(R.id.postCallMeta);
        phoneView.setText(phone.isEmpty() ? "번호 없음" : phone);
        metaView.setText(callTypeLabel(callType(), durationSec()) + " · " + formatDuration(durationSec()));

        String cachedName = safe(intent.getStringExtra(EXTRA_CACHED_NAME)).trim();
        if (existingCustomer != null) {
            nameInput.setText(existingCustomer.displayName);
            noteInput.setText(existingCustomer.memo);
        } else if (!cachedName.isEmpty() && !"이름없는고객".equals(cachedName)) {
            nameInput.setText(cachedName);
            noteInput.setText("");
        } else {
            nameInput.setText(defaultCustomerName(phone));
            noteInput.setText("");
        }
        nameInput.setSelection(nameInput.length());
        noteInput.setSelection(noteInput.length());
    }

    private void bindActions() {
        findViewById(R.id.postCallClose).setOnClickListener(v -> {
            if (!saving) finish();
        });
        saveButton.setOnClickListener(v -> saveMemo());
    }

    private void saveMemo() {
        if (saving) return;
        if (PhoneNumberNormalizer.normalize(phone).length() < 8) {
            Toast.makeText(this, "전화번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (SettingsStore.isCallProcessed(this, callFingerprint)) {
            markPendingHandledSafely();
            Toast.makeText(this, "이미 저장한 통화입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String name = nameInput.getText().toString().trim();
        String memo = noteInput.getText().toString().trim();
        if (name.isEmpty()) name = defaultCustomerName(phone);

        setSaving(true);
        try {
            Customer latest = db.findByPhone(phone);
            long customerId;
            String stage;
            if (latest == null) {
                stage = db.firstStage();
                customerId = db.insertCustomer(name, phone, stage, "");
            } else {
                customerId = latest.id;
                stage = latest.relationStatus;
            }
            db.updateCustomerProfile(customerId, name, stage, memo);

            long startedAt = startedAt();
            long endedAt = Math.max(startedAt, endedAt());
            long interactionId = CallInteractionDeduper.insertOnce(
                    db,
                    customerId,
                    CallDisposition.interactionType(callType()),
                    startedAt,
                    endedAt,
                    durationSec(),
                    "MEMO_SAVED",
                    memo);

            SettingsStore.markCallProcessed(this, callFingerprint);
            markPendingHandledSafely();
            CrashTelemetryStore.record(this, "post_call_save", "calltag_db_only",
                    "call=" + callLogId() + ",interaction=" + interactionId);

            Toast.makeText(this, "고객명과 메모를 저장했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IllegalArgumentException error) {
            setSaving(false);
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            setSaving(false);
            CrashTelemetryStore.record(this, "post_call_save", "failed",
                    error.getClass().getSimpleName());
            Toast.makeText(this, "저장하지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show();
        }
    }

    private void setSaving(boolean value) {
        saving = value;
        nameInput.setEnabled(!value);
        noteInput.setEnabled(!value);
        saveButton.setEnabled(!value);
        saveButton.setAlpha(value ? 0.55f : 1f);
        saveButton.setText(value ? "저장 중" : "저장");
    }

    private void markPendingHandledSafely() {
        try {
            markPendingHandled();
        } catch (RuntimeException error) {
            CrashTelemetryStore.record(this, "pending_call_cleanup", "failed",
                    error.getClass().getSimpleName());
        }
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
        sendBroadcast(new Intent(PendingCallSectionView.ACTION_CHANGED)
                .setPackage(getPackageName()));
    }

    private String buildCallFingerprint() {
        long callId = callLogId();
        if (callId > 0L) return "call_log:" + callId;
        return PhoneNumberNormalizer.normalize(phone) + ":" + startedAt() + ":"
                + durationSec() + ":" + callType();
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

    private String callTypeLabel(int type, long durationSec) {
        CallRecord record = new CallRecord(callLogId(), phone, "", type,
                startedAt(), Math.max(0L, durationSec));
        return CallDisposition.label(record);
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

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        PostCallPopupWindowInstaller.uninstall(this);
        super.onDestroy();
    }
}
