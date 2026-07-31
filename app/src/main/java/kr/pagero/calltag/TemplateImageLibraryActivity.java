package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class TemplateImageLibraryActivity extends Activity {
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageTemplateStore.ensureDefaults(this);
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
        root.setPadding(dp(20), dp(18), dp(20), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("템플릿 이미지", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        TextView guide = body("이미지를 붙일 템플릿을 선택하세요. 자동발송 기본 템플릿은 텍스트 전용으로 유지됩니다.");
        guide.setBackgroundResource(R.drawable.bg_soft_panel);
        guide.setPadding(dp(14), dp(12), dp(14), dp(12));
        root.addView(guide, topMargin(14));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, topMargin(14));
        return scroll;
    }

    private void render() {
        if (list == null) return;
        list.removeAllViews();
        List<MessageTemplateStore.Template> templates = MessageTemplateStore.list(this, "", "");
        for (MessageTemplateStore.Template template : templates) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackgroundResource(R.drawable.bg_card);

            card.addView(title(template.name, 16f), matchWrap());
            String meta = MessageTemplateStore.purposeLabel(template.purpose)
                    + " · " + template.category;
            if (MessageTemplateStore.isDefault(this, template.id)) {
                meta += " · 자동 기본 · 이미지 첨부 불가";
            } else if (MessageAttachmentStore.exists(this, template.imageRef)) {
                meta += " · 이미지 1장 · "
                        + MessageAttachmentStore.sizeLabel(this, template.imageRef);
            } else {
                meta += " · 이미지 없음";
            }
            card.addView(body(meta), topMargin(5));

            Button manage = button(MessageAttachmentStore.exists(this, template.imageRef)
                    ? "이미지 확인·교체" : "이미지 추가", false);
            manage.setEnabled(!MessageTemplateStore.isDefault(this, template.id));
            manage.setAlpha(manage.isEnabled() ? 1f : 0.45f);
            manage.setOnClickListener(v -> startActivity(
                    new Intent(this, TemplateImageActivity.class)
                            .putExtra(TemplateImageActivity.EXTRA_TEMPLATE_ID, template.id)));
            LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
            manageParams.topMargin = dp(10);
            card.addView(manage, manageParams);

            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.bottomMargin = dp(10);
            list.addView(card, cardParams);
        }
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(13f);
        text.setLineSpacing(dp(3), 1f);
        return text;
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
}
