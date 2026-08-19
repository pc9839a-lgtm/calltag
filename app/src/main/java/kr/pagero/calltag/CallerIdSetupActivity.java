package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** 사용자에게 필요한 발신자 정보 및 통화 후 고객관리 설정만 노출한다. */
public final class CallerIdSetupActivity extends Activity {
    public static final String EXTRA_REQUIRED_SETUP = "required_setup";

    private static final int REQUEST_RUNTIME_PERMISSIONS = 7301;
    private static final int REQUEST_CALL_SCREENING_ROLE = 7302;
    private static final int REQUEST_OVERLAY_PERMISSION = 7303;

    private TextView status;
    private TextView title;
    private TextView intro;
    private Button action;
    private Button contactSyncToggle;
    private boolean requiredSetup;
    private boolean roleRequestInFlight;
    private boolean overlayRequestInFlight;

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

        View back = findViewById(R.id.callerIdSetupBack);
        back.setVisibility(requiredSetup ? View.GONE : View.VISIBLE);
        back.setOnClickListener(v -> finish());
        action.setOnClickListener(v -> beginSetup());
        contactSyncToggle.setVisibility(View.GONE);

        if (requiredSetup) {
            title.setText("전화 고객정보 설정");
            intro.setText("통화가 끝나면 앱 전체화면을 열지 않고\n작은 콜태그 팝업으로 메모를 남깁니다.");
        }
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CallPopupNotificationManager.ensureChannels(this);
        render();
    }

    private void beginSetup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "Android 10 이상에서 사용할 수 있습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> missing = missingCoreRuntimePermissions();
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQUEST_RUNTIME_PERMISSIONS);
            return;
        }
        ensurePostCallOverlayOrContinue();
    }

    private List<String> missingCoreRuntimePermissions() {
        List<String> missing = new ArrayList<>();
        if (!SetupRequirements.hasContacts(this)) missing.add(Manifest.permission.READ_CONTACTS);
        if (!SetupRequirements.hasPhoneState(this)) missing.add(Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !SetupRequirements.hasPhoneNumbers(this)) {
            missing.add(Manifest.permission.READ_PHONE_NUMBERS);
        }
        if (!SetupRequirements.hasCallLog(this)) missing.add(Manifest.permission.READ_CALL_LOG);
        return missing;
    }

    private void ensurePostCallOverlayOrContinue() {
        if (SetupRequirements.hasOverlay(this)) {
            ensureScreeningRoleOrFinish();
            return;
        }
        requestOverlayPermission();
    }

    private void requestOverlayPermission() {
        if (overlayRequestInFlight || SetupRequirements.hasOverlay(this)) {
            ensureScreeningRoleOrFinish();
            return;
        }
        overlayRequestInFlight = true;
        action.setEnabled(false);
        action.setAlpha(0.6f);
        action.setText("팝업 권한 확인 중…");
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } catch (RuntimeException error) {
            overlayRequestInFlight = false;
            CrashTelemetryStore.record(this, "post_call_overlay_permission", "open_failed",
                    error.getClass().getSimpleName());
            Toast.makeText(this,
                    "통화 후 팝업 권한 설정을 열지 못했습니다. 다시 시도해주세요.",
                    Toast.LENGTH_LONG).show();
            render();
        }
    }

    private void ensureScreeningRoleOrFinish() {
        if (SetupRequirements.hasScreeningRole(this)) {
            finishSetup();
            return;
        }
        requestScreeningRole();
    }

    private void requestScreeningRole() {
        if (roleRequestInFlight || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;

        RoleManager manager = (RoleManager) getSystemService(ROLE_SERVICE);
        if (manager == null || !manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this,
                    "이 기기에서 발신자 정보 역할을 사용할 수 없습니다. 통화 후 고객관리는 계속 사용할 수 있습니다.",
                    Toast.LENGTH_LONG).show();
            finishCoreSetupWithoutScreening();
            return;
        }
        if (manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            finishSetup();
            return;
        }

        roleRequestInFlight = true;
        action.setEnabled(false);
        action.setAlpha(0.6f);
        action.setText("발신자 정보 권한 확인 중…");
        try {
            startActivityForResult(
                    manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                    REQUEST_CALL_SCREENING_ROLE);
        } catch (RuntimeException error) {
            roleRequestInFlight = false;
            CrashTelemetryStore.record(this, "caller_screening_role", "request_failed",
                    error.getClass().getSimpleName());
            Toast.makeText(this,
                    "발신자 정보 설정을 열지 못했습니다. 다시 시도해주세요.",
                    Toast.LENGTH_LONG).show();
            render();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RUNTIME_PERMISSIONS) return;
        if (missingCoreRuntimePermissions().isEmpty()) {
            ensurePostCallOverlayOrContinue();
        } else {
            Toast.makeText(this,
                    "전화 고객관리에 필요한 권한을 모두 허용해주세요.",
                    Toast.LENGTH_LONG).show();
            render();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            overlayRequestInFlight = false;
            if (SetupRequirements.hasOverlay(this)) {
                CrashTelemetryStore.record(this, "post_call_overlay_permission", "granted", "");
                ensureScreeningRoleOrFinish();
            } else {
                CrashTelemetryStore.record(this, "post_call_overlay_permission", "not_granted", "");
                Toast.makeText(this,
                        "통화 종료 후 작은 팝업을 사용하려면 ‘다른 앱 위에 표시’를 허용해주세요.",
                        Toast.LENGTH_LONG).show();
                render();
            }
            return;
        }
        if (requestCode != REQUEST_CALL_SCREENING_ROLE) return;
        roleRequestInFlight = false;
        if (resultCode == RESULT_OK && SetupRequirements.hasScreeningRole(this)) {
            CrashTelemetryStore.record(this, "caller_screening_role", "granted", "");
            finishSetup();
            return;
        }

        CrashTelemetryStore.record(this, "caller_screening_role", "not_granted", "");
        Toast.makeText(this,
                "수신 화면에 고객명·최근 메모를 표시하려면 발신자 정보 역할을 허용해주세요.",
                Toast.LENGTH_LONG).show();
        render();
    }

    private void render() {
        contactSyncToggle.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            status.setText("이 기기에서는 발신자 정보 기능을 사용할 수 없습니다.");
            action.setEnabled(false);
            action.setAlpha(0.5f);
            action.setText("지원되지 않는 기기");
            return;
        }

        boolean coreReady = SetupRequirements.hasCoreRuntimePermissions(this);
        if (!coreReady) {
            status.setText("설정을 완료하면 수신 고객정보와 통화 후 고객관리 기능을 사용할 수 있습니다.");
            action.setText("필수 권한 허용하고 시작");
        } else if (!SetupRequirements.hasOverlay(this)) {
            status.setText("통화 종료 후 앱을 열지 않고 작은 팝업만 표시하려면\n‘다른 앱 위에 표시’ 권한이 필요합니다.");
            action.setText("통화 후 팝업 권한 허용");
        } else if (SetupRequirements.hasScreeningRole(this)) {
            status.setText("전화 고객정보와 통화 후 작은 팝업이 준비되었습니다.\n연락처와 시스템 통화목록은 변경하지 않습니다.");
            action.setText("앱 시작");
        } else {
            status.setText("통화 후 작은 팝업은 준비되었습니다.\n발신자 정보 역할을 켜면 수신 시 고객명·최근 메모도 표시합니다.");
            action.setText("발신자 정보 역할 켜기");
        }

        if (!roleRequestInFlight && !overlayRequestInFlight) {
            action.setEnabled(true);
            action.setAlpha(1f);
        }
    }

    private void finishSetup() {
        if (!SetupRequirements.hasOverlay(this)) {
            requestOverlayPermission();
            return;
        }
        if (!SetupRequirements.hasScreeningRole(this)) {
            requestScreeningRole();
            return;
        }
        finishCoreSetup();
    }

    private void finishCoreSetupWithoutScreening() {
        if (!SetupRequirements.hasOverlay(this)) {
            requestOverlayPermission();
            return;
        }
        CrashTelemetryStore.record(this, "caller_screening_role", "unavailable", "");
        finishCoreSetup();
    }

    private void finishCoreSetup() {
        ContactNameSyncManager.disableAndRestore(this);
        SetupRequirements.markInitialFlowCompleted(this);
        SetupRequirements.startCallMonitoring(this);
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (!requiredSetup) {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("설정을 마치지 않고 나갈까요?")
                .setMessage("통화 후 작은 팝업과 수신 고객정보를 사용하려면 필요한 권한을 설정해주세요.")
                .setNegativeButton("계속 설정", null)
                .setPositiveButton("나가기", (dialog, which) -> moveTaskToBack(true))
                .show();
    }
}
