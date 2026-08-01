package kr.pagero.calltag;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

public final class SetupRequirements {
    private static final String PREFS = "calltag_required_setup";
    private static final String KEY_INITIAL_FLOW_COMPLETED = "initial_permission_flow_completed";
    private static final String KEY_OVERLAY_TEST_PASSED = "overlay_test_passed";

    private SetupRequirements() {}

    public static boolean hasContacts(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasContactWrite(Context context) {
        return context.checkSelfPermission(Manifest.permission.WRITE_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPhoneState(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPhoneNumbers(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasCallLog(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasSms(Context context) {
        return context.checkSelfPermission(Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasScreeningRole(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
        RoleManager manager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);
        return manager != null
                && manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                && manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    public static boolean hasOverlay(Context context) {
        return CallerOverlayManager.canShow(context);
    }

    public static boolean hasPostCallPopup(Context context) {
        return CallPopupNotificationManager.isPopupReady(
                context, CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
    }

    /** 상세 오버레이 설정 화면에서만 사용하는 선택 기능 준비 상태다. */
    public static boolean baseReady(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && hasContacts(context)
                && hasContactWrite(context)
                && hasPhoneState(context)
                && hasCallLog(context)
                && hasNotifications(context)
                && SettingsStore.isContactNameSyncEnabled(context)
                && hasPostCallPopup(context);
    }

    public static boolean initialFlowCompleted(Context context) {
        return preferences(context).getBoolean(KEY_INITIAL_FLOW_COMPLETED, false);
    }

    public static void markInitialFlowCompleted(Context context) {
        preferences(context).edit().putBoolean(KEY_INITIAL_FLOW_COMPLETED, true).apply();
    }

    public static void clearInitialFlow(Context context) {
        preferences(context).edit().putBoolean(KEY_INITIAL_FLOW_COMPLETED, false).apply();
    }

    public static boolean overlayTestPassed(Context context) {
        return preferences(context).getBoolean(KEY_OVERLAY_TEST_PASSED, false);
    }

    public static void markOverlayTestPassed(Context context) {
        preferences(context).edit().putBoolean(KEY_OVERLAY_TEST_PASSED, true).apply();
    }

    public static void clearOverlayTest(Context context) {
        preferences(context).edit().putBoolean(KEY_OVERLAY_TEST_PASSED, false).apply();
    }

    public static void invalidateTestWhenPrerequisitesMissing(Context context) {
        if (!hasOverlay(context) || !hasScreeningRole(context)) clearOverlayTest(context);
    }

    /**
     * 앱 진입을 별도 설정 페이지로 막지 않는다. 필요한 권한과 역할은 사용자가
     * 해당 기능을 누른 시점에 Android 시스템 창으로 직접 요청한다.
     */
    public static boolean isReady(Context context) {
        return true;
    }

    /** 이전 호출부 호환용이며 강제 설정 화면 대신 메인으로 이동한다. */
    public static Intent requiredSetupIntent(Context context) {
        return new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    public static void startCallMonitoring(Context context) {
        SettingsStore.setMonitorEnabled(context, true);
        Intent service = new Intent(context, CallMonitorService.class)
                .setAction(CallMonitorService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (RuntimeException ignored) {
            SettingsStore.setMonitorEnabled(context, false);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
