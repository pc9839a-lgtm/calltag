package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native management UI for CallTag Direct API keys. Raw keys are never persisted locally. */
public final class DirectApiIntegrationActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout keyList;
    private Button createButton;
    private String transientSecret = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Direct API");
        setContentView(buildContent());
        refreshKeys();
    }

    @Override
    protected void onDestroy() {
        transientSecret = "";
        io.shutdownNow();
        super.onDestroy();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(14), dp(18), dp(32));
        scroll.addView(body, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView back = title("‹  Direct API", 21f);
        back.setOnClickListener(v -> finish());
        back.setClickable(true);
        body.addView(back, matchWrap());

        TextView guide = body("외부 DB·서버에서 콜태그로 문의를 직접 전송할 때 사용하는 API Key입니다. " +
                "원문 Key는 발급 또는 교체 직후 한 번만 표시됩니다.");
        LinearLayout.LayoutParams guideParams = matchWrap();
        guideParams.topMargin = dp(18);
        body.addView(guide, guideParams);

        createButton = button("새 API Key 발급", true);
        createButton.setOnClickListener(v -> createKey());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        buttonParams.topMargin = dp(18);
        body.addView(createButton, buttonParams);

        TextView endpoint = body("Lead 입력: POST https://pagero.kr/api/calltag/v1/leads\n" +
                "Authorization: Bearer <ctk_...>\nIdempotency-Key: <unique-key>");
        endpoint.setTypeface(Typeface.MONOSPACE);
        endpoint.setTextSize(12f);
        endpoint.setPadding(dp(12), dp(12), dp(12), dp(12));
        endpoint.setBackgroundResource(R.drawable.bg_input);
        LinearLayout.LayoutParams endpointParams = matchWrap();
        endpointParams.topMargin = dp(14);
        body.addView(endpoint, endpointParams);

        TextView section = title("발급된 키", 15f);
        section.setTextColor(getColor(R.color.text_secondary));
        LinearLayout.LayoutParams sectionParams = matchWrap();
        sectionParams.topMargin = dp(26);
        body.addView(section, sectionParams);

        keyList = new LinearLayout(this);
        keyList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = matchWrap();
        listParams.topMargin = dp(8);
        body.addView(keyList, listParams);
        return scroll;
    }

    private void refreshKeys() {
        if (!AuthSessionStore.hasSession(this)) {
            toast("콜태그 로그인이 필요합니다.");
            finish();
            return;
        }
        setCreateBusy(true);
        String session = AuthSessionStore.session(this);
        io.execute(() -> {
            try {
                JSONObject response = ExternalLeadIntegrationApiClient.listApiKeys(session);
                JSONArray keys = response.optJSONArray("keys");
                runOnUiThread(() -> {
                    setCreateBusy(false);
                    renderKeys(keys == null ? new JSONArray() : keys);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setCreateBusy(false);
                    toast(errorMessage(error));
                });
            }
        });
    }

    private void renderKeys(JSONArray keys) {
        keyList.removeAllViews();
        ArrayList<JSONObject> active = new ArrayList<>();
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.optJSONObject(i);
            if (key != null && "active".equalsIgnoreCase(key.optString("status", ""))) active.add(key);
        }
        if (active.isEmpty()) {
            TextView empty = body("활성 API Key가 없습니다.");
            empty.setPadding(dp(4), dp(12), dp(4), dp(12));
            keyList.addView(empty, matchWrap());
            return;
        }
        for (JSONObject key : active) keyList.addView(keyCard(key), topMargin(10));
    }

    private LinearLayout keyCard(JSONObject key) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);

        String name = key.optString("name", "External Lead API");
        String prefix = key.optString("keyPrefix", "");
        TextView heading = title(name, 16f);
        card.addView(heading, matchWrap());
        TextView meta = body(prefix.isEmpty() ? "활성" : prefix + "… · 활성");
        card.addView(meta, topMargin(6));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button rotate = button("Key 교체", false);
        rotate.setOnClickListener(v -> confirmRotate(key));
        actions.addView(rotate, new LinearLayout.LayoutParams(0, dp(44), 1f));
        Button revoke = button("해제", false);
        revoke.setOnClickListener(v -> confirmRevoke(key));
        LinearLayout.LayoutParams revokeParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
        revokeParams.leftMargin = dp(8);
        actions.addView(revoke, revokeParams);
        card.addView(actions, topMargin(12));
        return card;
    }

    private void createKey() {
        runKeyAction(() -> ExternalLeadIntegrationApiClient.createApiKey(
                AuthSessionStore.session(this), "CallTag External DB"), "API Key", true);
    }

    private void confirmRotate(JSONObject key) {
        String id = key.optString("id", "");
        new AlertDialog.Builder(this)
                .setTitle("API Key 교체")
                .setMessage("기존 Key는 즉시 폐기됩니다. 외부 DB 연동 설정도 새 Key로 바로 바꿔야 합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("교체", (dialog, which) -> runKeyAction(
                        () -> ExternalLeadIntegrationApiClient.rotateApiKey(
                                AuthSessionStore.session(this), id), "새 API Key", true))
                .show();
    }

    private void confirmRevoke(JSONObject key) {
        String id = key.optString("id", "");
        new AlertDialog.Builder(this)
                .setTitle("API Key 해제")
                .setMessage("이 Key를 사용하는 외부 DB 문의 수신이 즉시 중단됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("해제", (dialog, which) -> runKeyAction(
                        () -> ExternalLeadIntegrationApiClient.revokeApiKey(
                                AuthSessionStore.session(this), id), "", false))
                .show();
    }

    private void runKeyAction(ApiTask task, String secretTitle, boolean showSecret) {
        setCreateBusy(true);
        io.execute(() -> {
            try {
                JSONObject response = task.run();
                runOnUiThread(() -> {
                    setCreateBusy(false);
                    if (showSecret) {
                        JSONObject key = response.optJSONObject("key");
                        String raw = key == null ? "" : key.optString("apiKey", "");
                        showOneTimeSecret(secretTitle, raw);
                    } else {
                        toast("API Key 해제됨");
                    }
                    refreshKeys();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setCreateBusy(false);
                    toast(errorMessage(error));
                });
            }
        });
    }

    private void showOneTimeSecret(String title, String raw) {
        transientSecret = raw == null ? "" : raw;
        if (transientSecret.isEmpty()) {
            toast("API Key를 표시하지 못했습니다.");
            return;
        }
        TextView value = body(transientSecret);
        value.setTextIsSelectable(true);
        value.setTypeface(Typeface.MONOSPACE);
        value.setTextSize(12f);
        value.setPadding(dp(12), dp(12), dp(12), dp(12));
        value.setBackgroundResource(R.drawable.bg_input);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("이 값은 다시 확인할 수 없습니다. 지금 안전한 곳에 저장하세요.")
                .setView(value)
                .setNegativeButton("닫기", null)
                .setPositiveButton("복사", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) clipboard.setPrimaryClip(
                            ClipData.newPlainText("CallTag API Key", transientSecret));
                    toast("복사됨");
                }));
        dialog.setOnDismissListener(ignored -> transientSecret = "");
        dialog.show();
    }

    private void setCreateBusy(boolean busy) {
        if (createButton == null) return;
        createButton.setEnabled(!busy);
        createButton.setAlpha(busy ? 0.6f : 1f);
        createButton.setText(busy ? "확인 중…" : "새 API Key 발급");
    }

    private String errorMessage(Exception error) {
        String message = error == null ? "" : error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Direct API 요청을 처리하지 못했습니다." : message.trim();
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(14f);
        return text;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private interface ApiTask { JSONObject run() throws Exception; }
}
