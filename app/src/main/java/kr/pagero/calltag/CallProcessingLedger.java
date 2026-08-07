package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent call-log receipt ledger.
 *
 * The old implementation remembered only the single last call id. That was not enough when the
 * process died between consecutive calls or when a ContentObserver and Telephony callback resolved
 * the same call independently. Keep a bounded set of recently resolved call-log ids instead.
 */
public final class CallProcessingLedger {
    private static final String PREFS = "calltag_call_processing_ledger";
    private static final String KEY_RESOLVED_IDS = "resolved_call_ids";
    private static final int MAX_IDS = 256;

    private CallProcessingLedger() {}

    public static synchronized boolean wasResolved(Context context, long callLogId) {
        if (callLogId <= 0L) return false;
        return prefs(context).getStringSet(KEY_RESOLVED_IDS, Collections.emptySet())
                .contains(String.valueOf(callLogId));
    }

    public static synchronized void markResolved(Context context, long callLogId) {
        if (callLogId <= 0L) return;
        Set<String> current = new HashSet<>(
                prefs(context).getStringSet(KEY_RESOLVED_IDS, Collections.emptySet()));
        current.add(String.valueOf(callLogId));
        if (current.size() > MAX_IDS) current = trim(current);
        prefs(context).edit().putStringSet(KEY_RESOLVED_IDS, current).apply();
    }

    static synchronized void clearForTests(Context context) {
        prefs(context).edit().remove(KEY_RESOLVED_IDS).commit();
    }

    private static Set<String> trim(Set<String> values) {
        List<Long> ids = new ArrayList<>();
        for (String value : values) {
            try {
                ids.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                // Drop corrupt values while compacting the ledger.
            }
        }
        ids.sort(Collections.reverseOrder());
        Set<String> result = new HashSet<>();
        for (int i = 0; i < Math.min(MAX_IDS, ids.size()); i++) {
            result.add(String.valueOf(ids.get(i)));
        }
        return result;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
