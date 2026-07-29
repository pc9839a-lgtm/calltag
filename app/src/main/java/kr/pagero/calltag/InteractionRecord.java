package kr.pagero.calltag;

public final class InteractionRecord {
    public final long id;
    public final long customerId;
    public final String customerName;
    public final String phone;
    public final String type;
    public final long startedAt;
    public final long durationSec;
    public final String result;
    public final String note;

    public InteractionRecord(long id, long customerId, String customerName, String phone,
                             String type, long startedAt, long durationSec,
                             String result, String note) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
        this.type = type;
        this.startedAt = startedAt;
        this.durationSec = durationSec;
        this.result = result;
        this.note = note;
    }
}
