package kr.pagero.calltag;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/** 홈 통화 감지 카드는 상태 문구 없이 기능 설명만 표시한다. */
public final class PhoneMonitorStateTextView extends TextView {
    private static final String DESCRIPTION =
            "수신·발신·부재중 통화를 고객 이력과 할 일에 연결합니다.";

    public PhoneMonitorStateTextView(Context context) { super(context); }
    public PhoneMonitorStateTextView(Context context, AttributeSet attrs) { super(context, attrs); }
    public PhoneMonitorStateTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        super.setText(DESCRIPTION, BufferType.NORMAL);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) super.setText(DESCRIPTION, BufferType.NORMAL);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (getContext() == null) super.setText(text, type);
        else super.setText(DESCRIPTION, BufferType.NORMAL);
    }
}
