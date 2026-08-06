package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class CallTagSyncDeviceStore {
    private static final String PREFS = "calltag_secure_sync_device";
    private static final String KEY_DEVICE = "device";
    private static final String ALIAS = "pagero_calltag_sync_device_v1";

    private CallTagSyncDeviceStore() {}

    public static synchronized String deviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_DEVICE, "");
        if (stored != null && !stored.isEmpty()) {
            try {
                String value = decrypt(stored);
                if (value.length() >= 24) return value;
            } catch (Exception ignored) {
                prefs.edit().remove(KEY_DEVICE).commit();
            }
        }
        return createAndPersist(context, prefs);
    }

    public static synchronized String rotate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_DEVICE).commit();
        return createAndPersist(context, prefs);
    }

    private static String createAndPersist(Context context, SharedPreferences prefs) {
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        String value = Base64.encodeToString(random,
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        try {
            prefs.edit().putString(KEY_DEVICE, encrypt(value)).commit();
        } catch (Exception error) {
            throw new IllegalStateException("동기화 기기 식별정보를 안전하게 저장하지 못했습니다.", error);
        }
        return value;
    }

    private static String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] packed = new byte[1 + iv.length + encrypted.length];
        packed[0] = (byte) iv.length;
        System.arraycopy(iv, 0, packed, 1, iv.length);
        System.arraycopy(encrypted, 0, packed, 1 + iv.length, encrypted.length);
        return Base64.encodeToString(packed, Base64.NO_WRAP);
    }

    private static String decrypt(String stored) throws Exception {
        byte[] packed = Base64.decode(stored, Base64.NO_WRAP);
        int ivLength = packed.length == 0 ? 0 : packed[0] & 0xff;
        if (ivLength < 8 || ivLength > 32 || packed.length <= 1 + ivLength) {
            throw new IllegalArgumentException("invalid encrypted device id");
        }
        byte[] iv = new byte[ivLength];
        byte[] encrypted = new byte[packed.length - 1 - ivLength];
        System.arraycopy(packed, 1, iv, 0, ivLength);
        System.arraycopy(packed, 1 + ivLength, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) {
            return ((KeyStore.SecretKeyEntry) store.getEntry(ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
