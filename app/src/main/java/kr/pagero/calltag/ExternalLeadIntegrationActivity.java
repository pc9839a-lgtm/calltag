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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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
    private JSONArray googleFormsConnections = new JSONArray();
    private String transientSecret = "";

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
        setContentView(buildContent());
        ExternalLeadAutoSyncScheduler.reconcile(this);
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

        addChannelCard("PageRo", "연결 관리", true, "관리",
                v -> startActivity(new Intent(this, PageroConnectionCompactActivity.class)), null, null);

        int metaCount = activeMetaCount();
        addChannelCard("Meta Lead Ads", metaCount > 0 ? metaCount + "개 연결" : "미연결", metaCount > 0,
                metaCount > 0 ? "추가 연결" : "연결", v -> startMetaOauth((Button) v),
                metaCount > 0 ? "연결 목록" : null, metaCount > 0 ? v -> showMetaConnections() : null);

        int googleCount = activeGoogleFormsCount();
        addChannelCard("Google Forms", googleCount > 0 ? googleCount + "개 연결" : "미연결", googleCount > 0,
                googleCount > 0 ? "추가 연결" : "연결", v -> startGoogleFormsOauth((Button) v),
                googleCount > 0 ? "관리" : null, googleCount > 0 ? v -> showGoogleFormsConnections() : null);

        int webhookCount = activeWebhookCount();
        JSONObject webhook = latestGenericWebhook();
        addChannelCard("Webhook", webhookCount > 0 ? webhookCount + "개 연결" : "미연결", webhookCount > 0,
                "Webhook 만들기", v -> createGenericWebhook((Button) v),
                webhookCount > 0 ? "관리" : null, webhookCount > 0 ? v -> manageWebhook(webhook) : null);
    }

    private void addChannelCard(String name, String state, boolean positive, String primaryLabel,
                                View.OnClickListener primaryListener, String secondaryLabel,
                                View.OnClickListener secondaryListener) {
        LinearLayout item = card();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(titleText(name, 16f), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
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

    private void refreshRemoteStatus() {
        if (!AuthSessionStore.hasSession(this) || remoteLoading) return;
        remoteLoading = true;
        final String session = AuthSessionStore.session(this);
        io.execute(() -> {
            JSONArray webhooks = null;
            JSONArray metas = null;
            JSONArray google = null;
            try { webhooks = ExternalLeadIntegrationApiClient.listWebhookConnections(session).optJSONArray("connections"); }
            catch (Exception ignored) {}
            try { metas = ExternalLeadIntegrationApiClient.listMetaConnections(session).optJSONArray("connections"); }
            catch (Exception ignored) {}
            try { google = ExternalLeadIntegrationApiClient.listGoogleFormsConnections(session).optJSONArray("connections"); }
            catch (Exception ignored) {}
            final JSONArray finalWebhooks = webhooks;
            final JSONArray finalMetas = metas;
            final JSONArray finalGoogle = google;
            runOnUiThread(() -> {
                remoteLoading = false;
                if (isFinishing() || isDestroyed()) return;
                if (finalWebhooks != null) webhookConnections = finalWebhooks;
                if (finalMetas != null) metaConnections = finalMetas;
                if (finalGoogle != null) googleFormsConnections = finalGoogle;
                renderChannels();
            });
        });
    }

    private void startGoogleFormsOauth(Button button) {
        runApi(button, "Google 여는 중...", ExternalLeadIntegrationApiClient::startGoogleFormsOauth, result -> {
            Uri uri = Uri.parse(result.optString("authorizationUrl", ""));
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"accounts.google.com".equals(host)) {
                toast("Google 인증 주소 오류");
                return;
            }
            openUrl(uri);
        });
    }

    private void handleGoogleFormsDeepLink(Uri uri) {
        String state = value(uri.getQueryParameter("googleForms"));
        String oauthId = value(uri.getQueryParameter("googleFormsOAuth"));
        String reason = value(uri.getQueryParameter("reason"));
        if ("ready".equals(state) && !oauthId.isEmpty()) loadGoogleForms(oauthId);
        else toast(reason.isEmpty() ? "Google Forms 연결 실패" : "Google Forms 연결 실패 · " + reason);
    }

    private void loadGoogleForms(String oauthId) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.googleFormsOauthSession(session, oauthId), result -> {
            JSONObject oauth = result.optJSONObject("oauth");
            if (oauth == null || !"authorized".equals(oauth.optString("status", ""))) {
                toast("Google Forms 권한을 확인해주세요.");
                return;
            }
            runApi(null, "", session -> ExternalLeadIntegrationApiClient.listGoogleForms(session, oauthId), formsResult -> {
                JSONArray forms = formsResult.optJSONArray("forms");
                if (forms == null || forms.length() == 0) {
                    toast("연결 가능한 Google Form이 없습니다.");
                    return;
                }
                showGoogleFormPicker(oauthId, forms);
            });
        });
    }

    private void showGoogleFormPicker(String oauthId, JSONArray forms) {
        String[] labels = new String[forms.length()];
        for (int i = 0; i < forms.length(); i++) {
            JSONObject form = forms.optJSONObject(i);
            labels[i] = form == null ? "Google Form" : form.optString("name", "Google Form");
        }
        new AlertDialog.Builder(this)
                .setTitle("연결할 Google Form 선택")
                .setItems(labels, (dialog, which) -> {
                    JSONObject form = forms.optJSONObject(which);
                    if (form != null && !form.optString("id", "").isEmpty()) {
                        connectGoogleForm(oauthId, form.optString("id"));
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void connectGoogleForm(String oauthId, String formId) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.connectGoogleForm(session, oauthId, formId), result -> {
            JSONObject connection = result.optJSONObject("connection");
            if (connection == null || !"active".equalsIgnoreCase(connection.optString("status", ""))) {
                toast("Google Forms 연결을 완료하지 못했습니다.");
                return;
            }
            ExternalLeadAutoSyncScheduler.reconcile(this);
            toast("Google Forms 연결 완료 · 자동 수신 켜짐");
            refreshRemoteStatus();
            syncGoogleFormsThenPull();
        });
    }

    private void showGoogleFormsConnections() {
        ArrayList<JSONObject> active = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < googleFormsConnections.length(); i++) {
            JSONObject item = googleFormsConnections.optJSONObject(i);
            if (item == null || !"active".equalsIgnoreCase(item.optString("status", "active"))) continue;
            active.add(item);
            String title = item.optString("formTitle", "Google Form");
            String email = item.optString("googleEmail", "");
            labels.add(email.isEmpty() ? title : title + " · " + email);
        }
        if (active.isEmpty()) {
            toast("활성 Google Forms 연결이 없습니다.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Google Forms")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> confirmRevokeGoogleForm(active.get(which)))
                .setNegativeButton("닫기", null)
                .show();
    }

    private void confirmRevokeGoogleForm(JSONObject connection) {
        String id = connection.optString("id", "");
        String title = connection.optString("formTitle", "Google Form");
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("이 폼의 새 응답 수신을 중단할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("연결 해제", (dialog, which) ->
                        runApi(null, "", session -> ExternalLeadIntegrationApiClient.revokeGoogleFormsConnection(session, id), result -> {
                            toast("Google Forms 연결 해제됨");
                            refreshRemoteStatus();
                        }))
                .show();
    }

    private void syncGoogleFormsThenPull() {
        if (!AuthSessionStore.hasSession(this)) return;
        String session = AuthSessionStore.session(this);
        io.execute(() -> {
            try { ExternalLeadIntegrationApiClient.syncGoogleForms(session); }
            catch (Exception ignored) {}
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) UniversalLeadSyncManager.requestSync(this, true);
            });
        });
    }

    private void createGenericWebhook(Button button) {
        runApi(button, "생성 중...", session -> ExternalLeadIntegrationApiClient.createWebhookConnection(
                session, "외부 Webhook", "External Webhook"), result -> {
            JSONObject connection = result.optJSONObject("connection");
            if (connection != null) prependWebhookConnection(connection);
            showOneTimeValue("Webhook URL", result.optString("endpointUrl", ""), "URL 복사");
            renderChannels();
        });
    }

    private void manageWebhook(JSONObject connection) {
        if (connection == null) return;
        String id = connection.optString("id", "");
        String[] actions = {"상태 확인", "필드 매핑", "URL 재발급", "연결 해제"};
        new AlertDialog.Builder(this).setTitle("Webhook").setItems(actions, (dialog, which) -> {
            if (which == 0) checkWebhook(connection);
            if (which == 1) openWebhookMapping(id);
            if (which == 2) rotateWebhook(id);
            if (which == 3) revokeWebhook(id);
        }).setNegativeButton("닫기", null).show();
    }

    private void checkWebhook(JSONObject connection) {
        String id = connection.optString("id", "");
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.webhookSamples(session, id), result -> {
            JSONObject current = result.optJSONObject("connection");
            if (current != null) replaceWebhookConnection(current);
            JSONArray samples = result.optJSONArray("samples");
            int count = samples == null ? 0 : samples.length();
            boolean ready = current != null && current.optBoolean("mappingReady", false);
            renderChannels();
            if (count == 0) {
                toast("먼저 Webhook URL로 테스트 문의 1건을 보내주세요.");
                return;
            }
            if (!ready) {
                showWebhookMappingDialog(id, current, samples.optJSONObject(0));
                return;
            }
            toast("샘플 " + count + " · 연결됨");
        });
    }

    private void openWebhookMapping(String connectionId) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.webhookSamples(session, connectionId), result -> {
            JSONObject current = result.optJSONObject("connection");
            if (current != null) replaceWebhookConnection(current);
            JSONArray samples = result.optJSONArray("samples");
            if (samples == null || samples.length() == 0) {
                toast("매핑할 샘플이 없습니다. 테스트 문의를 먼저 보내주세요.");
                return;
            }
            showWebhookMappingDialog(connectionId, current, samples.optJSONObject(0));
        });
    }

    private void showWebhookMappingDialog(String connectionId, JSONObject connection, JSONObject sample) {
        if (sample == null) {
            toast("Webhook 샘플을 읽지 못했습니다.");
            return;
        }
        JSONObject mapper = sample.optJSONObject("mapper");
        JSONArray fields = mapper == null ? null : mapper.optJSONArray("fields");
        if (fields == null || fields.length() == 0) {
            toast("샘플에서 매핑 가능한 필드를 찾지 못했습니다.");
            return;
        }

        JSONObject currentMapping = connection == null ? null : connection.optJSONObject("mapping");
        JSONObject draft = mapper.optJSONObject("draftMapping");
        JSONObject initial = connection != null && connection.optBoolean("mappingReady", false)
                ? currentMapping : draft;
        if (initial == null) initial = new JSONObject();

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(8), dp(20), 0);

        TextView guide = bodyText("샘플 값을 기준으로 콜태그 고객 필드를 연결합니다. 전화번호는 필수입니다.");
        panel.addView(guide, matchWrap());

        Spinner name = mappingSpinner(panel, "이름", fields, initial.optString("name", ""), true);
        Spinner phone = mappingSpinner(panel, "전화번호 *", fields, initial.optString("phone", ""), false);
        Spinner email = mappingSpinner(panel, "이메일", fields, initial.optString("email", ""), true);
        Spinner content = mappingSpinner(panel, "문의내용", fields, initial.optString("content", ""), true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(panel, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        JSONObject finalInitial = initial;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Webhook 필드 매핑")
                .setView(scroll)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String phonePath = selectedMappingPath(phone, fields, false);
                    if (phonePath.isEmpty()) {
                        toast("전화번호 필드를 선택해주세요.");
                        return;
                    }
                    JSONObject mapping = new JSONObject();
                    try {
                        putMapping(mapping, "name", selectedMappingPath(name, fields, true));
                        putMapping(mapping, "phone", phonePath);
                        putMapping(mapping, "email", selectedMappingPath(email, fields, true));
                        putMapping(mapping, "content", selectedMappingPath(content, fields, true));
                        putMapping(mapping, "externalId", finalInitial.optString("externalId", ""));
                        putMapping(mapping, "submittedAt", finalInitial.optString("submittedAt", ""));
                        mapping.put("customFields", new JSONArray());
                    } catch (Exception error) {
                        toast("필드 매핑을 만들지 못했습니다.");
                        return;
                    }
                    dialog.dismiss();
                    saveWebhookMapping(connectionId, mapping, sample.optLong("id", 0L));
                }));
        dialog.show();
    }

    private Spinner mappingSpinner(
            LinearLayout parent,
            String label,
            JSONArray fields,
            String initialPath,
            boolean allowNone) {
        TextView labelView = titleText(label, 13f);
        parent.addView(labelView, topMargin(16));

        ArrayList<String> labels = new ArrayList<>();
        if (allowNone) labels.add("선택 안 함");
        int initialIndex = allowNone ? 0 : -1;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            String pointer = field == null ? "" : field.optString("pointer", "");
            String preview = field == null ? "" : field.optString("preview", "");
            String row = pointer + (preview.isEmpty() ? "" : "  ·  " + preview);
            labels.add(row);
            if (!pointer.isEmpty() && pointer.equals(initialPath)) initialIndex = labels.size() - 1;
        }
        if (initialIndex < 0) initialIndex = 0;

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(initialIndex);
        parent.addView(spinner, topMargin(6));
        return spinner;
    }

    private String selectedMappingPath(Spinner spinner, JSONArray fields, boolean allowNone) {
        int selected = spinner.getSelectedItemPosition();
        if (allowNone) {
            if (selected <= 0) return "";
            selected -= 1;
        }
        JSONObject field = fields.optJSONObject(selected);
        return field == null ? "" : field.optString("pointer", "");
    }

    private void putMapping(JSONObject mapping, String key, String value) throws Exception {
        String clean = value(value);
        if (!clean.isEmpty()) mapping.put(key, clean);
    }

    private void saveWebhookMapping(String connectionId, JSONObject mapping, long rawEventId) {
        runApi(null, "", session -> {
            JSONObject updated = ExternalLeadIntegrationApiClient.updateWebhookMapping(session, connectionId, mapping);
            if (rawEventId > 0L) {
                ExternalLeadIntegrationApiClient.replayWebhookSample(session, connectionId, rawEventId);
            }
            return updated;
        }, result -> {
            JSONObject connection = result.optJSONObject("connection");
            if (connection != null) replaceWebhookConnection(connection);
            renderChannels();
            toast("Webhook 필드 매핑 완료");
            UniversalLeadSyncManager.requestSync(this, true);
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

    private void startMetaOauth(Button button) {
        runApi(button, "Meta 여는 중...", ExternalLeadIntegrationApiClient::startMetaOauth, result -> {
            Uri uri = Uri.parse(result.optString("authorizationUrl", ""));
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !(host.equals("facebook.com") || host.endsWith(".facebook.com"))) {
                toast("Meta 인증 주소 오류");
                return;
            }
            openUrl(uri);
        });
    }

    private void handleMetaDeepLink(Uri uri) {
        String state = value(uri.getQueryParameter("meta"));
        String oauthId = value(uri.getQueryParameter("metaOAuth"));
        String reason = value(uri.getQueryParameter("reason"));
        if ("ready".equals(state) && !oauthId.isEmpty()) loadMetaLeadForms(oauthId);
        else toast(reason.isEmpty() ? "Meta 연결 실패" : "Meta 연결 실패 · " + reason);
    }

    private void loadMetaLeadForms(String oauthId) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.metaOauthSession(session, oauthId), result -> {
            JSONObject oauth = result.optJSONObject("oauth");
            if (oauth == null || !"authorized".equals(oauth.optString("status", ""))) {
                toast("Meta 연결 상태를 확인해주세요.");
                return;
            }
            JSONArray forms = oauth.optJSONArray("forms");
            if (forms == null) forms = oauth.optJSONArray("pages");
            if (forms == null || forms.length() == 0) {
                toast("연결 가능한 Meta 리드폼이 없습니다.");
                return;
            }
            showMetaLeadFormPicker(oauthId, forms);
        });
    }

    private void showMetaLeadFormPicker(String oauthId, JSONArray forms) {
        int count = forms.length();
        String[] labels = new String[count];
        boolean[] checked = new boolean[count];
        for (int i = 0; i < count; i++) {
            JSONObject form = forms.optJSONObject(i);
            labels[i] = form == null ? "Meta 리드폼" : form.optString("name", form.optString("formName", "Meta 리드폼"));
            checked[i] = true;
        }
        new AlertDialog.Builder(this)
                .setTitle("받을 Meta 리드폼 선택")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("취소", null)
                .setPositiveButton("연결", (dialog, which) -> {
                    JSONArray ids = new JSONArray();
                    for (int i = 0; i < count; i++) {
                        if (!checked[i]) continue;
                        JSONObject form = forms.optJSONObject(i);
                        if (form == null) continue;
                        String formId = form.optString("formId", form.optString("id", ""));
                        if (!formId.isEmpty()) ids.put(formId);
                    }
                    if (ids.length() == 0) toast("리드폼을 선택해주세요.");
                    else completeMetaOauth(oauthId, ids);
                }).show();
    }

    private void completeMetaOauth(String oauthId, JSONArray formIds) {
        runApi(null, "", session -> ExternalLeadIntegrationApiClient.completeMetaOauth(session, oauthId, formIds), result -> {
            toast(result.optBoolean("completed", false) ? "Meta 연결 완료" : "일부 Meta 리드폼 연결 실패");
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
        new AlertDialog.Builder(this).setTitle("Meta 연결")
                .setItems(rows.toArray(new String[0]), null).setPositiveButton("확인", null).show();
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri uri = intent.getData();
        if (!"calltag".equalsIgnoreCase(uri.getScheme()) || !"external-lead".equalsIgnoreCase(uri.getHost())) return;
        String path = value(uri.getPath());
        intent.setData(null);
        if ("/meta".equals(path)) handleMetaDeepLink(uri);
        if ("/google-forms".equals(path)) handleGoogleFormsDeepLink(uri);
    }

    private void openUrl(Uri uri) {
        if (uri == null) return;
        try {
            new CustomTabsIntent.Builder().setShowTitle(false).build().launchUrl(this, uri);
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

    private void requestLeadSync() {
        if (!AuthSessionStore.hasSession(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        syncButton.setEnabled(false);
        syncButton.setText("확인 중...");
        setReceiverBadge("확인 중", false);
        String session = AuthSessionStore.session(this);
        io.execute(() -> {
            try { ExternalLeadIntegrationApiClient.syncGoogleForms(session); }
            catch (Exception ignored) {}
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                boolean started = UniversalLeadSyncManager.requestSync(this, true);
                if (!started && !UniversalLeadSyncManager.isRunning()) {
                    finishSyncButton();
                    setReceiverBadge("확인 필요", false);
                    toast("문의 확인을 시작하지 못했습니다.");
                }
            });
        });
    }

    private void finishSyncButton() {
        if (syncButton != null) {
            syncButton.setEnabled(true);
            syncButton.setText("새 문의 확인");
        }
    }

    private void refreshLocalStatus() {
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
                    if (isFinishing() || isDestroyed()) return;
                    if (button != null) { button.setEnabled(true); button.setText(original); }
                    success.accept(result == null ? new JSONObject() : result);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (button != null) { button.setEnabled(true); button.setText(original); }
                    toast(userFacingError(error));
                });
            }
        });
    }

    private String userFacingError(Exception error) {
        if (error instanceof ExternalLeadIntegrationApiClient.ApiException) {
            ExternalLeadIntegrationApiClient.ApiException api = (ExternalLeadIntegrationApiClient.ApiException) error;
            if (api.status == 401 || api.status == 403) return "로그인 또는 연동 권한을 다시 확인해주세요.";
            if ("CALLTAG_GOOGLE_FORMS_PHONE_FIELD_NOT_FOUND".equals(api.code)) return "폼에서 전화번호 질문을 찾지 못했습니다.";
            if ("CALLTAG_META_FORM_SELECTION_REQUIRED".equals(api.code)) return "받을 Meta 리드폼을 선택해주세요.";
            if ("CALLTAG_WEBHOOK_MAPPING_PHONE_REQUIRED".equals(api.code)) return "Webhook 전화번호 필드를 선택해주세요.";
            if (!api.code.isEmpty()) return value(api.getMessage());
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

    private int activeGoogleFormsCount() {
        int count = 0;
        for (int i = 0; i < googleFormsConnections.length(); i++) {
            JSONObject item = googleFormsConnections.optJSONObject(i);
            if (item != null && "active".equalsIgnoreCase(item.optString("status", "active"))) count++;
        }
        return count;
    }

    private int activeWebhookCount() {
        int count = 0;
        for (int i = 0; i < webhookConnections.length(); i++) {
            JSONObject item = webhookConnections.optJSONObject(i);
            if (item == null || !"active".equalsIgnoreCase(item.optString("status", "active"))) continue;
            if (!GOOGLE_FORMS_SOURCE.equals(item.optString("sourceName", "").trim())) count++;
        }
        return count;
    }

    private JSONObject latestGenericWebhook() {
        for (int i = 0; i < webhookConnections.length(); i++) {
            JSONObject item = webhookConnections.optJSONObject(i);
            if (item == null || !"active".equalsIgnoreCase(item.optString("status", "active"))) continue;
            if (!GOOGLE_FORMS_SOURCE.equals(item.optString("sourceName", "").trim())) return item;
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
            if (item != null && id.equals(item.optString("id", ""))) { next.put(replacement); replaced = true; }
            else next.put(webhookConnections.opt(i));
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
        scroller.addView(secret, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(scroller)
                .setNegativeButton("닫기", null).setPositiveButton(copyLabel, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText(copyLabel, transientSecret));
            toast("복사됨");
        }));
        dialog.setOnDismissListener(ignored -> transientSecret = "");
        dialog.show();
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

    private LinearLayout header(String title, View.OnClickListener backListener) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = titleText("‹", 32f);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(backListener);
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));
        TextView titleView = titleText(title, 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
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
        text.setBackground(pillBackground(getColor(positive ? R.color.success_soft : R.color.surface_soft),
                getColor(positive ? R.color.success : R.color.border)));
        return text;
    }

    private void setReceiverBadge(String label, boolean positive) {
        if (receiverBadge == null) return;
        receiverBadge.setText(label);
        receiverBadge.setTextColor(getColor(positive ? R.color.success : R.color.text_secondary));
        receiverBadge.setBackground(pillBackground(getColor(positive ? R.color.success_soft : R.color.surface_soft),
                getColor(positive ? R.color.success : R.color.border)));
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

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private ScrollView.LayoutParams matchWrapScroll() {
        return new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
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

    private interface ApiTask { JSONObject run(String session) throws Exception; }
    private interface ApiSuccess { void accept(JSONObject result); }
}
