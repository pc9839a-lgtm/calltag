package kr.pagero.calltag;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/** Persists the CallLog recovery safety net independently of the foreground monitor process. */
public final class CallMonitorRecoveryScheduler {
    private static final String PERIODIC_NAME = "calltag_call_monitor_recovery";
    private static final String IMMEDIATE_NAME = "calltag_call_monitor_recovery_now";

    private CallMonitorRecoveryScheduler() {}

    /**
     * Reconciles the durable recovery job from the user's monitor preference. This is safe to call
     * on every process start and does not start or restart CallMonitorService.
     */
    public static void reconcile(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        if (!SettingsStore.isMonitorEnabled(app)) {
            cancel(app);
            return;
        }
        ensureScheduled(app);
    }

    public static void ensureScheduled(Context context) {
        if (context == null) return;
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                CallMonitorRecoveryWorker.class, 15L, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(
                        PERIODIC_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        request);
    }

    public static void enqueueImmediate(Context context) {
        if (context == null) return;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(
                CallMonitorRecoveryWorker.class).build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(IMMEDIATE_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancel(Context context) {
        if (context == null) return;
        WorkManager manager = WorkManager.getInstance(context.getApplicationContext());
        manager.cancelUniqueWork(PERIODIC_NAME);
        manager.cancelUniqueWork(IMMEDIATE_NAME);
    }
}
