package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CallerIdSetupActivity extends Activity {
    public static final String EXTRA_REQUIRED_SETUP = "required_setup";

    private static final int REQUEST_RUNTIME_PERMISSIONS = 7301;
    private static final int REQUEST_SCREENING_ROLE = 7302;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private TextView title;
    private TextView intro;
    private Button action;
    private Button contactSyncToggle;
    private Button overlayTest;
    private RadioGroup privacyGroup;
    private boolean requiredSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caller_id_setup);
        requiredSetup = getIntent().getBooleanExtra(EXTRA_REQUIRED_SETUP, false);
        CallPopupNotificationManager.ensureChannels(this);

        status = findViewById(R.id.callerIdSetupStatus);
        title = findViewById(R.id.callerIdSetupTitle);
        intro = findViewById(R.id.callerIdSetupIntro);
        action = findViewById(R.id.callerIdSetupAction);
        contactSyncToggle = findViewById(R.id.contactNameSyncToggle);
        overlayTest = findViewById(R.id.callerIdTestPopup);
        privacyGroup = findViewById(R.id.callerIdPrivacyGroup);

        View back = findViewById(R.id.callerIdSetupBack);
        back.setVisibility(requiredSetup ? View.GONE : View.VISIBLE);
        back.setOnClickListener(v -> finish());
        action.setOnClickListener(v -> beginSetup());
        contactSyncToggle.setOnClickListener(v -> toggleContactNameSync());
        overlayTest.setOnClickListener(v -> showIncomingOverlayTest());
        findViewById(R.id.postCallTestPopup).setOnClickListener(v -> showPostCallTestPopup());
        findViewById(R.id.callerIdNotificationSettings).setOnClickListener(v ->
                CallPopupNotificationManager.openChannelSettings(
                        this, CallPopupNotificationManager.INCOMING_CHANNEL_ID));
        findViewById(R.id.postCallNotificationSettings).setOnClickListener(v ->
                CallPopupNotificationManager.openChannelSettings(
                        this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID));

        if (requiredSetup) {
            title.setText("필수 전화 화면 설정");
            intro.setText("전화가 오기 전에\n고객명 옆에 최근 메모를 준비합니다.");
        }
        bindPrivacyOptions();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CallPopupNotificationManager.ensureChannels(this);
        SetupRequirements.invalidateTestWhenPrerequisitesMissing(this);
        render();
    }

    private void bindPrivacyOptions() {
        int mode = SettingsStore.callerPrivacyMode(this);
        int checkedId = mode == SettingsStore.CALLER_PRIVACY_NAME
                ? R.id.callerIdPrivacyName
                : mode == SettingsStore.CALLER_PRIVACY_STAGE
                ? R.id.callerIdPrivacyStage
                : R.id.callerIdPrivacyMemo;
        ((RadioButton) findViewById(checkedId)).setChecked(true);
        privacyGroup.setOnCheckedChangeListener((group, id) -> {
            int selected = id == R.id.callerIdPrivacyName
                    ? SettingsStore.CALLER_PRIVACY_NAME
                    : id == R.id.callerIdPrivacyStage
                    ? SettingsStore.CALLER_PRIVACY_STAGE
                    : SettingsStore.CALLER_PRIVACY_MEMO;
            SettingsStore.setCallerPrivacyMode(this, selected);
        });
    }

    private void beginSetup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "이 기능은 Android 10 이상에서 지원됩니다.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> missing = missingRuntimePermissions();
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQUEST_RUNTIME_PERMISSIONS);
            return;
        }
        if (!SettingsStore.isContactNameSyncEnabled(this)) {
            ContactNameSyncManager.enable(this);
            Toast.makeText(this,
                    "연락처 이름에 최근 메모 표시를 켰습니다.", Toast.LENGTH_LONG).show();
        }
        finishSetup();
    }

    private List<String> missingRuntimePermissions() {
        List<String> missing = new ArrayList<>();
        if (!SetupRequirements.hasContacts(this)) missing.add(Manifest.permission.READ_CONTACTS);
        if (!SetupRequirements.hasContactWrite(this)) missing.add(Manifest.permission.WRITE_CONTACTS);
        if (!SetupRequirements.hasPhoneState(this)) missing.add(Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !SetupRequirements.hasPhoneNumbers(this)) {
            missing.add(Manifest.permission.READ_PHONE_NUMBERS);
        }
        if (!SetupRequirements.hasCallLog(this)) missing.add(Manifest.permission.READ_CALL_LOG);
        if (FeatureEntitlementStore.hasMessageAccess(this) && !SetupRequirements.hasSms(this)) {
            missing.add(Manifest.permission.SEND_SMS);
        }
        if (!SetupRequirements.hasNotifications(this)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return missing;
    }

    private void requestScreeningRole() {
        RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this,
                    "이 휴대전화에서는 상세 수신 오버레이를 사용할 수 없습니다. 연락처 메모 표시는 그대로 작동합니다.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            showIncomingOverlayTest();
            return;
        }
        startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                REQUEST_SCREENING_ROLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RUNTIME_PERMISSIONS) return;
        if (missingRuntimePermissions().isEmpty()) {
            beginSetup();
        } else {
            Toast.makeText(this,
                    "연락처 이름 표시와 통화 관리에 필요한 권한을 모두 허용해야 합니다.",
                    Toast.LENGTH_LONG).show();
            render();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SCREENING_ROLE) return;
        if (resultCode == RESULT_OK) {
            Toast.makeText(this,
                    "수신정보 앱 설정을 완료했습니다. 오버레이 테스트를 다시 눌러주세요.",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,
                    "상세 오버레이는 선택 기능입니다. 연락처 이름의 메모 표시는 그대로 사용됩니다.",
                    Toast.LENGTH_LONG).show();
        }
        render();
    }

    private void toggleContactNameSync() {
        if (!ContactNameSyncManager.hasPermissions(this)) {
            beginSetup();
            return;
        }
        if (!SettingsStore.isContactNameSyncEnabled(this)) {
            ContactNameSyncManager.enable(this);
            Toast.makeText(this,
                    "고객명 옆에 최근 메모를 표시합니다.", Toast.LENGTH_LONG).show();
            render();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("연락처 메모 표시 끄기")
                .setMessage("콜태그가 만든 연락처만 삭제합니다. Google·삼성 원본 연락처는 그대로 유지됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("끄고 복원", (dialog, which) -> {
                    ContactNameSyncManager.disableAndRestore(this);
                    SetupRequirements.clearInitialFlow(this);
                    Toast.makeText(this,
                            "콜태그 연락처를 삭제하고 원래 표시로 복원하고 있습니다.",
                            Toast.LENGTH_LONG).show();
                    render();
                })
                .show();
    }

    private void render() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            status.setText("Android 10 이상에서 사용할 수 있습니다.");
            action.setEnabled(false);
            action.setAlpha(0.5f);
            action.setText("지원되지 않는 기기");
            overlayTest.setEnabled(false);
            return;
        }

        boolean contacts = SetupRequirements.hasContacts(this);
        boolean contactWrite = SetupRequirements.hasContactWrite(this);
        boolean phoneState = SetupRequirements.hasPhoneState(this);
        boolean phoneNumbers = SetupRequirements.hasPhoneNumbers(this);
        boolean callLog = SetupRequirements.hasCallLog(this);
        boolean notifications = SetupRequirements.hasNotifications(this);
        boolean smsReady = !FeatureEntitlementStore.hasMessageAccess(this)
                || SetupRequirements.hasSms(this);
        boolean nameSync = SettingsStore.isContactNameSyncEnabled(this);
        boolean roleHeld = SetupRequirements.hasScreeningRole(this);
        boolean overlay = SetupRequirements.hasOverlay(this);
        boolean postCallPopup = SetupRequirements.hasPostCallPopup(this);
        long checkedAt = SettingsStore.lastCallerScreeningAt(this);
        String diagnostic = SettingsStore.lastCallerScreeningStatus(this);
        String diagnosticAt = checkedAt <= 0L ? ""
                : " · " + new SimpleDateFormat("M/d a h:mm", Locale.KOREA)
                .format(new Date(checkedAt));

        String smsStatus = FeatureEntitlementStore.hasMessageAccess(this)
                ? "\nSMS  " + state(smsReady) : "";
        status.setText("연락처 읽기  " + state(contacts)
                + "\n연락처 이름 수정  " + state(contactWrite)
                + "\n전화 상태  " + state(phoneState)
                + "\n전화번호  " + state(phoneNumbers)
                + "\n통화기록  " + state(callLog)
                + "\n알림  " + state(notifications)
                + smsStatus
                + "\n연락처 이름에 최근 메모  " + state(nameSync)
                + "\n\n동기화 상태"
                + "\n" + SettingsStore.contactNameSyncStatus(this)
                + "\n\n선택 기능"
                + "\n상세 수신정보 앱  " + optionalState(roleHeld)
                + "\n전화 화면 위 오버레이  " + optionalState(overlay)
                + "\n통화 종료 알림  " + optionalState(postCallPopup)
                + "\n\n최근 수신 확인" + diagnosticAt
                + "\n" + diagnostic);

        boolean runtimeReady = contacts && contactWrite && phoneState && phoneNumbers
                && callLog && notifications && smsReady;
        boolean complete = runtimeReady && nameSync;

        action.setEnabled(true);
        action.setAlpha(1f);
        contactSyncToggle.setEnabled(contacts && contactWrite);
        contactSyncToggle.setAlpha(contacts && contactWrite ? 1f : 0.45f);
        contactSyncToggle.setText(nameSync
                ? "연락처 메모 표시 끄기 · 원본 유지"
                : "연락처 이름에 최근 메모 표시 켜기");
        overlayTest.setEnabled(complete);
        overlayTest.setAlpha(complete ? 1f : 0.45f);

        if (complete) {
            action.setText("설정 완료 · 앱 시작");
        } else if (!runtimeReady) {
            action.setText("필수 권한 모두 허용");
        } else {
            action.setText("연락처 이름에 최근 메모 표시 켜기");
        }
    }

    private void showIncomingOverlayTest() {
        if (!SetupRequirements.baseReady(this)) {
            beginSetup();
            return;
        }
        if (!SetupRequirements.hasScreeningRole(this)) {
            requestScreeningRole();
            return;
        }
        if (!SetupRequirements.hasOverlay(this)) {
            Toast.makeText(this,
                    "상세 오버레이를 시험하려면 다른 앱 위 표시를 허용해주세요.",
                    Toast.LENGTH_LONG).show();
            CallerOverlayManager.openPermissionSettings(this);
            return;
        }
        SetupRequirements.clearOverlayTest(this);
        Toast.makeText(this,
                "선택 기능입니다. 실제 등록 고객의 상세 오버레이를 표시합니다.",
                Toast.LENGTH_LONG).show();
        moveTaskToBack(true);
        handler.postDelayed(() -> {
            boolean requested = CallerOverlayManager.showSetupTest(this);
            if (!requested) {
                runOnUiThread(() -> Toast.makeText(this,
                        "상세 오버레이를 표시하지 못했습니다. 연락처 메모 표시는 정상 사용됩니다.",
                        Toast.LENGTH_LONG).show());
            }
        }, 1200L);
    }

    private void showPostCallTestPopup() {
        if (!SetupRequirements.hasPostCallPopup(this)) {
            CallPopupNotificationManager.openChannelSettings(
                    this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
            return;
        }
        long now = System.currentTimeMillis();
        Customer demo = new Customer(
                -1L, "테스트 고객", "010-1234-5678", "01012345678",
                "진행 중", "", "견적서 수정 후 다시 연락하기",
                now - 86_400_000L, now - 3_600_000L, null);
        CallRecord record = new CallRecord(
                now, demo.primaryPhone, demo.displayName,
                CallLog.Calls.INCOMING_TYPE, now - 60_000L, 60L);
        Intent detail = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        moveTaskToBack(true);
        handler.postDelayed(() -> CallPopupNotificationManager.showPostCall(
                this, record, demo, detail, demo.memo), 1200L);
    }

    private void finishSetup() {
        ContactNameSyncManager.requestSyncAll(this);
        SetupRequirements.markInitialFlowCompleted(this);
        SetupRequirements.startCallMonitoring(this);
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    private String state(boolean complete) {
        return complete ? "완료" : "필요";
    }

    private String optionalState(boolean complete) {
        return complete ? "사용 중" : "선택";
    }

    @Override
    public void onBackPressed() {
        if (requiredSetup && !SetupRequirements.baseReady(this)) {
            moveTaskToBack(true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
