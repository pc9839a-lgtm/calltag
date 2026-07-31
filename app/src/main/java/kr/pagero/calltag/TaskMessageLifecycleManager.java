package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 일정 생성·변경·완료·삭제와 연결 후속문자의 상태를 함께 관리한다. */
public final class TaskMessageLifecycleManager {
    public static final String MODE_NONE = "NONE";
    public static final String MODE_NOW = "NOW";
    public static final String MODE_DAY_BEFORE = "DAY_BEFORE";
    public static final String MODE_AT_TIME = "AT_TIME";
    public static final String MODE_AFTER_COMPLETE = "AFTER_COMPLETE";

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private TaskMessageLifecycleManager() {}

    public static Result attach(Context context, FollowUpTask task, String requestedMode) {
        if (task == null || task.id <= 0L) return Result.fail("일정 정보를 찾을 수 없습니다.");
        String mode = normalizeMode(requestedMode);
        if (MODE_NONE.equals(mode)) {
            detach(context, task.id, "연결 문자를 사용하지 않도록 변경했습니다.");
            return Result.ok("연결 문자를 사용하지 않습니다.", 0L);
        }
        if (!FeatureEntitlementStore.hasMessageAccess(context)) {
            return Result.fail("문자자동화 이용권이 필요합니다.");
        }

        Snapshot snapshot = buildSnapshot(context, task);
        if (!snapshot.ready) return Result.fail(snapshot.error);

        MessageExclusionStore.Decision exclusion = MessageExclusionStore.evaluate(
                context, task.customerId, task.phone, MessageAutomationManager.TRIGGER_DELAYED);
        if (exclusion.blocked) return Result.fail(exclusion.reason);

        detach(context, task.id, "연결 문자 설정이 변경돼 이전 예약을 취소했습니다.");
        int subscriptionId = MessageAutomationStore.selectedSubscriptionId(context);
        long now = System.currentTimeMillis();
        long messageId = 0L;
        long messageScheduledAt = 0L;

        if (MODE_AFTER_COMPLETE.equals(mode)) {
            TaskMessageLinkStore.save(context, new TaskMessageLinkStore.Link(
                    task.id, task.customerId, mode, 0L, snapshot.templateId,
                    snapshot.body, subscriptionId, task.dueAt, 0L, now));
            notifyChanged(context);
            return Result.ok("일정 완료 후 후속문자를 발송하도록 연결했습니다.", 0L);
        }

        if (MODE_NOW.equals(mode)) {
            messageId = SmsSender.queueAndSend(context, task.customerId, 0L,
                    task.id, "", snapshot.templateId, task.phone, snapshot.body,
                    MessageAutomationManager.TRIGGER_DELAYED, subscriptionId, false);
            TaskMessageLinkStore.save(context, new TaskMessageLinkStore.Link(
                    task.id, task.customerId, mode, messageId, snapshot.templateId,
                    snapshot.body, subscriptionId, task.dueAt, now, now));
            notifyChanged(context);
            MessageRecord record = findMessage(context, messageId);
            if (record == null) return Result.fail("안내문자 발송 내역을 만들지 못했습니다.");
            if (MessageLogStore.STATUS_FAILED.equals(record.status)
                    || MessageLogStore.STATUS_SKIPPED.equals(record.status)
                    || MessageLogStore.STATUS_CANCELLED.equals(record.status)) {
                return Result.fail(record.error == null || record.error.trim().isEmpty()
                        ? "안내문자를 발송하지 못했습니다." : record.error);
            }
            return Result.ok("일정 안내문자 발송을 요청했습니다.", messageId);
        }

        messageScheduledAt = scheduledAt(mode, task.dueAt, now);
        MessageLogStore messages = new MessageLogStore(context);
        try {
            messageId = messages.createJobAdvanced(task.customerId, 0L, task.id,
                    "", snapshot.templateId, task.phone, snapshot.body,
                    MessageAutomationManager.TRIGGER_DELAYED,
                    MessageLogStore.STATUS_SCHEDULED, messageScheduledAt,
                    subscriptionId, false);
            MessageRecord record = messages.find(messageId);
            TaskMessageLinkStore.save(context, new TaskMessageLinkStore.Link(
                    task.id, task.customerId, mode, messageId, snapshot.templateId,
                    snapshot.body, subscriptionId, task.dueAt,
                    messageScheduledAt, now));
            if (record == null || !MessageLogStore.STATUS_SCHEDULED.equals(record.status)) {
                notifyChanged(context);
                return Result.fail(record == null || record.error == null
                        || record.error.trim().isEmpty()
                        ? "후속문자를 예약하지 못했습니다." : record.error);
            }
        } finally {
            messages.close();
        }
        MessageScheduler.schedule(context, messageId, messageScheduledAt);
        notifyChanged(context);
        String timing = MODE_DAY_BEFORE.equals(mode) && task.dueAt - DAY_MS <= now
                ? "일정이 24시간 이내라 지금 발송 예정으로 등록했습니다."
                : modeLabel(mode) + " 후속문자를 예약했습니다.";
        return Result.ok(timing, messageId);
    }

