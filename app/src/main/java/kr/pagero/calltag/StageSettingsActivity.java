package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class StageSettingsActivity extends Activity {
    private CallTagDbHelper db;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stage_settings);
        db = new CallTagDbHelper(this);
        list = findViewById(R.id.stageSettingsList);
        findViewById(R.id.stageSettingsBack).setOnClickListener(v -> finish());
        findViewById(R.id.stageSettingsAdd).setOnClickListener(v -> showStageEditor(null));
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
            StageOption stage = stages.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            card.setBackgroundResource(R.drawable.bg_card);

            TextView title = new TextView(this);
            title.setText((i + 1) + ".  " + stage.name);
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(17f);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setIncludeFontPadding(false);
            card.addView(title, matchWrap());

            LinearLayout controls = new LinearLayout(this);
            controls.setOrientation(LinearLayout.HORIZONTAL);
            controls.setGravity(Gravity.CENTER_VERTICAL);

            Button up = actionButton("위로", false);
            up.setEnabled(i > 0);
            up.setAlpha(i > 0 ? 1f : 0.35f);
            up.setOnClickListener(v -> {
                db.moveStage(stage.id, -1);
                render();
            });
            controls.addView(up, weightedButton());

            Button down = actionButton("아래로", false);
            down.setEnabled(i < stages.size() - 1);
            down.setAlpha(i < stages.size() - 1 ? 1f : 0.35f);
            down.setOnClickListener(v -> {
                db.moveStage(stage.id, 1);
                render();
            });
            LinearLayout.LayoutParams downParams = weightedButton();
            downParams.leftMargin = dp(7);
            controls.addView(down, downParams);

            Button edit = actionButton("이름 변경", false);
            edit.setOnClickListener(v -> showStageEditor(stage));
            LinearLayout.LayoutParams editParams = weightedButton();
            editParams.leftMargin = dp(7);
            controls.addView(edit, editParams);

            Button delete = actionButton("삭제", false);
            delete.setTextColor(getColor(R.color.danger));
            delete.setOnClickListener(v -> confirmDelete(stage));
            LinearLayout.LayoutParams deleteParams = weightedButton();
            deleteParams.leftMargin = dp(7);
            controls.addView(delete, deleteParams);

            LinearLayout.LayoutParams controlsParams = matchWrap();
            controlsParams.topMargin = dp(14);
            card.addView(controls, controlsParams);

            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.bottomMargin = dp(10);
            list.addView(card, cardParams);
        }
    }

    private void showStageEditor(StageOption stage) {
        EditText input = new EditText(this);
        input.setHint("예: 1차 상담 완료");
        input.setText(stage == null ? "" : stage.name);
        input.setSelection(input.getText().length());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        wrap.addView(input, matchWrap());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(stage == null ? "영업 단계 추가" : "단계 이름 변경")
                .setView(wrap)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        if (stage == null) db.addStage(input.getText().toString());
                        else db.renameStage(stage.id, stage.name, input.getText().toString());
                        dialog.dismiss();
                        render();
                    } catch (IllegalArgumentException error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }));
        dialog.show();
    }

    private void confirmDelete(StageOption stage) {
        new AlertDialog.Builder(this)
                .setTitle("영업 단계 삭제")
                .setMessage("‘" + stage.name + "’ 단계의 고객은 남아 있는 첫 번째 단계로 이동합니다.")
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

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(getColor(R.color.text_primary));
        button.setTextSize(12f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams weightedButton() {
        return new LinearLayout.LayoutParams(0, dp(42), 1f);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
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
