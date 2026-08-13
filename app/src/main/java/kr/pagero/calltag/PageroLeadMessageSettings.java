package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

/** 페이지로 신규 문의 전용 자동문자 설정. 기본값은 반드시 OFF다. */
public final class PageroLeadMessageSettings {
    public static final String DEFAULT_TEMPLATE =
            "{고객명}님, {페이지명}을 통해 문의가 정상 접수되었습니다.\n담당자가 확인 후 연락드리겠습니다.";

    private static final String PREFS = "calltag_pagero_lead_message";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_DEFAULT_TEMPLATE = "default_template";
    private static final String KEY_DELAY_MINUTES = "delay_minutes";
    private static final String SITE_TEMPLATE_PREFIX = "site_template_";

    private PageroLeadMessageSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static String defaultTemplate(Context context) {
        String value = prefs(context).getString(KEY_DEFAULT_TEMPLATE, DEFAULT_TEMPLATE);
        return value == null || value.trim().isEmpty() ? DEFAULT_TEMPLATE : value.trim();
    }

    public static void setDefaultTemplate(Context context, String template) {
        String value = template == null ? "" : template.trim();
        prefs(context).edit().putString(KEY_DEFAULT_TEMPLATE,
                value.isEmpty() ? DEFAULT_TEMPLATE : value).apply();
    }

    public static String templateFor(Context context, String siteId) {
        String key = siteKey(siteId);
        if (!key.isEmpty()) {
            String site = prefs(context).getString(SITE_TEMPLATE_PREFIX + key, "");
            if (site != null && !site.trim().isEmpty()) return site.trim();
        }
        return defaultTemplate(context);
    }

    public static void setTemplateFor(Context context, String siteId, String template) {
        String key = siteKey(siteId);
        if (key.isEmpty()) return;
        String value = template == null ? "" : template.trim();
        SharedPreferences.Editor edit = prefs(context).edit();
        if (value.isEmpty()) edit.remove(SITE_TEMPLATE_PREFIX + key);
        else edit.putString(SITE_TEMPLATE_PREFIX + key, value);
        edit.apply();
    }

    public static int delayMinutes(Context context) {
        return Math.max(0, Math.min(1440, prefs(context).getInt(KEY_DELAY_MINUTES, 0)));
    }

    public static void setDelayMinutes(Context context, int value) {
        prefs(context).edit().putInt(KEY_DELAY_MINUTES, Math.max(0, Math.min(1440, value))).apply();
    }

    private static String siteKey(String siteId) {
        String value = siteId == null ? "" : siteId.trim();
        if (value.isEmpty()) return "";
        return MessageDedupeEngine.sha256(value).substring(0, 24);
    }
}
