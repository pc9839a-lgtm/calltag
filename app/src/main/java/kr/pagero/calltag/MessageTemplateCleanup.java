package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 기존에 용도별 복사본으로 생긴 완전 동일 템플릿을 한 번만 합친다. */
public final class MessageTemplateCleanup {
    private static final String PREFS = "calltag_message_templates_v1";
    private static final String KEY_TEMPLATES = "templates_json";
    private static final String KEY_DONE = "compact_duplicate_templates_v1";
    private static final String[] DEFAULT_KEYS = {
            "default_incoming", "default_outgoing", "default_missed", "default_follow_up"
    };

    private MessageTemplateCleanup() {}

    public static synchronized void runOnce(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_DONE, false)) return;

        String raw = prefs.getString(KEY_TEMPLATES, "[]");
        try {
            JSONArray source = new JSONArray(raw);
            JSONArray compacted = new JSONArray();
            Map<String, JSONObject> byKey = new LinkedHashMap<>();
            Map<String, String> redirectedIds = new LinkedHashMap<>();

            for (int i = 0; i < source.length(); i++) {
                JSONObject item = source.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                String key = duplicateKey(item);
                JSONObject canonical = byKey.get(key);
                if (canonical == null) {
                    canonical = new JSONObject(item.toString());
                    byKey.put(key, canonical);
                    compacted.put(canonical);
                    continue;
                }
                String canonicalId = canonical.optString("id", "");
                if (!id.isEmpty() && !canonicalId.isEmpty()) redirectedIds.put(id, canonicalId);
                canonical.put("favorite", canonical.optBoolean("favorite", false)
                        || item.optBoolean("favorite", false));
                canonical.put("useCount", canonical.optInt("useCount", 0)
                        + item.optInt("useCount", 0));
                canonical.put("lastUsedAt", Math.max(canonical.optLong("lastUsedAt", 0L),
                        item.optLong("lastUsedAt", 0L)));
                canonical.put("updatedAt", Math.max(canonical.optLong("updatedAt", 0L),
                        item.optLong("updatedAt", 0L)));
                if (canonical.optString("category", "").trim().isEmpty()) {
                    canonical.put("category", item.optString("category", "기타"));
                }
            }

            SharedPreferences.Editor editor = prefs.edit()
                    .putString(KEY_TEMPLATES, compacted.toString())
                    .putBoolean(KEY_DONE, true);
            for (String key : DEFAULT_KEYS) {
                String current = prefs.getString(key, "");
                String replacement = redirectedIds.get(current);
                if (replacement != null) editor.putString(key, replacement);
            }
            editor.apply();
        } catch (JSONException ignored) {
            prefs.edit().putBoolean(KEY_DONE, true).apply();
        }
    }

    private static String duplicateKey(JSONObject item) {
        return normalize(item.optString("name", "")) + "\u0000"
                + normalize(item.optString("body", "")) + "\u0000"
                + item.optString("imageRef", "").trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ")
                .toLowerCase(Locale.KOREA);
    }
}
