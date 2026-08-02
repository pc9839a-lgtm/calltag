package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class PageroLeadSyncManager {
    public static final String ACTION_LEADS_UPDATED = "kr.pagero.calltag.PAGERO_LEADS_UPDATED";

    private static final String TAG = "PageroLeadSync";
    private static final long MIN_SYNC_INTERVAL_MS = 60_000L;
    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES_PER_RUN = 4;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "calltag-pagero-lead-sync");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final AtomicLong LAST_ATTEMPT_AT = new AtomicLong(0L);

    private PageroLeadSyncManager() {}

    public static void requestSync(Context context) {
        requestSync(context, false);
    }

    public static void requestSync(Context context, boolean force) {
        if (context == null || !AuthSessionStore.hasSession(context)) return;
        long now = System.currentTimeMillis();
        long previous = LAST_ATTEMPT_AT.get();
        if (!force && now - previous < MIN_SYNC_INTERVAL_MS) return;
        if (!LAST_ATTEMPT_AT.compareAndSet(previous, now) && !force) return;
        if (!RUNNING.compareAndSet(false, true)) return;

        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                SyncResult result = syncNow(appContext);
                if (result.imported > 0 || result.updated > 0) {
                    appContext.sendBroadcast(new android.content.Intent(ACTION_LEADS_UPDATED)
                            .setPackage(appContext.getPackageName())
                            .putExtra("imported", result.imported)
                            .putExtra("updated", result.updated));
                }
            } catch (PageroLeadApiClient.ApiException error) {
                Log.w(TAG, "PageRo sync API unavailable: " + error.status + "/" + error.code);
            } catch (Exception error) {
                Log.e(TAG, "PageRo lead sync failed", error);
            } finally {
                RUNNING.set(false);
            }
        });
    }

    private static SyncResult syncNow(Context context) throws Exception {
        String session = AuthSessionStore.session(context);
        if (session.isEmpty()) return new SyncResult();

        SyncResult result = new SyncResult();
        long after = 0L;
        try (CallTagDbHelper db = new CallTagDbHelper(context);
             PageroLeadReceiptStore receipts = new PageroLeadReceiptStore(context)) {
            for (int pageIndex = 0; pageIndex < MAX_PAGES_PER_RUN; pageIndex++) {
                PageroLeadApiClient.Page page = PageroLeadApiClient.list(session, after, PAGE_SIZE);
                if (page.leads.isEmpty()) break;

                List<Long> acknowledged = new ArrayList<>();
                for (PageroLead lead : page.leads) {
                    if (receipts.isImported(lead.eventId)) {
                        acknowledged.add(lead.id);
                        continue;
                    }
                    try {
                        ImportResult imported = importLead(db, lead);
                        receipts.markImported(lead.eventId, lead.id, imported.customerId);
                        acknowledged.add(lead.id);
                        if (imported.created) result.imported++;
                        else result.updated++;
                    } catch (IllegalArgumentException invalid) {
                        result.rejected++;
                        try {
                            PageroLeadApiClient.acknowledgeRejected(
                                    session, lead.id, safeMessage(invalid));
                        } catch (Exception ackError) {
                            Log.w(TAG, "Unable to reject invalid PageRo lead " + lead.id, ackError);
                        }
                    }
                }

                if (!acknowledged.isEmpty()) {
                    PageroLeadApiClient.acknowledgeImported(
                            session,
                            acknowledged,
                            "신규 고객 " + result.imported + "건, 기존 고객 갱신 " + result.updated + "건");
                    for (Long id : acknowledged) receipts.markAcked(id);
                }

                after = page.nextAfter;
                if (!page.hasMore) break;
            }
        }
        return result;
    }

    private static ImportResult importLead(CallTagDbHelper db, PageroLead lead) {
        Customer existing = db.findByPhone(lead.phone);
        boolean created = false;
        long customerId;

        if (existing == null) {
            try {
                customerId = db.insertCustomer(
                        lead.customerName,
                        lead.phone,
                        db.firstStage(),
                        CustomerSourceResolver.PAGERO);
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
        String mergedMemo = mergeMemo(current.memo, lead.memoLine());
        ContentValues values = new ContentValues();
        values.put("source", CustomerSourceResolver.PAGERO);
        values.put("memo", mergedMemo);
        values.put("last_contact_at", Math.max(current.lastContactAt, contactAt));
        values.put("updated_at", now);
        SQLiteDatabase database = db.getWritableDatabase();
        database.update("customers", values, "id=?", new String[]{String.valueOf(customerId)});

        db.insertInteraction(
                customerId,
                "PAGERO_INQUIRY",
                contactAt,
                contactAt,
                0L,
                "PAGERO_LEAD",
                lead.inquiryContent.isEmpty() ? "페이지로 문의 접수" : lead.inquiryContent);

        return new ImportResult(customerId, created);
    }

    private static String mergeMemo(String existing, String incoming) {
        String current = existing == null ? "" : existing.trim();
        String next = incoming == null ? "" : incoming.trim();
        if (next.isEmpty() || current.contains(next)) return current;
        if (current.isEmpty()) return next;
        String merged = next + "\n\n" + current;
        return merged.length() <= 4000 ? merged : merged.substring(0, 4000);
    }

    private static String safeMessage(Throwable error) {
        String message = error == null ? "" : error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "앱에서 문의를 처리할 수 없습니다."
                : message.trim();
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
    }
}
