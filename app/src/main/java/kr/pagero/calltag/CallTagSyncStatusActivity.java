package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CallTagSyncStatusActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Runnable refreshLoop = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, CallTagSyncManager.isRunning() ? 700L : 2_500L);
        }
    };

    private Switch enabledSwitch;
    private TextView statusTitle;
    private TextView statusMessage;
    private TextView details;
    private Button syncButton;
    private Button devicesButton;
    private Button eraseButton;
    private boolean binding;
    private boolean eraseRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("데이터 보호·복구");

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        root.setBackgroundColor(getColor(R.color.surface));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("앱을 다시 설치해도\n고객정보를 되찾습니다.", 25f, true,
                R.color.text_primary);
        root.addView(title);

        TextView intro = text(
                "고객·상담·메모·후속 일정을 계정별로 암호화해 보관합니다. " +
                        "통화 녹음, 휴대폰 전체 연락처와 전체 문자함은 올리지 않습니다.",
                14f, false, R.color.text_secondary);
        root.addView(intro, marginTop(12));

        LinearLayout consentCard = card();
        enabledSwitch = new Switch(this);
        enabledSwitch.setText("데이터 보호 켜기");
        enabledSwitch.setTextSize(16f);
        enabledSwitch.setTextColor(getColor(R.color.text_primary));
        enabledSwitch.setGravity(Gravity.CENTER_VERTICAL);
        consentCard.addView(enabledSwitch, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
        root.addView(consentCard, marginTop(22));

        LinearLayout statusCard = card();
        statusTitle = text("대기 중", 17f, true, R.color.text_primary);
        statusCard.addView(statusTitle);
        statusMessage = text("동기화 상태를 확인하고 있습니다.", 14f, false,
                R.color.text_secondary);
        statusCard.addView(statusMessage, marginTop(8));
        details = text("", 13f, false, R.color.text_muted);
        statusCard.addView(details, marginTop(12));
        root.addView(statusCard, marginTop(14));

        syncButton = new Button(this);
        syncButton.setText("지금 동기화");
        syncButton.setTextSize(15f);
        syncButton.setAllCaps(false);
        syncButton.setOnClickListener(v -> {
            if (!AuthSessionStore.hasSession(this)) {
                new AlertDialog.Builder(this)
                        .setMessage("로그인 후 사용할 수 있습니다.")
                        .setPositiveButton("확인", null)
                        .show();
                return;
            }
            if (!CallTagSyncPreferenceStore.isEnabled(this)) {
                requestEnable();
                return;
            }
            CallTagSyncManager.request(this, true);
            refresh();
        });
        root.addView(syncButton, marginTopHeight(16, 50));

        devicesButton = new Button(this);
        devicesButton.setText("연결된 기기 관리");
        devicesButton.setTextSize(15f);
        devicesButton.setAllCaps(false);
        devicesButton.setOnClickListener(v -> {
            if (!AuthSessionStore.hasSession(this)) {
                new AlertDialog.Builder(this)
                        .setMessage("로그인 후 사용할 수 있습니다.")
                        .setPositiveButton("확인", null)
                        .show();
                return;
            }
            startActivity(new Intent(this, CallTagSyncDevicesActivity.class));
        });
        root.addView(devicesButton, marginTopHeight(10, 50));

        LinearLayout deleteCard = card();
        deleteCard.addView(text("서버 복구본 관리", 16f, true, R.color.text_primary));
        deleteCard.addView(text(
                "데이터 보호를 끄는 것과 서버 복구본 삭제는 다릅니다. 보호를 끄면 서버 복구본은 유지되고, 아래 삭제를 실행해야 서버에서 완전히 제거됩니다.",
                13f, false, R.color.text_secondary), marginTop(8));
        eraseButton = new Button(this);
        eraseButton.setText("서버 복구본 삭제");
        eraseButton.setTextSize(14f);
        eraseButton.setAllCaps(false);
        eraseButton.setOnClickListener(v -> confirmErase());
        deleteCard.addView(eraseButton, marginTopHeight(12, 48));
        root.addView(deleteCard, marginTop(16));

        TextView localNote = text(
                "서버 연결에 실패해도 기기 안의 고객정보는 삭제되지 않습니다. " +
                        "기존 암호화 백업 파일 기능도 함께 유지됩니다.",
                13f, false, R.color.text_muted);
        root.addView(localNote, marginTop(18));

        enabledSwitch.setOnCheckedChangeListener(this::onEnabledChanged);
        setContentView(scroll);
    }

    private void onEnabledChanged(CompoundButton button, boolean checked) {
        if (binding) return;
        if (checked) {
            binding = true;
            button.setChecked(false);
            binding = false;
            requestEnable();
        } else {
            CallTagSyncPreferenceStore.setEnabled(this, false);
            refresh();
        }
    }

    private void requestEnable() {
        new AlertDialog.Builder(this)
                .setTitle("데이터 보호를 켤까요?")
                .setMessage("고객 이름·전화번호·메모·상담 기록·후속 일정이 계정별 암호화 상태로 서버에 보관됩니다. 통화 녹음과 전체 연락처는 전송하지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("켜기", (dialog, which) -> {
                    CallTagSyncPreferenceStore.setEnabled(this, true);
                    binding = true;
                    enabledSwitch.setChecked(true);
                    binding = false;
                    CallTagSyncManager.request(this, true);
                    refresh();
                })
                .show();
    }

    private void confirmErase() {
        if (eraseRunning) return;
        if (!AuthSessionStore.hasSession(this)) {
            new AlertDialog.Builder(this)
                    .setMessage("로그인 후 서버 복구본을 삭제할 수 있습니다.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("서버 복구본을 삭제할까요?")
                .setMessage("서버에 보관된 고객·상담·메모·후속 일정 복구본이 삭제됩니다. 이 휴대폰 안의 고객정보는 삭제되지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("계속", (dialog, which) -> confirmEraseAgain())
                .show();
    }

    private void confirmEraseAgain() {
        new AlertDialog.Builder(this)
                .setTitle("삭제 후 되돌릴 수 없습니다")
                .setMessage("다른 기기에서도 복구할 수 없게 됩니다. 현재 휴대폰의 데이터는 그대로 남습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("서버 복구본 삭제", (dialog, which) -> eraseServerCopy())
                .show();
    }

    private void eraseServerCopy() {
        if (!CallTagSyncManager.beginMaintenance()) {
            new AlertDialog.Builder(this)
                    .setMessage("현재 데이터 보호 작업이 진행 중입니다. 완료된 뒤 다시 시도해주세요.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }
        eraseRunning = true;
        eraseButton.setEnabled(false);
        syncButton.setEnabled(false);
        devicesButton.setEnabled(false);
        statusTitle.setText("서버 복구본 삭제 중");
        statusMessage.setText("현재 휴대폰의 고객정보는 건드리지 않습니다.");
        executor.execute(() -> {
            try {
                CallTagSyncApiClient.eraseServerData(
                        AuthSessionStore.session(this),
                        CallTagSyncDeviceStore.deviceId(this));

                CallTagSyncPreferenceStore.setEnabled(this, false);
                CallTagSyncLocalStore store = new CallTagSyncLocalStore(this);
                String accountKey = store.accountKey();
                if (!accountKey.isEmpty()) {
                    store.getWritableDatabase().delete("entity_map", "account_key=?",
                            new String[]{accountKey});
                    store.getWritableDatabase().delete("sync_meta", "account_key=?",
                            new String[]{accountKey});
                }
                store.close();
                CallTagSyncDeviceStore.rotate(this);

                handler.post(() -> {
                    eraseRunning = false;
                    new AlertDialog.Builder(this)
                            .setTitle("삭제 완료")
                            .setMessage("서버 복구본을 삭제했습니다. 이 휴대폰의 고객정보는 그대로 유지됩니다. 다시 데이터 보호를 켜면 새 복구본이 생성됩니다.")
                            .setPositiveButton("확인", null)
                            .show();
                    refresh();
                });
            } catch (Exception error) {
                handler.post(() -> {
                    eraseRunning = false;
                    new AlertDialog.Builder(this)
                            .setTitle("삭제하지 못했습니다")
                            .setMessage(userMessage(error))
                            .setPositiveButton("확인", null)
                            .show();
                    refresh();
                });
            } finally {
                CallTagSyncManager.endMaintenance();
            }
        });
    }

    private String userMessage(Exception error) {
        if (error instanceof CallTagSyncApiClient.ApiException) {
            CallTagSyncApiClient.ApiException api = (CallTagSyncApiClient.ApiException) error;
            if ("CALLTAG_SYNC_NOT_ENABLED".equals(api.code)
                    || api.status == 404 || api.status == 405 || api.status >= 500) {
                return "서버 기능을 준비 중입니다. 현재 휴대폰의 데이터는 그대로 유지됩니다.";
            }
            if (api.status == 401 || api.status == 403) {
                return "로그인을 다시 확인해주세요.";
            }
        }
        String message = error == null ? "" : String.valueOf(error.getMessage()).trim();
        return message.isEmpty() ? "서버 복구본을 삭제하지 못했습니다." : message;
    }

    private void refresh() {
        boolean loggedIn = AuthSessionStore.hasSession(this);
        boolean enabled = CallTagSyncPreferenceStore.isEnabled(this);
        boolean running = CallTagSyncManager.isRunning();
        boolean maintenance = CallTagSyncManager.isMaintenanceRunning();
        binding = true;
        enabledSwitch.setChecked(enabled);
        enabledSwitch.setEnabled(loggedIn && !eraseRunning && !maintenance);
        binding = false;

        CallTagSyncLocalStore store = new CallTagSyncLocalStore(this);
        CallTagSyncLocalStore.StatusSnapshot state = store.status();
        store.close();

        String label = statusLabel(state.status, enabled, loggedIn);
        if (!eraseRunning) {
            statusTitle.setText(label);
            statusMessage.setText(state.message);
        }
        details.setText("마지막 완료  " + time(state.lastSuccessAt)
                + "\n대기 중 변경  " + state.pendingCount + "건"
                + "\n서버 보관  " + state.serverRecords + "건");
        syncButton.setEnabled(loggedIn && !running && !maintenance && !eraseRunning);
        devicesButton.setEnabled(loggedIn && !eraseRunning);
        eraseButton.setEnabled(loggedIn && !running && !maintenance && !eraseRunning);
        syncButton.setText(running
                ? "동기화 중…" : enabled ? "지금 동기화" : "데이터 보호 켜기");
        eraseButton.setText(eraseRunning ? "삭제 중…" : "서버 복구본 삭제");
    }

    private String statusLabel(String status, boolean enabled, boolean loggedIn) {
        if (!loggedIn) return "로그인이 필요합니다";
        if (!enabled) return "기기 안에만 저장 중";
        if (CallTagSyncManager.isRunning()) return "안전하게 동기화 중";
        if ("SYNCED".equals(status)) return "데이터 보호 완료";
        if ("PREPARING".equals(status)) return "서버 기능 준비 중";
        if ("ERROR".equals(status)) return "다시 확인이 필요합니다";
        if ("AUTH_REQUIRED".equals(status)) return "로그인을 다시 확인해주세요";
        return "데이터 보호 대기 중";
    }

    private String time(long millis) {
        if (millis <= 0L) return "아직 없음";
        return new SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(new Date(millis));
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

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refreshLoop);
        handler.post(refreshLoop);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(refreshLoop);
        super.onPause();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
