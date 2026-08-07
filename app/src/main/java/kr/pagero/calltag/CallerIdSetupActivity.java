package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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

    private TextView status;
    private TextView title;
    private TextView intro;
    private Button action;
    private Button contactSyncToggle;
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

        View back = findViewById(R.id.callerIdSetupBack);
        back.setVisibility(requiredSetup ? View.GONE : View.VISIBLE);
        back.setOnClickListener(v -> finish());
        action.setOnClickListener(v -> beginSetup());
        contactSyncToggle.setVisibility(View.GONE);

        if (requiredSetup) {
            title.setText("전화 고객정보 설정");
            intro.setText("연락처와 전화앱 기록은 변경하지 않고\n콜태그가 고객명·최근 메모를 직접 표시합니다.");
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
        finishSetup();
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
        if (FeatureEntitlementStore.hasMessageAccess(this) && !SetupRequirements.hasSms(this)) {
            missing.add(Manifest.permission.SEND_SMS);
        }
        if (!SetupRequirements.hasNotifications(this)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return missing;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RUNTIME_PERMISSIONS) return;
        if (missingCoreRuntimePermissions().isEmpty()) {
            finishSetup();
        } else {
            Toast.makeText(this,
                    "전화 고객관리에 필요한 권한을 모두 허용해주세요.",
                    Toast.LENGTH_LONG).show();
            render();
        }
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

        boolean coreReady = SetupRequirements.hasRequiredRuntimePermissions(this);
        if (!coreReady) {
            status.setText("설정을 완료하면 수신 고객정보와 통화 후 고객관리 기능을 사용할 수 있습니다.");
            action.setText("필수 권한 허용하고 시작");
        } else if (SetupRequirements.hasScreeningRole(this)) {
            status.setText("발신자 정보 서비스가 준비되었습니다.\n연락처와 시스템 통화목록은 변경하지 않습니다.");
            action.setText("앱 시작");
        } else {
            status.setText("기본 고객관리 기능은 준비되었습니다.\n다음 단계에서 발신자 정보 역할을 켜면 수신 시 고객 메모를 표시할 수 있습니다.");
            action.setText("계속 설정");
        }

        action.setEnabled(true);
        action.setAlpha(1f);
    }

    private void finishSetup() {
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
        new AlertDialog.Builder(this)
                .setTitle("설정을 마치지 않고 나갈까요?")
                .setMessage("필수 전화 고객관리 설정을 완료해야 합니다.")
                .setNegativeButton("계속 설정", null)
                .setPositiveButton("나가기", (dialog, which) -> moveTaskToBack(true))
                .show();
    }
}
