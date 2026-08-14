package kr.pagero.calltag;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.Switch;
import android.widget.Toast;

/** 수신 전화 고객정보 표시를 앱 설정과 Android 통화 스크리닝 역할로 분리한다. */
public final class CallerIdSetupButton extends Switch {
    private static final int REQUEST_SCREENING_ROLE = 7302;
    private boolean syncing;
    private boolean enableAfterRoleGrant;

    public CallerIdSetupButton(Context context) { super(context); init(); }
    public CallerIdSetupButton(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public CallerIdSetupButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        setBackground(null);
        setText("");
        setShowText(false);
        setPadding(0, 0, 0, 0);
        setContentDescription("수신 전화 고객정보 표시");
        setOnCheckedChangeListener((button, checked) -> {
            if (syncing) return;
            if (!checked) {
                enableAfterRoleGrant = false;
                SettingsStore.setCallerInfoDisplayEnabled(getContext(), false);
                SettingsStore.setCallerScreeningStatus(getContext(), "수신 전화 고객정보 표시를 껐습니다.");
                refresh();
                return;
            }
            enableCallerInfoDisplay();
        });
        refresh();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refresh();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) return;
        if (enableAfterRoleGrant && hasRole()) {
            SettingsStore.setCallerInfoDisplayEnabled(getContext(), true);
            SettingsStore.setCallerScreeningStatus(getContext(), "수신 전화 고객정보 표시를 켰습니다.");
            enableAfterRoleGrant = false;
            Toast.makeText(getContext(), "수신 고객정보 표시를 켰습니다.", Toast.LENGTH_SHORT).show();
        }
        refresh();
    }

    private void enableCallerInfoDisplay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            SettingsStore.setCallerInfoDisplayEnabled(getContext(), false);
            Toast.makeText(getContext(), "이 기기에서는 수신 고객정보 표시를 지원하지 않습니다.",
                    Toast.LENGTH_SHORT).show();
            refresh();
            return;
        }

        if (hasRole()) {
            SettingsStore.setCallerInfoDisplayEnabled(getContext(), true);
            SettingsStore.setCallerScreeningStatus(getContext(), "수신 전화 고객정보 표시를 켰습니다.");
            refresh();
            return;
        }

        SettingsStore.setCallerInfoDisplayEnabled(getContext(), false);
        enableAfterRoleGrant = true;
        setCheckedSilently(false);
        requestScreeningRoleDirectly();
    }

    private void requestScreeningRoleDirectly() {
        RoleManager roleManager = roleManager();
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            enableAfterRoleGrant = false;
            openDefaultAppsSettings();
            return;
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            SettingsStore.setCallerInfoDisplayEnabled(getContext(), true);
            enableAfterRoleGrant = false;
            refresh();
            return;
        }

        Intent request = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
        Activity activity = findActivity(getContext());
        try {
            if (activity != null) {
                activity.startActivityForResult(request, REQUEST_SCREENING_ROLE);
            } else {
                request.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(request);
            }
        } catch (ActivityNotFoundException | SecurityException error) {
            enableAfterRoleGrant = false;
            openDefaultAppsSettings();
        }
    }

    private RoleManager roleManager() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null;
        return (RoleManager) getContext().getSystemService(Context.ROLE_SERVICE);
    }

    private boolean hasRole() {
        RoleManager manager = roleManager();
        return manager != null
                && manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                && manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    private void openDefaultAppsSettings() {
        try {
            Intent settings = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            Activity activity = findActivity(getContext());
            if (activity != null) activity.startActivity(settings);
            else {
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(settings);
            }
            Toast.makeText(getContext(), "발신자 ID 및 스팸 앱에서 콜태그를 허용해주세요.",
                    Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            Toast.makeText(getContext(), "Android 기본 앱 설정을 열지 못했습니다.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) return (Activity) current;
            Context base = ((ContextWrapper) current).getBaseContext();
            if (base == current) break;
            current = base;
        }
        return current instanceof Activity ? (Activity) current : null;
    }

    private void refresh() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setEnabled(false);
            setCheckedSilently(false);
            return;
        }
        RoleManager manager = roleManager();
        boolean available = manager != null && manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING);
        setEnabled(available);
        boolean held = available && manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        boolean enabled = held && SettingsStore.isCallerInfoDisplayEnabled(getContext());
        SettingsStore.updateScreeningRoleState(getContext(), held);
        setCheckedSilently(enabled);
    }

    private void setCheckedSilently(boolean checked) {
        syncing = true;
        try {
            setChecked(checked);
        } finally {
            syncing = false;
        }
    }
}
