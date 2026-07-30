package kr.pagero.calltag;

public final class MessageRecord {
    public final long id;
    public final long customerId;
    public final long callLogId;
    public final String phone;
    public final String normalizedPhone;
    public final String body;
    public final String triggerType;
    public final String status;
    public final long scheduledAt;
    public final long sentAt;
    public final String error;
    public final int subscriptionId;
    public final long createdAt;

    public MessageRecord(long id, long customerId, long callLogId,
                         String phone, String normalizedPhone, String body,
                         String triggerType, String status, long scheduledAt,
                         long sentAt, String error, int subscriptionId, long createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.callLogId = callLogId;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
        this.body = body;
        this.triggerType = triggerType;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.sentAt = sentAt;
        this.error = error;
        this.subscriptionId = subscriptionId;
        this.createdAt = createdAt;
    }
}
