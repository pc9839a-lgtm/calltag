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

/** Keeps PostCallActivity as a large centered memo popup, never a full-screen page. */
public final class PostCallPopupWindowInstaller {
    private static final float HEIGHT_RATIO = 0.54f;
    private static final int MAX_WIDTH_DP = 420;
    private static final int MAX_HEIGHT_DP = 470;
    private static final int MIN_HEIGHT_DP = 360;

    private PostCallPopupWindowInstaller() {}

    public static void install(Activity activity) {
        if (!(activity instanceof PostCallActivity) || activity.isFinishing()) return;
        Window window = activity.getWindow();
        if (window == null) return;

        activity.setFinishOnTouchOutside(false);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        View decor = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            decor.setOnApplyWindowInsetsListener((view, insets) -> {
                int keyboardHeight = insets.isVisible(WindowInsets.Type.ime())
                        ? insets.getInsets(WindowInsets.Type.ime()).bottom : 0;
                applyBounds(activity, keyboardHeight);
                return insets;
            });
            decor.requestApplyInsets();
        }
        applyBounds(activity, 0);

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

    private static void applyBounds(Activity activity, int keyboardHeight) {
        if (activity.isFinishing()) return;
        Window window = activity.getWindow();
        if (window == null) return;

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int totalHorizontalMargin = dp(activity,
                metrics.widthPixels < dp(activity, 360) ? 22 : 36);
        int verticalReserved = dp(activity, 74);
        int availableWidth = Math.max(dp(activity, 280),
                metrics.widthPixels - totalHorizontalMargin);
        int availableHeight = Math.max(dp(activity, 300),
                metrics.heightPixels - verticalReserved - Math.max(0, keyboardHeight));

        int width = Math.min(availableWidth, dp(activity, MAX_WIDTH_DP));
        int preferredHeight = Math.round(metrics.heightPixels * HEIGHT_RATIO);
        int height = Math.min(availableHeight,
                Math.min(preferredHeight, dp(activity, MAX_HEIGHT_DP)));
        int minimumHeight = Math.min(availableHeight, dp(activity, MIN_HEIGHT_DP));
        height = Math.max(minimumHeight, height);

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = width;
        params.height = height;
        params.gravity = Gravity.CENTER;
        params.dimAmount = 0.46f;
        params.windowAnimations = android.R.style.Animation_Dialog;
        window.setAttributes(params);
        window.setLayout(width, height);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
