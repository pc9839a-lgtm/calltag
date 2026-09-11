package kr.pagero.calltag;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/** Background safety-net for provider-pulled external form leads. */
public final class ExternalLeadAutoSyncScheduler {
    private static final String UNIQUE_WORK = "calltag-external-lead-auto-sync";
    private static final String TAG = "calltag-external-lead-auto-sync";
    private static final long PERIOD_MINUTES = 15L;

    private ExternalLeadAutoSyncScheduler() {}

    public static void reconcile(Context context) {
        Context app = context.getApplicationContext();
        WorkManager manager = WorkManager.getInstance(app);
        if (!AuthSessionStore.hasSession(app)) {
            manager.cancelUniqueWork(UNIQUE_WORK);
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ExternalLeadAutoSyncWorker.class,
                PERIOD_MINUTES,
                TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .addTag(TAG)
                .build();
        manager.enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_WORK);
    }
}
