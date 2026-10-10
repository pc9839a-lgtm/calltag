package kr.pagero.calltag;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
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
    public static final String KEY_SKIP_GOOGLE_FORMS_POLL = "skip_google_forms_poll";
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
                // FCM and pending-ACK recovery must not wait for a slow Google Forms poll.
                .setInputData(new Data.Builder()
                        .putBoolean(KEY_SKIP_GOOGLE_FORMS_POLL, true)
                        .build())
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .addTag(TAG)
                .build();
        WorkManager.getInstance(app).enqueueUniqueWork(
                IMMEDIATE_NAME,
                ExistingWorkPolicy.KEEP,
                request);
    }

    private static Constraints networkConstraints() {
        return new Constraints.Builder()
                // Lead collection must not stop simply because battery is low.
                // Android's scheduler can still defer work for Doze / quota policies.
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }
}
