package kr.pagero.calltag;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;

import java.io.File;

/** MMS 발송 결과를 기록하고 실패 시 같은 작업을 일반 SMS로 한 번만 대체 발송한다. */
public final class MmsStatusReceiver extends BroadcastReceiver {
    public static final String ACTION_MMS_SENT = "kr.pagero.calltag.MMS_SENT";
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_PDU_PATH = "pdu_path";

    private static final String PREFS = "calltag_mms_fallback_v1";

    @Override
    public void onReceive(Context context, Intent intent) {
        long messageId = intent == null ? -1L
                : intent.getLongExtra(EXTRA_MESSAGE_ID, -1L);
        String pduPath = intent == null ? "" : intent.getStringExtra(EXTRA_PDU_PATH);
        deletePdu(pduPath);
        if (messageId <= 0L) return;

        int resultCode = getResultCode();
        if (resultCode == Activity.RESULT_OK) {
            MessageRecord record;
            MessageLogStore store = new MessageLogStore(context);
            try {
                record = store.find(messageId);
                if (record == null || !MessageLogStore.STATUS_SENDING.equals(record.status)) {
                    DataIntegrityManager.recordLateCallback(context, messageId,
                            record == null ? "MISSING" : record.status);
                    return;
                }
                store.markSent(messageId);
                recordTimeline(context, record, true, "");
            } finally {
                store.close();
            }
            clearFallbackGuard(context, messageId);
            MmsComposer.forget(context, messageId);
            DiagnosticEventStore.record(context, "MMS 발송 완료", messageId,
                    "통신사 MMS 요청 성공");
            CampaignRuntimeManager.onSendResult(context, messageId, true, resultCode);
            notifyChanged(context);
            return;
        }

        fallbackToText(context, messageId, errorLabel(resultCode));
    }

    public static void fallbackToText(Context context, long messageId, String reason) {
        if (context == null || messageId <= 0L) return;
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = "fallback_" + messageId;
        synchronized (MmsStatusReceiver.class) {
            if (prefs.getBoolean(key, false)) return;
            prefs.edit().putBoolean(key, true).apply();
        }

        MessageRecord record;
        MessageLogStore store = new MessageLogStore(app);
        try {
            record = store.find(messageId);
            if (record == null) return;
            MmsComposer.forget(app, messageId);
            store.markReady(messageId);
            DiagnosticEventStore.record(app, "MMS 실패·SMS 전환", messageId,
                    reason == null ? "MMS 실패" : reason);
        } finally {
            store.close();
        }
        SmsSender.sendExisting(app, messageId);
        notifyChanged(app);
    }

    private static void recordTimeline(Context context, MessageRecord record,
                                       boolean sent, String error) {
        if (record == null || record.customerId <= 0L) return;
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            if (db.findCustomerById(record.customerId) == null) return;
            long now = System.currentTimeMillis();
            db.insertInteraction(
                    record.customerId,
                    sent ? "MESSAGE_SENT" : "MESSAGE_FAILED",
                    now,
                    now,
                    0L,
                    sent ? "SENT" : "FAILED",
                    sent ? "[사진 MMS] " + record.body : error + " · " + record.body);
        } finally {
            db.close();
        }
    }

    private static String errorLabel(int resultCode) {
        if (resultCode == SmsManager.MMS_ERROR_HTTP_FAILURE) {
            return "통신사 MMS 서버 연결에 실패해 일반 문자로 전환했습니다.";
        }
        if (resultCode == SmsManager.MMS_ERROR_CONFIGURATION_ERROR) {
            return "통신사 MMS 설정 오류로 일반 문자로 전환했습니다.";
        }
        if (resultCode == SmsManager.MMS_ERROR_NO_DATA_NETWORK) {
            return "MMS 데이터망을 사용할 수 없어 일반 문자로 전환했습니다.";
        }
        if (resultCode == SmsManager.MMS_ERROR_INVALID_APN) {
            return "MMS APN 설정을 사용할 수 없어 일반 문자로 전환했습니다.";
        }
        if (resultCode == SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS) {
            return "MMS 서버에 연결하지 못해 일반 문자로 전환했습니다.";
        }
        if (resultCode == SmsManager.MMS_ERROR_RETRY) {
            return "MMS 재시도가 필요해 일반 문자로 전환했습니다.";
        }
        return "MMS 발송 실패 코드 " + resultCode + " · 일반 문자로 전환했습니다.";
    }

    private static void clearFallbackGuard(Context context, long messageId) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove("fallback_" + messageId).apply();
    }

    private static void notifyChanged(Context context) {
        context.sendBroadcast(new Intent(MessageSectionView.ACTION_CHANGED)
                .setPackage(context.getPackageName()));
    }

    private static void deletePdu(String path) {
        if (path == null || path.trim().isEmpty()) return;
        try {
            File file = new File(path);
            if (file.isFile()) file.delete();
        } catch (SecurityException ignored) {
        }
    }
}
