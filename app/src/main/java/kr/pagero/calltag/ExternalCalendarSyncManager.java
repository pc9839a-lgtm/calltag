package kr.pagero.calltag;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

/** 콜태그의 모든 예정 일정을 사용자가 선택한 Android 캘린더에 일괄 반영한다. */
public final class ExternalCalendarSyncManager {
    private static final long DEFAULT_DURATION_MS = 30L * 60L * 1000L;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    public interface Callback {
        void onComplete(Result result);
    }

    public static final class CalendarInfo {
        public final long id;
        public final String name;
        public final String account;

        CalendarInfo(long id, String name, String account) {
            this.id = id;
            this.name = safe(name, "외부 캘린더");
            this.account = safe(account, "");
        }

        public String label() {
            return account.isEmpty() ? name : name + " · " + account;
        }
    }

    public static final class Result {
        public final boolean success;
        public final int synced;
        public final String error;

        Result(boolean success, int synced, String error) {
            this.success = success;
            this.synced = Math.max(0, synced);
            this.error = safe(error, "");
        }
    }

    private ExternalCalendarSyncManager() {}

    public static boolean hasPermissions(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static List<CalendarInfo> writableCalendars(Context context) {
        List<CalendarInfo> result = new ArrayList<>();
        if (context == null || !hasPermissions(context)) return result;
        String[] projection = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME
        };
        String selection = CalendarContract.Calendars.VISIBLE + "=1 AND "
                + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">=?";
        String[] args = {String.valueOf(CalendarContract.Calendars.CAL_ACCESS_READ)};
        try (Cursor cursor = context.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                args,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " COLLATE NOCASE ASC")) {
            if (cursor == null) return result;
            while (cursor.moveToNext()) {
                result.add(new CalendarInfo(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2)));
            }
        } catch (RuntimeException ignored) {
            result.clear();
        }
        return result;
    }

    public static void requestSync(Context context, boolean force, Callback callback) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        if (!force && !ExternalCalendarSyncStore.isEnabled(app)) return;
        if (!hasPermissions(app)) {
            dispatch(callback, new Result(false, 0, "캘린더 권한이 필요합니다."));
            return;
        }
        long calendarId = ExternalCalendarSyncStore.calendarId(app);
        if (calendarId < 0L) {
            dispatch(callback, new Result(false, 0, "연동할 캘린더를 선택해주세요."));
            return;
        }
        if (!RUNNING.compareAndSet(false, true)) return;
        new Thread(() -> {
            Result result;
            try {
                result = syncNow(app, calendarId);
            } catch (Exception error) {
                result = new Result(false, 0, "전체 일정을 연동하지 못했습니다.");
            } finally {
                RUNNING.set(false);
            }
            ExternalCalendarSyncStore.saveResult(app, result.synced, result.error);
            dispatch(callback, result);
        }, "calltag-external-calendar-sync").start();
    }

    private static Result syncNow(Context context, long calendarId) {
        if (!calendarExists(context, calendarId)) {
            ExternalCalendarSyncStore.setEnabled(context, false);
            return new Result(false, 0, "선택한 캘린더를 사용할 수 없습니다.");
        }

        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            List<FollowUpTask> tasks = db.listPendingTasks();
            Set<Long> activeTaskIds = new HashSet<>();
            int synced = 0;
            for (FollowUpTask task : tasks) {
                activeTaskIds.add(task.id);
                Customer customer = db.findCustomerById(task.customerId);
                String memo = customer == null ? "" : CustomerInsightResolver.latestMemo(db, customer);
                long eventId = upsertEvent(context, calendarId, task, customer, memo);
                if (eventId > 0L) {
                    ExternalCalendarSyncStore.setEventId(context, task.id, eventId);
                    synced++;
                }
            }
            removeStaleEvents(context, activeTaskIds,
                    ExternalCalendarSyncStore.eventMappings(context));
            return new Result(true, synced, "");
        } finally {
            db.close();
        }
    }

    private static long upsertEvent(Context context, long calendarId, FollowUpTask task,
                                    Customer customer, String memo) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.TITLE,
                "콜태그 · " + safe(task.customerName, "고객") + " · " + safe(task.title, "일정"));
        StringBuilder description = new StringBuilder();
        if (!safe(task.phone, "").isEmpty()) {
            description.append("연락처: ").append(task.phone.trim());
        }
        if (!safe(memo, "").isEmpty()) {
            if (description.length() > 0) description.append("\n");
            description.append("최근 메모: ").append(memo.trim());
        }
        description.append(description.length() > 0 ? "\n" : "")
                .append("콜태그 일정 ID: ").append(task.id);
        values.put(CalendarContract.Events.DESCRIPTION, description.toString());
        values.put(CalendarContract.Events.DTSTART, task.dueAt);
        values.put(CalendarContract.Events.DTEND, task.dueAt + DEFAULT_DURATION_MS);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        values.put(CalendarContract.Events.HAS_ALARM, 1);

        long mapped = ExternalCalendarSyncStore.eventId(context, task.id);
        if (mapped > 0L) {
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, mapped);
            try {
                int updated = resolver.update(uri, values, null, null);
                if (updated > 0) return mapped;
            } catch (RuntimeException ignored) {
                // Event was deleted externally; recreate it below.
            }
        }
        try {
            Uri inserted = resolver.insert(CalendarContract.Events.CONTENT_URI, values);
            if (inserted == null) return -1L;
            long eventId = ContentUris.parseId(inserted);
            addDefaultReminder(resolver, eventId);
            return eventId;
        } catch (RuntimeException ignored) {
            return -1L;
        }
    }

    private static void addDefaultReminder(ContentResolver resolver, long eventId) {
        ContentValues reminder = new ContentValues();
        reminder.put(CalendarContract.Reminders.EVENT_ID, eventId);
        reminder.put(CalendarContract.Reminders.MINUTES, 10);
        reminder.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        try {
            resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder);
        } catch (RuntimeException ignored) {
            // Some account providers do not allow custom reminders.
        }
    }

    private static void removeStaleEvents(Context context, Set<Long> activeTaskIds,
                                          Map<Long, Long> mappings) {
        ContentResolver resolver = context.getContentResolver();
        for (Map.Entry<Long, Long> entry : mappings.entrySet()) {
            if (activeTaskIds.contains(entry.getKey())) continue;
            try {
                resolver.delete(ContentUris.withAppendedId(
                        CalendarContract.Events.CONTENT_URI, entry.getValue()), null, null);
            } catch (RuntimeException ignored) {
                // Mapping is still removed so a deleted task cannot return later.
            }
            ExternalCalendarSyncStore.removeEventId(context, entry.getKey());
        }
    }

    private static boolean calendarExists(Context context, long calendarId) {
        try (Cursor cursor = context.getContentResolver().query(
                ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId),
                new String[]{CalendarContract.Calendars._ID},
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">?",
                new String[]{String.valueOf(CalendarContract.Calendars.CAL_ACCESS_READ)},
                null)) {
            return cursor != null && cursor.moveToFirst();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void dispatch(Callback callback, Result result) {
        if (callback != null) callback.onComplete(result);
    }

    private static String safe(String value, String fallback) {
        String result = value == null ? "" : value.trim();
        return result.isEmpty() ? fallback : result;
    }
}
