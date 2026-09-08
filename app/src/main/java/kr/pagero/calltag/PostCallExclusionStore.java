package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stores phone numbers that must never receive the post-call popup. */
public final class PostCallExclusionStore {
    private static final String PREFS = "calltag_post_call_exclusions";
    private static final String KEY_ENTRIES = "entries_v1";

    private PostCallExclusionStore() {}

    public static synchronized void add(Context context, String displayName, String phone) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.length() < 8) {
            throw new IllegalArgumentException("전화번호를 확인해주세요.");
        }
        Map<String, Entry> entries = readMap(context);
        entries.put(normalized, new Entry(
                safe(displayName).trim(),
                safe(phone).trim().isEmpty() ? normalized : safe(phone).trim(),
                normalized,
                System.currentTimeMillis()));
        write(context, entries.values());
    }

    public static synchronized void remove(Context context, String phone) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.isEmpty()) return;
        Map<String, Entry> entries = readMap(context);
        if (entries.remove(normalized) != null) write(context, entries.values());
    }

    public static synchronized boolean contains(Context context, String phone) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        return normalized.length() >= 8 && readMap(context).containsKey(normalized);
    }

    public static synchronized List<Entry> list(Context context) {
        List<Entry> result = new ArrayList<>(readMap(context).values());
        Collections.sort(result, (left, right) -> Long.compare(right.updatedAt, left.updatedAt));
        return result;
    }

    private static Map<String, Entry> readMap(Context context) {
        Map<String, Entry> result = new LinkedHashMap<>();
        String raw = prefs(context).getString(KEY_ENTRIES, "[]");
        try {
            JSONArray array = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String normalized = object.optString("normalizedPhone", "");
                if (normalized.length() < 8) continue;
                result.put(normalized, new Entry(
                        object.optString("displayName", ""),
                        object.optString("phone", normalized),
                        normalized,
                        object.optLong("updatedAt", 0L)));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    private static void write(Context context, Iterable<Entry> entries) {
        JSONArray array = new JSONArray();
        for (Entry entry : entries) {
            try {
                JSONObject object = new JSONObject();
                object.put("displayName", entry.displayName);
                object.put("phone", entry.phone);
                object.put("normalizedPhone", entry.normalizedPhone);
                object.put("updatedAt", entry.updatedAt);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        prefs(context).edit().putString(KEY_ENTRIES, array.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Entry {
        public final String displayName;
        public final String phone;
        public final String normalizedPhone;
        public final long updatedAt;

        Entry(String displayName, String phone, String normalizedPhone, long updatedAt) {
            this.displayName = displayName;
            this.phone = phone;
            this.normalizedPhone = normalizedPhone;
            this.updatedAt = updatedAt;
        }
    }
}
