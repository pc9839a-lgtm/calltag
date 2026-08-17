package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/** Processes a CallLog row discovered after the live monitor missed it or the process was killed. */
public final class CallRecoveryProcessor {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private CallRecoveryProcessor() {}

    public static boolean resolveOnce(Context context, CallRecord record, String source) {
        if (context == null || record == null || record.id <= 0L) return false;
        Context app = context.getApplicationContext();
        if (PhoneNumberNormalizer.normalize(record.phone).length() < 8) {
            CallProcessingLedger.markResolved(app, record.id);
            SettingsStore.advanceCallRecoveryCursor(app, recoveryPoint(record));
            return false;
        }
        if (CallProcessingLedger.wasResolved(app, record.id)) {
            SettingsStore.advanceCallRecoveryCursor(app, recoveryPoint(record));
            return false;
        }

        try {
            process(app, record);
            CallProcessingLedger.markResolved(app, record.id);
            SettingsStore.setLastCallId(app, record.id);
            SettingsStore.advanceCallRecoveryCursor(app, recoveryPoint(record));
            CrashTelemetryStore.record(app, "call_resolution", "recovered_once",
                    safe(source) + ",call=" + record.id);
            return true;
        } catch (RuntimeException error) {
            CrashTelemetryStore.record(app, "call_resolution", "recovery_failed",
                    safe(source) + ",call=" + record.id + ","
                            + error.getClass().getSimpleName());
            return false;
        }
    }

    private static void process(Context context, CallRecord record) {
        boolean phoneAccess = FeatureEntitlementStore.hasPhoneAccess(context);
        boolean messageAccess = FeatureEntitlementStore.hasMessageAccess(context);
        if (!phoneAccess && !messageAccess) return;

        CallTagDbHelper db = new CallTagDbHelper(context);
        PendingCallStore pendingStore = phoneAccess ? new PendingCallStore(context) : null;
        try {
            if (db.isExcluded(record.phone)) return;

            boolean deferred = CallDisposition.needsFollowUp(record);
            boolean connected = CallDisposition.isConnected(record);
            if (phoneAccess && pendingStore != null) {
                if (deferred) {
                    pendingStore.upsert(record);
                    sendPendingChanged(context);
                } else if (connected) {
                    pendingStore.markUnansweredHandledByPhone(record.phone, record.startedAt + 1L);
                    TaskAutomation.completeNextCallTask(context, record.phone);
                    sendPendingChanged(context);
                }
            }

            Customer customer = db.findByPhone(record.phone);
            if (messageAccess) {
                MessageAutomationManager.onCallResolved(context, record, customer);
            }

            if (phoneAccess) {
                long pendingCallId = deferred ? record.id : -1L;
                Intent review = new Intent(context, PostCallActivity.class)
                        .putExtra(PostCallActivity.EXTRA_PENDING_CALL_ID, pendingCallId)
                        .putExtra(PostCallActivity.EXTRA_CALL_LOG_ID, record.id)
                        .putExtra(PostCallActivity.EXTRA_PHONE, record.phone)
                        .putExtra(PostCallActivity.EXTRA_CACHED_NAME, record.cachedName)
                        .putExtra(PostCallActivity.EXTRA_CALL_TYPE, record.type)
                        .putExtra(PostCallActivity.EXTRA_STARTED_AT, record.startedAt)
                        .putExtra(PostCallActivity.EXTRA_ENDED_AT,
                                Math.max(record.endedAt(), System.currentTimeMillis()))
                        .putExtra(PostCallActivity.EXTRA_DURATION_SEC,
                                Math.max(0L, record.durationSec))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                PostCallRecoveryStore.arm(context, record, pendingCallId);
                String memo = customer == null
                        ? "" : CustomerInsightResolver.latestMemo(db, customer);
                // Recovery can originate from a Worker while the app is backgrounded. Do not try
                // to force an Activity from that state. The compact overlay is used when already
                // authorized; otherwise the notification fallback remains available.
                MAIN.post(() -> {
                    boolean delivered = CallPopupNotificationManager.showPostCall(
                            context, record, customer, review, memo);
                    if (delivered) PostCallRecoveryStore.markDelivered(context, record.id);
                });
            }
        } finally {
            if (pendingStore != null) pendingStore.close();
            db.close();
        }
    }

    private static void sendPendingChanged(Context context) {
        context.sendBroadcast(new Intent(PendingCallSectionView.ACTION_CHANGED)
                .setPackage(context.getPackageName()));
    }

    private static long recoveryPoint(CallRecord record) {
        return Math.max(record.startedAt + 1L, record.endedAt() + 1L);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
