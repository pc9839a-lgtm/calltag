package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CallTagSyncDevicesActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private LinearLayout deviceList;
    private TextView status;
    private ProgressBar progress;
    private Button refreshButton;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("연결된 기기");

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        root.setBackgroundColor(getColor(R.color.surface));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        root.addView(text("내 계정에 연결된 기기", 24f, true, R.color.text_primary));
        root.addView(text(
                "분실했거나 더 이상 사용하지 않는 기기의 데이터 보호 권한을 해제할 수 있습니다. 현재 기기는 이 화면에서 해제되지 않습니다.",
                14f, false, R.color.text_secondary), marginTop(10));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        statusRow.addView(progress, new LinearLayout.LayoutParams(dp(24), dp(24)));
        status = text("기기 목록을 확인하고 있습니다.", 14f, false, R.color.text_secondary);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        statusParams.leftMargin = dp(10);
        statusRow.addView(status, statusParams);
        root.addView(statusRow, marginTop(22));

        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        root.addView(deviceList, marginTop(12));

        refreshButton = new Button(this);
        refreshButton.setAllCaps(false);
        refreshButton.setText("새로고침");
        refreshButton.setOnClickListener(v -> loadDevices());
        root.addView(refreshButton, marginTopHeight(18, 50));

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDevices();
    }

    private void loadDevices() {
        if (loading) return;
        if (!AuthSessionStore.hasSession(this)) {
            showEmpty("로그인 후 연결된 기기를 확인할 수 있습니다.");
            return;
        }
        loading = true;
        progress.setVisibility(View.VISIBLE);
        status.setText("기기 목록을 확인하고 있습니다.");
        refreshButton.setEnabled(false);
        executor.execute(() -> {
            try {
                JSONObject response = CallTagSyncApiClient.devices(
                        AuthSessionStore.session(this),
                        CallTagSyncDeviceStore.deviceId(this));
                JSONArray devices = response.optJSONArray("devices");
                main.post(() -> renderDevices(devices == null ? new JSONArray() : devices));
            } catch (Exception error) {
                main.post(() -> showEmpty(userMessage(error)));
            } finally {
                main.post(() -> {
                    loading = false;
                    progress.setVisibility(View.GONE);
                    refreshButton.setEnabled(true);
                });
            }
        });
    }

    private void renderDevices(JSONArray devices) {
        deviceList.removeAllViews();
        status.setText(devices.length() == 0
                ? "연결된 기기가 없습니다."
                : "총 " + devices.length() + "개 기기가 확인됐습니다.");
        for (int index = 0; index < devices.length(); index++) {
            JSONObject item = devices.optJSONObject(index);
            if (item == null) continue;
            deviceList.addView(deviceCard(item), index == 0 ? null : marginTop(10));
        }
    }

    private View deviceCard(JSONObject item) {
        boolean current = item.optBoolean("current", false);
        boolean active = item.optBoolean("active", false);
        String key = item.optString("deviceKey", "");
        String label = item.optString("label", "Android 기기").trim();
        String version = item.optString("appVersion", "").trim();
        String lastSeen = readableTime(item.optString("lastSeenAt", ""));

        LinearLayout card = card();
        TextView name = text(label.isEmpty() ? "Android 기기" : label,
                16f, true, R.color.text_primary);
        card.addView(name);
        String badge = current ? "현재 기기" : active ? "연결됨" : "해제됨";
        TextView state = text(badge, 13f, true,
                active ? R.color.text_primary : R.color.text_muted);
        card.addView(state, marginTop(5));
        String detail = "마지막 접속  " + (lastSeen.isEmpty() ? "확인되지 않음" : lastSeen);
        if (!version.isEmpty()) detail += "\n앱 버전  " + version;
        card.addView(text(detail, 13f, false, R.color.text_secondary), marginTop(9));

        if (!current && active && key.matches("^[a-f0-9]{64}$")) {
            Button revoke = new Button(this);
            revoke.setAllCaps(false);
            revoke.setText("이 기기 연결 해제");
            revoke.setOnClickListener(v -> confirmRevoke(key, label));
            card.addView(revoke, marginTopHeight(12, 46));
        }
        return card;
    }

    private void confirmRevoke(String deviceKey, String label) {
        new AlertDialog.Builder(this)
                .setTitle("기기 연결을 해제할까요?")
                .setMessage((label == null || label.trim().isEmpty() ? "선택한 기기" : label)
                        + "에서는 이후 고객정보를 동기화하거나 복구할 수 없습니다. 이 휴대폰의 데이터는 삭제되지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("연결 해제", (dialog, which) -> revoke(deviceKey))
                .show();
    }

    private void revoke(String deviceKey) {
        if (loading) return;
        loading = true;
        progress.setVisibility(View.VISIBLE);
        status.setText("기기 연결을 해제하고 있습니다.");
        refreshButton.setEnabled(false);
        executor.execute(() -> {
            try {
                CallTagSyncApiClient.revokeDevice(
                        AuthSessionStore.session(this),
                        CallTagSyncDeviceStore.deviceId(this),
                        deviceKey);
                main.post(() -> {
                    new AlertDialog.Builder(this)
                            .setMessage("선택한 기기의 데이터 보호 연결을 해제했습니다.")
                            .setPositiveButton("확인", null)
                            .show();
                    loading = false;
                    loadDevices();
                });
            } catch (Exception error) {
                main.post(() -> {
                    loading = false;
                    progress.setVisibility(View.GONE);
                    refreshButton.setEnabled(true);
                    status.setText(userMessage(error));
                });
            }
        });
    }

    private void showEmpty(String message) {
        deviceList.removeAllViews();
        status.setText(message);
        progress.setVisibility(View.GONE);
        refreshButton.setEnabled(true);
        loading = false;
    }

    private String userMessage(Exception error) {
        if (error instanceof CallTagSyncApiClient.ApiException) {
            CallTagSyncApiClient.ApiException api = (CallTagSyncApiClient.ApiException) error;
            if ("CALLTAG_SYNC_NOT_ENABLED".equals(api.code)
                    || api.status == 404 || api.status == 405 || api.status >= 500) {
                return "서버 기능을 준비 중입니다. 기기 안의 데이터는 그대로 유지됩니다.";
            }
            if (api.status == 401 || api.status == 403) {
                return "로그인을 다시 확인해주세요.";
            }
        }
        String message = error == null ? "" : String.valueOf(error.getMessage()).trim();
        return message.isEmpty() ? "기기 목록을 확인하지 못했습니다." : message;
    }

    private String readableTime(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String result = value.trim().replace('T', ' ');
        int dot = result.indexOf('.');
        if (dot > 0) result = result.substring(0, dot);
        if (result.endsWith("Z")) result = result.substring(0, result.length() - 1);
        return result.length() > 16 ? result.substring(0, 16) : result;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(color));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams marginTop(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        return params;
    }

    private LinearLayout.LayoutParams marginTopHeight(int top, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(top);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
