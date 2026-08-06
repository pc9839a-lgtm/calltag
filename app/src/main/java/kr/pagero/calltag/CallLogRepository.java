package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

public final class CallLogRepository {
    private static final int RECENT_SCAN_LIMIT = 12;

    private CallLogRepository() {}

    public static CallRecord findLatest(Context context, long notBeforeMillis) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        String[] projection = projection();
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
            return read(cursor);
        } catch (SecurityException | IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * 전화 상태 콜백을 제조사 전화 앱이 누락해도 통화기록의 종료 시각을 기준으로
     * 방금 끝난 통화를 복구한다. 긴 통화는 시작 시각이 오래됐기 때문에 DATE 조건만으로
     * 찾으면 누락될 수 있어 최근 행을 제한적으로 확인한다.
     */
    public static CallRecord findLatestEndingAfter(Context context, long endedAfterMillis) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        String selection = CallLog.Calls.TYPE + " IN (?,?,?,?)";
        String[] args = {
                String.valueOf(CallLog.Calls.INCOMING_TYPE),
                String.valueOf(CallLog.Calls.MISSED_TYPE),
                String.valueOf(CallLog.Calls.REJECTED_TYPE),
                String.valueOf(CallLog.Calls.OUTGOING_TYPE)
        };
        try (Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection(),
                selection,
                args,
                CallLog.Calls.DATE + " DESC")) {
            if (cursor == null) return null;
            int scanned = 0;
            while (cursor.moveToNext() && scanned++ < RECENT_SCAN_LIMIT) {
                CallRecord record = read(cursor);
                long resolvedAt = Math.max(record.startedAt, record.endedAt());
                if (resolvedAt >= endedAfterMillis) return record;
            }
            return null;
        } catch (SecurityException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String[] projection() {
        return new String[]{
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };
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
}
