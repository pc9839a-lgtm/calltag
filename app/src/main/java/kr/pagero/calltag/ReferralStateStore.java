package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** 추천인 코드와 가입 추천 상태의 표시 캐시. */
public final class ReferralStateStore {
    private static final String PREFS = "calltag_referrals";
    private static final String KEY_CODE = "my_code";
    private static final String KEY_LINK = "share_link";
    private static final String KEY_APPLIED = "applied";
    private static final String KEY_APPLIED_CODE = "applied_code";
    private static final String KEY_BONUS_DAYS = "bonus_days";
    private static final String KEY_LAST_CHECKED_AT = "last_checked_at";
    private static final String KEY_CODE_CHECKED_AT = "code_checked_at";

    private ReferralStateStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void saveMe(Context context, JSONObject response) {
        JSONObject referral = response == null ? null : response.optJSONObject("referral");
        if (referral == null) referral = response == null ? new JSONObject() : response;

        JSONObject mine = referral.optJSONObject("mine");
        if (mine == null) mine = referral;

        JSONObject applied = referral.optJSONObject("applied");

        String code = firstNonEmpty(
                mine.optString("code", ""),
                referral.optString("code", ""));
        String link = firstNonEmpty(
                mine.optString("shareUrl", ""),
                mine.optString("link", ""),
                referral.optString("shareUrl", ""));

        boolean isApplied = referral.optBoolean("applied", false);
        String appliedCode = referral.optString("appliedCode", "");
        int bonusDays = referral.optInt("bonusDays", isApplied ? 7 : 0);

        if (applied != null) {
            isApplied = applied.optBoolean("completed", applied.optBoolean("active", true));
            appliedCode = firstNonEmpty(appliedCode, applied.optString("code", ""));
            bonusDays = applied.optInt("bonusDays", bonusDays);
        }

        long now = System.currentTimeMillis();
        prefs(context).edit()
                .putString(KEY_CODE, code)
                .putString(KEY_LINK, link)
                .putBoolean(KEY_APPLIED, isApplied)
                .putString(KEY_APPLIED_CODE, appliedCode)
                .putInt(KEY_BONUS_DAYS, bonusDays)
                .putLong(KEY_CODE_CHECKED_AT, now)
                .putLong(KEY_LAST_CHECKED_AT, now)
                .apply();
    }

    public static Snapshot snapshot(Context context) {
        SharedPreferences value = prefs(context);
        long legacyCheckedAt = value.getLong(KEY_LAST_CHECKED_AT, 0L);
        return new Snapshot(
                value.getString(KEY_CODE, ""),
                value.getString(KEY_LINK, ""),
                value.getBoolean(KEY_APPLIED, false),
                value.getString(KEY_APPLIED_CODE, ""),
                value.getInt(KEY_BONUS_DAYS, 0),
                legacyCheckedAt,
                value.getLong(KEY_CODE_CHECKED_AT, legacyCheckedAt));
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    public static final class Snapshot {
        public final String code;
        public final String shareUrl;
        public final boolean applied;
        public final String appliedCode;
        public final int bonusDays;
        public final long lastCheckedAt;
        public final long codeCheckedAt;

        Snapshot(
                String code,
                String shareUrl,
                boolean applied,
                String appliedCode,
                int bonusDays,
                long lastCheckedAt,
                long codeCheckedAt) {
            this.code = safe(code);
            this.shareUrl = safe(shareUrl);
            this.applied = applied;
            this.appliedCode = safe(appliedCode);
            this.bonusDays = bonusDays;
            this.lastCheckedAt = lastCheckedAt;
            this.codeCheckedAt = codeCheckedAt;
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
