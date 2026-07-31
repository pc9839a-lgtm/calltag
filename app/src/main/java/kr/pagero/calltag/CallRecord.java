package kr.pagero.calltag;

public final class CallRecord {
    public final long id;
    public final String phone;
    public final String cachedName;
    public final int type;
    public final long startedAt;
    public final long durationSec;

    public CallRecord(long id, String phone, String cachedName, int type, long startedAt, long durationSec) {
        this.id = id;
        this.phone = phone == null ? "" : phone;
        this.cachedName = cachedName == null ? "" : cachedName;
        this.type = type;
        this.startedAt = startedAt;
        this.durationSec = durationSec;
    }

    public long endedAt() {
        return startedAt + durationSec * 1000L;
    }
}
