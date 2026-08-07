package kr.pagero.calltag;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import java.util.WeakHashMap;

/**
 * Android 15/16 edge-to-edge enforcement can place ordinary activity content below the
 * status/navigation bars. Apply one inset path and explicitly keep system-bar icons readable.
 */
public final class SystemBarInsetsInstaller {
    private static final WeakHashMap<Activity, BasePadding> INSTALLED = new WeakHashMap<>();

    private SystemBarInsetsInstaller() {}

    public static void install(Activity activity) {
        if (activity == null || activity.isFinishing() || excluded(activity)) return;
        keepSystemBarsReadable(activity);

        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        BasePadding base = INSTALLED.get(activity);
        if (base == null) {
            base = new BasePadding(
                    content.getPaddingLeft(),
                    content.getPaddingTop(),
                    content.getPaddingRight(),
                    content.getPaddingBottom());
            INSTALLED.put(activity, base);
        }
        BasePadding stable = base;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            content.setOnApplyWindowInsetsListener((view, insets) -> {
                int top;
                int bottom;
                int left;
                int right;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.graphics.Insets bars = insets.getInsets(
                            WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    top = bars.top;
                    bottom = bars.bottom;
                    left = bars.left;
                    right = bars.right;
                } else {
                    top = insets.getSystemWindowInsetTop();
                    bottom = insets.getSystemWindowInsetBottom();
                    left = insets.getSystemWindowInsetLeft();
                    right = insets.getSystemWindowInsetRight();
                }
                view.setPadding(
                        stable.left + left,
                        stable.top + top,
                        stable.right + right,
                        stable.bottom + bottom);
                return insets;
            });
            content.requestApplyInsets();
        }
    }

    private static void keepSystemBarsReadable(Activity activity) {
        Window window = activity.getWindow();
        if (window == null) return;
        window.setStatusBarColor(activity.getColor(R.color.background));
        window.setNavigationBarColor(activity.getColor(R.color.surface_soft));

        View decor = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                int lightMask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(0, lightMask);
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int visibility = decor.getSystemUiVisibility();
            visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(visibility);
        }
    }

    public static void uninstall(Activity activity) {
        if (activity == null) return;
        View content = activity.findViewById(android.R.id.content);
        BasePadding base = INSTALLED.remove(activity);
        if (content != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            content.setOnApplyWindowInsetsListener(null);
            if (base != null) {
                content.setPadding(base.left, base.top, base.right, base.bottom);
            }
        }
    }

    private static boolean excluded(Activity activity) {
        return activity instanceof PostCallActivity
                || activity instanceof CallerInfoActivity
                || activity instanceof MmsComposeActivity;
    }

    private static final class BasePadding {
        final int left;
        final int top;
        final int right;
        final int bottom;

        BasePadding(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
