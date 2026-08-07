package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** 추천코드와 파트너 요약의 최소 표시 캐시. 정산 계산은 서버가 담당한다. */
public final class ReferralStateStore {
    private static final String PREFS = "calltag_referrals";
    private static final String KEY_CODE = "my_code";
    private static final String KEY_LINK = "share_link";
    private static final String KEY_APPLIED = "applied";
    private static final String KEY_APPLIED_CODE = "applied_code";
    private static final String KEY_BONUS_DAYS = "bonus_days";
    private static final String KEY_REFERRED_COUNT = "referred_count";
    private static final String KEY_ACTIVE_PAID_COUNT = "active_paid_count";
    private static final String KEY_ESTIMATED_REVENUE = "estimated_revenue";
    private static final String KEY_CONFIRMED_REVENUE = "confirmed_revenue";
    private static final String KEY_PARTNER_URL = "partner_url";
    private static final String KEY_LAST_CHECKED_AT = "last_checked_at";

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

        prefs(context).edit()
                .putString(KEY_CODE, code)
                .putString(KEY_LINK, link)
                .putBoolean(KEY_APPLIED, isApplied)
                .putString(KEY_APPLIED_CODE, appliedCode)
                .putInt(KEY_BONUS_DAYS, bonusDays)
                .putLong(KEY_LAST_CHECKED_AT, System.currentTimeMillis())
                .apply();
    }

    public static void saveSummary(Context context, JSONObject response) {
        JSONObject summary = response == null ? null : response.optJSONObject("summary");
        if (summary == null) summary = response == null ? new JSONObject() : response;
        prefs(context).edit()
                .putInt(KEY_REFERRED_COUNT, firstInt(summary, "referredCount", "members"))
                .putInt(KEY_ACTIVE_PAID_COUNT, firstInt(summary, "activePaidCount", "paidMembers"))
                .putLong(KEY_ESTIMATED_REVENUE,
                        firstLong(summary, "estimatedRevenueKrw", "estimatedRevenue"))
                .putLong(KEY_CONFIRMED_REVENUE,
                        firstLong(summary, "confirmedRevenueKrw", "confirmedRevenue"))
                .putString(KEY_PARTNER_URL, firstNonEmpty(
                        summary.optString("partnerCenterUrl", ""),
                        summary.optString("partnerUrl", "")))
                .putLong(KEY_LAST_CHECKED_AT, System.currentTimeMillis())
                .apply();
    }

    public static Snapshot snapshot(Context context) {
        SharedPreferences value = prefs(context);
        return new Snapshot(
                value.getString(KEY_CODE, ""),
                value.getString(KEY_LINK, ""),
                value.getBoolean(KEY_APPLIED, false),
                value.getString(KEY_APPLIED_CODE, ""),
                value.getInt(KEY_BONUS_DAYS, 0),
                value.getInt(KEY_REFERRED_COUNT, 0),
                value.getInt(KEY_ACTIVE_PAID_COUNT, 0),
                value.getLong(KEY_ESTIMATED_REVENUE, 0L),
                value.getLong(KEY_CONFIRMED_REVENUE, 0L),
                value.getString(KEY_PARTNER_URL, ""),
                value.getLong(KEY_LAST_CHECKED_AT, 0L));
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

    private static int firstInt(JSONObject source, String first, String second) {
        return source.has(first) ? source.optInt(first, 0) : source.optInt(second, 0);
    }

    private static long firstLong(JSONObject source, String first, String second) {
        return source.has(first) ? source.optLong(first, 0L) : source.optLong(second, 0L);
    }

    public static final class Snapshot {
        public final String code;
        public final String shareUrl;
        public final boolean applied;
        public final String appliedCode;
        public final int bonusDays;
        public final int referredCount;
        public final int activePaidCount;
        public final long estimatedRevenueKrw;
        public final long confirmedRevenueKrw;
        public final String partnerUrl;
        public final long lastCheckedAt;

        Snapshot(
                String code,
                String shareUrl,
                boolean applied,
                String appliedCode,
                int bonusDays,
                int referredCount,
                int activePaidCount,
                long estimatedRevenueKrw,
                long confirmedRevenueKrw,
                String partnerUrl,
                long lastCheckedAt) {
            this.code = safe(code);
            this.shareUrl = safe(shareUrl);
            this.applied = applied;
            this.appliedCode = safe(appliedCode);
            this.bonusDays = bonusDays;
            this.referredCount = referredCount;
            this.activePaidCount = activePaidCount;
            this.estimatedRevenueKrw = estimatedRevenueKrw;
            this.confirmedRevenueKrw = confirmedRevenueKrw;
            this.partnerUrl = safe(partnerUrl);
            this.lastCheckedAt = lastCheckedAt;
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
