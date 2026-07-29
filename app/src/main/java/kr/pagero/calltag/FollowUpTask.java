package kr.pagero.calltag;

public final class FollowUpTask {
    public final long id;
    public final long customerId;
    public final String customerName;
    public final String phone;
    public final String title;
    public final String taskType;
    public final long dueAt;

    public FollowUpTask(long id, long customerId, String customerName, String phone,
                        String title, String taskType, long dueAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
        this.title = title;
        this.taskType = taskType;
        this.dueAt = dueAt;
    }

    public boolean isOverdue() {
        return dueAt < System.currentTimeMillis();
    }
}
