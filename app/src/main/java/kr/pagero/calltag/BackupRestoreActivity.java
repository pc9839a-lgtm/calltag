package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public final class BackupRestoreActivity extends Activity {
    private static final int REQUEST_CREATE_BACKUP = 2101;
    private static final int REQUEST_OPEN_BACKUP = 2102;

    private Button createButton;
    private Button restoreButton;
    private Button legacyRecoveryButton;
    private TextView statusView;
    private boolean working;
    private char[] pendingBackupPassword;
    private Uri pendingRestoreUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        renderStatus();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(27f);
        back.setOnClickListener(v -> {
            if (!working) finish();
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = title("백업 및 복원", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        TextView label = title("최근 상태", 13f);
        label.setTextColor(getColor(R.color.text_secondary));
        root.addView(label, topMargin(16));

        statusView = body("기록을 확인하는 중입니다");
        statusView.setTextIsSelectable(true);
        statusView.setMaxLines(4);
        statusView.setEllipsize(TextUtils.TruncateAt.END);
        statusView.setBackgroundResource(R.drawable.bg_card);
        statusView.setPadding(dp(14), dp(13), dp(14), dp(13));
        root.addView(statusView, topMargin(8));

        createButton = button("암호화 백업 만들기", true);
        createButton.setOnClickListener(v -> showBackupPasswordDialog());
        createButton.setEnabled(true);
        root.addView(createButton, fixedHeight(52, 16));

        restoreButton = button("백업 파일 복원", false);
        restoreButton.setOnClickListener(v -> chooseRestoreFile());
        restoreButton.setEnabled(true);
        root.addView(restoreButton, fixedHeight(50, 8));

        if (LegacyCrmRecoveryManager.hasLegacyData(this)) {
            legacyRecoveryButton = button("구버전 고객·상담 기록 복구", false);
            legacyRecoveryButton.setOnClickListener(v -> confirmLegacyRecovery());
            root.addView(legacyRecoveryButton, fixedHeight(52, 14));
        }

        TextView format = body("콜태그 계정별 암호화 .ctbackup v2 · 로그인·결제 권한 제외");
        format.setGravity(Gravity.CENTER);
        format.setSingleLine(true);
        format.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(format, topMargin(10));

        TextView warning = body("현재 계정의 DB와 설정만 암호화합니다. 다른 계정, 소유자 미확인 구버전(v1) 백업, 공용 이미지 파일은 복원 대상이 아닙니다.");
        warning.setTextColor(getColor(R.color.danger));
        warning.setGravity(Gravity.CENTER_VERTICAL);
        warning.setPadding(dp(14), dp(11), dp(14), dp(11));
        warning.setBackgroundResource(R.drawable.bg_soft_panel);
        root.addView(warning, topMargin(18));
        return scroll;
    }

    private void confirmLegacyRecovery() {
        if (working) return;
        String owner = AccountDataScope.requireOwner(this);
        EditText confirmation = new EditText(this);
        confirmation.setSingleLine(true);
        confirmation.setHint("복구 입력");
        confirmation.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("구버전 고객 기록 복구")
                .setMessage("구버전 공용 DB는 고객마다 계정 ID가 없습니다. "
                        + "이 기기의 이전 고객·상담·일정 기록이 현재 로그인한 계정의 기록이 맞고, "
                        + "다른 사용자의 기록이 섞이지 않았을 때만 진행하세요. "
                        + "다른 계정 사용 흔적이나 현재 계정의 기존 고객 기록이 확인되면 차단됩니다. "
                        + "원본 DB는 삭제하지 않습니다. 진행하려면 '복구'를 입력하세요.")
                .setView(confirmation)
                .setNegativeButton("취소", null)
                .setPositiveButton("내 계정으로 복구", (dialog, which) -> {
                    if (!"복구".equals(confirmation.getText().toString().trim())) {
                        Toast.makeText(this, "'복구' 입력이 일치하지 않습니다.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    startLegacyRecovery(owner);
                })
                .show();
    }

    private void startLegacyRecovery(String owner) {
        if (working) return;
        setWorking(true, "원본을 보존하면서 이전 고객 기록을 복구 중입니다…");
        new Thread(() -> {
            try {
                LegacyCrmRecoveryManager.Result result =
                        LegacyCrmRecoveryManager.recoverAfterOwnerConfirmation(this, owner);
                runOnUiThread(() -> {
                    setWorking(false, "");
                    if (legacyRecoveryButton != null) legacyRecoveryButton.setEnabled(false);
                    new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                            .setTitle(result.alreadyRecovered ? "이미 복구된 기록" : "고객 기록 복구 완료")
                            .setMessage(result.alreadyRecovered
                                    ? "이 계정의 이전 기록이 이미 복구되었습니다."
                                    : "고객 " + result.customers + "건, 상담 " + result.interactions
                                      + "건, 일정 " + result.tasks
                                      + "건을 복구했습니다. 구버전 원본은 그대로 남아 있습니다.")
                            .setPositiveButton("앱 다시 시작", (d, w) -> restartApp())
                            .show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setWorking(false, "");
                    showError("복구 차단", error);
                });
            }
        }, "calltag-legacy-crm-recovery").start();
    }

    private void showBackupPasswordDialog() {
        if (working) return;
        LinearLayout form = dialogForm();
        EditText password = passwordField("백업 암호 8자 이상");
        EditText confirm = passwordField("암호 다시 입력");
        form.addView(password, matchWrap());
        form.addView(confirm, topMargin(8));

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("백업 암호 설정")
                .setMessage("암호는 저장되지 않습니다. 잊으면 복원할 수 없습니다.")
                .setView(form)
                .setNegativeButton("취소", null)
                .setPositiveButton("파일 선택", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String first = password.getText().toString();
                    String second = confirm.getText().toString();
                    if (first.length() < 8) {
                        password.setError("8자 이상 입력해주세요.");
                        return;
                    }
                    if (!first.equals(second)) {
                        confirm.setError("암호가 일치하지 않습니다.");
                        return;
                    }
                    clearPendingBackupPassword();
                    pendingBackupPassword = first.toCharArray();
                    password.setText("");
                    confirm.setText("");
                    dialog.dismiss();
                    chooseBackupTarget();
                }));
        dialog.show();
    }

    private void chooseBackupTarget() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/octet-stream")
                .putExtra(Intent.EXTRA_TITLE, defaultBackupName());
        try {
            startActivityForResult(intent, REQUEST_CREATE_BACKUP);
        } catch (RuntimeException error) {
            clearPendingBackupPassword();
            Toast.makeText(this, "파일 저장 화면을 열지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void chooseRestoreFile() {
        if (working) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/octet-stream");
        try {
            startActivityForResult(intent, REQUEST_OPEN_BACKUP);
        } catch (RuntimeException error) {
            Toast.makeText(this, "백업 파일 선택 화면을 열지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = resultCode == RESULT_OK && data != null ? data.getData() : null;
        if (requestCode == REQUEST_CREATE_BACKUP) {
            if (uri == null || pendingBackupPassword == null) {
                clearPendingBackupPassword();
                return;
            }
            runBackup(uri, pendingBackupPassword);
            pendingBackupPassword = null;
            return;
        }
        if (requestCode == REQUEST_OPEN_BACKUP && uri != null) {
            pendingRestoreUri = uri;
            showRestorePasswordDialog();
        }
    }

    private void showRestorePasswordDialog() {
        if (pendingRestoreUri == null || working) return;
        LinearLayout form = dialogForm();
        EditText password = passwordField("백업 암호");
        form.addView(password, matchWrap());
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("백업 암호 입력")
                .setView(form)
                .setNegativeButton("취소", (ignored, which) -> pendingRestoreUri = null)
                .setPositiveButton("다음", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = password.getText().toString();
                    if (value.length() < 8) {
                        password.setError("백업 암호를 입력해주세요.");
                        return;
                    }
                    char[] entered = value.toCharArray();
                    password.setText("");
                    dialog.dismiss();
                    confirmRestore(entered);
                }));
        dialog.show();
    }

    private void confirmRestore(char[] password) {
        Uri source = pendingRestoreUri;
        pendingRestoreUri = null;
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("현재 데이터를 교체할까요?")
                .setMessage("현재 로그인한 계정의 데이터만 교체됩니다. 실패 시 복원 전 데이터로 롤백합니다. 복원 후 클라우드 동기화는 검토한 뒤 직접 다시 켜야 합니다. 이미지는 복원하지 않습니다.")
                .setNegativeButton("취소", (dialog, which) -> Arrays.fill(password, '\0'))
                .setPositiveButton("복원 시작", (dialog, which) -> runRestore(source, password))
                .show();
    }

    private void runBackup(Uri target, char[] password) {
        if (working) {
            Arrays.fill(password, '\0');
            return;
        }
        setWorking(true, "암호화 백업을 만드는 중입니다…");
        new Thread(() -> {
            try {
                CallTagBackupManager.BackupResult result =
                        CallTagBackupManager.createBackup(this, target, password);
                runOnUiThread(() -> {
                    setWorking(false, "");
                    renderStatus();
                    new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                            .setTitle("백업 완료")
                            .setMessage(result.summary())
                            .setPositiveButton("확인", null)
                            .show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setWorking(false, "");
                    renderStatus();
                    showError("백업 실패", error);
                });
            } finally {
                Arrays.fill(password, '\0');
            }
        }, "calltag-backup-create").start();
    }

    private void runRestore(Uri source, char[] password) {
        if (working) {
            Arrays.fill(password, '\0');
            return;
        }
        setWorking(true, "백업 파일을 검증하고 복원하는 중입니다…");
        new Thread(() -> {
            try {
                CallTagBackupManager.RestoreResult result =
                        CallTagBackupManager.restoreBackup(this, source, password);
                runOnUiThread(() -> {
                    setWorking(false, "");
                    renderStatus();
                    String message = result.summary();
                    if (!result.backupAppVersion.isEmpty()) {
                        message += "\n백업 앱 버전 " + result.backupAppVersion;
                    }
                    if (result.missingImageCount > 0) {
                        message += "\n이미지 누락 " + result.missingImageCount + "개";
                    }
                    new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                            .setTitle("복원 완료")
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton("앱 다시 시작", (dialog, which) -> restartApp())
                            .show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setWorking(false, "");
                    renderStatus();
                    showError("복원 실패", error);
                });
            } finally {
                Arrays.fill(password, '\0');
            }
        }, "calltag-backup-restore").start();
    }

    private void restartApp() {
        Intent intent = new Intent(this, AuthGateActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void renderStatus() {
        if (statusView != null) statusView.setText(CallTagBackupManager.lastSummary(this));
    }

    private void setWorking(boolean value, String label) {
        working = value;
        createButton.setEnabled(!value);
        restoreButton.setEnabled(!value);
        createButton.setAlpha(value ? 0.5f : 1f);
        restoreButton.setAlpha(value ? 0.5f : 1f);
        if (legacyRecoveryButton != null) legacyRecoveryButton.setEnabled(!value);
        if (value) statusView.setText(label);
    }

    private void showError(String title, Exception error) {
        String message = error == null ? "알 수 없는 오류" : error.getMessage();
        if (message == null || message.trim().isEmpty()) message = "알 수 없는 오류";
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }

    private LinearLayout dialogForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);
        return form;
    }

    private EditText passwordField(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(15f);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_secondary));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setMinHeight(dp(52));
        return input;
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(12f);
        text.setIncludeFontPadding(false);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private LinearLayout.LayoutParams fixedHeight(int height, int marginTop) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(marginTop);
        return params;
    }

    private String defaultBackupName() {
        String time = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.KOREA).format(new Date());
        return "calltag-owner-v2-" + time + ".ctbackup";
    }

    private void clearPendingBackupPassword() {
        if (pendingBackupPassword != null) Arrays.fill(pendingBackupPassword, '\0');
        pendingBackupPassword = null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        clearPendingBackupPassword();
        super.onDestroy();
    }
}
