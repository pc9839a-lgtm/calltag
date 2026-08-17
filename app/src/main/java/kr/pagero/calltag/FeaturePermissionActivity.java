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
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Small bridge that opens the Android permission prompt immediately instead of leaving the user on
 * a dead "permission missing" state. After grant it forwards to the originally requested screen.
 */
public final class FeaturePermissionActivity extends Activity {
    public static final String KIND_SMS = "sms";
    public static final String KIND_NOTIFICATION = "notification";

    private static final String EXTRA_KIND = "permission_kind";
    private static final String EXTRA_DESTINATION = "permission_destination";
    private static final int REQUEST_PERMISSION = 8711;

    private String kind;
    private String destination;
    private boolean requested;

    public static Intent intent(Activity context, String kind, Class<?> destination) {
        Intent intent = new Intent(context, FeaturePermissionActivity.class)
                .putExtra(EXTRA_KIND, kind);
        if (destination != null) intent.putExtra(EXTRA_DESTINATION, destination.getName());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        kind = safe(getIntent().getStringExtra(EXTRA_KIND));
        destination = safe(getIntent().getStringExtra(EXTRA_DESTINATION));
        setContentView(buildScreen());
        getWindow().getDecorView().postDelayed(this::requestNow, 180L);
    }

    private void requestNow() {
        if (hasPermission()) {
            continueToDestination();
            return;
        }
        String permission = permissionName();
        if (permission.isEmpty()) {
            continueToDestination();
            return;
        }
        requested = true;
        requestPermissions(new String[]{permission}, REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSION) return;
        if (hasPermission()) {
            continueToDestination();
        } else {
            renderDenied();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requested && hasPermission()) continueToDestination();
    }

    private LinearLayout buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(36), dp(28), dp(36));
        root.setBackgroundColor(getColor(R.color.background));

        TextView title = text(title(), 21f, true, R.color.text_primary);
        title.setGravity(Gravity.CENTER);
        root.addView(title, wrap());

        TextView detail = text(description(), 14f, false, R.color.text_secondary);
        detail.setTag("detail");
        detail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailParams = wrap();
        detailParams.topMargin = dp(12);
        root.addView(detail, detailParams);

        TextView allow = action("권한 허용", true);
        allow.setTag("allow");
        allow.setOnClickListener(v -> requestNow());
        LinearLayout.LayoutParams allowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        allowParams.topMargin = dp(24);
        root.addView(allow, allowParams);

        TextView settings = action("앱 권한 설정 열기", false);
        settings.setTag("settings");
        settings.setVisibility(android.view.View.GONE);
        settings.setOnClickListener(v -> openSettings());
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        settingsParams.topMargin = dp(9);
        root.addView(settings, settingsParams);
        return root;
    }

    private void renderDenied() {
        TextView detail = findByTag("detail");
        TextView allow = findByTag("allow");
        TextView settings = findByTag("settings");
        if (detail != null) detail.setText("권한이 아직 꺼져 있습니다. 권한 허용을 다시 누르거나 앱 권한 설정에서 직접 켜주세요.");
        if (allow != null) allow.setText("권한 다시 허용");
        if (settings != null) settings.setVisibility(android.view.View.VISIBLE);
    }

    @SuppressWarnings("unchecked")
    private <T extends android.view.View> T findByTag(String tag) {
        return (T) getWindow().getDecorView().findViewWithTag(tag);
    }

    private void continueToDestination() {
        if (isFinishing()) return;
        if (!destination.isEmpty()) {
            try {
                Class<?> target = Class.forName(destination);
                startActivity(new Intent(this, target));
            } catch (ClassNotFoundException ignored) {
                // Destination was removed in an app update; simply close the bridge.
            }
        }
        finish();
    }

    private void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (RuntimeException ignored) {
            // Some OEMs do not expose the per-app settings screen.
        }
    }

    private boolean hasPermission() {
        if (KIND_SMS.equals(kind)) return SetupRequirements.hasSms(this);
        if (KIND_NOTIFICATION.equals(kind)) return SetupRequirements.hasNotifications(this);
        return true;
    }

    private String permissionName() {
        if (KIND_SMS.equals(kind)) return Manifest.permission.SEND_SMS;
        if (KIND_NOTIFICATION.equals(kind) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.POST_NOTIFICATIONS;
        }
        return "";
    }

    private String title() {
        return KIND_SMS.equals(kind) ? "문자 권한" : "알림 권한";
    }

    private String description() {
        if (KIND_SMS.equals(kind)) {
            return "문자 기능을 사용하려면 문자 보내기 권한이 필요합니다. 지금 바로 권한창을 엽니다.";
        }
        return "통화 후 알림을 받으려면 알림 권한이 필요합니다. 지금 바로 권한창을 엽니다.";
    }

    private TextView action(String value, boolean primary) {
        TextView view = text(value, 15f, true,
                primary ? android.R.color.white : R.color.primary);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(color));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
