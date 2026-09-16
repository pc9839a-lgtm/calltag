package kr.pagero.calltag;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.WeakHashMap;

/**
 * Android 15/16 edge-to-edge enforcement can place ordinary activity content below the
 * status/navigation bars. Enable edge-to-edge consistently and apply system-bar/cutout insets
 * to the root content view so controls remain reachable on phones, tablets and foldables.
 */
public final class SystemBarInsetsInstaller {
    private static final WeakHashMap<Activity, BasePadding> INSTALLED = new WeakHashMap<>();

    private SystemBarInsetsInstaller() {}

    public static void install(Activity activity) {
        if (activity == null || activity.isFinishing() || excluded(activity)) return;
        Window window = activity.getWindow();
        if (window == null) return;

        // Backward-compatible edge-to-edge path. Android 15+ enforces this for targetSdk 35+;
        // calling it explicitly also gives older Android versions the same inset behavior.
        WindowCompat.enableEdgeToEdge(window);
        keepSystemBarsReadable(activity, window);

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

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    stable.left + bars.left,
                    stable.top + bars.top,
                    stable.right + bars.right,
                    stable.bottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private static void keepSystemBarsReadable(Activity activity, Window window) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                window, window.getDecorView());
        boolean lightIcons = !CallTagThemeManager.isBlack(activity);
        controller.setAppearanceLightStatusBars(lightIcons);
        controller.setAppearanceLightNavigationBars(lightIcons);
        controller.show(WindowInsetsCompat.Type.systemBars());
    }

    public static void uninstall(Activity activity) {
        if (activity == null) return;
        View content = activity.findViewById(android.R.id.content);
        BasePadding base = INSTALLED.remove(activity);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, null);
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
