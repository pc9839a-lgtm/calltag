package kr.pagero.calltag;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/** Applies CallTag's dark card language to framework dialogs that otherwise inherit OEM styling. */
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
            params.dimAmount = 0.72f;
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

        if (dialog.getListView() != null) {
            dialog.getListView().setBackgroundColor(dialog.getContext().getColor(R.color.surface_soft));
            dialog.getListView().setDividerHeight(dp(dialog, 4));
        }
    }

    /** 삭제 팝업은 OEM 테마 tint와 무관하게 회색 취소 / 빨간 삭제로 고정한다. */
    public static void applyDanger(AlertDialog dialog) {
        apply(dialog);
        if (dialog == null || !dialog.isShowing()) return;

        Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (cancel != null) {
            styleButton(cancel, false);
            cancel.setTextColor(cancel.getContext().getColor(R.color.text_primary));
            cancel.setBackground(rounded(cancel, Color.parseColor("#282B31"),
                    Color.parseColor("#3B3F47")));
            cancel.setMinWidth(dp(cancel, 96));
        }

        Button delete = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (delete != null) {
            styleButton(delete, true);
            delete.setTextColor(Color.WHITE);
            delete.setBackground(rounded(delete, Color.parseColor("#D9515D"),
                    Color.parseColor("#E46973")));
            delete.setMinWidth(dp(delete, 96));
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
