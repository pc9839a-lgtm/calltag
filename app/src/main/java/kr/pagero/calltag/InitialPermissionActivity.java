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
        if (SetupRequirements.hasRequiredRuntimePermissions(this)) {
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
        title.setText("전화 화면 메모 설정");
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(23f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, wrap());

        detail = new TextView(this);
        detail.setText("전화가 올 때 번호를 콜태그 고객과 대조해 최근 메모를 표시합니다. 휴대전화 연락처를 만들거나 이름을 수정하지 않습니다.");
        detail.setTextColor(getColor(R.color.text_secondary));
        detail.setTextSize(14f);
        detail.setGravity(Gravity.CENTER);
        detail.setLineSpacing(0f, 1.25f);
        root.addView(detail, top(12));

        requestButton = action("전화 화면 표시 권한 허용", true);
        requestButton.setOnClickListener(v -> startPermissionFlow());
        root.addView(requestButton, fixedTop(52, 24));

        settingsButton = action("앱 설정에서 권한 열기", false);
        settingsButton.setVisibility(View.GONE);
        settingsButton.setOnClickListener(v -> openAppSettings());
        root.addView(settingsButton, fixedTop(50, 9));

        TextView note = new TextView(this);
        note.setText("연락처 읽기, 전화 상태, 전화번호, 통화기록, 알림 권한이 필요합니다. 연락처 수정 권한은 요청하지 않습니다.");
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
        return view;
    }

    private void startPermissionFlow() {
        if (requestInFlight || completing) return;
        List<String> missing = missingPermissions();
        if (missing.isEmpty()) {
            complete();
            return;
        }

        requestInFlight = true;
        requestButton.setEnabled(false);
        requestButton.setAlpha(0.6f);
        requestButton.setText("권한 요청 중…");
        settingsButton.setVisibility(View.GONE);
        detail.setText("Android 권한창에서 모두 허용해주세요.");
        requestPermissions(missing.toArray(new String[0]), REQUEST_PERMISSIONS);
    }

    private List<String> missingPermissions() {
        List<String> missing = new ArrayList<>();
        addIfMissing(missing, Manifest.permission.READ_CONTACTS);
        addIfMissing(missing, Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addIfMissing(missing, Manifest.permission.READ_PHONE_NUMBERS);
        }
        addIfMissing(missing, Manifest.permission.READ_CALL_LOG);
        if (FeatureEntitlementStore.hasMessageAccess(this)) {
            addIfMissing(missing, Manifest.permission.SEND_SMS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addIfMissing(missing, Manifest.permission.POST_NOTIFICATIONS);
        }
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
        if (SetupRequirements.hasRequiredRuntimePermissions(this)) {
            complete();
        } else {
            renderMissingPermissions();
        }
    }

    private void renderMissingPermissions() {
        requestButton.setEnabled(true);
        requestButton.setAlpha(1f);
        requestButton.setText("권한 다시 요청");
        settingsButton.setVisibility(View.VISIBLE);
        detail.setText("아직 허용되지 않은 권한이 있어 전화 화면 메모를 시작할 수 없습니다. 다시 요청해도 창이 뜨지 않으면 앱 설정에서 직접 허용해주세요.\n\n미허용: "
                + missingPermissionLabels());
    }

    private String missingPermissionLabels() {
        List<String> labels = new ArrayList<>();
        if (!SetupRequirements.hasContacts(this)) labels.add("연락처 읽기");
        if (!SetupRequirements.hasPhoneState(this)) labels.add("전화 상태");
        if (!SetupRequirements.hasPhoneNumbers(this)) labels.add("전화번호");
        if (!SetupRequirements.hasCallLog(this)) labels.add("통화기록");
        if (FeatureEntitlementStore.hasMessageAccess(this) && !SetupRequirements.hasSms(this)) {
            labels.add("SMS");
        }
        if (!SetupRequirements.hasNotifications(this)) labels.add("알림");
        return labels.isEmpty() ? "확인 필요" : String.join(", ", labels);
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
        if (completing || !SetupRequirements.hasRequiredRuntimePermissions(this)) return;
        completing = true;
        requestButton.setEnabled(false);
        requestButton.setAlpha(0.6f);
        requestButton.setText("전화 화면 메모 준비 중…");
        detail.setText("연락처 동기화를 끄고 수신 번호 조회 방식으로 전환하고 있습니다.");

        SettingsStore.setContactNameSyncEnabled(this, false);
        ContactNameSyncManager.disableAndRestore(this);
        SetupRequirements.startCallMonitoring(this);
        MessageAutomationStore.ensureDefaults(this);
        MessageScheduler.rescheduleAll(this);
        startActivity(new Intent(this, CallerIdSetupActivity.class)
                .putExtra(CallerIdSetupActivity.EXTRA_REQUIRED_SETUP, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
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