    public static Result reschedule(Context context, FollowUpTask updatedTask) {
        TaskMessageLinkStore.Link link = TaskMessageLinkStore.find(context, updatedTask.id);
        if (link == null || MODE_NONE.equals(normalizeMode(link.mode))) {
            return Result.ok("연결된 후속문자가 없습니다.", 0L);
        }
        if (MODE_NOW.equals(link.mode)) {
            return Result.ok("이미 발송한 일정 안내문자는 유지했습니다.", link.messageId);
        }
        return attach(context, updatedTask, link.mode);
    }

    public static void onTaskCompleted(Context context, FollowUpTask task) {
        TaskMessageLinkStore.Link link = TaskMessageLinkStore.find(context, task.id);
        if (link == null) return;
        if (MODE_AFTER_COMPLETE.equals(link.mode)) {
            MessageRecord previous = findMessage(context, link.messageId);
            if (previous != null && (MessageLogStore.STATUS_READY.equals(previous.status)
                    || MessageLogStore.STATUS_SENDING.equals(previous.status)
                    || MessageLogStore.STATUS_SENT.equals(previous.status))) return;

            String body = link.bodySnapshot;
            String templateId = link.templateId;
            if (body == null || body.trim().isEmpty()) {
                Snapshot snapshot = buildSnapshot(context, task);
                if (!snapshot.ready) return;
                body = snapshot.body;
                templateId = snapshot.templateId;
            }
            long messageId = SmsSender.queueAndSend(context, task.customerId, 0L,
                    task.id, "", templateId, task.phone, body,
                    MessageAutomationManager.TRIGGER_DELAYED,
                    link.subscriptionId, false);
            link.messageId = messageId;
            link.messageScheduledAt = System.currentTimeMillis();
            link.bodySnapshot = body;
            link.templateId = templateId;
            TaskMessageLinkStore.save(context, link);
            notifyChanged(context);
            return;
        }
        if (MODE_DAY_BEFORE.equals(link.mode) || MODE_AT_TIME.equals(link.mode)) {
            cancelLinkedMessage(context, link,
                    "연결된 일정이 완료돼 남은 후속문자를 취소했습니다.");
            TaskMessageLinkStore.save(context, link);
            notifyChanged(context);
        }
    }

    public static void onTaskDeleted(Context context, FollowUpTask task) {
        if (task == null) return;
        TaskMessageLinkStore.Link link = TaskMessageLinkStore.find(context, task.id);
        if (link != null) {
            cancelLinkedMessage(context, link, "연결된 일정 삭제");
            TaskMessageLinkStore.remove(context, task.id);
            notifyChanged(context);
        }
    }

