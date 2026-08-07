package kr.pagero.calltag;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class AuthApiClient {
    private static final String[] BASE_URLS = {
            "https://pagero.kr",
            "https://inlet-8mr.pages.dev",
            "https://call.pagero.kr"
    };

    public static final class ApiException extends Exception {
        public final int status;
        public final String code;

        ApiException(String message, int status, String code) {
            super(message);
            this.status = status;
            this.code = code == null ? "" : code;
        }
    }

    private AuthApiClient() {}

    public static String googleLoginUrl() {
        return BASE_URLS[0] + "/api/call/google/start?return_scheme=calltag";
    }

    public static JSONObject exchangeGoogleTicket(String ticket) throws Exception {
        return post("/api/call/google/exchange", new JSONObject()
                .put("ticket", clean(ticket)), "");
    }

    public static JSONObject pageroConnection(String session) throws Exception {
        return get("/api/call/pagero/account", session);
    }

    public static JSONObject login(String email, String password) throws Exception {
        return post("/api/call/login", new JSONObject()
                .put("email", normalizeEmail(email))
                .put("password", password == null ? "" : password), "");
    }

    public static JSONObject register(
            String name,
            String phone,
            String email,
            String verificationCode,
            String brandName,
            String industry,
            String password) throws Exception {
        return register(name, phone, email, verificationCode, brandName, industry, password, "");
    }

    public static JSONObject register(
            String name,
            String phone,
            String email,
            String verificationCode,
            String brandName,
            String industry,
            String password,
            String referralCode) throws Exception {
        return post("/api/call/register", new JSONObject()
                .put("name", clean(name))
                .put("phone", clean(phone))
                .put("email", normalizeEmail(email))
                .put("token", clean(verificationCode))
                .put("brandName", clean(brandName))
                .put("industry", clean(industry))
                .put("password", password == null ? "" : password)
                .put("referralCode", clean(referralCode).toUpperCase()), "");
    }

    public static JSONObject requestVerification(String email, String purpose) throws Exception {
        return post("/api/auth/email-verification", new JSONObject()
                .put("email", normalizeEmail(email))
                .put("purpose", normalizePurpose(purpose)), "");
    }

    public static JSONObject resetPassword(
            String email,
            String verificationCode,
            String password) throws Exception {
        return post("/api/auth/password", new JSONObject()
                .put("email", normalizeEmail(email))
                .put("token", clean(verificationCode))
                .put("password", password == null ? "" : password), "");
    }

    public static JSONObject refresh(String session) throws Exception {
        return post("/api/call/session", new JSONObject().put("session", session), session);
    }

    public static JSONObject deleteAccount(String session) throws Exception {
        return post("/api/call/delete-account", new JSONObject()
                .put("session", session)
                .put("confirm", "DELETE"), session);
    }

    public static JSONObject billingEntitlements(String session) throws Exception {
        return get("/api/billing/entitlements", session);
    }

    public static JSONObject billingSubscriptions(String session) throws Exception {
        return get("/api/billing/subscriptions", session);
    }

    public static JSONObject billingReadiness(String session) throws Exception {
        return get("/api/billing/readiness", session);
    }

    public static JSONObject webCheckoutPrecheck(String session, String productCode)
            throws Exception {
        return post("/api/billing/web/precheck", new JSONObject()
                .put("productCode", clean(productCode)), session);
    }

    public static JSONObject referralMe(String session) throws Exception {
        return get("/api/referrals/me", session);
    }

    public static JSONObject referralSummary(String session) throws Exception {
        return get("/api/referrals/summary", session);
    }

    public static JSONObject applyReferral(String session, String code) throws Exception {
        return post("/api/referrals/apply", new JSONObject()
                .put("code", clean(code).toUpperCase()), session);
    }

    public static JSONObject verifyGooglePurchase(
            String session,
            String productId,
            String purchaseToken,
            String orderId) throws Exception {
        return post("/api/billing/google/verify", new JSONObject()
                .put("packageName", "kr.pagero.calltag")
                .put("productId", clean(productId))
                .put("purchaseToken", clean(purchaseToken))
                .put("orderId", clean(orderId)), session);
    }

    public static JSONObject restoreGooglePurchases(
            String session,
            JSONArray purchases) throws Exception {
        return post("/api/billing/google/restore", new JSONObject()
                .put("packageName", "kr.pagero.calltag")
                .put("purchases", purchases == null ? new JSONArray() : purchases), session);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeEmail(String value) {
        return clean(value).toLowerCase();
    }

    private static String normalizePurpose(String purpose) {
        String value = purpose == null ? "signup" : purpose.trim().toLowerCase();
        return "password_reset".equals(value) || "password-reset".equals(value)
                ? "password-reset" : "signup";
    }

    private static JSONObject post(String path, JSONObject body, String session) throws Exception {
        return callWithFallback("POST", path, body, session);
    }

    private static JSONObject get(String path, String session) throws Exception {
        return callWithFallback("GET", path, null, session);
    }

    private static JSONObject callWithFallback(
            String method,
            String path,
            JSONObject body,
            String session) throws Exception {
        ApiException lastApi = null;
        Exception lastTransport = null;
        for (String base : BASE_URLS) {
            try {
                return request(base + path, method, body, session);
            } catch (ApiException error) {
                lastApi = error;
                if (!shouldTryNextBase(error)) throw error;
            } catch (Exception error) {
                lastTransport = error;
            }
        }
        if (lastApi != null) throw lastApi;
        throw lastTransport == null
                ? new Exception("서버에 연결하지 못했습니다.")
                : lastTransport;
    }

    private static boolean shouldTryNextBase(ApiException error) {
        return error.status == 404
                || error.status == 405
                || error.status == 408
                || error.status >= 500
                || "NON_JSON_RESPONSE".equals(error.code);
    }

    private static JSONObject request(
            String address,
            String method,
            JSONObject body,
            String session) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-CallLink-Client", "android");
            connection.setRequestProperty("X-Pagero-Product", "calltag");
            if (session != null && !session.trim().isEmpty()) {
                connection.setRequestProperty("X-Inlet-Session", session.trim());
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload);
                }
            }

            int status = connection.getResponseCode();
            InputStream input = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String text = read(input);
            JSONObject response;
            try {
                response = text.isEmpty() ? new JSONObject() : new JSONObject(text);
            } catch (JSONException invalidJson) {
                throw new ApiException(
                        "로그인 서버가 올바른 응답을 보내지 않았습니다.",
                        status,
                        "NON_JSON_RESPONSE");
            }
            if (status < 200 || status >= 300 || !response.optBoolean("ok", true)) {
                JSONObject details = response.optJSONObject("details");
                String code = details == null ? "" : details.optString("code", "");
                if (code.isEmpty()) code = response.optString("code", "");
                throw new ApiException(
                        response.optString("error", "요청을 처리하지 못했습니다."),
                        status,
                        code);
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }
}
