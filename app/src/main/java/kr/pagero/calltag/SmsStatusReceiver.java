package kr.pagero.calltag;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;

public final class SmsStatusReceiver extends BroadcastReceiver {
    public static final String ACTION_SENT = "kr.pagero.calltag.SMS_SENT";
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_PART_INDEX = "part_index";
    public static final String EXTRA_PART_COUNT = "part_count";

    private static final String PREFS = "calltag_sms_part_status";

    @Override
    public void onReceive(Context context, Intent intent) {
        long messageId = intent == null ? -1L : intent.getLongExtra(EXTRA_MESSAGE_ID, -1L);
        int partCount = intent == null ? 1 : Math.max(1, intent.getIntExtra(EXTRA_PART_COUNT, 1));
        if (messageId <= 0L) return;

        MessageLogStore store = new MessageLogStore(context);
        try {
            MessageRecord before = store.find(messageId);
            if (getResultCode() == Activity.RESULT_OK) {
                synchronized (SmsStatusReceiver.class) {
                    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                    String key = "ok_" + messageId;
                    int success = prefs.getInt(key, 0) + 1;
                    if (success >= partCount) {
                        prefs.edit().remove(key).apply();
                        store.markSent(messageId);
                        recordTimeline(context, before, true, "");
                        DiagnosticEventStore.record(context, "SMS 발송 완료", messageId,
                                "분할 " + partCount + "개 완료");
                    } else {
                        prefs.edit().putInt(key, success).apply();
                    }
                }
            } else {
                String error = errorLabel(getResultCode());
                boolean firstFailure = before != null
                        && !MessageLogStore.STATUS_FAILED.equals(before.status)
                        && !MessageLogStore.STATUS_SENT.equals(before.status);
                store.markFailed(messageId, error);
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().remove("ok_" + messageId).apply();
                if (firstFailure) recordTimeline(context, before, false, error);
                DiagnosticEventStore.record(context, "SMS 발송 실패", messageId,
                        errorCategory(getResultCode()));
            }
        } finally {
            store.close();
        }
        context.sendBroadcast(new Intent(MessageSectionView.ACTION_CHANGED)
                .setPackage(context.getPackageName()));
    }

    private void recordTimeline(Context context, MessageRecord record,
                                boolean sent, String error) {
        if (record == null || record.customerId <= 0L) return;
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            long now = System.currentTimeMillis();
            db.insertInteraction(
                    record.customerId,
                    sent ? "MESSAGE_SENT" : "MESSAGE_FAILED",
                    now,
                    now,
                    0L,
                    sent ? "SENT" : "FAILED",
                    sent ? record.body : error + " · " + record.body);
        } finally {
            db.close();
        }
    }

    private String errorCategory(int resultCode) {
        if (resultCode == SmsManager.RESULT_ERROR_NO_SERVICE) return "NO_SERVICE";
        if (resultCode == SmsManager.RESULT_ERROR_RADIO_OFF) return "RADIO_OFF";
        if (resultCode == SmsManager.RESULT_ERROR_NULL_PDU) return "NULL_PDU";
        if (resultCode == SmsManager.RESULT_ERROR_GENERIC_FAILURE) return "GENERIC_FAILURE";
        return "RESULT_" + resultCode;
    }

    private String errorLabel(int resultCode) {
        if (resultCode == SmsManager.RESULT_ERROR_NO_SERVICE) return "통신 서비스에 연결되지 않았습니다.";
        if (resultCode == SmsManager.RESULT_ERROR_RADIO_OFF) return "휴대전화 통신 기능이 꺼져 있습니다.";
        if (resultCode == SmsManager.RESULT_ERROR_NULL_PDU) return "문자 데이터를 만들지 못했습니다.";
        if (resultCode == SmsManager.RESULT_ERROR_GENERIC_FAILURE) return "통신사 문자 발송에 실패했습니다.";
        return "문자 발송에 실패했습니다. 오류 " + resultCode;
    }
}
