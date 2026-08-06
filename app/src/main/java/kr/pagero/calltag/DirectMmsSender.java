package kr.pagero.calltag;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;

import com.google.android.mms.MMSPart;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 사진 첨부 문자 작업을 작성창 없이 실제 MMS로 자동 발송한다. */
public final class DirectMmsSender {
    private static final int MAX_IMAGE_SIDE = 1280;
    private static final int MAX_IMAGE_BYTES = 540 * 1024;
    private static final int MAX_PDU_BYTES = 600 * 1024;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private DirectMmsSender() {}

    public static void sendExisting(Context context, long messageId) {
        if (context == null || messageId <= 0L) return;
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> sendBlocking(app, messageId));
    }

    private static void sendBlocking(Context context, long messageId) {
        File pduFile = null;
        MessageLogStore store = new MessageLogStore(context);
        try {
            MessageRecord record = store.find(messageId);
            if (record == null || !MessageLogStore.STATUS_READY.equals(record.status)) return;
            if (!MmsComposer.hasAttachment(context, messageId)) {
                MmsStatusReceiver.fallbackToText(context, messageId,
                        "첨부 이미지를 찾지 못해 일반 문자로 전환했습니다.");
                return;
            }
            if (context.checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                store.markFailed(messageId, "문자 발송 권한이 필요합니다.");
                MmsComposer.forget(context, messageId);
                CampaignRuntimeManager.onSendResult(context, messageId, false,
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                return;
            }
            if (!context.getPackageManager().hasSystemFeature("android.hardware.telephony.messaging")) {
                store.markFailed(messageId, "이 기기는 MMS 자동발송을 지원하지 않습니다.");
                MmsComposer.forget(context, messageId);
                CampaignRuntimeManager.onSendResult(context, messageId, false,
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                return;
            }

            int subscriptionId = resolveSubscriptionId(record.subscriptionId);
            if (!SubscriptionManager.isValidSubscriptionId(subscriptionId)
                    || !SimProfileManager.isActive(context, subscriptionId)) {
                store.markFailed(messageId, "선택한 문자 SIM을 사용할 수 없습니다.");
                MmsComposer.forget(context, messageId);
                CampaignRuntimeManager.onSendResult(context, messageId, false,
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                return;
            }

            String phone = PhoneNumberNormalizer.normalize(record.phone);
            if (phone.length() < 8 || record.body.trim().isEmpty()) {
                store.markFailed(messageId, "전화번호 또는 문자 내용을 확인해주세요.");
                MmsComposer.forget(context, messageId);
                CampaignRuntimeManager.onSendResult(context, messageId, false,
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                return;
            }

            Uri imageUri = MessageAttachmentStore.shareUri(
                    context, MmsComposer.attachmentRef(context, messageId));
            if (imageUri == null) {
                MmsStatusReceiver.fallbackToText(context, messageId,
                        "첨부 이미지를 열지 못해 일반 문자로 전환했습니다.");
                return;
            }
            byte[] imageBytes = prepareImage(context, imageUri);

            // 콜링크에서 검증했던 Klinker PDU builder를 그대로 사용한다.
            new Transaction(context, new Settings());
            MMSPart imagePart = new MMSPart();
            imagePart.Name = "calltag-image.jpg";
            imagePart.MimeType = "image/jpeg";
            imagePart.Data = imageBytes;

            MMSPart textPart = new MMSPart();
            textPart.Name = "message.txt";
            textPart.MimeType = "text/plain";
            textPart.Data = record.body.getBytes(StandardCharsets.UTF_8);

            Transaction.MessageInfo info = Transaction.getBytes(
                    context,
                    false,
                    "",
                    new String[]{phone},
                    new MMSPart[]{imagePart, textPart},
                    null);
            if (info == null || info.bytes == null || info.bytes.length == 0) {
                throw new IllegalStateException("MMS 데이터를 만들지 못했습니다.");
            }
            if (info.bytes.length > MAX_PDU_BYTES) {
                throw new IllegalStateException("MMS 전체 용량이 통신사 제한을 초과했습니다.");
            }

            File directory = new File(context.getCacheDir(), "mms_outbox");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IllegalStateException("MMS 임시폴더를 만들지 못했습니다.");
            }
            String fileName = "calltag_" + messageId + "_"
                    + System.currentTimeMillis() + ".pdu";
            pduFile = new File(directory, fileName);
            try (FileOutputStream output = new FileOutputStream(pduFile)) {
                output.write(info.bytes);
                output.flush();
            }

            Uri contentUri = new Uri.Builder()
                    .scheme("content")
                    .authority(BuildConfig.APPLICATION_ID + ".mms-pdu")
                    .appendPath(MmsPduProvider.PATH)
                    .appendPath(fileName)
                    .build();
            grantReadAccess(context, contentUri);

            Intent sent = new Intent(context, MmsStatusReceiver.class)
                    .setAction(MmsStatusReceiver.ACTION_MMS_SENT)
                    .setPackage(context.getPackageName())
                    .setData(Uri.parse("calltag://mms-sent/" + messageId))
                    .putExtra(MmsStatusReceiver.EXTRA_MESSAGE_ID, messageId)
                    .putExtra(MmsStatusReceiver.EXTRA_PDU_PATH, pduFile.getAbsolutePath());
            PendingIntent sentIntent = PendingIntent.getBroadcast(
                    context,
                    (int) ((messageId * 37L) & 0x7fffffffL),
                    sent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            store.markSending(messageId);
            SmsManager manager = managerFor(context, subscriptionId);
            Bundle config = new Bundle();
            config.putInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, MAX_PDU_BYTES);
            manager.sendMultimediaMessage(context, contentUri, null, config, sentIntent);
            DiagnosticEventStore.record(context, "MMS 발송 요청", messageId,
                    "사진 포함 자동문자 · SIM " + subscriptionId);
        } catch (Exception error) {
            if (pduFile != null && pduFile.isFile()) pduFile.delete();
            MmsStatusReceiver.fallbackToText(context, messageId,
                    "MMS 자동발송 준비 실패: " + safeMessage(error));
        } finally {
            store.close();
        }
    }

    private static int resolveSubscriptionId(int requested) {
        if (SubscriptionManager.isValidSubscriptionId(requested)) return requested;
        return SubscriptionManager.getDefaultSmsSubscriptionId();
    }

    private static SmsManager managerFor(Context context, int subscriptionId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SmsManager service = context.getSystemService(SmsManager.class);
            if (service == null) throw new IllegalStateException("문자 서비스를 사용할 수 없습니다.");
            return service.createForSubscriptionId(subscriptionId);
        }
        return SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
    }

    private static byte[] prepareImage(Context context, Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("이미지 파일을 열 수 없습니다.");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IllegalStateException("지원하지 않는 이미지입니다.");
        }

        int sample = 1;
        while (bounds.outWidth / sample > MAX_IMAGE_SIDE * 2
                || bounds.outHeight / sample > MAX_IMAGE_SIDE * 2) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("이미지 파일을 열 수 없습니다.");
            decoded = BitmapFactory.decodeStream(input, null, options);
        }
        if (decoded == null) throw new IllegalStateException("이미지 변환에 실패했습니다.");

        Bitmap scaled = scaleDown(decoded, MAX_IMAGE_SIDE);
        if (scaled != decoded) decoded.recycle();
        Bitmap flattened = Bitmap.createBitmap(
                scaled.getWidth(), scaled.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(flattened);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(scaled, 0f, 0f, null);
        if (scaled != flattened) scaled.recycle();

        byte[] bytes = null;
        for (int quality = 86; quality >= 42; quality -= 6) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!flattened.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                flattened.recycle();
                throw new IllegalStateException("이미지 압축에 실패했습니다.");
            }
            bytes = output.toByteArray();
            if (bytes.length <= MAX_IMAGE_BYTES) break;
        }
        flattened.recycle();
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalStateException("이미지 용량이 너무 큽니다.");
        }
        return bytes;
    }

    private static Bitmap scaleDown(Bitmap source, int maxSide) {
        int largest = Math.max(source.getWidth(), source.getHeight());
        if (largest <= maxSide) return source;
        float ratio = maxSide / (float) largest;
        return Bitmap.createScaledBitmap(source,
                Math.max(1, Math.round(source.getWidth() * ratio)),
                Math.max(1, Math.round(source.getHeight() * ratio)), true);
    }

    private static void grantReadAccess(Context context, Uri uri) {
        String defaultSms = Telephony.Sms.getDefaultSmsPackage(context);
        if (defaultSms != null && !defaultSms.trim().isEmpty()) {
            context.grantUriPermission(defaultSms, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        try {
            context.grantUriPermission("com.android.phone", uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
    }

    private static String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName() : message.trim();
    }
}
