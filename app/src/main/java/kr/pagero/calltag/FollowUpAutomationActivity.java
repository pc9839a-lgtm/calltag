package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** 1차·2차·3차 등 여러 후속문자를 각각 추가하고 켜고 끈다. */
public final class FollowUpAutomationActivity extends Activity {
    private static final int REQUEST_EDIT = 9001;
    private LinearLayout rulesContainer;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FollowUpRuleStore.ensureMigrated(this);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private ScrollView buildContent() {
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
        TextView screenTitle = title("후속문자 자동화", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(6);
        header.addView(screenTitle, titleParams);
        root.addView(header, matchWrap());

        root.addView(body("통화 후 1일·3일·7일 등 서로 다른 시점에 보낼 후속문자를 여러 개 만들 수 있습니다. "
                + "새 통화가 감지되면 이전 통화에서 예약된 후속문자는 자동 취소됩니다."), top(12));

        Button add = button("+ 후속문자 추가", true);
        add.setOnClickListener(v -> openEditor(""));
        root.addView(add, fixedTop(52, 18));

        empty = body("등록된 후속문자가 없습니다.\n위 버튼을 눌러 첫 후속문자를 추가하세요.");
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(18), dp(28), dp(18), dp(28));
        empty.setBackgroundResource(R.drawable.bg_card);
        root.addView(empty, top(12));

        rulesContainer = new LinearLayout(this);
        rulesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(rulesContainer, top(4));

        TextView note = body("후속문자는 이미지 없는 문자 템플릿만 사용할 수 있습니다. "
                + "발송 결과와 예약 상태는 고객 문자 > 발송 내역에서 확인할 수 있습니다.");
        root.addView(note, top(20));
        return scroll;
    }

    private void render() {
        if (rulesContainer == null) return;
        rulesContainer.removeAllViews();
        List<FollowUpRuleStore.Rule> rules = FollowUpRuleStore.list(this);
        empty.setVisibility(rules.isEmpty() ? View.VISIBLE : View.GONE);
        for (FollowUpRuleStore.Rule rule : rules) {
            rulesContainer.addView(ruleCard(rule), top(8));
        }
    }

    private View ruleCard(FollowUpRuleStore.Rule rule) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(title(rule.name, 16f), matchWrap());
        MessageTemplateStore.Template template = MessageTemplateStore.get(this, rule.templateId);
        texts.addView(body("통화 후 " + rule.delayDays + "일 · "
                + (template == null ? "템플릿 선택 필요" : template.name)), top(5));
        top.addView(texts, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Switch toggle = new Switch(this);
        toggle.setChecked(rule.enabled);
        toggle.setContentDescription(rule.name + " 사용");
        toggle.setOnCheckedChangeListener((button, checked) -> {
            FollowUpRuleStore.setEnabled(this, rule.id, checked);
            if (checked && checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 9002);
            }
        });
        top.addView(toggle);
        card.addView(top, matchWrap());

        LinearLayout actions = new LinearLayout(this);
        Button edit = button("편집", false);
        edit.setOnClickListener(v -> openEditor(rule.id));
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(46), 1f));
        Button delete = button("삭제", false);
        delete.setTextColor(getColor(R.color.danger));
        delete.setOnClickListener(v -> confirmDelete(rule));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        deleteParams.leftMargin = dp(8);
        actions.addView(delete, deleteParams);
        card.addView(actions, top(13));
        return card;
    }

    private void openEditor(String id) {
        Intent intent = new Intent(this, FollowUpRuleEditorActivity.class);
        if (id != null && !id.isEmpty()) {
            intent.putExtra(FollowUpRuleEditorActivity.EXTRA_RULE_ID, id);
        }
        startActivityForResult(intent, REQUEST_EDIT);
    }

    private void confirmDelete(FollowUpRuleStore.Rule rule) {
        new AlertDialog.Builder(this)
                .setTitle("후속문자 삭제")
                .setMessage("‘" + rule.name + "’ 규칙을 삭제합니다. 이미 예약된 문자는 발송 내역에서 별도로 취소할 수 있습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    FollowUpRuleStore.delete(this, rule.id);
                    Toast.makeText(this, "후속문자를 삭제했습니다.", Toast.LENGTH_SHORT).show();
                    render();
                })
                .show();
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
