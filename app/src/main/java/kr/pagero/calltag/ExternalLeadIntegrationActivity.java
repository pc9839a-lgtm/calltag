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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Compact production hub for PageRo, Meta, Google Forms and Webhook integrations. */
public final class ExternalLeadIntegrationActivity extends Activity {
    private static final String GOOGLE_FORMS_SOURCE = "Google Forms";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean receiverRegistered;
    private boolean remoteLoading;
    private TextView receiverBadge;
    private Button syncButton;
    private LinearLayout channelList;
    private JSONArray webhookConnections = new JSONArray();
    private JSONArray metaConnections = new JSONArray();
    private String transientSecret = "";

    // Google Forms guided flow state. Nothing secret is persisted.
    private boolean googleFlow;
    private int googleStep = 1;
    private String googleFormId = "";
    private String googleConnectionId = "";
    private String googleScript = "";
    private JSONObject googleConnection;
    private TextView googleStatus;

    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !UniversalLeadSyncManager.ACTION_LEADS_UPDATED.equals(intent.getAction())) return;
            boolean success = intent.getBooleanExtra(UniversalLeadSyncManager.EXTRA_SUCCESS, false);
            int imported = intent.getIntExtra(UniversalLeadSyncManager.EXTRA_IMPORTED, 0);
            int updated = intent.getIntExtra(UniversalLeadSyncManager.EXTRA_UPDATED, 0);
            int rejected = intent.getIntExtra(UniversalLeadSyncManager.EXTRA_REJECTED, 0);
            setReceiverBadge(success ? "정상" : "확인 필요", success);
            finishSyncButton();
            if (success && imported + updated + rejected > 0) {
                toast("신규 " + imported + " · 갱신 " + updated + (rejected > 0 ? " · 확인 " + rejected : ""));
            } else if (!success) {
                String message = value(intent.getStringExtra(UniversalLeadSyncManager.EXTRA_MESSAGE));
                toast(message.isEmpty() ? "문의 확인에 실패했습니다." : message);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("외부 문의 연동");
        setContentView(buildMainContent());
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
        if (!googleFlow) refreshRemoteStatus();
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
        googleScript = "";
        io.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (googleFlow) {
            closeGoogleFlow();
            return;
        }
        super.onBackPressed();
    }

    private View buildMainContent() {
        googleFlow = false;
        ScrollView scroll = baseScroll();
        LinearLayout body = bodyContainer();
        scroll.addView(body, matchWrapScroll());

        body.addView(header("외부 문의 연동", v -> finish()), matchWrap());

        LinearLayout receiver = card();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(titleText("문의 수신", 16f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        receiverBadge = badge("확인 중", false);
        top.addView(receiverBadge);
        receiver.addView(top, matchWrap());
        syncButton = actionButton("새 문의 확인", true);
        syncButton.setOnClickListener(v -> requestLeadSync());
        receiver.addView(syncButton, fixedTop(48, 12));
        body.addView(receiver, topMargin(10));

        body.addView(sectionTitle("연동"), topMargin(22));
        channelList = new LinearLayout(this);
        channelList.setOrientation(LinearLayout.VERTICAL);
        body.addView(channelList, topMargin(4));
        renderChannels();
        return scroll;
    }

    private void renderChannels() {
        if (channelList == null) return;
        channelList.removeAllViews();

        addChannelCard(
                "PageRo",
                "연결 관리",
                true,
                "관리",
                v -> startActivity(new Intent(this, PageroConnectionCompactActivity.class)),
                null,
                null);

        int metaCount = activeMetaCount();
        addChannelCard(
                "Meta Lead Ads",
                metaCount > 0 ? metaCount + "개 연결" : "미연결",
                metaCount > 0,
                metaCount > 0 ? "추가 연결" : "연결",
                v -> startMetaOauth((Button) v),
                metaCount > 0 ? "연결 목록" : null,
                metaCount > 0 ? v -> showMetaConnections() : null);

        JSONObject google = latestWebhookConnection(GOOGLE_FORMS_SOURCE, true);
        boolean googleReady = google != null && google.optBoolean("mappingReady", false);
        addChannelCard(
                "Google Forms",
                google == null ? "미연결" : (googleReady ? "연결됨" : "설정 필요"),
                googleReady,
                googleReady ? "관리" : "연결",
                v -> openGoogleFlow(googleReady ? 4 : 1, google),
                googleReady ? "새로 연결" : null,
                googleReady ? v -> openGoogleFlow(1, null) : null);

        int webhookCount = activeCount(webhookConnections, GOOGLE_FORMS_SOURCE, false);
        JSONObject webhook = latestWebhookConnection(GOOGLE_FORMS_SOURCE, false);
        addChannelCard(
                "Webhook",
                webhookCount > 0 ? webhookCount + "개 연결" : "미연결",
                webhookCount > 0,
                "Webhook 만들기",
                v -> createGenericWebhook((Button) v),
                webhookCount > 0 ? "관리" : null,
                webhookCount > 0 ? v -> manageWebhook(webhook) : null);
    }

    private void addChannelCard(
            String name,
            String state,
            boolean positive,
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

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button primary = actionButton(primaryLabel, true);
        primary.setOnClickListener(primaryListener);
        actions.addView(primary, new LinearLayout.LayoutParams(0, dp(46), secondaryLabel == null ? 1f : 1.25f));
        if (secondaryLabel != null && secondaryListener != null) {
            Button secondary = actionButton(secondaryLabel, false);
            secondary.setOnClickListener(secondaryListener);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
            params.leftMargin = dp(8);
            actions.addView(secondary, params);
        }
        item.addView(actions, topMargin(12));
        channelList.addView(item, topMargin(10));
    }

    // ------------------------- Google Forms guided flow -------------------------

    private void openGoogleFlow(int step, JSONObject existing) {
        googleFlow = true;
        googleStep = step;
        googleConnection = existing;
        googleConnectionId = existing == null ? "" : existing.optString("id", "");
        googleFormId = "";
        googleScript = "";
        transientSecret = "";
        renderGoogleFlow();
    }

    private void closeGoogleFlow() {
        googleFlow = false;
        googleScript = "";
        transientSecret = "";
        setContentView(buildMainContent());
        refreshLocalStatus();
        refreshRemoteStatus();
    }

    private void renderGoogleFlow() {
        if (!googleFlow) return;
        ScrollView scroll = baseScroll();
        LinearLayout body = bodyContainer();
        scroll.addView(body, matchWrapScroll());
        body.addView(header("Google Forms", v -> closeGoogleFlow()), matchWrap());
        body.addView(googleProgress(), topMargin(12));

        if (googleStep == 1) renderGoogleStep1(body);
        if (googleStep == 2) renderGoogleStep2(body);
        if (googleStep == 3) renderGoogleStep3(body);
        if (googleStep == 4) renderGoogleStep4(body);
        setContentView(scroll);
    }

    private View googleProgress() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"1 폼", "2 설정", "3 테스트", "4 완료"};
        for (int i = 0; i < labels.length; i++) {
            int step = i + 1;
            TextView item = new TextView(this);
            item.setText(labels[i]);
            item.setGravity(Gravity.CENTER);
            item.setTextSize(12f);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            boolean active = step == googleStep;
            boolean done = step < googleStep;
            item.setTextColor(getColor(active || done ? R.color.text_primary : R.color.text_muted));
            item.setBackground(pillBackground(
                    getColor(active ? R.color.primary_soft : R.color.surface_soft),
                    getColor(active ? R.color.primary : R.color.border)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(38), 1f);
            if (i > 0) params.leftMargin = dp(6);
            row.addView(item, params);
        }
        return row;
    }

    private void renderGoogleStep1(LinearLayout body) {
        LinearLayout box = card();
        box.addView(titleText("Google Form 선택", 18f), matchWrap());
        box.addView(mutedText("편집 화면 주소를 붙여넣으세요."), topMargin(7));

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(14f);
        input.setHint("docs.google.com/forms/d/.../edit");
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), 0, dp(14), 0);
        box.addView(input, fixedTop(52, 16));

        TextView hint = mutedText("forms.gle 공유 링크가 아니라 폼 편집 화면의 주소가 필요합니다.");
        box.addView(hint, topMargin(8));

        Button next = actionButton("다음", true);
        next.setOnClickListener(v -> {
            String raw = input.getText() == null ? "" : input.getText().toString();
            String id = extractGoogleFormId(raw);
            if (id.isEmpty()) {
                input.setError("Google Forms 편집 링크를 붙여넣어주세요.");
                return;
            }
            googleFormId = id;
            createGoogleConnection(next);
        });
        box.addView(next, fixedTop(50, 18));
        body.addView(box, topMargin(20));
    }

    private void createGoogleConnection(Button button) {
        runApi(button, "준비 중...", session -> {
            if (googleConnection != null && !googleConnection.optBoolean("mappingReady", false)) {
                String staleId = googleConnection.optString("id", "");
                if (!staleId.isEmpty()) {
                    try { ExternalLeadIntegrationApiClient.revokeWebhookConnection(session, staleId); }
                    catch (Exception ignored) {}
                }
            }
            return ExternalLeadIntegrationApiClient.createWebhookConnection(
                    session, "Google Forms", GOOGLE_FORMS_SOURCE);
        }, result -> {
            JSONObject connection = result.optJSONObject("connection");
            String endpoint = result.optString("endpointUrl", "");
            if (connection == null || endpoint.isEmpty()) {
                toast("Google Forms 연결 준비에 실패했습니다.");
                return;
            }
            googleConnection = connection;
            googleConnectionId = connection.optString("id", "");
            googleScript = googleFormsScript(endpoint, googleFormId);
            transientSecret = googleScript;
            googleStep = 2;
            renderGoogleFlow();
        });
    }

    private void renderGoogleStep2(LinearLayout body) {
        LinearLayout box = card();
        box.addView(titleText("Google 설정", 18f), matchWrap());
        box.addView(bodyText("① 코드 복사  →  ② Apps Script 열기"), topMargin(14));
        box.addView(bodyText("붙여넣기 → 저장 → installCallTag 실행 → 권한 허용"), topMargin(8));

        Button copy = actionButton("1. 코드 복사", true);
        copy.setOnClickListener(v -> {
            copyToClipboard("CallTag Google Forms", googleScript);
            toast("연결 코드가 복사됐습니다.");
        });
        box.addView(copy, fixedTop(50, 18));

        Button open = actionButton("2. Apps Script 열기", false);
        open.setOnClickListener(v -> openUrl(Uri.parse("https://script.new")));
        box.addView(open, fixedTop(50, 9));

        Button done = actionButton("3. 설정 완료", true);
        done.setOnClickListener(v -> {
            googleStep = 3;
            renderGoogleFlow();
        });
        box.addView(done, fixedTop(50, 9));
        body.addView(box, topMargin(20));
    }

    private void renderGoogleStep3(LinearLayout body) {
        LinearLayout box = card();
        box.addView(titleText("테스트 응답", 18f), matchWrap());
        box.addView(bodyText("전화번호가 포함된 테스트 응답 1건을 제출하세요."), topMargin(12));

        Button form = actionButton("Google Form 열기", false);
        form.setOnClickListener(v -> {
            if (googleFormId.isEmpty()) {
                toast("폼 주소를 다시 연결해주세요.");
                googleStep = 1;
                renderGoogleFlow();
                return;
            }
            openUrl(Uri.parse("https://docs.google.com/forms/d/" + googleFormId + "/viewform"));
        });
        box.addView(form, fixedTop(50, 16));

        Button check = actionButton("연결 확인", true);
        check.setOnClickListener(v -> checkGoogleForms(check));
        box.addView(check, fixedTop(50, 9));

        googleStatus = mutedText("");
        box.addView(googleStatus, topMargin(10));
        body.addView(box, topMargin(20));
    }

    private void checkGoogleForms(Button button) {
        if (googleConnectionId.isEmpty()) {
            googleStep = 1;
            renderGoogleFlow();
            return;
        }
        runApi(button, "확인 중...", session ->
                        ExternalLeadIntegrationApiClient.webhookSamples(session, googleConnectionId),
                result -> {
                    JSONObject current = result.optJSONObject("connection");
                    if (current != null) googleConnection = current;
                    if (current != null && current.optBoolean("mappingReady", false)) {
                        googleStep = 4;
                        renderGoogleFlow();
                        return;
                    }
                    JSONArray samples = result.optJSONArray("samples");
                    if (samples == null || samples.length() == 0) {
                        setGoogleStatus("아직 테스트 응답이 없습니다.");
                        return;
                    }
                    JSONObject latest = samples.optJSONObject(0);
                    JSONObject mapper = latest == null ? null : latest.optJSONObject("mapper");
                    JSONObject draft = mapper == null ? null : mapper.optJSONObject("draftMapping");
                    String phone = draft == null ? "" : draft.optString("phone", "").trim();
                    if (phone.isEmpty()) {
                        setGoogleStatus("전화번호 항목을 찾지 못했습니다. 질문 제목을 ‘전화번호’로 바꿔 다시 제출하세요.");
                        return;
                    }
                    JSONObject mapping = mergeRecommendedMapping(current, draft);
                    saveGoogleFormsMapping(button, mapping);
                });
    }

    private void saveGoogleFormsMapping(Button button, JSONObject mapping) {
        runApi(button, "연결 중...", session ->
                        ExternalLeadIntegrationApiClient.updateWebhookMapping(
                                session, googleConnectionId, mapping),
                result -> {
                    JSONObject connection = result.optJSONObject("connection");
                    if (connection != null) googleConnection = connection;
                    googleStep = 4;
                    renderGoogleFlow();
                });
    }

    private void renderGoogleStep4(LinearLayout body) {
        LinearLayout box = card();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(titleText("Google Forms", 18f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge("연결됨", true));
        box.addView(top, matchWrap());

        Button back = actionButton("완료", true);
        back.setOnClickListener(v -> closeGoogleFlow());
        box.addView(back, fixedTop(50, 20));
        body.addView(box, topMargin(20));
    }

    private String extractGoogleFormId(String raw) {
        String text = value(raw);
        if (text.matches("[A-Za-z0-9_-]{20,}")) return text;
        try {
            Uri uri = Uri.parse(text);
            if (!"docs.google.com".equalsIgnoreCase(uri.getHost())) return "";
            List<String> segments = uri.getPathSegments();
            for (int i = 0; i + 1 < segments.size(); i++) {
                if (!"d".equals(segments.get(i))) continue;
                String candidate = value(segments.get(i + 1));
                if ("e".equals(candidate)) return "";
                if (candidate.matches("[A-Za-z0-9_-]{20,}")) return candidate;
            }
        } catch (RuntimeException ignored) {}
        return "";
    }

    private String googleFormsScript(String endpoint, String formId) {
        String safeEndpoint = endpoint.replace("'", "");
        String safeFormId = formId.replace("'", "");
        return "const CALLTAG_WEBHOOK_URL = '" + safeEndpoint + "';\n"
                + "const CALLTAG_FORM_ID = '" + safeFormId + "';\n\n"
                + "function installCallTag() {\n"
                + "  const form = FormApp.openById(CALLTAG_FORM_ID);\n"
                + "  ScriptApp.getProjectTriggers()\n"
                + "    .filter(t => t.getHandlerFunction() === 'sendToCallTag')\n"
                + "    .forEach(t => ScriptApp.deleteTrigger(t));\n"
                + "  ScriptApp.newTrigger('sendToCallTag').forForm(form).onFormSubmit().create();\n"
                + "}\n\n"
                + "function sendToCallTag(e) {\n"
                + "  const form = FormApp.openById(CALLTAG_FORM_ID);\n"
                + "  const response = e.response;\n"
                + "  const answers = {};\n"
                + "  response.getItemResponses().forEach(r => {\n"
                + "    const raw = r.getResponse();\n"
                + "    answers[r.getItem().getTitle()] = Array.isArray(raw) ? raw.join(', ') : String(raw == null ? '' : raw);\n"
                + "  });\n"
                + "  const responseId = response.getId() || Utilities.getUuid();\n"
                + "  const submittedAt = response.getTimestamp();\n"
                + "  const payload = { source: 'google_forms', form_id: CALLTAG_FORM_ID, form_title: form.getTitle(),\n"
                + "    response_id: responseId, submitted_at: submittedAt ? submittedAt.toISOString() : new Date().toISOString(), answers };\n"
                + "  const result = UrlFetchApp.fetch(CALLTAG_WEBHOOK_URL, { method: 'post', contentType: 'application/json',\n"
                + "    payload: JSON.stringify(payload), headers: { 'Idempotency-Key': responseId }, muteHttpExceptions: true });\n"
                + "  const status = result.getResponseCode();\n"
                + "  if (status < 200 || status >= 300) throw new Error('CallTag HTTP ' + status + ': ' + result.getContentText());\n"
                + "}\n";
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

    private void setGoogleStatus(String text) {
        if (googleStatus != null) googleStatus.setText(text == null ? "" : text);
    }

    // ------------------------- Meta -------------------------

    private void startMetaOauth(Button button) {
        runApi(button, "Meta 여는 중...", ExternalLeadIntegrationApiClient::startMetaOauth, result -> {
            String url = result.optString("authorizationUrl", "");
            Uri uri = Uri.parse(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !(host.equals("facebook.com") || host.endsWith(".facebook.com"))) {
                toast("Meta 인증 주소 오류");
                return;
            }
            openUrl(uri);
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
            googleFlow = false;
            loadMetaOauthPages(oauthId);
        } else {
            toast(reason.isEmpty() ? "Meta 연결 실패" : "Meta 연결 실패 · " + reason);
        }
    }

    private void loadMetaOauthPages(String oauthId) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.metaOauthSession(session, oauthId), result -> {
            JSONObject oauth = result.optJSONObject("oauth");
            if (oauth == null || !"authorized".equals(oauth.optString("status", ""))) {
                toast("Meta 연결 상태를 확인해주세요.");
                return;
            }
            JSONArray pages = oauth.optJSONArray("pages");
            if (pages == null || pages.length() == 0) {
                toast("연결 가능한 Meta 페이지가 없습니다.");
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
                .setTitle("Meta 페이지 선택")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("취소", null)
                .setPositiveButton("연결", (dialog, which) -> {
                    JSONArray ids = new JSONArray();
                    for (int i = 0; i < count; i++) {
                        if (!checked[i]) continue;
                        JSONObject page = pages.optJSONObject(i);
                        if (page != null && !page.optString("id", "").isEmpty()) ids.put(page.optString("id"));
                    }
                    if (ids.length() == 0) {
                        toast("페이지를 선택해주세요.");
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
            toast(completed ? "Meta 연결 완료" : "일부 Meta 페이지 연결 실패");
            refreshRemoteStatus();
        });
    }

    private void showMetaConnections() {
        ArrayList<String> rows = new ArrayList<>();
        for (int i = 0; i < metaConnections.length(); i++) {
            JSONObject item = metaConnections.optJSONObject(i);
            if (item == null || !"active".equalsIgnoreCase(item.optString("status", "active"))) continue;
            rows.add(item.optString("pageName", item.optString("page_name", "Meta Page")));
        }
        new AlertDialog.Builder(this)
                .setTitle("Meta 연결")
                .setItems(rows.toArray(new String[0]), null)
                .setPositiveButton("확인", null)
                .show();
    }

    // ------------------------- Webhook -------------------------

    private void createGenericWebhook(Button button) {
        runApi(button, "생성 중...", session ->
                        ExternalLeadIntegrationApiClient.createWebhookConnection(
                                session, "외부 Webhook", "External Webhook"),
                result -> {
                    JSONObject connection = result.optJSONObject("connection");
                    if (connection != null) prependWebhookConnection(connection);
                    showOneTimeValue("Webhook URL", result.optString("endpointUrl", ""), "URL 복사");
                    renderChannels();
                });
    }

    private void manageWebhook(JSONObject connection) {
        if (connection == null) return;
        String id = connection.optString("id", "");
        String[] actions = {"상태 확인", "URL 재발급", "연결 해제"};
        new AlertDialog.Builder(this)
                .setTitle("Webhook")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) checkWebhook(connection);
                    if (which == 1) rotateWebhook(id);
                    if (which == 2) revokeWebhook(id);
                })
                .show();
    }

    private void checkWebhook(JSONObject connection) {
        String id = connection.optString("id", "");
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.webhookSamples(session, id), result -> {
            JSONObject current = result.optJSONObject("connection");
            if (current != null) replaceWebhookConnection(current);
            JSONArray samples = result.optJSONArray("samples");
            boolean ready = current != null && current.optBoolean("mappingReady", false);
            renderChannels();
            toast("샘플 " + (samples == null ? 0 : samples.length()) + " · " + (ready ? "연결됨" : "매핑 필요"));
        });
    }

    private void rotateWebhook(String connectionId) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.rotateWebhookConnection(session, connectionId), result -> {
            JSONObject connection = result.optJSONObject("connection");
            if (connection != null) replaceWebhookConnection(connection);
            showOneTimeValue("새 Webhook URL", result.optString("endpointUrl", ""), "URL 복사");
            renderChannels();
        });
    }

    private void revokeWebhook(String connectionId) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.revokeWebhookConnection(session, connectionId), result -> {
            JSONObject connection = result.optJSONObject("connection");
            if (connection != null) replaceWebhookConnection(connection);
            renderChannels();
            toast("Webhook 연결 해제됨");
        });
    }

    // ------------------------- Shared -------------------------

    private void refreshRemoteStatus() {
        if (!AuthSessionStore.hasSession(this) || remoteLoading || googleFlow) return;
        remoteLoading = true;
        final String session = AuthSessionStore.session(this);
        io.execute(() -> {
            JSONArray webhooks = null;
            JSONArray metas = null;
            try {
                webhooks = ExternalLeadIntegrationApiClient.listWebhookConnections(session).optJSONArray("connections");
            } catch (Exception ignored) {}
            try {
                metas = ExternalLeadIntegrationApiClient.listMetaConnections(session).optJSONArray("connections");
            } catch (Exception ignored) {}
            final JSONArray finalWebhooks = webhooks;
            final JSONArray finalMetas = metas;
            runOnUiThread(() -> {
                remoteLoading = false;
                if (isFinishing() || isDestroyed() || googleFlow) return;
                if (finalWebhooks != null) webhookConnections = finalWebhooks;
                if (finalMetas != null) metaConnections = finalMetas;
                renderChannels();
            });
        });
    }

    private void requestLeadSync() {
        if (!AuthSessionStore.hasSession(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        syncButton.setEnabled(false);
        syncButton.setText("확인 중...");
        setReceiverBadge("확인 중", false);
        boolean started = UniversalLeadSyncManager.requestSync(this, true);
        if (!started && !UniversalLeadSyncManager.isRunning()) {
            finishSyncButton();
            setReceiverBadge("확인 필요", false);
            toast("문의 확인을 시작하지 못했습니다.");
        }
    }

    private void finishSyncButton() {
        if (syncButton == null) return;
        syncButton.setEnabled(true);
        syncButton.setText("새 문의 확인");
    }

    private void refreshLocalStatus() {
        if (receiverBadge == null) return;
        boolean signedIn = AuthSessionStore.hasSession(this);
        if (!UniversalLeadSyncManager.isRunning()) setReceiverBadge(signedIn ? "정상" : "로그인 필요", signedIn);
    }

    private void runApi(Button button, String loadingLabel, ApiTask task, ApiSuccess success) {
        if (!AuthSessionStore.hasSession(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        String original = button == null ? "" : String.valueOf(button.getText());
        if (button != null) {
            button.setEnabled(false);
            if (!loadingLabel.isEmpty()) button.setText(loadingLabel);
        }
        String session = AuthSessionStore.session(this);
        io.execute(() -> {
            try {
                JSONObject result = task.run(session);
                runOnUiThread(() -> {
                    if (button != null) {
                        button.setEnabled(true);
                        button.setText(original);
                    }
                    success.accept(result == null ? new JSONObject() : result);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (button != null) {
                        button.setEnabled(true);
                        button.setText(original);
                    }
                    toast(userFacingError(error));
                });
            }
        });
    }

    private String userFacingError(Exception error) {
        if (error instanceof ExternalLeadIntegrationApiClient.ApiException) {
            ExternalLeadIntegrationApiClient.ApiException api = (ExternalLeadIntegrationApiClient.ApiException) error;
            if (api.status == 401 || api.status == 403) return "로그인을 다시 확인해주세요.";
            if (!api.code.isEmpty()) return value(api.getMessage()) + " · " + api.code;
        }
        String message = error == null ? "" : value(error.getMessage());
        return message.isEmpty() ? "연결에 실패했습니다." : message;
    }

    private int activeMetaCount() {
        int count = 0;
        for (int i = 0; i < metaConnections.length(); i++) {
            JSONObject item = metaConnections.optJSONObject(i);
            if (item != null && "active".equalsIgnoreCase(item.optString("status", "active"))) count++;
        }
        return count;
    }

    private int activeCount(JSONArray array, String sourceName, boolean equal) {
        int count = 0;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null || !"active".equalsIgnoreCase(item.optString("status", "active"))) continue;
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

    private void showOneTimeValue(String title, String value, String copyLabel) {
        transientSecret = value == null ? "" : value;
        TextView secret = bodyText(transientSecret);
        secret.setTextIsSelectable(true);
        secret.setTypeface(Typeface.MONOSPACE);
        secret.setTextSize(12f);
        secret.setPadding(dp(14), dp(12), dp(14), dp(12));
        secret.setBackgroundResource(R.drawable.bg_input);
        ScrollView scroller = new ScrollView(this);
        scroller.setPadding(dp(20), dp(8), dp(20), 0);
        scroller.addView(secret, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(scroller)
                .setNegativeButton("닫기", null)
                .setPositiveButton(copyLabel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            copyToClipboard(copyLabel, transientSecret);
            toast("복사됨");
        }));
        dialog.setOnDismissListener(ignored -> transientSecret = "");
        dialog.show();
    }

    private void copyToClipboard(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText(label, value == null ? "" : value));
    }

    private void openUrl(Uri uri) {
        if (uri == null) return;
        try {
            CustomTabsIntent tabs = new CustomTabsIntent.Builder().setShowTitle(false).build();
            tabs.launchUrl(this, uri);
            return;
        } catch (RuntimeException ignored) {}
        try {
            Intent browser = new Intent(Intent.ACTION_VIEW, uri);
            browser.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(browser);
        } catch (RuntimeException error) {
            toast("브라우저를 열 수 없습니다.");
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

    private ScrollView baseScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        return scroll;
    }

    private LinearLayout bodyContainer() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(10), dp(18), dp(32));
        return body;
    }

    private View header(String title, View.OnClickListener backListener) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = titleText("‹", 32f);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(backListener);
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));
        TextView titleView = titleText(title, 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(4);
        header.addView(titleView, titleParams);
        return header;
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

    private ScrollView.LayoutParams matchWrapScroll() {
        return new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT);
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

    private interface ApiTask {
        JSONObject run(String session) throws Exception;
    }

    private interface ApiSuccess {
        void accept(JSONObject result);
    }
}
