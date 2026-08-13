package kr.pagero.calltag;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

/** 홈의 통화 감지 상태를 '사용 중/사용 안 함/권한 필요'로 명확히 표시한다. */
public final class PhoneMonitorStateTextView extends TextView {
    public PhoneMonitorStateTextView(Context context) { super(context); }
    public PhoneMonitorStateTextView(Context context, AttributeSet attrs) { super(context, attrs); }
    public PhoneMonitorStateTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        renderState();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) renderState();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        // MainActivity의 구버전 안내문 호출도 항상 새 상태 표현으로 정규화한다.
        if (getContext() == null) super.setText(text, type);
        else renderState();
    }

    private void renderState() {
        boolean permissionReady = SetupRequirements.hasPhoneState(getContext())
                && SetupRequirements.hasCallLog(getContext())
                && SetupRequirements.hasNotifications(getContext());
        String state;
        if (!permissionReady) {
            state = "권한 필요 · 통화기록 권한을 허용해주세요.\n수신·발신·부재중 통화를 고객 이력과 할 일에 연결합니다.";
        } else if (SettingsStore.isMonitorEnabled(getContext())) {
            state = "사용 중\n수신·발신·부재중 통화를 고객 이력과 할 일에 연결합니다.";
        } else {
            state = "사용 안 함\n켜면 통화가 끝난 뒤 고객 이력과 할 일을 자동으로 연결합니다.";
        }
        super.setText(state, BufferType.NORMAL);
    }
}
