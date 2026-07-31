package kr.pagero.calltag;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;

public final class SmsSender {
    private SmsSender() {}

    public static long queueAndSend(Context context, long customerId, long callLogId,
                                    String phone, String body, String triggerType,
                                    int subscriptionId) {
        MessageLogStore store = new MessageLogStore(context);
        try {
            if (callLogId > 0L && store.hasActiveImmediateForCall(callLogId)) {
                return -1L;
            }
            long id = store.createJob(customerId, callLogId, phone, body, triggerType,
                    MessageLogStore.STATUS_READY, System.currentTimeMillis(), subscriptionId);
            sendExisting(context, id);
            return id;
        } finally {
            store.close();
        }
    }

    public static void sendExisting(Context context, long messageId) {
        MessageLogStore store = new MessageLogStore(context);
        try {
            MessageRecord record = store.find(messageId);
            if (record == null) return;
            if (!FeatureEntitlementStore.hasMessageAccess(context)) {
                store.markFailed(messageId, "문자자동화 구독 권한이 없습니다.");
                return;
            }
            if (context.checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                store.markFailed(messageId, "문자 발송 권한이 필요합니다.");
                return;
            }
            String normalized = PhoneNumberNormalizer.normalize(record.phone);
            if (normalized.length() < 8 || record.body.trim().isEmpty()) {
                store.markFailed(messageId, "전화번호 또는 문자 내용을 확인해주세요.");
                return;
            }

            SmsManager manager = smsManager(record.subscriptionId);
            ArrayList<String> parts = manager.divideMessage(record.body);
            if (parts == null || parts.isEmpty()) {
                store.markFailed(messageId, "문자 내용을 분할하지 못했습니다.");
                return;
            }

            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++) {
                Intent sent = new Intent(context, SmsStatusReceiver.class)
                        .setAction(SmsStatusReceiver.ACTION_SENT)
                        .setPackage(context.getPackageName())
                        .setData(Uri.parse("calltag://sms-sent/" + messageId + "/" + i))
                        .putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, messageId)
                        .putExtra(SmsStatusReceiver.EXTRA_PART_INDEX, i)
                        .putExtra(SmsStatusReceiver.EXTRA_PART_COUNT, parts.size());
                int requestCode = (int) ((messageId * 31L + i) & 0x7fffffffL);
                sentIntents.add(PendingIntent.getBroadcast(
                        context, requestCode, sent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }

            store.markSending(messageId);
            if (parts.size() == 1) {
                manager.sendTextMessage(normalized, null, parts.get(0), sentIntents.get(0), null);
            } else {
                manager.sendMultipartTextMessage(normalized, null, parts, sentIntents, null);
            }
        } catch (SecurityException error) {
            store.markFailed(messageId, "문자 발송 권한을 확인해주세요.");
        } catch (RuntimeException error) {
            String message = error.getMessage();
            store.markFailed(messageId,
                    message == null || message.trim().isEmpty() ? "문자 발송 요청에 실패했습니다." : message);
        } finally {
            store.close();
        }
    }

    private static SmsManager smsManager(int subscriptionId) {
        if (SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
            return SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
        }
        return SmsManager.getDefault();
    }
}
