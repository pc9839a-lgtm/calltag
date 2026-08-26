package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native CallTag hub for receiving and configuring external lead channels. */
public final class ExternalLeadIntegrationActivity extends Activity {
    private static final String GOOGLE_FORMS_SOURCE = "Google Forms";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private TextView receiverBadge;
    private TextView syncMessage;
    private TextView sourceCustomerCount;
    private TextView remoteSummary;
    private LinearLayout channelList;
    private Button syncButton;
    private boolean receiverRegistered;
    private boolean remoteLoading;
    private JSONArray webhookConnections = new JSONArray();
    private JSONArray apiKeys = new JSONArray();
    private JSONArray metaConnections = new JSONArray();
    private String transientSecret = "";

    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !UniversalLeadSyncManager.ACTION_LEADS_UPDATED.equals(intent.getAction())) return;
            boolean success = intent.getBooleanExtra(UniversalLeadSyncManager.EXTRA_SUCCESS, false);
            int imported = intent.getIntExtra(UniversalLeadSyncManager.EXTRA_IMPORTED, 0);
            int updated = intent.getIntExtra(UniversalLeadSyncManager.EXTRA_UPDATED, 0);
            int rejected = intent.getIntExtra(UniversalLeadSyncManager.EXTRA_REJECTED, 0);
            String message = intent.getStringExtra(UniversalLeadSyncManager.EXTRA_MESSAGE);
            if (message == null || message.trim().isEmpty()) {
                message = success ? "외부 문의 확인을 완료했습니다." : "외부 문의를 확인하지 못했습니다.";
            }
            if (success && (imported > 0 || updated > 0 || rejected > 0)) {
                String counts = "신규 " + imported + "건 · 기존 고객 " + updated + "건";
                if (rejected > 0) counts += " · 확인 필요 " + rejected + "건";
                syncMessage.setText(counts + "\n" + message);
            } else {
                syncMessage.setText(message);
            }
            setReceiverBadge(success ? "확인 완료" : "확인 필요", success);
            finishSyncButton();
            refreshLocalStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("외부 문의 연동");
        setContentView(buildContent());
        refreshLocalStatus();
        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(UniversalLeadSyncManager.ACTION_LEADS_UPDATED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(syncReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(syncReceiver, filter);
        }
        receiverRegistered = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLocalStatus();
        refreshRemoteStatus();
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(syncReceiver);
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        transientSecret = "";
        io.shutdownNow();
        super.onDestroy();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(body, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = titleText("‹", 32f);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setContentDescription("뒤로가기");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));
        TextView title = titleText("외부 문의 연동", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(4);
        header.addView(title, titleParams);
        body.addView(header, matchWrap());

        LinearLayout hero = card();
        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        heroText.addView(titleText("외부 문의 수신", 18f), matchWrap());
        heroText.addView(mutedText("연결된 채널의 새 문의를 콜태그 고객으로 가져옵니다."), topMargin(5));
        heroTop.addView(heroText, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        receiverBadge = badge("확인 중", false);
        heroTop.addView(receiverBadge);
        hero.addView(heroTop, matchWrap());

        syncMessage = bodyText("앱 수신 상태를 확인하고 있습니다.");
        syncMessage.setLineSpacing(0f, 1.22f);
        hero.addView(syncMessage, topMargin(14));

        sourceCustomerCount = mutedText("외부 출처 고객 확인 중");
        hero.addView(sourceCustomerCount, topMargin(9));

        syncButton = actionButton("지금 문의 확인", true);
        syncButton.setOnClickListener(v -> requestLeadSync());
        hero.addView(syncButton, fixedTop(50, 15));
        body.addView(hero, topMargin(12));

        body.addView(sectionTitle("연동 채널"), topMargin(24));
        body.addView(mutedText("이 화면에서 채널을 직접 연결하고 관리합니다. 웹 홈으로 이동시키지 않습니다."), topMargin(5));
        remoteSummary = mutedText("연동 상태 확인 중");
        body.addView(remoteSummary, topMargin(8));

        channelList = new LinearLayout(this);
        channelList.setOrientation(LinearLayout.VERTICAL);
        body.addView(channelList, topMargin(4));
        renderChannels();

        LinearLayout safety = card();
        safety.addView(sectionTitle("앱 수신 방식"), matchWrap());
        safety.addView(bodyText("• 알림 신호에는 고객 이름·전화번호를 넣지 않습니다.\n"
                + "• 앱이 로그인 세션으로 서버에서 실제 문의를 가져옵니다.\n"
                + "• 고객 저장이 끝난 뒤에만 서버에 완료 처리합니다.\n"
                + "• Webhook URL과 API Key는 발급 직후 한 번만 화면에 표시합니다."), topMargin(11));
        body.addView(safety, topMargin(18));

        return scroll;
    }

    private void renderChannels() {
        if (channelList == null) return;
        channelList.removeAllViews();

        addChannelCard(
                "PageRo",
                "앱에서 관리",
                true,
                "페이지로 랜딩 문의는 기존 전용 수신 경로와 문자 자동화를 그대로 사용합니다.",
                "페이지로 연결 관리",
                v -> startActivity(new Intent(this, PageroConnectionCompactActivity.class)),
                null,
                null);

        int metaCount = activeCount(metaConnections, null, false);
        addChannelCard(
                "Meta Lead Ads",
                metaCount > 0 ? metaCount + "개 연결" : "미연결",
                metaCount > 0,
                "앱에서 Meta 로그인을 시작하고, 관리 중인 Facebook 페이지를 선택해 연결합니다.",
                "Meta 연결",
                v -> startMetaOauth((Button) v),
                metaCount > 0 ? "연결 목록" : null,
                metaCount > 0 ? v -> showMetaConnections() : null);

        int googleCount = activeCount(webhookConnections, GOOGLE_FORMS_SOURCE, true);
        JSONObject google = latestWebhookConnection(GOOGLE_FORMS_SOURCE, true);
        addChannelCard(
                "Google Forms",
                googleCount > 0 ? googleFormsState(google) : "미연결",
                googleCount > 0 && google != null && google.optBoolean("mappingReady", false),
                "앱에서 Webhook을 만들고 URL이 들어간 Apps Script를 복사한 뒤 테스트·전화번호 매핑까지 확인합니다.",
                googleCount > 0 ? "새 Google Forms 연결" : "Google Forms 연결",
                v -> createGoogleForms((Button) v),
                googleCount > 0 ? "테스트·매핑 확인" : null,
                googleCount > 0 ? v -> checkGoogleForms(google) : null);

        int webhookCount = activeCount(webhookConnections, GOOGLE_FORMS_SOURCE, false);
        JSONObject webhook = latestWebhookConnection(GOOGLE_FORMS_SOURCE, false);
        addChannelCard(
                "Generic Webhook",
                webhookCount > 0 ? webhookCount + "개 연결" : "미연결",
                webhookCount > 0,
                "외부 폼·CRM·자동화 서비스용 전용 Webhook URL을 앱에서 직접 발급합니다.",
                "Webhook 만들기",
                v -> createGenericWebhook((Button) v),
                webhookCount > 0 ? "Webhook 관리" : null,
                webhookCount > 0 ? v -> manageWebhook(webhook) : null);

        int keyCount = activeKeyCount();
        JSONObject key = latestActiveKey();
        addChannelCard(
                "Direct API",
                keyCount > 0 ? keyCount + "개 활성 키" : "미연결",
                keyCount > 0,
                "외부 서버가 CallTag Lead API로 문의를 직접 보낼 때 사용할 API Key를 앱에서 발급합니다.",
                "API Key 발급",
                v -> createDirectApiKey((Button) v),
                keyCount > 0 ? "API Key 관리" : null,
                keyCount > 0 ? v -> manageApiKey(key) : null);
    }

    private void addChannelCard(
            String name,
            String state,
            boolean positive,
            String description,
            String primaryLabel,
            View.OnClickListener primaryListener,
            String secondaryLabel,
            View.OnClickListener secondaryListener) {
        LinearLayout item = card();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(titleText(name, 16f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge(state, positive));
        item.addView(top, matchWrap());
        item.addView(mutedText(description), topMargin(8));

        Button primary = actionButton(primaryLabel, true);
        primary.setOnClickListener(primaryListener);
        item.addView(primary, fixedTop(48, 13));

        if (secondaryLabel != null && secondaryListener != null) {
            Button secondary = actionButton(secondaryLabel, false);
            secondary.setOnClickListener(secondaryListener);
            item.addView(secondary, fixedTop(46, 8));
        }
        channelList.addView(item, topMargin(10));
    }

    private void refreshRemoteStatus() {
        if (!AuthSessionStore.hasSession(this) || remoteLoading) return;
        remoteLoading = true;
        if (remoteSummary != null) remoteSummary.setText("연동 상태 확인 중...");
        final String session = AuthSessionStore.session(this);
        io.execute(() -> {
            JSONArray webhooks = null;
            JSONArray keys = null;
            JSONArray metas = null;
            Exception lastError = null;
            try {
                webhooks = ExternalLeadIntegrationApiClient
                        .listWebhookConnections(session)
                        .optJSONArray("connections");
            } catch (Exception error) {
                lastError = error;
            }
            try {
                keys = ExternalLeadIntegrationApiClient
                        .listApiKeys(session)
                        .optJSONArray("keys");
            } catch (Exception error) {
                lastError = error;
            }
            try {
                metas = ExternalLeadIntegrationApiClient
                        .listMetaConnections(session)
                        .optJSONArray("connections");
            } catch (Exception error) {
                lastError = error;
            }
            final JSONArray finalWebhooks = webhooks;
            final JSONArray finalKeys = keys;
            final JSONArray finalMetas = metas;
            final Exception finalError = lastError;
            runOnUiThread(() -> {
                remoteLoading = false;
                if (isFinishing() || isDestroyed()) return;
                if (finalWebhooks != null) webhookConnections = finalWebhooks;
                if (finalKeys != null) apiKeys = finalKeys;
                if (finalMetas != null) metaConnections = finalMetas;
                renderChannels();
                if (remoteSummary != null) {
                    if (finalWebhooks != null || finalKeys != null || finalMetas != null) {
                        remoteSummary.setText("앱에서 직접 연결 가능 · 상태 최신화 완료");
                    } else {
                        remoteSummary.setText(userFacingError(finalError));
                    }
                }
            });
        });
    }

    private void createGoogleForms(Button button) {
        runApi(button, "생성 중...", session ->
                        ExternalLeadIntegrationApiClient.createWebhookConnection(
                                session, "Google Forms", GOOGLE_FORMS_SOURCE),
                result -> {
                    JSONObject connection = result.optJSONObject("connection");
                    if (connection != null) prependWebhookConnection(connection);
                    String endpoint = result.optString("endpointUrl", "");
                    if (endpoint.isEmpty()) {
                        toast("Webhook URL을 받지 못했습니다.");
                        return;
                    }
                    showOneTimeValue(
                            "Google Forms 연결 준비 완료",
                            googleFormsScript(endpoint),
                            "URL이 이미 들어간 Apps Script입니다. Google Form → Apps Script에 붙여넣고 installCallTag()를 한 번 실행하세요.",
                            "Apps Script 복사");
                    renderChannels();
                });
    }

    private void checkGoogleForms(JSONObject connection) {
        if (connection == null) return;
        String id = connection.optString("id", "");
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.webhookSamples(session, id), result -> {
            JSONObject current = result.optJSONObject("connection");
            if (current != null) replaceWebhookConnection(current);
            if (current != null && current.optBoolean("mappingReady", false)) {
                new AlertDialog.Builder(this)
                        .setTitle("Google Forms 수집 준비 완료")
                        .setMessage("테스트 응답 수신과 전화번호 매핑이 완료되어 있습니다.")
                        .setPositiveButton("확인", null)
                        .show();
                renderChannels();
                return;
            }
            JSONArray samples = result.optJSONArray("samples");
            if (samples == null || samples.length() == 0) {
                new AlertDialog.Builder(this)
                        .setTitle("테스트 응답이 필요합니다")
                        .setMessage("Google Form에서 테스트 응답을 1건 제출한 뒤 다시 '테스트·매핑 확인'을 눌러주세요.")
                        .setPositiveButton("확인", null)
                        .show();
                return;
            }
            JSONObject latest = samples.optJSONObject(0);
            JSONObject mapper = latest == null ? null : latest.optJSONObject("mapper");
            JSONObject draft = mapper == null ? null : mapper.optJSONObject("draftMapping");
            String phonePointer = draft == null ? "" : draft.optString("phone", "");
            if (phonePointer.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("전화번호 후보를 찾지 못했습니다")
                        .setMessage("폼 질문 제목에 '전화번호', '연락처', '휴대폰' 등을 사용한 뒤 테스트 응답을 다시 보내주세요.")
                        .setPositiveButton("확인", null)
                        .show();
                return;
            }
            JSONObject mapping = mergeRecommendedMapping(current, draft);
            new AlertDialog.Builder(this)
                    .setTitle("추천 전화번호 매핑")
                    .setMessage(phonePointer + " 항목을 전화번호로 사용합니다.\n\n테스트 응답 자체는 고객으로 자동 등록하지 않습니다.")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("매핑 저장", (dialog, which) ->
                            saveGoogleFormsMapping(id, mapping))
                    .show();
        });
    }

    private void saveGoogleFormsMapping(String connectionId, JSONObject mapping) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.updateWebhookMapping(
                session, connectionId, mapping), result -> {
            JSONObject connection = result.optJSONObject("connection");
            if (connection != null) replaceWebhookConnection(connection);
            renderChannels();
            new AlertDialog.Builder(this)
                    .setTitle("매핑 저장 완료")
                    .setMessage("이후 Google Forms 제출부터 CallTag 문의로 정상 수집됩니다.")
                    .setPositiveButton("확인", null)
                    .show();
        });
    }

    private JSONObject mergeRecommendedMapping(JSONObject connection, JSONObject draft) {
        JSONObject merged = new JSONObject();
        try {
            JSONObject current = connection == null ? null : connection.optJSONObject("mapping");
            if (current != null) merged = new JSONObject(current.toString());
            String[] roles = {"name", "phone", "email", "content", "externalId", "submittedAt"};
            for (String role : roles) {
                String existing = merged.optString(role, "").trim();
                String recommended = draft == null ? "" : draft.optString(role, "").trim();
                if (existing.isEmpty() && !recommended.isEmpty()) merged.put(role, recommended);
            }
            if (!merged.has("customFields")) merged.put("customFields", new JSONArray());
        } catch (Exception ignored) {
            return draft == null ? new JSONObject() : draft;
        }
        return merged;
    }

    private void createGenericWebhook(Button button) {
        runApi(button, "생성 중...", session ->
                        ExternalLeadIntegrationApiClient.createWebhookConnection(
                                session, "외부 Webhook", "External Webhook"),
                result -> {
                    JSONObject connection = result.optJSONObject("connection");
                    if (connection != null) prependWebhookConnection(connection);
                    String endpoint = result.optString("endpointUrl", "");
                    showOneTimeValue(
                            "Webhook URL 발급 완료",
                            endpoint,
                            "이 URL 자체가 비밀값입니다. 외부 서비스의 Webhook 전송 주소에 넣으세요.",
                            "URL 복사");
                    renderChannels();
                });
    }

    private void manageWebhook(JSONObject connection) {
        if (connection == null) return;
        String id = connection.optString("id", "");
        String name = connection.optString("name", "Webhook");
        String[] actions = {"상태 확인", "URL 재발급", "연결 해제"};
        new AlertDialog.Builder(this)
                .setTitle(name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) checkWebhook(connection);
                    if (which == 1) confirmRotateWebhook(id);
                    if (which == 2) confirmRevokeWebhook(id);
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void checkWebhook(JSONObject connection) {
        String id = connection.optString("id", "");
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.webhookSamples(session, id), result -> {
            JSONObject current = result.optJSONObject("connection");
            if (current != null) replaceWebhookConnection(current);
            JSONArray samples = result.optJSONArray("samples");
            int count = samples == null ? 0 : samples.length();
            boolean ready = current != null && current.optBoolean("mappingReady", false);
            new AlertDialog.Builder(this)
                    .setTitle("Webhook 상태")
                    .setMessage("최근 확인 가능한 샘플 " + count + "건\n전화번호 매핑 " + (ready ? "완료" : "필요"))
                    .setPositiveButton("확인", null)
                    .show();
            renderChannels();
        });
    }

    private void confirmRotateWebhook(String connectionId) {
        new AlertDialog.Builder(this)
                .setTitle("Webhook URL 재발급")
                .setMessage("기존 URL은 즉시 사용할 수 없게 됩니다. 새 URL로 외부 서비스를 반드시 교체해야 합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("재발급", (dialog, which) -> runApi(null, "", session ->
                                ExternalLeadIntegrationApiClient.rotateWebhookConnection(session, connectionId),
                        result -> {
                            JSONObject connection = result.optJSONObject("connection");
                            if (connection != null) replaceWebhookConnection(connection);
                            showOneTimeValue(
                                    "새 Webhook URL",
                                    result.optString("endpointUrl", ""),
                                    "기존 URL은 폐기되었습니다. 새 URL은 지금 한 번만 표시됩니다.",
                                    "URL 복사");
                            renderChannels();
                        }))
                .show();
    }

    private void confirmRevokeWebhook(String connectionId) {
        new AlertDialog.Builder(this)
                .setTitle("Webhook 연결 해제")
                .setMessage("이 연결로 들어오는 새 문의 수신이 중단됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("연결 해제", (dialog, which) -> runApi(null, "", session ->
                                ExternalLeadIntegrationApiClient.revokeWebhookConnection(session, connectionId),
                        result -> {
                            JSONObject connection = result.optJSONObject("connection");
                            if (connection != null) replaceWebhookConnection(connection);
                            renderChannels();
                            toast("Webhook 연결을 해제했습니다.");
                        }))
                .show();
    }

    private void createDirectApiKey(Button button) {
        runApi(button, "발급 중...", session ->
                        ExternalLeadIntegrationApiClient.createApiKey(session, "CallTag Android"),
                result -> {
                    JSONObject key = result.optJSONObject("key");
                    if (key == null) {
                        toast("API Key를 받지 못했습니다.");
                        return;
                    }
                    prependApiKey(key);
                    showOneTimeValue(
                            "Direct API Key 발급 완료",
                            key.optString("apiKey", ""),
                            "이 키는 지금 한 번만 표시됩니다. 외부 서버의 Authorization: Bearer 값으로 사용하세요.",
                            "API Key 복사");
                    renderChannels();
                });
    }

    private void manageApiKey(JSONObject key) {
        if (key == null) return;
        String keyId = key.optString("id", "");
        String prefix = key.optString("keyPrefix", "API Key");
        String[] actions = {"새 키로 교체", "키 해제"};
        new AlertDialog.Builder(this)
                .setTitle(prefix)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) confirmRotateApiKey(keyId);
                    if (which == 1) confirmRevokeApiKey(keyId);
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void confirmRotateApiKey(String keyId) {
        new AlertDialog.Builder(this)
                .setTitle("API Key 교체")
                .setMessage("기존 키는 즉시 폐기됩니다. 외부 서버에 새 키를 바로 적용할 수 있을 때만 진행하세요.")
                .setNegativeButton("취소", null)
                .setPositiveButton("교체", (dialog, which) -> runApi(null, "", session ->
                                ExternalLeadIntegrationApiClient.rotateApiKey(session, keyId),
                        result -> {
                            JSONObject key = result.optJSONObject("key");
                            if (key != null) {
                                markApiKeyRevoked(keyId);
                                prependApiKey(key);
                                showOneTimeValue(
                                        "새 Direct API Key",
                                        key.optString("apiKey", ""),
                                        "기존 키는 폐기되었습니다. 새 키는 지금 한 번만 표시됩니다.",
                                        "API Key 복사");
                            }
                            renderChannels();
                        }))
                .show();
    }

    private void confirmRevokeApiKey(String keyId) {
        new AlertDialog.Builder(this)
                .setTitle("API Key 해제")
                .setMessage("이 키를 사용하는 외부 서버의 문의 전송이 즉시 중단됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("키 해제", (dialog, which) -> runApi(null, "", session ->
                                ExternalLeadIntegrationApiClient.revokeApiKey(session, keyId),
                        result -> {
                            markApiKeyRevoked(keyId);
                            renderChannels();
                            toast("API Key를 해제했습니다.");
                        }))
                .show();
    }

    private void startMetaOauth(Button button) {
        runApi(button, "Meta 여는 중...", ExternalLeadIntegrationApiClient::startMetaOauth, result -> {
            String url = result.optString("authorizationUrl", "");
            Uri uri = Uri.parse(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !(host.equals("facebook.com") || host.endsWith(".facebook.com"))) {
                toast("Meta 인증 주소를 확인할 수 없습니다.");
                return;
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (RuntimeException error) {
                toast("Meta 로그인 화면을 열 수 없습니다.");
            }
        });
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri uri = intent.getData();
        if (!"calltag".equalsIgnoreCase(uri.getScheme())
                || !"external-lead".equalsIgnoreCase(uri.getHost())
                || !"/meta".equals(uri.getPath())) return;
        String state = value(uri.getQueryParameter("meta"));
        String oauthId = value(uri.getQueryParameter("metaOAuth"));
        String reason = value(uri.getQueryParameter("reason"));
        intent.setData(null);
        if ("ready".equals(state) && !oauthId.isEmpty()) {
            loadMetaOauthPages(oauthId);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Meta 연결을 완료하지 못했습니다")
                    .setMessage(reason.isEmpty() ? "Meta 권한 승인 상태를 확인하고 다시 시도해주세요." : reason)
                    .setPositiveButton("확인", null)
                    .show();
        }
    }

    private void loadMetaOauthPages(String oauthId) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.metaOauthSession(session, oauthId), result -> {
            JSONObject oauth = result.optJSONObject("oauth");
            if (oauth == null || !"authorized".equals(oauth.optString("status", ""))) {
                toast("Meta 페이지 선택을 준비하지 못했습니다.");
                return;
            }
            JSONArray pages = oauth.optJSONArray("pages");
            if (pages == null || pages.length() == 0) {
                new AlertDialog.Builder(this)
                        .setTitle("연결 가능한 Meta 페이지가 없습니다")
                        .setMessage("현재 Facebook 계정에서 관리 권한이 있는 페이지를 확인해주세요.")
                        .setPositiveButton("확인", null)
                        .show();
                return;
            }
            showMetaPagePicker(oauthId, pages);
        });
    }

    private void showMetaPagePicker(String oauthId, JSONArray pages) {
        int count = pages.length();
        String[] labels = new String[count];
        boolean[] checked = new boolean[count];
        for (int i = 0; i < count; i++) {
            JSONObject page = pages.optJSONObject(i);
            labels[i] = page == null ? "Meta Page" : page.optString("name", page.optString("id", "Meta Page"));
            checked[i] = true;
        }
        new AlertDialog.Builder(this)
                .setTitle("연결할 Meta 페이지 선택")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("취소", null)
                .setPositiveButton("선택한 페이지 연결", (dialog, which) -> {
                    JSONArray ids = new JSONArray();
                    for (int i = 0; i < count; i++) {
                        if (!checked[i]) continue;
                        JSONObject page = pages.optJSONObject(i);
                        if (page != null && !page.optString("id", "").isEmpty()) ids.put(page.optString("id"));
                    }
                    if (ids.length() == 0) {
                        toast("페이지를 하나 이상 선택해주세요.");
                        return;
                    }
                    completeMetaOauth(oauthId, ids);
                })
                .show();
    }

    private void completeMetaOauth(String oauthId, JSONArray pageIds) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.completeMetaOauth(
                session, oauthId, pageIds), result -> {
            boolean completed = result.optBoolean("completed", false);
            new AlertDialog.Builder(this)
                    .setTitle(completed ? "Meta 연결 완료" : "Meta 연결 확인 필요")
                    .setMessage(completed
                            ? "선택한 Meta 페이지의 Lead Ads 문의가 CallTag로 연결되었습니다."
                            : "일부 페이지 연결을 완료하지 못했습니다. 연결 상태를 다시 확인해주세요.")
                    .setPositiveButton("확인", null)
                    .show();
            refreshRemoteStatus();
        });
    }

    private void showMetaConnections() {
        ArrayList<String> rows = new ArrayList<>();
        for (int i = 0; i < metaConnections.length(); i++) {
            JSONObject item = metaConnections.optJSONObject(i);
            if (item == null || !"active".equalsIgnoreCase(item.optString("status", "active"))) continue;
            String name = item.optString("pageName", item.optString("page_name", "Meta Page"));
            rows.add("• " + name);
        }
        new AlertDialog.Builder(this)
                .setTitle("Meta 연결 목록")
                .setMessage(rows.isEmpty() ? "활성 연결이 없습니다." : joinLines(rows))
                .setPositiveButton("확인", null)
                .show();
    }

    private void showOneTimeValue(String title, String value, String message, String copyLabel) {
        transientSecret = value == null ? "" : value;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(6), dp(20), dp(6));

        TextView notice = bodyText(message);
        content.addView(notice, matchWrap());

        TextView secret = bodyText(transientSecret);
        secret.setTextIsSelectable(true);
        secret.setTypeface(Typeface.MONOSPACE);
        secret.setTextSize(12f);
        secret.setPadding(dp(12), dp(12), dp(12), dp(12));
        secret.setBackgroundResource(R.drawable.bg_input);
        ScrollView scroller = new ScrollView(this);
        scroller.addView(secret, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(300));
        scrollParams.topMargin = dp(12);
        content.addView(scroller, scrollParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(content)
                .setNegativeButton("닫기", null)
                .setPositiveButton(copyLabel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            copyToClipboard(copyLabel, transientSecret);
            toast("복사했습니다.");
        }));
        dialog.setOnDismissListener(ignored -> transientSecret = "");
        dialog.show();
    }

    private void copyToClipboard(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText(label, value == null ? "" : value));
    }

    private String googleFormsScript(String endpoint) {
        return "const CALLTAG_WEBHOOK_URL = '" + endpoint.replace("'", "") + "';\n\n"
                + "function installCallTag() {\n"
                + "  const form = FormApp.getActiveForm();\n"
                + "  if (!form) throw new Error('Google Form에 연결된 Apps Script에서 실행해주세요.');\n"
                + "  ScriptApp.getProjectTriggers()\n"
                + "    .filter((trigger) => trigger.getHandlerFunction() === 'sendToCallTag')\n"
                + "    .forEach((trigger) => ScriptApp.deleteTrigger(trigger));\n"
                + "  ScriptApp.newTrigger('sendToCallTag').forForm(form).onFormSubmit().create();\n"
                + "}\n\n"
                + "function sendToCallTag(e) {\n"
                + "  const form = e.source;\n"
                + "  const response = e.response;\n"
                + "  const answers = {};\n"
                + "  response.getItemResponses().forEach((itemResponse) => {\n"
                + "    const title = itemResponse.getItem().getTitle();\n"
                + "    const raw = itemResponse.getResponse();\n"
                + "    answers[title] = Array.isArray(raw) ? raw.join(', ') : String(raw == null ? '' : raw);\n"
                + "  });\n"
                + "  const responseId = response.getId() || Utilities.getUuid();\n"
                + "  const submittedAt = response.getTimestamp();\n"
                + "  const payload = { source: 'google_forms', form_id: form.getId(), form_title: form.getTitle(),\n"
                + "    response_id: responseId, submitted_at: submittedAt ? submittedAt.toISOString() : new Date().toISOString(), answers };\n"
                + "  const result = UrlFetchApp.fetch(CALLTAG_WEBHOOK_URL, { method: 'post', contentType: 'application/json',\n"
                + "    payload: JSON.stringify(payload), headers: { 'Idempotency-Key': responseId }, muteHttpExceptions: true });\n"
                + "  const status = result.getResponseCode();\n"
                + "  if (status < 200 || status >= 300) throw new Error('CallTag 전송 실패: HTTP ' + status);\n"
                + "}\n";
    }

    private void requestLeadSync() {
        if (!AuthSessionStore.hasSession(this)) {
            toast("콜태그 로그인이 필요합니다.");
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        syncButton.setEnabled(false);
        syncButton.setText("문의 확인 중...");
        setReceiverBadge("확인 중", false);
        syncMessage.setText("서버에서 새 외부 문의를 확인하고 있습니다.");
        boolean started = UniversalLeadSyncManager.requestSync(this, true);
        if (!started && !UniversalLeadSyncManager.isRunning()) {
            syncMessage.setText("동기화를 시작하지 못했습니다. 잠시 후 다시 시도해주세요.");
            setReceiverBadge("확인 필요", false);
            finishSyncButton();
        }
    }

    private void finishSyncButton() {
        syncButton.setEnabled(true);
        syncButton.setText("지금 문의 확인");
    }

    private void refreshLocalStatus() {
        boolean signedIn = AuthSessionStore.hasSession(this);
        if (!UniversalLeadSyncManager.isRunning()) {
            setReceiverBadge(signedIn ? "앱 수신 준비" : "로그인 필요", signedIn);
        }
        int withSource = 0;
        try (CallTagDbHelper db = new CallTagDbHelper(this)) {
            List<Customer> customers = db.listCustomers(null);
            for (Customer customer : customers) {
                String source = customer == null || customer.source == null ? "" : customer.source.trim();
                if (!source.isEmpty()) withSource++;
            }
        } catch (RuntimeException ignored) {
            // Status UI must never block the CRM screen when a local read fails.
        }
        sourceCustomerCount.setText("현재 출처가 기록된 고객 " + withSource + "명");
        if (!signedIn) {
            syncMessage.setText("외부 문의를 받으려면 콜태그 로그인이 필요합니다.");
            if (remoteSummary != null) remoteSummary.setText("로그인 후 앱에서 채널을 연결할 수 있습니다.");
        } else if (!UniversalLeadSyncManager.isRunning()
                && (syncMessage.getText() == null || syncMessage.getText().toString().contains("확인하고 있습니다"))) {
            syncMessage.setText("FCM 신호 수신 + 안전 동기화가 준비되어 있습니다.");
        }
    }

    private void runApi(Button button, String loadingLabel, ApiTask task, ApiSuccess success) {
        if (!AuthSessionStore.hasSession(this)) {
            toast("콜태그 로그인이 필요합니다.");
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        String original = button == null ? "" : String.valueOf(button.getText());
        if (button != null) {
            button.setEnabled(false);
            if (loadingLabel != null && !loadingLabel.isEmpty()) button.setText(loadingLabel);
        }
        final String session = AuthSessionStore.session(this);
        io.execute(() -> {
            try {
                JSONObject result = task.run(session);
                runOnUiThread(() -> {
                    if (button != null) {
                        button.setEnabled(true);
                        button.setText(original);
                    }
                    if (isFinishing() || isDestroyed()) return;
                    success.accept(result);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (button != null) {
                        button.setEnabled(true);
                        button.setText(original);
                    }
                    if (isFinishing() || isDestroyed()) return;
                    new AlertDialog.Builder(this)
                            .setTitle("연동을 처리하지 못했습니다")
                            .setMessage(userFacingError(error))
                            .setPositiveButton("확인", null)
                            .show();
                });
            }
        });
    }

    private String userFacingError(Exception error) {
        if (error instanceof ExternalLeadIntegrationApiClient.ApiException) {
            ExternalLeadIntegrationApiClient.ApiException api =
                    (ExternalLeadIntegrationApiClient.ApiException) error;
            if (api.status == 404 || api.status == 503 || api.status >= 500) {
                return "외부 연동 서버 기능이 아직 현재 서버에 준비되지 않았습니다. 홈으로 이동하지 않고 이 화면에 그대로 있습니다.";
            }
            if (api.status == 401) return "로그인 세션이 만료되었습니다. 다시 로그인해주세요.";
        }
        String message = error == null ? "" : value(error.getMessage());
        return message.isEmpty() ? "외부 연동 서버에 연결하지 못했습니다." : message;
    }

    private int activeCount(JSONArray array, String sourceName, boolean equal) {
        int count = 0;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null || !"active".equalsIgnoreCase(item.optString("status", "active"))) continue;
            if (sourceName == null) {
                count++;
                continue;
            }
            boolean same = sourceName.equals(item.optString("sourceName", "").trim());
            if ((equal && same) || (!equal && !same)) count++;
        }
        return count;
    }

    private JSONObject latestWebhookConnection(String sourceName, boolean equal) {
        for (int i = 0; i < webhookConnections.length(); i++) {
            JSONObject item = webhookConnections.optJSONObject(i);
            if (item == null || !"active".equalsIgnoreCase(item.optString("status", "active"))) continue;
            boolean same = sourceName.equals(item.optString("sourceName", "").trim());
            if ((equal && same) || (!equal && !same)) return item;
        }
        return null;
    }

    private String googleFormsState(JSONObject connection) {
        if (connection == null) return "설정 필요";
        if (connection.optBoolean("mappingReady", false) && connection.optInt("sampleCount", 0) > 0) return "수집 준비";
        if (connection.optInt("sampleCount", 0) > 0) return "매핑 필요";
        return "테스트 필요";
    }

    private int activeKeyCount() {
        int count = 0;
        for (int i = 0; i < apiKeys.length(); i++) {
            JSONObject item = apiKeys.optJSONObject(i);
            if (item != null && "active".equalsIgnoreCase(item.optString("status", ""))) count++;
        }
        return count;
    }

    private JSONObject latestActiveKey() {
        for (int i = 0; i < apiKeys.length(); i++) {
            JSONObject item = apiKeys.optJSONObject(i);
            if (item != null && "active".equalsIgnoreCase(item.optString("status", ""))) return item;
        }
        return null;
    }

    private void prependWebhookConnection(JSONObject connection) {
        JSONArray next = new JSONArray();
        next.put(connection);
        for (int i = 0; i < webhookConnections.length(); i++) next.put(webhookConnections.opt(i));
        webhookConnections = next;
    }

    private void replaceWebhookConnection(JSONObject replacement) {
        if (replacement == null) return;
        String id = replacement.optString("id", "");
        JSONArray next = new JSONArray();
        boolean replaced = false;
        for (int i = 0; i < webhookConnections.length(); i++) {
            JSONObject item = webhookConnections.optJSONObject(i);
            if (item != null && id.equals(item.optString("id", ""))) {
                next.put(replacement);
                replaced = true;
            } else {
                next.put(webhookConnections.opt(i));
            }
        }
        if (!replaced) next.put(replacement);
        webhookConnections = next;
    }

    private void prependApiKey(JSONObject key) {
        JSONArray next = new JSONArray();
        next.put(key);
        for (int i = 0; i < apiKeys.length(); i++) next.put(apiKeys.opt(i));
        apiKeys = next;
    }

    private void markApiKeyRevoked(String keyId) {
        for (int i = 0; i < apiKeys.length(); i++) {
            JSONObject item = apiKeys.optJSONObject(i);
            if (item != null && keyId.equals(item.optString("id", ""))) {
                try { item.put("status", "revoked"); } catch (Exception ignored) {}
            }
        }
    }

    private void setReceiverBadge(String label, boolean positive) {
        if (receiverBadge == null) return;
        receiverBadge.setText(label);
        receiverBadge.setTextColor(getColor(positive ? R.color.success : R.color.text_secondary));
        receiverBadge.setBackground(pillBackground(
                getColor(positive ? R.color.success_soft : R.color.surface_soft),
                getColor(positive ? R.color.success : R.color.border)));
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(16), dp(17), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView badge(String value, boolean positive) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(11f);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(10), dp(6), dp(10), dp(6));
        text.setTextColor(getColor(positive ? R.color.success : R.color.text_secondary));
        text.setBackground(pillBackground(
                getColor(positive ? R.color.success_soft : R.color.surface_soft),
                getColor(positive ? R.color.success : R.color.border)));
        return text;
    }

    private GradientDrawable pillBackground(int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(999));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private TextView titleText(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView sectionTitle(String value) {
        TextView text = titleText(value, 15f);
        text.setTextColor(getColor(R.color.text_secondary));
        return text;
    }

    private TextView bodyText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(14f);
        return text;
    }

    private TextView mutedText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_muted));
        text.setTextSize(12f);
        text.setLineSpacing(0f, 1.18f);
        return text;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(top);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String joinLines(List<String> rows) {
        StringBuilder builder = new StringBuilder();
        for (String row : rows) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(row);
        }
        return builder.toString();
    }

    private interface ApiTask {
        JSONObject run(String session) throws Exception;
    }

    private interface ApiSuccess {
        void accept(JSONObject result);
    }
}
