package kr.pagero.calltag;

import android.content.Context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Keeps automatic SMS choices private to the authenticated server owner. */
public final class AccountDataScope {
    private AccountDataScope() {}

    public static String preferenceName(Context context, String original) {
        if (original == null || !original.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid preference name");
        }
        String owner = AuthSessionStore.hasSession(context)
                ? AuthSessionStore.ownerId(context).trim() : "";
        // Application.onCreate initializes defaults before login. This isolated, default-off
        // scope must never inherit an earlier account's automatic SMS choices.
        if (owner.isEmpty()) return original + "-signed-out";
        return original + "-owner-" + fingerprint(owner);
    }

    private static String fingerprint(String owner) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(owner.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                int value = digest[i] & 0xff;
                hex.append(Character.forDigit(value >>> 4, 16));
                hex.append(Character.forDigit(value & 0x0f, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
