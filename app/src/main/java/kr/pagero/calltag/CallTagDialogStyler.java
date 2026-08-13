package kr.pagero.calltag;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.Color;
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

    /** Use for irreversible delete confirmations: cancel stays neutral, delete is visibly destructive. */
    public static void applyDanger(AlertDialog dialog) {
        apply(dialog);
        if (dialog == null || !dialog.isShowing()) return;
        Button delete = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (delete != null) {
            delete.setTextColor(Color.WHITE);
            delete.setBackgroundResource(R.drawable.bg_primary_button);
            int[][] states = new int[][]{
                    new int[]{-android.R.attr.state_enabled},
                    new int[]{android.R.attr.state_pressed},
                    new int[]{}
            };
            int[] colors = new int[]{
                    Color.parseColor("#563238"),
                    Color.parseColor("#B83F4C"),
                    Color.parseColor("#D9515D")
            };
            delete.setBackgroundTintList(new ColorStateList(states, colors));
        }
        Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (cancel != null) {
            cancel.setBackgroundTintList(null);
            cancel.setBackgroundResource(R.drawable.bg_secondary_button);
            cancel.setTextColor(cancel.getContext().getColor(R.color.text_primary));
        }
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
        button.setPadding(dp(button, 12), dp(button, 6), dp(button, 12), dp(button, 6));
        button.setBackgroundTintList(null);
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
