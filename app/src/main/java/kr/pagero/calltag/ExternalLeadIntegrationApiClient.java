package kr.pagero.calltag;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Signed CallTag integration API client used by the native Android integration hub. */
public final class ExternalLeadIntegrationApiClient {
    private static final String[] BASE_URLS = {
            "https://pagero.kr",
            "https://inlet-8mr.pages.dev",
            "https://calltag.pagero.kr",
            "https://agent-calltag-foundation.calltag.pages.dev"
    };
    public static final String META_ANDROID_RETURN_PATH =
            "/api/calltag/v1/meta/oauth/android-return";

    public static final class ApiException extends Exception {
        public final int status;
        public final String code;

        ApiException(String message, int status, String code) {
            super(message);
            this.status = status;
            this.code = code == null ? "" : code;
        }
    }

    private ExternalLeadIntegrationApiClient() {}

    public static JSONObject listWebhookConnections(String session) throws Exception {
        return requestWithFallback("GET", "/api/calltag/v1/connections", null, session);
    }

    public static JSONObject createWebhookConnection(
            String session,
            String name,
            String sourceName) throws Exception {
        JSONObject body = new JSONObject()
                .put("name", clean(name))
                .put("sourceName", clean(sourceName))
                .put("rawRetentionDays", 7);
        return requestWithFallback("POST", "/api/calltag/v1/connections", body, session);
    }

    public static JSONObject rotateWebhookConnection(String session, String connectionId) throws Exception {
        JSONObject body = new JSONObject()
                .put("action", "rotate_endpoint")
                .put("connectionId", clean(connectionId));
        return requestWithFallback("PATCH", "/api/calltag/v1/connections", body, session);
    }

    public static JSONObject revokeWebhookConnection(String session, String connectionId) throws Exception {
        JSONObject body = new JSONObject()
                .put("action", "revoke")
                .put("connectionId", clean(connectionId));
        return requestWithFallback("PATCH", "/api/calltag/v1/connections", body, session);
    }

    public static JSONObject webhookSamples(String session, String connectionId) throws Exception {
        String encoded = URLEncoder.encode(clean(connectionId), StandardCharsets.UTF_8.toString());
        return requestWithFallback(
                "GET",
                "/api/calltag/v1/connections/" + encoded + "/samples?limit=5",
                null,
                session);
    }

    public static JSONObject updateWebhookMapping(
            String session,
            String connectionId,
            JSONObject mapping) throws Exception {
        JSONObject body = new JSONObject()
                .put("action", "update_mapping")
                .put("connectionId", clean(connectionId))
                .put("mapping", mapping == null ? new JSONObject() : mapping);
        return requestWithFallback("PATCH", "/api/calltag/v1/connections", body, session);
    }

    public static JSONObject listMetaConnections(String session) throws Exception {
        return requestWithFallback("GET", "/api/calltag/v1/meta/connections", null, session);
    }

    public static JSONObject startMetaOauth(String session) throws Exception {
        JSONObject body = new JSONObject().put("returnPath", META_ANDROID_RETURN_PATH);
        return requestWithFallback("POST", "/api/calltag/v1/meta/oauth/start", body, session);
    }

    public static JSONObject metaOauthSession(String session, String oauthId) throws Exception {
        String encoded = URLEncoder.encode(clean(oauthId), StandardCharsets.UTF_8.toString());
        return requestWithFallback(
                "GET",
                "/api/calltag/v1/meta/oauth/session?id=" + encoded,
                null,
                session);
    }

    public static JSONObject completeMetaOauth(
            String session,
            String oauthId,
            JSONArray pageIds) throws Exception {
        JSONObject body = new JSONObject()
                .put("sessionId", clean(oauthId))
                .put("pageIds", pageIds == null ? new JSONArray() : pageIds);
        return requestWithFallback("POST", "/api/calltag/v1/meta/oauth/complete", body, session);
    }

    private static JSONObject requestWithFallback(
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
                if (!shouldTryNext(error)) throw error;
            } catch (Exception error) {
                lastTransport = error;
            }
        }
        if (lastApi != null) throw lastApi;
        throw lastTransport == null
                ? new Exception("외부 연동 서버에 연결하지 못했습니다.")
                : lastTransport;
    }

    private static boolean shouldTryNext(ApiException error) {
        return error.status == 404
                || error.status == 405
                || error.status == 408
                || error.status == 503
                || error.status >= 500
                || "NON_JSON_RESPONSE".equals(error.code);
    }

    private static JSONObject request(
            String address,
            String method,
            JSONObject body,
            String session) throws Exception {
        if (session == null || session.trim().isEmpty()) {
            throw new ApiException("콜태그 로그인이 필요합니다.", 401, "SESSION_REQUIRED");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-Inlet-Session", session.trim());
            connection.setRequestProperty("X-Pagero-Product", "calltag");
            connection.setRequestProperty("X-CallLink-Client", "android");

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
            } catch (Exception invalidJson) {
                throw new ApiException(
                        "외부 연동 서버가 올바른 응답을 보내지 않았습니다.",
                        status,
                        "NON_JSON_RESPONSE");
            }

            if (status < 200 || status >= 300 || !response.optBoolean("ok", true)) {
                JSONObject details = response.optJSONObject("details");
                String code = details == null ? "" : details.optString("code", "");
                if (code.isEmpty()) code = response.optString("code", "");
                throw new ApiException(
                        response.optString("error", "외부 연동 요청을 처리하지 못했습니다."),
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
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        return builder.toString();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