    public static void onTaskReopened(Context context, FollowUpTask task) {
        if (task == null) return;
        TaskMessageLinkStore.Link link = TaskMessageLinkStore.find(context, task.id);
        if (link == null) return;
        if (MODE_DAY_BEFORE.equals(link.mode) || MODE_AT_TIME.equals(link.mode)) {
            MessageRecord record = findMessage(context, link.messageId);
            if (record == null || MessageLogStore.STATUS_CANCELLED.equals(record.status)
                    || MessageLogStore.STATUS_FAILED.equals(record.status)
                    || MessageLogStore.STATUS_SKIPPED.equals(record.status)) {
                attach(context, task, link.mode);
            }
        }
    }

    public static void detach(Context context, long taskId, String reason) {
        TaskMessageLinkStore.Link link = TaskMessageLinkStore.find(context, taskId);
        if (link == null) return;
        cancelLinkedMessage(context, link, reason);
        TaskMessageLinkStore.remove(context, taskId);
        notifyChanged(context);
    }

    public static String validateScheduledSend(Context context, long messageId) {
        TaskMessageLinkStore.Link link = TaskMessageLinkStore.findByMessageId(context, messageId);
        if (link == null) return "";
        FollowUpTask pending = findPendingTask(context, link.taskId, link.customerId);
        if (pending == null) {
            return "연결된 일정이 완료되거나 삭제돼 발송하지 않았습니다.";
        }
        TaskMessageLinkStore.Link latest = TaskMessageLinkStore.find(context, link.taskId);
        if (latest == null || latest.messageId != messageId) {
            return "일정에 연결된 새 후속문자로 대체돼 발송하지 않았습니다.";
        }
        return "";
    }

    public static boolean hasAdjustableLink(Context context, long taskId) {
        TaskMessageLinkStore.Link link = TaskMessageLinkStore.find(context, taskId);
        if (link == null) return false;
        return MODE_DAY_BEFORE.equals(link.mode)
                || MODE_AT_TIME.equals(link.mode)
                || MODE_AFTER_COMPLETE.equals(link.mode);
    }

    public static String currentMode(Context context, long taskId) {
        TaskMessageLinkStore.Link link = TaskMessageLinkStore.find(context, taskId);
        return link == null ? MODE_NONE : normalizeMode(link.mode);
    }

    public static String summary(Context context, FollowUpTask task) {
        if (task == null) return "";
        TaskMessageLinkStore.Link link = TaskMessageLinkStore.find(context, task.id);
        if (link == null) return "";
        String prefix = "후속문자 · " + modeLabel(link.mode);
        if (link.messageId <= 0L) return prefix + " · 일정 완료 대기";
        MessageRecord record = findMessage(context, link.messageId);
        if (record == null) return prefix + " · 내역 확인 필요";
        if (MessageLogStore.STATUS_SCHEDULED.equals(record.status)) {
            return prefix + " · " + new SimpleDateFormat("M/d a h:mm", Locale.KOREA)
                    .format(new Date(record.scheduledAt)) + " 발송 예정";
        }
        if (MessageLogStore.STATUS_SENT.equals(record.status)) return prefix + " · 발송 완료";
        if (MessageLogStore.STATUS_SENDING.equals(record.status)) return prefix + " · 발송 중";
        if (MessageLogStore.STATUS_READY.equals(record.status)) return prefix + " · 발송 준비";
        if (MessageLogStore.STATUS_CANCELLED.equals(record.status)) return prefix + " · 취소됨";
        if (MessageLogStore.STATUS_SKIPPED.equals(record.status)) return prefix + " · 건너뜀";
        if (MessageLogStore.STATUS_FAILED.equals(record.status)) return prefix + " · 발송 실패";
        return prefix;
    }

    public static String modeLabel(String mode) {
        switch (normalizeMode(mode)) {
            case MODE_NOW:
                return "지금 안내";
            case MODE_DAY_BEFORE:
                return "일정 하루 전";
            case MODE_AT_TIME:
                return "일정 시간";
            case MODE_AFTER_COMPLETE:
                return "일정 완료 후";
            default:
                return "없음";
        }
    }

