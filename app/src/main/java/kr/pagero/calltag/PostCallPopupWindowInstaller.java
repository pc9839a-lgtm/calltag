package kr.pagero.calltag;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/** Keeps PostCallActivity as a compact, persistent center popup. */
public final class PostCallPopupWindowInstaller {
    private static final float HEIGHT_RATIO = 0.66f;
    private static final int MAX_WIDTH_DP = 420;
    private static final int MAX_HEIGHT_DP = 560;
    private static final int MIN_HEIGHT_DP = 420;

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
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int availableWidth = Math.max(dp(activity, 280), metrics.widthPixels - dp(activity, 32));
        int availableHeight = Math.max(dp(activity, 360), metrics.heightPixels - dp(activity, 112));

        int width = Math.min(availableWidth, dp(activity, MAX_WIDTH_DP));
        int preferredHeight = Math.round(metrics.heightPixels * HEIGHT_RATIO);
        int height = Math.min(availableHeight,
                Math.min(preferredHeight, dp(activity, MAX_HEIGHT_DP)));
        height = Math.max(Math.min(availableHeight, dp(activity, MIN_HEIGHT_DP)), height);

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = width;
        params.height = height;
        params.gravity = Gravity.CENTER;
        params.dimAmount = 0.42f;
        params.windowAnimations = android.R.style.Animation_Dialog;
        window.setAttributes(params);
        window.setLayout(width, height);

        View root = activity.findViewById(R.id.postCallRoot);
        if (root != null) {
            root.setBackgroundResource(R.drawable.bg_dialog_panel);
            root.setElevation(dp(activity, 18));
            root.setClipToOutline(true);
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
