package kr.pagero.calltag;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/** Makes PostCallActivity a large floating popup instead of a full app screen. */
public final class PostCallPopupWindowInstaller {
    private PostCallPopupWindowInstaller() {}

    public static void install(Activity activity) {
        if (!(activity instanceof PostCallActivity)) return;
        Window window = activity.getWindow();
        if (window == null) return;

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int horizontalMargin = dp(activity, 12);
        int verticalMargin = dp(activity, 28);
        int width = Math.max(dp(activity, 300), metrics.widthPixels - horizontalMargin * 2);
        int height = Math.max(dp(activity, 480), metrics.heightPixels - verticalMargin * 2);

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = width;
        params.height = height;
        params.gravity = Gravity.CENTER;
        params.dimAmount = 0.68f;
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
