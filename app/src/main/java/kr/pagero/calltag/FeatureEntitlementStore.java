package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * 콜태그 기능 이용권과 결제 준비 상태의 로컬 표시 캐시다.
 * 최종 권한과 결제 가능 여부는 서버 응답을 우선한다.
 */
public final class FeatureEntitlementStore {
    public static final String PLAN_PHONE = "call_monthly";
    public static final String PLAN_MESSAGE = "message_monthly";
    public static final String PLAN_BUNDLE = "all_monthly";

    public static final int PHONE_PRICE_KRW = 1900;
    public static final int MESSAGE_PRICE_KRW = 990;
    public static final int BUNDLE_PRICE_KRW = 6000;

    public static final String CHANNEL_NONE = "none";
    public static final String CHANNEL_GOOGLE_PLAY = "google_play";
    public static final String CHANNEL_WEB = "web";

    private static final String PREFS = "calltag_entitlements";
    private static final String KEY_PLAN = "active_plan";
    private static final String KEY_SERVER_CHECKED = "server_checked";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_STATUS = "status";
    private static final String KEY_CHANNEL = "channel";
    private static final String KEY_ENDS_AT = "ends_at";
    private static final String KEY_NEXT_BILLING_AT = "next_billing_at";
    private static final String KEY_REMAINING_DAYS = "remaining_days";
    private static final String KEY_PURCHASE_BLOCKED = "purchase_blocked";
    private static final String KEY_BLOCK_REASON = "block_reason";
    private static final String KEY_PLAY_AVAILABLE = "play_billing_available";
    private static final String KEY_PLAY_STAGE = "play_billing_stage";
    private static final String KEY_PLAY_MESSAGE = "play_billing_message";
    private static final String KEY_LAST_CHECKED_AT = "last_checked_at";

    private FeatureEntitlementStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String activePlan(Context context) {
        return normalizePlan(prefs(context).getString(KEY_PLAN, PLAN_BUNDLE));
    }

