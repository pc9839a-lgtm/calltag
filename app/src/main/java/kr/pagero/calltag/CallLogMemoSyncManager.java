package kr.pagero.calltag;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps CallTag memo text inside the system call-log cache only.
 * This class never writes to ContactsContract and never creates/merges contacts.
 */
public final class CallLogMemoSyncManager {
    private static final int MAX_BASE_LENGTH = 14;
    private static final int MAX_MEMO_LENGTH = 16;
    private static final int MAX_ALIAS_LENGTH = 32;
    private static final int MAX_MATCHED_ROWS = 100;
    private static final long HISTORY_WINDOW_MS = 365L * 24L * 60L * 60L * 1000L;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean SYNCING_ALL = new AtomicBoolean(false);
    private static final AtomicBoolean RESYNC_ALL = new AtomicBoolean(false);

    private CallLogMemoSyncManager() {}

    public static boolean hasPermissions(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.WRITE_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestSyncForCall(Context context, long callLogId, String phone,
                                          String customerName, String memo) {
        Context app = context.getApplicationContext();
        if (!hasPermissions(app)) {
            CrashTelemetryStore.record(app, "call_log_memo", "permission_missing", "single");
            return;
        }
        String safePhone = safe(phone);
        String alias = buildDisplayName(customerName, memo, safePhone);
        EXECUTOR.execute(() -> {
            int updated = updateCallById(app.getContentResolver(), callLogId, alias);
            if (updated == 0 && !safePhone.isEmpty()) {
                updated = updateRecentMatching(app.getContentResolver(), safePhone, alias, 1);
            }
            CrashTelemetryStore.record(app, "call_log_memo",
                    updated > 0 ? "single_updated" : "single_not_found",
                    "count=" + updated);
        });
    }

    public static void requestSyncForCustomer(Context context, String phone,
                                              String customerName, String memo) {
        Context app = context.getApplicationContext();
        if (!hasPermissions(app)) {
            CrashTelemetryStore.record(app, "call_log_memo", "permission_missing", "customer");
            return;
        }
        String safePhone = safe(phone);
        if (PhoneNumberNormalizer.normalize(safePhone).length() < 8) return;
        String alias = buildDisplayName(customerName, memo, safePhone);
        EXECUTOR.execute(() -> {
            int updated = updateRecentMatching(app.getContentResolver(), safePhone,
                    alias, MAX_MATCHED_ROWS);
            CrashTelemetryStore.record(app, "call_log_memo", "customer_updated",
                    "count=" + updated);
        });
    }

    public static void requestSyncAll(Context context) {
        Context app = context.getApplicationContext();
        if (!hasPermissions(app)) return;
        if (!SYNCING_ALL.compareAndSet(false, true)) {
            RESYNC_ALL.set(true);
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                do {
                    RESYNC_ALL.set(false);
                    syncAllNow(app);
                } while (RESYNC_ALL.get());
            } finally {
                SYNCING_ALL.set(false);
            }
        });
    }

    private static void syncAllNow(Context context) {
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            List<Customer> customers = db.listCustomers(null);
            for (Customer customer : customers) {
                if (customer == null) continue;
                String phone = safe(customer.primaryPhone);
                if (PhoneNumberNormalizer.normalize(phone).length() < 8) continue;
                String memo = CustomerInsightResolver.latestMemo(db, customer);
                String alias = buildDisplayName(customer.displayName, memo, phone);
                updateRecentMatching(context.getContentResolver(), phone, alias,
                        MAX_MATCHED_ROWS);
            }
        } catch (RuntimeException error) {
            CrashTelemetryStore.record(context, "call_log_memo", "sync_all_failed",
                    error.getClass().getSimpleName());
        } finally {
            db.close();
        }
    }

    private static int updateCallById(ContentResolver resolver, long callLogId, String alias) {
        if (callLogId <= 0L) return 0;
        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.CACHED_NAME, alias);
        try {
            return resolver.update(ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, callLogId),
                    values, null, null);
        } catch (SecurityException | IllegalArgumentException error) {
            return 0;
        }
    }

    private static int updateRecentMatching(ContentResolver resolver, String phone,
                                            String alias, int maxMatches) {
        String target = PhoneNumberNormalizer.normalize(phone);
        if (target.length() < 8) return 0;

        long since = System.currentTimeMillis() - HISTORY_WINDOW_MS;
        int updated = 0;
        try (Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls._ID, CallLog.Calls.NUMBER},
                CallLog.Calls.DATE + ">=?",
                new String[]{String.valueOf(since)},
                CallLog.Calls.DATE + " DESC")) {
            if (cursor == null) return 0;
            int idIndex = cursor.getColumnIndexOrThrow(CallLog.Calls._ID);
            int numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER);
            while (cursor.moveToNext() && updated < Math.max(1, maxMatches)) {
                String rowPhone = cursor.getString(numberIndex);
                if (!target.equals(PhoneNumberNormalizer.normalize(rowPhone))) continue;
                long rowId = cursor.getLong(idIndex);
                updated += updateCallById(resolver, rowId, alias) > 0 ? 1 : 0;
            }
        } catch (SecurityException | IllegalArgumentException error) {
            return updated;
        }
        return updated;
    }

    static String buildDisplayName(String customerName, String memo, String phone) {
        String base = clean(customerName);
        if (base.isEmpty() || "이름없는고객".equals(base) || "이름 없음".equals(base)) {
            String digits = PhoneNumberNormalizer.normalize(phone);
            String suffix = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
            base = suffix.isEmpty() ? "고객" : "고객 " + suffix;
        }
        base = shorten(base, MAX_BASE_LENGTH);

        String note = shorten(clean(memo), MAX_MEMO_LENGTH);
        if (note.isEmpty()) return base;
        return shorten(base + " · " + note, MAX_ALIAS_LENGTH);
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .replace("·", " ")
                .trim();
    }

    private static String shorten(String value, int maxLength) {
        String clean = safe(value).trim();
        if (clean.length() <= maxLength) return clean;
        return clean.substring(0, Math.max(1, maxLength - 1)).trim() + "…";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
