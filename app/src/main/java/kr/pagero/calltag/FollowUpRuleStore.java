package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 통화 후 여러 시점에 보낼 수 있는 후속문자 규칙 목록. */
public final class FollowUpRuleStore {
    private static final String PREFS = "calltag_follow_up_rules_v1";
    private static final String KEY_RULES = "rules";
    private static final String KEY_MIGRATED = "migrated_from_single_rule";

    public static final class Rule {
        public String id;
        public String name;
        public boolean enabled;
        public int delayDays;
        public String templateId;

        public Rule(String id, String name, boolean enabled, int delayDays, String templateId) {
            this.id = safe(id).isEmpty() ? UUID.randomUUID().toString() : id;
            this.name = clean(name, "후속 안내");
            this.enabled = enabled;
            this.delayDays = clamp(delayDays, 1, 30);
            this.templateId = safe(templateId);
        }

        Rule copy() {
            return new Rule(id, name, enabled, delayDays, templateId);
        }
    }

    private FollowUpRuleStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized void ensureMigrated(Context context) {
        SharedPreferences p = prefs(context);
        if (p.getBoolean(KEY_MIGRATED, false)) return;
        List<Rule> rules = read(context);
        if (rules.isEmpty() && MessageAutomationStore.delayedEnabled(context)) {
            String templateId = MessageTemplateStore.defaultId(
                    context, MessageTemplateStore.PURPOSE_FOLLOW_UP);
            rules.add(new Rule(
                    UUID.randomUUID().toString(),
                    "기본 후속 안내",
                    true,
                    MessageAutomationStore.delayDays(context),
                    templateId));
            write(context, rules);
        }
        MessageAutomationStore.setDelayedEnabled(context, false);
        p.edit().putBoolean(KEY_MIGRATED, true).apply();
    }

    public static synchronized List<Rule> list(Context context) {
        ensureMigrated(context);
        List<Rule> result = new ArrayList<>();
        for (Rule rule : read(context)) result.add(rule.copy());
        return result;
    }

    public static synchronized List<Rule> enabledRules(Context context) {
        List<Rule> result = new ArrayList<>();
        for (Rule rule : list(context)) {
            if (rule.enabled) result.add(rule);
        }
        return result;
    }

    public static synchronized Rule find(Context context, String id) {
        for (Rule rule : list(context)) {
            if (rule.id.equals(id)) return rule;
        }
        return null;
    }

    public static synchronized Rule save(Context context, Rule value) {
        if (value == null) throw new IllegalArgumentException("후속문자 규칙이 없습니다.");
        Rule normalized = new Rule(
                value.id, value.name, value.enabled, value.delayDays, value.templateId);
        List<Rule> rules = read(context);
        boolean replaced = false;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).id.equals(normalized.id)) {
                rules.set(i, normalized);
                replaced = true;
                break;
            }
        }
        if (!replaced) rules.add(normalized);
        write(context, rules);
        return normalized.copy();
    }

    public static synchronized void setEnabled(Context context, String id, boolean enabled) {
        List<Rule> rules = read(context);
        for (Rule rule : rules) {
            if (rule.id.equals(id)) rule.enabled = enabled;
        }
        write(context, rules);
    }

    public static synchronized boolean delete(Context context, String id) {
        List<Rule> rules = read(context);
        boolean removed = false;
        for (int i = rules.size() - 1; i >= 0; i--) {
            if (rules.get(i).id.equals(id)) {
                rules.remove(i);
                removed = true;
            }
        }
        if (removed) write(context, rules);
        return removed;
    }

    private static List<Rule> read(Context context) {
        List<Rule> result = new ArrayList<>();
        String raw = prefs(context).getString(KEY_RULES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                result.add(new Rule(
                        item.optString("id", ""),
                        item.optString("name", "후속 안내"),
                        item.optBoolean("enabled", true),
                        item.optInt("delayDays", 3),
                        item.optString("templateId", "")));
            }
        } catch (Exception ignored) {
            result.clear();
        }
        return result;
    }

    private static void write(Context context, List<Rule> rules) {
        JSONArray array = new JSONArray();
        for (Rule rule : rules) {
            try {
                array.put(new JSONObject()
                        .put("id", rule.id)
                        .put("name", rule.name)
                        .put("enabled", rule.enabled)
                        .put("delayDays", rule.delayDays)
                        .put("templateId", rule.templateId));
            } catch (Exception ignored) {
                // JSONObject fields are primitive and should not fail.
            }
        }
        prefs(context).edit().putString(KEY_RULES, array.toString()).apply();
    }

    private static String clean(String value, String fallback) {
        String result = safe(value).trim();
        return result.isEmpty() ? fallback : result;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
