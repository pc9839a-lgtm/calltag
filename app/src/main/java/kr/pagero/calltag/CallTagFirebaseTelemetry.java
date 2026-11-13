package kr.pagero.calltag;

import android.content.Context;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/** SDK startup only; never forwards CRM records, SMS payloads or local diagnostic logs. */
public final class CallTagFirebaseTelemetry {
    private CallTagFirebaseTelemetry() {}

    public static void install(Context context) {
        if (context == null || !CallTagFirebaseInitializer.ensureInitialized(context)) return;
        try {
            // Automatic lifecycle events and crash/ANR reports use SDK defaults.
            // Collection is enabled only for release by manifest configuration.
            FirebaseAnalytics.getInstance(context.getApplicationContext());
            FirebaseCrashlytics.getInstance();
        } catch (RuntimeException ignored) {
            // Diagnostics must not prevent calling, messaging, or account access.
        }
    }
}
