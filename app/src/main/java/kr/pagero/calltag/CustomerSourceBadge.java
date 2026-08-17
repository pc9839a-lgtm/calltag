package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

public final class CustomerSourceBadge {
    private CustomerSourceBadge() {}

    public static TextView create(Context context, String label) {
        String safeLabel = label == null ? "" : label.trim();
        TextView badge = new TextView(context);
        badge.setText(safeLabel);
        badge.setTextSize(11f);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setIncludeFontPadding(false);
        badge.setPadding(dp(context, 9), dp(context, 5), dp(context, 9), dp(context, 5));
        badge.setTextColor(context.getColor(R.color.primary_muted));

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(context.getColor(R.color.primary_soft));
        shape.setCornerRadius(dp(context, 14));
        badge.setBackground(shape);
        badge.setTag("customer_source_badge");
        badge.setVisibility(safeLabel.isEmpty() ? View.GONE : View.VISIBLE);
        return badge;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
