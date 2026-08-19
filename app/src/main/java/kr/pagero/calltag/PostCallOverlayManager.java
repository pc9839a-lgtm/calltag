package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Compact post-call memo overlay. It never launches the app Activity automatically and requires
 * SYSTEM_ALERT_WINDOW to have been explicitly granted by the user.
 */
public final class PostCallOverlayManager {
    private static final long MAIN_THREAD_DELIVERY_TIMEOUT_MS = 2_000L;

    private static WindowManager windowManager;
    private static View overlayView;
    private static long showingCallId = -1L;

    private PostCallOverlayManager() {}

    public static boolean canShow(Context context) {
        return context != null && CallerOverlayManager.canShow(context);
    }

    /**
     * WindowManager.addView must run on the main thread. Live call delivery already arrives there,
     * but WorkManager recovery can run on a worker thread; synchronously marshal that case to main
     * so a recovered call can still show the same compact popup instead of silently degrading.
     */
    public static boolean show(Context context, CallRecord record, Customer customer,
                               Intent reviewIntent, String memo) {
        if (context == null || record == null || reviewIntent == null || !canShow(context)) {
            return false;
        }
        Context app = context.getApplicationContext();
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return showCheckedOnMain(app, record, customer, reviewIntent, memo);
        }

        AtomicBoolean shown = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                shown.set(showCheckedOnMain(app, record, customer, reviewIntent, memo));
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(MAIN_THREAD_DELIVERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                CrashTelemetryStore.record(app, "post_call_overlay", "main_dispatch_timeout",
                        deviceLabel() + ",call=" + record.id);
                return false;
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            CrashTelemetryStore.record(app, "post_call_overlay", "main_dispatch_interrupted",
                    deviceLabel() + ",call=" + record.id);
            return false;
        }
        return shown.get();
    }

    private static boolean showCheckedOnMain(Context context, CallRecord record, Customer customer,
                                             Intent reviewIntent, String memo) {
        if (showingCallId == record.id && overlayView != null && overlayView.isAttachedToWindow()) {
            return true;
        }
        return showOnMain(context, record, customer, reviewIntent, memo);
    }

    public static void hide(Context context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            hideOnMain();
        } else if (context != null) {
            new Handler(Looper.getMainLooper()).post(PostCallOverlayManager::hideOnMain);
        }
    }

    public static boolean isShowing() {
        View current = overlayView;
        return current != null && current.isAttachedToWindow();
    }

    private static boolean showOnMain(Context context, CallRecord record, Customer customer,
                                      Intent reviewIntent, String memo) {
        hideOnMain();
        try {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager == null) return false;
            overlayView = buildView(context, record, customer, reviewIntent, memo);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_SECURE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = dp(context, 54);
            params.windowAnimations = android.R.style.Animation_Dialog;
            params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
            windowManager.addView(overlayView, params);
            if (!overlayView.isAttachedToWindow()) {
                hideOnMain();
                return false;
            }
            showingCallId = record.id;
            CrashTelemetryStore.record(context, "post_call_overlay", "shown",
                    deviceLabel() + ",call=" + record.id);
            return true;
        } catch (RuntimeException error) {
            CrashTelemetryStore.record(context, "post_call_overlay", "show_failed",
                    deviceLabel() + "," + error.getClass().getSimpleName());
            hideOnMain();
            return false;
        }
    }

    private static View buildView(Context context, CallRecord record, Customer customer,
                                  Intent reviewIntent, String memo) {
        LinearLayout outer = new LinearLayout(context);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(context, 12), 0, dp(context, 12), 0);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 16));
        card.setElevation(dp(context, 18));
        card.setBackground(cardBackground(context));
        outer.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(context, "콜태그 · 통화 종료", 13f,
                context.getColor(R.color.primary), true);
        header.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = action(context, "닫기", false);
        close.setOnClickListener(v -> hide(context));
        header.addView(close, new LinearLayout.LayoutParams(dp(context, 62), dp(context, 38)));
        card.addView(header);

        EditText nameInput = new EditText(context);
        nameInput.setSingleLine(true);
        nameInput.setTextSize(19f);
        nameInput.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameInput.setTextColor(context.getColor(R.color.text_primary));
        nameInput.setHintTextColor(context.getColor(R.color.text_muted));
        nameInput.setBackgroundResource(R.drawable.bg_input);
        nameInput.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        String initialName = customer == null
                ? safe(record.cachedName) : safe(customer.displayName);
        if (initialName.isEmpty() || "이름없는고객".equals(initialName)) {
            initialName = defaultCustomerName(record.phone);
        }
        nameInput.setText(initialName);
        nameInput.setSelection(nameInput.length());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 46));
        nameParams.topMargin = dp(context, 10);
        card.addView(nameInput, nameParams);

        TextView phone = text(context,
                safe(record.phone) + " · " + CallDisposition.label(record),
                12.5f, context.getColor(R.color.text_secondary), false);
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        phoneParams.topMargin = dp(context, 7);
        card.addView(phone, phoneParams);

        EditText memoInput = new EditText(context);
        memoInput.setMinLines(2);
        memoInput.setMaxLines(4);
        memoInput.setGravity(Gravity.TOP | Gravity.START);
        memoInput.setTextSize(15f);
        memoInput.setTextColor(context.getColor(R.color.text_primary));
        memoInput.setHintTextColor(context.getColor(R.color.text_muted));
        memoInput.setHint("메모를 입력하세요");
        memoInput.setBackgroundResource(R.drawable.bg_input);
        memoInput.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        String initialMemo = customer == null ? "" : safe(memo);
        memoInput.setText(initialMemo);
        memoInput.setSelection(memoInput.length());
        LinearLayout.LayoutParams memoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 92));
        memoParams.topMargin = dp(context, 10);
        card.addView(memoInput, memoParams);

        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        TextView save = action(context, "저장", true);
        save.setOnClickListener(v -> save(context, record, customer, reviewIntent,
                nameInput, memoInput, save));
        actions.addView(save, new LinearLayout.LayoutParams(dp(context, 112), dp(context, 44)));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        actionParams.topMargin = dp(context, 10);
        card.addView(actions, actionParams);
        return outer;
    }

    private static void save(Context context, CallRecord record, Customer customer,
                             Intent reviewIntent, EditText nameInput, EditText memoInput,
                             TextView saveButton) {
        if (PhoneNumberNormalizer.normalize(record.phone).length() < 8) {
            Toast.makeText(context, "전화번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        String fingerprint = record.id > 0L
                ? "call_log:" + record.id
                : PhoneNumberNormalizer.normalize(record.phone) + ":" + record.startedAt + ":"
                + record.durationSec + ":" + record.type;
        if (SettingsStore.isCallProcessed(context, fingerprint)) {
            markPendingHandled(context, reviewIntent);
            hide(context);
            return;
        }

        String name = safe(nameInput.getText().toString());
        String note = safe(memoInput.getText().toString());
        if (name.isEmpty()) name = defaultCustomerName(record.phone);
        saveButton.setEnabled(false);
        saveButton.setAlpha(0.55f);
        saveButton.setText("저장 중");

        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            Customer latest = db.findByPhone(record.phone);
            long customerId;
            String stage;
            if (latest == null) {
                stage = db.firstStage();
                customerId = db.insertCustomer(name, record.phone, stage, "");
            } else {
                customerId = latest.id;
                stage = latest.relationStatus;
            }
            db.updateCustomerProfile(customerId, name, stage, note);
            long startedAt = Math.max(0L, record.startedAt);
            long endedAt = Math.max(startedAt, record.endedAt());
            long interactionId = CallInteractionDeduper.insertOnce(
                    db,
                    customerId,
                    CallDisposition.interactionType(record.type),
                    startedAt,
                    endedAt,
                    Math.max(0L, record.durationSec),
                    "MEMO_SAVED",
                    note);
            SettingsStore.markCallProcessed(context, fingerprint);
            markPendingHandled(context, reviewIntent);
            PostCallRecoveryStore.markDelivered(context, record.id);
            CrashTelemetryStore.record(context, "post_call_overlay", "saved",
                    "call=" + record.id + ",interaction=" + interactionId);
            hideKeyboard(context, memoInput);
            hide(context);
            Toast.makeText(context, "고객명과 메모를 저장했습니다.", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException error) {
            saveButton.setEnabled(true);
            saveButton.setAlpha(1f);
            saveButton.setText("저장");
            CrashTelemetryStore.record(context, "post_call_overlay", "save_failed",
                    error.getClass().getSimpleName());
            Toast.makeText(context, "저장하지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show();
        } finally {
            db.close();
        }
    }

    private static void markPendingHandled(Context context, Intent reviewIntent) {
        long pendingId = reviewIntent.getLongExtra(PostCallActivity.EXTRA_PENDING_CALL_ID, -1L);
        if (pendingId <= 0L) return;
        PendingCallStore store = new PendingCallStore(context);
        try {
            store.markHandled(pendingId);
        } catch (RuntimeException error) {
            CrashTelemetryStore.record(context, "pending_call_cleanup", "overlay_failed",
                    error.getClass().getSimpleName());
        } finally {
            store.close();
        }
        context.sendBroadcast(new Intent(PendingCallSectionView.ACTION_CHANGED)
                .setPackage(context.getPackageName()));
    }

    private static void hideKeyboard(Context context, View target) {
        try {
            InputMethodManager manager =
                    (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) manager.hideSoftInputFromWindow(target.getWindowToken(), 0);
        } catch (RuntimeException ignored) {
            // Window can disappear between save and keyboard dismissal.
        }
    }

    private static void hideOnMain() {
        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeViewImmediate(overlayView);
            } catch (RuntimeException ignored) {
                // System/OEM may already have removed the overlay.
            }
        }
        overlayView = null;
        windowManager = null;
        showingCallId = -1L;
    }

    private static TextView action(Context context, String value, boolean primary) {
        TextView view = text(context, value, 14f,
                context.getColor(primary ? android.R.color.white : R.color.text_primary), true);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
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

    private static String defaultCustomerName(String rawPhone) {
        String normalized = PhoneNumberNormalizer.normalize(rawPhone);
        String suffix = normalized.length() >= 4
                ? normalized.substring(normalized.length() - 4) : normalized;
        return suffix.isEmpty() ? "새 고객" : "고객 " + suffix;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String deviceLabel() {
        return "oem=" + safe(Build.MANUFACTURER) + ",model=" + safe(Build.MODEL);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
