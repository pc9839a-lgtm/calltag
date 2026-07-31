package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public final class CustomerAddLauncherView extends TextView {
    private long lastLaunchAt;

    public CustomerAddLauncherView(Context context) {
        super(context);
    }

    public CustomerAddLauncherView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomerAddLauncherView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnClickListener(View.OnClickListener ignored) {
        super.setOnClickListener(v -> {
            long now = SystemClock.elapsedRealtime();
            if (now - lastLaunchAt < 700L) return;
            lastLaunchAt = now;

            Context context = getContext();
            Intent intent = new Intent(context, CustomerAddActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        });
    }
}