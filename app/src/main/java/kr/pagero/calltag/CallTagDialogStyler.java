package kr.pagero.calltag;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Applies CallTag's theme-aware card language to framework dialogs that otherwise inherit OEM styling. */
public final class CallTagDialogStyler {
    private static final int MAX_WIDTH_DP = 420;
    private static final int SIDE_MARGIN_DP = 28;

    private CallTagDialogStyler() {}

    public static void apply(AlertDialog dialog) {
        if (dialog == null || !dialog.isShowing()) return;

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.bg_dialog_panel);
            WindowManager.LayoutParams params = window.getAttributes();
            int displayWidth = dialog.getContext().getResources().getDisplayMetrics().widthPixels;
            int availableWidth = Math.max(1, displayWidth - dp(dialog, SIDE_MARGIN_DP * 2));
            params.width = Math.min(availableWidth, dp(dialog, MAX_WIDTH_DP));
            params.dimAmount = CallTagThemeManager.isBlack(dialog.getContext()) ? 0.72f : 0.38f;
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            window.setLayout(params.width, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        styleText(dialog, android.R.id.message, R.color.text_secondary, 14f, false);
        int titleId = dialog.getContext().getResources().getIdentifier("alertTitle", "id", "android");
        if (titleId != 0) styleText(dialog, titleId, R.color.text_primary, 19f, true);

        styleButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), true);
        styleButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), false);
        styleButton(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), false);

        if (isDeleteConfirmation(dialog)) {
            applyCompactDangerActions(dialog);
        }

        if (dialog.getListView() != null) {
            dialog.getListView().setBackgroundColor(dialog.getContext().getColor(R.color.surface_soft));
            dialog.getListView().setDividerHeight(dp(dialog, 4));
        }
    }

    /** 삭제 팝업은 OEM 테마 tint와 무관하게 테마형 취소 / 빨간 삭제로 고정한다. */
    public static void applyDanger(AlertDialog dialog) {
        apply(dialog);
        if (dialog == null || !dialog.isShowing()) return;

        Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (cancel != null) {
            styleButton(cancel, false);
            cancel.setTextColor(cancel.getContext().getColor(R.color.text_primary));
            cancel.setBackground(rounded(cancel,
                    cancel.getContext().getColor(R.color.secondary_button_surface),
                    cancel.getContext().getColor(R.color.secondary_button_border)));
            cancel.setMinWidth(dp(cancel, 96));
        }

        Button delete = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (delete != null) {
            styleButton(delete, true);
            delete.setTextColor(Color.WHITE);
            int danger = delete.getContext().getColor(R.color.danger);
            delete.setBackground(rounded(delete, danger, danger));
            delete.setMinWidth(dp(delete, 96));
        }
    }

    /**
     * 삭제 확인창은 버튼을 내용 크기에 맞춰 작게 유지하고 버튼 사이 여백을 분명히 둔다.
     */
    public static void applyDangerCompact(AlertDialog dialog) {
        apply(dialog);
        if (dialog == null || !dialog.isShowing()) return;
        applyCompactDangerActions(dialog);
    }

    private static boolean isDeleteConfirmation(AlertDialog dialog) {
        if (dialog == null || !dialog.isShowing()) return false;
        Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button delete = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        return cancel != null && delete != null
                && "취소".contentEquals(cancel.getText())
                && "삭제".contentEquals(delete.getText());
    }

    private static void applyCompactDangerActions(AlertDialog dialog) {
        Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button delete = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        styleCompactDangerButton(cancel, false, true);
        styleCompactDangerButton(delete, true, false);

        View parent = cancel != null ? (View) cancel.getParent()
                : delete != null ? (View) delete.getParent() : null;
        if (parent instanceof LinearLayout) {
            LinearLayout actions = (LinearLayout) parent;
            actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            actions.setPadding(dp(actions, 16), dp(actions, 6),
                    dp(actions, 16), dp(actions, 10));
        }
    }

    private static void styleCompactDangerButton(Button button, boolean destructive,
                                                  boolean addRightGap) {
        if (button == null) return;
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextSize(13f);
        int fill = button.getContext().getColor(destructive
                ? R.color.danger : R.color.secondary_button_surface);
        int stroke = button.getContext().getColor(destructive
                ? R.color.danger : R.color.secondary_button_border);
        button.setTextColor(destructive
                ? Color.WHITE : button.getContext().getColor(R.color.text_primary));
        button.setBackground(rounded(button, fill, stroke));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(button, 40));
        button.setMinimumHeight(dp(button, 40));
        button.setPadding(dp(button, 14), dp(button, 2), dp(button, 14), dp(button, 2));

        ViewGroup.LayoutParams raw = button.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) raw;
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = dp(button, 40);
            params.weight = 0f;
            params.leftMargin = addRightGap ? 0 : dp(button, 8);
            params.rightMargin = addRightGap ? dp(button, 8) : 0;
            button.setLayoutParams(params);
        }
    }

    private static GradientDrawable rounded(View view, int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(view, 12));
        drawable.setStroke(dp(view, 1), stroke);
        return drawable;
    }

    private static void styleText(AlertDialog dialog, int id, int color, float size, boolean bold) {
        View value = dialog.findViewById(id);
        if (!(value instanceof TextView)) return;
        TextView text = (TextView) value;
        text.setTextColor(dialog.getContext().getColor(color));
        text.setTextSize(size);
        text.setIncludeFontPadding(false);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    }

    private static void styleButton(Button button, boolean primary) {
        if (button == null) return;
        button.setAllCaps(false);
        button.setTextColor(button.getContext().getColor(primary
                ? android.R.color.white : R.color.text_primary));
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextSize(14f);
        button.setMinHeight(dp(button, 44));
        button.setPadding(dp(button, 16), dp(button, 6), dp(button, 16), dp(button, 6));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
    }

    private static int dp(AlertDialog dialog, int value) {
        return Math.round(value * dialog.getContext().getResources().getDisplayMetrics().density);
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}