    private static Snapshot buildSnapshot(Context context, FollowUpTask task) {
        CallTagDbHelper db = new CallTagDbHelper(context);
        Customer customer;
        try {
            customer = db.findCustomerById(task.customerId);
        } finally {
            db.close();
        }
        if (customer == null) return Snapshot.fail("고객 정보를 찾을 수 없습니다.");
        MessageTemplateStore.Template template = MessageTemplateStore.defaultTemplate(
                context, MessageTemplateStore.PURPOSE_FOLLOW_UP);
        String templateId = template == null ? "" : template.id;
        String templateBody = template == null
                ? MessageAutomationStore.delayedTemplate(context) : template.body;
        CallRecord contextRecord = new CallRecord(
                0L, task.phone, task.customerName, 0,
                System.currentTimeMillis(), 0L);
        MessageTemplateEngine.RenderResult rendered = MessageTemplateEngine.render(
                context, templateBody, customer, contextRecord);
        if (!rendered.isReady()) {
            return Snapshot.fail("후속문자 템플릿의 고객·계정·일정 정보를 확인해주세요.");
        }
        return Snapshot.ok(templateId, rendered.body);
    }

    private static long scheduledAt(String mode, long dueAt, long now) {
        if (MODE_DAY_BEFORE.equals(mode)) return Math.max(now + 1_000L, dueAt - DAY_MS);
        return Math.max(now + 1_000L, dueAt);
    }

    private static void cancelLinkedMessage(Context context, TaskMessageLinkStore.Link link,
                                            String reason) {
        if (link == null || link.messageId <= 0L) return;
        MessageLogStore messages = new MessageLogStore(context);
        try {
            MessageRecord record = messages.find(link.messageId);
            if (record == null) return;
            if (MessageLogStore.STATUS_SCHEDULED.equals(record.status)
                    || MessageLogStore.STATUS_READY.equals(record.status)) {
                MessageScheduler.cancel(context, record.id);
                messages.cancel(record.id, reason == null ? "연결 일정 변경" : reason);
            }
        } finally {
            messages.close();
        }
    }

    private static FollowUpTask findPendingTask(Context context, long taskId, long customerId) {
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            List<FollowUpTask> tasks = customerId > 0L
                    ? db.listTasksForCustomer(customerId) : db.listPendingTasks();
            for (FollowUpTask task : tasks) {
                if (task.id == taskId && !task.isCompleted()) return task;
            }
            return null;
        } finally {
            db.close();
        }
    }

    private static MessageRecord findMessage(Context context, long messageId) {
        if (messageId <= 0L) return null;
        MessageLogStore messages = new MessageLogStore(context);
        try {
            return messages.find(messageId);
        } finally {
            messages.close();
        }
    }

    private static String normalizeMode(String value) {
        if (MODE_NOW.equals(value) || MODE_DAY_BEFORE.equals(value)
                || MODE_AT_TIME.equals(value) || MODE_AFTER_COMPLETE.equals(value)) {
            return value;
        }
        return MODE_NONE;
    }

    private static void notifyChanged(Context context) {
        context.sendBroadcast(new Intent(MessageSectionView.ACTION_CHANGED)
                .setPackage(context.getPackageName()));
    }

    private static final class Snapshot {
        final boolean ready;
        final String templateId;
        final String body;
        final String error;

        private Snapshot(boolean ready, String templateId, String body, String error) {
            this.ready = ready;
            this.templateId = templateId;
            this.body = body;
            this.error = error;
        }

        static Snapshot ok(String templateId, String body) {
            return new Snapshot(true, templateId, body, "");
        }

        static Snapshot fail(String error) {
            return new Snapshot(false, "", "", error);
        }
    }

    public static final class Result {
        public final boolean success;
        public final String message;
        public final long messageId;

        private Result(boolean success, String message, long messageId) {
            this.success = success;
            this.message = message == null ? "" : message;
            this.messageId = messageId;
        }

        static Result ok(String message, long messageId) {
            return new Result(true, message, messageId);
        }

        static Result fail(String message) {
            return new Result(false, message, 0L);
        }
    }
}
