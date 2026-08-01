package kr.pagero.calltag;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public final class CallerIdSetupButton extends Button {
    public CallerIdSetupButton(Context context) {
        super(context);
        init();
    }

    public CallerIdSetupButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CallerIdSetupButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setAllCaps(false);
        setOnClickListener(v -> requestScreeningRoleDirectly());
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
        if (hasWindowFocus) refresh();
    }

    private void requestScreeningRoleDirectly() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(getContext(),
                    "Android 10 이상에서 사용할 수 있습니다.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        RoleManager roleManager =
                (RoleManager) getContext().getSystemService(Context.ROLE_SERVICE);
        if (roleManager == null
                || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(getContext(),
                    "이 휴대전화에서는 수신 고객정보 표시를 사용할 수 없습니다.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(getContext(),
                    "수신 고객정보 표시를 사용 중입니다.",
                    Toast.LENGTH_SHORT).show();
            refresh();
            return;
        }

        Intent request = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
        getContext().startActivity(request);
    }

    private void refresh() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);
        RoleManager roleManager =
                (RoleManager) getContext().getSystemService(Context.ROLE_SERVICE);
        boolean enabled = roleManager != null
                && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        setText(enabled ? "✓ 수신 고객정보 표시 사용 중" : "수신 고객정보 표시 켜기");
        setBackgroundResource(enabled
                ? R.drawable.bg_secondary_button : R.drawable.bg_primary_button);
        setTextColor(getContext().getColor(R.color.text_primary));
    }
}
