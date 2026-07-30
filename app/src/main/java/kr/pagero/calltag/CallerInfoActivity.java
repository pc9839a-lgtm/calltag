package kr.pagero.calltag;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CallerInfoActivity extends Activity {
    public static final String EXTRA_CUSTOMER_ID = "customer_id";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_STAGE = "stage";
    public static final String EXTRA_STAGE_COLOR = "stage_color";
    public static final String EXTRA_MEMO = "memo";
    public static final String EXTRA_LAST_CONTACT_AT = "last_contact_at";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoClose = this::finish;
    private int notificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_caller_info);

        Intent intent = getIntent();
        long customerId = intent.getLongExtra(EXTRA_CUSTOMER_ID, -1L);
        notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
        String name = safe(intent.getStringExtra(EXTRA_NAME), "등록 고객");
        String phone = safe(intent.getStringExtra(EXTRA_PHONE), "");
        String stage = safe(intent.getStringExtra(EXTRA_STAGE), "상태 미지정");
        String memo = safe(intent.getStringExtra(EXTRA_MEMO), "저장된 메모가 없습니다.");
        String stageColor = safe(intent.getStringExtra(EXTRA_STAGE_COLOR), "#4389FF");
        long lastContactAt = intent.getLongExtra(EXTRA_LAST_CONTACT_AT, 0L);

        TextView nameView = findViewById(R.id.callerInfoName);
        TextView phoneView = findViewById(R.id.callerInfoPhone);
        TextView stageView = findViewById(R.id.callerInfoStage);
        TextView memoWrap = findViewById(R.id.callerInfoMemoWrap);
        TextView memoView = findViewById(R.id.callerInfoMemo);
        TextView lastContactView = findViewById(R.id.callerInfoLastContact);
        TextView privacyMessage = findViewById(R.id.callerInfoPrivacyMessage);

        nameView.setText(name);
        phoneView.setText(phone);
        stageView.setText(stage);
        memoView.setText(memo);
        applyStageStyle(stageView, stageColor);
        lastContactView.setText(lastContactAt > 0L
                ? "최근 연락 · " + new SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA)
                .format(new Date(lastContactAt))
                : "최근 연락 기록 없음");

        boolean locked = isDeviceLocked();
        int privacyMode = SettingsStore.callerPrivacyMode(this);
        if (locked && privacyMode == SettingsStore.CALLER_PRIVACY_NAME) {
            stageView.setVisibility(View.GONE);
            memoWrap.setVisibility(View.GONE);
            lastContactView.setVisibility(View.GONE);
            privacyMessage.setVisibility(View.VISIBLE);
            privacyMessage.setText("잠금화면에서는 고객명만 표시하도록 설정되어 있습니다.");
        } else if (locked && privacyMode == SettingsStore.CALLER_PRIVACY_STAGE) {
            memoWrap.setVisibility(View.GONE);
            lastContactView.setVisibility(View.GONE);
            privacyMessage.setVisibility(View.VISIBLE);
            privacyMessage.setText("잠금화면에서는 고객명과 상태까지만 표시합니다.");
        }

        findViewById(R.id.callerInfoClose).setOnClickListener(v -> finish());
        findViewById(R.id.callerInfoReturn).setOnClickListener(v -> finish());
        findViewById(R.id.callerInfoDetail).setOnClickListener(v -> {
            if (customerId <= 0L) return;
            startActivity(new Intent(this, CustomerDetailActivity.class)
                    .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customerId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        handler.postDelayed(autoClose, 45000L);
    }

    private boolean isDeviceLocked() {
        KeyguardManager manager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return manager != null && manager.isDeviceLocked();
    }

    private void applyStageStyle(TextView view, String rawColor) {
        int color;
        try {
            color = Color.parseColor(rawColor);
        } catch (IllegalArgumentException ignored) {
            color = getColor(R.color.primary);
        }
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(11));
        view.setBackground(background);
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(autoClose);
        if (notificationId != 0) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(notificationId);
        }
        super.onDestroy();
    }
}
