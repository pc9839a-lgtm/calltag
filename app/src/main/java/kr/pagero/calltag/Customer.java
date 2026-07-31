package kr.pagero.calltag;

public final class Customer {
    public final long id;
    public final String displayName;
    public final String primaryPhone;
    public final String normalizedPhone;
    public final String relationStatus;
    public final String source;
    public final String memo;
    public final long firstContactAt;
    public final long lastContactAt;
    public final Long firstTransactionAt;

    public Customer(
            long id,
            String displayName,
            String primaryPhone,
            String normalizedPhone,
            String relationStatus,
            String source,
            String memo,
            long firstContactAt,
            long lastContactAt,
            Long firstTransactionAt) {
        this.id = id;
        this.displayName = displayName;
        this.primaryPhone = primaryPhone;
        this.normalizedPhone = normalizedPhone;
        this.relationStatus = relationStatus;
        this.source = source;
        this.memo = memo;
        this.firstContactAt = firstContactAt;
        this.lastContactAt = lastContactAt;
        this.firstTransactionAt = firstTransactionAt;
    }
}
