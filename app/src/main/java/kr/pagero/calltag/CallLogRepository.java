package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

public final class CallLogRepository {
    private CallLogRepository() {}

    public static CallRecord findLatest(Context context, long notBeforeMillis) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        String[] projection = {
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };
        String selection = CallLog.Calls.DATE + " >= ? AND " + CallLog.Calls.TYPE + " IN (?,?,?,?)";
        String[] args = {
                String.valueOf(notBeforeMillis),
                String.valueOf(CallLog.Calls.INCOMING_TYPE),
                String.valueOf(CallLog.Calls.MISSED_TYPE),
                String.valueOf(CallLog.Calls.REJECTED_TYPE),
                String.valueOf(CallLog.Calls.OUTGOING_TYPE)
        };

        try (Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                args,
                CallLog.Calls.DATE + " DESC")) {
            if (cursor == null || !cursor.moveToFirst()) return null;
            return new CallRecord(
                    cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls._ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)));
        } catch (SecurityException | IllegalArgumentException ignored) {
            return null;
        }
    }
}
