package kr.pagero.calltag;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** 할 일 등록용 콜태그 전용 시간 선택 UI. 시스템 TimePicker를 사용하지 않는다. */
public final class TaskTimeChoiceDialog {
    public interface Listener {
        void onSelected(int hourOfDay, int minute);
    }

    private TaskTimeChoiceDialog() {}

    public static void show(Context context, Listener listener) {
        Calendar now = Calendar.getInstance();
        int hour24 = now.get(Calendar.HOUR_OF_DAY);
        int minute = ((now.get(Calendar.MINUTE) + 9) / 10) * 10;
        if (minute >= 60) {
            minute = 0;
            hour24 = (hour24 + 1) % 24;
        }

        State state = new State();
        state.pm = hour24 >= 12;
        int hour12 = hour24 % 12;
        state.hour = hour12 == 0 ? 12 : hour12;
        state.minute = minute;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 16));
        root.setBackgroundResource(R.drawable.bg_dialog_panel);

        TextView title = text(context, "시간 선택", 19f, true, R.color.text_primary);
        root.addView(title, matchWrap());
        TextView helper = text(context, "오전·오후와 시간을 바로 눌러 선택하세요.",
                12.5f, false, R.color.text_secondary);
        root.addView(helper, top(context, 6));

        TextView summary = text(context, "", 22f, true, R.color.primary);
        summary.setGravity(Gravity.CENTER);
        summary.setBackgroundResource(R.drawable.bg_soft_panel);
        root.addView(summary, fixedTop(context, 58, 14));

        LinearLayout periodRow = new LinearLayout(context);
        periodRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView am = choice(context, "오전");
        TextView pm = choice(context, "오후");
        periodRow.addView(am, weighted(context, 1f));
        LinearLayout.LayoutParams pmParams = weighted(context, 1f);
        pmParams.leftMargin = dp(context, 8);
        periodRow.addView(pm, pmParams);
        root.addView(label(context, "오전 / 오후"), top(context, 16));
        root.addView(periodRow, fixedTop(context, 46, 7));

        root.addView(label(context, "시"), top(context, 16));
        LinearLayout hourRows = new LinearLayout(context);
        hourRows.setOrientation(LinearLayout.VERTICAL);
        List<TextView> hourButtons = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            LinearLayout line = new LinearLayout(context);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 4; col++) {
                int hour = row * 4 + col + 1;
                TextView button = choice(context, String.valueOf(hour));
                button.setTag(hour);
                hourButtons.add(button);
                LinearLayout.LayoutParams p = weighted(context, 1f);
                if (col > 0) p.leftMargin = dp(context, 6);
                line.addView(button, p);
            }
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 42));
            if (row > 0) lineParams.topMargin = dp(context, 6);
            hourRows.addView(line, lineParams);
        }
        root.addView(hourRows, top(context, 7));

        root.addView(label(context, "분"), top(context, 16));
        LinearLayout minuteRows = new LinearLayout(context);
        minuteRows.setOrientation(LinearLayout.VERTICAL);
        List<TextView> minuteButtons = new ArrayList<>();
        int[] minutes = {0, 10, 20, 30, 40, 50};
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(context);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 3; col++) {
                int value = minutes[row * 3 + col];
                TextView button = choice(context, String.format("%02d", value));
                button.setTag(value);
                minuteButtons.add(button);
                LinearLayout.LayoutParams p = weighted(context, 1f);
                if (col > 0) p.leftMargin = dp(context, 6);
                line.addView(button, p);
            }
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 42));
            if (row > 0) lineParams.topMargin = dp(context, 6);
            minuteRows.addView(line, lineParams);
        }
        root.addView(minuteRows, top(context, 7));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = action(context, "취소", false);
        TextView done = action(context, "이 시간으로 등록", true);
        actions.addView(cancel, weighted(context, 0.8f));
        LinearLayout.LayoutParams doneParams = weighted(context, 1.4f);
        doneParams.leftMargin = dp(context, 8);
        actions.addView(done, doneParams);
        root.addView(actions, fixedTop(context, 50, 18));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(root)
                .create();

        Runnable render = () -> {
            styleChoice(am, !state.pm);
            styleChoice(pm, state.pm);
            for (TextView button : hourButtons) {
                styleChoice(button, ((Integer) button.getTag()) == state.hour);
            }
            for (TextView button : minuteButtons) {
                styleChoice(button, ((Integer) button.getTag()) == state.minute);
            }
            summary.setText((state.pm ? "오후 " : "오전 ")
                    + state.hour + ":" + String.format("%02d", state.minute));
        };

        am.setOnClickListener(v -> {
            state.pm = false;
            render.run();
        });
        pm.setOnClickListener(v -> {
            state.pm = true;
            render.run();
        });
        for (TextView button : hourButtons) {
            button.setOnClickListener(v -> {
                state.hour = (Integer) v.getTag();
                render.run();
            });
        }
        for (TextView button : minuteButtons) {
            button.setOnClickListener(v -> {
                state.minute = (Integer) v.getTag();
                render.run();
            });
        }
        cancel.setOnClickListener(v -> dialog.dismiss());
        done.setOnClickListener(v -> {
            int hour = state.hour % 12 + (state.pm ? 12 : 0);
            dialog.dismiss();
            if (listener != null) listener.onSelected(hour, state.minute);
        });

        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        render.run();
        dialog.show();
    }

    private static void styleChoice(TextView view, boolean selected) {
        view.setBackgroundResource(selected
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        view.setTextColor(view.getContext().getColor(selected
                ? android.R.color.white : R.color.text_primary));
    }

    private static TextView choice(Context context, String value) {
        TextView view = text(context, value, 14f, true, R.color.text_primary);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(R.drawable.bg_secondary_button);
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
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private static TextView label(Context context, String value) {
        return text(context, value, 12.5f, true, R.color.text_secondary);
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

    private static LinearLayout.LayoutParams fixedTop(Context context, int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, height));
        params.topMargin = dp(context, margin);
        return params;
    }

    private static LinearLayout.LayoutParams weighted(Context context, float weight) {
        return new LinearLayout.LayoutParams(0, dp(context, 46), weight);
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
