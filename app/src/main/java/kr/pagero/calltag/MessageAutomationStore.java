package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SubscriptionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MessageAutomationStore {
    private static final String PREFS = "calltag_message_automation";

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_CONNECTED_ENABLED = "connected_enabled";
    private static final String KEY_MISSED_ENABLED = "missed_enabled";
    private static final String KEY_DELAYED_ENABLED = "delayed_enabled";
    private static final String KEY_CONNECTED_TEMPLATE = "connected_template";
    private static final String KEY_MISSED_TEMPLATE = "missed_template";
    private static final String KEY_DELAYED_TEMPLATE = "delayed_template";
    private static final String KEY_DELAY_DAYS = "delay_days";
    private static final String KEY_COOLDOWN_HOURS = "cooldown_hours";
    private static final String KEY_SELECTED_SUBSCRIPTION_ID = "selected_subscription_id";
    private static final String KEY_BUSINESS_HOURS_ENABLED = "business_hours_enabled";
    private static final String KEY_START_HOUR = "start_hour";
    private static final String KEY_END_HOUR = "end_hour";

    public static final String DEFAULT_CONNECTED_TEMPLATE =
            "안녕하세요, {고객명}님. 방금 통화드린 콜태그입니다. 안내드린 내용을 확인해주세요.";
    public static final String DEFAULT_MISSED_TEMPLATE =
            "안녕하세요. 전화를 받지 못했습니다. 확인 후 다시 연락드리겠습니다.";
    public static final String DEFAULT_DELAYED_TEMPLATE =
            "안녕하세요, {고객명}님. 지난 상담 내용은 검토해보셨을까요? 궁금한 점이 있으면 편하게 연락주세요.";

    private MessageAutomationStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void ensureDefaults(Context context) {
        SharedPreferences p = prefs(context);
        SharedPreferences.Editor editor = p.edit();
        if (!p.contains(KEY_ENABLED)) editor.putBoolean(KEY_ENABLED, true);
        if (!p.contains(KEY_CONNECTED_ENABLED)) editor.putBoolean(KEY_CONNECTED_ENABLED, false);
        if (!p.contains(KEY_MISSED_ENABLED)) editor.putBoolean(KEY_MISSED_ENABLED, false);
        if (!p.contains(KEY_DELAYED_ENABLED)) editor.putBoolean(KEY_DELAYED_ENABLED, false);
        if (!p.contains(KEY_CONNECTED_TEMPLATE)) editor.putString(KEY_CONNECTED_TEMPLATE, DEFAULT_CONNECTED_TEMPLATE);
        if (!p.contains(KEY_MISSED_TEMPLATE)) editor.putString(KEY_MISSED_TEMPLATE, DEFAULT_MISSED_TEMPLATE);
        if (!p.contains(KEY_DELAYED_TEMPLATE)) editor.putString(KEY_DELAYED_TEMPLATE, DEFAULT_DELAYED_TEMPLATE);
        if (!p.contains(KEY_DELAY_DAYS)) editor.putInt(KEY_DELAY_DAYS, 3);
        if (!p.contains(KEY_COOLDOWN_HOURS)) editor.putInt(KEY_COOLDOWN_HOURS, 24);
        if (!p.contains(KEY_SELECTED_SUBSCRIPTION_ID)) {
            editor.putInt(KEY_SELECTED_SUBSCRIPTION_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
        if (!p.contains(KEY_BUSINESS_HOURS_ENABLED)) editor.putBoolean(KEY_BUSINESS_HOURS_ENABLED, false);
        if (!p.contains(KEY_START_HOUR)) editor.putInt(KEY_START_HOUR, 9);
        if (!p.contains(KEY_END_HOUR)) editor.putInt(KEY_END_HOUR, 20);
        editor.apply();
    }

    public static boolean isEnabled(Context context) {
        ensureDefaults(context);
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean connectedEnabled(Context context) {
        ensureDefaults(context);
        return prefs(context).getBoolean(KEY_CONNECTED_ENABLED, false);
    }

    public static void setConnectedEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_CONNECTED_ENABLED, enabled).apply();
    }

    public static boolean missedEnabled(Context context) {
        ensureDefaults(context);
        return prefs(context).getBoolean(KEY_MISSED_ENABLED, false);
    }

    public static void setMissedEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_MISSED_ENABLED, enabled).apply();
    }

    public static boolean delayedEnabled(Context context) {
        ensureDefaults(context);
        return prefs(context).getBoolean(KEY_DELAYED_ENABLED, false);
    }

    public static void setDelayedEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DELAYED_ENABLED, enabled).apply();
    }

    public static String connectedTemplate(Context context) {
        ensureDefaults(context);
        return prefs(context).getString(KEY_CONNECTED_TEMPLATE, DEFAULT_CONNECTED_TEMPLATE);
    }

    public static void setConnectedTemplate(Context context, String value) {
        prefs(context).edit().putString(KEY_CONNECTED_TEMPLATE, clean(value, DEFAULT_CONNECTED_TEMPLATE)).apply();
    }

    public static String missedTemplate(Context context) {
        ensureDefaults(context);
        return prefs(context).getString(KEY_MISSED_TEMPLATE, DEFAULT_MISSED_TEMPLATE);
    }

    public static void setMissedTemplate(Context context, String value) {
        prefs(context).edit().putString(KEY_MISSED_TEMPLATE, clean(value, DEFAULT_MISSED_TEMPLATE)).apply();
    }

    public static String delayedTemplate(Context context) {
        ensureDefaults(context);
        return prefs(context).getString(KEY_DELAYED_TEMPLATE, DEFAULT_DELAYED_TEMPLATE);
    }

    public static void setDelayedTemplate(Context context, String value) {
        prefs(context).edit().putString(KEY_DELAYED_TEMPLATE, clean(value, DEFAULT_DELAYED_TEMPLATE)).apply();
    }

    public static int delayDays(Context context) {
        ensureDefaults(context);
        return Math.max(1, Math.min(30, prefs(context).getInt(KEY_DELAY_DAYS, 3)));
    }

    public static void setDelayDays(Context context, int days) {
        prefs(context).edit().putInt(KEY_DELAY_DAYS, Math.max(1, Math.min(30, days))).apply();
    }

    public static int cooldownHours(Context context) {
        ensureDefaults(context);
        return Math.max(1, Math.min(24 * 30, prefs(context).getInt(KEY_COOLDOWN_HOURS, 24)));
    }

    public static void setCooldownHours(Context context, int hours) {
        prefs(context).edit().putInt(KEY_COOLDOWN_HOURS, Math.max(1, Math.min(24 * 30, hours))).apply();
    }

    public static int selectedSubscriptionId(Context context) {
        ensureDefaults(context);
        int saved = prefs(context).getInt(KEY_SELECTED_SUBSCRIPTION_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        if (SubscriptionManager.isValidSubscriptionId(saved)) return saved;
        return SimProfileManager.selectedOrDefault(context);
    }

    public static void setSelectedSubscriptionId(Context context, int subscriptionId) {
        prefs(context).edit().putInt(KEY_SELECTED_SUBSCRIPTION_ID, subscriptionId).apply();
    }

    public static boolean businessHoursEnabled(Context context) {
        ensureDefaults(context);
        return prefs(context).getBoolean(KEY_BUSINESS_HOURS_ENABLED, false);
    }

    public static void setBusinessHoursEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_BUSINESS_HOURS_ENABLED, enabled).apply();
    }

    public static int startHour(Context context) {
        ensureDefaults(context);
        return Math.max(0, Math.min(23, prefs(context).getInt(KEY_START_HOUR, 9)));
    }

    public static int endHour(Context context) {
        ensureDefaults(context);
        return Math.max(0, Math.min(23, prefs(context).getInt(KEY_END_HOUR, 20)));
    }

    public static void setBusinessHours(Context context, int startHour, int endHour) {
        prefs(context).edit()
                .putInt(KEY_START_HOUR, Math.max(0, Math.min(23, startHour)))
                .putInt(KEY_END_HOUR, Math.max(0, Math.min(23, endHour)))
                .apply();
    }

    public static boolean isWithinBusinessHours(Context context, long now) {
        if (!businessHoursEnabled(context)) return true;
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(now);
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int start = startHour(context);
        int end = endHour(context);
        if (start == end) return true;
        if (start < end) return hour >= start && hour < end;
        return hour >= start || hour < end;
    }

    public static String buildMessage(String template, Customer customer, CallRecord record) {
        String customerName = customer == null ? "고객" : cleanCustomerName(customer.displayName);
        String phone = record == null ? "" : safe(record.phone);
        long time = record == null ? System.currentTimeMillis() : record.startedAt;
        String dateText = new SimpleDateFormat("M월 d일", Locale.KOREA).format(new Date(time));
        String timeText = new SimpleDateFormat("a h:mm", Locale.KOREA).format(new Date(time));
        return safe(template)
                .replace("{고객명}", customerName)
                .replace("{전화번호}", phone)
                .replace("{날짜}", dateText)
                .replace("{시간}", timeText)
                .trim();
    }

    private static String clean(String value, String fallback) {
        String result = safe(value).trim();
        return result.isEmpty() ? fallback : result;
    }

    private static String cleanCustomerName(String value) {
        String result = safe(value).trim();
        if (result.isEmpty() || "이름없는고객".equals(result) || "이름 없음".equals(result)) return "고객";
        return result;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
