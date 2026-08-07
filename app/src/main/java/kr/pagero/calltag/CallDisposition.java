package kr.pagero.calltag;

import android.provider.CallLog;

/** Single source of truth for connected vs. unanswered call handling. */
public final class CallDisposition {
    private CallDisposition() {}

    public static boolean needsFollowUp(CallRecord record) {
        if (record == null) return false;
        return record.type == CallLog.Calls.MISSED_TYPE
                || record.type == CallLog.Calls.REJECTED_TYPE
                || (record.type == CallLog.Calls.OUTGOING_TYPE && record.durationSec == 0L);
    }

    public static boolean isConnected(CallRecord record) {
        return record != null && !needsFollowUp(record) && record.durationSec > 0L;
    }

    public static String interactionType(int callType) {
        if (callType == CallLog.Calls.OUTGOING_TYPE) return "OUTGOING_CALL";
        if (callType == CallLog.Calls.MISSED_TYPE) return "MISSED_CALL";
        if (callType == CallLog.Calls.REJECTED_TYPE) return "REJECTED_CALL";
        return "INCOMING_CALL";
    }

    public static String label(CallRecord record) {
        if (record == null) return "통화";
        if (record.type == CallLog.Calls.MISSED_TYPE) return "부재중 전화";
        if (record.type == CallLog.Calls.REJECTED_TYPE) return "거절한 전화";
        if (record.type == CallLog.Calls.OUTGOING_TYPE && record.durationSec == 0L) {
            return "발신 · 연결 안 됨";
        }
        if (record.type == CallLog.Calls.OUTGOING_TYPE) return "발신 통화 종료";
        return "수신 통화 종료";
    }
}
