package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MessageTemplateStore {
    public static final String PURPOSE_INCOMING = "incoming";
    public static final String PURPOSE_OUTGOING = "outgoing";
    public static final String PURPOSE_MISSED = "missed";
    public static final String PURPOSE_FOLLOW_UP = "follow_up";
    public static final String PURPOSE_GENERAL = "general";

    private static final String PREFS = "calltag_message_templates_v1";
    private static final String KEY_TEMPLATES = "templates_json";
    private static final String KEY_DEFAULT_PREFIX = "default_";
    private static final String LEGACY_PREFS = "calltag_message_automation";

    private MessageTemplateStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized void ensureDefaults(Context context) {
        List<Template> templates = readAll(context);
        if (templates.isEmpty()) {
            SharedPreferences legacy = context.getApplicationContext()
                    .getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
            String connected = MessageTemplateEngine.normalizeLegacyAliases(legacy.getString(
                    "connected_template", MessageAutomationStore.DEFAULT_CONNECTED_TEMPLATE));
            String missed = MessageTemplateEngine.normalizeLegacyAliases(legacy.getString(
                    "missed_template", MessageAutomationStore.DEFAULT_MISSED_TEMPLATE));
            String followUp = MessageTemplateEngine.normalizeLegacyAliases(legacy.getString(
                    "delayed_template", MessageAutomationStore.DEFAULT_DELAYED_TEMPLATE));
            long now = System.currentTimeMillis();

            Template incoming = new Template(newId(), "수신 통화 안내", connected,
                    "통화 안내", PURPOSE_INCOMING, true, now, now, 0L, 0);
            Template outgoing = new Template(newId(), "발신 통화 안내", connected,
                    "통화 안내", PURPOSE_OUTGOING, true, now + 1L, now + 1L, 0L, 0);
            Template missedTemplate = new Template(newId(), "부재중 안내", missed,
                    "부재중", PURPOSE_MISSED, true, now + 2L, now + 2L, 0L, 0);
            Template followUpTemplate = new Template(newId(), "후속문자", followUp,
                    "후속", PURPOSE_FOLLOW_UP, true, now + 3L, now + 3L, 0L, 0);
            templates.add(incoming);
            templates.add(outgoing);
            templates.add(missedTemplate);
            templates.add(followUpTemplate);
            writeAll(context, templates);
            SharedPreferences.Editor editor = prefs(context).edit();
            editor.putString(defaultKey(PURPOSE_INCOMING), incoming.id);
            editor.putString(defaultKey(PURPOSE_OUTGOING), outgoing.id);
            editor.putString(defaultKey(PURPOSE_MISSED), missedTemplate.id);
            editor.putString(defaultKey(PURPOSE_FOLLOW_UP), followUpTemplate.id);
            editor.apply();
            return;
        }

        SharedPreferences p = prefs(context);
        SharedPreferences.Editor editor = p.edit();
        boolean changed = false;
        for (String purpose : defaultPurposes()) {
            String key = defaultKey(purpose);
            String current = p.getString(key, "");
            if (findById(templates, current) == null) {
                Template replacement = firstForPurpose(templates, purpose);
                if (replacement != null) {
                    editor.putString(key, replacement.id);
                    changed = true;
                }
            }
        }
        if (changed) editor.apply();
    }

    public static synchronized List<Template> list(Context context, String query, String purpose) {
        ensureDefaults(context);
        String normalizedQuery = safe(query).trim().toLowerCase(Locale.KOREA);
        String normalizedPurpose = normalizePurpose(purpose);
        List<Template> result = new ArrayList<>();
        for (Template template : readAll(context)) {
            boolean purposeMatches = normalizedPurpose.isEmpty()
                    || normalizedPurpose.equals(template.purpose)
                    || PURPOSE_GENERAL.equals(template.purpose);
            if (!purposeMatches) continue;
            if (!normalizedQuery.isEmpty()) {
                String haystack = (template.name + " " + template.body + " " + template.category
                        + " " + purposeLabel(template.purpose)).toLowerCase(Locale.KOREA);
                if (!haystack.contains(normalizedQuery)) continue;
            }
            result.add(template.copy());
        }
        Collections.sort(result, TEMPLATE_ORDER);
        return result;
    }

    public static synchronized Template get(Context context, String id) {
        ensureDefaults(context);
        Template template = findById(readAll(context), id);
        return template == null ? null : template.copy();
    }

    public static synchronized Template save(Context context, Template value) {
        ensureDefaults(context);
        if (value == null) throw new IllegalArgumentException("템플릿 정보가 없습니다.");
        MessageTemplateEngine.ValidationResult validation =
                MessageTemplateEngine.validateTemplate(value.body);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("지원하지 않는 변수가 있습니다: "
                    + MessageTemplateEngine.describeVariables(validation.unsupportedVariables));
        }
        String body = validation.normalizedTemplate.trim();
        if (body.isEmpty()) throw new IllegalArgumentException("문자 내용을 입력해주세요.");

        List<Template> templates = readAll(context);
        long now = System.currentTimeMillis();
        Template existing = findById(templates, value.id);
        String id = safe(value.id).trim().isEmpty() ? newId() : value.id;
        long createdAt = existing == null ? now : existing.createdAt;
        long lastUsedAt = existing == null ? 0L : existing.lastUsedAt;
        int useCount = existing == null ? 0 : existing.useCount;
        Template saved = new Template(
                id,
                clean(value.name, "새 템플릿"),
                body,
                clean(value.category, "기타"),
                normalizePurposeOrGeneral(value.purpose),
                value.favorite,
                createdAt,
                now,
                lastUsedAt,
                useCount
        );
        if (existing == null) {
            templates.add(saved);
        } else {
            int index = indexOf(templates, existing.id);
            templates.set(index, saved);
        }
        writeAll(context, templates);
        return saved.copy();
    }

    public static synchronized Template duplicate(Context context, String id) {
        Template source = get(context, id);
        if (source == null) return null;
        source.id = "";
        source.name = source.name + " 복사본";
        source.favorite = false;
        source.createdAt = 0L;
        source.updatedAt = 0L;
        source.lastUsedAt = 0L;
        source.useCount = 0;
        return save(context, source);
    }

    public static synchronized boolean delete(Context context, String id) {
        ensureDefaults(context);
        if (safe(id).trim().isEmpty() || isDefault(context, id)) return false;
        List<Template> templates = readAll(context);
        int index = indexOf(templates, id);
        if (index < 0) return false;
        templates.remove(index);
        writeAll(context, templates);
        return true;
    }

    public static synchronized void setFavorite(Context context, String id, boolean favorite) {
        ensureDefaults(context);
        List<Template> templates = readAll(context);
        Template target = findById(templates, id);
        if (target == null) return;
        target.favorite = favorite;
        target.updatedAt = System.currentTimeMillis();
        writeAll(context, templates);
    }

    public static synchronized void markUsed(Context context, String id) {
        ensureDefaults(context);
        List<Template> templates = readAll(context);
        Template target = findById(templates, id);
        if (target == null) return;
        target.lastUsedAt = System.currentTimeMillis();
        target.useCount = Math.max(0, target.useCount) + 1;
        writeAll(context, templates);
    }

    public static synchronized boolean setDefault(Context context, String purpose, String id) {
        ensureDefaults(context);
        String normalizedPurpose = normalizePurpose(purpose);
        if (normalizedPurpose.isEmpty() || PURPOSE_GENERAL.equals(normalizedPurpose)) return false;
        if (findById(readAll(context), id) == null) return false;
        prefs(context).edit().putString(defaultKey(normalizedPurpose), id).apply();
        return true;
    }

    public static synchronized String defaultId(Context context, String purpose) {
        ensureDefaults(context);
        String normalizedPurpose = normalizePurpose(purpose);
        return prefs(context).getString(defaultKey(normalizedPurpose), "");
    }

    public static synchronized Template defaultTemplate(Context context, String purpose) {
        ensureDefaults(context);
        List<Template> templates = readAll(context);
        String normalizedPurpose = normalizePurpose(purpose);
        Template byId = findById(templates, prefs(context).getString(
                defaultKey(normalizedPurpose), ""));
        if (byId != null) return byId.copy();
        Template fallback = firstForPurpose(templates, normalizedPurpose);
        return fallback == null ? null : fallback.copy();
    }

    public static synchronized String defaultBody(Context context, String purpose, String fallback) {
        Template template = defaultTemplate(context, purpose);
        return template == null ? fallback : clean(template.body, fallback);
    }

    public static synchronized String defaultName(Context context, String purpose) {
        Template template = defaultTemplate(context, purpose);
        return template == null ? "선택 안 됨" : template.name;
    }

    public static synchronized boolean updateDefaultBody(Context context, String purpose, String body) {
        Template template = defaultTemplate(context, purpose);
        if (template == null) return false;
        template.body = body;
        save(context, template);
        return true;
    }

    public static synchronized boolean isDefault(Context context, String id) {
        ensureDefaults(context);
        if (safe(id).trim().isEmpty()) return false;
        SharedPreferences p = prefs(context);
        for (String purpose : defaultPurposes()) {
            if (id.equals(p.getString(defaultKey(purpose), ""))) return true;
        }
        return false;
    }

    public static synchronized String defaultUsageLabel(Context context, String id) {
        ensureDefaults(context);
        List<String> labels = new ArrayList<>();
        SharedPreferences p = prefs(context);
        for (String purpose : defaultPurposes()) {
            if (id.equals(p.getString(defaultKey(purpose), ""))) {
                labels.add(purposeLabel(purpose));
            }
        }
        return labels.isEmpty() ? "" : String.join(" · ", labels) + " 기본";
    }

    public static String purposeLabel(String purpose) {
        switch (normalizePurposeOrGeneral(purpose)) {
            case PURPOSE_INCOMING:
                return "수신";
            case PURPOSE_OUTGOING:
                return "발신";
            case PURPOSE_MISSED:
                return "부재중";
            case PURPOSE_FOLLOW_UP:
                return "후속";
            default:
                return "일반";
        }
    }

    public static List<String> purposeValues() {
        List<String> values = new ArrayList<>();
        values.add(PURPOSE_INCOMING);
        values.add(PURPOSE_OUTGOING);
        values.add(PURPOSE_MISSED);
        values.add(PURPOSE_FOLLOW_UP);
        values.add(PURPOSE_GENERAL);
        return values;
    }

    private static List<String> defaultPurposes() {
        List<String> values = new ArrayList<>();
        values.add(PURPOSE_INCOMING);
        values.add(PURPOSE_OUTGOING);
        values.add(PURPOSE_MISSED);
        values.add(PURPOSE_FOLLOW_UP);
        return values;
    }

    private static String defaultKey(String purpose) {
        return KEY_DEFAULT_PREFIX + normalizePurposeOrGeneral(purpose);
    }

    private static String normalizePurpose(String purpose) {
        String normalized = safe(purpose).trim();
        if (PURPOSE_INCOMING.equals(normalized)
                || PURPOSE_OUTGOING.equals(normalized)
                || PURPOSE_MISSED.equals(normalized)
                || PURPOSE_FOLLOW_UP.equals(normalized)
                || PURPOSE_GENERAL.equals(normalized)) return normalized;
        return "";
    }

    private static String normalizePurposeOrGeneral(String purpose) {
        String normalized = normalizePurpose(purpose);
        return normalized.isEmpty() ? PURPOSE_GENERAL : normalized;
    }

    private static List<Template> readAll(Context context) {
        List<Template> result = new ArrayList<>();
        String raw = prefs(context).getString(KEY_TEMPLATES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) result.add(Template.fromJson(object));
            }
        } catch (JSONException ignored) {
            prefs(context).edit().remove(KEY_TEMPLATES).apply();
        }
        return result;
    }

    private static void writeAll(Context context, List<Template> templates) {
        JSONArray array = new JSONArray();
        for (Template template : templates) array.put(template.toJson());
        prefs(context).edit().putString(KEY_TEMPLATES, array.toString()).apply();
    }

    private static Template firstForPurpose(List<Template> templates, String purpose) {
        for (Template template : templates) {
            if (normalizePurposeOrGeneral(purpose).equals(template.purpose)) return template;
        }
        for (Template template : templates) {
            if (PURPOSE_GENERAL.equals(template.purpose)) return template;
        }
        return templates.isEmpty() ? null : templates.get(0);
    }

    private static Template findById(List<Template> templates, String id) {
        int index = indexOf(templates, id);
        return index < 0 ? null : templates.get(index);
    }

    private static int indexOf(List<Template> templates, String id) {
        if (safe(id).trim().isEmpty()) return -1;
        for (int i = 0; i < templates.size(); i++) {
            if (id.equals(templates.get(i).id)) return i;
        }
        return -1;
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private static String clean(String value, String fallback) {
        String normalized = safe(value).trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final Comparator<Template> TEMPLATE_ORDER = (left, right) -> {
        if (left.favorite != right.favorite) return left.favorite ? -1 : 1;
        int recent = Long.compare(right.lastUsedAt, left.lastUsedAt);
        if (recent != 0) return recent;
        int updated = Long.compare(right.updatedAt, left.updatedAt);
        if (updated != 0) return updated;
        return left.name.compareToIgnoreCase(right.name);
    };

    public static final class Template {
        public String id;
        public String name;
        public String body;
        public String category;
        public String purpose;
        public boolean favorite;
        public long createdAt;
        public long updatedAt;
        public long lastUsedAt;
        public int useCount;

        public Template(String id, String name, String body, String category, String purpose,
                        boolean favorite, long createdAt, long updatedAt,
                        long lastUsedAt, int useCount) {
            this.id = safe(id);
            this.name = safe(name);
            this.body = safe(body);
            this.category = safe(category);
            this.purpose = normalizePurposeOrGeneral(purpose);
            this.favorite = favorite;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.lastUsedAt = lastUsedAt;
            this.useCount = useCount;
        }

        public static Template create(String name, String body, String category, String purpose) {
            return new Template("", name, body, category, purpose,
                    false, 0L, 0L, 0L, 0);
        }

        Template copy() {
            return new Template(id, name, body, category, purpose, favorite,
                    createdAt, updatedAt, lastUsedAt, useCount);
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("id", id);
                object.put("name", name);
                object.put("body", body);
                object.put("category", category);
                object.put("purpose", purpose);
                object.put("favorite", favorite);
                object.put("createdAt", createdAt);
                object.put("updatedAt", updatedAt);
                object.put("lastUsedAt", lastUsedAt);
                object.put("useCount", useCount);
            } catch (JSONException ignored) {
            }
            return object;
        }

        static Template fromJson(JSONObject object) {
            return new Template(
                    object.optString("id", newId()),
                    object.optString("name", "새 템플릿"),
                    object.optString("body", ""),
                    object.optString("category", "기타"),
                    object.optString("purpose", PURPOSE_GENERAL),
                    object.optBoolean("favorite", false),
                    object.optLong("createdAt", 0L),
                    object.optLong("updatedAt", 0L),
                    object.optLong("lastUsedAt", 0L),
                    object.optInt("useCount", 0)
            );
        }
    }
}
