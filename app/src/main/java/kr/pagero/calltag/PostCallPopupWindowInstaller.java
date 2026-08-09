package kr.pagero.calltag;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

/** Keeps PostCallActivity as one compact partial popup over the existing screen. */
public final class PostCallPopupWindowInstaller {
    private static final float PREFERRED_HEIGHT_RATIO = 0.42f;
    private static final float HARD_MAX_SCREEN_HEIGHT_RATIO = 0.55f;
    private static final int MAX_WIDTH_DP = 390;
    private static final int MAX_HEIGHT_DP = 350;
    private static final int MIN_HEIGHT_DP = 320;

    private PostCallPopupWindowInstaller() {}

    public static void install(Activity activity) {
        if (!(activity instanceof PostCallActivity) || activity.isFinishing()) return;
        Window window = activity.getWindow();
        if (window == null) return;

        activity.setFinishOnTouchOutside(false);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Post-call must never become a full-screen surface, even if an OEM changes window flags.
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        View decor = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            decor.setOnApplyWindowInsetsListener((view, insets) -> {
                applyBounds(activity);
                return insets;
            });
            decor.requestApplyInsets();
        }
        applyBounds(activity);
        decor.post(() -> applyBounds(activity));

        View root = activity.findViewById(R.id.postCallRoot);
        if (root != null) {
            root.setBackgroundResource(R.drawable.bg_dialog_panel);
            root.setElevation(dp(activity, 18));
            root.setClipToOutline(true);
        }
    }

    public static void uninstall(Activity activity) {
        if (!(activity instanceof PostCallActivity)) return;
        Window window = activity.getWindow();
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.getDecorView().setOnApplyWindowInsetsListener(null);
        }
    }

    private static void applyBounds(Activity activity) {
        if (activity.isFinishing()) return;
        Window window = activity.getWindow();
        if (window == null) return;

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int totalHorizontalMargin = dp(activity,
                metrics.widthPixels < dp(activity, 360) ? 20 : 28);
        int availableWidth = Math.max(dp(activity, 280),
                metrics.widthPixels - totalHorizontalMargin);

        int width = Math.min(availableWidth, dp(activity, MAX_WIDTH_DP));
        int preferredHeight = Math.round(metrics.heightPixels * PREFERRED_HEIGHT_RATIO);
        int maximumHeight = Math.min(preferredHeight, dp(activity, MAX_HEIGHT_DP));
        int height = Math.max(dp(activity, MIN_HEIGHT_DP), maximumHeight);

        // Absolute guardrail: a post-call window may never occupy most/all of the display.
        int ratioCap = Math.max(1,
                Math.round(metrics.heightPixels * HARD_MAX_SCREEN_HEIGHT_RATIO));
        int systemMarginCap = Math.max(1, metrics.heightPixels - dp(activity, 96));
        height = Math.min(height, Math.min(ratioCap, systemMarginCap));

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = width;
        params.height = height;
        params.gravity = Gravity.CENTER;
        params.dimAmount = 0f;
        params.windowAnimations = android.R.style.Animation_Dialog;
        window.setAttributes(params);
        window.setLayout(width, height);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
