package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
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

/** 이름과 본문 미리보기 중심의 compact 템플릿 목록. */
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
        header.setPadding(dp(16), dp(10), dp(16), dp(8));
        Button back = button("‹", false);
        back.setTextSize(27f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView title = title(selectMode ? "템플릿 선택" : "문자 템플릿", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);

        if (!selectMode) {
            Button add = button("+ 추가", true);
            add.setOnClickListener(v -> openEditor(null));
            header.addView(add, new LinearLayout.LayoutParams(dp(84), dp(44)));
        }
        page.addView(header, matchWrap());

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(dp(16), 0, dp(16), dp(10));
        searchInput = new EditText(this);
        searchInput.setHint("템플릿 검색");
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14f);
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
        tools.addView(searchInput, new LinearLayout.LayoutParams(0, dp(46), 1f));

        countText = body("");
        countText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(dp(52), dp(46));
        countParams.leftMargin = dp(8);
        tools.addView(countText, countParams);
        page.addView(tools, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(16), 0, dp(16), dp(32));
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
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            listContainer.addView(empty, matchWrap());
            return;
        }

        for (MessageTemplateStore.Template template : templates) {
            listContainer.addView(templateCard(template), topMargin(7));
        }
    }

    private View templateCard(MessageTemplateStore.Template template) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(13), dp(15), dp(13));
        card.setBackgroundResource(selectMode
                ? R.drawable.bg_clickable_row : R.drawable.bg_card);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = title(template.name, 16f);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (MessageTemplateStore.isDefault(this, template.id)) {
            top.addView(badge("자동문자"), badgeParams(70));
        }
        if (MessageAttachmentStore.exists(this, template.imageRef)) {
            LinearLayout.LayoutParams imageBadge = badgeParams(50);
            imageBadge.leftMargin = dp(5);
            top.addView(badge("이미지"), imageBadge);
        }
        if (selectMode) {
            TextView arrow = title("›", 23f);
            arrow.setTextColor(getColor(R.color.text_muted));
            arrow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(28), dp(36));
            arrowParams.leftMargin = dp(4);
            top.addView(arrow, arrowParams);
        }
        card.addView(top, matchWrap());

        LinearLayout previewRow = new LinearLayout(this);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView preview = body(template.body);
        preview.setTextColor(getColor(R.color.text_secondary));
        preview.setTextSize(13f);
        preview.setMaxLines(2);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        preview.setLineSpacing(0f, 1.15f);
        previewRow.addView(preview, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (MessageAttachmentStore.exists(this, template.imageRef)) {
            Bitmap bitmap = MessageAttachmentStore.preview(this, template.imageRef);
            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(52), dp(52));
            imageParams.leftMargin = dp(10);
            previewRow.addView(image, imageParams);
        }
        card.addView(previewRow, topMargin(7));

        if (selectMode) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> choose(template));
        } else {
            LinearLayout actions = new LinearLayout(this);
            Button edit = button("수정", true);
            edit.setOnClickListener(v -> openEditor(template));
            actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1f));

            Button manage = button("관리", false);
            manage.setOnClickListener(v -> showManageDialog(template));
            LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
            manageParams.leftMargin = dp(7);
            actions.addView(manage, manageParams);
            card.addView(actions, topMargin(11));
        }
        return card;
    }

    private void showManageDialog(MessageTemplateStore.Template template) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(6), dp(20), dp(6));

        Button duplicate = button("복제", false);
        Button delete = button("삭제", false);
        delete.setTextColor(getColor(R.color.danger));
        content.addView(duplicate, fixedHeight(50, 0));
        content.addView(delete, fixedHeight(50, 8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(template.name)
                .setView(content)
                .setNegativeButton("닫기", null)
                .create();
        duplicate.setOnClickListener(v -> {
            dialog.dismiss();
            duplicate(template);
        });
        delete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(template);
        });
        dialog.show();
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
        return new LinearLayout.LayoutParams(dp(width), dp(26));
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

    private LinearLayout.LayoutParams fixedHeight(int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(top);
        return params;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
