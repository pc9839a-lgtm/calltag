package kr.pagero.calltag;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/** Periodic sync independent from optional secure cloud-backup settings. */
public final class ExternalLeadSyncWorkScheduler {
    private static final String PERIODIC_NAME = "calltag-external-leads-periodic";
    private static final String IMMEDIATE_NAME = "calltag-external-leads-immediate";
    private static final String TAG = "calltag-external-leads";
    private static final long PERIOD_MINUTES = 15L;

    private ExternalLeadSyncWorkScheduler() {}

    public static void reconcile(Context context) {
        Context app = context.getApplicationContext();
        WorkManager manager = WorkManager.getInstance(app);
        if (!AuthSessionStore.hasSession(app)) {
            manager.cancelAllWorkByTag(TAG);
            return;
        }
        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
                ExternalLeadSyncWorker.class,
                PERIOD_MINUTES,
                TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .addTag(TAG)
                .build();
        manager.enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodic);
    }

    public static void enqueueImmediate(Context context) {
        Context app = context.getApplicationContext();
        if (!AuthSessionStore.hasSession(app)) return;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExternalLeadSyncWorker.class)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .addTag(TAG)
                .build();
        WorkManager.getInstance(app).enqueueUniqueWork(
                IMMEDIATE_NAME,
                ExistingWorkPolicy.REPLACE,
                request);
    }

    private static Constraints networkConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();
    }
}
