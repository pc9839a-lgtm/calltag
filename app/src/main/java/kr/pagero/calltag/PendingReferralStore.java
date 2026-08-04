package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.Locale;

/** 로그인 전 추천 링크를 보관하고 로그인 후 서버 등록에 사용한다. */
public final class PendingReferralStore {
    private static final String PREFS = "calltag_pending_referral";
    private static final String KEY_CODE = "code";
    private static final String KEY_CAPTURED_AT = "captured_at";
    private static final long MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L;

    private PendingReferralStore() {}

    public static boolean capture(Context context, Intent intent) {
        if (intent == null) return false;
        String code = referralCode(intent.getData());
        if (code.isEmpty()) code = normalize(intent.getStringExtra("referralCode"));
        if (code.isEmpty()) return false;
        prefs(context).edit()
                .putString(KEY_CODE, code)
                .putLong(KEY_CAPTURED_AT, System.currentTimeMillis())
                .apply();
        intent.setData(null);
        intent.removeExtra("referralCode");
        return true;
    }

    public static String peek(Context context) {
        SharedPreferences value = prefs(context);
        long capturedAt = value.getLong(KEY_CAPTURED_AT, 0L);
        if (capturedAt <= 0L || System.currentTimeMillis() - capturedAt > MAX_AGE_MS) {
            clear(context);
            return "";
        }
        return normalize(value.getString(KEY_CODE, ""));
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static String referralCode(Uri uri) {
        if (uri == null) return "";
        String scheme = lower(uri.getScheme());
        String host = lower(uri.getHost());
        if ("calltag".equals(scheme) && "referral".equals(host)) {
            return normalize(uri.getQueryParameter("code"));
        }
        if (("https".equals(scheme) || "http".equals(scheme))
                && ("pagero.kr".equals(host) || "www.pagero.kr".equals(host))) {
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (path.startsWith("/r/")) return normalize(path.substring(3));
            return normalize(uri.getQueryParameter("ref"));
        }
        return "";
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.KOREA)
                .replaceAll("[^A-Z0-9]", "").substring(0,
                        Math.min(20, value.trim().replaceAll("[^A-Za-z0-9]", "").length()));
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
