package kr.pagero.calltag;

import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class CallTagSyncApiClient {
    private static final String[] BASE_URLS = {
            "https://pagero.kr",
            "https://inlet-8mr.pages.dev"
    };

    private CallTagSyncApiClient() {}

    public static JSONObject bootstrap(
            String session,
            String deviceId,
            Long snapshotCursor,
            String afterType,
            String afterId,
            int limit) throws Exception {
        StringBuilder path = new StringBuilder("/api/calltag-sync/bootstrap?limit=")
                .append(Math.max(1, Math.min(100, limit)));
        if (snapshotCursor != null) path.append("&snapshotCursor=").append(snapshotCursor);
        if (afterType != null && !afterType.isEmpty()) {
            path.append("&afterType=").append(url(afterType));
            path.append("&afterId=").append(url(afterId));
        }
        return callWithFallback("GET", path.toString(), null, session, deviceId);
    }

    public static JSONObject pull(
            String session,
            String deviceId,
            long cursor,
            int limit) throws Exception {
        return callWithFallback("GET",
                "/api/calltag-sync/pull?cursor=" + Math.max(0L, cursor)
                        + "&limit=" + Math.max(1, Math.min(100, limit)),
                null, session, deviceId);
    }

    public static JSONObject push(
            String session,
            String deviceId,
            JSONArray items) throws Exception {
        return callWithFallback("POST", "/api/calltag-sync/push",
                new JSONObject().put("items", items == null ? new JSONArray() : items),
                session, deviceId);
    }

    public static JSONObject status(String session, String deviceId) throws Exception {
        return callWithFallback("GET", "/api/calltag-sync/status",
                null, session, deviceId);
    }

    private static JSONObject callWithFallback(
            String method,
            String path,
            JSONObject body,
            String session,
            String deviceId) throws Exception {
        ApiException lastApi = null;
        Exception lastTransport = null;
        for (String base : BASE_URLS) {
            try {
                return request(base + path, method, body, session, deviceId);
            } catch (ApiException error) {
                lastApi = error;
                if (!shouldTryNextBase(error)) throw error;
            } catch (Exception error) {
                lastTransport = error;
            }
        }
        if (lastApi != null) throw lastApi;
        throw lastTransport == null
                ? new Exception("데이터 보호 서버에 연결하지 못했습니다.")
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
            String session,
            String deviceId) throws Exception {
        if (session == null || session.trim().isEmpty()) {
            throw new ApiException("로그인이 필요합니다.", 401, "AUTH_SESSION_REQUIRED");
        }
        if (deviceId == null || deviceId.length() < 16) {
            throw new ApiException("동기화 기기 정보가 필요합니다.", 400,
                    "CALLTAG_SYNC_DEVICE_REQUIRED");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(20_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-Inlet-Session", session.trim());
            connection.setRequestProperty("X-CallTag-Device", deviceId);
            connection.setRequestProperty("X-CallTag-Device-Label", deviceLabel());
            connection.setRequestProperty("X-CallTag-App-Version", BuildConfig.VERSION_NAME);
            connection.setRequestProperty("X-CallLink-Client", "android");
            connection.setRequestProperty("X-Pagero-Product", "calltag");
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
            } catch (JSONException invalidJson) {
                throw new ApiException("동기화 서버가 올바른 응답을 보내지 않았습니다.",
                        status, "NON_JSON_RESPONSE");
            }
            if (status < 200 || status >= 300 || !response.optBoolean("ok", true)) {
                JSONObject details = response.optJSONObject("details");
                String code = details == null ? "" : details.optString("code", "");
                if (code.isEmpty()) code = response.optString("code", "");
                throw new ApiException(
                        response.optString("error", "동기화 요청을 처리하지 못했습니다."),
                        status,
                        code);
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private static String deviceLabel() {
        String manufacturer = Build.MANUFACTURER == null ? "Android" : Build.MANUFACTURER.trim();
        String model = Build.MODEL == null ? "device" : Build.MODEL.trim();
        String value = (manufacturer + " " + model).trim();
        return value.length() > 80 ? value.substring(0, 80) : value;
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

    private static String url(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
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