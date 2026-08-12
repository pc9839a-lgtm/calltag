package kr.pagero.calltag;

import android.app.Activity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Play Billing 연결, 상품 조회, 구매, 복원을 담당한다.
 * 서버가 결제 가능 상태를 내려주기 전에는 BillingClient 연결 자체를 시작하지 않는다.
 */
public final class PlayBillingManager implements PurchasesUpdatedListener {
    public interface Listener {
        void onBillingReady(Map<String, ProductDetails> products);
        void onBillingMessage(String message);
        void onServerVerified();
    }

    private final Activity activity;
    private final Listener listener;
    private final BillingClient billingClient;
    private final Map<String, ProductDetails> products = new HashMap<>();
    private boolean connecting;
    private boolean ready;

    public PlayBillingManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        billingClient = BillingClient.newBuilder(activity.getApplicationContext())
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build())
                .enableAutoServiceReconnection()
                .build();
    }

    public void connectAndLoad() {
        if (!billingReleaseAvailable()) {
            connecting = false;
            ready = false;
            products.clear();
            listener.onBillingReady(Collections.emptyMap());
            return;
        }
        if (ready) {
            queryProducts();
            return;
        }
        if (connecting) return;
        connecting = true;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                connecting = false;
                ready = billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
                if (ready) {
                    queryProducts();
                } else {
                    listener.onBillingMessage("Google Play 결제를 준비하지 못했어요. 잠시 후 다시 시도해주세요.");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                ready = false;
            }
        });
    }

    public void purchase(String productId) {
        if (!billingReleaseAvailable()) {
            listener.onBillingMessage(playPreparationMessage());
            return;
        }
        if (!FeatureEntitlementStore.PLAN_PHONE.equals(productId)
                && !FeatureEntitlementStore.PLAN_MESSAGE.equals(productId)) {
            listener.onBillingMessage("현재 Google Play에서 판매 중인 상품이 아닙니다.");
            return;
        }
        ProductDetails details = products.get(productId);
        if (!ready || details == null) {
            listener.onBillingMessage("결제 상품을 확인 중이에요. 잠시 후 다시 눌러주세요.");
            connectAndLoad();
            return;
        }
        ProductDetails.SubscriptionOfferDetails selectedOffer = selectPurchaseOffer(
                details.getSubscriptionOfferDetails());
        if (selectedOffer == null) {
            listener.onBillingMessage("Google Play 구독 상품 구성을 확인해주세요. 구매 가능한 기본 요금제를 하나로 유지해야 합니다.");
            return;
        }
        BillingFlowParams.ProductDetailsParams productParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(selectedOffer.getOfferToken())
                        .build();
        BillingFlowParams.Builder builder = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productParams));
        String accountId = obfuscatedAccountId();
        if (!accountId.isEmpty()) builder.setObfuscatedAccountId(accountId);
        BillingResult result = billingClient.launchBillingFlow(activity, builder.build());
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            listener.onBillingMessage(userMessage(result));
        }
    }

    public void restore() {
        if (!billingReleaseAvailable()) {
            listener.onBillingMessage(playPreparationMessage());
            return;
        }
        if (!ready) {
            listener.onBillingMessage("Google Play 연결을 확인하고 있어요.");
            connectAndLoad();
            return;
        }
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .includeSuspendedSubscriptions(true)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                listener.onBillingMessage(userMessage(billingResult));
                return;
            }
            if (purchases == null || purchases.isEmpty()) {
                listener.onBillingMessage("복원할 Google Play 구독이 없습니다.");
                return;
            }
            restoreOnServer(purchases);
        });
    }

    public void close() {
        if (billingClient.isReady()) billingClient.endConnection();
        ready = false;
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (!billingReleaseAvailable()) {
            listener.onBillingMessage(playPreparationMessage());
            return;
        }
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            listener.onBillingMessage("결제가 취소되었습니다.");
            return;
        }
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK
                || purchases == null) {
            listener.onBillingMessage(userMessage(billingResult));
            return;
        }
        for (Purchase purchase : purchases) processPurchase(purchase);
    }

    private void queryProducts() {
        if (!billingReleaseAvailable()) return;
        List<QueryProductDetailsParams.Product> request = new ArrayList<>();
        request.add(subscription(FeatureEntitlementStore.PLAN_PHONE));
        request.add(subscription(FeatureEntitlementStore.PLAN_MESSAGE));
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(request)
                .build();
        billingClient.queryProductDetailsAsync(params, this::handleProductDetails);
    }

    private void handleProductDetails(
            BillingResult billingResult,
            QueryProductDetailsResult result) {
        products.clear();
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && result != null) {
            for (ProductDetails details : result.getProductDetailsList()) {
                if (FeatureEntitlementStore.PLAN_PHONE.equals(details.getProductId())
                        || FeatureEntitlementStore.PLAN_MESSAGE.equals(details.getProductId())) {
                    products.put(details.getProductId(), details);
                }
            }
        }
        listener.onBillingReady(Collections.unmodifiableMap(new HashMap<>(products)));
        if (billingReleaseAvailable() && products.isEmpty()) {
            listener.onBillingMessage("결제 상품을 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
        }
    }

    private QueryProductDetailsParams.Product subscription(String productId) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
    }

    private ProductDetails.SubscriptionOfferDetails selectPurchaseOffer(
            List<ProductDetails.SubscriptionOfferDetails> offers) {
        if (offers == null || offers.isEmpty()) return null;
        if (offers.size() == 1) return offers.get(0);

        ProductDetails.SubscriptionOfferDetails basePlanOnly = null;
        for (ProductDetails.SubscriptionOfferDetails offer : offers) {
            String offerId = offer.getOfferId();
            if (offerId == null || offerId.trim().isEmpty()) {
                if (basePlanOnly != null) return null;
                basePlanOnly = offer;
            }
        }
        return basePlanOnly;
    }

    private void processPurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            listener.onBillingMessage("결제가 보류 중입니다. 결제가 완료되면 이용권이 반영됩니다.");
            return;
        }
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
        String session = AuthSessionStore.session(activity);
        if (session.isEmpty()) {
            listener.onBillingMessage("로그인 정보를 다시 확인해주세요.");
            return;
        }
        String productId = purchase.getProducts().isEmpty()
                ? "" : purchase.getProducts().get(0);
        if (!FeatureEntitlementStore.PLAN_PHONE.equals(productId)
                && !FeatureEntitlementStore.PLAN_MESSAGE.equals(productId)) {
            listener.onBillingMessage("현재 지원하지 않는 Google Play 상품입니다.");
            return;
        }
        new Thread(() -> {
            try {
                JSONObject response = AuthApiClient.verifyGooglePurchase(
                        session,
                        productId,
                        purchase.getPurchaseToken(),
                        purchase.getOrderId());
                FeatureEntitlementStore.saveServerEntitlement(activity, response);
                activity.runOnUiThread(listener::onServerVerified);
            } catch (Exception error) {
                activity.runOnUiThread(() -> listener.onBillingMessage(
                        "결제 확인을 완료하지 못했어요. 구매 복원을 눌러주세요."));
            }
        }, "calltag-play-verify").start();
    }

    private void restoreOnServer(List<Purchase> purchases) {
        String session = AuthSessionStore.session(activity);
        if (session.isEmpty()) {
            listener.onBillingMessage("로그인 정보를 다시 확인해주세요.");
            return;
        }
        JSONArray payload = new JSONArray();
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
            JSONArray supportedProducts = new JSONArray();
            for (String product : purchase.getProducts()) {
                if (FeatureEntitlementStore.PLAN_PHONE.equals(product)
                        || FeatureEntitlementStore.PLAN_MESSAGE.equals(product)) {
                    supportedProducts.put(product);
                }
            }
            if (supportedProducts.length() == 0) continue;
            JSONObject item = new JSONObject();
            try {
                item.put("purchaseToken", purchase.getPurchaseToken());
                item.put("orderId", purchase.getOrderId());
                item.put("products", supportedProducts);
                payload.put(item);
            } catch (Exception ignored) {
                // JSONObject put failures are not expected for primitive values.
            }
        }
        if (payload.length() == 0) {
            listener.onBillingMessage("복원할 완료된 구독이 없습니다.");
            return;
        }
        new Thread(() -> {
            try {
                JSONObject response = AuthApiClient.restoreGooglePurchases(session, payload);
                FeatureEntitlementStore.saveServerEntitlement(activity, response);
                activity.runOnUiThread(listener::onServerVerified);
            } catch (Exception error) {
                activity.runOnUiThread(() -> listener.onBillingMessage(
                        "구매 내역을 복원하지 못했어요. 잠시 후 다시 시도해주세요."));
            }
        }, "calltag-play-restore").start();
    }

    private boolean billingReleaseAvailable() {
        return FeatureEntitlementStore.isPlayBillingAvailable(activity);
    }

    private String playPreparationMessage() {
        FeatureEntitlementStore.Snapshot snapshot = FeatureEntitlementStore.snapshot(activity);
        String message = snapshot.playBillingMessage == null
                ? "앱 결제 기능을 준비하고 있습니다." : snapshot.playBillingMessage.trim();
        return message.isEmpty() ? "앱 결제 기능을 준비하고 있습니다." : message;
    }

    private String obfuscatedAccountId() {
        String source = AuthSessionStore.email(activity);
        if (source == null || source.trim().isEmpty()) return "";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder();
            for (byte item : digest) value.append(String.format("%02x", item));
            return value.toString();
        } catch (Exception error) {
            return "";
        }
    }

    private String userMessage(BillingResult result) {
        int code = result == null ? BillingClient.BillingResponseCode.ERROR
                : result.getResponseCode();
        if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            return "이미 Google Play에서 이용 중입니다. 구매 복원을 눌러주세요.";
        }
        if (code == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE) {
            return "현재 구매할 수 없는 상품입니다.";
        }
        if (code == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
                || code == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
            return "Google Play 연결을 확인해주세요.";
        }
        return "결제를 진행하지 못했어요. 잠시 후 다시 시도해주세요.";
    }
}
