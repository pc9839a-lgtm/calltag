package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** CallTag Universal Lead API를 로컬 CRM 고객/상담이력으로 동기화한다. */
public final class UniversalLeadSyncManager {
    public static final String ACTION_LEADS_UPDATED = "kr.pagero.calltag.UNIVERSAL_LEADS_UPDATED";
    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_IMPORTED = "imported";
    public static final String EXTRA_UPDATED = "updated";
    public static final String EXTRA_REJECTED = "rejected";
    public static final String EXTRA_CUSTOMER_IDS = "customer_ids";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_ERROR_CODE = "error_code";

    private static final String TAG = "UniversalLeadSync";
    private static final long MIN_SYNC_INTERVAL_MS = 30_000L;
    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES_PER_RUN = 4;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "calltag-universal-lead-sync");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final AtomicBoolean PENDING_FORCE = new AtomicBoolean(false);
    private static final AtomicBoolean NOTIFY_WHEN_CHANGED = new AtomicBoolean(false);
    private static final AtomicLong LAST_ATTEMPT_AT = new AtomicLong(0L);

    private UniversalLeadSyncManager() {}

    public static boolean requestSync(Context context) {
        return requestSync(context, false);
    }

    public static boolean requestSync(Context context, boolean force) {
        return requestSyncInternal(context, force, false);
    }

    public static boolean requestRealtimeSync(Context context) {
        return requestSyncInternal(context, true, true);
    }

    public static boolean requestSyncAndNotify(Context context, boolean force) {
        return requestSyncInternal(context, force, true);
    }

    private static boolean requestSyncInternal(Context context, boolean force, boolean notifyWhenChanged) {
        if (context == null) return false;
        Context appContext = context.getApplicationContext();
        if (!AuthSessionStore.hasSession(appContext)) {
            if (notifyWhenChanged) NOTIFY_WHEN_CHANGED.set(false);
            sendResult(appContext, false, new SyncResult(),
                    "콜태그 로그인이 필요합니다.", "SESSION_REQUIRED");
            return false;
        }
        if (notifyWhenChanged) NOTIFY_WHEN_CHANGED.set(true);

        long now = System.currentTimeMillis();
        long previous = LAST_ATTEMPT_AT.get();
        if (!force && now - previous < MIN_SYNC_INTERVAL_MS) return false;
        if (force) LAST_ATTEMPT_AT.set(now);
        else if (!LAST_ATTEMPT_AT.compareAndSet(previous, now)) return false;

        if (!RUNNING.compareAndSet(false, true)) {
            if (force) PENDING_FORCE.set(true);
            return true;
        }

        EXECUTOR.execute(() -> {
            boolean changed = false;
            try {
                SyncResult result = syncNow(appContext);
                changed = result.imported > 0 || result.updated > 0;
                if (changed) {
                    ContactNameSyncManager.requestSyncAll(appContext);
                    if (NOTIFY_WHEN_CHANGED.getAndSet(false)) {
                        UniversalLeadNotificationManager.showImported(
                                appContext, result.imported, result.updated, result.customerIds());
                    }
                }
                sendResult(appContext, true, result, successMessage(result), "");
            } catch (UniversalLeadApiClient.ApiException error) {
                String message = safeMessage(error);
                Log.w(TAG, "Universal lead API unavailable: " + error.code);
                sendResult(appContext, false, new SyncResult(), message, error.code);
            } catch (Exception error) {
                String message = safeMessage(error);
                Log.e(TAG, "Universal lead sync failed: " + error.getClass().getSimpleName());
                sendResult(appContext, false, new SyncResult(), message,
                        error.getClass().getSimpleName());
            } finally {
                boolean rerun = PENDING_FORCE.getAndSet(false);
                RUNNING.set(false);
                if (rerun) requestSyncInternal(appContext, true, NOTIFY_WHEN_CHANGED.get());
                else if (!changed) NOTIFY_WHEN_CHANGED.set(false);
            }
        });
        return true;
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    private static SyncResult syncNow(Context context) throws Exception {
        String session = AuthSessionStore.session(context);
        if (session.isEmpty()) throw new IllegalStateException("콜태그 로그인이 필요합니다.");

        // Google Forms is provider-pulled. Refresh it before reading the canonical lead queue.
        // A provider outage must never block Meta/Webhook/PageRo lead delivery.
        try {
            ExternalLeadIntegrationApiClient.syncGoogleForms(session);
        } catch (ExternalLeadIntegrationApiClient.ApiException error) {
            Log.w(TAG, "Google Forms pre-sync skipped: " + error.code);
        } catch (Exception error) {
            Log.w(TAG, "Google Forms pre-sync failed: " + error.getClass().getSimpleName());
        }

        SyncResult result = new SyncResult();
        long after = 0L;
        try (CallTagDbHelper db = new CallTagDbHelper(context);
             UniversalLeadReceiptStore receipts = new UniversalLeadReceiptStore(context)) {
            for (int pageIndex = 0; pageIndex < MAX_PAGES_PER_RUN; pageIndex++) {
                UniversalLeadApiClient.Page page = UniversalLeadApiClient.list(session, after, PAGE_SIZE);
                if (page.leads.isEmpty()) break;

                List<Long> acknowledged = new ArrayList<>();
                for (UniversalLead lead : page.leads) {
                    if (receipts.isImported(lead.eventId)) {
                        acknowledged.add(lead.id);
                        continue;
                    }
                    try {
                        ImportResult imported = importLead(db, lead);
                        receipts.markImported(lead.eventId, lead.id, imported.customerId);
                        acknowledged.add(lead.id);
                        result.record(imported);
                    } catch (IllegalArgumentException invalid) {
                        result.rejected++;
                        try {
                            UniversalLeadApiClient.acknowledgeRejected(
                                    session, lead.id, safeMessage(invalid));
                        } catch (Exception ackError) {
                            Log.w(TAG, "Unable to reject invalid universal lead");
                        }
                    }
                }

                if (!acknowledged.isEmpty()) {
                    UniversalLeadApiClient.acknowledgeImported(
                            session,
                            acknowledged,
                            "신규 고객 " + result.imported + "건, 기존 고객 갱신 "
                                    + result.updated + "건");
                    for (Long id : acknowledged) receipts.markAcked(id);
                }

                after = page.nextAfter;
                if (!page.hasMore) break;
            }
        }
        return result;
    }

    private static ImportResult importLead(CallTagDbHelper db, UniversalLead lead) {
        Customer existing = db.findByPhone(lead.phone);
        boolean created = false;
        long customerId;
        boolean e2eTest = lead.isE2eTest();
        String sourceLabel = sanitizeSourceLabel(lead.sourceLabel());
        if (existing == null) {
            try {
                customerId = db.insertCustomer(
                        lead.customerName, lead.phone, db.firstStage(), sourceLabel);
                created = true;
            } catch (IllegalArgumentException duplicateRace) {
                existing = db.findByPhone(lead.phone);
                if (existing == null) throw duplicateRace;
                customerId = existing.id;
            }
        } else {
            customerId = existing.id;
        }

        Customer current = db.findCustomerById(customerId);
        if (current == null) throw new IllegalArgumentException("고객 저장 후 조회에 실패했습니다.");

        long now = System.currentTimeMillis();
        long contactAt = Math.min(now, Math.max(1L, lead.submittedAt));
        SQLiteDatabase database = db.getWritableDatabase();

        if (!e2eTest || created) {
            String mergedMemo = mergeMemo(current.memo, lead.memoLine());
            ContentValues values = new ContentValues();
            values.put("source", sourceLabel);
            values.put("memo", mergedMemo);
            values.put("last_contact_at", Math.max(current.lastContactAt, contactAt));
            values.put("updated_at", now);
            database.update("customers", values, "id=?", new String[]{String.valueOf(customerId)});
        }

        db.insertInteraction(
                customerId,
                "LEAD_INQUIRY",
                contactAt,
                contactAt,
                0L,
                e2eTest ? "CALLTAG_E2E_TEST" : "CALLTAG_LEAD",
                lead.interactionNote());
        return new ImportResult(customerId, created);
    }

    private static String sanitizeSourceLabel(String value) {
        String safe = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (safe.isEmpty()) return "외부 문의";
        return safe.length() <= 80 ? safe : safe.substring(0, 80);
    }

    private static String mergeMemo(String existing, String incoming) {
        String current = existing == null ? "" : existing.trim();
        String next = incoming == null ? "" : incoming.trim();
        if (next.isEmpty() || current.contains(next)) return current;
        if (current.isEmpty()) return next;
        String merged = next + "\n\n" + current;
        return merged.length() <= 8000 ? merged : merged.substring(0, 8000);
    }

    private static String safeMessage(Throwable error) {
        String message = error == null ? "" : error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "외부 문의를 동기화하지 못했습니다." : message.trim();
    }

    private static String successMessage(SyncResult result) {
        if (result.imported == 0 && result.updated == 0 && result.rejected == 0) {
            return "새로 가져올 문의가 없습니다.";
        }
        StringBuilder message = new StringBuilder();
        if (result.imported > 0) message.append("신규 ").append(result.imported).append("건");
        if (result.updated > 0) {
            if (message.length() > 0) message.append(", ");
            message.append("기존 고객 갱신 ").append(result.updated).append("건");
        }
        if (result.rejected > 0) {
            if (message.length() > 0) message.append(", ");
            message.append("확인 필요 ").append(result.rejected).append("건");
        }
        return message.toString();
    }

    private static void sendResult(
            Context context,
            boolean success,
            SyncResult result,
            String message,
            String errorCode) {
        Intent intent = new Intent(ACTION_LEADS_UPDATED)
                .setPackage(context.getPackageName())
                .putExtra(EXTRA_SUCCESS, success)
                .putExtra(EXTRA_IMPORTED, result.imported)
                .putExtra(EXTRA_UPDATED, result.updated)
                .putExtra(EXTRA_REJECTED, result.rejected)
                .putExtra(EXTRA_CUSTOMER_IDS, result.customerIds())
                .putExtra(EXTRA_MESSAGE, message == null ? "" : message)
                .putExtra(EXTRA_ERROR_CODE, errorCode == null ? "" : errorCode);
        context.sendBroadcast(intent);
    }

    private static final class ImportResult {
        final long customerId;
        final boolean created;

        ImportResult(long customerId, boolean created) {
            this.customerId = customerId;
            this.created = created;
        }
    }

    private static final class SyncResult {
        int imported;
        int updated;
        int rejected;
        final Set<Long> changedCustomerIds = new LinkedHashSet<>();

        void record(ImportResult importedResult) {
            if (importedResult == null) return;
            if (importedResult.created) imported++;
            else updated++;
            if (importedResult.customerId > 0L) changedCustomerIds.add(importedResult.customerId);
        }

        long[] customerIds() {
            long[] values = new long[changedCustomerIds.size()];
            int index = 0;
            for (Long customerId : changedCustomerIds) values[index++] = customerId;
            return values;
        }
    }
}
