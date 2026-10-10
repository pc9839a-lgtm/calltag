package kr.pagero.calltag;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Independent background safety-net for Meta/Google Forms/Webhook universal leads. */
public final class ExternalLeadSyncWorker extends Worker {
    public ExternalLeadSyncWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context app = getApplicationContext();
        if (!AuthSessionStore.hasSession(app)) return Result.success();
        if (isStopped()) return Result.retry();

        boolean pollGoogleForms = !getInputData().getBoolean(
                ExternalLeadSyncWorkScheduler.KEY_SKIP_GOOGLE_FORMS_POLL, false);
        UniversalLeadSyncManager.WorkerSyncResult outcome =
                UniversalLeadSyncManager.runWorkerSync(app, pollGoogleForms);
        if (outcome == UniversalLeadSyncManager.WorkerSyncResult.RETRY) {
            return Result.retry();
        }
        if (outcome == UniversalLeadSyncManager.WorkerSyncResult.FAILURE) {
            return Result.failure();
        }
        return Result.success();
    }
}
