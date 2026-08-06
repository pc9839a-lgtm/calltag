package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/** 외부 Android Calendar Provider 연동 상태와 콜태그 일정-event 매핑을 저장한다. */
public final class ExternalCalendarSyncStore {
    private static final String PREFS = "calltag_external_calendar_sync";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_CALENDAR_ID = "calendar_id";
    private static final String KEY_CALENDAR_NAME = "calendar_name";
    private static final String KEY_LAST_SYNC_AT = "last_sync_at";
    private static final String KEY_LAST_SYNC_COUNT = "last_sync_count";
    private static final String KEY_LAST_ERROR = "last_error";
    private static final String EVENT_PREFIX = "event_";

    private ExternalCalendarSyncStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static long calendarId(Context context) {
        return prefs(context).getLong(KEY_CALENDAR_ID, -1L);
    }

    public static String calendarName(Context context) {
        return prefs(context).getString(KEY_CALENDAR_NAME, "");
    }

    public static void setCalendar(Context context, long id, String name) {
        prefs(context).edit()
                .putLong(KEY_CALENDAR_ID, id)
                .putString(KEY_CALENDAR_NAME, name == null ? "" : name)
                .apply();
    }

    public static long eventId(Context context, long taskId) {
        return prefs(context).getLong(EVENT_PREFIX + taskId, -1L);
    }

    public static void setEventId(Context context, long taskId, long eventId) {
        prefs(context).edit().putLong(EVENT_PREFIX + taskId, eventId).apply();
    }

    public static void removeEventId(Context context, long taskId) {
        prefs(context).edit().remove(EVENT_PREFIX + taskId).apply();
    }

    public static Map<Long, Long> eventMappings(Context context) {
        Map<Long, Long> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : prefs(context).getAll().entrySet()) {
            if (!entry.getKey().startsWith(EVENT_PREFIX) || !(entry.getValue() instanceof Long)) continue;
            try {
                long taskId = Long.parseLong(entry.getKey().substring(EVENT_PREFIX.length()));
                result.put(taskId, (Long) entry.getValue());
            } catch (NumberFormatException ignored) {
                // Ignore malformed legacy keys.
            }
        }
        return result;
    }

    public static void saveResult(Context context, int count, String error) {
        prefs(context).edit()
                .putLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())
                .putInt(KEY_LAST_SYNC_COUNT, Math.max(0, count))
                .putString(KEY_LAST_ERROR, error == null ? "" : error)
                .apply();
    }

    public static long lastSyncAt(Context context) {
        return prefs(context).getLong(KEY_LAST_SYNC_AT, 0L);
    }

    public static int lastSyncCount(Context context) {
        return prefs(context).getInt(KEY_LAST_SYNC_COUNT, 0);
    }

    public static String lastError(Context context) {
        return prefs(context).getString(KEY_LAST_ERROR, "");
    }
}
