package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CallerOverlayManager {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private static WindowManager windowManager;
    private static View overlayView;

    private CallerOverlayManager() {}

    public static boolean canShow(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static void openPermissionSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (RuntimeException ignored) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static boolean show(Context context, Customer customer, String memo, String stageColor) {
        if (customer == null || !canShow(context)) return false;
        Context app = context.getApplicationContext();
        HANDLER.post(() -> showOnMain(app, customer, memo, stageColor, false));
        return true;
    }

    public static boolean showSetupTest(Context context) {
        if (!canShow(context)) return false;
        Context app = context.getApplicationContext();
        long now = System.currentTimeMillis();
        Customer demo = new Customer(
                -1L,
                "테스트 고객",
                "010-1234-5678",
                "01012345678",
                "진행 중",
                "",
                "견적서 수정 후 금요일 오전에 다시 연락하기",
                now - 86_400_000L,
                now - 3_600_000L,
                null);
        HANDLER.post(() -> showOnMain(app, demo, demo.memo, "#F5A524", true));
        return true;
    }

    public static void hide(Context context) {
        HANDLER.post(CallerOverlayManager::hideOnMain);
    }

    private static void showOnMain(Context context, Customer customer, String memo,
                                   String stageColor, boolean setupTest) {
        hideOnMain();
        try {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager == null) return;

            overlayView = buildView(context, customer, memo, stageColor, setupTest);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = dp(context, 54);
            params.windowAnimations = android.R.style.Animation_Dialog;
            windowManager.addView(overlayView, params);
        } catch (RuntimeException ignored) {
            hideOnMain();
        }
    }

    private static View buildView(Context context, Customer customer, String memo,
                                  String stageColor, boolean setupTest) {
        LinearLayout outer = new LinearLayout(context);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(context, 12), 0, dp(context, 12), 0);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 17));
        card.setElevation(dp(context, 18));
        card.setBackground(cardBackground(context));
        outer.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(context,
                setupTest ? "콜태그 · 수신 화면 테스트" : "콜태그 · 전화 온 고객",
                13f, context.getColor(R.color.primary), true);
        header.addView(label, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = text(context, setupTest ? "다시 설정" : "닫기", 13f,
                context.getColor(R.color.text_primary), true);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundResource(R.drawable.bg_secondary_button);
        close.setOnClickListener(v -> hide(context));
        header.addView(close, new LinearLayout.LayoutParams(
                setupTest ? dp(context, 86) : dp(context, 62), dp(context, 40)));
        card.addView(header);

        TextView name = text(context, customer.displayName, 24f,
                context.getColor(R.color.text_primary), true);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(context, 10);
        card.addView(name, nameParams);

        LinearLayout meta = new LinearLayout(context);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        int stageInt = parseColor(context, stageColor);
        TextView stage = text(context,
                empty(customer.relationStatus) ? "상태 미지정" : customer.relationStatus,
                13f, contrast(stageInt), true);
        stage.setPadding(dp(context, 11), dp(context, 6), dp(context, 11), dp(context, 6));
        stage.setBackground(pill(stageInt));
        meta.addView(stage);

        TextView phone = text(context, customer.primaryPhone, 14f,
                context.getColor(R.color.text_secondary), false);
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        phoneParams.leftMargin = dp(context, 11);
        meta.addView(phone, phoneParams);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = dp(context, 9);
        card.addView(meta, metaParams);

        String safeMemo = memo == null ? "" : memo.trim();
        TextView memoTitle = text(context, "최근 메모", 12f,
                context.getColor(R.color.text_muted), true);
        LinearLayout.LayoutParams memoTitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        memoTitleParams.topMargin = dp(context, 14);
        card.addView(memoTitle, memoTitleParams);

        TextView memoView = text(context,
                safeMemo.isEmpty() ? "저장된 메모가 없습니다." : safeMemo,
                15f,
                safeMemo.isEmpty() ? context.getColor(R.color.text_muted)
                        : context.getColor(R.color.text_primary),
                false);
        memoView.setMaxLines(4);
        memoView.setLineSpacing(0f, 1.25f);
        memoView.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        memoView.setBackgroundResource(R.drawable.bg_soft_panel);
        LinearLayout.LayoutParams memoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        memoParams.topMargin = dp(context, 7);
        card.addView(memoView, memoParams);

        TextView lastContact = text(context,
                "최근 연락 · " + new SimpleDateFormat("M/d a h:mm", Locale.KOREA)
                        .format(new Date(customer.lastContactAt)),
                12f, context.getColor(R.color.text_muted), false);
        LinearLayout.LayoutParams lastParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lastParams.topMargin = dp(context, 9);
        card.addView(lastContact, lastParams);

        Button primary = new Button(context);
        primary.setText(setupTest ? "정상적으로 보입니다 · 앱 시작" : "고객 상세 보기");
        primary.setAllCaps(false);
        primary.setTextSize(15f);
        primary.setTextColor(context.getColor(R.color.text_primary));
        primary.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        primary.setMinHeight(0);
        primary.setMinWidth(0);
        primary.setBackgroundResource(R.drawable.bg_primary_button);
        primary.setOnClickListener(v -> {
            if (setupTest) {
                SetupRequirements.markOverlayTestPassed(context);
                SetupRequirements.startCallMonitoring(context);
                hide(context);
                try {
                    context.startActivity(new Intent(context, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                } catch (RuntimeException ignored) {
                    context.startActivity(SetupRequirements.requiredSetupIntent(context)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
                return;
            }
            hide(context);
            try {
                context.startActivity(new Intent(context, CustomerDetailActivity.class)
                        .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customer.id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            } catch (RuntimeException ignored) {
                // 전화 화면은 그대로 유지한다.
            }
        });
        LinearLayout.LayoutParams primaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 48));
        primaryParams.topMargin = dp(context, 12);
        card.addView(primary, primaryParams);
        return outer;
    }

    private static void hideOnMain() {
        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeViewImmediate(overlayView);
            } catch (RuntimeException ignored) {
                // 이미 시스템에서 제거했을 수 있다.
            }
        }
        overlayView = null;
        windowManager = null;
    }

    private static TextView text(Context context, String value, float size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value == null ? "" : value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static GradientDrawable cardBackground(Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(context.getColor(R.color.surface));
        shape.setCornerRadius(dp(context, 20));
        shape.setStroke(dp(context, 1), context.getColor(R.color.border));
        return shape;
    }

    private static GradientDrawable pill(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(999f);
        return shape;
    }

    private static int parseColor(Context context, String value) {
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException ignored) {
            return context.getColor(R.color.primary);
        }
    }

    private static int contrast(int background) {
        double luminance = (0.299 * Color.red(background)
                + 0.587 * Color.green(background)
                + 0.114 * Color.blue(background)) / 255.0;
        return luminance > 0.62 ? Color.BLACK : Color.WHITE;
    }

    private static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
