package kr.pagero.calltag;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager's 15-minute minimum is not a guaranteed execution time; Android
 * Doze, network and battery management can defer the poll.
 */
public final class CallTagRemotePolicyScheduler {
    private static final String UNIQUE_WORK = "calltag-remote-safety-policy-v1";

    private CallTagRemotePolicyScheduler() {}

    public static void ensureScheduled(Context context) {
        if (context == null) return;
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                CallTagRemotePolicyWorker.class, 15L, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}
