package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
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
        overlayTest = findViewById(R.id.callerIdTestPopup);
        privacyGroup = findViewById(R.id.callerIdPrivacyGroup);

        View back = findViewById(R.id.callerIdSetupBack);
        back.setVisibility(requiredSetup ? View.GONE : View.VISIBLE);
        back.setOnClickListener(v -> finish());
        action.setOnClickListener(v -> beginSetup());
        overlayTest.setOnClickListener(v -> showIncomingOverlayTest());
        findViewById(R.id.postCallTestPopup).setOnClickListener(v -> showPostCallTestPopup());
        findViewById(R.id.callerIdNotificationSettings).setOnClickListener(v ->
                CallPopupNotificationManager.openChannelSettings(
                        this, CallPopupNotificationManager.INCOMING_CHANNEL_ID));
        findViewById(R.id.postCallNotificationSettings).setOnClickListener(v ->
                CallPopupNotificationManager.openChannelSettings(
                        this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID));

        if (requiredSetup) {
            title.setText("필수 초기 설정");
            intro.setText("전화가 왔을 때 고객정보가 보이도록\n처음 한 번만 설정합니다.");
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
        if (!SetupRequirements.hasScreeningRole(this)) {
            requestScreeningRole();
            return;
        }
        if (!SetupRequirements.hasOverlay(this)) {
            Toast.makeText(this,
                    "콜태그의 ‘다른 앱 위에 표시’를 허용해주세요.", Toast.LENGTH_LONG).show();
            CallerOverlayManager.openPermissionSettings(this);
            return;
        }
        if (!postCallPopupReady()) {
            Toast.makeText(this,
                    "‘통화 종료 정리 팝업’의 알림을 허용해주세요.", Toast.LENGTH_LONG).show();
            CallPopupNotificationManager.openChannelSettings(
                    this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
            return;
        }
        if (!SetupRequirements.overlayTestPassed(this)) {
            showIncomingOverlayTest();
            return;
        }
        finishSetup();
    }

    private List<String> missingRuntimePermissions() {
        List<String> missing = new ArrayList<>();
        if (!SetupRequirements.hasContacts(this)) missing.add(Manifest.permission.READ_CONTACTS);
        if (!SetupRequirements.hasPhoneState(this)) missing.add(Manifest.permission.READ_PHONE_STATE);
        if (!SetupRequirements.hasCallLog(this)) missing.add(Manifest.permission.READ_CALL_LOG);
        if (!SetupRequirements.hasNotifications(this)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return missing;
    }

    private void requestScreeningRole() {
        RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "이 휴대전화에서는 수신정보 역할을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            beginSetup();
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
                    "전화 고객정보 기능에 필요한 권한을 모두 허용해야 합니다.",
                    Toast.LENGTH_LONG).show();
            render();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SCREENING_ROLE) return;
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, "콜태그를 수신정보 앱으로 설정했습니다.", Toast.LENGTH_SHORT).show();
            beginSetup();
        } else {
            Toast.makeText(this, "수신정보 앱 설정이 완료되지 않았습니다.", Toast.LENGTH_SHORT).show();
        }
        render();
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
        boolean phoneState = SetupRequirements.hasPhoneState(this);
        boolean callLog = SetupRequirements.hasCallLog(this);
        boolean notifications = SetupRequirements.hasNotifications(this);
        boolean roleHeld = SetupRequirements.hasScreeningRole(this);
        boolean overlay = SetupRequirements.hasOverlay(this);
        boolean postCallPopup = postCallPopupReady();
        boolean testPassed = SetupRequirements.overlayTestPassed(this);
        long checkedAt = SettingsStore.lastCallerScreeningAt(this);
        String diagnostic = SettingsStore.lastCallerScreeningStatus(this);
        String diagnosticAt = checkedAt <= 0L ? ""
                : " · " + new SimpleDateFormat("M/d a h:mm", Locale.KOREA)
                .format(new Date(checkedAt));

        status.setText("연락처  " + state(contacts)
                + "\n전화 상태  " + state(phoneState)
                + "\n통화기록  " + state(callLog)
                + "\n알림  " + state(notifications)
                + "\n수신정보 앱  " + state(roleHeld)
                + "\n다른 앱 위 표시  " + state(overlay)
                + "\n통화 종료 알림  " + state(postCallPopup)
                + "\n실제 오버레이 테스트  " + state(testPassed)
                + "\n\n최근 수신 확인" + diagnosticAt
                + "\n" + diagnostic);

        boolean runtimeReady = contacts && phoneState && callLog && notifications;
        boolean baseReady = runtimeReady && roleHeld && overlay && postCallPopup;
        boolean complete = baseReady && testPassed;

        action.setEnabled(true);
        action.setAlpha(1f);
        overlayTest.setEnabled(baseReady);
        overlayTest.setAlpha(baseReady ? 1f : 0.45f);

        if (complete) {
            action.setText("설정 완료 · 앱 시작");
        } else if (!runtimeReady) {
            action.setText("필수 권한 모두 허용");
        } else if (!roleHeld) {
            action.setText("콜태그를 수신정보 앱으로 선택");
        } else if (!overlay) {
            action.setText("다른 앱 위 표시 허용");
        } else if (!postCallPopup) {
            action.setText("통화 종료 알림 허용");
        } else {
            action.setText("실제 수신 화면 테스트");
        }
    }

    private void showIncomingOverlayTest() {
        if (!SetupRequirements.baseReady(this) || !postCallPopupReady()) {
            beginSetup();
            return;
        }
        SetupRequirements.clearOverlayTest(this);
        Toast.makeText(this,
                "앱을 뒤로 보냅니다. 표시된 카드에서 ‘정상적으로 보입니다’를 눌러주세요.",
                Toast.LENGTH_LONG).show();
        moveTaskToBack(true);
        handler.postDelayed(() -> {
            boolean requested = CallerOverlayManager.showSetupTest(this);
            if (!requested) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "오버레이를 표시하지 못했습니다.", Toast.LENGTH_LONG).show();
                    startActivity(SetupRequirements.requiredSetupIntent(this));
                });
            }
        }, 1200L);
    }

    private void showPostCallTestPopup() {
        if (!postCallPopupReady()) {
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

    private boolean postCallPopupReady() {
        return CallPopupNotificationManager.isPopupReady(
                this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
    }

    private void finishSetup() {
        SetupRequirements.startCallMonitoring(this);
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    private String state(boolean complete) {
        return complete ? "완료" : "필요";
    }

    @Override
    public void onBackPressed() {
        if (requiredSetup && !SetupRequirements.isReady(this)) {
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
