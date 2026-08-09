package kr.pagero.calltag;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Lightweight local crash/launch breadcrumb store used for field diagnosis. */
public final class CrashTelemetryStore {
    private static final String PREFS = "calltag_crash_telemetry";
    private static final String KEY_EVENTS = "events";
    private static final int MAX_EVENTS = 60;
    private static final Object LOCK = new Object();

    private CrashTelemetryStore() {}

    public static void install(Application application) {
        if (application == null) return;
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        if (previous instanceof TelemetryHandler) return;
        Thread.setDefaultUncaughtExceptionHandler(new TelemetryHandler(application, previous));
        record(application, "app", "telemetry_installed", "");
    }

    public static void record(Context context, String source, String result, String detail) {
        if (context == null) return;
        try {
            String event = System.currentTimeMillis() + "|" + clean(source) + "|"
                    + clean(result) + "|" + clean(detail);
            synchronized (LOCK) {
                SharedPreferences prefs = prefs(context);
                String raw = prefs.getString(KEY_EVENTS, "");
                List<String> rows = new ArrayList<>();
                if (raw != null && !raw.isEmpty()) {
                    for (String row : raw.split("\\n")) {
                        if (!row.trim().isEmpty()) rows.add(row);
                    }
                }
                rows.add(event);
                while (rows.size() > MAX_EVENTS) rows.remove(0);
                prefs.edit().putString(KEY_EVENTS, join(rows)).apply();
            }
        } catch (RuntimeException ignored) {
            // Diagnostics must never affect the app path they observe.
        }
    }

    public static String snapshot(Context context) {
        if (context == null) return "";
        try {
            return prefs(context).getString(KEY_EVENTS, "");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String clean(String value) {
        String safe = value == null ? "" : value.trim().replace('\n', ' ').replace('|', '/');
        return safe.length() <= 240 ? safe : safe.substring(0, 240);
    }

    private static String join(List<String> rows) {
        StringBuilder out = new StringBuilder();
        for (String row : rows) {
            if (out.length() > 0) out.append('\n');
            out.append(row);
        }
        return out.toString();
    }

    private static final class TelemetryHandler implements Thread.UncaughtExceptionHandler {
        private final Context app;
        private final Thread.UncaughtExceptionHandler delegate;

        TelemetryHandler(Context app, Thread.UncaughtExceptionHandler delegate) {
            this.app = app.getApplicationContext();
            this.delegate = delegate;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable error) {
            String threadName = thread == null ? "unknown" : thread.getName();
            String detail = error == null ? "unknown" : error.getClass().getSimpleName();
            record(app, "uncaught:" + threadName, "crash", detail);
            if (delegate != null) delegate.uncaughtException(thread, error);
        }
    }
}
