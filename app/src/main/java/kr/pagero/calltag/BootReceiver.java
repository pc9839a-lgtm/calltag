package kr.pagero.calltag;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        Context app = context.getApplicationContext();
        boolean packageReplaced = Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
        String recoveryTrigger = packageReplaced
                ? MessageRecoveryManager.TRIGGER_PACKAGE_REPLACED
                : MessageRecoveryManager.TRIGGER_BOOT;
        String integrityTrigger = packageReplaced
                ? DataIntegrityManager.TRIGGER_PACKAGE_REPLACED
                : DataIntegrityManager.TRIGGER_BOOT;

        PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                MessageAutomationStore.ensureDefaults(app);
                MessageRecoveryManager.recoverNow(app, recoveryTrigger);
                DataIntegrityManager.recoverNow(app, integrityTrigger);
                CallTagSyncWorkScheduler.reconcile(app);
                CallTagSyncWorkScheduler.enqueueImmediate(
                        app,
                        packageReplaced ? "package_replaced" : "boot");

                // Local CallLog recovery is independent of whether Android lets the foreground
                // monitor service restart right now. Reconcile the 15-minute worker first and run
                // one immediate pass after reboot/update to close any gap while the process was off.
                CallMonitorRecoveryScheduler.reconcile(app);
                if (SettingsStore.isMonitorEnabled(app)) {
                    CallMonitorRecoveryScheduler.enqueueImmediate(app);
                }

                startMonitorIfAllowed(app);
            } finally {
                pendingResult.finish();
            }
        }, "calltag-boot-recovery").start();
    }

    private void startMonitorIfAllowed(Context context) {
        if (!SettingsStore.isMonitorEnabled(context)) return;
        if (!AuthSessionStore.hasSession(context) || !hasRequiredPermissions(context)) {
            // Keep the user's monitor preference and the WorkManager safety net intact. The worker
            // itself no-ops safely until session/permissions are available again.
            CrashTelemetryStore.record(context, "call_watchdog", "service_start_deferred",
                    "missing_session_or_permission");
            return;
        }

        Intent service = new Intent(context, CallMonitorService.class)
                .setAction(CallMonitorService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (RuntimeException error) {
            // OEM/background restrictions may reject the foreground-service start. Do not turn off
            // monitoring here: the independent periodic CallLog worker remains the recovery path.
            CallMonitorRecoveryScheduler.ensureScheduled(context);
            CallMonitorRecoveryScheduler.enqueueImmediate(context);
            CrashTelemetryStore.record(context, "call_watchdog", "service_start_blocked",
                    error.getClass().getSimpleName());
        }
    }

    private boolean hasRequiredPermissions(Context context) {
        // Notification permission controls whether fallback notifications are visible. It must not
        // disable the call monitor itself after a reboot/package update. Live call resolution only
        // requires phone-state and call-log access; the post-call delivery layer handles its own
        // notification availability independently.
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }
}
