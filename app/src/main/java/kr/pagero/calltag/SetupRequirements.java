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

    /** Legacy migration permission only. It is no longer required for normal CallTag use. */
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

    /** Base phone CRM readiness. Contact write is migration-only and never part of the gate. */
    public static boolean baseReady(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && hasRequiredRuntimePermissions(context);
    }

    public static boolean hasRequiredRuntimePermissions(Context context) {
        if (!hasContacts(context)
                || !hasPhoneState(context)
                || !hasPhoneNumbers(context)
                || !hasCallLog(context)
                || !hasNotifications(context)) {
            return false;
        }
        return !FeatureEntitlementStore.hasMessageAccess(context) || hasSms(context);
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

    public static boolean isReady(Context context) {
        return initialFlowCompleted(context)
                && hasRequiredRuntimePermissions(context);
    }

    public static Intent requiredSetupIntent(Context context) {
        return new Intent(context, InitialPermissionActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    public static void startCallMonitoring(Context context) {
        if (!hasPhoneState(context) || !hasCallLog(context)) {
            SettingsStore.setMonitorEnabled(context, false);
            return;
        }
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
