package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class InitialPermissionActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 8401;

    private TextView detail;
    private TextView requestButton;
    private TextView settingsButton;
    private boolean requestInFlight;
    private boolean completing;
    private boolean openedSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        getWindow().getDecorView().postDelayed(this::startPermissionFlow, 300L);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!openedSettings) return;
        openedSettings = false;
        if (SetupRequirements.hasCoreRuntimePermissions(this)) {
            complete();
        } else {
            renderMissingPermissions();
        }
    }

    private LinearLayout buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(32), dp(28), dp(32));
        root.setBackgroundColor(getColor(R.color.background));

        TextView title = new TextView(this);
        title.setText("전화 고객관리 권한");
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(23f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, wrap());

        detail = new TextView(this);
        detail.setText("고객 확인과 통화 이력 연결에 필요한 권한만 먼저 요청합니다. 문자·알림 권한은 해당 기능을 사용할 때 바로 요청합니다.");
        detail.setTextColor(getColor(R.color.text_secondary));
        detail.setTextSize(14f);
        detail.setGravity(Gravity.CENTER);
        detail.setLineSpacing(0f, 1.25f);
        root.addView(detail, top(12));

        requestButton = action("필수 권한 허용", true);
        requestButton.setOnClickListener(v -> startPermissionFlow());
        root.addView(requestButton, fixedTop(52, 24));

        settingsButton = action("권한 설정 열기", false);
        settingsButton.setVisibility(View.GONE);
        settingsButton.setOnClickListener(v -> openAppSettings());
        root.addView(settingsButton, fixedTop(50, 9));

        TextView note = new TextView(this);
        note.setText("연락처 내용은 수정하지 않습니다. 통화기록은 고객 이력 연결에만 사용합니다.");
        note.setTextColor(getColor(R.color.text_muted));
        note.setTextSize(12f);
        note.setGravity(Gravity.CENTER);
        note.setLineSpacing(0f, 1.2f);
        root.addView(note, top(18));
        return root;
    }

    private TextView action(String text, boolean primary) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(getColor(primary ? android.R.color.white : R.color.primary));
        view.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        view.setMinHeight(dp(50));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private void startPermissionFlow() {
        if (requestInFlight || completing) return;
        List<String> missing = missingCorePermissions();
        if (missing.isEmpty()) {
            complete();
            return;
        }

        requestInFlight = true;
        requestButton.setEnabled(false);
        requestButton.setAlpha(0.6f);
        requestButton.setText("권한 요청 중…");
        settingsButton.setVisibility(View.GONE);
        detail.setText("연락처 · 전화 · 전화번호 · 통화기록 권한을 확인하고 있습니다. 표시되는 항목을 허용해주세요.");
        requestPermissions(missing.toArray(new String[0]), REQUEST_PERMISSIONS);
    }

    private List<String> missingCorePermissions() {
        List<String> missing = new ArrayList<>();
        addIfMissing(missing, Manifest.permission.READ_CONTACTS);
        addIfMissing(missing, Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addIfMissing(missing, Manifest.permission.READ_PHONE_NUMBERS);
        }
        addIfMissing(missing, Manifest.permission.READ_CALL_LOG);
        return missing;
    }

    private void addIfMissing(List<String> target, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            target.add(permission);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) return;
        requestInFlight = false;
        if (SetupRequirements.hasCoreRuntimePermissions(this)) {
            complete();
        } else {
            renderMissingPermissions();
        }
    }

    private void renderMissingPermissions() {
        requestButton.setEnabled(true);
        requestButton.setAlpha(1f);
        requestButton.setText("남은 권한 허용");
        settingsButton.setVisibility(View.VISIBLE);
        detail.setText("필수 권한이 남아 있습니다. 위 버튼을 누르면 권한창을 다시 열고, 더 이상 권한창이 뜨지 않으면 아래에서 직접 허용할 수 있습니다.\n\n남은 항목: "
                + missingPermissionLabels());
    }

    private String missingPermissionLabels() {
        List<String> labels = new ArrayList<>();
        if (!SetupRequirements.hasContacts(this)) labels.add("연락처");
        if (!SetupRequirements.hasPhoneState(this)) labels.add("전화 상태");
        if (!SetupRequirements.hasPhoneNumbers(this)) labels.add("전화번호");
        if (!SetupRequirements.hasCallLog(this)) labels.add("통화기록");
        return labels.isEmpty() ? "필수 권한" : String.join(" · ", labels);
    }

    private void openAppSettings() {
        openedSettings = true;
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (RuntimeException ignored) {
            openedSettings = false;
        }
    }

    private void complete() {
        if (completing || !SetupRequirements.hasCoreRuntimePermissions(this)) return;
        completing = true;
        requestButton.setEnabled(false);
        requestButton.setAlpha(0.6f);
        requestButton.setText("설정 마무리 중…");
        detail.setText("통화 고객관리를 준비하고 있습니다.");

        new Thread(() -> {
            // Upgrade migration only: remove legacy CallTag-owned contact rows when an older
            // installation had already granted WRITE_CONTACTS. New installs never request it.
            ContactNameSyncManager.disableAndRestore(this);
            runOnUiThread(() -> {
                SetupRequirements.startCallMonitoring(this);
                MessageAutomationStore.ensureDefaults(this);
                MessageScheduler.rescheduleAll(this);
                startActivity(new Intent(this, CallerIdSetupActivity.class)
                        .putExtra(CallerIdSetupActivity.EXTRA_REQUIRED_SETUP, true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            });
        }, "calltag-phone-setup").start();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = wrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
