package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
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

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(42));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> {
            if (!working) finish();
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("백업 및 복원", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        LinearLayout notice = card();
        notice.addView(title("콜태그 전용 암호화 백업", 17f), matchWrap());
        notice.addView(body("고객·통화·일정·문자·그룹·캠페인·템플릿 이미지를 하나의 .ctbackup 파일로 보관합니다."), topMargin(8));
        notice.addView(body("로그인 세션과 결제 권한은 백업하지 않습니다. 새 기기에서는 본인 계정으로 다시 로그인해야 합니다."), topMargin(7));
        notice.addView(body("이 기능은 CSV·엑셀·고객 목록 내보내기가 아닙니다."), topMargin(7));
        root.addView(notice, topMargin(18));

        createButton = button("암호화 백업 만들기", true);
        createButton.setOnClickListener(v -> showBackupPasswordDialog());
        root.addView(createButton, fixedHeight(54, 18));

        restoreButton = button("백업 파일 복원", false);
        restoreButton.setOnClickListener(v -> chooseRestoreFile());
        root.addView(restoreButton, fixedHeight(54, 10));

        LinearLayout warning = card();
        TextView warningTitle = title("복원 전 확인", 16f);
        warningTitle.setTextColor(getColor(R.color.danger));
        warning.addView(warningTitle, matchWrap());
        warning.addView(body("복원은 현재 데이터와 백업 데이터를 합치지 않습니다. 현재 고객·문자·일정·캠페인 데이터를 백업 시점 데이터로 교체합니다."), topMargin(8));
        warning.addView(body("복원 직전 현재 데이터는 앱 내부 롤백 스냅샷으로 보존되며, 실패하면 자동으로 되돌립니다."), topMargin(7));
        root.addView(warning, topMargin(18));

        root.addView(title("최근 작업", 17f), topMargin(24));
        statusView = body("기록을 확인하는 중입니다.");
        statusView.setTextIsSelectable(true);
        statusView.setBackgroundResource(R.drawable.bg_card);
        statusView.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.addView(statusView, topMargin(10));
        return scroll;
    }

    private void showBackupPasswordDialog() {
        if (working) return;
        LinearLayout form = dialogForm();
        EditText password = passwordField("백업 암호 8자 이상");
        EditText confirm = passwordField("암호 다시 입력");
        form.addView(password, matchWrap());
        form.addView(confirm, topMargin(8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("백업 암호 설정")
                .setMessage("암호는 콜태그에 저장되지 않습니다. 암호를 잊으면 복원할 수 없습니다.")
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("백업 암호 입력")
                .setMessage("선택한 .ctbackup 파일의 암호를 입력해주세요.")
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
        new AlertDialog.Builder(this)
                .setTitle("현재 데이터를 교체할까요?")
                .setMessage("현재 고객·상담·일정·문자·그룹·캠페인 데이터가 백업 시점으로 교체됩니다. 복원 직전 현재 데이터는 자동 롤백용으로 먼저 보존합니다.")
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
                    new AlertDialog.Builder(this)
                            .setTitle("백업 완료")
                            .setMessage(result.summary()
                                    + "\n\n선택한 위치에 콜태그 전용 암호화 파일을 저장했습니다.")
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
                        message += "\n\n참조된 이미지 " + result.missingImageCount
                                + "개가 백업에 없어 해당 템플릿은 이미지 없이 표시됩니다.";
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("복원 완료")
                            .setMessage(message + "\n\n새 데이터로 화면을 다시 시작합니다.")
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
        if (value) statusView.setText(label);
    }

    private void showError(String title, Exception error) {
        String message = error == null ? "알 수 없는 오류" : error.getMessage();
        if (message == null || message.trim().isEmpty()) message = "알 수 없는 오류";
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }

    private LinearLayout dialogForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(24);
        form.setPadding(horizontal, dp(8), horizontal, 0);
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

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
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
        text.setTextSize(13f);
        text.setLineSpacing(dp(3), 1f);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? R.color.background : R.color.text_primary));
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
        return "calltag-backup-" + time + ".ctbackup";
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
