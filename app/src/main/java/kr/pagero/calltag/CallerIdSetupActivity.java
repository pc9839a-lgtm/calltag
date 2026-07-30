package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public final class CallerIdSetupActivity extends Activity {
    private static final int REQUEST_CONTACTS = 7301;
    private static final int REQUEST_SCREENING_ROLE = 7302;

    private TextView status;
    private Button action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caller_id_setup);
        status = findViewById(R.id.callerIdSetupStatus);
        action = findViewById(R.id.callerIdSetupAction);
        findViewById(R.id.callerIdSetupBack).setOnClickListener(v -> finish());
        action.setOnClickListener(v -> beginSetup());
        findViewById(R.id.callerIdNotificationSettings).setOnClickListener(v -> openNotificationSettings());
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void beginSetup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "이 기능은 Android 10 이상에서 지원됩니다.", Toast.LENGTH_LONG).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACTS);
            return;
        }
        requestScreeningRole();
    }

    private void requestScreeningRole() {
        RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "이 휴대전화에서는 수신정보 역할을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "이미 수신 고객정보 표시가 켜져 있습니다.", Toast.LENGTH_SHORT).show();
            render();
            return;
        }
        startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                REQUEST_SCREENING_ROLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CONTACTS) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestScreeningRole();
        } else {
            Toast.makeText(this,
                    "연락처에 저장된 고객까지 확인하려면 연락처 권한이 필요합니다.",
                    Toast.LENGTH_LONG).show();
            render();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SCREENING_ROLE) return;
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, "수신 고객정보 표시를 켰습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "수신 고객정보 표시가 켜지지 않았습니다.", Toast.LENGTH_SHORT).show();
        }
        render();
    }

    private void render() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            status.setText("Android 10 이상에서 사용할 수 있습니다.");
            action.setEnabled(false);
            action.setAlpha(0.5f);
            action.setText("지원되지 않는 기기");
            return;
        }
        RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
        boolean roleHeld = roleManager != null
                && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        boolean contacts = checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;

        if (roleHeld && contacts) {
            status.setText("사용 중\n전화가 오면 고객명·영업 단계·최근 메모가 상단에 표시됩니다.");
            action.setText("설정 완료");
            action.setEnabled(false);
            action.setAlpha(0.65f);
        } else {
            status.setText((contacts ? "연락처 권한 완료" : "1. 연락처 권한 허용")
                    + "\n2. 콜태그를 수신정보 앱으로 선택");
            action.setText("권한 설정 시작");
            action.setEnabled(true);
            action.setAlpha(1f);
        }
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (RuntimeException error) {
            Toast.makeText(this, "알림 설정을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
