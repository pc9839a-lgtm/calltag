package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Distinguishes a deliberate app foreground entry from automatic call-related UI.
 *
 * Firebase itself logs first_open/session_start/user_engagement after collection is
 * enabled. We only add one business-level 'calltag_app_open' per foreground entry.
 * Never include contacts, phone numbers, email, owner IDs, message text or memos.
 */
public final class CallTagAppAnalytics {
    private static final String EVENT_APP_OPEN = "calltag_app_open";

    private CallTagAppAnalytics() {}

    public static void recordUserOpen(Context context, Activity activity) {
        if (!BuildConfig.CALLTAG_ANALYTICS_ENABLED || activity == null) return;
        // Fallback after-call and caller ID screens can appear automatically.
        // They are not evidence that the user consciously opened the app.
        if (activity instanceof PostCallActivity || activity instanceof CallerInfoActivity
                || activity instanceof GoogleAuthCallbackActivity
                || activity instanceof GoogleCredentialLoginActivity) return;
        try {
            if (FirebaseApp.getApps(context.getApplicationContext()).isEmpty()) return;
            FirebaseAnalytics.getInstance(context.getApplicationContext())
                    .logEvent(EVENT_APP_OPEN, new Bundle());
        } catch (RuntimeException ignored) {
            // Analytics failure must never affect call handling or app navigation.
        }
    }
}
