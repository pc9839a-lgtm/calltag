package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** 토글 없이 이름·본문·이미지를 한 화면에서 바로 확인하는 템플릿 목록. */
public final class MessageTemplateLibraryActivity extends Activity {
    public static final String EXTRA_SELECT_MODE = "select_mode";
    public static final String EXTRA_PURPOSE_FILTER = "purpose_filter";
    public static final String EXTRA_DEFAULT_PURPOSE = "default_purpose";
    public static final String EXTRA_TEMPLATE_ID = "template_id";
    public static final String EXTRA_TEMPLATE_BODY = "template_body";
    public static final String EXTRA_TEMPLATE_NAME = "template_name";

    private static final int REQUEST_EDITOR = 8301;

    private EditText searchInput;
    private LinearLayout listContainer;
    private TextView countText;
    private boolean selectMode;
    private String fixedPurpose = "";
    private String defaultPurpose = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageTemplateStore.ensureDefaults(this);
        MessageTemplateCleanup.runOnce(this);
        selectMode = getIntent().getBooleanExtra(EXTRA_SELECT_MODE, false);
        fixedPurpose = safe(getIntent().getStringExtra(EXTRA_PURPOSE_FILTER));
        defaultPurpose = safe(getIntent().getStringExtra(EXTRA_DEFAULT_PURPOSE));
        setContentView(buildContent());
        renderTemplates();
    }

    private View buildContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(getColor(R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(16), dp(20), dp(12));
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView title = title(selectMode ? "템플릿 선택" : "문자 템플릿", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);

        if (!selectMode) {
            Button add = button("+ 추가", true);
            add.setOnClickListener(v -> openEditor(null));
            header.addView(add, new LinearLayout.LayoutParams(dp(96), dp(48)));
        }
        page.addView(header, matchWrap());

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(dp(20), 0, dp(20), dp(12));
        searchInput = new EditText(this);
        searchInput.setHint("템플릿 검색");
        searchInput.setSingleLine(true);
        searchInput.setTextSize(15f);
        searchInput.setTextColor(getColor(R.color.text_primary));
        searchInput.setHintTextColor(getColor(R.color.text_muted));
        searchInput.setBackgroundResource(R.drawable.bg_input);
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderTemplates();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        tools.addView(searchInput, new LinearLayout.LayoutParams(0, dp(50), 1f));

        countText = body("");
        countText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(dp(62), dp(50));
        countParams.leftMargin = dp(8);
        tools.addView(countText, countParams);
        page.addView(tools, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(20), 0, dp(20), dp(40));
        scroll.addView(listContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void renderTemplates() {
        if (listContainer == null) return;
        List<MessageTemplateStore.Template> templates = MessageTemplateStore.list(
                this,
                searchInput == null ? "" : searchInput.getText().toString(),
                "");
        countText.setText(templates.size() + "개");
        listContainer.removeAllViews();

        if (templates.isEmpty()) {
            TextView empty = body("템플릿이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setBackgroundResource(R.drawable.bg_card);
            empty.setPadding(dp(18), dp(34), dp(18), dp(34));
            listContainer.addView(empty, matchWrap());
            return;
        }

        for (MessageTemplateStore.Template template : templates) {
            listContainer.addView(templateCard(template), topMargin(10));
        }
    }

    private View templateCard(MessageTemplateStore.Template template) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = title(template.name, 17f);
        top.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (MessageTemplateStore.isDefault(this, template.id)) {
            top.addView(badge("자동문자 사용 중"), badgeParams(104));
        }
        if (MessageAttachmentStore.exists(this, template.imageRef)) {
            LinearLayout.LayoutParams imageBadge = badgeParams(56);
            imageBadge.leftMargin = dp(6);
            top.addView(badge("이미지"), imageBadge);
        }
        card.addView(top, matchWrap());

        TextView preview = body(template.body);
        preview.setTextColor(getColor(R.color.text_primary));
        preview.setTextSize(14f);
        preview.setMaxLines(4);
        preview.setLineSpacing(0f, 1.25f);
        preview.setPadding(0, dp(10), 0, 0);
        card.addView(preview, matchWrap());

        if (MessageAttachmentStore.exists(this, template.imageRef)) {
            Bitmap bitmap = MessageAttachmentStore.preview(this, template.imageRef);
            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(150));
            imageParams.topMargin = dp(10);
            card.addView(image, imageParams);
        }

        if (selectMode) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> choose(template));
            Button choose = button("이 템플릿 선택", true);
            choose.setOnClickListener(v -> choose(template));
            card.addView(choose, actionParams(14));
        } else {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            Button edit = button("수정", true);
            edit.setOnClickListener(v -> openEditor(template));
            actions.addView(edit, new LinearLayout.LayoutParams(0, dp(48), 1f));

            Button duplicate = button("복제", false);
            duplicate.setOnClickListener(v -> duplicate(template));
            LinearLayout.LayoutParams duplicateParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            duplicateParams.leftMargin = dp(7);
            actions.addView(duplicate, duplicateParams);

            Button delete = button("삭제", false);
            delete.setTextColor(getColor(R.color.danger));
            delete.setOnClickListener(v -> confirmDelete(template));
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            deleteParams.leftMargin = dp(7);
            actions.addView(delete, deleteParams);
            card.addView(actions, topMargin(14));
        }
        return card;
    }

    private void openEditor(MessageTemplateStore.Template template) {
        Intent intent = new Intent(this, MessageTemplateEditorActivity.class)
                .putExtra(MessageTemplateEditorActivity.EXTRA_FIXED_PURPOSE, fixedPurpose);
        if (template != null) {
            intent.putExtra(MessageTemplateEditorActivity.EXTRA_TEMPLATE_ID, template.id);
        }
        startActivityForResult(intent, REQUEST_EDITOR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDITOR && resultCode == RESULT_OK) renderTemplates();
    }

    private void choose(MessageTemplateStore.Template template) {
        if (!defaultPurpose.isEmpty() && safe(template.imageRef).isEmpty()) {
            MessageTemplateStore.setDefault(this, defaultPurpose, template.id);
        }
        MessageTemplateStore.markUsed(this, template.id);
        setResult(RESULT_OK, new Intent()
                .putExtra(EXTRA_TEMPLATE_ID, template.id)
                .putExtra(EXTRA_TEMPLATE_NAME, template.name)
                .putExtra(EXTRA_TEMPLATE_BODY, template.body));
        finish();
    }

    private void duplicate(MessageTemplateStore.Template template) {
        MessageTemplateStore.Template copied = MessageTemplateStore.duplicate(this, template.id);
        if (copied != null) {
            Toast.makeText(this, "복제했습니다.", Toast.LENGTH_SHORT).show();
            renderTemplates();
        }
    }

    private void confirmDelete(MessageTemplateStore.Template template) {
        if (MessageTemplateStore.isDefault(this, template.id)) {
            Toast.makeText(this,
                    "자동문자에서 사용 중입니다. 자동문자 템플릿을 먼저 변경해주세요.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("템플릿 삭제")
                .setMessage("‘" + template.name + "’을 삭제합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    if (MessageTemplateStore.delete(this, template.id)) {
                        Toast.makeText(this, "삭제했습니다.", Toast.LENGTH_SHORT).show();
                        renderTemplates();
                    }
                })
                .show();
    }

    private TextView badge(String value) {
        TextView badge = new TextView(this);
        badge.setText(value);
        badge.setTextSize(10f);
        badge.setTextColor(getColor(R.color.primary));
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.bg_soft_panel);
        return badge;
    }

    private LinearLayout.LayoutParams badgeParams(int width) {
        return new LinearLayout.LayoutParams(dp(width), dp(28));
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        button.setMinWidth(0);
        return button;
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(13f);
        text.setIncludeFontPadding(false);
        return text;
    }

    private LinearLayout.LayoutParams actionParams(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        params.topMargin = dp(top);
        return params;
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
