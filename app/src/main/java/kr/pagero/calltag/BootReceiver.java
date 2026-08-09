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
                startMonitorIfAllowed(app);
            } finally {
                pendingResult.finish();
            }
        }, "calltag-boot-recovery").start();
    }

    private void startMonitorIfAllowed(Context context) {
        if (!SettingsStore.isMonitorEnabled(context)) return;
        if (!AuthSessionStore.hasSession(context) || !hasRequiredPermissions(context)) {
            SettingsStore.setMonitorEnabled(context, false);
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
        } catch (RuntimeException ignored) {
            SettingsStore.setMonitorEnabled(context, false);
        }
    }

    private boolean hasRequiredPermissions(Context context) {
        boolean granted = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted = granted && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return granted;
    }
}
