package kr.pagero.calltag;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 고객센터 문의 전송 전용 API 클라이언트. */
final class SupportApiClient {
    private static final String[] BASE_URLS = {
            "https://pagero.kr",
            "https://inlet-8mr.pages.dev",
            "https://call.pagero.kr"
    };

    private SupportApiClient() {}

    static JSONObject send(
            String session,
            String type,
            String name,
            String contact,
            String email,
            String message,
            String appVersion) throws Exception {
        JSONObject body = new JSONObject()
                .put("type", clean(type))
                .put("name", clean(name))
                .put("contact", clean(contact))
                .put("email", clean(email).toLowerCase())
                .put("message", clean(message))
                .put("appVersion", clean(appVersion));

        Exception last = null;
        for (String base : BASE_URLS) {
            try {
                return post(base + "/api/call/support", body, session);
            } catch (Exception error) {
                last = error;
            }
        }
        throw last == null ? new Exception("고객센터에 연결하지 못했습니다.") : last;
    }

    private static JSONObject post(String address, JSONObject body, String session) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(20_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("X-CallLink-Client", "android");
            connection.setRequestProperty("X-Pagero-Product", "calltag");
            if (session != null && !session.trim().isEmpty()) {
                connection.setRequestProperty("X-Inlet-Session", session.trim());
            }
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }

            int status = connection.getResponseCode();
            InputStream input = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String raw = read(input);
            JSONObject response = raw.isEmpty() ? new JSONObject() : new JSONObject(raw);
            if (status < 200 || status >= 300 || !response.optBoolean("ok", false)) {
                throw new Exception(response.optString("error", "문의 전송에 실패했습니다."));
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

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
