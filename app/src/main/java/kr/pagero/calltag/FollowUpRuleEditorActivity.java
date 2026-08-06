package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

/** 후속문자 규칙 한 개의 이름·발송일·템플릿을 편집한다. */
public final class FollowUpRuleEditorActivity extends Activity {
    public static final String EXTRA_RULE_ID = "rule_id";
    private static final int REQUEST_TEMPLATE = 8901;

    private String ruleId;
    private String templateId;
    private EditText nameInput;
    private EditText daysInput;
    private Switch enabled;
    private TextView templateValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageTemplateStore.ensureDefaults(this);
        ruleId = safe(getIntent().getStringExtra(EXTRA_RULE_ID));
        FollowUpRuleStore.Rule existing = ruleId.isEmpty()
                ? null : FollowUpRuleStore.find(this, ruleId);
        if (existing == null) {
            ruleId = UUID.randomUUID().toString();
            templateId = MessageTemplateStore.defaultId(
                    this, MessageTemplateStore.PURPOSE_FOLLOW_UP);
        } else {
            templateId = existing.templateId;
        }
        setContentView(buildContent(existing));
        renderTemplate();
    }

    private ScrollView buildContent(FollowUpRuleStore.Rule existing) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = title("‹", 30f);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView screenTitle = title(existing == null ? "후속문자 추가" : "후속문자 편집", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(6);
        header.addView(screenTitle, titleParams);
        root.addView(header, matchWrap());

        root.addView(body("통화가 연결된 뒤 설정한 날짜에 자동으로 보냅니다. "
                + "여러 규칙을 만들어 1차·2차·3차 후속문자를 각각 운영할 수 있습니다."), top(12));

        root.addView(label("규칙 이름"), top(20));
        nameInput = input("예: 1차 상담 확인", InputType.TYPE_CLASS_TEXT);
        nameInput.setText(existing == null ? "후속 안내" : existing.name);
        root.addView(nameInput, fixedTop(52, 7));

        root.addView(label("통화 후 며칠 뒤"), top(16));
        daysInput = input("1~30", InputType.TYPE_CLASS_NUMBER);
        daysInput.setText(String.valueOf(existing == null ? 3 : existing.delayDays));
        root.addView(daysInput, fixedTop(52, 7));

        root.addView(label("문자 템플릿"), top(16));
        LinearLayout templateCard = card();
        templateCard.setClickable(true);
        templateCard.setFocusable(true);
        templateCard.setOnClickListener(v -> chooseTemplate());
        templateValue = title("", 16f);
        templateCard.addView(templateValue, matchWrap());
        TextView templateHint = body("눌러서 후속문자 템플릿 선택");
        templateHint.setTextColor(getColor(R.color.primary));
        templateCard.addView(templateHint, top(6));
        root.addView(templateCard, top(7));

        enabled = new Switch(this);
        enabled.setText("이 후속문자 사용");
        enabled.setTextColor(getColor(R.color.text_primary));
        enabled.setTextSize(15f);
        enabled.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        enabled.setChecked(existing == null || existing.enabled);
        enabled.setPadding(dp(16), dp(10), dp(12), dp(10));
        enabled.setBackgroundResource(R.drawable.bg_card);
        root.addView(enabled, top(14));

        Button save = button("저장", true);
        save.setOnClickListener(v -> save());
        root.addView(save, fixedTop(52, 22));
        return scroll;
    }

    private void chooseTemplate() {
        startActivityForResult(new Intent(this, MessageTemplateLibraryActivity.class)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_SELECT_MODE, true)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_PURPOSE_FILTER,
                        MessageTemplateStore.PURPOSE_FOLLOW_UP), REQUEST_TEMPLATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TEMPLATE || resultCode != RESULT_OK || data == null) return;
        String selected = safe(data.getStringExtra(MessageTemplateLibraryActivity.EXTRA_TEMPLATE_ID));
        MessageTemplateStore.Template template = MessageTemplateStore.get(this, selected);
        if (template == null) return;
        if (!safe(template.imageRef).isEmpty()) {
            Toast.makeText(this, "후속 자동문자는 이미지 없는 템플릿만 사용할 수 있습니다.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        templateId = selected;
        renderTemplate();
    }

    private void renderTemplate() {
        if (templateValue == null) return;
        MessageTemplateStore.Template template = MessageTemplateStore.get(this, templateId);
        templateValue.setText(template == null ? "템플릿 선택 필요" : template.name);
    }

    private void save() {
        String name = nameInput.getText().toString().trim();
        int days;
        try {
            days = Integer.parseInt(daysInput.getText().toString().trim());
        } catch (NumberFormatException error) {
            days = -1;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "후속문자 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (days < 1 || days > 30) {
            Toast.makeText(this, "발송일은 1~30일 사이로 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        MessageTemplateStore.Template template = MessageTemplateStore.get(this, templateId);
        if (template == null || !safe(template.imageRef).isEmpty()) {
            Toast.makeText(this, "후속문자 템플릿을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        FollowUpRuleStore.save(this, new FollowUpRuleStore.Rule(
                ruleId, name, enabled.isChecked(), days, templateId));
        Toast.makeText(this, "후속문자 규칙을 저장했습니다.", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private LinearLayout card() {
        LinearLayout value = new LinearLayout(this);
        value.setOrientation(LinearLayout.VERTICAL);
        value.setPadding(dp(16), dp(15), dp(16), dp(15));
        value.setBackgroundResource(R.drawable.bg_card);
        return value;
    }

    private EditText input(String hint, int type) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setInputType(type);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextSize(15f);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), 0, dp(14), 0);
        return input;
    }

    private TextView label(String value) {
        TextView view = body(value);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView title(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView body(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(14f);
        view.setTextColor(getColor(R.color.text_secondary));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.2f);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
