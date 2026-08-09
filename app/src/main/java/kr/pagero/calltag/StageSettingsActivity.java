package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class StageSettingsActivity extends Activity {
    private static final String[] COLOR_OPTIONS = {
            "#4389FF", "#7A5AF8", "#F5A524", "#32D583", "#F97066", "#A7ABB2"
    };

    private CallTagDbHelper db;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stage_settings);
        db = new CallTagDbHelper(this);
        list = findViewById(R.id.stageSettingsList);
        findViewById(R.id.stageSettingsBack).setOnClickListener(v -> finish());
        findViewById(R.id.stageSettingsAdd).setOnClickListener(v -> showStageEditor(null, true));
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        list.removeAllViews();
        List<StageOption> stages = db.listStages();
        for (int i = 0; i < stages.size(); i++) {
            if (i == 0) list.addView(sectionLabel("기본 상태"), sectionParams());
            if (i == 3) list.addView(sectionLabel("사용자 상태"), sectionParams());

            StageOption stage = stages.get(i);
            boolean removable = i >= 3;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(12), dp(12));
            row.setBackgroundResource(R.drawable.bg_card);

            View swatch = new View(this);
            swatch.setBackground(colorShape(stage.color, false));
            row.addView(swatch, new LinearLayout.LayoutParams(dp(18), dp(18)));

            TextView title = new TextView(this);
            title.setText(stage.name);
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(16f);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setIncludeFontPadding(false);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            titleParams.leftMargin = dp(12);
            row.addView(title, titleParams);

            Button edit = actionButton("수정");
            edit.setOnClickListener(v -> showStageEditor(stage, removable));
            row.addView(edit, new LinearLayout.LayoutParams(dp(76), dp(42)));

            LinearLayout.LayoutParams rowParams = matchWrap();
            rowParams.bottomMargin = dp(8);
            list.addView(row, rowParams);
        }
    }

    private TextView sectionLabel(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(13f);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private LinearLayout.LayoutParams sectionParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(4);
        params.bottomMargin = dp(10);
        return params;
    }

    private void showStageEditor(StageOption stage, boolean removable) {
        EditText input = new EditText(this);
        input.setHint("상태 이름");
        input.setText(stage == null ? "" : stage.name);
        input.setSelection(input.getText().length());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        String[] selectedColor = {
                stage == null ? COLOR_OPTIONS[Math.min(db.listStages().size() % COLOR_OPTIONS.length,
                        COLOR_OPTIONS.length - 1)] : stage.color
        };

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);
        content.addView(input, matchWrap());

        TextView colorLabel = sectionLabel("태그 색상");
        LinearLayout.LayoutParams colorLabelParams = matchWrap();
        colorLabelParams.topMargin = dp(18);
        content.addView(colorLabel, colorLabelParams);

        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(Gravity.CENTER_VERTICAL);
        List<View> colorViews = new ArrayList<>();

        EditText customColor = new EditText(this);
        customColor.setSingleLine(true);
        customColor.setHint("#4389FF");
        customColor.setText(selectedColor[0]);
        customColor.setTextColor(getColor(R.color.text_primary));
        customColor.setHintTextColor(getColor(R.color.text_muted));
        customColor.setTextSize(14f);
        customColor.setBackgroundResource(R.drawable.bg_input);
        customColor.setPadding(dp(14), 0, dp(14), 0);

        View customPreview = new View(this);
        customPreview.setBackground(colorShape(selectedColor[0], true));

        for (String color : COLOR_OPTIONS) {
            View choice = new View(this);
            choice.setTag(color);
            choice.setClickable(true);
            choice.setFocusable(true);
            colorViews.add(choice);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
            params.setMargins(dp(3), dp(3), dp(3), dp(3));
            colorRow.addView(choice, params);
            choice.setOnClickListener(v -> {
                selectedColor[0] = (String) v.getTag();
                customColor.setText(selectedColor[0]);
                customColor.setSelection(customColor.getText().length());
                refreshColorChoices(colorViews, selectedColor[0]);
                customPreview.setBackground(colorShape(selectedColor[0], true));
            });
        }
        refreshColorChoices(colorViews, selectedColor[0]);
        content.addView(colorRow, topMargin(8));

        content.addView(sectionLabel("직접 색상"), topMargin(12));
        LinearLayout customRow = new LinearLayout(this);
        customRow.setGravity(Gravity.CENTER_VERTICAL);
        customRow.addView(customColor, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        previewParams.leftMargin = dp(8);
        customRow.addView(customPreview, previewParams);
        content.addView(customRow, topMargin(6));

        customColor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String normalized = normalizeColor(s == null ? "" : s.toString());
                if (normalized != null) {
                    selectedColor[0] = normalized;
                    customPreview.setBackground(colorShape(normalized, true));
                    refreshColorChoices(colorViews, normalized);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle(stage == null ? "사용자 상태 추가" : "상태 수정")
                .setView(content)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", null);
        if (stage != null && removable) builder.setNeutralButton("삭제", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String color = normalizeColor(customColor.getText().toString());
                if (color == null) {
                    customColor.setError("#RRGGBB 형식으로 입력해주세요.");
                    return;
                }
                try {
                    if (stage == null) {
                        db.addStage(input.getText().toString(), color);
                    } else {
                        db.updateStage(stage.id, stage.name, input.getText().toString(), color);
                    }
                    dialog.dismiss();
                    render();
                } catch (IllegalArgumentException error) {
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            if (stage != null && removable) {
                Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                delete.setTextColor(getColor(R.color.danger));
                delete.setOnClickListener(v -> {
                    dialog.dismiss();
                    confirmDelete(stage);
                });
            }
        });
        dialog.show();
    }

    private String normalizeColor(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase();
        if (!value.startsWith("#")) value = "#" + value;
        if (!value.matches("#[0-9A-F]{6}")) return null;
        try {
            Color.parseColor(value);
            return value;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void refreshColorChoices(List<View> views, String selected) {
        for (View view : views) {
            String color = (String) view.getTag();
            view.setBackground(colorShape(color, color.equalsIgnoreCase(selected)));
        }
    }

    private GradientDrawable colorShape(String colorHex, boolean selected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dp(12));
        shape.setColor(parseColor(colorHex));
        shape.setStroke(dp(selected ? 3 : 1), selected
                ? getColor(android.R.color.white) : getColor(R.color.border));
        return shape;
    }

    private int parseColor(String value) {
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException ignored) {
            return getColor(R.color.primary);
        }
    }

    private void confirmDelete(StageOption stage) {
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("사용자 상태 삭제")
                .setMessage("‘" + stage.name + "’ 상태의 고객은 첫 번째 기본 상태로 이동합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    try {
                        db.deleteStage(stage.id, stage.name);
                        render();
                    } catch (IllegalArgumentException error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(getColor(R.color.text_primary));
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinWidth(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackgroundResource(R.drawable.bg_secondary_button);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}
