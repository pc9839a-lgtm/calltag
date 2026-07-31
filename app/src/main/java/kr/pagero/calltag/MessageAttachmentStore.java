package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/** Imports gallery images into app-private storage and exposes read-only share URIs. */
public final class MessageAttachmentStore {
    private static final String DIRECTORY = "message_images";
    private static final int MAX_DIMENSION = 1600;
    private static final int MAX_BYTES = 900 * 1024;

    private MessageAttachmentStore() {}

    public static String importImage(Context context, Uri source, String ownerKey) {
        if (context == null || source == null) {
            throw new IllegalArgumentException("이미지를 선택해주세요.");
        }
        String mime = context.getContentResolver().getType(source);
        if (mime != null && !mime.toLowerCase(Locale.US).startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 첨부할 수 있습니다.");
        }

        Bitmap decoded = decodeScaled(context, source);
        if (decoded == null) throw new IllegalArgumentException("이미지를 불러오지 못했습니다.");
        Bitmap flattened = flattenToWhite(decoded);
        if (flattened != decoded) decoded.recycle();

        File directory = directory(context);
        String fileName = sanitize(ownerKey) + "-" + System.currentTimeMillis() + ".jpg";
        File target = new File(directory, fileName);
        try {
            byte[] data = encodeWithinLimit(flattened);
            try (FileOutputStream output = new FileOutputStream(target)) {
                output.write(data);
                output.flush();
            }
            return DIRECTORY + "/" + fileName;
        } catch (IOException error) {
            if (target.exists()) target.delete();
            throw new IllegalArgumentException("이미지를 저장하지 못했습니다.");
        } finally {
            flattened.recycle();
        }
    }

    public static String duplicate(Context context, String imageRef, String ownerKey) {
        File source = resolve(context, imageRef);
        if (source == null || !source.exists()) return "";
        File target = new File(directory(context),
                sanitize(ownerKey) + "-" + System.currentTimeMillis() + ".jpg");
        try (InputStream input = new java.io.FileInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            output.flush();
            return DIRECTORY + "/" + target.getName();
        } catch (IOException error) {
            if (target.exists()) target.delete();
            return "";
        }
    }

    public static Bitmap preview(Context context, String imageRef) {
        File file = resolve(context, imageRef);
        return file == null || !file.exists() ? null : BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    public static Uri shareUri(Context context, String imageRef) {
        File file = resolve(context, imageRef);
        if (file == null || !file.exists()) return null;
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".files", file);
    }

    public static boolean exists(Context context, String imageRef) {
        File file = resolve(context, imageRef);
        return file != null && file.exists() && file.length() > 0L;
    }

    public static String sizeLabel(Context context, String imageRef) {
        File file = resolve(context, imageRef);
        if (file == null || !file.exists()) return "파일 없음";
        long kb = Math.max(1L, file.length() / 1024L);
        return kb >= 1024L
                ? String.format(Locale.KOREA, "%.1fMB", kb / 1024d)
                : kb + "KB";
    }

    public static void delete(Context context, String imageRef) {
        File file = resolve(context, imageRef);
        if (file != null && file.exists()) file.delete();
    }

    private static Bitmap decodeScaled(Context context, Uri source) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            BitmapFactory.decodeStream(input, null, bounds);
        } catch (IOException ignored) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int sample = 1;
        while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_DIMENSION * 2) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            if (bitmap == null) return null;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int longest = Math.max(width, height);
            if (longest <= MAX_DIMENSION) return bitmap;
            float ratio = MAX_DIMENSION / (float) longest;
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                    Math.max(1, Math.round(width * ratio)),
                    Math.max(1, Math.round(height * ratio)), true);
            if (scaled != bitmap) bitmap.recycle();
            return scaled;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static Bitmap flattenToWhite(Bitmap source) {
        if (!source.hasAlpha()) return source;
        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(source, 0f, 0f, null);
        return result;
    }

    private static byte[] encodeWithinLimit(Bitmap source) throws IOException {
        Bitmap working = source;
        boolean ownsWorking = false;
        try {
            for (int scalePass = 0; scalePass < 4; scalePass++) {
                for (int quality = 88; quality >= 52; quality -= 8) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!working.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                        throw new IOException("compress failed");
                    }
                    byte[] bytes = output.toByteArray();
                    if (bytes.length <= MAX_BYTES || quality == 52 && scalePass == 3) {
                        return bytes;
                    }
                }
                int nextWidth = Math.max(480, Math.round(working.getWidth() * 0.82f));
                int nextHeight = Math.max(480, Math.round(working.getHeight() * 0.82f));
                Bitmap next = Bitmap.createScaledBitmap(working, nextWidth, nextHeight, true);
                if (ownsWorking && next != working) working.recycle();
                working = next;
                ownsWorking = working != source;
            }
            throw new IOException("image too large");
        } finally {
            if (ownsWorking && working != source && !working.isRecycled()) working.recycle();
        }
    }

    private static File directory(Context context) {
        File directory = new File(context.getFilesDir(), DIRECTORY);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalArgumentException("이미지 저장 공간을 만들지 못했습니다.");
        }
        return directory;
    }

    private static File resolve(Context context, String imageRef) {
        String value = imageRef == null ? "" : imageRef.trim();
        if (value.isEmpty() || value.contains("..") || value.startsWith("/")) return null;
        File file = new File(context.getFilesDir(), value);
        try {
            String root = context.getFilesDir().getCanonicalPath() + File.separator;
            return file.getCanonicalPath().startsWith(root) ? file : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String sanitize(String ownerKey) {
        String value = ownerKey == null ? "image" : ownerKey.trim();
        value = value.replaceAll("[^A-Za-z0-9_-]", "_");
        if (value.isEmpty()) value = "image";
        return value.length() > 48 ? value.substring(0, 48) : value;
    }
}
