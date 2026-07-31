package kr.pagero.calltag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MessageDedupeEngine {
    public static final String PURPOSE_CALL_IMMEDIATE = "CALL_IMMEDIATE";
    public static final String PURPOSE_FOLLOW_UP = "FOLLOW_UP";
    public static final String PURPOSE_CAMPAIGN = "CAMPAIGN";
    public static final String PURPOSE_MANUAL = "MANUAL";

    public static final long SCHEDULE_COLLISION_MS = 15L * 60L * 1000L;
    private static final long MIN_BUCKET_MS = 60L * 60L * 1000L;
    private static final String DUPLICATE_PREFIX = "중복 발송 차단";

    private MessageDedupeEngine() {}

    public static Metadata metadata(long callLogId, long scheduleId, String campaignId,
                                    String phone, String body, String triggerType,
                                    long scheduledAt, long duplicateWindowMs) {
        String normalizedPhone = PhoneNumberNormalizer.normalize(phone);
        String normalizedBody = normalizeBody(body);
        String bodyHash = sha256(normalizedBody);
        String purpose = purposeType(triggerType, callLogId, scheduleId, campaignId);
        long safeWindow = Math.max(MIN_BUCKET_MS, duplicateWindowMs);
        long bucket = Math.max(0L, scheduledAt) / safeWindow;

        String contextKey;
        if (callLogId > 0L) {
            contextKey = "call:" + callLogId + ":" + purpose;
        } else if (scheduleId > 0L) {
            contextKey = "schedule:" + scheduleId + ":" + purpose;
        } else if (!safe(campaignId).trim().isEmpty()) {
            contextKey = "campaign:" + safe(campaignId).trim() + ":" + normalizedPhone;
        } else {
            contextKey = "recent:" + normalizedPhone + ":" + purpose + ":" + bodyHash
                    + ":" + bucket;
        }
        return new Metadata(normalizedPhone, normalizedBody, bodyHash, purpose,
                sha256(contextKey), safeWindow);
    }

    public static String forceActiveKey(String idempotencyKey, long now) {
        return safe(idempotencyKey) + ":force:" + now + ":" + Long.toHexString(System.nanoTime());
    }

    public static String duplicateReason(String reason, long duplicateId,
                                         long duplicateTime, String duplicateStatus) {
        String time = duplicateTime <= 0L ? "시각 확인 불가"
                : new SimpleDateFormat("M/d a h:mm", Locale.KOREA)
                .format(new Date(duplicateTime));
        return DUPLICATE_PREFIX + " · " + safe(reason)
                + " · 기존 내역 #" + duplicateId
                + " · " + time
                + " · " + statusLabel(duplicateStatus);
    }

    public static boolean isDuplicateReason(String value) {
        return safe(value).startsWith(DUPLICATE_PREFIX);
    }

    public static String normalizeBody(String body) {
        return safe(body)
                .replace('\r', '\n')
                .replaceAll("\\n+", "\n")
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .trim();
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(safe(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte valueByte : bytes) result.append(String.format(Locale.US, "%02x", valueByte));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            return Integer.toHexString(safe(value).hashCode());
        }
    }

    public static String purposeType(String triggerType, long callLogId,
                                     long scheduleId, String campaignId) {
        if (!safe(campaignId).trim().isEmpty()) return PURPOSE_CAMPAIGN;
        if (scheduleId > 0L || MessageAutomationManager.TRIGGER_DELAYED.equals(triggerType)) {
            return PURPOSE_FOLLOW_UP;
        }
        if (callLogId > 0L) return PURPOSE_CALL_IMMEDIATE;
        if (MessageAutomationManager.TRIGGER_MANUAL.equals(triggerType)) return PURPOSE_MANUAL;
        return safe(triggerType).trim().isEmpty() ? PURPOSE_MANUAL : safe(triggerType).trim();
    }

    public static String statusLabel(String status) {
        if (MessageLogStore.STATUS_SCHEDULED.equals(status)) return "발송 예정";
        if (MessageLogStore.STATUS_READY.equals(status)) return "발송 준비";
        if (MessageLogStore.STATUS_SENDING.equals(status)) return "발송 중";
        if (MessageLogStore.STATUS_SENT.equals(status)) return "발송 완료";
        if (MessageLogStore.STATUS_FAILED.equals(status)) return "발송 실패";
        if (MessageLogStore.STATUS_SKIPPED.equals(status)) return "건너뜀";
        if (MessageLogStore.STATUS_CANCELLED.equals(status)) return "취소됨";
        return safe(status);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Metadata {
        public final String normalizedPhone;
        public final String normalizedBody;
        public final String bodyHash;
        public final String purposeType;
        public final String idempotencyKey;
        public final long duplicateWindowMs;

        Metadata(String normalizedPhone, String normalizedBody, String bodyHash,
                 String purposeType, String idempotencyKey, long duplicateWindowMs) {
            this.normalizedPhone = normalizedPhone;
            this.normalizedBody = normalizedBody;
            this.bodyHash = bodyHash;
            this.purposeType = purposeType;
            this.idempotencyKey = idempotencyKey;
            this.duplicateWindowMs = duplicateWindowMs;
        }
    }
}
