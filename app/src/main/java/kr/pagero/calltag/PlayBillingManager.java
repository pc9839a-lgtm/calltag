package kr.pagero.calltag;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

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

/** Google Play Billing 연결, 상품 조회, 구매, 복원을 담당한다. */
public final class PlayBillingManager implements PurchasesUpdatedListener {
    public interface Listener {
        void onBillingReady(Map<String, ProductDetails> products);
        void onBillingUnavailable(String message);
        void onBillingMessage(String message);
        void onServerVerified();
    }

    private final Activity activity;
    private final Listener listener;
    private final BillingClient billingClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, ProductDetails> products = new HashMap<>();
    private boolean connecting;
    private boolean ready;
    private boolean reconnectAttempted;
    private boolean closed;

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
        if (closed) return;
        if (ready && billingClient.isReady()) {
            queryProducts();
            return;
        }
        if (connecting) return;
        connecting = true;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                connecting = false;
                if (closed) return;
                ready = billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
                if (ready) {
                    reconnectAttempted = false;
                    queryProducts();
                } else {
                    products.clear();
                    listener.onBillingUnavailable(userMessage(billingResult));
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                ready = false;
                if (closed) return;
                if (!reconnectAttempted) {
                    reconnectAttempted = true;
                    mainHandler.postDelayed(() -> {
                        if (!closed) connectAndLoad();
                    }, 700L);
                } else {
                    listener.onBillingUnavailable("Google Play 결제 연결이 끊겼습니다. 다시 시도해주세요.");
                }
            }
        });
    }

    public boolean isReady() {
        return !closed && ready && billingClient.isReady();
    }

    public void purchase(String productId) {
        if (!FeatureEntitlementStore.PLAN_PHONE.equals(productId)
                && !FeatureEntitlementStore.PLAN_MESSAGE.equals(productId)) {
            listener.onBillingMessage("현재 구매할 수 없는 이용권입니다.");
            return;
        }
        ProductDetails details = products.get(productId);
        if (!isReady() || details == null) {
            listener.onBillingMessage("결제 정보를 다시 불러옵니다.");
            connectAndLoad();
            return;
        }
        ProductDetails.SubscriptionOfferDetails selectedOffer = selectPurchaseOffer(
                details.getSubscriptionOfferDetails());
        if (selectedOffer == null) {
            listener.onBillingMessage("이 Google Play 계정에서 사용할 수 있는 결제 옵션이 없습니다.");
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
        if (!isReady()) {
            listener.onBillingMessage("Google Play에 다시 연결합니다.");
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
                listener.onBillingMessage("복원할 구독이 없습니다.");
                return;
            }
            restoreOnServer(purchases);
        });
    }

    public void close() {
        closed = true;
        mainHandler.removeCallbacksAndMessages(null);
        if (billingClient.isReady()) billingClient.endConnection();
        ready = false;
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
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
        if (closed || !isReady()) return;
        List<QueryProductDetailsParams.Product> request = new ArrayList<>();
        request.add(subscription(FeatureEntitlementStore.PLAN_PHONE));
        request.add(subscription(FeatureEntitlementStore.PLAN_MESSAGE));
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(request)
                .build();
        billingClient.queryProductDetailsAsync(params, this::handleProductDetails);
    }

    private void handleProductDetails(BillingResult billingResult, QueryProductDetailsResult result) {
        products.clear();
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            listener.onBillingUnavailable(userMessage(billingResult));
            return;
        }
        if (result != null) {
            for (ProductDetails details : result.getProductDetailsList()) {
                if (FeatureEntitlementStore.PLAN_PHONE.equals(details.getProductId())
                        || FeatureEntitlementStore.PLAN_MESSAGE.equals(details.getProductId())) {
                    products.put(details.getProductId(), details);
                }
            }
        }
        if (products.isEmpty()) {
            listener.onBillingUnavailable(
                    "Google Play에서 이용권을 찾지 못했습니다. 계정 국가와 상품 판매 국가를 확인해주세요.");
            return;
        }
        listener.onBillingReady(Collections.unmodifiableMap(new HashMap<>(products)));
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
            listener.onBillingMessage("결제가 보류 중입니다. 완료되면 이용권이 적용됩니다.");
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
            listener.onBillingMessage("현재 이용할 수 없는 상품입니다.");
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
                        "결제 확인을 완료하지 못했습니다. 구매 복원을 눌러주세요."));
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
                        "구매 내역을 복원하지 못했습니다. 잠시 후 다시 시도해주세요."));
            }
        }, "calltag-play-restore").start();
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
            return "이미 이용 중입니다. 구매 복원을 눌러주세요.";
        }
        if (code == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE) {
            return "현재 계정 또는 국가에서는 이 이용권을 구매할 수 없습니다.";
        }
        if (code == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
            return "이 Google Play 계정에서는 결제를 사용할 수 없습니다. 계정 국가와 결제 프로필을 확인해주세요.";
        }
        if (code == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
                || code == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
                || code == BillingClient.BillingResponseCode.NETWORK_ERROR) {
            return "Google Play 결제 연결을 확인해주세요.";
        }
        if (code == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
            return "Google Play 상품 설정을 확인할 수 없습니다.";
        }
        return "결제를 진행하지 못했습니다. 잠시 후 다시 시도해주세요.";
    }
}
