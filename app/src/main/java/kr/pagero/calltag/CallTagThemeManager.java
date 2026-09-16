package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** 앱 전체의 블랙/화이트 테마를 사용자가 직접 고정 선택하도록 관리한다. */
public final class CallTagThemeManager {
    public static final String MODE_BLACK = "black";
    public static final String MODE_WHITE = "white";

    private static final String PREFS = "calltag_theme_ui";
    private static final String KEY_MODE = "theme_mode";

    private CallTagThemeManager() {}

    public static String current(Context context) {
        String value = prefs(context).getString(KEY_MODE, MODE_BLACK);
        return MODE_WHITE.equals(value) ? MODE_WHITE : MODE_BLACK;
    }

    public static String currentLabel(Context context) {
        return MODE_WHITE.equals(current(context)) ? "화이트" : "블랙";
    }

    public static boolean isBlack(Context context) {
        return MODE_BLACK.equals(current(context));
    }

    public static void applyApplicationMode(Context context) {
        if (context == null) return;
        boolean black = isBlack(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            UiModeManager manager = context.getSystemService(UiModeManager.class);
            if (manager != null) {
                manager.setApplicationNightMode(
                        black ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
            }
            return;
        }

        Resources resources = context.getResources();
        Configuration current = resources.getConfiguration();
        int target = black ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        if ((current.uiMode & Configuration.UI_MODE_NIGHT_MASK) == target) return;
        Configuration updated = new Configuration(current);
        updated.uiMode = (updated.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | target;
        resources.updateConfiguration(updated, resources.getDisplayMetrics());
    }

    public static void applyActivityAppearance(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;

        // Android 15+ ignores status/navigation bar color APIs for edge-to-edge apps.
        // Keep icon contrast through WindowInsetsControllerCompat instead of deprecated APIs.
        View decor = window.getDecorView();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decor);
        boolean lightIcons = !isBlack(activity);
        controller.setAppearanceLightStatusBars(lightIcons);
        controller.setAppearanceLightNavigationBars(lightIcons);
    }

    public static void showChooser(Context context) {
        if (context == null) return;
        String[] labels = {"블랙", "화이트"};
        int checked = MODE_WHITE.equals(current(context)) ? 1 : 0;
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.Theme_CallTag_Dialog)
                .setTitle("테마")
                .setSingleChoiceItems(labels, checked, (choiceDialog, which) -> {
                    String next = which == 1 ? MODE_WHITE : MODE_BLACK;
                    boolean changed = !next.equals(current(context));
                    prefs(context).edit().putString(KEY_MODE, next).apply();
                    choiceDialog.dismiss();
                    if (!changed) return;

                    Context app = context.getApplicationContext();
                    applyApplicationMode(app);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                            && context instanceof Activity) {
                        Activity activity = (Activity) context;
                        activity.getWindow().getDecorView().post(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                activity.recreate();
                            }
                        });
                    }
                })
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
