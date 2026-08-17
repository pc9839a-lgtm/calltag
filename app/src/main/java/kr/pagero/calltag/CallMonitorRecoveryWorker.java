package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

/** Durable secondary recovery when the foreground monitor process/service was reclaimed. */
public final class CallMonitorRecoveryWorker extends Worker {
    private static final long LOOKBACK_MS = 12L * 60L * 60L * 1000L;
    private static final long MATCH_TOLERANCE_MS = 20_000L;
    private static final long GRACE_MS = 5L * 60L * 1000L;
    private static final int LIMIT = 40;

    public CallMonitorRecoveryWorker(@NonNull Context appContext,
                                     @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!SettingsStore.isMonitorEnabled(context)) return Result.success();
        if (!AuthSessionStore.hasSession(context) || !hasCorePermissions(context)) {
            return Result.success();
        }

        try {
            PostCallRecoveryStore.recoverLatest(context, false);
            long now = System.currentTimeMillis();
            long cursor = SettingsStore.callRecoveryCursorAt(context);
            if (cursor <= 0L) {
                cursor = Math.max(0L, now - 2L * 60L * 1000L);
                SettingsStore.setCallRecoveryCursorAt(context, cursor);
            }
            long notBefore = Math.max(now - LOOKBACK_MS,
                    Math.max(0L, cursor - MATCH_TOLERANCE_MS));
            List<CallRecord> recent = CallLogRepository.findRecent(context, notBefore, LIMIT);
            for (CallRecord record : recent) {
                CallRecoveryProcessor.resolveOnce(context, record, "periodic_watchdog");
            }
            SettingsStore.advanceCallRecoveryCursor(context, Math.max(0L, now - GRACE_MS));
            CrashTelemetryStore.record(context, "call_watchdog", "reconciled",
                    "rows=" + recent.size());
            return Result.success();
        } catch (RuntimeException error) {
            CrashTelemetryStore.record(context, "call_watchdog", "failed",
                    error.getClass().getSimpleName());
            return Result.retry();
        }
    }

    private boolean hasCorePermissions(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }
}
