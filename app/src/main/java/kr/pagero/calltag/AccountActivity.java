package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public final class AccountActivity extends Activity {
    private TextView profile;
    private TextView refresh;
    private TextView delete;
    private boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        profile = findViewById(R.id.accountProfile);
        refresh = findViewById(R.id.accountRefresh);
        delete = findViewById(R.id.accountDelete);

        findViewById(R.id.accountBack).setOnClickListener(v -> {
            if (!working) finish();
        });
        refresh.setOnClickListener(v -> refreshAccount());
        findViewById(R.id.accountLogout).setOnClickListener(v -> confirmLogout());
        delete.setOnClickListener(v -> confirmDeleteAccount());
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        StringBuilder value = new StringBuilder();
        append(value, "이름", AuthSessionStore.name(this));
        append(value, "연락처", formatPhone(AuthSessionStore.phone(this)));
        append(value, "이메일", AuthSessionStore.email(this));
        append(value, "브랜드", AuthSessionStore.brand(this));
        append(value, "업종", AuthSessionStore.industry(this));
        profile.setText(value.length() == 0 ? "회원정보를 불러오지 못했어요." : value.toString());
    }

    private void append(StringBuilder target, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (target.length() > 0) target.append("\n");
        target.append(label).append("  ").append(value.trim());
    }

    private void refreshAccount() {
        if (working) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            logout();
            return;
        }
        setWorking(true, "확인 중…");
        new Thread(() -> {
            try {
                JSONObject response = AuthApiClient.refresh(session);
                AuthSessionStore.save(this, response);
                runOnUiThread(() -> {
                    setWorking(false, "회원정보 다시 불러오기");
                    render();
                    PageroAccountConnectionManager.refresh(this, false);
                    CallTagPushManager.registerIfAvailable(this);
                    Toast.makeText(this, "최신 회원정보를 불러왔어요.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setWorking(false, "회원정보 다시 불러오기");
                    Toast.makeText(this,
                            "인터넷 연결을 확인한 뒤 다시 시도해주세요.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "calltag-account-refresh").start();
    }

    private void confirmLogout() {
        if (working) return;
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("로그아웃")
                .setMessage("자동문자와 새 문의 알림이 멈춥니다. 이 휴대전화의 고객·일정·발송 기록은 그대로 유지됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("로그아웃", (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        String session = AuthSessionStore.session(this);
        stopService(new Intent(this, CallMonitorService.class));
        SettingsStore.setMonitorEnabled(this, false);
        CallTagPushManager.unregisterBestEffort(this, session);
        PageroAccountStatusStore.clear(this);
        FeatureEntitlementStore.clear(this);
        ReferralStateStore.clear(this);
        AuthSessionStore.clear(this);
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    private void confirmDeleteAccount() {
        if (working) return;
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("회원탈퇴")
                .setMessage("콜태그 계정과 이 휴대전화의 고객정보·통화기록·상담메모·일정·문자 발송기록을 모두 삭제합니다. 되돌릴 수 없습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("탈퇴하기", (dialog, which) -> confirmDeleteAgain())
                .show();
    }

    private void confirmDeleteAgain() {
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("정말 탈퇴하시겠습니까?")
                .setMessage("콜태그 계정과 앱 데이터를 영구 삭제합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("계정 삭제", (dialog, which) -> deleteAccount())
                .show();
    }

    private void deleteAccount() {
        if (working) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            logout();
            return;
        }
        working = true;
        delete.setEnabled(false);
        delete.setText("탈퇴 처리 중…");
        refresh.setEnabled(false);
        new Thread(() -> {
            try {
                AuthApiClient.deleteAccount(session);
                runOnUiThread(() -> finishAccountDeletion(session));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    working = false;
                    delete.setEnabled(true);
                    delete.setText("회원탈퇴");
                    refresh.setEnabled(true);
                    Toast.makeText(this,
                            "회원탈퇴를 처리하지 못했어요. 잠시 후 다시 시도해주세요.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "calltag-account-delete").start();
    }

    private void finishAccountDeletion(String session) {
        stopService(new Intent(this, CallMonitorService.class));
        CallTagPushManager.unregisterBestEffort(this, session);
        PageroAccountStatusStore.clear(this);
        FeatureEntitlementStore.clear(this);
        ReferralStateStore.clear(this);
        getSharedPreferences("calltag_settings", MODE_PRIVATE).edit().clear().commit();
        getSharedPreferences("calltag_message_automation", MODE_PRIVATE).edit().clear().commit();
        AuthSessionStore.clear(this);
        for (String databaseName : databaseList()) {
            deleteDatabase(databaseName);
        }
        Toast.makeText(this, "회원탈퇴가 완료됐어요.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    private void setWorking(boolean value, String refreshLabel) {
        working = value;
        refresh.setEnabled(!value);
        refresh.setAlpha(value ? 0.6f : 1f);
        refresh.setText(refreshLabel);
        delete.setEnabled(!value);
    }

    private String formatPhone(String raw) {
        String value = PhoneNumberNormalizer.normalize(raw);
        if (value.length() == 11) {
            return value.substring(0, 3) + "-" + value.substring(3, 7) + "-" + value.substring(7);
        }
        if (value.length() == 10) {
            return value.substring(0, 3) + "-" + value.substring(3, 6) + "-" + value.substring(6);
        }
        return raw == null ? "" : raw;
    }
}
