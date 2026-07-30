package kr.pagero.calltag;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

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
        setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), CallerIdSetupActivity.class)));
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

    private void refresh() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);
        RoleManager roleManager = (RoleManager) getContext().getSystemService(Context.ROLE_SERVICE);
        boolean enabled = roleManager != null
                && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        setText(enabled ? "✓ 수신 고객정보 표시 사용 중" : "수신 고객정보 표시 켜기");
        setBackgroundResource(enabled ? R.drawable.bg_secondary_button : R.drawable.bg_primary_button);
        setTextColor(getContext().getColor(R.color.text_primary));
    }
}
