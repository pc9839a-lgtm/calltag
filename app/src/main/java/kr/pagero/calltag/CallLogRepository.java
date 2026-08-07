package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CallLogRepository {
    private static final String[] PROJECTION = {
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
    };

    private CallLogRepository() {}

    public static CallRecord findLatest(Context context, long notBeforeMillis) {
        if (!canRead(context)) return null;

        try (Cursor cursor = query(context, notBeforeMillis)) {
            if (cursor == null || !cursor.moveToFirst()) return null;
            return read(cursor);
        } catch (SecurityException | IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Returns recent call-log rows oldest-first so process-restart recovery preserves call order.
     */
    public static List<CallRecord> findRecent(Context context, long notBeforeMillis, int limit) {
        if (!canRead(context)) return Collections.emptyList();
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<CallRecord> rows = new ArrayList<>();
        try (Cursor cursor = query(context, notBeforeMillis)) {
            if (cursor == null) return rows;
            while (cursor.moveToNext() && rows.size() < safeLimit) {
                rows.add(read(cursor));
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
            return Collections.emptyList();
        }
        Collections.reverse(rows);
        return rows;
    }

    private static Cursor query(Context context, long notBeforeMillis) {
        String selection = CallLog.Calls.DATE + " >= ? AND "
                + CallLog.Calls.TYPE + " IN (?,?,?,?)";
        String[] args = {
                String.valueOf(Math.max(0L, notBeforeMillis)),
                String.valueOf(CallLog.Calls.INCOMING_TYPE),
                String.valueOf(CallLog.Calls.MISSED_TYPE),
                String.valueOf(CallLog.Calls.REJECTED_TYPE),
                String.valueOf(CallLog.Calls.OUTGOING_TYPE)
        };
        return context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                PROJECTION,
                selection,
                args,
                CallLog.Calls.DATE + " DESC," + CallLog.Calls._ID + " DESC");
    }

    private static CallRecord read(Cursor cursor) {
        return new CallRecord(
                cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls._ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)),
                cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)));
    }

    private static boolean canRead(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }
}
