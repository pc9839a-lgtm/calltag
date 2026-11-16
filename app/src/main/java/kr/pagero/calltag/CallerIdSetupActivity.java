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
    private Button action;
    private Button skip;
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
        action = findViewById(R.id.callerIdSetupAction);
        skip = findViewById(R.id.callerIdSetupSkip);

        View back = findViewById(R.id.callerIdSetupBack);
        back.setVisibility(requiredSetup ? View.GONE : View.VISIBLE);
        back.setOnClickListener(v -> finish());
        action.setOnClickListener(v -> beginSetup());
        skip.setOnClickListener(v -> {
            if (requiredSetup) finishCoreSetupWithoutScreening();
            else finish();
        });
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
        if (!SetupRequirements.hasOverlay(this)) requestOverlayPermission();
        else if (!SetupRequirements.hasScreeningRole(this)) launchScreeningRoleRequest();
        else finishCoreSetup();
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

    private void requestOverlayPermission() {
        if (overlayRequestInFlight || SetupRequirements.hasOverlay(this)) return;
        overlayRequestInFlight = true;
        action.setEnabled(false);
        action.setAlpha(0.6f);
        action.setText("확인 중…");
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } catch (RuntimeException error) {
            overlayRequestInFlight = false;
            CrashTelemetryStore.record(this, "post_call_overlay_permission", "open_failed",
                    error.getClass().getSimpleName());
            Toast.makeText(this,
                    "설정을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
            render();
        }
    }

    private void launchScreeningRoleRequest() {
        if (roleRequestInFlight || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;

        RoleManager manager = (RoleManager) getSystemService(ROLE_SERVICE);
        if (manager == null || !manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this,
                    "이 기기는 발신자 정보를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finishCoreSetupWithoutScreening();
            return;
        }
        if (manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            finishCoreSetup();
            return;
        }

        roleRequestInFlight = true;
        render();
        try {
            startActivityForResult(
                    manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                    REQUEST_CALL_SCREENING_ROLE);
        } catch (RuntimeException error) {
            roleRequestInFlight = false;
            CrashTelemetryStore.record(this, "caller_screening_role", "request_failed",
                    error.getClass().getSimpleName());
            Toast.makeText(this,
                    "설정을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
            render();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RUNTIME_PERMISSIONS) return;
        if (SetupRequirements.hasCoreRuntimePermissions(this)
                && SetupRequirements.hasOverlay(this) && SetupRequirements.hasScreeningRole(this)) {
            finishCoreSetup();
            return;
        }
        render();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            overlayRequestInFlight = false;
            if (SetupRequirements.hasOverlay(this)) {
                CrashTelemetryStore.record(this, "post_call_overlay_permission", "granted", "");
                if (SetupRequirements.hasScreeningRole(this)) finishCoreSetup();
                else render();
            } else {
                CrashTelemetryStore.record(this, "post_call_overlay_permission", "not_granted", "");
                render();
            }
            return;
        }
        if (requestCode != REQUEST_CALL_SCREENING_ROLE) return;
        roleRequestInFlight = false;
        if (resultCode == RESULT_OK && SetupRequirements.hasScreeningRole(this)) {
            CrashTelemetryStore.record(this, "caller_screening_role", "granted", "");
            finishCoreSetup();
            return;
        }

        CrashTelemetryStore.record(this, "caller_screening_role", "not_granted", "");
        render();
    }

    private void render() {
        skip.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            title.setText("전화 설정");
            status.setText("Android 10 이상에서 사용할 수 있습니다.");
            action.setEnabled(false);
            action.setAlpha(0.5f);
            action.setText("지원되지 않는 기기");
            return;
        }

        boolean coreReady = SetupRequirements.hasCoreRuntimePermissions(this);
        if (!coreReady) {
            title.setText("통화 후 기록");
            status.setText(InitialPermissionActivity.CORE_DISCLOSURE);
            action.setText("권한 허용");
        } else if (!SetupRequirements.hasOverlay(this)) {
            title.setText("통화 후 팝업");
            status.setText("통화가 끝나면 메모창을 띄웁니다. ‘다른 앱 위에 표시’를 허용해 주세요.");
            action.setText("설정 열기");
        } else if (SetupRequirements.hasScreeningRole(this)) {
            title.setText("설정 완료");
            status.setText("");
            action.setText("앱 시작");
        } else {
            title.setText("발신자 정보 (선택)");
            status.setText("전화가 오면 고객명과 최근 메모를 표시합니다. 기본 전화 앱은 그대로 사용합니다.");
            action.setText("발신자 정보 켜기");
            skip.setVisibility(View.VISIBLE);
        }

        boolean busy = roleRequestInFlight || overlayRequestInFlight;
        action.setEnabled(!busy);
        action.setAlpha(busy ? 0.6f : 1f);
        skip.setEnabled(!busy);
        if (busy) action.setText("확인 중…");
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
        if (!SetupRequirements.hasCoreRuntimePermissions(this) || !SetupRequirements.hasOverlay(this)) {
            render();
            return;
        }
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
                .setNegativeButton("계속 설정", null)
                .setPositiveButton("나가기", (dialog, which) -> moveTaskToBack(true))
                .show();
    }
}
