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
import android.widget.Button;
import android.widget.Toast;

/** 수신 전화 고객정보 표시 역할의 상태/설정 버튼. */
public final class CallerIdSetupButton extends Button {
    private static final int REQUEST_SCREENING_ROLE = 7302;

    public CallerIdSetupButton(Context context) { super(context); init(); }
    public CallerIdSetupButton(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public CallerIdSetupButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        setAllCaps(false);
        setOnClickListener(v -> requestScreeningRoleDirectly());
        refresh();
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); refresh(); }
    @Override public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) refresh();
    }

    private void requestScreeningRoleDirectly() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(getContext(), "이 기기에서는 수신 고객정보 표시를 지원하지 않습니다.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        RoleManager roleManager = (RoleManager) getContext().getSystemService(Context.ROLE_SERVICE);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            openDefaultAppsSettings();
            return;
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(getContext(), "수신 전화 고객정보 표시를 사용 중입니다.",
                    Toast.LENGTH_SHORT).show();
            refresh();
            return;
        }

        Intent request = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
        Activity activity = findActivity(getContext());
        try {
            if (activity != null) activity.startActivityForResult(request, REQUEST_SCREENING_ROLE);
            else {
                request.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(request);
            }
        } catch (ActivityNotFoundException | SecurityException error) {
            openDefaultAppsSettings();
        }
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
            Toast.makeText(getContext(), "발신자 ID 및 스팸 앱을 콜태그로 선택해주세요.",
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
            setText("지원 안 됨");
            setBackgroundResource(R.drawable.bg_secondary_button);
            setTextColor(getContext().getColor(R.color.text_muted));
            return;
        }
        RoleManager roleManager = (RoleManager) getContext().getSystemService(Context.ROLE_SERVICE);
        boolean available = roleManager != null
                && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING);
        if (!available) {
            setEnabled(true);
            setText("지원 안 됨");
            setBackgroundResource(R.drawable.bg_secondary_button);
            setTextColor(getContext().getColor(R.color.text_muted));
            return;
        }
        boolean enabled = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        setEnabled(true);
        setText(enabled ? "사용 중" : "권한 필요");
        setBackgroundResource(enabled ? R.drawable.bg_secondary_button : R.drawable.bg_primary_button);
        setTextColor(getContext().getColor(enabled ? R.color.text_primary : android.R.color.white));
    }
}
