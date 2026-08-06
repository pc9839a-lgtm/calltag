package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.WeakHashMap;

/** 메인 화면에서 뒤로가기로 앱이 바로 닫히지 않도록 보호한다. */
public final class MainExitGuard {
    private static final WeakHashMap<Activity, Object> CALLBACKS = new WeakHashMap<>();
    private static final WeakHashMap<Activity, Window.Callback> ORIGINAL_CALLBACKS = new WeakHashMap<>();
    private static final WeakHashMap<Activity, Window.Callback> WRAPPED_CALLBACKS = new WeakHashMap<>();
    private static final WeakHashMap<Activity, AlertDialog> DIALOGS = new WeakHashMap<>();

    private MainExitGuard() {}

    public static void install(Activity activity) {
        if (!(activity instanceof MainActivity) || activity.isFinishing()) return;
        HomeTodayTaskFilter.install(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (CALLBACKS.containsKey(activity)) return;
            Object callback = Api33.register(activity, () -> handleBack(activity));
            CALLBACKS.put(activity, callback);
            return;
        }
        installLegacyWindowCallback(activity);
    }

    private static void installLegacyWindowCallback(Activity activity) {
        if (WRAPPED_CALLBACKS.containsKey(activity)) return;
        Window window = activity.getWindow();
        if (window == null) return;
        Window.Callback original = window.getCallback();
        if (original == null) return;

        Window.Callback wrapped = (Window.Callback) Proxy.newProxyInstance(
                Window.Callback.class.getClassLoader(),
                new Class<?>[]{Window.Callback.class},
                (proxy, method, args) -> {
                    if ("dispatchKeyEvent".equals(method.getName())
                            && args != null && args.length == 1
                            && args[0] instanceof KeyEvent) {
                        KeyEvent event = (KeyEvent) args[0];
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                                && event.getAction() == KeyEvent.ACTION_UP
                                && event.getRepeatCount() == 0) {
                            handleBack(activity);
                            return true;
                        }
                    }
                    try {
                        return method.invoke(original, args);
                    } catch (InvocationTargetException error) {
                        throw error.getCause();
                    }
                });
        ORIGINAL_CALLBACKS.put(activity, original);
        WRAPPED_CALLBACKS.put(activity, wrapped);
        window.setCallback(wrapped);
    }

    public static void uninstall(Activity activity) {
        if (activity == null) return;
        HomeTodayTaskFilter.uninstall(activity);
        Object callback = CALLBACKS.remove(activity);
        if (callback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Api33.unregister(activity, callback);
        }

        Window.Callback original = ORIGINAL_CALLBACKS.remove(activity);
        Window.Callback wrapped = WRAPPED_CALLBACKS.remove(activity);
        Window window = activity.getWindow();
        if (window != null && original != null && window.getCallback() == wrapped) {
            window.setCallback(original);
        }

        AlertDialog dialog = DIALOGS.remove(activity);
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    public static void handleBack(Activity activity) {
        if (!(activity instanceof MainActivity) || activity.isFinishing()) return;

        View home = activity.findViewById(R.id.sectionToday);
        View homeNav = activity.findViewById(R.id.navToday);
        if (home != null && home.getVisibility() != View.VISIBLE && homeNav != null) {
            homeNav.performClick();
            return;
        }

        AlertDialog existing = DIALOGS.get(activity);
        if (existing != null && existing.isShowing()) return;

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("앱을 닫을까요?")
                .setMessage("실수로 종료되지 않도록 확인합니다. 통화 감지와 예약 기능은 계속 유지됩니다.")
                .setNegativeButton("계속 사용", null)
                .setPositiveButton("앱 닫기", (d, which) -> {
                    uninstall(activity);
                    activity.finishAffinity();
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
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
