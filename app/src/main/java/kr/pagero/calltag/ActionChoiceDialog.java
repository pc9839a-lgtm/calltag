package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class ActionChoiceDialog {
    public interface Listener {
        void onSelected(Option option);
    }

    public static final class Option {
        public final String key;
        public final String title;
        public final String subtitle;
        public final String color;

        public Option(String key, String title) {
            this(key, title, "", "");
        }

        public Option(String key, String title, String subtitle, String color) {
            this.key = key == null ? "" : key;
            this.title = title == null ? "" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.color = color == null ? "" : color;
        }
    }

    private ActionChoiceDialog() {}

    public static void show(Activity activity,
                            String title,
                            String description,
                            List<Option> options,
                            Listener listener) {
        show(activity, title, description, options, listener, null, null);
    }

    public static void show(Activity activity,
                            String title,
                            String description,
                            List<Option> options,
                            Listener listener,
                            String footerTitle,
                            View.OnClickListener footerListener) {
        if (activity == null || activity.isFinishing()) return;

        boolean saveMode = title != null && title.trim().startsWith("연결 문자");
        int[] selectedIndex = {findSelectedIndex(options)};

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 18), dp(activity, 10), dp(activity, 18), dp(activity, 8));

        if (description != null && !description.trim().isEmpty()) {
            TextView helper = text(activity, description.trim(), 13f, R.color.text_secondary, false);
            helper.setLineSpacing(0f, 1.2f);
            content.addView(helper, matchWrap());
        }

        LinearLayout optionList = new LinearLayout(activity);
        optionList.setOrientation(LinearLayout.VERTICAL);
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                Option option = options.get(i);
                View row = optionRow(activity, option);
                row.setTag(R.id.customerListTab, i);
                LinearLayout.LayoutParams rowParams = matchWrap();
                rowParams.topMargin = dp(activity, 9);
                optionList.addView(row, rowParams);
                if (saveMode) {
                    row.setOnClickListener(v -> {
                        Object rawIndex = v.getTag(R.id.customerListTab);
                        selectedIndex[0] = rawIndex instanceof Integer ? (Integer) rawIndex : 0;
                        renderSelection(activity, optionList, selectedIndex[0]);
                    });
                } else {
                    row.setOnClickListener(v -> {
                        AlertDialog dialog = (AlertDialog) v.getTag(R.id.appTitle);
                        if (dialog != null) dialog.dismiss();
                        if (listener != null) listener.onSelected(option);
                    });
                }
            }
        }
        content.addView(optionList, matchWrap());

        if (footerTitle != null && !footerTitle.trim().isEmpty()) {
            TextView footer = text(activity, footerTitle.trim(), 14f, R.color.primary, true);
            footer.setGravity(Gravity.CENTER);
            footer.setBackgroundResource(R.drawable.bg_secondary_button);
            footer.setClickable(true);
            footer.setFocusable(true);
            LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 50));
            footerParams.topMargin = dp(activity, 12);
            content.addView(footer, footerParams);
            footer.setOnClickListener(v -> {
                AlertDialog dialog = (AlertDialog) v.getTag(R.id.appTitle);
                if (dialog != null) dialog.dismiss();
                if (footerListener != null) footerListener.onClick(v);
            });
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(scroll);
        if (saveMode) {
            builder.setNegativeButton("취소", null)
                    .setPositiveButton("저장", null);
        } else {
            builder.setNegativeButton("닫기", null);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            bindDialogTag(optionList, dialog);
            if (footerTitle != null && content.getChildCount() > 0) {
                View footer = content.getChildAt(content.getChildCount() - 1);
                footer.setTag(R.id.appTitle, dialog);
            }
            if (saveMode) {
                renderSelection(activity, optionList, selectedIndex[0]);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (options == null || options.isEmpty()) {
                        dialog.dismiss();
                        return;
                    }
                    int safeIndex = Math.max(0, Math.min(selectedIndex[0], options.size() - 1));
                    if (listener != null) listener.onSelected(options.get(safeIndex));
                    dialog.dismiss();
                });
            }
        });
        dialog.show();
    }

    private static int findSelectedIndex(List<Option> options) {
        if (options == null || options.isEmpty()) return 0;
        for (int i = 0; i < options.size(); i++) {
            String subtitle = options.get(i).subtitle;
            if (subtitle != null && subtitle.startsWith("현재 설정")) return i;
        }
        return 0;
    }

    private static void renderSelection(Activity activity, LinearLayout list, int selectedIndex) {
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            boolean selected = i == selectedIndex;
            child.setBackgroundResource(selected
                    ? R.drawable.bg_secondary_button
                    : R.drawable.bg_clickable_row);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                View indicator = row.getChildAt(row.getChildCount() - 1);
                if (indicator instanceof TextView) {
                    TextView marker = (TextView) indicator;
                    marker.setText(selected ? "✓" : "");
                    marker.setTextColor(activity.getColor(selected
                            ? R.color.primary : R.color.text_muted));
                    marker.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                }
            }
        }
    }

    private static void bindDialogTag(LinearLayout list, AlertDialog dialog) {
        for (int i = 0; i < list.getChildCount(); i++) {
            list.getChildAt(i).setTag(R.id.appTitle, dialog);
        }
    }

    private static View optionRow(Activity activity, Option option) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 16), dp(activity, 11), dp(activity, 13), dp(activity, 11));
        row.setMinimumHeight(dp(activity, 62));
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);

        if (!option.color.trim().isEmpty()) {
            View swatch = new View(activity);
            swatch.setBackground(swatch(option.color));
            row.addView(swatch, new LinearLayout.LayoutParams(dp(activity, 14), dp(activity, 36)));
        }

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(activity, option.title, 16f, R.color.text_primary, true);
        labels.addView(title, matchWrap());
        if (!option.subtitle.trim().isEmpty()) {
            TextView subtitle = text(activity, option.subtitle, 13f, R.color.text_secondary, false);
            subtitle.setMaxLines(2);
            LinearLayout.LayoutParams subtitleParams = matchWrap();
            subtitleParams.topMargin = dp(activity, 4);
            labels.addView(subtitle, subtitleParams);
        }
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.leftMargin = option.color.trim().isEmpty() ? 0 : dp(activity, 12);
        row.addView(labels, labelParams);

        TextView arrow = text(activity, "", 20f, R.color.text_muted, true);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 40)));
        return row;
    }

    private static GradientDrawable swatch(String rawColor) {
        int color;
        try {
            color = Color.parseColor(rawColor);
        } catch (IllegalArgumentException ignored) {
            color = Color.rgb(67, 137, 255);
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(999f);
        return shape;
    }

    private static TextView text(Activity activity, String value, float size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(activity.getColor(color));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
