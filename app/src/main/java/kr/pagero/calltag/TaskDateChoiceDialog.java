package kr.pagero.calltag;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** CallTag 전용 날짜 선택 UI. OEM 날짜 선택기를 사용하지 않는다. */
public final class TaskDateChoiceDialog {
    public interface Listener {
        void onSelected(int year, int month, int dayOfMonth);
    }

    private TaskDateChoiceDialog() {}

    public static void show(Context context, Calendar initial, String actionLabel, Listener listener) {
        show(context, initial, null, actionLabel, listener);
    }

    public static void show(Context context, Calendar initial, Calendar maximumDate,
                            String actionLabel, Listener listener) {
        Calendar safeInitial = initial == null ? Calendar.getInstance() : (Calendar) initial.clone();
        clearTime(safeInitial);
        Calendar safeMaximum = maximumDate == null ? null : (Calendar) maximumDate.clone();
        if (safeMaximum != null) clearTime(safeMaximum);
        if (safeMaximum != null && safeInitial.after(safeMaximum)) {
            safeInitial.setTimeInMillis(safeMaximum.getTimeInMillis());
        }

        State state = new State();
        state.selected = (Calendar) safeInitial.clone();
        state.visibleMonth = (Calendar) safeInitial.clone();
        state.visibleMonth.set(Calendar.DAY_OF_MONTH, 1);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 16));
        root.setBackgroundResource(R.drawable.bg_dialog_panel);

        TextView title = text(context, "날짜 선택", 19f, true, R.color.text_primary);
        root.addView(title, matchWrap());
        root.addView(text(context, "월을 이동한 뒤 날짜를 선택하세요.",
                12.5f, false, R.color.text_secondary), top(context, 6));

        TextView summary = text(context, "", 18f, true, R.color.primary);
        summary.setGravity(Gravity.CENTER);
        summary.setBackgroundResource(R.drawable.bg_soft_panel);
        summary.setMinHeight(dp(context, 54));
        summary.setPadding(dp(context, 8), dp(context, 10), dp(context, 8), dp(context, 10));
        root.addView(summary, top(context, 14));

        LinearLayout monthHeader = new LinearLayout(context);
        monthHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView prev = action(context, "‹", false);
        prev.setTextSize(25f);
        TextView monthTitle = text(context, "", 17f, true, R.color.text_primary);
        monthTitle.setGravity(Gravity.CENTER);
        TextView next = action(context, "›", false);
        next.setTextSize(25f);
        monthHeader.addView(prev, new LinearLayout.LayoutParams(dp(context, 48), wrap()));
        monthHeader.addView(monthTitle, new LinearLayout.LayoutParams(0, wrap(), 1f));
        monthHeader.addView(next, new LinearLayout.LayoutParams(dp(context, 48), wrap()));
        root.addView(monthHeader, top(context, 14));

        LinearLayout weekdayRow = new LinearLayout(context);
        String[] weekdays = {"일", "월", "화", "수", "목", "금", "토"};
        for (int index = 0; index < weekdays.length; index++) {
            TextView day = text(context, weekdays[index], 12f, true,
                    index == 0 ? R.color.danger : index == 6 ? R.color.primary : R.color.text_secondary);
            day.setGravity(Gravity.CENTER);
            day.setMinHeight(dp(context, 30));
            weekdayRow.addView(day, new LinearLayout.LayoutParams(0, wrap(), 1f));
        }
        root.addView(weekdayRow, top(context, 8));

        LinearLayout calendarRows = new LinearLayout(context);
        calendarRows.setOrientation(LinearLayout.VERTICAL);
        root.addView(calendarRows, top(context, 2));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = action(context, "취소", false);
        String doneLabel = actionLabel == null || actionLabel.trim().isEmpty()
                ? "이 날짜로 선택" : actionLabel.trim();
        TextView done = action(context, doneLabel, true);
        actions.addView(cancel, weighted(context, 0.8f));
        LinearLayout.LayoutParams doneParams = weighted(context, 1.4f);
        doneParams.leftMargin = dp(context, 8);
        actions.addView(done, doneParams);
        root.addView(actions, top(context, 16));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(root, matchWrap());

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.Theme_CallTag_Dialog)
                .setView(scroll)
                .create();

        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            monthTitle.setText(new SimpleDateFormat("yyyy년 M월", Locale.KOREA)
                    .format(state.visibleMonth.getTime()));
            summary.setText(new SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREA)
                    .format(state.selected.getTime()));
            calendarRows.removeAllViews();

            Calendar monthStart = (Calendar) state.visibleMonth.clone();
            monthStart.set(Calendar.DAY_OF_MONTH, 1);
            int offset = monthStart.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
            int maximum = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar today = Calendar.getInstance();
            clearTime(today);

            for (int week = 0; week < 6; week++) {
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                for (int column = 0; column < 7; column++) {
                    int dayNumber = week * 7 + column - offset + 1;
                    if (dayNumber < 1 || dayNumber > maximum) {
                        row.addView(new View(context), new LinearLayout.LayoutParams(0, dp(context, 42), 1f));
                        continue;
                    }

                    Calendar date = (Calendar) monthStart.clone();
                    date.set(Calendar.DAY_OF_MONTH, dayNumber);
                    clearTime(date);
                    boolean selected = sameDay(date, state.selected);
                    boolean isToday = sameDay(date, today);
                    boolean disabled = safeMaximum != null && date.after(safeMaximum);

                    TextView day = choice(context, String.valueOf(dayNumber));
                    day.setTextColor(context.getColor(disabled
                            ? R.color.text_muted
                            : selected ? android.R.color.white
                            : column == 0 ? R.color.danger
                            : column == 6 ? R.color.primary : R.color.text_primary));
                    day.setAlpha(disabled ? 0.45f : 1f);
                    day.setBackgroundResource(selected
                            ? R.drawable.bg_primary_button
                            : isToday ? R.drawable.bg_selected_row : R.drawable.bg_secondary_button);
                    if (!disabled) {
                        day.setOnClickListener(v -> {
                            state.selected.setTimeInMillis(date.getTimeInMillis());
                            render[0].run();
                        });
                    }
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, wrap(), 1f);
                    if (column > 0) params.leftMargin = dp(context, 4);
                    row.addView(day, params);
                }
                LinearLayout.LayoutParams rowParams = matchWrap();
                if (week > 0) rowParams.topMargin = dp(context, 4);
                calendarRows.addView(row, rowParams);
            }
        };

        prev.setOnClickListener(v -> {
            state.visibleMonth.add(Calendar.MONTH, -1);
            state.visibleMonth.set(Calendar.DAY_OF_MONTH, 1);
            render[0].run();
        });
        next.setOnClickListener(v -> {
            state.visibleMonth.add(Calendar.MONTH, 1);
            state.visibleMonth.set(Calendar.DAY_OF_MONTH, 1);
            render[0].run();
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        done.setOnClickListener(v -> {
            Calendar selected = (Calendar) state.selected.clone();
            dialog.dismiss();
            if (listener != null) {
                listener.onSelected(selected.get(Calendar.YEAR), selected.get(Calendar.MONTH),
                        selected.get(Calendar.DAY_OF_MONTH));
            }
        });

        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        render[0].run();
        dialog.show();
    }

    private static TextView choice(Context context, String value) {
        TextView view = text(context, value, 13f, true, R.color.text_primary);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(context, 42));
        view.setPadding(dp(context, 2), dp(context, 8), dp(context, 2), dp(context, 8));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private static TextView action(Context context, String value, boolean primary) {
        TextView view = text(context, value, 14f, true,
                primary ? android.R.color.white : R.color.text_primary);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(context, 46));
        view.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        view.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private static TextView text(Context context, String value, float size,
                                 boolean bold, int color) {
        TextView view = new TextView(context);
        view.setText(value == null ? "" : value);
        view.setTextSize(size);
        view.setTextColor(context.getColor(color));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, wrap());
    }

    private static LinearLayout.LayoutParams top(Context context, int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(context, margin);
        return params;
    }

    private static LinearLayout.LayoutParams weighted(Context context, float weight) {
        return new LinearLayout.LayoutParams(0, wrap(), weight);
    }

    private static int wrap() {
        return LinearLayout.LayoutParams.WRAP_CONTENT;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private static boolean sameDay(Calendar left, Calendar right) {
        return left.get(Calendar.ERA) == right.get(Calendar.ERA)
                && left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    private static final class State {
        Calendar selected;
        Calendar visibleMonth;
    }
}
