package kr.pagero.calltag;

import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

/** Adds the external-lead entry to MainActivity's dynamically rendered More menu. */
public final class ExternalLeadMenuInstaller {
    private static final String ROW_TAG = "calltag-external-lead-menu-row";
    private static final Map<MainActivity, ViewTreeObserver.OnGlobalLayoutListener> LISTENERS = new WeakHashMap<>();

    private ExternalLeadMenuInstaller() {}

    public static void install(MainActivity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        LinearLayout menu = activity.findViewById(R.id.moreMenuList);
        if (menu == null) return;
        ensureRow(activity, menu);
        if (LISTENERS.containsKey(activity)) return;

        ViewTreeObserver.OnGlobalLayoutListener listener = () -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) ensureRow(activity, menu);
        };
        LISTENERS.put(activity, listener);
        menu.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    public static void uninstall(MainActivity activity) {
        if (activity == null) return;
        ViewTreeObserver.OnGlobalLayoutListener listener = LISTENERS.remove(activity);
        if (listener == null) return;
        LinearLayout menu = activity.findViewById(R.id.moreMenuList);
        if (menu != null && menu.getViewTreeObserver().isAlive()) {
            menu.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    private static void ensureRow(MainActivity activity, LinearLayout menu) {
        if (menu.findViewWithTag(ROW_TAG) != null) return;

        LinearLayout row = new LinearLayout(activity);
        row.setTag(ROW_TAG);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 18), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription("외부 문의 연동 설정 열기");
        row.setOnClickListener(v -> activity.startActivity(
                new Intent(activity, ExternalLeadIntegrationActivity.class)));

        LinearLayout textWrap = new LinearLayout(activity);
        textWrap.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setText("외부 문의 연동");
        title.setTextColor(activity.getColor(R.color.text_primary));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setIncludeFontPadding(false);

        TextView subtitle = new TextView(activity);
        subtitle.setText("Meta · Google Forms · Webhook · API 문의 수신");
        subtitle.setTextColor(activity.getColor(R.color.text_muted));
        subtitle.setTextSize(13f);
        subtitle.setIncludeFontPadding(false);

        textWrap.addView(title, matchWrap());
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.topMargin = dp(activity, 4);
        textWrap.addView(subtitle, subtitleParams);
        row.addView(textWrap, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(activity);
        arrow.setText("›");
        arrow.setTextColor(activity.getColor(R.color.text_muted));
        arrow.setTextSize(24f);
        arrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(arrow);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 72));
        rowParams.bottomMargin = dp(activity, 8);
        menu.addView(row, 0, rowParams);
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private static int dp(MainActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
