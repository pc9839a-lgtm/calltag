package kr.pagero.calltag;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** CallTag Universal Lead pull/ack API client. */
public final class UniversalLeadApiClient {
    private static final String[] BASE_URLS = {
            "https://pagero.kr",
            "https://inlet-8mr.pages.dev",
            "https://calltag.pagero.kr",
            "https://agent-calltag-foundation.calltag.pages.dev"
    };

    public static final class Page {
        public final List<UniversalLead> leads;
        public final long nextAfter;
        public final boolean hasMore;

        Page(List<UniversalLead> leads, long nextAfter, boolean hasMore) {
            this.leads = Collections.unmodifiableList(new ArrayList<>(leads));
            this.nextAfter = nextAfter;
            this.hasMore = hasMore;
        }
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

    private UniversalLeadApiClient() {}

    public static Page list(String session, long after, int limit) throws Exception {
        int safeLimit = Math.max(1, Math.min(100, limit));
        // PageRo는 기존 전용 queue/SMS 자동화 경로가 계속 담당한다. canonical dual-write된
        // 같은 PageRo 문의를 Universal 경로에서 다시 import하지 않도록 서버 단계에서 제외한다.
        String path = "/api/calltag/v1/leads?after=" + Math.max(0L, after)
                + "&limit=" + safeLimit
                + "&excludeSourceType=pagero";
        JSONObject response = requestWithFallback("GET", path, null, session);
        JSONArray values = response.optJSONArray("leads");
        List<UniversalLead> leads = new ArrayList<>();
        if (values != null) {
            for (int i = 0; i < values.length(); i++) {
                JSONObject item = values.optJSONObject(i);
                if (item == null) continue;
                leads.add(UniversalLead.fromJson(item));
            }
        }
        return new Page(
                leads,
                response.optLong("nextAfter", after),
                response.optBoolean("hasMore", false));
    }

    public static void acknowledgeImported(
            String session,
            List<Long> leadIds,
            String result) throws Exception {
        acknowledge(session, leadIds, "IMPORTED", result);
    }

    public static void acknowledgeRejected(
            String session,
            long leadId,
            String result) throws Exception {
        List<Long> ids = new ArrayList<>();
        ids.add(leadId);
        acknowledge(session, ids, "REJECTED", result);
    }

    private static void acknowledge(
            String session,
            List<Long> leadIds,
            String status,
            String result) throws Exception {
        if (leadIds == null || leadIds.isEmpty()) return;
        JSONArray ids = new JSONArray();
        for (Long id : leadIds) {
            if (id != null && id > 0L) ids.put(id);
        }
        if (ids.length() == 0) return;
        JSONObject body = new JSONObject()
                .put("leadIds", ids)
                .put("status", status)
                .put("result", result == null ? "" : result.trim());
        requestWithFallback("POST", "/api/calltag/v1/leads/ack", body, session);
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
                ? new Exception("콜태그 문의 서버에 연결하지 못했습니다.")
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
                        "콜태그 문의 서버가 올바른 응답을 보내지 않았습니다.",
                        status,
                        "NON_JSON_RESPONSE");
            }

            if (status < 200 || status >= 300 || !response.optBoolean("ok", true)) {
                JSONObject details = response.optJSONObject("details");
                String code = details == null ? "" : details.optString("code", "");
                throw new ApiException(
                        response.optString("error", "콜태그 문의를 동기화하지 못했습니다."),
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
