package kr.pagero.calltag;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 통화 종료 큰 팝업에 필요한 알림 채널과 full-screen intent 권한을 확인한다. */
public final class PostCallPopupAccessActivity extends Activity {
    private TextView status;
    private Button notificationButton;
    private Button fullScreenButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CallPopupNotificationManager.ensureChannels(this);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, true, R.color.text_primary);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = text("통화 종료 팝업", 22f, true, R.color.text_primary);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(6);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        status = text("", 16f, true, R.color.text_primary);
        status.setPadding(dp(16), dp(18), dp(16), dp(18));
        status.setBackgroundResource(R.drawable.bg_card);
        root.addView(status, top(16));

        root.addView(text(
                "통화가 끝난 직후 고객 메모·상태·다음 할 일을 남기는 큰 화면을 띄웁니다. "
                        + "Android 14 이상에서는 알림과 전체 화면 알림 권한을 각각 확인해야 합니다.",
                14f, false, R.color.text_secondary), top(14));

        notificationButton = button("통화 종료 알림 설정", false);
        notificationButton.setOnClickListener(v -> CallPopupNotificationManager.openChannelSettings(
                this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID));
        root.addView(notificationButton, fixedTop(52, 20));

        fullScreenButton = button("전체 화면 팝업 허용", true);
        fullScreenButton.setOnClickListener(v -> openFullScreenSettings());
        root.addView(fullScreenButton, fixedTop(52, 9));

        Button done = button("확인", false);
        done.setOnClickListener(v -> finish());
        root.addView(done, fixedTop(50, 22));
        return scroll;
    }

    private void render() {
        boolean channel = CallPopupNotificationManager.isPopupReady(
                this, CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
        boolean fullScreen = CallPopupNotificationManager.canUsePostCallFullScreen(this);
        if (channel && fullScreen) {
            status.setText("통화 종료 팝업 사용 가능");
            status.setTextColor(getColor(R.color.primary));
        } else if (!channel) {
            status.setText("통화 종료 알림이 꺼져 있습니다.");
            status.setTextColor(getColor(R.color.danger));
        } else {
            status.setText("전체 화면 팝업 권한을 켜주세요.");
            status.setTextColor(getColor(R.color.danger));
        }
        notificationButton.setText(channel ? "통화 종료 알림 확인" : "통화 종료 알림 켜기");
        boolean needsFullScreen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
        fullScreenButton.setVisibility(needsFullScreen ? Button.VISIBLE : Button.GONE);
        if (needsFullScreen) {
            fullScreenButton.setText(fullScreen ? "전체 화면 팝업 허용됨" : "전체 화면 팝업 허용");
            fullScreenButton.setEnabled(!fullScreen);
            fullScreenButton.setAlpha(fullScreen ? 0.55f : 1f);
        }
    }

    private void openFullScreenSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return;
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (RuntimeException error) {
            try {
                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            } catch (RuntimeException ignored) {
                Toast.makeText(this, "휴대전화 설정에서 전체 화면 알림을 허용해주세요.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(color));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.2f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
