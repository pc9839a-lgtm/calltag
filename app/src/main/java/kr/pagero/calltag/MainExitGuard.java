package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;

import java.util.WeakHashMap;

/** 메인 탭에서는 뒤로가기로 즉시 종료되지 않게 보호한다. */
public final class MainExitGuard {
    private static final WeakHashMap<Activity, Object> CALLBACKS = new WeakHashMap<>();
    private static final WeakHashMap<Activity, View.OnKeyListener> KEY_LISTENERS = new WeakHashMap<>();
    private static final WeakHashMap<Activity, AlertDialog> DIALOGS = new WeakHashMap<>();

    private MainExitGuard() {}

    public static void install(Activity activity) {
        if (!(activity instanceof MainActivity) || activity.isFinishing()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (CALLBACKS.containsKey(activity)) return;
            Object callback = Api33.register(activity, () -> handleBack(activity));
            CALLBACKS.put(activity, callback);
            return;
        }
        if (KEY_LISTENERS.containsKey(activity)) return;
        View decor = activity.getWindow().getDecorView();
        View.OnKeyListener listener = (v, keyCode, event) -> {
            if (keyCode != KeyEvent.KEYCODE_BACK || event.getAction() != KeyEvent.ACTION_UP) {
                return false;
            }
            handleBack(activity);
            return true;
        };
        decor.setFocusableInTouchMode(true);
        decor.setOnKeyListener(listener);
        KEY_LISTENERS.put(activity, listener);
    }

    public static void uninstall(Activity activity) {
        if (activity == null) return;
        Object callback = CALLBACKS.remove(activity);
        if (callback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Api33.unregister(activity, callback);
        }
        View.OnKeyListener listener = KEY_LISTENERS.remove(activity);
        if (listener != null) {
            activity.getWindow().getDecorView().setOnKeyListener(null);
        }
        AlertDialog dialog = DIALOGS.remove(activity);
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    private static void handleBack(Activity activity) {
        if (activity.isFinishing()) return;
        View home = activity.findViewById(R.id.sectionToday);
        View homeNav = activity.findViewById(R.id.navToday);
        if (home != null && home.getVisibility() != View.VISIBLE && homeNav != null) {
            homeNav.performClick();
            return;
        }
        AlertDialog existing = DIALOGS.get(activity);
        if (existing != null && existing.isShowing()) return;

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("콜태그 종료")
                .setMessage("앱을 종료할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("종료", (d, which) -> {
                    uninstall(activity);
                    activity.finishAffinity();
                })
                .create();
        dialog.setOnDismissListener(d -> DIALOGS.remove(activity));
        DIALOGS.put(activity, dialog);
        dialog.show();
    }

    private static final class Api33 {
        private Api33() {}

        static Object register(Activity activity, Runnable action) {
            android.window.OnBackInvokedCallback callback = action::run;
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return callback;
        }

        static void unregister(Activity activity, Object rawCallback) {
            activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                    (android.window.OnBackInvokedCallback) rawCallback);
        }
    }
}
