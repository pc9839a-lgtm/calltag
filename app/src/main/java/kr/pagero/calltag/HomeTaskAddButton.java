package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.Toast;

/** Opens task registration in an isolated activity to keep MainActivity stable. */
public final class HomeTaskAddButton extends Button {
    public HomeTaskAddButton(Context context) {
        super(context);
        initialize();
    }

    public HomeTaskAddButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public HomeTaskAddButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setOnClickListener(v -> openEditor());
    }

    private void openEditor() {
        Activity activity = activity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        setEnabled(false);
        try {
            activity.startActivity(new Intent(activity, HomeTaskEditorActivity.class));
        } catch (RuntimeException error) {
            Toast.makeText(activity, "할 일 등록 화면을 열지 못했습니다.", Toast.LENGTH_LONG).show();
        } finally {
            setEnabled(true);
        }
    }

    private Activity activity() {
        Context current = getContext();
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) return (Activity) current;
            Context base = ((ContextWrapper) current).getBaseContext();
            if (base == current) break;
            current = base;
        }
        return current instanceof Activity ? (Activity) current : null;
    }
}
