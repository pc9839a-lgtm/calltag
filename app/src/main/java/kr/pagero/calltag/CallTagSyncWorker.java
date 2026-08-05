package kr.pagero.calltag;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class CallTagSyncWorker extends Worker {
    private static final long MAX_WAIT_MS = 8L * 60L * 1000L;
    private static final long POLL_MS = 400L;

    public CallTagSyncWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context app = getApplicationContext();
        if (!AuthSessionStore.hasSession(app)
                || !CallTagSyncPreferenceStore.isEnabled(app)) {
            return Result.success();
        }
        if (CallTagSyncManager.isMaintenanceRunning()) {
            return Result.success();
        }

        boolean force = getInputData().getBoolean(
                CallTagSyncWorkScheduler.INPUT_FORCE, false);
        try {
            CallTagSyncManager.request(app, force);
            long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
            while (CallTagSyncManager.isRunning()
                    && !isStopped()
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(POLL_MS);
            }
            if (isStopped()) return Result.retry();
            if (CallTagSyncManager.isRunning()) return Result.retry();

            CallTagSyncLocalStore store = new CallTagSyncLocalStore(app);
            CallTagSyncLocalStore.StatusSnapshot status;
            try {
                status = store.status();
            } finally {
                store.close();
            }
            if ("WAITING".equals(status.status)) return Result.retry();
            if ("ERROR".equals(status.status) && !isConflict(status.message)) {
                return Result.retry();
            }
            return Result.success();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (RuntimeException error) {
            return Result.retry();
        }
    }

    private boolean isConflict(String message) {
        String value = message == null ? "" : message;
        return value.contains("다른 기기")
                || value.contains("겹쳤")
                || value.contains("충돌");
    }
}
