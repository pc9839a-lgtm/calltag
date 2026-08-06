package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** 계좌번호 등 정산정보를 계정별 Android Keystore AES-GCM으로 암호화해 보관한다. */
public final class PartnerSettlementStore {
    private static final String PREFS = "calltag_partner_settlement_v1";
    private static final String KEY_PREFIX = "profile_";
    private static final String ALIAS = "pagero_calltag_partner_settlement_v1";

    public static final class Profile {
        public final String payoutType;
        public final String bankName;
        public final String accountHolder;
        public final String accountNumber;
        public final String settlementEmail;
        public final String businessNumber;
        public final long updatedAt;

        public Profile(String payoutType, String bankName, String accountHolder,
                       String accountNumber, String settlementEmail,
                       String businessNumber, long updatedAt) {
            this.payoutType = clean(payoutType);
            this.bankName = clean(bankName);
            this.accountHolder = clean(accountHolder);
            this.accountNumber = clean(accountNumber);
            this.settlementEmail = clean(settlementEmail).toLowerCase();
            this.businessNumber = clean(businessNumber);
            this.updatedAt = Math.max(0L, updatedAt);
        }

        public boolean isComplete() {
            return !bankName.isEmpty() && !accountHolder.isEmpty()
                    && accountNumber.replaceAll("[^0-9]", "").length() >= 8
                    && settlementEmail.contains("@");
        }

        public String maskedAccount() {
            String digits = accountNumber.replaceAll("[^0-9]", "");
            if (digits.length() <= 4) return digits;
            return "•••• " + digits.substring(digits.length() - 4);
        }
    }

    private PartnerSettlementStore() {}

    public static synchronized Profile read(Context context) {
        String account = accountKey(context);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_PREFIX + account, "");
        if (stored == null || stored.isEmpty()) {
            return new Profile("개인", "", "", "", AuthSessionStore.email(context), "", 0L);
        }
        try {
            JSONObject value = new JSONObject(decrypt(stored, account));
            return new Profile(
                    value.optString("payoutType", "개인"),
                    value.optString("bankName", ""),
                    value.optString("accountHolder", ""),
                    value.optString("accountNumber", ""),
                    value.optString("settlementEmail", AuthSessionStore.email(context)),
                    value.optString("businessNumber", ""),
                    value.optLong("updatedAt", 0L));
        } catch (Exception ignored) {
            prefs.edit().remove(KEY_PREFIX + account).apply();
            return new Profile("개인", "", "", "", AuthSessionStore.email(context), "", 0L);
        }
    }

    public static synchronized void save(Context context, Profile profile) {
        if (profile == null) throw new IllegalArgumentException("정산정보가 없습니다.");
        String account = accountKey(context);
        try {
            JSONObject value = new JSONObject()
                    .put("payoutType", profile.payoutType)
                    .put("bankName", profile.bankName)
                    .put("accountHolder", profile.accountHolder)
                    .put("accountNumber", profile.accountNumber)
                    .put("settlementEmail", profile.settlementEmail)
                    .put("businessNumber", profile.businessNumber)
                    .put("updatedAt", System.currentTimeMillis());
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_PREFIX + account, encrypt(value.toString(), account))
                    .apply();
        } catch (Exception error) {
            throw new IllegalStateException("정산정보를 안전하게 저장하지 못했습니다.", error);
        }
    }

    public static synchronized void clear(Context context) {
        String account = accountKey(context);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_PREFIX + account).apply();
    }

    private static String encrypt(String value, String account) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        cipher.updateAAD(account.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] packed = new byte[1 + iv.length + encrypted.length];
        packed[0] = (byte) iv.length;
        System.arraycopy(iv, 0, packed, 1, iv.length);
        System.arraycopy(encrypted, 0, packed, 1 + iv.length, encrypted.length);
        return Base64.encodeToString(packed, Base64.NO_WRAP);
    }

    private static String decrypt(String stored, String account) throws Exception {
        byte[] packed = Base64.decode(stored, Base64.NO_WRAP);
        int ivLength = packed.length == 0 ? 0 : packed[0] & 0xff;
        if (ivLength < 8 || ivLength > 32 || packed.length <= 1 + ivLength) {
            throw new IllegalArgumentException("invalid settlement profile");
        }
        byte[] iv = new byte[ivLength];
        byte[] encrypted = new byte[packed.length - 1 - ivLength];
        System.arraycopy(packed, 1, iv, 0, ivLength);
        System.arraycopy(packed, 1 + ivLength, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        cipher.updateAAD(account.getBytes(StandardCharsets.UTF_8));
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

    private static String accountKey(Context context) {
        String owner = AuthSessionStore.ownerId(context).trim();
        String identity = owner.isEmpty()
                ? AuthSessionStore.email(context).trim().toLowerCase() : owner;
        if (identity.isEmpty()) identity = "anonymous";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(digest,
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (Exception ignored) {
            return Integer.toHexString(identity.hashCode());
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
