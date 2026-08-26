package kr.pagero.calltag;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 실제 production Direct API → D1 → FCM best-effort → signed pull → local CRM → ACK 흐름을
 * 현재 로그인 계정/기기에서 검증한다. 테스트용 API Key는 메모리에만 두고 즉시 폐기한다.
 */
public final class ExternalLeadE2eActivity extends Activity {
    private static final String BASE_URL = "https://pagero.kr";
    private static final long REALTIME_WAIT_MS = 8_000L;
    private static final long FALLBACK_RESULT_WAIT_MS = 15_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "calltag-direct-api-e2e");
        thread.setDaemon(true);
        return thread;
    });
    private final SecureRandom random = new SecureRandom();

    private TextView statusView;
    private TextView detailView;
    private Button runButton;
    private volatile boolean waitingForImport;
    private volatile boolean fallbackTriggered;
    private volatile String activePhone = "";
    private volatile String activeEventId = "";
    private boolean receiverRegistered;

    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!waitingForImport || intent == null) return;
            boolean success = intent.getBooleanExtra(UniversalLeadSyncManager.EXTRA_SUCCESS, false);
            String message = intent.getStringExtra(UniversalLeadSyncManager.EXTRA_MESSAGE);
            String errorCode = intent.getStringExtra(UniversalLeadSyncManager.EXTRA_ERROR_CODE);
            boolean imported = hasTestCustomer();

            if (success && imported) {
                waitingForImport = false;
                mainHandler.removeCallbacksAndMessages(null);
                String route = fallbackTriggered ? "안전 pull" : "실시간 동기화";
                showSuccess(route, message == null ? "" : message);
                return;
            }

            if (!success && imported) {
                // CRM 저장 뒤 ACK가 실패했을 수 있다. receipt 기반 재동기화로 중복 import 없이 ACK를 재시도한다.
                setStatus("CRM 저장 완료 · ACK 재확인 중", safe(message));
                fallbackTriggered = true;
                UniversalLeadSyncManager.requestSync(thisContext(), true);
                scheduleFinalTimeout();
            } else if (!success && fallbackTriggered) {
                setStatus("동기화 실패", safe(message) + codeSuffix(errorCode));
                finishProbe(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("외부 문의 수신 테스트");
        setContentView(buildContent());
        ContextCompat.registerReceiver(
                this,
                syncReceiver,
                new IntentFilter(UniversalLeadSyncManager.ACTION_LEADS_UPDATED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
        renderReadyState();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("실제 외부 문의 수신 테스트", 24f, R.color.text_primary);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView description = text(
                "현재 로그인 계정으로 production Direct API 문의 1건을 실제 생성하고, 앱 CRM 저장과 ACK까지 확인합니다. " +
                        "테스트용 API Key는 발급 직후 메모리에서만 사용하고 즉시 폐기합니다.",
                14f,
                R.color.text_secondary);
        LinearLayout.LayoutParams descriptionParams = wrap();
        descriptionParams.topMargin = dp(12);
        root.addView(description, descriptionParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams cardParams = wrap();
        cardParams.topMargin = dp(24);
        root.addView(card, cardParams);

        statusView = text("준비 중", 17f, R.color.text_primary);
        statusView.setTypeface(statusView.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(statusView);

        detailView = text("", 13f, R.color.text_secondary);
        LinearLayout.LayoutParams detailParams = wrap();
        detailParams.topMargin = dp(10);
        card.addView(detailView, detailParams);

        runButton = new Button(this);
        runButton.setText("실제 테스트 시작");
        runButton.setAllCaps(false);
        runButton.setTextSize(15f);
        runButton.setOnClickListener(v -> startProbe());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        buttonParams.topMargin = dp(24);
        root.addView(runButton, buttonParams);

        TextView flow = text(
                "검증 순서\n1. 임시 Direct API Key 발급\n2. 실제 production 문의 접수\n" +
                        "3. 임시 Key 즉시 폐기\n4. FCM 실시간 수신 대기\n" +
                        "5. 필요 시 signed pull 안전 동기화\n6. CRM 저장 + 서버 ACK 확인",
                13f,
                R.color.text_secondary);
        flow.setLineSpacing(0f, 1.22f);
        LinearLayout.LayoutParams flowParams = wrap();
        flowParams.topMargin = dp(22);
        root.addView(flow, flowParams);

        TextView note = text(
                "테스트 고객은 099로 시작하는 비실사용 테스트 번호와 ‘CallTag Direct API 테스트’ 이름으로 CRM에 1건 남습니다.",
                12f,
                R.color.text_muted);
        note.setGravity(Gravity.START);
        LinearLayout.LayoutParams noteParams = wrap();
        noteParams.topMargin = dp(16);
        root.addView(note, noteParams);
        return scroll;
    }

    private void renderReadyState() {
        if (!AuthSessionStore.hasSession(this)) {
            setStatus("로그인 필요", "외부 문의 수신 테스트는 현재 로그인된 CallTag 계정으로만 실행됩니다.");
            runButton.setText("로그인 화면 열기");
            runButton.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
            return;
        }
        setStatus("실제 수신 테스트 준비 완료", "버튼을 누르면 production에 테스트 문의 1건을 생성합니다.");
    }

    private void startProbe() {
        if (waitingForImport) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            renderReadyState();
            return;
        }

        waitingForImport = true;
        fallbackTriggered = false;
        activePhone = testPhone();
        activeEventId = "android-e2e-" + UUID.randomUUID();
        runButton.setEnabled(false);
        setStatus("1/6 · 임시 API Key 발급 중", "production 서버와 현재 로그인 세션을 확인하고 있습니다.");

        executor.execute(() -> runProductionProbe(session, activePhone, activeEventId));
    }

    private void runProductionProbe(String session, String phone, String eventId) {
        String keyId = "";
        String apiKey = "";
        boolean accepted = false;
        String serverResult = "";
        try {
            JSONObject created = postJson(
                    "/api/calltag/v1/keys",
                    new JSONObject()
                            .put("action", "create")
                            .put("name", "Android actual receive test"),
                    "X-Inlet-Session",
                    session,
                    "");
            JSONObject key = created.optJSONObject("key");
            if (key == null) throw new IllegalStateException("임시 API Key 응답이 없습니다.");
            keyId = key.optString("id", "").trim();
            apiKey = key.optString("apiKey", "").trim();
            if (keyId.isEmpty() || apiKey.isEmpty()) {
                throw new IllegalStateException("임시 API Key 발급값이 없습니다.");
            }

            postUi("2/6 · 실제 문의 접수 중", "Direct API로 production canonical intake를 호출합니다.");
            JSONObject lead = new JSONObject()
                    .put("event_id", eventId)
                    .put("external_id", eventId)
                    .put("source", new JSONObject()
                            .put("type", "direct_api")
                            .put("name", "Direct API")
                            .put("provider", "direct_api"))
                    .put("customer", new JSONObject()
                            .put("name", "CallTag Direct API 테스트")
                            .put("phone", phone))
                    .put("inquiry", new JSONObject()
                            .put("content", "CallTag Android 실제 외부 문의 수신 E2E 테스트"))
                    .put("metadata", new JSONObject()
                            .put("test", true)
                            .put("origin", "android_actual_receive_test"));
            JSONObject response = postJson(
                    "/api/calltag/v1/leads",
                    lead,
                    "Authorization",
                    "Bearer " + apiKey,
                    eventId);
            accepted = response.optBoolean("ok", false);
            serverResult = response.optString("result", "");
            String responseEventId = response.optString("eventId", "");
            if (!accepted || (!responseEventId.isEmpty() && !eventId.equals(responseEventId))) {
                throw new IllegalStateException("production 문의 접수 확인에 실패했습니다.");
            }
        } catch (Exception error) {
            postFailure("서버 접수 실패", message(error));
        } finally {
            // 원문 key는 이 메서드의 로컬 변수에서만 존재한다. 화면/Preferences/DB에 저장하지 않는다.
            if (!keyId.isEmpty()) {
                boolean revoked = revokeWithRetry(session, keyId);
                if (!revoked) {
                    postFailure("임시 API Key 폐기 실패",
                            "문의 접수 테스트용 Key를 자동 폐기하지 못했습니다. 외부 문의 연동의 Direct API에서 활성 Key를 확인해주세요.");
                    apiKey = "";
                    return;
                }
            }
            apiKey = "";
        }

        if (!accepted) return;
        final String resultText = serverResult;
        mainHandler.post(() -> {
            if (!waitingForImport) return;
            setStatus("3/6 · 서버 접수 완료", "결과 " + safe(resultText) + " · 임시 API Key 폐기 완료");
            mainHandler.postDelayed(this::fallbackPullIfNeeded, REALTIME_WAIT_MS);
        });
    }

    private boolean revokeWithRetry(String session, String keyId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                postJson(
                        "/api/calltag/v1/keys",
                        new JSONObject().put("action", "revoke").put("keyId", keyId),
                        "X-Inlet-Session",
                        session,
                        "");
                return true;
            } catch (Exception ignored) {
                try { Thread.sleep(350L); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void fallbackPullIfNeeded() {
        if (!waitingForImport) return;
        if (hasTestCustomer()) {
            // 아주 드물게 브로드캐스트를 놓쳤을 경우 서버 ACK를 확정하기 위해 한 번 더 pull한다.
            fallbackTriggered = true;
            setStatus("5/6 · CRM 확인 · ACK 재확인", "테스트 고객이 저장됐습니다. 서버 ACK 상태를 한 번 더 확인합니다.");
        } else {
            fallbackTriggered = true;
            setStatus("4/6 · FCM 대기 완료 · 안전 pull", "실시간 신호가 없거나 늦어 signed pull로 동일 문의를 확인합니다.");
        }
        UniversalLeadSyncManager.requestSync(this, true);
        scheduleFinalTimeout();
    }

    private void scheduleFinalTimeout() {
        mainHandler.postDelayed(() -> {
            if (!waitingForImport) return;
            if (hasTestCustomer()) {
                setStatus("CRM 저장 확인 · ACK 응답 대기 초과",
                        "고객은 저장됐지만 동기화 성공 응답을 확인하지 못했습니다. 지금 문의 확인을 다시 실행해 ACK를 재시도할 수 있습니다.");
            } else {
                setStatus("수신 테스트 실패",
                        "production 접수 후 CRM에 테스트 고객이 생성되지 않았습니다. 로그인/서버 연결/동기화 상태를 확인해주세요.");
            }
            finishProbe(false);
        }, FALLBACK_RESULT_WAIT_MS);
    }

    private JSONObject postJson(
            String path,
            JSONObject body,
            String authHeader,
            String authValue,
            String idempotencyKey) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(20_000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty(authHeader, authValue);
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                connection.setRequestProperty("Idempotency-Key", idempotencyKey);
            }
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
            int code = connection.getResponseCode();
            String responseBody = readBody(code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream());
            JSONObject response = responseBody.trim().isEmpty()
                    ? new JSONObject() : new JSONObject(responseBody);
            if (code < 200 || code >= 300 || !response.optBoolean("ok", false)) {
                String error = response.optString("error", "HTTP " + code);
                throw new IllegalStateException(error + " (" + code + ")");
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private static String readBody(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && value.length() < 262_144) {
                value.append(line);
            }
        }
        return value.toString();
    }

    private boolean hasTestCustomer() {
        String phone = activePhone;
        if (phone == null || phone.isEmpty()) return false;
        try (CallTagDbHelper db = new CallTagDbHelper(this)) {
            return db.findByPhone(phone) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String testPhone() {
        int value = random.nextInt(100_000_000);
        return "099" + String.format(java.util.Locale.US, "%08d", value);
    }

    private void showSuccess(String route, String syncMessage) {
        setStatus("6/6 · 실제 수신 테스트 성공",
                "Direct API → production D1 → " + route + " → CRM 저장 → ACK 완료\n" +
                        "테스트 번호: " + activePhone +
                        (syncMessage.isEmpty() ? "" : "\n" + syncMessage));
        finishProbe(true);
    }

    private void postUi(String status, String detail) {
        mainHandler.post(() -> setStatus(status, detail));
    }

    private void postFailure(String status, String detail) {
        mainHandler.post(() -> {
            if (!waitingForImport) return;
            setStatus(status, detail);
            finishProbe(false);
        });
    }

    private void finishProbe(boolean success) {
        waitingForImport = false;
        mainHandler.removeCallbacksAndMessages(null);
        runButton.setEnabled(true);
        runButton.setText(success ? "테스트 다시 실행" : "다시 시도");
    }

    private void setStatus(String status, String detail) {
        statusView.setText(status == null ? "" : status);
        detailView.setText(detail == null ? "" : detail);
    }

    private Context thisContext() {
        return this;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String message(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()) {
            return "외부 문의 테스트 중 오류가 발생했습니다.";
        }
        return error.getMessage().trim();
    }

    private static String codeSuffix(String code) {
        String safe = safe(code);
        return safe.isEmpty() ? "" : " (" + safe + ")";
    }

    private TextView text(String value, float size, int colorRes) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(colorRes));
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        waitingForImport = false;
        mainHandler.removeCallbacksAndMessages(null);
        if (receiverRegistered) {
            try { unregisterReceiver(syncReceiver); } catch (Exception ignored) {}
            receiverRegistered = false;
        }
        executor.shutdownNow();
        super.onDestroy();
    }
}
