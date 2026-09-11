package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
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

/** Generic Webhook sample field mapping UI. */
public final class WebhookMappingActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout connectionList;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Webhook 필드 매핑");
        setContentView(buildContent());
        loadConnections();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("‹  Webhook 필드 매핑", 21f, true);
        title.setOnClickListener(v -> finish());
        body.addView(title, matchWrap());

        TextView guide = text("Webhook에 테스트 문의 1건을 먼저 전송한 뒤 전화번호·이름·이메일·문의내용 필드를 연결합니다.", 14f, false);
        LinearLayout.LayoutParams gp = matchWrap();
        gp.topMargin = dp(16);
        body.addView(guide, gp);

        statusView = text("연결 확인 중…", 13f, false);
        LinearLayout.LayoutParams sp = matchWrap();
        sp.topMargin = dp(16);
        body.addView(statusView, sp);

        connectionList = new LinearLayout(this);
        connectionList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(8);
        body.addView(connectionList, lp);
        return scroll;
    }

    private void loadConnections() {
        if (!AuthSessionStore.hasSession(this)) {
            toast("콜태그 로그인이 필요합니다.");
            finish();
            return;
        }
        final String session = AuthSessionStore.session(this);
        io.execute(() -> {
            try {
                JSONObject result = ExternalLeadIntegrationApiClient.listWebhookConnections(session);
                JSONArray all = result.optJSONArray("connections");
                ArrayList<JSONObject> active = new ArrayList<>();
                if (all != null) {
                    for (int i = 0; i < all.length(); i++) {
                        JSONObject item = all.optJSONObject(i);
                        if (item == null) continue;
                        if (!"active".equalsIgnoreCase(item.optString("status", "active"))) continue;
                        if ("Google Forms".equals(item.optString("sourceName", "").trim())) continue;
                        active.add(item);
                    }
                }
                runOnUiThread(() -> renderConnections(active));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    statusView.setText("Webhook 연결을 불러오지 못했습니다.");
                    toast(errorMessage(error));
                });
            }
        });
    }

    private void renderConnections(ArrayList<JSONObject> rows) {
        connectionList.removeAllViews();
        if (rows.isEmpty()) {
            statusView.setText("먼저 외부 문의 연동에서 Webhook을 만들어주세요.");
            return;
        }
        statusView.setText("매핑할 Webhook을 선택하세요.");
        for (JSONObject row : rows) {
            String id = row.optString("id", "");
            String name = row.optString("name", row.optString("sourceName", "Webhook"));
            boolean ready = row.optBoolean("mappingReady", false);
            int samples = row.optInt("sampleCount", 0);
            Button button = new Button(this);
            button.setAllCaps(false);
            button.setText(name + "\n샘플 " + samples + "건 · " + (ready ? "매핑 완료" : "매핑 필요"));
            button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            button.setPadding(dp(16), 0, dp(16), 0);
            button.setBackgroundResource(R.drawable.bg_card);
            button.setTextColor(getColor(R.color.text_primary));
            button.setOnClickListener(v -> loadSamples(id, name));
            connectionList.addView(button, topHeight(70, 10));
        }
    }

    private void loadSamples(String connectionId, String connectionName) {
        statusView.setText("샘플 분석 중…");
        final String session = AuthSessionStore.session(this);
        io.execute(() -> {
            try {
                JSONObject result = ExternalLeadIntegrationApiClient.webhookSamples(session, connectionId);
                JSONArray samples = result.optJSONArray("samples");
                JSONObject sample = samples == null || samples.length() == 0 ? null : samples.optJSONObject(0);
                runOnUiThread(() -> {
                    if (sample == null) {
                        statusView.setText("샘플이 없습니다. Webhook URL로 테스트 문의 1건을 먼저 전송해주세요.");
                        return;
                    }
                    beginMapping(connectionId, connectionName, sample);
                });
            } catch (Exception error) {
                runOnUiThread(() -> toast(errorMessage(error)));
            }
        });
    }

    private void beginMapping(String connectionId, String connectionName, JSONObject sample) {
        JSONObject mapper = sample.optJSONObject("mapper");
        JSONArray fields = mapper == null ? null : mapper.optJSONArray("fields");
        JSONObject draft = mapper == null ? null : mapper.optJSONObject("draftMapping");
        if (fields == null || fields.length() == 0) {
            toast("매핑 가능한 필드를 찾지 못했습니다.");
            return;
        }
        JSONObject mapping = draft == null ? new JSONObject() : copyJson(draft);
        chooseRole(connectionId, connectionName, fields, mapping, 0);
    }

    private void chooseRole(String connectionId, String connectionName, JSONArray fields, JSONObject mapping, int step) {
        final String[] roles = {"phone", "name", "email", "content"};
        final String[] titles = {"전화번호 필드 선택", "이름 필드 선택", "이메일 필드 선택", "문의내용 필드 선택"};
        if (step >= roles.length) {
            saveMapping(connectionId, connectionName, mapping);
            return;
        }

        String role = roles[step];
        boolean required = "phone".equals(role);
        int extra = required ? 0 : 1;
        String[] labels = new String[fields.length() + extra];
        if (!required) labels[0] = "건너뛰기";
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            String pointer = field == null ? "" : field.optString("pointer", "");
            String preview = field == null ? "" : field.optString("preview", "");
            labels[i + extra] = pointer + (preview.isEmpty() ? "" : "  ·  " + preview);
        }

        new AlertDialog.Builder(this)
                .setTitle(titles[step])
                .setItems(labels, (dialog, which) -> {
                    if (!required && which == 0) {
                        mapping.remove(role);
                        chooseRole(connectionId, connectionName, fields, mapping, step + 1);
                        return;
                    }
                    int fieldIndex = which - extra;
                    JSONObject field = fields.optJSONObject(fieldIndex);
                    String pointer = field == null ? "" : field.optString("pointer", "");
                    if (pointer.isEmpty()) {
                        toast("필드를 선택해주세요.");
                        return;
                    }
                    try { mapping.put(role, pointer); } catch (Exception ignored) {}
                    chooseRole(connectionId, connectionName, fields, mapping, step + 1);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveMapping(String connectionId, String connectionName, JSONObject mapping) {
        if (mapping.optString("phone", "").isEmpty()) {
            toast("전화번호 필드는 필수입니다.");
            return;
        }
        if (!mapping.has("customFields")) {
            try { mapping.put("customFields", new JSONArray()); } catch (Exception ignored) {}
        }
        statusView.setText("매핑 저장 중…");
        final String session = AuthSessionStore.session(this);
        io.execute(() -> {
            try {
                JSONObject result = ExternalLeadIntegrationApiClient.updateWebhookMapping(session, connectionId, mapping);
                JSONObject connection = result.optJSONObject("connection");
                boolean ready = connection != null && connection.optBoolean("mappingReady", false);
                runOnUiThread(() -> {
                    statusView.setText(ready ? connectionName + " · 매핑 완료" : connectionName + " · 매핑 확인 필요");
                    toast(ready ? "Webhook 매핑이 저장됐습니다. 이제 새 문의가 자동 등록됩니다." : "매핑을 다시 확인해주세요.");
                    loadConnections();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    statusView.setText("매핑 저장 실패");
                    toast(errorMessage(error));
                });
            }
        });
    }

    private JSONObject copyJson(JSONObject source) {
        try { return new JSONObject(source == null ? "{}" : source.toString()); }
        catch (Exception ignored) { return new JSONObject(); }
    }

    private String errorMessage(Exception error) {
        String message = error == null ? "" : error.getMessage();
        return message == null || message.trim().isEmpty() ? "Webhook 매핑 요청에 실패했습니다." : message.trim();
    }

    private TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.text_primary));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams topHeight(int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(height));
        params.topMargin = dp(top);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
