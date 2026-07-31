package kr.pagero.calltag;

public final class PendingCallRecord {
    public final long callLogId;
    public final String phone;
    public final String cachedName;
    public final int type;
    public final long startedAt;
    public final long durationSec;

    public PendingCallRecord(long callLogId, String phone, String cachedName,
                             int type, long startedAt, long durationSec) {
        this.callLogId = callLogId;
        this.phone = phone == null ? "" : phone;
        this.cachedName = cachedName == null ? "" : cachedName;
        this.type = type;
        this.startedAt = startedAt;
        this.durationSec = Math.max(0L, durationSec);
    }

    public boolean isOutgoingNoAnswer() {
        return type == android.provider.CallLog.Calls.OUTGOING_TYPE && durationSec == 0L;
    }
}
