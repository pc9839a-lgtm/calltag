package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Foreground app opens and low-cardinality subscriber cohorts.
 * No owner ID, email, phone, message, call history or purchase token leaves the app.
 */
public final class CallTagAppAnalytics {
    private static final String EVENT_APP_OPEN = "calltag_app_open";
    private static final String PARAM_SUBSCRIBER_STATUS = "subscriber_status";
    private static final String PARAM_PLAN_GROUP = "subscriber_plan";
    private static final long MAX_ENTITLEMENT_AGE_MS = 24L * 60L * 60L * 1000L;

    private CallTagAppAnalytics() {}

    public static void recordUserOpen(Context context, Activity activity) {
        if (!BuildConfig.CALLTAG_ANALYTICS_ENABLED || activity == null) return;
        // Incoming call UI, fallback call popup and authentication callbacks are not app opens.
        if (activity instanceof PostCallActivity || activity instanceof CallerInfoActivity
                || activity instanceof GoogleAuthCallbackActivity
                || activity instanceof GoogleCredentialLoginActivity) return;
        try {
            FirebaseAnalytics analytics = analytics(context);
            if (analytics == null) return;
            FeatureEntitlementStore.Snapshot snapshot = FeatureEntitlementStore.snapshot(context);
            String status = subscriberStatus(snapshot);
            String plan = subscriberPlan(snapshot, status);
            analytics.setUserProperty(PARAM_SUBSCRIBER_STATUS, status);
            analytics.setUserProperty(PARAM_PLAN_GROUP, plan);
            Bundle params = new Bundle();
            params.putString(PARAM_SUBSCRIBER_STATUS, status);
            params.putString(PARAM_PLAN_GROUP, plan);
            analytics.logEvent(EVENT_APP_OPEN, params);
        } catch (RuntimeException ignored) {
            // Reporting must never prevent the app, call services or billing from working.
        }
    }

    /** Called only after the server entitlement response is saved. No conversion event is inferred. */
    public static void onEntitlementUpdated(Context context) {
        if (!BuildConfig.CALLTAG_ANALYTICS_ENABLED) return;
        try {
            FirebaseAnalytics analytics = analytics(context);
            if (analytics == null) return;
            FeatureEntitlementStore.Snapshot snapshot = FeatureEntitlementStore.snapshot(context);
            String status = subscriberStatus(snapshot);
            analytics.setUserProperty(PARAM_SUBSCRIBER_STATUS, status);
            analytics.setUserProperty(PARAM_PLAN_GROUP, subscriberPlan(snapshot, status));
        } catch (RuntimeException ignored) {
            // Analytics is best effort only.
        }
    }

    /** Remove the previous account's segment on logout/account change. */
    public static void clearSubscriptionSegment(Context context) {
        if (!BuildConfig.CALLTAG_ANALYTICS_ENABLED) return;
        try {
            FirebaseAnalytics analytics = analytics(context);
            if (analytics == null) return;
            analytics.setUserProperty(PARAM_SUBSCRIBER_STATUS, null);
            analytics.setUserProperty(PARAM_PLAN_GROUP, null);
        } catch (RuntimeException ignored) {
            // Logging out never depends on Analytics availability.
        }
    }

    private static FirebaseAnalytics analytics(Context context) {
        Context app = context.getApplicationContext();
        return FirebaseApp.getApps(app).isEmpty() ? null : FirebaseAnalytics.getInstance(app);
    }

    private static String subscriberStatus(FeatureEntitlementStore.Snapshot snapshot) {
        if (snapshot == null || !snapshot.serverChecked) return "unknown";
        if (snapshot.lastCheckedAt <= 0L
                || System.currentTimeMillis() - snapshot.lastCheckedAt > MAX_ENTITLEMENT_AGE_MS)
            return "unknown";
        if (snapshot.isTrial()) return "trial";
        boolean productAccess = snapshot.phoneSubscribed || snapshot.messageSubscribed;
        boolean paidChannel = FeatureEntitlementStore.CHANNEL_GOOGLE_PLAY.equals(snapshot.channel)
                || FeatureEntitlementStore.CHANNEL_WEB.equals(snapshot.channel);
        if (snapshot.active && paidChannel && (productAccess || snapshot.isWebSubscription()))
            return "subscriber";
        if (productAccess && snapshot.active) return "granted";
        return "free";
    }

    private static String subscriberPlan(FeatureEntitlementStore.Snapshot snapshot, String status) {
        if (!"subscriber".equals(status)) return "none";
        if (snapshot.phoneSubscribed && snapshot.messageSubscribed) return "both";
        if (snapshot.phoneSubscribed) return "call";
        if (snapshot.messageSubscribed) return "message";
        return snapshot.isWebSubscription() ? "web_bundle" : "none";
    }
}
