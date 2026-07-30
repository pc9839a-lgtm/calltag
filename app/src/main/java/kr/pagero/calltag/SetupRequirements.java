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
    private static final String KEY_OVERLAY_TEST_PASSED = "overlay_test_passed";

    private SetupRequirements() {}

    public static boolean hasContacts(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPhoneState(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasCallLog(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
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

    public static boolean baseReady(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && hasContacts(context)
                && hasPhoneState(context)
                && hasCallLog(context)
                && hasNotifications(context)
                && hasScreeningRole(context)
                && hasOverlay(context);
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
        if (!baseReady(context)) clearOverlayTest(context);
    }

    public static boolean isReady(Context context) {
        return baseReady(context) && overlayTestPassed(context);
    }

    public static Intent requiredSetupIntent(Context context) {
        return new Intent(context, CallerIdSetupActivity.class)
                .putExtra(CallerIdSetupActivity.EXTRA_REQUIRED_SETUP, true)
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
