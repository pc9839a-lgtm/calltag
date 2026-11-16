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

/** Keeps a short, device-local list of calls already processed by CallTag. */
final class RecentCustomerCallStore {
    private static final String PREFS = "calltag_recent_customer_calls";
    private static final String KEY_CALLS = "calls";
    private static final long MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;
    private static final int MAX_CALLS = 30;

    private RecentCustomerCallStore() {}

    static synchronized void remember(Context context, CallRecord call) {
        if (!AuthSessionStore.hasSession(context) || AuthSessionStore.ownerId(context).trim().isEmpty()) return;
        long cutoff = System.currentTimeMillis() - MAX_AGE_MS;
        String number = PhoneNumberNormalizer.normalize(call.phone);
        if (call.startedAt < cutoff || number.length() < 8) return;
        SharedPreferences prefs = prefs(context);
        List<CallRecord> calls = read(prefs, cutoff);
        for (CallRecord previous : calls) {
            if (number.equals(PhoneNumberNormalizer.normalize(previous.phone))
                    && previous.startedAt >= call.startedAt) return;
        }
        calls.removeIf(previous -> number.equals(PhoneNumberNormalizer.normalize(previous.phone)));
        calls.add(call);
        calls.sort(Comparator.comparingLong((CallRecord item) -> item.startedAt).reversed());
        write(prefs, calls);
    }

    static synchronized List<CallRecord> recent(Context context) {
        if (!AuthSessionStore.hasSession(context) || AuthSessionStore.ownerId(context).trim().isEmpty()
                || !SetupRequirements.hasCallLog(context) || !FeatureEntitlementStore.hasPhoneAccess(context)) {
            return Collections.emptyList();
        }
        SharedPreferences prefs = prefs(context);
        List<CallRecord> calls = read(prefs, System.currentTimeMillis() - MAX_AGE_MS);
        write(prefs, calls);
        return calls;
    }

    static synchronized void clear(Context context) {
        prefs(context).edit().clear().commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(AccountDataScope.preferenceName(context, PREFS), Context.MODE_PRIVATE);
    }

    private static List<CallRecord> read(SharedPreferences prefs, long cutoff) {
        List<CallRecord> result = new ArrayList<>();
        try {
            JSONArray rows = new JSONArray(prefs.getString(KEY_CALLS, "[]"));
            for (int i = 0; i < rows.length() && result.size() < MAX_CALLS; i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null || row.optLong("time") < cutoff) continue;
                result.add(new CallRecord(0L, row.optString("phone"), row.optString("name"),
                        row.optInt("type"), row.optLong("time"), row.optLong("duration")));
            }
        } catch (JSONException ignored) {
            // A malformed cache should not prevent customer entry.
        }
        return result;
    }

    private static void write(SharedPreferences prefs, List<CallRecord> calls) {
        JSONArray rows = new JSONArray();
        for (int i = 0; i < calls.size() && i < MAX_CALLS; i++) {
            CallRecord call = calls.get(i);
            try {
                JSONObject row = new JSONObject();
                row.put("phone", call.phone);
                row.put("name", call.cachedName);
                row.put("type", call.type);
                row.put("time", call.startedAt);
                row.put("duration", call.durationSec);
                rows.put(row);
            } catch (JSONException ignored) {
                // Skip malformed rows without interrupting call handling.
            }
        }
        prefs.edit().putString(KEY_CALLS, rows.toString()).apply();
    }
}
