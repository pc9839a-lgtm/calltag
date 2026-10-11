package kr.pagero.calltag;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Periodic best-effort HTTPS policy refresh even when Android app UI is closed. */
public final class CallTagRemotePolicyWorker extends Worker {
    public CallTagRemotePolicyWorker(@NonNull Context context,
                                     @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    @NonNull @Override
    public Result doWork() {
        // No login token is sent; these are public, global disable-only switches.
        CallTagRemotePolicy.refreshOnWorker(getApplicationContext());
        return Result.success();
    }
}
