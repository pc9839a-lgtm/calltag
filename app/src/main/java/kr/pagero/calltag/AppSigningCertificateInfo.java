package kr.pagero.calltag;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Reads the certificates of the APK that is actually installed on the device. */
public final class AppSigningCertificateInfo {
    private AppSigningCertificateInfo() {}

    public static String sha1Summary(Context context) {
        Set<String> values = sha1Fingerprints(context);
        if (values.isEmpty()) return "확인 불가";
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(" / ");
            out.append(value);
        }
        return out.toString();
    }

    public static Set<String> sha1Fingerprints(Context context) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo != null) {
                    Signature[] signatures = info.signingInfo.hasMultipleSigners()
                            ? info.signingInfo.getApkContentsSigners()
                            : info.signingInfo.getSigningCertificateHistory();
                    addFingerprints(values, signatures);
                }
            } else {
                @SuppressWarnings("deprecation")
                PackageInfo legacy = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
                info = legacy;
                @SuppressWarnings("deprecation")
                Signature[] signatures = info.signatures;
                addFingerprints(values, signatures);
            }
        } catch (Exception ignored) {
        }
        return values;
    }

    private static void addFingerprints(Set<String> out, Signature[] signatures) throws Exception {
        if (signatures == null) return;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        for (Signature signature : signatures) {
            if (signature == null) continue;
            byte[] digest = sha1.digest(signature.toByteArray());
            StringBuilder value = new StringBuilder();
            for (byte part : digest) {
                if (value.length() > 0) value.append(':');
                value.append(String.format(Locale.US, "%02X", part & 0xFF));
            }
            out.add(value.toString());
        }
    }
}
