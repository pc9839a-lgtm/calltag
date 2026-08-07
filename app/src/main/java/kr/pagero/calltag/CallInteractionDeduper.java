package kr.pagero.calltag;

import android.database.Cursor;

/**
 * Prevents duplicate call interactions even if the post-call screen is delivered more than once.
 *
 * This deliberately dedupes against the CRM database itself instead of relying only on a
 * SharedPreferences receipt. If the process dies immediately after the INSERT, the next delivery
 * still sees the existing interaction and returns its id instead of inserting a second row.
 */
public final class CallInteractionDeduper {
    private CallInteractionDeduper() {}

    public static synchronized long insertOnce(
            CallTagDbHelper db,
            long customerId,
            String type,
            long startedAt,
            long endedAt,
            long durationSec,
            String result,
            String note) {
        long existing = findExisting(db, customerId, type, startedAt, endedAt, durationSec);
        if (existing > 0L) return existing;
        return db.insertInteraction(customerId, type, startedAt, endedAt,
                durationSec, result, note);
    }

    static long findExisting(
            CallTagDbHelper db,
            long customerId,
            String type,
            long startedAt,
            long endedAt,
            long durationSec) {
        String sql = "SELECT id FROM interactions WHERE customer_id=? AND type=? "
                + "AND started_at=? AND COALESCE(ended_at,0)=? AND duration_sec=? "
                + "ORDER BY id DESC LIMIT 1";
        String[] args = {
                String.valueOf(customerId),
                type == null ? "" : type,
                String.valueOf(startedAt),
                String.valueOf(endedAt),
                String.valueOf(Math.max(0L, durationSec))
        };
        try (Cursor cursor = db.getReadableDatabase().rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }
}