    public static void setActivePlanForDevelopment(Context context, String plan) {
        String normalized = normalizePlan(plan);
        if (!PLAN_PHONE.equals(normalized)
                && !PLAN_MESSAGE.equals(normalized)
                && !PLAN_BUNDLE.equals(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 구독 상품입니다.");
        }
        prefs(context).edit().putString(KEY_PLAN, normalized).apply();
    }

    public static void saveServerEntitlement(Context context, JSONObject response) {
        JSONObject entitlement = response == null ? null : response.optJSONObject("entitlement");
        if (entitlement == null) entitlement = response == null ? new JSONObject() : response;
        JSONObject trial = entitlement.optJSONObject("trial");
        JSONObject purchase = entitlement.optJSONObject("purchase");
        JSONObject availability = entitlement.optJSONObject("billingAvailability");
        JSONObject googlePlay = availability == null ? null : availability.optJSONObject("googlePlay");

        String plan = firstNonEmpty(
                entitlement.optString("productCode", ""),
                entitlement.optString("plan", ""),
                entitlement.optString("scope", ""));
        if (trial != null && plan.isEmpty()) plan = trial.optString("scope", "");

        String status = entitlement.optString("status", "").trim();
        boolean active = entitlement.has("active")
                ? entitlement.optBoolean("active", false)
                : trial != null && trial.optBoolean("active", false);
        if (status.isEmpty()) {
            status = trial != null && trial.optBoolean("active", false)
                    ? "trial" : active ? "active" : "inactive";
        }

        String channel = firstNonEmpty(
                entitlement.optString("channel", ""),
                entitlement.optString("billingSource", ""),
                entitlement.optString("source", ""));
        if (channel.isEmpty()) channel = CHANNEL_NONE;

        String endsAt = firstNonEmpty(
                entitlement.optString("endsAt", ""),
                entitlement.optString("expiresAt", ""));
        int remainingDays = entitlement.optInt("remainingDays", -1);
        if (trial != null) {
            endsAt = firstNonEmpty(endsAt, trial.optString("endsAt", ""));
            remainingDays = trial.has("remainingDays")
                    ? trial.optInt("remainingDays", remainingDays) : remainingDays;
        }

        String nextBillingAt = firstNonEmpty(
                entitlement.optString("nextBillingAt", ""),
                entitlement.optString("renewsAt", ""));
        boolean purchaseBlocked = entitlement.optBoolean("purchaseBlocked", false);
        String blockReason = entitlement.optString("purchaseBlockReason", "");
        if (purchase != null) {
            purchaseBlocked = purchase.has("blocked")
                    ? purchase.optBoolean("blocked", purchaseBlocked) : purchaseBlocked;
            blockReason = firstNonEmpty(blockReason, purchase.optString("reason", ""));
        }
        if ((CHANNEL_WEB.equals(channel) || CHANNEL_GOOGLE_PLAY.equals(channel)) && active) {
            purchaseBlocked = true;
        }

        boolean playAvailable = googlePlay != null
                && googlePlay.optBoolean("available", false);
        String playStage = googlePlay == null
                ? "pre_registration" : googlePlay.optString("stage", "pre_registration");
        String playMessage = googlePlay == null
                ? "앱 결제 기능을 준비하고 있습니다."
                : googlePlay.optString("message", "앱 결제 기능을 준비하고 있습니다.");

        prefs(context).edit()
                .putBoolean(KEY_SERVER_CHECKED, true)
                .putBoolean(KEY_ACTIVE, active)
                .putString(KEY_STATUS, status)
                .putString(KEY_PLAN, normalizePlan(plan))
                .putString(KEY_CHANNEL, channel)
                .putString(KEY_ENDS_AT, endsAt)
                .putString(KEY_NEXT_BILLING_AT, nextBillingAt)
                .putInt(KEY_REMAINING_DAYS, remainingDays)
                .putBoolean(KEY_PURCHASE_BLOCKED, purchaseBlocked)
                .putString(KEY_BLOCK_REASON, blockReason)
                .putBoolean(KEY_PLAY_AVAILABLE, playAvailable)
                .putString(KEY_PLAY_STAGE, playStage)
                .putString(KEY_PLAY_MESSAGE, playMessage)
                .putLong(KEY_LAST_CHECKED_AT, System.currentTimeMillis())
                .apply();
    }

    public static Snapshot snapshot(Context context) {
        SharedPreferences value = prefs(context);
        boolean checked = value.getBoolean(KEY_SERVER_CHECKED, false);
        return new Snapshot(
                checked,
                checked ? value.getBoolean(KEY_ACTIVE, false) : true,
                value.getString(KEY_STATUS, checked ? "inactive" : "development"),
                normalizePlan(value.getString(KEY_PLAN, PLAN_BUNDLE)),
                value.getString(KEY_CHANNEL, CHANNEL_NONE),
                value.getString(KEY_ENDS_AT, ""),
                value.getString(KEY_NEXT_BILLING_AT, ""),
                value.getInt(KEY_REMAINING_DAYS, -1),
                value.getBoolean(KEY_PURCHASE_BLOCKED, false),
                value.getString(KEY_BLOCK_REASON, ""),
                value.getBoolean(KEY_PLAY_AVAILABLE, false),
                value.getString(KEY_PLAY_STAGE, "pre_registration"),
                value.getString(KEY_PLAY_MESSAGE, "앱 결제 기능을 준비하고 있습니다."),
                value.getLong(KEY_LAST_CHECKED_AT, 0L));
    }

    public static boolean isPlayBillingAvailable(Context context) {
        return snapshot(context).playBillingAvailable;
    }

    public static boolean hasPhoneAccess(Context context) {
        Snapshot value = snapshot(context);
        if (value.serverChecked && !value.active) return false;
        return PLAN_PHONE.equals(value.plan) || PLAN_BUNDLE.equals(value.plan);
    }

    public static boolean hasMessageAccess(Context context) {
        Snapshot value = snapshot(context);
        if (value.serverChecked && !value.active) return false;
        return PLAN_MESSAGE.equals(value.plan) || PLAN_BUNDLE.equals(value.plan);
    }

    public static String planLabel(Context context) {
        Snapshot value = snapshot(context);
        if ("trial".equals(value.status)) return "무료 이용 중";
        if (PLAN_PHONE.equals(value.plan)) return "전화관리 · 월 1,900원";
        if (PLAN_MESSAGE.equals(value.plan)) return "문자자동화 · 월 990원";
        return "통합권 · 월 6,000원";
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static String normalizePlan(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (PLAN_PHONE.equals(value) || "PHONE_1900".equals(value) || "call".equals(value)) {
            return PLAN_PHONE;
        }
        if (PLAN_MESSAGE.equals(value) || "MESSAGE_990".equals(value) || "message".equals(value)) {
            return PLAN_MESSAGE;
        }
        if (PLAN_BUNDLE.equals(value)
                || "BUNDLE_2500".equals(value)
                || "BUNDLE_6000".equals(value)
                || "all".equals(value)) {
            return PLAN_BUNDLE;
        }
        return PLAN_BUNDLE;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    public static final class Snapshot {
        public final boolean serverChecked;
        public final boolean active;
        public final String status;
        public final String plan;
        public final String channel;
        public final String endsAt;
        public final String nextBillingAt;
        public final int remainingDays;
        public final boolean purchaseBlocked;
        public final String blockReason;
        public final boolean playBillingAvailable;
        public final String playBillingStage;
        public final String playBillingMessage;
        public final long lastCheckedAt;

        Snapshot(
                boolean serverChecked,
                boolean active,
                String status,
                String plan,
                String channel,
                String endsAt,
                String nextBillingAt,
                int remainingDays,
                boolean purchaseBlocked,
                String blockReason,
                boolean playBillingAvailable,
                String playBillingStage,
                String playBillingMessage,
                long lastCheckedAt) {
            this.serverChecked = serverChecked;
            this.active = active;
            this.status = status == null ? "" : status;
            this.plan = plan == null ? PLAN_BUNDLE : plan;
            this.channel = channel == null ? CHANNEL_NONE : channel;
            this.endsAt = endsAt == null ? "" : endsAt;
            this.nextBillingAt = nextBillingAt == null ? "" : nextBillingAt;
            this.remainingDays = remainingDays;
            this.purchaseBlocked = purchaseBlocked;
            this.blockReason = blockReason == null ? "" : blockReason;
            this.playBillingAvailable = playBillingAvailable;
            this.playBillingStage = playBillingStage == null ? "pre_registration" : playBillingStage;
            this.playBillingMessage = playBillingMessage == null
                    ? "앱 결제 기능을 준비하고 있습니다." : playBillingMessage;
            this.lastCheckedAt = lastCheckedAt;
        }

        public boolean isTrial() {
            return "trial".equalsIgnoreCase(status);
        }

        public boolean isWebSubscription() {
            return CHANNEL_WEB.equals(channel) && active;
        }

        public boolean canStartPlayPurchase() {
            return serverChecked
                    && playBillingAvailable
                    && !purchaseBlocked
                    && !isWebSubscription();
        }
    }
}
