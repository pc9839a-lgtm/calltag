package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class CallTagSyncWorkScheduler {
    static final String INPUT_FORCE = "force";
    static final String INPUT_REASON = "reason";
    static final String INPUT_ACCOUNT = "account";

    private static final String TAG = "calltag-secure-sync";
    private static final String PREFS = "calltag_secure_sync_work";
    private static final String KEY_ACCOUNT = "scheduled_account";
    private static final long PERIOD_MINUTES = 15L;

    private CallTagSyncWorkScheduler() {}

    public static void reconcile(Context context) {
        Context app = context.getApplicationContext();

        // CallTagApplication invokes this on every process start. Reconcile the local CallLog
        // safety net here as well so it remains scheduled independently of cloud-sync eligibility.
        CallMonitorRecoveryScheduler.reconcile(app);

        WorkManager manager = WorkManager.getInstance(app);
        if (!eligible(app)) {
            manager.cancelAllWorkByTag(TAG);
            prefs(app).edit().remove(KEY_ACCOUNT).apply();
            return;
        }

        String account = currentAccountToken(app);
        SharedPreferences prefs = prefs(app);
        String scheduled = prefs.getString(KEY_ACCOUNT, "");
        if (!account.equals(scheduled)) {
            manager.cancelAllWorkByTag(TAG);
            prefs.edit().putString(KEY_ACCOUNT, account).apply();
        }

        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
                CallTagSyncWorker.class,
                PERIOD_MINUTES,
                TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .addTag(TAG)
                .addTag(periodicName(account))
                .setInputData(new Data.Builder()
                        .putBoolean(INPUT_FORCE, false)
                        .putString(INPUT_REASON, "periodic")
                        .putString(INPUT_ACCOUNT, account)
                        .build())
                .build();
        manager.enqueueUniquePeriodicWork(
                periodicName(account),
                ExistingPeriodicWorkPolicy.UPDATE,
                periodic);
    }

    public static void enqueueImmediate(Context context, String reason) {
        Context app = context.getApplicationContext();
        if (!eligible(app) || CallTagSyncManager.isMaintenanceRunning()) return;
        reconcile(app);
        String account = currentAccountToken(app);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CallTagSyncWorker.class)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .setInputData(new Data.Builder()
                        .putBoolean(INPUT_FORCE, true)
                        .putString(INPUT_REASON, safeReason(reason))
                        .putString(INPUT_ACCOUNT, account)
                        .build())
                .addTag(TAG)
                .addTag(immediateName(account))
                .build();
        WorkManager.getInstance(app).enqueueUniqueWork(
                immediateName(account),
                ExistingWorkPolicy.KEEP,
                request);
    }

    public static void cancel(Context context) {
        Context app = context.getApplicationContext();
        WorkManager.getInstance(app).cancelAllWorkByTag(TAG);
        prefs(app).edit().remove(KEY_ACCOUNT).apply();
    }

    static String currentAccountToken(Context context) {
        return hashAccount(CallTagSyncLocalStore.accountKey(context));
    }

    private static boolean eligible(Context context) {
        return AuthSessionStore.hasSession(context)
                && CallTagSyncPreferenceStore.isEnabled(context)
                && !CallTagSyncLocalStore.accountKey(context).isEmpty();
    }

    private static Constraints networkConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String periodicName(String account) {
        return "calltag-secure-sync-periodic-" + account;
    }

    private static String immediateName(String account) {
        return "calltag-secure-sync-immediate-" + account;
    }

    private static String hashAccount(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < 12 && index < bytes.length; index++) {
                result.append(String.format(Locale.ROOT, "%02x", bytes[index]));
            }
            return result.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static String safeReason(String reason) {
        String value = reason == null ? "manual" : reason.trim();
        if (value.isEmpty()) value = "manual";
        return value.length() > 32 ? value.substring(0, 32) : value;
    }
}
