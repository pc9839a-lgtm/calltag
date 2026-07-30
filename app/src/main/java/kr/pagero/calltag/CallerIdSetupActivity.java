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
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CallerIdSetupActivity extends Activity {
    private static final int REQUEST_CONTACTS = 7301;
    private static final int REQUEST_SCREENING_ROLE = 7302;
    private static final int REQUEST_NOTIFICATIONS = 7303;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private Button action;
    private RadioGroup privacyGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caller_id_setup);
        CallPopupNotificationManager.ensureChannels(this);
        status = findViewById(R.id.callerIdSetupStatus);
        action = findViewById(R.id.callerIdSetupAction);
        privacyGroup = findViewById(R.id.callerIdPrivacyGroup);
        findViewById(R.id.callerIdSetupBack).setOnClickListener(v -> finish());
        action.setOnClickListener(v -> beginSetup());
        findViewById(R.id.callerIdTestPopup).setOnClickListener(v -> showIncomingTestPopup());
        findViewById(R.id.postCallTestPopup).setOnClickListener(v -> showPostCallTestPopup());
        findViewById(R.id.callerIdNotificationSettings).setOnClickListener(v ->
                CallPopupNotificationManager.openChannelSettings(
                        this, CallPopupNotificationManager.INCOMING_CHANNEL_ID));
        findViewById(R.id.postCallNotificationSettings).setOnClickListener(v ->
                CallPopupNotificationManager.openChannelSettings(
                        this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID));
        bindPrivacyOptions();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CallPopupNotificationManager.ensureChannels(this);
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
            Toast.makeText(this, "잠금화면 표시 범위를 저장했습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private void beginSetup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "이 기능은 Android 10 이상에서 지원됩니다.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!hasContactsPermission()) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACTS);
            return;
        }
        if (!hasNotificationPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        if (!hasScreeningRole()) {
            requestScreeningRole();
            return;
        }

        CallPopupNotificationManager.ensureChannels(this);
        if (!incomingPopupReady()) {
            Toast.makeText(this,
                    "‘전화 수신 고객정보 팝업’에서 알림 허용과 팝업 표시를 켜주세요.",
                    Toast.LENGTH_LONG).show();
            CallPopupNotificationManager.openChannelSettings(
                    this, CallPopupNotificationManager.INCOMING_CHANNEL_ID);
            return;
        }
        if (!postCallPopupReady()) {
            Toast.makeText(this,
                    "‘통화 종료 정리 팝업’에서 알림 허용과 팝업 표시를 켜주세요.",
                    Toast.LENGTH_LONG).show();
            CallPopupNotificationManager.openChannelSettings(
                    this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
            return;
        }
        Toast.makeText(this,
                "앱을 열지 않아도 전화 수신과 통화 종료 알림 팝업이 표시됩니다.",
                Toast.LENGTH_LONG).show();
        render();
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
        if (requestCode != REQUEST_CONTACTS && requestCode != REQUEST_NOTIFICATIONS) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            beginSetup();
        } else {
            Toast.makeText(this,
                    requestCode == REQUEST_CONTACTS
                            ? "고객 번호를 확인하려면 연락처 권한이 필요합니다."
                            : "전화 알림 팝업을 표시하려면 알림 권한이 필요합니다.",
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
            return;
        }

        boolean contacts = hasContactsPermission();
        boolean notifications = hasNotificationPermission();
        boolean roleHeld = hasScreeningRole();
        boolean incomingPopup = incomingPopupReady();
        boolean postCallPopup = postCallPopupReady();
        long checkedAt = SettingsStore.lastCallerScreeningAt(this);
        String diagnostic = SettingsStore.lastCallerScreeningStatus(this);
        String diagnosticAt = checkedAt <= 0L ? ""
                : " · " + new SimpleDateFormat("M/d a h:mm", Locale.KOREA)
                .format(new Date(checkedAt));

        status.setText("연락처 권한  " + state(contacts)
                + "\n알림 권한  " + state(notifications)
                + "\n수신정보 앱  " + state(roleHeld)
                + "\n전화 수신 팝업  " + state(incomingPopup)
                + "\n통화 종료 팝업  " + state(postCallPopup)
                + "\n\n최근 수신 확인" + diagnosticAt
                + "\n" + diagnostic);

        boolean complete = contacts && notifications && roleHeld && incomingPopup && postCallPopup;
        action.setEnabled(true);
        action.setAlpha(1f);
        if (complete) {
            action.setText("설정 완료 · 팝업 상태 확인");
        } else if (!contacts || !notifications) {
            action.setText("필수 권한 허용");
        } else if (!roleHeld) {
            action.setText("콜태그를 수신정보 앱으로 선택");
        } else {
            action.setText("알림 팝업 설정 확인");
        }
    }

    private void showIncomingTestPopup() {
        CallTagDbHelper db = new CallTagDbHelper(this);
        try {
            List<Customer> customers = db.listCustomers(null);
            if (customers.isEmpty()) {
                Toast.makeText(this, "테스트할 고객을 먼저 추가해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!incomingPopupReady()) {
                CallPopupNotificationManager.openChannelSettings(
                        this, CallPopupNotificationManager.INCOMING_CHANNEL_ID);
                return;
            }
            Customer customer = customers.get(0);
            String memo = CustomerInsightResolver.latestMemo(db, customer);
            String color = db.stageColor(customer.relationStatus);
            Toast.makeText(this, "앱을 뒤로 보냅니다. 2초 후 수신 팝업이 표시됩니다.", Toast.LENGTH_LONG).show();
            moveTaskToBack(true);
            handler.postDelayed(() -> CallPopupNotificationManager.showIncoming(
                    this, customer, memo, color), 2000L);
        } finally {
            db.close();
        }
    }

    private void showPostCallTestPopup() {
        CallTagDbHelper db = new CallTagDbHelper(this);
        try {
            List<Customer> customers = db.listCustomers(null);
            if (customers.isEmpty()) {
                Toast.makeText(this, "테스트할 고객을 먼저 추가해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!postCallPopupReady()) {
                CallPopupNotificationManager.openChannelSettings(
                        this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
                return;
            }
            Customer customer = customers.get(0);
            long now = System.currentTimeMillis();
            CallRecord record = new CallRecord(
                    now, customer.primaryPhone, customer.displayName,
                    CallLog.Calls.INCOMING_TYPE, now - 60_000L, 60L);
            Intent detail = new Intent(this, CustomerDetailActivity.class)
                    .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customer.id)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String memo = CustomerInsightResolver.latestMemo(db, customer);
            Toast.makeText(this, "앱을 뒤로 보냅니다. 2초 후 통화 종료 팝업이 표시됩니다.", Toast.LENGTH_LONG).show();
            moveTaskToBack(true);
            handler.postDelayed(() -> CallPopupNotificationManager.showPostCall(
                    this, record, customer, detail, memo), 2000L);
        } finally {
            db.close();
        }
    }

    private boolean incomingPopupReady() {
        return CallPopupNotificationManager.isPopupReady(
                this, CallPopupNotificationManager.INCOMING_CHANNEL_ID);
    }

    private boolean postCallPopupReady() {
        return CallPopupNotificationManager.isPopupReady(
                this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
    }

    private boolean hasContactsPermission() {
        return checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasScreeningRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
        RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
        return roleManager != null
                && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    private String state(boolean complete) {
        return complete ? "완료" : "필요";
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
