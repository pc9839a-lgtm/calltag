package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;

/** Presents caller-info control with the exact same native Switch appearance as call monitoring. */
public final class CallerInfoToggleView extends LinearLayout {
    private CallerIdSetupButton logicSwitch;
    private Switch visualSwitch;
    private boolean syncing;

    public CallerInfoToggleView(Context context) {
        super(context);
        init();
    }

    public CallerInfoToggleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CallerInfoToggleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);

        logicSwitch = new CallerIdSetupButton(getContext());
        logicSwitch.setVisibility(View.GONE);
        addView(logicSwitch, new LayoutParams(0, 0));

        visualSwitch = new Switch(getContext());
        visualSwitch.setText("");
        visualSwitch.setShowText(false);
        visualSwitch.setContentDescription("수신 전화 고객정보 표시");
        visualSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (syncing) return;
            if (checked != logicSwitch.isChecked()) logicSwitch.performClick();
            post(this::syncFromLogic);
        });
        addView(visualSwitch, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        syncFromLogic();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(this::syncFromLogic);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) return;
        logicSwitch.onWindowFocusChanged(true);
        postDelayed(this::syncFromLogic, 80L);
    }

    private void syncFromLogic() {
        syncing = true;
        try {
            visualSwitch.setEnabled(logicSwitch.isEnabled());
            visualSwitch.setChecked(logicSwitch.isChecked());
        } finally {
            syncing = false;
        }
    }
}
