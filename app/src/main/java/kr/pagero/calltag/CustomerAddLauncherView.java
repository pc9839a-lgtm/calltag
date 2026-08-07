package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public final class CustomerAddLauncherView extends TextView {
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
        super.setOnClickListener(v -> openCustomerAdd());
    }

    private void openCustomerAdd() {
        Context context = getContext();
        if (!UiLaunchGuard.tryAcquire("customer_add", 900L)) {
            CrashTelemetryStore.record(context, "customer_add", "duplicate_suppressed", "");
            return;
        }
        Intent intent = new Intent(context, CustomerAddActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            CrashTelemetryStore.record(context, "customer_add", "launch_accepted", "");
        } catch (RuntimeException error) {
            UiLaunchGuard.release("customer_add");
            CrashTelemetryStore.record(context, "customer_add", "launch_failed",
                    error.getClass().getSimpleName());
            Toast.makeText(context, "고객 추가 화면을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
