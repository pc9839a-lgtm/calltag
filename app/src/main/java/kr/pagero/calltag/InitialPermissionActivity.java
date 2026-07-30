package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class InitialPermissionActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 8401;
    private boolean requested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        getWindow().getDecorView().postDelayed(this::startPermissionFlow, 250L);
    }

    private LinearLayout buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(32), dp(32), dp(32), dp(32));
        root.setBackgroundColor(getColor(R.color.background));
        TextView title = new TextView(this);
        title.setText("콜태그 사용 준비 중");
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView detail = new TextView(this);
        detail.setText("필요한 Android 권한창이 자동으로 열립니다.");
        detail.setTextColor(getColor(R.color.text_secondary));
        detail.setTextSize(14f);
        detail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        root.addView(detail, params);
        return root;
    }

    private void startPermissionFlow() {
        if (requested) return;
        requested = true;
        List<String> missing = new ArrayList<>();
        addIfMissing(missing, Manifest.permission.READ_CONTACTS);
        addIfMissing(missing, Manifest.permission.WRITE_CONTACTS);
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
        if (missing.isEmpty()) {
            complete();
        } else {
            requestPermissions(missing.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    private void addIfMissing(List<String> target, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) target.add(permission);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) complete();
    }

    private void complete() {
        SetupRequirements.markInitialFlowCompleted(this);
        if (SetupRequirements.hasContacts(this) && SetupRequirements.hasContactWrite(this)
                && FeatureEntitlementStore.hasPhoneAccess(this)) {
            ContactNameSyncManager.enable(this);
        }
        if (SetupRequirements.hasPhoneState(this) && SetupRequirements.hasCallLog(this)) {
            SetupRequirements.startCallMonitoring(this);
        }
        MessageAutomationStore.ensureDefaults(this);
        MessageScheduler.rescheduleAll(this);
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
