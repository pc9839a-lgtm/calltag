package kr.pagero.calltag;

public final class FollowUpTask {
    public final long id;
    public final long customerId;
    public final String customerName;
    public final String phone;
    public final String title;
    public final String taskType;
    public final long dueAt;
    public final String status;

    public FollowUpTask(long id, long customerId, String customerName, String phone,
                        String title, String taskType, long dueAt) {
        this(id, customerId, customerName, phone, title, taskType, dueAt, "PENDING");
    }

    public FollowUpTask(long id, long customerId, String customerName, String phone,
                        String title, String taskType, long dueAt, String status) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
        this.title = title;
        this.taskType = taskType;
        this.dueAt = dueAt;
        this.status = status == null ? "PENDING" : status;
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isOverdue() {
        return !isCompleted() && dueAt < System.currentTimeMillis();
    }
}
