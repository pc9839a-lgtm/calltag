package kr.pagero.calltag;

import android.content.Context;

/**
 * Conservative server minimums are relevant only to automatic triggers.
 * Manual SMS/campaign sends never acquire a hidden automatic delay.
 */
public final class CallTagRemoteAutomationLimits {
    private static final long MINUTE = 60_000L;
    private static final long DAY = 24L * 60L * MINUTE;

    private CallTagRemoteAutomationLimits() {}

    public static long earliestAutomatedSendAt(Context context, MessageRecord record) {
        if (context == null || record == null || record.createdAt <= 0L) return 0L;
        if (PageroLeadMessageAutomation.TRIGGER_PAGERO_LEAD_RECEIVED
                .equals(record.triggerType)) {
            return record.createdAt
                    + CallTagRemotePolicy.minimumLeadDelayMinutes(context) * MINUTE;
        }
        if (MessageAutomationManager.TRIGGER_DELAYED.equals(record.triggerType)) {
            return record.createdAt
                    + CallTagRemotePolicy.minimumFollowupDays(context) * DAY;
        }
        return 0L;
    }
}
