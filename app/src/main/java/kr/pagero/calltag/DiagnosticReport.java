package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class DiagnosticReport {
    private static final long PAST_DUE_GRACE_MS = 60_000L;

    private DiagnosticReport() {}

    public static Snapshot collect(Context context) {
        Context app = context.getApplicationContext();
        long now = System.currentTimeMillis();
        StringBuilder report = new StringBuilder();
        List<String> warnings = new ArrayList<>();

        append(report, "콜태그 앱 진단");
        append(report, "생성 시각: " + formatTime(now));
        append(report, "앱 버전: " + versionLabel(app));
        append(report, "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        append(report, "단말: " + clean(Build.MANUFACTURER) + " " + clean(Build.MODEL));

        append(report, "");
        append(report, "[필수 권한]");
        permission(report, warnings, app, Manifest.permission.SEND_SMS, "문자 발송", true);
        permission(report, warnings, app, Manifest.permission.READ_PHONE_STATE, "전화 상태", true);
        permission(report, warnings, app, Manifest.permission.READ_PHONE_NUMBERS, "전화번호", true);
        permission(report, warnings, app, Manifest.permission.READ_CALL_LOG, "통화기록", true);
        permission(report, warnings, app, Manifest.permission.READ_CONTACTS, "연락처 읽기", true);
        permission(report, warnings, app, Manifest.permission.WRITE_CONTACTS, "연락처 수정", true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission(report, warnings, app, Manifest.permission.POST_NOTIFICATIONS, "알림", true);
        } else {
            append(report, "알림: OS 별도 권한 불필요");
        }
        boolean overlay = Settings.canDrawOverlays(app);
        append(report, "다른 앱 위 표시: " + yesNo(overlay));
        if (!overlay) warnings.add("다른 앱 위 표시 권한이 꺼져 있습니다.");

        append(report, "");
        append(report, "[SIM·회선]");
        List<SimProfileManager.Profile> profiles = SimProfileManager.activeProfiles(app);
        int selectedId = MessageAutomationStore.selectedSubscriptionId(app);
        boolean selectedValid = false;
        append(report, "활성 회선 수: " + profiles.size());
        append(report, "선택 회선 ID: " + selectedId);
        for (SimProfileManager.Profile profile : profiles) {
            boolean selected = profile.subscriptionId == selectedId;
            selectedValid |= selected;
            append(report, (selected ? "선택 · " : "대기 · ") + profile.label()
                    + " · ID " + profile.subscriptionId);
        }
        if (profiles.isEmpty()) {
            warnings.add("활성 SIM 정보를 확인하지 못했습니다. 권한 또는 SIM 상태를 확인해주세요.");
        } else if (!SubscriptionManager.isValidSubscriptionId(selectedId) || !selectedValid) {
            warnings.add("선택한 문자 회선이 현재 활성 회선과 일치하지 않습니다.");
        }

        CallTagDbHelper crm = new CallTagDbHelper(app);
        MessageGroupStore groups = new MessageGroupStore(app);
        MessageLogStore messages = new MessageLogStore(app);
        CampaignStore campaigns = new CampaignStore(app);
        try {
            append(report, "");
            append(report, "[고객·일정]");
            append(report, "고객: " + crm.countCustomers() + "명");
            append(report, "미완료 일정: " + crm.countPendingTasks() + "건");
            append(report, "문자 그룹: " + groups.list().size() + "개");

            int scheduled = messages.countByStatus(MessageLogStore.STATUS_SCHEDULED);
            int ready = messages.countByStatus(MessageLogStore.STATUS_READY);
            int sending = messages.countByStatus(MessageLogStore.STATUS_SENDING);
            int sent = messages.countByStatus(MessageLogStore.STATUS_SENT);
            int failed = messages.countByStatus(MessageLogStore.STATUS_FAILED);
            int skipped = messages.countByStatus(MessageLogStore.STATUS_SKIPPED);
            int cancelled = messages.countByStatus(MessageLogStore.STATUS_CANCELLED);
            int overdue = 0;
            for (MessageRecord record : messages.listScheduled()) {
                if (record.scheduledAt + PAST_DUE_GRACE_MS < now) overdue++;
            }

            append(report, "");
            append(report, "[문자 작업]");
            append(report, "전체: " + (scheduled + ready + sending + sent + failed + skipped + cancelled) + "건");
            append(report, "예약: " + scheduled + " · 준비: " + ready + " · 발송 중: " + sending);
            append(report, "완료: " + sent + " · 실패: " + failed + " · 건너뜀: " + skipped
                    + " · 취소: " + cancelled);
            append(report, "예약시각 경과: " + overdue + "건");
            if (overdue > 0) warnings.add("예약시각이 지났지만 예약 상태인 문자 작업이 " + overdue + "건 있습니다.");
            if (sending > 0) warnings.add("발송 중 상태 작업이 있습니다. 실제 단말에서 상태 갱신 여부를 확인해주세요.");

            int campaignCount = 0;
            int recipientCount = 0;
            int linkedMissing = 0;
            int stateMismatch = 0;
            int activeWithoutJob = 0;
            for (CampaignStore.Campaign campaign : campaigns.list()) {
                campaignCount++;
                for (CampaignStore.Recipient recipient : campaigns.recipients(campaign.id)) {
                    recipientCount++;
                    if (recipient.messageId <= 0L) {
                        if (isActive(recipient.status)) activeWithoutJob++;
                        continue;
                    }
                    MessageRecord record = messages.find(recipient.messageId);
                    if (record == null) {
                        linkedMissing++;
                        continue;
                    }
                    if (!same(record.status, recipient.status)
                            || !same(clean(record.error), clean(recipient.reason))) {
                        stateMismatch++;
                    }
                }
            }

            append(report, "");
            append(report, "[단체문자 캠페인]");
            append(report, "캠페인: " + campaignCount + "개 · 수신자: " + recipientCount + "명");
            append(report, "상태 불일치: " + stateMismatch + "건");
            append(report, "연결 문자 작업 누락: " + linkedMissing + "건");
            append(report, "작업 없는 진행 상태: " + activeWithoutJob + "건");
            if (stateMismatch > 0) warnings.add("캠페인 수신자 상태와 문자 작업 상태가 다른 항목이 " + stateMismatch + "건 있습니다.");
            if (linkedMissing > 0) warnings.add("캠페인이 참조하는 문자 작업을 찾지 못한 항목이 " + linkedMissing + "건 있습니다.");
            if (activeWithoutJob > 0) warnings.add("문자 작업 없이 진행 상태인 캠페인 수신자가 " + activeWithoutJob + "명 있습니다.");

            List<MessageTemplateStore.Template> templates = MessageTemplateStore.list(app, "", "");
            int imageTemplates = 0;
            int missingImages = 0;
            for (MessageTemplateStore.Template template : templates) {
                if (clean(template.imageRef).isEmpty()) continue;
                imageTemplates++;
                if (!MessageAttachmentStore.exists(app, template.imageRef)) missingImages++;
            }
            append(report, "");
            append(report, "[템플릿 이미지]");
            append(report, "전체 템플릿: " + templates.size() + "개 · 이미지 템플릿: " + imageTemplates + "개");
            append(report, "이미지 파일 누락: " + missingImages + "건");
            if (missingImages > 0) warnings.add("저장된 이미지 템플릿 중 파일이 없는 항목이 " + missingImages + "건 있습니다.");
        } finally {
            campaigns.close();
            messages.close();
            groups.close();
            crm.close();
        }

        DataIntegrityManager.Inspection integrity = DataIntegrityManager.inspect(app);
        append(report, "");
        append(report, "[데이터 정합성]");
        append(report, "고아 캠페인 문자 작업: " + integrity.orphanCampaignJobs + "건");
        append(report, "연결 작업 누락 수신자: " + integrity.missingRecipientJobs + "건");
        append(report, "작업 없는 진행 수신자: " + integrity.activeRecipientsWithoutJob + "건");
        append(report, "캠페인 연결 불일치: " + integrity.mismatchedCampaignLinks + "건");
        append(report, "삭제 고객 그룹 참조: " + integrity.staleManualGroupMembers + "건");
        append(report, "늦은 SMS 콜백 누적 무시: " + integrity.lateCallbacksIgnored + "건");
        append(report, "마지막 복구:\n" + DataIntegrityManager.lastSummary(app));
        if (integrity.orphanCampaignJobs > 0) warnings.add("캠페인 또는 수신자 연결이 없는 문자 작업이 있습니다.");
        if (integrity.missingRecipientJobs > 0) warnings.add("문자 작업을 찾을 수 없는 캠페인 수신자가 있습니다.");
        if (integrity.activeRecipientsWithoutJob > 0) warnings.add("문자 작업 없이 진행 상태인 캠페인 수신자가 있습니다.");
        if (integrity.mismatchedCampaignLinks > 0) warnings.add("캠페인과 문자 작업의 연결 ID가 일치하지 않는 항목이 있습니다.");
        if (integrity.staleManualGroupMembers > 0) warnings.add("삭제된 고객 ID가 수동 그룹에 남아 있습니다.");

        append(report, "");
        append(report, "[판정]");
        if (warnings.isEmpty()) {
            append(report, "자동 점검에서 즉시 확인할 문제를 찾지 못했습니다.");
        } else {
            append(report, "확인 필요: " + warnings.size() + "개");
            for (int i = 0; i < warnings.size(); i++) {
                append(report, (i + 1) + ". " + warnings.get(i));
            }
        }
        append(report, "");
        append(report, "※ 이 정보에는 고객명, 전화번호, 문자 본문이 포함되지 않습니다.");
        append(report, "※ 자동 점검은 실제 SIM 문자 발송 성공을 대신하지 않습니다.");
        return new Snapshot(report.toString(), warnings.size());
    }

    public static RepairResult reconcileCampaigns(Context context) {
        DataIntegrityManager.Result result = DataIntegrityManager.recoverNow(
                context, DataIntegrityManager.TRIGGER_MANUAL);
        DataIntegrityManager.Inspection remaining = DataIntegrityManager.inspect(context);
        return new RepairResult(result, remaining);
    }

    private static void permission(StringBuilder report, List<String> warnings,
                                   Context context, String permission, String label,
                                   boolean required) {
        boolean granted = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        append(report, label + ": " + yesNo(granted));
        if (required && !granted) warnings.add(label + " 권한이 없습니다.");
    }

    private static boolean isActive(String status) {
        return MessageLogStore.STATUS_SCHEDULED.equals(status)
                || MessageLogStore.STATUS_READY.equals(status)
                || MessageLogStore.STATUS_SENDING.equals(status);
    }

    private static String versionLabel(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? info.getLongVersionCode() : info.versionCode;
            return clean(info.versionName) + " (" + code + ")";
        } catch (PackageManager.NameNotFoundException ignored) {
            return "확인 불가";
        }
    }

    private static String formatTime(long value) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date(value));
    }

    private static String yesNo(boolean value) {
        return value ? "정상" : "확인 필요";
    }

    private static boolean same(String first, String second) {
        return clean(first).equals(clean(second));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static void append(StringBuilder builder, String value) {
        if (builder.length() > 0) builder.append('\n');
        builder.append(value == null ? "" : value);
    }

    public static final class Snapshot {
        public final String text;
        public final int warningCount;

        Snapshot(String text, int warningCount) {
            this.text = text;
            this.warningCount = warningCount;
        }
    }

    public static final class RepairResult {
        private final DataIntegrityManager.Result result;
        private final DataIntegrityManager.Inspection remaining;

        RepairResult(DataIntegrityManager.Result result,
                     DataIntegrityManager.Inspection remaining) {
            this.result = result;
            this.remaining = remaining;
        }

        public String summary() {
            int remainingCount = remaining.orphanCampaignJobs
                    + remaining.missingRecipientJobs
                    + remaining.activeRecipientsWithoutJob
                    + remaining.mismatchedCampaignLinks
                    + remaining.staleManualGroupMembers;
            return result.summary() + " · 남은 정합성 항목 " + remainingCount + "건";
        }
    }
}
