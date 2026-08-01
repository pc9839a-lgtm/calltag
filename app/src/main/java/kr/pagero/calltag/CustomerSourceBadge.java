package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;

public final class CustomerSourceBadge {
    private CustomerSourceBadge() {}

    public static TextView create(Context context, String label) {
        TextView badge = new TextView(context);
        badge.setText(CustomerSourceResolver.PAGERO);
        badge.setTextSize(11f);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setIncludeFontPadding(false);
        badge.setPadding(dp(context, 9), dp(context, 5), dp(context, 9), dp(context, 5));
        badge.setTextColor(Color.parseColor("#2458A6"));

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor("#EAF1FF"));
        shape.setCornerRadius(dp(context, 14));
        badge.setBackground(shape);
        badge.setTag("customer_source_badge");
        return badge;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
