package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryPurchasesParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Re-verifies active Google Play subscriptions in the background while the app is in use.
 *
 * Google renewals reuse the same purchase token, while the Publisher API exposes the latest
 * renewal order id. Sending the token back through restore lets the server record one idempotent
 * 20% referral commission for each newly verified renewal order without storing raw tokens.
 */
public final class PlaySubscriptionReconcileManager {
    private static final String PREFS = "calltag_play_reconcile";
    private static final String KEY_LAST_ATTEMPT_AT = "last_attempt_at";
    private static final String KEY_LAST_SUCCESS_AT = "last_success_at";
    private static final String KEY_OWNER_ID = "owner_id";
    private static final long MIN_INTERVAL_MS = 6L * 60L * 60L * 1000L;
    private static final long RETRY_INTERVAL_MS = 5L * 60L * 1000L;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private PlaySubscriptionReconcileManager() {}

    public static void reconcileIfDue(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        String session = AuthSessionStore.session(app);
        String ownerId = AuthSessionStore.ownerId(app);
        if (session == null || session.trim().isEmpty()
                || ownerId == null || ownerId.trim().isEmpty()) return;

        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!RUNNING.compareAndSet(false, true)) return;
        String previousOwner = prefs.getString(KEY_OWNER_ID, "");
        if (!ownerId.equals(previousOwner)) {
            // A different user must not inherit the previous account's 6h throttle.
            prefs.edit().putString(KEY_OWNER_ID, ownerId)
                    .remove(KEY_LAST_ATTEMPT_AT).remove(KEY_LAST_SUCCESS_AT).commit();
        }
        long now = System.currentTimeMillis();
        long lastAttempt = prefs.getLong(KEY_LAST_ATTEMPT_AT, 0L);
        long lastSuccess = prefs.getLong(KEY_LAST_SUCCESS_AT, 0L);
        long interval = lastAttempt > 0L && lastSuccess >= lastAttempt
                ? MIN_INTERVAL_MS : RETRY_INTERVAL_MS;
        if (now - lastAttempt < interval) {
            RUNNING.set(false);
            return;
        }
        prefs.edit().putLong(KEY_LAST_ATTEMPT_AT, now).apply();

        BillingClient client = BillingClient.newBuilder(app)
                .setListener((billingResult, purchases) -> {
                    // This reconciler never launches a purchase flow.
                })
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build())
                .enableAutoServiceReconnection()
                .build();

        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    finish(client);
                    CrashTelemetryStore.record(app, "play_subscription_reconcile",
                            "billing_unavailable", String.valueOf(billingResult.getResponseCode()));
                    return;
                }
                QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .includeSuspendedSubscriptions(true)
                        .build();
                client.queryPurchasesAsync(params, (result, purchases) -> {
                    if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        CrashTelemetryStore.record(app, "play_subscription_reconcile",
                                "query_failed", String.valueOf(result.getResponseCode()));
                        finish(client);
                        return;
                    }
                    JSONArray payload = purchasePayload(purchases);
                    finish(client);
                    if (payload.length() == 0) {
                        if (matchesAccount(app, ownerId, session)) {
                            prefs.edit().putLong(KEY_LAST_SUCCESS_AT, System.currentTimeMillis()).apply();
                        }
                        return;
                    }
                    new Thread(() -> {
                        try {
                            // A logout or account switch can happen while Play is querying.
                            // Never apply the old user's verification to the current user's cache.
                            if (!matchesAccount(app, ownerId, session)) return;
                            JSONObject response = AuthApiClient.restoreGooglePurchases(session, payload);
                            if (!matchesAccount(app, ownerId, session)) return;
                            FeatureEntitlementStore.saveServerEntitlement(app, response);
                            // The expired notice can be in the foreground instead of MainActivity.
                            // Let it re-evaluate only server-verified subscription state.
                            app.sendBroadcast(new android.content.Intent(
                                    EntitlementNoticeActivity.ACTION_ENTITLEMENT_VERIFIED)
                                    .setPackage(app.getPackageName()));
                            prefs.edit()
                                    .putLong(KEY_LAST_SUCCESS_AT, System.currentTimeMillis())
                                    .apply();
                            CrashTelemetryStore.record(app, "play_subscription_reconcile",
                                    "success", "purchases=" + payload.length());
                        } catch (Exception error) {
                            CrashTelemetryStore.record(app, "play_subscription_reconcile",
                                    "server_failed", error.getClass().getSimpleName());
                        } finally {
                            RUNNING.set(false);
                        }
                    }, "calltag-play-subscription-reconcile").start();
                });
            }

            @Override
            public void onBillingServiceDisconnected() {
                CrashTelemetryStore.record(app, "play_subscription_reconcile",
                        "billing_disconnected", "");
                finish(client);
            }
        });
    }

    private static boolean matchesAccount(Context app, String ownerId, String session) {
        return ownerId.equals(AuthSessionStore.ownerId(app))
                && session.equals(AuthSessionStore.session(app));
    }

    private static JSONArray purchasePayload(List<Purchase> purchases) {
        JSONArray payload = new JSONArray();
        if (purchases == null) return payload;
        for (Purchase purchase : purchases) {
            if (purchase == null
                    || purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
                continue;
            }
            JSONArray products = new JSONArray();
            for (String product : purchase.getProducts()) {
                if (FeatureEntitlementStore.PLAN_PHONE.equals(product)
                        || FeatureEntitlementStore.PLAN_MESSAGE.equals(product)) {
                    products.put(product);
                }
            }
            if (products.length() == 0) continue;
            try {
                payload.put(new JSONObject()
                        .put("purchaseToken", purchase.getPurchaseToken())
                        .put("orderId", purchase.getOrderId())
                        .put("products", products));
            } catch (Exception ignored) {
                // One malformed Play row must not block reconciliation of the remaining purchases.
            }
        }
        return payload;
    }

    private static void finish(BillingClient client) {
        try {
            if (client != null && client.isReady()) client.endConnection();
        } catch (RuntimeException ignored) {
        }
        RUNNING.set(false);
    }
}
