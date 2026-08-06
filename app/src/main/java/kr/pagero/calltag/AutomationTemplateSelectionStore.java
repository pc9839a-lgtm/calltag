package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 자동발송용 템플릿 선택값을 별도 보관한다.
 * 일반 템플릿 기본값 정책과 분리해 이미지가 포함된 템플릿도 통화 후·부재중 자동발송에 쓸 수 있다.
 */
public final class AutomationTemplateSelectionStore {
    private static final String PREFS = "calltag_automation_template_selection_v1";
    private static final String KEY_PREFIX = "selected_";

    private AutomationTemplateSelectionStore() {}

    public static void set(Context context, String purpose, String templateId) {
        String normalizedPurpose = normalizePurpose(purpose);
        String id = safe(templateId);
        if (normalizedPurpose.isEmpty() || id.isEmpty()) return;
        MessageTemplateStore.Template template = MessageTemplateStore.get(context, id);
        if (template == null) return;
        prefs(context).edit().putString(KEY_PREFIX + normalizedPurpose, id).apply();
    }

    public static String id(Context context, String purpose) {
        String normalizedPurpose = normalizePurpose(purpose);
        if (normalizedPurpose.isEmpty()) return "";
        String stored = prefs(context).getString(KEY_PREFIX + normalizedPurpose, "");
        if (!safe(stored).isEmpty() && MessageTemplateStore.get(context, stored) != null) {
            return stored;
        }
        return MessageTemplateStore.defaultId(context, normalizedPurpose);
    }

    public static MessageTemplateStore.Template template(Context context, String purpose) {
        String id = id(context, purpose);
        MessageTemplateStore.Template template = MessageTemplateStore.get(context, id);
        if (template != null) return template;
        return MessageTemplateStore.defaultTemplate(context, purpose);
    }

    public static String name(Context context, String purpose) {
        MessageTemplateStore.Template template = template(context, purpose);
        if (template == null) return "선택 안 됨";
        return template.name + (safe(template.imageRef).isEmpty() ? "" : " · 사진 포함");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String normalizePurpose(String value) {
        String purpose = safe(value);
        if (MessageTemplateStore.PURPOSE_INCOMING.equals(purpose)
                || MessageTemplateStore.PURPOSE_OUTGOING.equals(purpose)
                || MessageTemplateStore.PURPOSE_MISSED.equals(purpose)
                || MessageTemplateStore.PURPOSE_FOLLOW_UP.equals(purpose)) {
            return purpose;
        }
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
