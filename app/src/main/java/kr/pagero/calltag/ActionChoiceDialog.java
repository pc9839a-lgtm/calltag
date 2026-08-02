package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

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
        boolean searchable = options != null && options.size() > 8;
        int[] selectedIndex = {findSelectedIndex(options)};

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 16), dp(activity, 6), dp(activity, 16), dp(activity, 6));

        if (description != null && !description.trim().isEmpty()) {
            TextView helper = text(activity, description.trim(), 13f,
                    R.color.text_secondary, false);
            helper.setMaxLines(2);
            content.addView(helper, matchWrap());
        }

        TextView resultCount = null;
        if (searchable) {
            LinearLayout searchRow = new LinearLayout(activity);
            searchRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams searchRowParams = matchWrap();
            searchRowParams.topMargin = dp(activity, 10);
            content.addView(searchRow, searchRowParams);

            EditText search = new EditText(activity);
            search.setHint("고객명·전화번호 검색");
            search.setSingleLine(true);
            search.setTextSize(14f);
            search.setTextColor(activity.getColor(R.color.text_primary));
            search.setHintTextColor(activity.getColor(R.color.text_muted));
            search.setBackgroundResource(R.drawable.bg_input);
            search.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
            searchRow.addView(search, new LinearLayout.LayoutParams(0, dp(activity, 48), 1f));

            resultCount = text(activity, options.size() + "명", 12f,
                    R.color.text_secondary, true);
            resultCount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                    dp(activity, 54), dp(activity, 48));
            countParams.leftMargin = dp(activity, 8);
            searchRow.addView(resultCount, countParams);

            final TextView countView = resultCount;
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    int visible = filterOptions(optionListRef[0], s == null ? "" : s.toString());
                    countView.setText(visible + "명");
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        LinearLayout optionList = new LinearLayout(activity);
        optionList.setOrientation(LinearLayout.VERTICAL);
        optionListRef[0] = optionList;
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                Option option = options.get(i);
                View row = optionRow(activity, option, saveMode);
                row.setTag(R.id.customerListTab, i);
                row.setContentDescription((option.title + " " + option.subtitle)
                        .toLowerCase(Locale.KOREA));
                LinearLayout.LayoutParams rowParams = matchWrap();
                rowParams.topMargin = dp(activity, saveMode ? 6 : 7);
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

        ScrollView listScroll = new ScrollView(activity);
        listScroll.setFillViewport(false);
        listScroll.addView(optionList, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        int maxListHeight = Math.min(dp(activity, 430),
                Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.52f));
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                searchable ? maxListHeight : LinearLayout.LayoutParams.WRAP_CONTENT);
        listParams.topMargin = dp(activity, description == null || description.trim().isEmpty() ? 0 : 4);
        content.addView(listScroll, listParams);

        if (footerTitle != null && !footerTitle.trim().isEmpty()) {
            TextView footer = text(activity, footerTitle.trim(), 14f, R.color.primary, true);
            footer.setGravity(Gravity.CENTER);
            footer.setBackgroundResource(R.drawable.bg_secondary_button);
            footer.setClickable(true);
            footer.setFocusable(true);
            LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 46));
            footerParams.topMargin = dp(activity, 10);
            content.addView(footer, footerParams);
            footer.setOnClickListener(v -> {
                AlertDialog dialog = (AlertDialog) v.getTag(R.id.appTitle);
                if (dialog != null) dialog.dismiss();
                if (footerListener != null) footerListener.onClick(v);
            });
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(content);
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

    private static final LinearLayout[] optionListRef = new LinearLayout[1];

    private static int filterOptions(LinearLayout list, String rawQuery) {
        if (list == null) return 0;
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.KOREA);
        String digits = PhoneNumberNormalizer.normalize(rawQuery);
        int visible = 0;
        for (int i = 0; i < list.getChildCount(); i++) {
            View row = list.getChildAt(i);
            String searchable = String.valueOf(row.getContentDescription());
            String rowDigits = PhoneNumberNormalizer.normalize(searchable);
            boolean match = query.isEmpty()
                    || searchable.contains(query)
                    || (!digits.isEmpty() && rowDigits.contains(digits));
            row.setVisibility(match ? View.VISIBLE : View.GONE);
            if (match) visible++;
        }
        return visible;
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
            Object rawIndex = child.getTag(R.id.customerListTab);
            boolean selected = rawIndex instanceof Integer && (Integer) rawIndex == selectedIndex;
            child.setBackgroundResource(selected
                    ? R.drawable.bg_selected_row
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

    private static View optionRow(Activity activity, Option option, boolean compact) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 14), compact ? dp(activity, 9) : dp(activity, 10),
                dp(activity, 11), compact ? dp(activity, 9) : dp(activity, 10));
        row.setMinimumHeight(dp(activity, compact ? 54 : 58));
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);

        boolean showColor = !compact && !option.color.trim().isEmpty();
        if (showColor) {
            View swatch = new View(activity);
            swatch.setBackground(swatch(option.color));
            row.addView(swatch, new LinearLayout.LayoutParams(dp(activity, 10), dp(activity, 30)));
        }

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(activity, option.title, compact ? 15f : 15f,
                R.color.text_primary, true);
        title.setSingleLine(true);
        labels.addView(title, matchWrap());
        if (!option.subtitle.trim().isEmpty()) {
            String subtitleText = compact
                    ? option.subtitle.replace("현재 설정 · ", "")
                    : option.subtitle;
            TextView subtitle = text(activity, subtitleText, compact ? 12.5f : 12.5f,
                    R.color.text_secondary, false);
            subtitle.setSingleLine(true);
            LinearLayout.LayoutParams subtitleParams = matchWrap();
            subtitleParams.topMargin = dp(activity, 3);
            labels.addView(subtitle, subtitleParams);
        }
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.leftMargin = showColor ? dp(activity, 10) : 0;
        row.addView(labels, labelParams);

        TextView marker = text(activity, "", 19f, R.color.text_muted, true);
        marker.setGravity(Gravity.CENTER);
        row.addView(marker, new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 36)));
        return row;
    }

    private static GradientDrawable swatch(String rawColor) {
        int color;
        try {
            color = Color.parseColor(rawColor);
        } catch (IllegalArgumentException ignored) {
            color = Color.rgb(74, 141, 255);
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
