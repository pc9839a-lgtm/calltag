package kr.pagero.calltag;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MessageRecoveryManager {
    public static final String TRIGGER_APP_START = "APP_START";
    public static final String TRIGGER_BOOT = "BOOT_COMPLETED";
    public static final String TRIGGER_PACKAGE_REPLACED = "PACKAGE_REPLACED";
    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_LEGACY = "LEGACY_RESCHEDULE";

    private static final long OVERDUE_GRACE_MS = 30L * 60L * 1000L;
    private static final long RECOVERY_START_DELAY_MS = 5_000L;
    private static final long RECOVERY_SPACING_MS = 5_000L;
    private static final long SENDING_STALE_MS = 10L * 60L * 1000L;

    private static final String PREFS = "calltag_message_recovery";
    private static final String KEY_LAST_SUMMARY = "last_summary";
    private static final String KEY_LAST_TIME = "last_time";
    private static final String KEY_LAST_TRIGGER = "last_trigger";

    private static final Object REQUEST_LOCK = new Object();
    private static boolean running;
    private static String pendingTrigger = "";

    private MessageRecoveryManager() {}

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
        }, "calltag-message-recovery").start();
    }

    public static synchronized Result recoverNow(Context context, String trigger) {
        Context app = context.getApplicationContext();
        String safeTrigger = normalizeTrigger(trigger);
        long now = System.currentTimeMillis();
        Result result = new Result(safeTrigger, now);

        if (!FeatureEntitlementStore.hasMessageAccess(app)) {
            result.skippedByEntitlement = true;
            saveResult(app, result);
            DiagnosticEventStore.record(app, "예약 복구 보류", 0L, "문자 이용 권한 없음");
            return result;
        }

        MessageLogStore store = new MessageLogStore(app);
        CampaignStore campaignStore = new CampaignStore(app);
        Set<String> changedCampaignIds = new LinkedHashSet<>();
        try {
            List<Candidate> candidates = loadCandidates(store);
            int backlogIndex = 0;
            for (Candidate candidate : candidates) {
                if (isPausedCampaign(campaignStore, candidate.campaignId)
                        && !MessageLogStore.STATUS_SENDING.equals(candidate.status)) {
                    MessageScheduler.cancel(app, candidate.id);
                    if (MessageLogStore.STATUS_READY.equals(candidate.status)) {
                        resetReadyToScheduled(store, candidate.id, now);
                        addCampaign(changedCampaignIds, candidate.campaignId);
                    }
                    result.pausedPreserved++;
                    DiagnosticEventStore.record(app, "예약 복구 보류", candidate.id,
                            "일시정지 캠페인");
                    continue;
                }

                if (MessageLogStore.STATUS_SENDING.equals(candidate.status)) {
                    boolean definitiveRestart = TRIGGER_BOOT.equals(safeTrigger)
                            || TRIGGER_PACKAGE_REPLACED.equals(safeTrigger);
                    boolean stale = now - candidate.updatedAt >= SENDING_STALE_MS;
                    if (definitiveRestart || stale) {
                        store.markFailed(candidate.id,
                                "발송 결과를 확인할 수 없어 중복발송 방지를 위해 자동 재발송하지 않았습니다. 발송내역에서 확인 후 필요한 경우 다시 보내주세요.");
                        MessageScheduler.cancel(app, candidate.id);
                        result.sendingMarkedFailed++;
                        addCampaign(changedCampaignIds, candidate.campaignId);
                        DiagnosticEventStore.record(app, "발송 상태 복구", candidate.id,
                                "발송 결과 불명확 · 자동 재발송 차단");
                    } else {
                        result.sendingLeftPending++;
                    }
                    continue;
                }

                boolean ready = MessageLogStore.STATUS_READY.equals(candidate.status);
                long overdue = Math.max(0L, now - candidate.scheduledAt);
                if (candidate.scheduledAt < now && overdue > OVERDUE_GRACE_MS) {
                    MessageScheduler.cancel(app, candidate.id);
                    store.markSkipped(candidate.id,
                            "예약 시간이 30분 이상 지나 늦은 오발송을 막기 위해 자동 발송하지 않았습니다. 발송내역에서 확인 후 필요한 경우 다시 보내주세요.");
                    result.overdueSkipped++;
                    addCampaign(changedCampaignIds, candidate.campaignId);
                    DiagnosticEventStore.record(app, "예약 복구 건너뜀", candidate.id,
                            "30분 초과 지연");
                    continue;
                }

                if (ready) {
                    resetReadyToScheduled(store, candidate.id, now);
                    result.readyRecovered++;
                    addCampaign(changedCampaignIds, candidate.campaignId);
                }

                if (candidate.scheduledAt >= now) {
                    MessageScheduler.schedule(app, candidate.id, candidate.scheduledAt);
                    result.futureRescheduled++;
                } else {
                    long recoveredAt = now + RECOVERY_START_DELAY_MS
                            + (backlogIndex * RECOVERY_SPACING_MS);
                    backlogIndex++;
                    MessageScheduler.schedule(app, candidate.id, recoveredAt);
                    result.overdueRecovered++;
                    DiagnosticEventStore.record(app, "예약 복구", candidate.id,
                            "30분 이내 지연 · 순차 재등록");
                }
            }
        } catch (RuntimeException error) {
            result.error = safeError(error);
            DiagnosticEventStore.record(app, "예약 복구 실패", 0L, result.error);
        } finally {
            campaignStore.close();
            store.close();
        }

        if (!changedCampaignIds.isEmpty()) {
            CampaignStore campaigns = new CampaignStore(app);
            try {
                for (String campaignId : changedCampaignIds) {
                    campaigns.sync(app, campaignId);
                    result.campaignsSynced++;
                }
            } finally {
                campaigns.close();
            }
        }

        saveResult(app, result);
        DiagnosticEventStore.record(app, "예약 복구 완료", 0L, result.compactSummary());
        return result;
    }

    public static String lastSummary(Context context) {
        SharedPreferences prefs = prefs(context);
        String summary = prefs.getString(KEY_LAST_SUMMARY, "");
        long time = prefs.getLong(KEY_LAST_TIME, 0L);
        String trigger = prefs.getString(KEY_LAST_TRIGGER, "");
        if (summary == null || summary.trim().isEmpty()) return "아직 예약 복구 기록이 없습니다.";
        String formatted = time <= 0L ? "시각 없음"
                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
                .format(new Date(time));
        return formatted + " · " + triggerLabel(trigger) + "\n" + summary;
    }

    public static long overdueGraceMs() {
        return OVERDUE_GRACE_MS;
    }

    private static boolean isPausedCampaign(CampaignStore store, String campaignId) {
        String value = campaignId == null ? "" : campaignId.trim();
        if (value.isEmpty()) return false;
        CampaignStore.Campaign campaign = store.find(value);
        return campaign != null && CampaignStore.STATUS_PAUSED.equals(campaign.status);
    }

    private static List<Candidate> loadCandidates(MessageLogStore store) {
        List<Candidate> rows = new ArrayList<>();
        String selection = "status IN (?,?,?)";
        String[] args = new String[]{
                MessageLogStore.STATUS_SCHEDULED,
                MessageLogStore.STATUS_READY,
                MessageLogStore.STATUS_SENDING
        };
        try (Cursor cursor = store.getReadableDatabase().query(
                "message_jobs",
                new String[]{"id", "status", "scheduled_at", "updated_at", "campaign_id"},
                selection, args, null, null, "scheduled_at ASC,id ASC")) {
            while (cursor.moveToNext()) {
                rows.add(new Candidate(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("scheduled_at")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                        cursor.getString(cursor.getColumnIndexOrThrow("campaign_id"))));
            }
        }
        return rows;
    }

    private static void resetReadyToScheduled(MessageLogStore store, long id, long now) {
        ContentValues values = new ContentValues();
        values.put("status", MessageLogStore.STATUS_SCHEDULED);
        values.put("error", "앱 종료 중 멈춘 발송 준비 상태를 안전하게 복구했습니다.");
        values.put("updated_at", now);
        store.getWritableDatabase().update("message_jobs", values, "id=?",
                new String[]{String.valueOf(id)});
    }

    private static void addCampaign(Set<String> campaigns, String campaignId) {
        String value = campaignId == null ? "" : campaignId.trim();
        if (!value.isEmpty()) campaigns.add(value);
    }

    private static void saveResult(Context context, Result result) {
        prefs(context).edit()
                .putString(KEY_LAST_SUMMARY, result.summary())
                .putLong(KEY_LAST_TIME, result.startedAt)
                .putString(KEY_LAST_TRIGGER, result.trigger)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String strongerTrigger(String current, String incoming) {
        return triggerPriority(incoming) >= triggerPriority(current) ? incoming : current;
    }

    private static int triggerPriority(String trigger) {
        if (TRIGGER_BOOT.equals(trigger)) return 5;
        if (TRIGGER_PACKAGE_REPLACED.equals(trigger)) return 4;
        if (TRIGGER_MANUAL.equals(trigger)) return 3;
        if (TRIGGER_APP_START.equals(trigger)) return 2;
        return 1;
    }

    private static String normalizeTrigger(String trigger) {
        String value = trigger == null ? "" : trigger.trim();
        if (TRIGGER_BOOT.equals(value)
                || TRIGGER_PACKAGE_REPLACED.equals(value)
                || TRIGGER_MANUAL.equals(value)
                || TRIGGER_LEGACY.equals(value)) return value;
        return TRIGGER_APP_START;
    }

    private static String triggerLabel(String trigger) {
        if (TRIGGER_BOOT.equals(trigger)) return "재부팅 복구";
        if (TRIGGER_PACKAGE_REPLACED.equals(trigger)) return "앱 업데이트 복구";
        if (TRIGGER_MANUAL.equals(trigger)) return "수동 복구";
        if (TRIGGER_LEGACY.equals(trigger)) return "호환 복구";
        return "앱 시작 복구";
    }

    private static String safeError(Throwable error) {
        String value = error == null ? "" : error.getMessage();
        if (value == null || value.trim().isEmpty()) value = "알 수 없는 복구 오류";
        value = value.replace('\n', ' ').replace('\r', ' ').trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private static final class Candidate {
        final long id;
        final String status;
        final long scheduledAt;
        final long updatedAt;
        final String campaignId;

        Candidate(long id, String status, long scheduledAt, long updatedAt,
                  String campaignId) {
            this.id = id;
            this.status = status == null ? "" : status;
            this.scheduledAt = scheduledAt;
            this.updatedAt = updatedAt;
            this.campaignId = campaignId == null ? "" : campaignId;
        }
    }

    public static final class Result {
        public final String trigger;
        public final long startedAt;
        public int futureRescheduled;
        public int overdueRecovered;
        public int overdueSkipped;
        public int readyRecovered;
        public int pausedPreserved;
        public int sendingMarkedFailed;
        public int sendingLeftPending;
        public int campaignsSynced;
        public boolean skippedByEntitlement;
        public String error = "";

        Result(String trigger, long startedAt) {
            this.trigger = trigger;
            this.startedAt = startedAt;
        }

        public String compactSummary() {
            if (skippedByEntitlement) return "문자 이용 권한 없음";
            if (!error.isEmpty()) return "복구 오류 발생";
            return "미래 " + futureRescheduled
                    + " · 지연복구 " + overdueRecovered
                    + " · 지연차단 " + overdueSkipped
                    + " · 일시정지보존 " + pausedPreserved
                    + " · 발송불명 " + sendingMarkedFailed;
        }

        public String summary() {
            StringBuilder value = new StringBuilder();
            value.append("미래 예약 재등록 ").append(futureRescheduled).append("건")
                    .append(" · 30분 이내 지연 복구 ").append(overdueRecovered).append("건")
                    .append(" · 30분 초과 차단 ").append(overdueSkipped).append("건")
                    .append(" · 준비 상태 복구 ").append(readyRecovered).append("건")
                    .append(" · 일시정지 보존 ").append(pausedPreserved).append("건")
                    .append(" · 발송 결과 불명확 실패 전환 ").append(sendingMarkedFailed).append("건")
                    .append(" · 확인 대기 발송 중 ").append(sendingLeftPending).append("건")
                    .append(" · 캠페인 동기화 ").append(campaignsSynced).append("개");
            if (skippedByEntitlement) value.append(" · 문자 이용 권한 없어 보류");
            if (!error.isEmpty()) value.append(" · 오류: ").append(error);
            return value.toString();
        }
    }
}
