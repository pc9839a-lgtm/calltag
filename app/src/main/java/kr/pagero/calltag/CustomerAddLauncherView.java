package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

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
        super.setOnClickListener(v -> {
            Context context = getContext();
            Intent intent = new Intent(context, CustomerAddActivity.class);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        });
    }
}
