package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class TaskTypeSettingsActivity extends Activity {
    private static final String[] COLORS = {
            "#4389FF", "#7A5AF8", "#F5A524", "#32D583", "#F97066", "#A7ABB2"
    };

    private TaskTypeStore store;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_type_settings);
        store = new TaskTypeStore(this);
        list = findViewById(R.id.taskTypeSettingsList);
        findViewById(R.id.taskTypeSettingsBack).setOnClickListener(v -> finish());
        findViewById(R.id.taskTypeSettingsAdd).setOnClickListener(v -> showEditor(null));
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        list.removeAllViews();
        List<TaskTypeOption> types = store.list();
        for (int i = 0; i < types.size(); i++) {
            if (i == 0) list.addView(sectionLabel("기본 일정 종류"), sectionParams());
            if (i > 0 && !types.get(i - 1).defaultType && types.get(i).defaultType) {
                list.addView(sectionLabel("기본 일정 종류"), sectionParams());
            }
            if (i > 0 && types.get(i - 1).defaultType && !types.get(i).defaultType) {
                list.addView(sectionLabel("사용자 일정 종류"), sectionParams());
            }

            TaskTypeOption option = types.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(12), dp(12));
            row.setBackgroundResource(R.drawable.bg_card);

            View swatch = new View(this);
            swatch.setBackground(colorShape(option.color, false));
            row.addView(swatch, new LinearLayout.LayoutParams(dp(18), dp(36)));

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView title = text(option.name, 16f, R.color.text_primary, true);
            labels.addView(title, matchWrap());
            TextView subtitle = text(option.defaultType ? "기본 종류 · 이름과 색상 수정 가능" : "사용자 추가 종류",
                    12f, R.color.text_muted, false);
            labels.addView(subtitle, topMargin(4));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            labelParams.leftMargin = dp(12);
            row.addView(labels, labelParams);

            Button edit = actionButton("수정");
            edit.setOnClickListener(v -> showEditor(option));
            row.addView(edit, new LinearLayout.LayoutParams(dp(76), dp(42)));

            LinearLayout.LayoutParams rowParams = matchWrap();
            rowParams.bottomMargin = dp(8);
            list.addView(row, rowParams);
        }
    }

    private void showEditor(TaskTypeOption option) {
        EditText input = new EditText(this);
        input.setHint("일정 종류 이름");
        input.setText(option == null ? "" : option.name);
        input.setSelection(input.getText().length());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        String[] selectedColor = {option == null ? COLORS[store.list().size() % COLORS.length] : option.color};
        List<View> swatches = new ArrayList<>();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);
        content.addView(input, matchWrap());
        content.addView(sectionLabel("구분 색상"), topMargin(18));

        LinearLayout colors = new LinearLayout(this);
        colors.setOrientation(LinearLayout.HORIZONTAL);
        for (String color : COLORS) {
            View swatch = new View(this);
            swatch.setTag(color);
            swatch.setClickable(true);
            swatch.setFocusable(true);
            swatch.setOnClickListener(v -> {
                selectedColor[0] = (String) v.getTag();
                refreshSwatches(swatches, selectedColor[0]);
            });
            swatches.add(swatch);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
            params.setMargins(dp(3), dp(3), dp(3), dp(3));
            colors.addView(swatch, params);
        }
        refreshSwatches(swatches, selectedColor[0]);
        content.addView(colors, topMargin(8));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(option == null ? "일정 종류 추가" : "일정 종류 수정")
                .setView(content)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", null);
        if (option != null && !option.defaultType) builder.setNeutralButton("삭제", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    if (option == null) store.add(input.getText().toString(), selectedColor[0]);
                    else store.update(option, input.getText().toString(), selectedColor[0]);
                    dialog.dismiss();
                    render();
                } catch (IllegalArgumentException error) {
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            if (option != null && !option.defaultType) {
                Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                delete.setTextColor(getColor(R.color.danger));
                delete.setOnClickListener(v -> {
                    dialog.dismiss();
                    confirmDelete(option);
                });
            }
        });
        dialog.show();
    }

    private void confirmDelete(TaskTypeOption option) {
        new AlertDialog.Builder(this)
                .setTitle("일정 종류 삭제")
                .setMessage("기존 일정의 제목은 유지되고 종류 색상만 기본값으로 표시됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    try {
                        store.delete(option);
                        render();
                    } catch (IllegalArgumentException error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void refreshSwatches(List<View> views, String selected) {
        for (View view : views) {
            String color = (String) view.getTag();
            view.setBackground(colorShape(color, color.equalsIgnoreCase(selected)));
        }
    }

    private GradientDrawable colorShape(String raw, boolean selected) {
        int color;
        try {
            color = Color.parseColor(raw);
        } catch (IllegalArgumentException ignored) {
            color = getColor(R.color.primary);
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(12));
        shape.setStroke(dp(selected ? 3 : 1), selected ? Color.WHITE : getColor(R.color.border));
        return shape;
    }

    private TextView sectionLabel(String value) {
        return text(value, 13f, R.color.text_secondary, true);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(getColor(R.color.text_primary));
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_secondary_button);
        button.setMinWidth(0);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private LinearLayout.LayoutParams sectionParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(4);
        params.bottomMargin = dp(10);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (store != null) store.close();
        super.onDestroy();
    }
}
