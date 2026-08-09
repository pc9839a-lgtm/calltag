package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.Locale;

/** 로그인 전 HTTPS 추천 링크만 보관하고 로그인 후 서버 등록에 사용한다. */
public final class PendingReferralStore {
    private static final String PREFS = "calltag_pending_referral";
    private static final String KEY_CODE = "code";
    private static final String KEY_CAPTURED_AT = "captured_at";
    private static final long MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L;

    private PendingReferralStore() {}

    public static boolean capture(Context context, Intent intent) {
        if (context == null || intent == null) return false;
        String code = referralCode(intent.getData());
        intent.removeExtra("referralCode");
        if (code.isEmpty()) return false;
        prefs(context).edit()
                .putString(KEY_CODE, code)
                .putLong(KEY_CAPTURED_AT, System.currentTimeMillis())
                .apply();
        intent.setData(null);
        return true;
    }

    public static String peek(Context context) {
        SharedPreferences value = prefs(context);
        long capturedAt = value.getLong(KEY_CAPTURED_AT, 0L);
        long age = System.currentTimeMillis() - capturedAt;
        if (capturedAt <= 0L || age < 0L || age > MAX_AGE_MS) {
            clear(context);
            return "";
        }
        String code = normalize(value.getString(KEY_CODE, ""));
        if (code.isEmpty()) clear(context);
        return code;
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static String referralCode(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return "";
        String host = lower(uri.getHost());
        if (!("pagero.kr".equals(host) || "www.pagero.kr".equals(host))) return "";
        if (uri.getPort() != -1 || uri.getUserInfo() != null || uri.getFragment() != null) return "";
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!path.startsWith("/r/") || path.length() <= 3 || path.indexOf('/', 3) >= 0) return "";
        return normalize(path.substring(3));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String cleaned = value.trim().toUpperCase(Locale.ROOT);
        return cleaned.matches("[A-Z0-9]{4,20}") ? cleaned : "";
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
