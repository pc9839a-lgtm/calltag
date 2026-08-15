package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class AuthSessionStore {
    private static final String PREFS = "calltag_auth_session";
    private static final String ALIAS = "pagero_calltag_auth_v1";
    private static final String KEY_SESSION = "session";
    private static final String KEY_OWNER_ID = "owner_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_BRAND = "brand";
    private static final String KEY_INDUSTRY = "industry";
    private static final String KEY_STATUS = "entitlement_status";
    private static final String KEY_ACTIVE = "entitlement_active";

    private AuthSessionStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void save(Context context, JSONObject response) throws Exception {
        String responseSession = response.optString("session", "").trim();
        String session = responseSession.isEmpty() ? session(context) : responseSession;
        if (session.isEmpty()) throw new IllegalStateException("Login session is missing.");

        JSONObject profile = response.optJSONObject("profile");
        if (profile == null) profile = response.optJSONObject("user");
        if (profile == null) profile = new JSONObject();
        JSONObject entitlement = response.optJSONObject("entitlement");
        if (entitlement == null) entitlement = new JSONObject();

        SharedPreferences current = prefs(context);
        String currentOwnerId = current.getString(KEY_OWNER_ID, "");
        String incomingOwnerId = profile.has("ownerId")
                ? profile.optString("ownerId", "").trim() : currentOwnerId;
        if (!currentOwnerId.isEmpty()
                && !incomingOwnerId.isEmpty()
                && !currentOwnerId.equals(incomingOwnerId)) {
            FeatureEntitlementStore.clear(context);
            ReferralStateStore.clear(context);
            PageroAccountStatusStore.clear(context);
        }

        boolean saved = current.edit()
                .putString(KEY_SESSION, encrypt(session))
                .putString(KEY_OWNER_ID, incomingOwnerId)
                .putString(KEY_NAME, profile.has("name")
                        ? profile.optString("name", "") : current.getString(KEY_NAME, ""))
                .putString(KEY_EMAIL, profile.has("email")
                        ? profile.optString("email", "") : current.getString(KEY_EMAIL, ""))
                .putString(KEY_PHONE, profile.has("phone")
                        ? profile.optString("phone", "") : current.getString(KEY_PHONE, ""))
                .putString(KEY_BRAND, profile.has("brandName")
                        ? profile.optString("brandName", "") : current.getString(KEY_BRAND, ""))
                .putString(KEY_INDUSTRY, profile.has("industry")
                        ? profile.optString("industry", "") : current.getString(KEY_INDUSTRY, ""))
                .putString(KEY_STATUS, entitlement.has("status")
                        ? entitlement.optString("status", "beta") : current.getString(KEY_STATUS, "beta"))
                .putBoolean(KEY_ACTIVE, entitlement.has("active")
                        ? entitlement.optBoolean("active", true) : current.getBoolean(KEY_ACTIVE, true))
                .commit();
        if (!saved || !hasSession(context)) {
            throw new IllegalStateException("Login session was not persisted.");
        }
        CallTagSyncWorkScheduler.reconcile(context);
    }

    public static String session(Context context) {
        String stored = prefs(context).getString(KEY_SESSION, "");
        if (stored == null || stored.isEmpty()) return "";
        try {
            return decrypt(stored);
        } catch (Exception e) {
            clear(context);
            return "";
        }
    }

    public static boolean hasSession(Context context) {
        return !session(context).isEmpty();
    }

    public static String ownerId(Context context) { return prefs(context).getString(KEY_OWNER_ID, ""); }
    public static String name(Context context) { return prefs(context).getString(KEY_NAME, ""); }
    public static String email(Context context) { return prefs(context).getString(KEY_EMAIL, ""); }
    public static String phone(Context context) { return prefs(context).getString(KEY_PHONE, ""); }
    public static String brand(Context context) { return prefs(context).getString(KEY_BRAND, ""); }
    public static String industry(Context context) { return prefs(context).getString(KEY_INDUSTRY, ""); }
    public static String status(Context context) { return prefs(context).getString(KEY_STATUS, "beta"); }
    public static boolean active(Context context) { return prefs(context).getBoolean(KEY_ACTIVE, true); }

    public static void clear(Context context) {
        CallTagSyncWorkScheduler.cancel(context);
        prefs(context).edit().clear().commit();
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
        if (packed.length <= 12) return "";
        int ivLength = packed[0] & 0xff;
        if (ivLength < 8 || ivLength > 32 || packed.length <= 1 + ivLength) return "";
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
