package kr.pagero.calltag;

import android.content.Context;

/** 고객 목록/상세에서 페이지로 문의 문자의 실제 최신 상태를 같은 표현으로 보여준다. */
public final class PageroLeadSmsStatusResolver {
    private PageroLeadSmsStatusResolver() {}

    public static State latest(Context context, long customerId) {
        PageroLeadReceiptStore receipts = new PageroLeadReceiptStore(context);
        PageroLeadReceiptStore.SmsSnapshot snapshot;
        try {
            snapshot = receipts.latestSmsForCustomer(customerId);
        } finally {
            receipts.close();
        }
        if (snapshot == null) return null;

        if (snapshot.jobId > 0L) {
            MessageLogStore messages = new MessageLogStore(context);
            try {
                MessageRecord record = messages.find(snapshot.jobId);
                if (record != null) return fromMessage(record, snapshot.reason);
            } finally {
                messages.close();
            }
        }
        return fromStored(snapshot.status, snapshot.reason);
    }

    private static State fromMessage(MessageRecord record, String fallbackReason) {
        if (MessageLogStore.STATUS_SENT.equals(record.status)) {
            return new State(PageroLeadReceiptStore.SMS_SENT, "문자 발송완료", "");
        }
        if (MessageLogStore.STATUS_SENDING.equals(record.status)
                || MessageLogStore.STATUS_READY.equals(record.status)) {
            return new State(PageroLeadReceiptStore.SMS_SENDING, "문자 발송중", "");
        }
        if (MessageLogStore.STATUS_FAILED.equals(record.status)) {
            return new State(PageroLeadReceiptStore.SMS_FAILED, "문자 발송실패",
                    first(record.error, fallbackReason));
        }
        String reason = first(record.error, fallbackReason);
        if (MessageLogStore.STATUS_SCHEDULED.equals(record.status) && reason.isEmpty()) {
            reason = "발송 예정";
        }
        return new State(PageroLeadReceiptStore.SMS_NOT_SENT, "문자 미발송", reason);
    }

    private static State fromStored(String status, String reason) {
        if (PageroLeadReceiptStore.SMS_SENT.equals(status)) {
            return new State(status, "문자 발송완료", "");
        }
        if (PageroLeadReceiptStore.SMS_SENDING.equals(status)) {
            return new State(status, "문자 발송중", reason);
        }
        if (PageroLeadReceiptStore.SMS_FAILED.equals(status)) {
            return new State(status, "문자 발송실패", reason);
        }
        return new State(PageroLeadReceiptStore.SMS_NOT_SENT, "문자 미발송", reason);
    }

    private static String first(String first, String second) {
        String a = first == null ? "" : first.trim();
        return a.isEmpty() ? (second == null ? "" : second.trim()) : a;
    }

    public static final class State {
        public final String code;
        public final String label;
        public final String reason;

        State(String code, String label, String reason) {
            this.code = code;
            this.label = label;
            this.reason = reason == null ? "" : reason;
        }
    }
}
