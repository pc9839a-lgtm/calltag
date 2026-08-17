package kr.pagero.calltag;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

/** 할 일 등록용 콜태그 전용 시간 선택 UI. 시/분을 휠로 빠르게 고른다. */
public final class TaskTimeChoiceDialog {
    public interface Listener {
        void onSelected(int hourOfDay, int minute);
    }

    private TaskTimeChoiceDialog() {}

    public static void show(Context context, Listener listener) {
        Calendar now = Calendar.getInstance();
        show(context, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE),
                "이 시간으로 등록", listener);
    }

    public static void show(Context context, int initialHourOfDay, int initialMinute,
                            String actionLabel, Listener listener) {
        int hour24 = Math.max(0, Math.min(23, initialHourOfDay));
        int rawMinute = Math.max(0, Math.min(59, initialMinute));
        // The picker only returns a time, not a date. Never round 58/59 to the next hour because
        // 23:59 -> 00:00 would silently move an edited task to the beginning of the same date.
        int minute = Math.min(55, ((rawMinute + 2) / 5) * 5);

        State state = new State();
        state.pm = hour24 >= 12;
        int hour12 = hour24 % 12;
        state.hour = hour12 == 0 ? 12 : hour12;
        state.minute = minute;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18));
        root.setBackgroundResource(R.drawable.bg_dialog_panel);

        LinearLayout heading = new LinearLayout(context);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(context, "시간 선택", 19f, true, R.color.text_primary);
        heading.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView summary = text(context, "", 17f, true, R.color.primary);
        summary.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        heading.addView(summary, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 42)));
        root.addView(heading, matchWrap());

        LinearLayout periodRow = new LinearLayout(context);
        periodRow.setOrientation(LinearLayout.HORIZONTAL);
        periodRow.setBackgroundResource(R.drawable.bg_soft_panel);
        periodRow.setPadding(dp(context, 3), dp(context, 3), dp(context, 3), dp(context, 3));
        TextView am = segment(context, "오전");
        TextView pm = segment(context, "오후");
        periodRow.addView(am, weightedHeight(context, 1f, 44));
        periodRow.addView(pm, weightedHeight(context, 1f, 44));
        root.addView(periodRow, top(context, 12));

        NumberPicker hourPicker = new NumberPicker(context);
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setValue(state.hour);
        hourPicker.setWrapSelectorWheel(true);
        hourPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        hourPicker.setFormatter(value -> String.format(Locale.KOREA, "%02d", value));
        hourPicker.setBackgroundColor(Color.TRANSPARENT);
        hourPicker.setContentDescription("시 선택");

        NumberPicker minutePicker = new NumberPicker(context);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(11);
        minutePicker.setDisplayedValues(new String[]{
                "00", "05", "10", "15", "20", "25",
                "30", "35", "40", "45", "50", "55"
        });
        minutePicker.setValue(state.minute / 5);
        minutePicker.setWrapSelectorWheel(true);
        minutePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        minutePicker.setBackgroundColor(Color.TRANSPARENT);
        minutePicker.setContentDescription("분 선택");

        LinearLayout wheelRow = new LinearLayout(context);
        wheelRow.setOrientation(LinearLayout.HORIZONTAL);
        wheelRow.setGravity(Gravity.CENTER);
        FrameLayout hourFrame = wheel(context, hourPicker);
        FrameLayout minuteFrame = wheel(context, minutePicker);
        wheelRow.addView(hourFrame, weightedHeight(context, 1f, 210));

        TextView colon = text(context, ":", 24f, true, R.color.text_primary);
        colon.setGravity(Gravity.CENTER);
        wheelRow.addView(colon, new LinearLayout.LayoutParams(dp(context, 38), dp(context, 210)));
        wheelRow.addView(minuteFrame, weightedHeight(context, 1f, 210));
        root.addView(wheelRow, top(context, 12));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = action(context, "취소", false);
        String doneLabel = actionLabel == null || actionLabel.trim().isEmpty()
                ? "확인" : actionLabel.trim();
        TextView done = action(context, "✓  " + doneLabel, true);
        actions.addView(cancel, weightedHeight(context, 0.9f, 50));
        LinearLayout.LayoutParams doneParams = weightedHeight(context, 1.55f, 50);
        doneParams.leftMargin = dp(context, 8);
        actions.addView(done, doneParams);
        root.addView(actions, top(context, 14));

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.Theme_CallTag_Dialog)
                .setView(root)
                .create();

        Runnable render = () -> {
            styleSegment(am, !state.pm);
            styleSegment(pm, state.pm);
            summary.setText((state.pm ? "오후 " : "오전 ")
                    + state.hour + ":" + String.format(Locale.KOREA, "%02d", state.minute));
        };

        am.setOnClickListener(v -> {
            state.pm = false;
            render.run();
        });
        pm.setOnClickListener(v -> {
            state.pm = true;
            render.run();
        });
        hourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            state.hour = newVal;
            render.run();
            stylePickerText(picker);
        });
        minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            state.minute = newVal * 5;
            render.run();
            stylePickerText(picker);
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        done.setOnClickListener(v -> {
            int hour = state.hour % 12 + (state.pm ? 12 : 0);
            dialog.dismiss();
            if (listener != null) listener.onSelected(hour, state.minute);
        });

        dialog.setOnShowListener(ignored -> {
            CallTagDialogStyler.apply(dialog);
            hourPicker.post(() -> stylePickerText(hourPicker));
            minutePicker.post(() -> stylePickerText(minutePicker));
        });
        render.run();
        dialog.show();
    }

    private static FrameLayout wheel(Context context, NumberPicker picker) {
        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundResource(R.drawable.bg_soft_panel);
        View selectedBand = new View(context);
        selectedBand.setBackgroundResource(R.drawable.bg_selected_row);
        FrameLayout.LayoutParams band = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(context, 48), Gravity.CENTER);
        band.leftMargin = dp(context, 4);
        band.rightMargin = dp(context, 4);
        frame.addView(selectedBand, band);
        frame.addView(picker, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return frame;
    }

    private static void stylePickerText(NumberPicker picker) {
        for (int i = 0; i < picker.getChildCount(); i++) {
            View child = picker.getChildAt(i);
            if (child instanceof EditText) {
                EditText text = (EditText) child;
                text.setTextColor(picker.getContext().getColor(R.color.primary));
                text.setTextSize(20f);
                text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                text.setGravity(Gravity.CENTER);
                text.setBackgroundColor(Color.TRANSPARENT);
                text.setCursorVisible(false);
            }
        }
        picker.invalidate();
    }

    private static void styleSegment(TextView view, boolean selected) {
        view.setBackgroundResource(selected
                ? R.drawable.bg_primary_button : android.R.color.transparent);
        view.setTextColor(view.getContext().getColor(selected
                ? android.R.color.white : R.color.text_secondary));
    }

    private static TextView segment(Context context, String value) {
        TextView view = text(context, value, 15f, true, R.color.text_secondary);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private static TextView action(Context context, String value, boolean primary) {
        TextView view = text(context, value, 14f, true,
                primary ? android.R.color.white : R.color.text_primary);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        view.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private static TextView text(Context context, String value, float size,
                                 boolean bold, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(context.getColor(color));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private static LinearLayout.LayoutParams top(Context context, int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(context, margin);
        return params;
    }

    private static LinearLayout.LayoutParams weightedHeight(Context context, float weight, int heightDp) {
        return new LinearLayout.LayoutParams(0, dp(context, heightDp), weight);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class State {
        boolean pm;
        int hour;
        int minute;
    }
}
