package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;

/**
 * 콜태그 기능 이용권과 결제 준비 상태의 로컬 표시 캐시다.
 * 서버 시각을 기준으로 만료를 계산해 기기 시각 변경이나 오프라인 상태에서도
 * 무료기간이 무기한 연장되지 않도록 한다.
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

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
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
    private static final String KEY_SERVER_NOW_AT_CHECK = "server_now_at_check";
    private static final String KEY_DEVICE_NOW_AT_CHECK = "device_now_at_check";
    private static final String KEY_NOTICE_CODE = "notice_code";
    private static final String KEY_NOTICE_TITLE = "notice_title";
    private static final String KEY_NOTICE_MESSAGE = "notice_message";
    private static final String KEY_LAST_CHECKED_AT = "last_checked_at";
    private static final String KEY_PHONE_SUBSCRIBED = "phone_subscribed";
    private static final String KEY_MESSAGE_SUBSCRIBED = "message_subscribed";
    private static final String KEY_PHONE_NEXT_BILLING_AT = "phone_next_billing_at";
    private static final String KEY_MESSAGE_NEXT_BILLING_AT = "message_next_billing_at";
    private static final String KEY_PHONE_PURCHASE_AVAILABLE = "phone_purchase_available";
    private static final String KEY_MESSAGE_PURCHASE_AVAILABLE = "message_purchase_available";

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
        JSONObject notice = entitlement.optJSONObject("notice");
        JSONObject productAccess = entitlement.optJSONObject("productAccess");
        JSONObject purchaseOptions = entitlement.optJSONObject("purchaseOptions");

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

        boolean playAvailable = googlePlay != null
                && googlePlay.optBoolean("available", false);
        String playStage = googlePlay == null
                ? "pre_registration" : googlePlay.optString("stage", "pre_registration");
        String playMessage = googlePlay == null
                ? "앱 결제 기능을 준비하고 있습니다."
                : googlePlay.optString("message", "앱 결제 기능을 준비하고 있습니다.");

        boolean phoneSubscribed = false;
        boolean messageSubscribed = false;
        JSONArray activeProducts = entitlement.optJSONArray("activeProducts");
        if (activeProducts != null) {
            for (int index = 0; index < activeProducts.length(); index++) {
                String product = activeProducts.optString(index, "");
                if (PLAN_PHONE.equals(product)) phoneSubscribed = true;
                if (PLAN_MESSAGE.equals(product)) messageSubscribed = true;
            }
        }

        String phoneNextBillingAt = "";
        String messageNextBillingAt = "";
        if (productAccess != null) {
            JSONObject phone = productAccess.optJSONObject(PLAN_PHONE);
            JSONObject message = productAccess.optJSONObject(PLAN_MESSAGE);
            if (phone != null) {
                phoneSubscribed = phone.optBoolean("active", phoneSubscribed);
                phoneNextBillingAt = firstNonEmpty(
                        phone.optString("nextBillingAt", ""),
                        phone.optString("expiresAt", ""));
            }
            if (message != null) {
                messageSubscribed = message.optBoolean("active", messageSubscribed);
                messageNextBillingAt = firstNonEmpty(
                        message.optString("nextBillingAt", ""),
                        message.optString("expiresAt", ""));
            }
        }

        boolean paid = active && !"trial".equalsIgnoreCase(status)
                && !CHANNEL_NONE.equals(channel);
        if (activeProducts == null && productAccess == null && paid) {
            String normalizedPlan = normalizePlan(plan);
            if (PLAN_PHONE.equals(normalizedPlan)) phoneSubscribed = true;
            if (PLAN_MESSAGE.equals(normalizedPlan)) messageSubscribed = true;
            if (PLAN_BUNDLE.equals(normalizedPlan)) {
                phoneSubscribed = true;
                messageSubscribed = true;
            }
        }
        if (phoneSubscribed && phoneNextBillingAt.isEmpty()
                && PLAN_PHONE.equals(normalizePlan(plan))) {
            phoneNextBillingAt = nextBillingAt;
        }
        if (messageSubscribed && messageNextBillingAt.isEmpty()
                && PLAN_MESSAGE.equals(normalizePlan(plan))) {
            messageNextBillingAt = nextBillingAt;
        }

        boolean phonePurchaseAvailable = playAvailable
                && !purchaseBlocked && !phoneSubscribed && !CHANNEL_WEB.equals(channel);
        boolean messagePurchaseAvailable = playAvailable
                && !purchaseBlocked && !messageSubscribed && !CHANNEL_WEB.equals(channel);
        if (purchaseOptions != null) {
            JSONObject phone = purchaseOptions.optJSONObject(PLAN_PHONE);
            JSONObject message = purchaseOptions.optJSONObject(PLAN_MESSAGE);
            if (phone != null && phone.has("available")) {
                phonePurchaseAvailable = phone.optBoolean("available", phonePurchaseAvailable);
            }
            if (message != null && message.has("available")) {
                messagePurchaseAvailable = message.optBoolean("available", messagePurchaseAvailable);
            }
        }

        long deviceNow = System.currentTimeMillis();
        String rawServerNow = firstNonEmpty(
                response == null ? "" : response.optString("serverNow", ""),
                entitlement.optString("serverNow", ""));
        long serverNow = parseInstant(rawServerNow);
        if (serverNow <= 0L) serverNow = deviceNow;

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
                .putLong(KEY_SERVER_NOW_AT_CHECK, serverNow)
                .putLong(KEY_DEVICE_NOW_AT_CHECK, deviceNow)
                .putString(KEY_NOTICE_CODE, notice == null ? "" : notice.optString("code", ""))
                .putString(KEY_NOTICE_TITLE, notice == null ? "" : notice.optString("title", ""))
                .putString(KEY_NOTICE_MESSAGE, notice == null ? "" : notice.optString("message", ""))
                .putLong(KEY_LAST_CHECKED_AT, deviceNow)
                .putBoolean(KEY_PHONE_SUBSCRIBED, phoneSubscribed)
                .putBoolean(KEY_MESSAGE_SUBSCRIBED, messageSubscribed)
                .putString(KEY_PHONE_NEXT_BILLING_AT, phoneNextBillingAt)
                .putString(KEY_MESSAGE_NEXT_BILLING_AT, messageNextBillingAt)
                .putBoolean(KEY_PHONE_PURCHASE_AVAILABLE, phonePurchaseAvailable)
                .putBoolean(KEY_MESSAGE_PURCHASE_AVAILABLE, messagePurchaseAvailable)
                .apply();
    }

    public static Snapshot snapshot(Context context) {
        SharedPreferences value = prefs(context);
        boolean checked = value.getBoolean(KEY_SERVER_CHECKED, false);
        long estimatedServerNow = estimatedServerNow(value);
        String endsAt = value.getString(KEY_ENDS_AT, "");
        long endsAtMillis = parseInstant(endsAt);
        boolean active = checked ? value.getBoolean(KEY_ACTIVE, false) : true;
        String status = value.getString(KEY_STATUS, checked ? "inactive" : "development");
        int remainingDays = value.getInt(KEY_REMAINING_DAYS, -1);

        if (checked && endsAtMillis > 0L) {
            remainingDays = endsAtMillis <= estimatedServerNow
                    ? 0 : (int) Math.ceil((endsAtMillis - estimatedServerNow) / (double) DAY_MS);
            if (active && estimatedServerNow >= endsAtMillis
                    && !value.getBoolean(KEY_PHONE_SUBSCRIBED, false)
                    && !value.getBoolean(KEY_MESSAGE_SUBSCRIBED, false)) {
                active = false;
            }
            if (!active && estimatedServerNow >= endsAtMillis
                    && ("trial".equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status))) {
                status = "expired";
            }
        }

        return new Snapshot(
                checked,
                active,
                status,
                normalizePlan(value.getString(KEY_PLAN, PLAN_BUNDLE)),
                value.getString(KEY_CHANNEL, CHANNEL_NONE),
                endsAt,
                value.getString(KEY_NEXT_BILLING_AT, ""),
                remainingDays,
                value.getBoolean(KEY_PURCHASE_BLOCKED, false),
                value.getString(KEY_BLOCK_REASON, ""),
                value.getBoolean(KEY_PLAY_AVAILABLE, false),
                value.getString(KEY_PLAY_STAGE, "pre_registration"),
                value.getString(KEY_PLAY_MESSAGE, "앱 결제 기능을 준비하고 있습니다."),
                estimatedServerNow,
                value.getString(KEY_NOTICE_CODE, ""),
                value.getString(KEY_NOTICE_TITLE, ""),
                value.getString(KEY_NOTICE_MESSAGE, ""),
                value.getLong(KEY_LAST_CHECKED_AT, 0L),
                value.getBoolean(KEY_PHONE_SUBSCRIBED, false),
                value.getBoolean(KEY_MESSAGE_SUBSCRIBED, false),
                value.getString(KEY_PHONE_NEXT_BILLING_AT, ""),
                value.getString(KEY_MESSAGE_NEXT_BILLING_AT, ""),
                value.getBoolean(KEY_PHONE_PURCHASE_AVAILABLE, false),
                value.getBoolean(KEY_MESSAGE_PURCHASE_AVAILABLE, false));
    }

    public static boolean isPlayBillingAvailable(Context context) {
        return snapshot(context).playBillingAvailable;
    }

    public static boolean hasPhoneAccess(Context context) {
        Snapshot value = snapshot(context);
        if (value.isTrial()) return true;
        return value.phoneSubscribed || (value.active && PLAN_BUNDLE.equals(value.plan));
    }

    public static boolean hasMessageAccess(Context context) {
        Snapshot value = snapshot(context);
        if (value.isTrial()) return true;
        return value.messageSubscribed || (value.active && PLAN_BUNDLE.equals(value.plan));
    }

    public static String planLabel(Context context) {
        Snapshot value = snapshot(context);
        if (value.isTrial()) return "무료 이용 중";
        if (value.phoneSubscribed && value.messageSubscribed) return "전화관리 · 문자자동화 이용 중";
        if (value.phoneSubscribed) return "전화관리 이용 중";
        if (value.messageSubscribed) return "문자자동화 이용 중";
        if (!value.active && value.serverChecked) return "이용권 필요";
        return "이용권 확인 중";
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static long estimatedServerNow(SharedPreferences value) {
        long deviceNow = System.currentTimeMillis();
        long serverAtCheck = value.getLong(KEY_SERVER_NOW_AT_CHECK, 0L);
        long deviceAtCheck = value.getLong(KEY_DEVICE_NOW_AT_CHECK, 0L);
        if (serverAtCheck <= 0L || deviceAtCheck <= 0L) return deviceNow;
        return serverAtCheck + Math.max(0L, deviceNow - deviceAtCheck);
    }

    private static long parseInstant(String raw) {
        try {
            return raw == null || raw.trim().isEmpty() ? 0L : Instant.parse(raw.trim()).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
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
        public final long estimatedServerNow;
        public final String noticeCode;
        public final String noticeTitle;
        public final String noticeMessage;
        public final long lastCheckedAt;
        public final boolean phoneSubscribed;
        public final boolean messageSubscribed;
        public final String phoneNextBillingAt;
        public final String messageNextBillingAt;
        public final boolean phonePurchaseAvailable;
        public final boolean messagePurchaseAvailable;

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
                long estimatedServerNow,
                String noticeCode,
                String noticeTitle,
                String noticeMessage,
                long lastCheckedAt,
                boolean phoneSubscribed,
                boolean messageSubscribed,
                String phoneNextBillingAt,
                String messageNextBillingAt,
                boolean phonePurchaseAvailable,
                boolean messagePurchaseAvailable) {
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
            this.estimatedServerNow = estimatedServerNow;
            this.noticeCode = noticeCode == null ? "" : noticeCode;
            this.noticeTitle = noticeTitle == null ? "" : noticeTitle;
            this.noticeMessage = noticeMessage == null ? "" : noticeMessage;
            this.lastCheckedAt = lastCheckedAt;
            this.phoneSubscribed = phoneSubscribed;
            this.messageSubscribed = messageSubscribed;
            this.phoneNextBillingAt = phoneNextBillingAt == null ? "" : phoneNextBillingAt;
            this.messageNextBillingAt = messageNextBillingAt == null ? "" : messageNextBillingAt;
            this.phonePurchaseAvailable = phonePurchaseAvailable;
            this.messagePurchaseAvailable = messagePurchaseAvailable;
        }

        public boolean isTrial() {
            return "trial".equalsIgnoreCase(status) && active;
        }

        public boolean isExpired() {
            return serverChecked && !active && "expired".equalsIgnoreCase(status);
        }

        public boolean isTrialEndingSoon() {
            return isTrial() && remainingDays <= 1;
        }

        public boolean isWebSubscription() {
            return CHANNEL_WEB.equals(channel) && active;
        }

        public boolean isProductSubscribed(String productId) {
            if (PLAN_PHONE.equals(productId)) return phoneSubscribed;
            if (PLAN_MESSAGE.equals(productId)) return messageSubscribed;
            return phoneSubscribed && messageSubscribed;
        }

        public String nextBillingAtFor(String productId) {
            if (PLAN_PHONE.equals(productId)) return phoneNextBillingAt;
            if (PLAN_MESSAGE.equals(productId)) return messageNextBillingAt;
            return nextBillingAt;
        }

        public boolean canStartPlayPurchase(String productId) {
            if (!serverChecked || !playBillingAvailable || isWebSubscription()) return false;
            if (PLAN_PHONE.equals(productId)) return phonePurchaseAvailable && !phoneSubscribed;
            if (PLAN_MESSAGE.equals(productId)) return messagePurchaseAvailable && !messageSubscribed;
            return false;
        }

        public boolean canStartPlayPurchase() {
            return canStartPlayPurchase(PLAN_PHONE) || canStartPlayPurchase(PLAN_MESSAGE);
        }
    }
}
