package kr.pagero.calltag;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class CallTagPushApiClient {
    private static final String[] BASE_URLS = {
            "https://pagero.kr",
            "https://inlet-8mr.pages.dev",
            "https://call.pagero.kr"
    };

    private CallTagPushApiClient() {}

    public static JSONObject register(
            String session,
            String deviceId,
            String token,
            String appVersion) throws Exception {
        return requestWithFallback("POST", "/api/call/push/register", new JSONObject()
                .put("deviceId", clean(deviceId))
                .put("token", clean(token))
                .put("appVersion", clean(appVersion)), session);
    }

    public static JSONObject unregister(String session, String deviceId) throws Exception {
        return requestWithFallback("POST", "/api/call/push/unregister", new JSONObject()
                .put("deviceId", clean(deviceId)), session);
    }

    public static JSONObject status(String session, String deviceId) throws Exception {
        String path = "/api/call/push/status?deviceId="
                + URLEncoder.encode(clean(deviceId), StandardCharsets.UTF_8.name());
        return requestWithFallback("GET", path, null, session);
    }

    private static JSONObject requestWithFallback(
            String method,
            String path,
            JSONObject body,
            String session) throws Exception {
        Exception last = null;
        for (String base : BASE_URLS) {
            try {
                return request(base + path, method, body, session);
            } catch (ApiException error) {
                last = error;
                if (error.status != 404 && error.status != 405
                        && error.status != 408 && error.status < 500) throw error;
            } catch (Exception error) {
                last = error;
            }
        }
        throw last == null ? new Exception("실시간 알림 서버에 연결하지 못했습니다.") : last;
    }

    private static JSONObject request(
            String address,
            String method,
            JSONObject body,
            String session) throws Exception {
        if (clean(session).isEmpty()) throw new ApiException("로그인이 필요합니다.", 401, "SESSION_REQUIRED");
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-Inlet-Session", clean(session));
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
                    ? connection.getInputStream() : connection.getErrorStream();
            String text = read(input);
            JSONObject response;
            try {
                response = text.isEmpty() ? new JSONObject() : new JSONObject(text);
            } catch (JSONException invalid) {
                throw new ApiException("실시간 알림 서버 응답이 올바르지 않습니다.", status, "NON_JSON_RESPONSE");
            }
            if (status < 200 || status >= 300 || !response.optBoolean("ok", true)) {
                JSONObject details = response.optJSONObject("details");
                String code = details == null ? "" : details.optString("code", "");
                throw new ApiException(response.optString("error", "실시간 알림 요청에 실패했습니다."), status, code);
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class ApiException extends Exception {
        public final int status;
        public final String code;

        ApiException(String message, int status, String code) {
            super(message);
            this.status = status;
            this.code = code == null ? "" : code;
        }
    }
}
