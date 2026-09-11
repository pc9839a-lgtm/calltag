package kr.pagero.calltag;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Independent background safety-net for Meta/Google Forms/Webhook universal leads. */
public final class ExternalLeadSyncWorker extends Worker {
    private static final long MAX_WAIT_MS = 4L * 60L * 1000L;
    private static final long POLL_MS = 400L;

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
        try {
            UniversalLeadSyncManager.requestSync(app, true);
            long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
            while (UniversalLeadSyncManager.isRunning()
                    && !isStopped()
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(POLL_MS);
            }
            if (isStopped()) return Result.retry();
            if (UniversalLeadSyncManager.isRunning()) return Result.retry();
            return Result.success();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (RuntimeException error) {
            return Result.retry();
        }
    }
}
