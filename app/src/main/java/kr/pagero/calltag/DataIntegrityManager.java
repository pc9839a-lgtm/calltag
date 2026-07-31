package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DataIntegrityManager {
    public static final String TRIGGER_APP_START = "APP_START";
    public static final String TRIGGER_BOOT = "BOOT_COMPLETED";
    public static final String TRIGGER_PACKAGE_REPLACED = "PACKAGE_REPLACED";
    public static final String TRIGGER_MANUAL = "MANUAL";

    private static final String PREFS = "calltag_data_integrity";
    private static final String KEY_LAST_TIME = "last_time";
    private static final String KEY_LAST_TRIGGER = "last_trigger";
    private static final String KEY_LAST_SUMMARY = "last_summary";
    private static final String KEY_LATE_CALLBACKS = "late_callbacks";
    private static final String KEY_LAST_LATE_CALLBACK_TIME = "last_late_callback_time";

    private static final long TERMINAL_ALARM_LOOKBACK_MS = 24L * 60L * 60L * 1000L;
    private static final int TERMINAL_ALARM_LIMIT = 5_000;
    private static final Object REQUEST_LOCK = new Object();
    private static boolean running;
    private static String pendingTrigger = "";

    private DataIntegrityManager() {}

    public static void recoverAsync(Context context, String trigger) {
        Context app = context.getApplicationContext();
        String safeTrigger = normalizeTrigger(trigger);
        synchronized (REQUEST_LOCK) {
            if (running) {
                pendingTrigger = strongerTrigger(pendingTrigger, safeTrigger);
                return;
            }
            running = true;
        }

        new Thread(() -> {
            String current = safeTrigger;
            while (!current.isEmpty()) {
                recoverNow(app, current);
                synchronized (REQUEST_LOCK) {
                    current = pendingTrigger;
                    pendingTrigger = "";
                    if (current.isEmpty()) running = false;
                }
            }
        }, "calltag-data-integrity").start();
    }

    public static synchronized Result recoverNow(Context context, String trigger) {
        Context app = context.getApplicationContext();
        String safeTrigger = normalizeTrigger(trigger);
        long now = System.currentTimeMillis();
        Result result = new Result(safeTrigger, now);

        CallTagDbHelper crm = new CallTagDbHelper(app);
        MessageGroupStore groups = new MessageGroupStore(app);
        MessageLogStore messages = new MessageLogStore(app);
        CampaignStore campaigns = new CampaignStore(app);
        try {
            Map<Long, JobRef> jobs = loadJobs(messages);
            Set<String> campaignIds = new LinkedHashSet<>();
            Set<String> recipientLinks = new HashSet<>();
            Set<String> changedCampaignIds = new LinkedHashSet<>();

            for (CampaignStore.Campaign campaign : campaigns.list()) {
                campaignIds.add(campaign.id);
                for (CampaignStore.Recipient recipient : campaigns.recipients(campaign.id)) {
                    if (recipient.messageId > 0L) {
                        recipientLinks.add(linkKey(campaign.id, recipient.messageId));
                    }
                    if (recipient.messageId <= 0L) {
                        if (isActive(recipient.status)) {
                            campaigns.replaceRecipientJob(recipient.id, 0L,
                                    MessageLogStore.STATUS_FAILED,
                                    "연결된 문자 작업이 없어 자동 발송하지 않았습니다.",
                                    recipient.scheduledAt);
                            result.activeRecipientsWithoutJobFailed++;
                            changedCampaignIds.add(campaign.id);
                        }
                        continue;
                    }

                    JobRef job = jobs.get(recipient.messageId);
                    if (job == null) {
                        campaigns.replaceRecipientJob(recipient.id, recipient.messageId,
                                MessageLogStore.STATUS_FAILED,
                                "연결된 문자 작업을 찾을 수 없어 자동 발송하지 않았습니다.",
                                recipient.scheduledAt);
                        result.missingRecipientJobsFailed++;
                        changedCampaignIds.add(campaign.id);
                        continue;
                    }
                    if (!campaign.id.equals(job.campaignId)) {
                        campaigns.replaceRecipientJob(recipient.id, recipient.messageId,
                                MessageLogStore.STATUS_FAILED,
                                "문자 작업의 캠페인 연결이 일치하지 않아 자동 발송하지 않았습니다.",
                                recipient.scheduledAt);
                        result.mismatchedCampaignLinksFailed++;
                        changedCampaignIds.add(campaign.id);
                    }
                }
            }

            for (JobRef job : jobs.values()) {
                if (job.campaignId.isEmpty()) continue;
                boolean campaignMissing = !campaignIds.contains(job.campaignId);
                boolean recipientMissing = !recipientLinks.contains(linkKey(job.campaignId, job.id));
                if (!campaignMissing && !recipientMissing) continue;

                MessageScheduler.cancel(app, job.id);
                if (MessageLogStore.STATUS_SCHEDULED.equals(job.status)
                        || MessageLogStore.STATUS_READY.equals(job.status)) {
                    messages.cancel(job.id,
                            campaignMissing
                                    ? "연결된 캠페인이 없어 고아 예약 작업을 취소했습니다."
                                    : "연결된 캠페인 수신자가 없어 고아 예약 작업을 취소했습니다.");
                    result.orphanActiveJobsCancelled++;
                } else if (MessageLogStore.STATUS_SENDING.equals(job.status)) {
                    messages.markFailed(job.id,
                            "캠페인 연결을 확인할 수 없어 자동 재발송하지 않았습니다. 발송내역을 확인해주세요.");
                    result.orphanSendingJobsFailed++;
                } else {
                    result.orphanTerminalAlarmsCancelled++;
                }
                if (!job.campaignId.isEmpty() && campaignIds.contains(job.campaignId)) {
                    changedCampaignIds.add(job.campaignId);
                }
            }

            result.terminalAlarmCancelAttempts = cancelTerminalAlarms(app, messages, now);
            result.staleManualGroupMembersRemoved = removeMissingManualMembers(crm, groups);

            for (CampaignStore.Campaign campaign : campaigns.list()) {
                campaigns.sync(app, campaign.id);
                result.campaignsSynced++;
            }
            for (String campaignId : changedCampaignIds) {
                campaigns.updateCampaignStatus(campaignId);
            }
        } catch (RuntimeException error) {
            result.error = safeError(error);
            DiagnosticEventStore.record(app, "정합성 복구 실패", 0L, result.error);
        } finally {
            campaigns.close();
            messages.close();
            groups.close();
            crm.close();
        }

        saveResult(app, result);
        DiagnosticEventStore.record(app, "정합성 복구 완료", 0L, result.compactSummary());
        return result;
    }

    public static Inspection inspect(Context context) {
        Context app = context.getApplicationContext();
        Inspection inspection = new Inspection();
        CallTagDbHelper crm = new CallTagDbHelper(app);
        MessageGroupStore groups = new MessageGroupStore(app);
        MessageLogStore messages = new MessageLogStore(app);
        CampaignStore campaigns = new CampaignStore(app);
        try {
            Map<Long, JobRef> jobs = loadJobs(messages);
            Set<String> campaignIds = new HashSet<>();
            Set<String> recipientLinks = new HashSet<>();

            for (CampaignStore.Campaign campaign : campaigns.list()) {
                campaignIds.add(campaign.id);
                for (CampaignStore.Recipient recipient : campaigns.recipients(campaign.id)) {
                    if (recipient.messageId <= 0L) {
                        if (isActive(recipient.status)) inspection.activeRecipientsWithoutJob++;
                        continue;
                    }
                    recipientLinks.add(linkKey(campaign.id, recipient.messageId));
                    JobRef job = jobs.get(recipient.messageId);
                    if (job == null) inspection.missingRecipientJobs++;
                    else if (!campaign.id.equals(job.campaignId)) inspection.mismatchedCampaignLinks++;
                }
            }

            for (JobRef job : jobs.values()) {
                if (job.campaignId.isEmpty()) continue;
                if (!campaignIds.contains(job.campaignId)
                        || !recipientLinks.contains(linkKey(job.campaignId, job.id))) {
                    inspection.orphanCampaignJobs++;
                }
            }
            inspection.staleManualGroupMembers = countMissingManualMembers(crm, groups);
            inspection.lateCallbacksIgnored = prefs(app).getInt(KEY_LATE_CALLBACKS, 0);
        } finally {
            campaigns.close();
            messages.close();
            groups.close();
            crm.close();
        }
        return inspection;
    }

    public static void recordLateCallback(Context context, long messageId, String currentStatus) {
        Context app = context.getApplicationContext();
        SharedPreferences preferences = prefs(app);
        int count = Math.max(0, preferences.getInt(KEY_LATE_CALLBACKS, 0)) + 1;
        preferences.edit()
                .putInt(KEY_LATE_CALLBACKS, count)
                .putLong(KEY_LAST_LATE_CALLBACK_TIME, System.currentTimeMillis())
                .apply();
        DiagnosticEventStore.record(app, "늦은 SMS 콜백 무시", messageId,
                "현재 상태 " + clean(currentStatus));
    }

    public static String lastSummary(Context context) {
        SharedPreferences preferences = prefs(context);
        long time = preferences.getLong(KEY_LAST_TIME, 0L);
        String trigger = preferences.getString(KEY_LAST_TRIGGER, "");
        String summary = preferences.getString(KEY_LAST_SUMMARY, "");
        if (summary == null || summary.trim().isEmpty()) {
            return "아직 데이터 정합성 복구 기록이 없습니다.";
        }
        String formatted = time <= 0L ? "시각 없음"
                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
                .format(new Date(time));
        return formatted + " · " + triggerLabel(trigger) + "\n" + summary;
    }

    private static int cancelTerminalAlarms(Context context, MessageLogStore messages, long now) {
        int count = 0;
        String selection = "status IN (?,?,?,?) AND scheduled_at>=?";
        String[] args = new String[]{
                MessageLogStore.STATUS_SENT,
                MessageLogStore.STATUS_FAILED,
                MessageLogStore.STATUS_SKIPPED,
                MessageLogStore.STATUS_CANCELLED,
                String.valueOf(now - TERMINAL_ALARM_LOOKBACK_MS)
        };
        try (Cursor cursor = messages.getReadableDatabase().query(
                "message_jobs", new String[]{"id"}, selection, args,
                null, null, "id DESC", String.valueOf(TERMINAL_ALARM_LIMIT))) {
            while (cursor.moveToNext()) {
                MessageScheduler.cancel(context, cursor.getLong(0));
                count++;
            }
        }
        return count;
    }

    private static int removeMissingManualMembers(CallTagDbHelper crm, MessageGroupStore groups) {
        Set<Long> customerIds = customerIds(crm);
        List<Long> staleRows = new ArrayList<>();
        try (Cursor cursor = groups.getReadableDatabase().query(
                "message_group_members", new String[]{"rowid", "customer_id"},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                if (!customerIds.contains(cursor.getLong(1))) staleRows.add(cursor.getLong(0));
            }
        }
        for (Long rowId : staleRows) {
            groups.getWritableDatabase().delete("message_group_members", "rowid=?",
                    new String[]{String.valueOf(rowId)});
        }
        return staleRows.size();
    }

    private static int countMissingManualMembers(CallTagDbHelper crm, MessageGroupStore groups) {
        Set<Long> customerIds = customerIds(crm);
        int count = 0;
        try (Cursor cursor = groups.getReadableDatabase().query(
                "message_group_members", new String[]{"customer_id"},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                if (!customerIds.contains(cursor.getLong(0))) count++;
            }
        }
        return count;
    }

    private static Set<Long> customerIds(CallTagDbHelper crm) {
        Set<Long> ids = new HashSet<>();
        for (Customer customer : crm.listCustomers(null)) ids.add(customer.id);
        return ids;
    }

    private static Map<Long, JobRef> loadJobs(MessageLogStore messages) {
        Map<Long, JobRef> jobs = new HashMap<>();
        try (Cursor cursor = messages.getReadableDatabase().query(
                "message_jobs", new String[]{"id", "campaign_id", "status"},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                jobs.put(id, new JobRef(id, cursor.getString(1), cursor.getString(2)));
            }
        }
        return jobs;
    }

    private static boolean isActive(String status) {
        return MessageLogStore.STATUS_SCHEDULED.equals(status)
                || MessageLogStore.STATUS_READY.equals(status)
                || MessageLogStore.STATUS_SENDING.equals(status);
    }

    private static String linkKey(String campaignId, long messageId) {
        return clean(campaignId) + "#" + messageId;
    }

    private static void saveResult(Context context, Result result) {
        prefs(context).edit()
                .putLong(KEY_LAST_TIME, result.startedAt)
                .putString(KEY_LAST_TRIGGER, result.trigger)
                .putString(KEY_LAST_SUMMARY, result.summary())
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String strongerTrigger(String current, String incoming) {
        return triggerPriority(incoming) >= triggerPriority(current) ? incoming : current;
    }

    private static int triggerPriority(String trigger) {
        if (TRIGGER_BOOT.equals(trigger)) return 4;
        if (TRIGGER_PACKAGE_REPLACED.equals(trigger)) return 3;
        if (TRIGGER_MANUAL.equals(trigger)) return 2;
        return 1;
    }

    private static String normalizeTrigger(String trigger) {
        String value = clean(trigger);
        if (TRIGGER_BOOT.equals(value)
                || TRIGGER_PACKAGE_REPLACED.equals(value)
                || TRIGGER_MANUAL.equals(value)) return value;
        return TRIGGER_APP_START;
    }

    private static String triggerLabel(String trigger) {
        if (TRIGGER_BOOT.equals(trigger)) return "재부팅 후 자동 복구";
        if (TRIGGER_PACKAGE_REPLACED.equals(trigger)) return "앱 업데이트 후 자동 복구";
        if (TRIGGER_MANUAL.equals(trigger)) return "수동 정합성 복구";
        return "앱 시작 자동 복구";
    }

    private static String safeError(Throwable error) {
        String value = error == null ? "" : error.getMessage();
        if (value == null || value.trim().isEmpty()) value = "알 수 없는 정합성 복구 오류";
        value = value.replace('\n', ' ').replace('\r', ' ').trim();
        return value.length() > 140 ? value.substring(0, 140) : value;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class JobRef {
        final long id;
        final String campaignId;
        final String status;

        JobRef(long id, String campaignId, String status) {
            this.id = id;
            this.campaignId = clean(campaignId);
            this.status = clean(status);
        }
    }

    public static final class Inspection {
        public int orphanCampaignJobs;
        public int missingRecipientJobs;
        public int activeRecipientsWithoutJob;
        public int mismatchedCampaignLinks;
        public int staleManualGroupMembers;
        public int lateCallbacksIgnored;

        public int warningCount() {
            int count = 0;
            if (orphanCampaignJobs > 0) count++;
            if (missingRecipientJobs > 0) count++;
            if (activeRecipientsWithoutJob > 0) count++;
            if (mismatchedCampaignLinks > 0) count++;
            if (staleManualGroupMembers > 0) count++;
            return count;
        }
    }

    public static final class Result {
        public final String trigger;
        public final long startedAt;
        public int missingRecipientJobsFailed;
        public int activeRecipientsWithoutJobFailed;
        public int mismatchedCampaignLinksFailed;
        public int orphanActiveJobsCancelled;
        public int orphanSendingJobsFailed;
        public int orphanTerminalAlarmsCancelled;
        public int terminalAlarmCancelAttempts;
        public int staleManualGroupMembersRemoved;
        public int campaignsSynced;
        public String error = "";

        Result(String trigger, long startedAt) {
            this.trigger = trigger;
            this.startedAt = startedAt;
        }

        public String compactSummary() {
            if (!error.isEmpty()) return "정합성 복구 오류";
            return "누락수신자 " + (missingRecipientJobsFailed + activeRecipientsWithoutJobFailed)
                    + " · 고아작업 " + (orphanActiveJobsCancelled + orphanSendingJobsFailed)
                    + " · 그룹참조 " + staleManualGroupMembersRemoved;
        }

        public String summary() {
            String value = "연결 작업 누락 수신자 실패 전환 " + missingRecipientJobsFailed + "건"
                    + " · 작업 없는 진행 수신자 실패 전환 " + activeRecipientsWithoutJobFailed + "건"
                    + " · 캠페인 연결 불일치 수신자 실패 전환 " + mismatchedCampaignLinksFailed + "건"
                    + " · 고아 예약 작업 취소 " + orphanActiveJobsCancelled + "건"
                    + " · 고아 발송 중 작업 실패 전환 " + orphanSendingJobsFailed + "건"
                    + " · 고아 최종 작업 알람 취소 " + orphanTerminalAlarmsCancelled + "건"
                    + " · 최종 상태 알람 정리 시도 " + terminalAlarmCancelAttempts + "건"
                    + " · 삭제 고객 그룹 참조 제거 " + staleManualGroupMembersRemoved + "건"
                    + " · 캠페인 동기화 " + campaignsSynced + "개";
            return error.isEmpty() ? value : value + " · 오류: " + error;
        }
    }
}
