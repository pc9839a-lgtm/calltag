package kr.pagero.calltag;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Periodic provider sync. UniversalLeadSyncManager pre-syncs Google Forms before CRM pull. */
public final class ExternalLeadAutoSyncWorker extends Worker {
    private static final long MAX_WAIT_MS = 2L * 60L * 1000L;
    private static final long POLL_MS = 400L;

    public ExternalLeadAutoSyncWorker(
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
            boolean started = UniversalLeadSyncManager.requestSync(app, true);
            if (!started && !UniversalLeadSyncManager.isRunning()) return Result.success();

            long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
            while (UniversalLeadSyncManager.isRunning()
                    && !isStopped()
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(POLL_MS);
            }
            if (isStopped()) return Result.retry();
            return UniversalLeadSyncManager.isRunning() ? Result.retry() : Result.success();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (RuntimeException error) {
            return Result.retry();
        }
    }
}
