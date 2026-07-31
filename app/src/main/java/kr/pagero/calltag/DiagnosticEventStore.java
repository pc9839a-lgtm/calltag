package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class DiagnosticEventStore {
    private static final String PREFS = "calltag_diagnostic_events";
    private static final String KEY_EVENTS = "events";
    private static final int MAX_EVENTS = 120;

    private DiagnosticEventStore() {}

    public static synchronized void record(Context context, String event,
                                           long messageId, String detail) {
        try {
            SharedPreferences prefs = prefs(context);
            JSONArray current = new JSONArray(prefs.getString(KEY_EVENTS, "[]"));
            JSONArray next = new JSONArray();
            int start = Math.max(0, current.length() - (MAX_EVENTS - 1));
            for (int i = start; i < current.length(); i++) next.put(current.opt(i));

            JSONObject row = new JSONObject();
            row.put("time", System.currentTimeMillis());
            row.put("event", sanitize(event, 40));
            row.put("message_id", Math.max(0L, messageId));
            row.put("detail", sanitize(detail, 120));
            next.put(row);
            prefs.edit().putString(KEY_EVENTS, next.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static synchronized List<String> recent(Context context, int limit) {
        List<String> rows = new ArrayList<>();
        int safeLimit = Math.max(1, Math.min(30, limit));
        try {
            JSONArray events = new JSONArray(prefs(context).getString(KEY_EVENTS, "[]"));
            for (int i = events.length() - 1; i >= 0 && rows.size() < safeLimit; i--) {
                JSONObject row = events.optJSONObject(i);
                if (row == null) continue;
                long time = row.optLong("time", 0L);
                String event = sanitize(row.optString("event", "EVENT"), 40);
                long messageId = Math.max(0L, row.optLong("message_id", 0L));
                String detail = sanitize(row.optString("detail", ""), 120);
                StringBuilder value = new StringBuilder();
                value.append(format(time)).append(" · ").append(event);
                if (messageId > 0L) value.append(" · 작업 #").append(messageId);
                if (!detail.isEmpty()) value.append(" · ").append(detail);
                rows.add(value.toString());
            }
        } catch (Exception ignored) {
        }
        return rows;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String sanitize(String value, int maxLength) {
        String clean = value == null ? "" : value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (clean.length() > maxLength) clean = clean.substring(0, maxLength);
        return clean;
    }

    private static String format(long time) {
        if (time <= 0L) return "시각 없음";
        return new SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA).format(new Date(time));
    }
}
