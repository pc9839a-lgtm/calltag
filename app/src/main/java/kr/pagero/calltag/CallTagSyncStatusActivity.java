package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
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

/** 더보기 > 데이터 보호·복구. 콜태그 공통 다크 카드 UI를 사용한다. */
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
    private TextView syncButton;
    private TextView devicesButton;
    private TextView eraseButton;
    private boolean binding;
    private boolean eraseRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(36));
        root.setBackgroundColor(getColor(R.color.background));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, false, R.color.text_primary);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView screenTitle = text("데이터 보호·복구", 21f, true, R.color.text_primary);
        LinearLayout.LayoutParams screenTitleParams = new LinearLayout.LayoutParams(
                0, dp(46), 1f);
        screenTitleParams.leftMargin = dp(8);
        screenTitle.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(screenTitle, screenTitleParams);
        root.addView(header);

        LinearLayout hero = card();
        hero.addView(text("앱을 다시 설치해도 고객정보를 되찾습니다.",
                18f, true, R.color.text_primary));
        hero.addView(text(
                "고객·상담·메모·후속 일정을 계정별로 암호화해 보관합니다. 통화 녹음과 휴대폰 전체 연락처·문자함은 올리지 않습니다.",
                13.5f, false, R.color.text_secondary), marginTop(8));
        root.addView(hero, marginTop(18));

        LinearLayout consentCard = card();
        enabledSwitch = new Switch(this);
        enabledSwitch.setText("데이터 보호 켜기");
        enabledSwitch.setTextSize(15f);
        enabledSwitch.setTextColor(getColor(R.color.text_primary));
        enabledSwitch.setGravity(Gravity.CENTER_VERTICAL);
        enabledSwitch.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        consentCard.addView(enabledSwitch, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        root.addView(consentCard, marginTop(10));

        LinearLayout statusCard = card();
        statusTitle = text("대기 중", 17f, true, R.color.text_primary);
        statusCard.addView(statusTitle);
        statusMessage = text("동기화 상태를 확인하고 있습니다.", 13.5f, false,
                R.color.text_secondary);
        statusCard.addView(statusMessage, marginTop(7));
        details = text("", 12.5f, false, R.color.text_muted);
        details.setLineSpacing(dp(3), 1f);
        statusCard.addView(details, marginTop(10));
        root.addView(statusCard, marginTop(10));

        syncButton = actionButton("지금 동기화", true, false);
        syncButton.setOnClickListener(v -> {
            if (!AuthSessionStore.hasSession(this)) {
                showMessage("로그인 후 사용할 수 있습니다.");
                return;
            }
            if (!CallTagSyncPreferenceStore.isEnabled(this)) {
                requestEnable();
                return;
            }
            CallTagSyncManager.request(this, true);
            refresh();
        });
        root.addView(syncButton, marginTopHeight(14, 50));

        devicesButton = actionButton("연결된 기기 관리", false, false);
        devicesButton.setOnClickListener(v -> {
            if (!AuthSessionStore.hasSession(this)) {
                showMessage("로그인 후 사용할 수 있습니다.");
                return;
            }
            startActivity(new Intent(this, CallTagSyncDevicesActivity.class));
        });
        root.addView(devicesButton, marginTopHeight(8, 50));

        LinearLayout deleteCard = card();
        deleteCard.addView(text("서버 복구본 관리", 15f, true, R.color.text_primary));
        deleteCard.addView(text(
                "데이터 보호를 끄면 서버 복구본은 유지됩니다. 서버에서 완전히 지우려면 아래 삭제를 실행해야 합니다.",
                13f, false, R.color.text_secondary), marginTop(7));
        eraseButton = actionButton("서버 복구본 삭제", false, true);
        eraseButton.setOnClickListener(v -> confirmErase());
        deleteCard.addView(eraseButton, marginTopHeight(12, 48));
        root.addView(deleteCard, marginTop(16));

        TextView localNote = text(
                "서버 연결에 실패해도 이 기기의 고객정보는 삭제되지 않습니다. 기존 암호화 백업 파일 기능도 유지됩니다.",
                12.5f, false, R.color.text_muted);
        root.addView(localNote, marginTop(16));

        enabledSwitch.setOnCheckedChangeListener(this::onEnabledChanged);
        return scroll;
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
        showConfirm(
                "데이터 보호를 켤까요?",
                "고객 이름·전화번호·메모·상담 기록·후속 일정이 계정별 암호화 상태로 서버에 보관됩니다. 통화 녹음과 전체 연락처는 전송하지 않습니다.",
                "켜기",
                () -> {
                    CallTagSyncPreferenceStore.setEnabled(this, true);
                    binding = true;
                    enabledSwitch.setChecked(true);
                    binding = false;
                    CallTagSyncManager.request(this, true);
                    refresh();
                });
    }

    private void confirmErase() {
        if (eraseRunning) return;
        if (!AuthSessionStore.hasSession(this)) {
            showMessage("로그인 후 서버 복구본을 삭제할 수 있습니다.");
            return;
        }
        showConfirm(
                "서버 복구본을 삭제할까요?",
                "서버에 보관된 고객·상담·메모·후속 일정 복구본이 삭제됩니다. 이 휴대폰 안의 고객정보는 삭제되지 않습니다.",
                "계속",
                this::confirmEraseAgain);
    }

    private void confirmEraseAgain() {
        showConfirm(
                "삭제 후 되돌릴 수 없습니다",
                "다른 기기에서도 복구할 수 없게 됩니다. 현재 휴대폰의 데이터는 그대로 남습니다.",
                "서버 복구본 삭제",
                this::eraseServerCopy);
    }

    private void eraseServerCopy() {
        if (!CallTagSyncManager.beginMaintenance()) {
            showMessage("현재 데이터 보호 작업이 진행 중입니다. 완료된 뒤 다시 시도해주세요.");
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
                    showMessage("서버 복구본을 삭제했습니다. 이 휴대폰의 고객정보는 그대로 유지됩니다. 다시 데이터 보호를 켜면 새 복구본이 생성됩니다.");
                    refresh();
                });
            } catch (Exception error) {
                handler.post(() -> {
                    eraseRunning = false;
                    showMessage(userMessage(error));
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
        setEnabled(syncButton, loggedIn && !running && !maintenance && !eraseRunning);
        setEnabled(devicesButton, loggedIn && !eraseRunning);
        setEnabled(eraseButton, loggedIn && !running && !maintenance && !eraseRunning);
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

    private void showMessage(String message) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setMessage(message)
                .setPositiveButton("확인", null)
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private void showConfirm(String title, String message, String positive, Runnable action) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("취소", null)
                .setPositiveButton(positive, (ignored, which) -> action.run())
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private TextView actionButton(String value, boolean primary, boolean danger) {
        TextView view = text(value, 14f, true,
                danger ? R.color.danger : primary ? android.R.color.white : R.color.text_primary);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private void setEnabled(TextView view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.45f);
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
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
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
