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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 내부 자동문자 용도를 숨기고 카테고리·기본·이미지만 보여주는 템플릿 보관함. */
public final class MessageTemplateLibraryActivity extends Activity {
    public static final String EXTRA_SELECT_MODE = "select_mode";
    public static final String EXTRA_PURPOSE_FILTER = "purpose_filter";
    public static final String EXTRA_DEFAULT_PURPOSE = "default_purpose";
    public static final String EXTRA_TEMPLATE_ID = "template_id";
    public static final String EXTRA_TEMPLATE_BODY = "template_body";
    public static final String EXTRA_TEMPLATE_NAME = "template_name";

    private static final int REQUEST_EDITOR = 8301;

    private final Set<String> openCategories = new HashSet<>();
    private final Set<String> expandedTemplates = new HashSet<>();
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
        if (!defaultPurpose.isEmpty() && fixedPurpose.isEmpty()) fixedPurpose = defaultPurpose;
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
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(dp(60), dp(50));
        countParams.leftMargin = dp(8);
        tools.addView(countText, countParams);
        page.addView(tools, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(20), 0, dp(20), dp(40));
        scroll.addView(listContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void renderTemplates() {
        if (listContainer == null) return;
        List<MessageTemplateStore.Template> templates = MessageTemplateStore.list(
                this,
                searchInput == null ? "" : searchInput.getText().toString(),
                fixedPurpose);
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

        Map<String, List<MessageTemplateStore.Template>> grouped = new LinkedHashMap<>();
        for (MessageTemplateStore.Template template : templates) {
            String category = safe(template.category).trim();
            if (category.isEmpty()) category = "기타";
            grouped.computeIfAbsent(category, ignored -> new ArrayList<>()).add(template);
        }
        if (openCategories.isEmpty() && !grouped.isEmpty()) {
            openCategories.add(grouped.keySet().iterator().next());
        }
        boolean searching = searchInput != null && !searchInput.getText().toString().trim().isEmpty();
        for (Map.Entry<String, List<MessageTemplateStore.Template>> entry : grouped.entrySet()) {
            listContainer.addView(categoryGroup(entry.getKey(), entry.getValue(), searching), topMargin(10));
        }
    }

    private View categoryGroup(String category,
                               List<MessageTemplateStore.Template> templates,
                               boolean searching) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackgroundResource(R.drawable.bg_card);

        boolean open = searching || openCategories.contains(category);
        TextView header = title(category + "    " + templates.size() + "개    "
                + (open ? "︿" : "﹀"), 16f);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), 0, dp(16), 0);
        header.setClickable(true);
        header.setFocusable(true);
        header.setOnClickListener(v -> {
            if (openCategories.contains(category)) openCategories.remove(category);
            else openCategories.add(category);
            renderTemplates();
        });
        group.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(8), 0, dp(8), dp(8));
        body.setVisibility(open ? View.VISIBLE : View.GONE);
        for (MessageTemplateStore.Template template : templates) {
            body.addView(templateCard(template), topMargin(7));
        }
        group.addView(body, matchWrap());
        return group;
    }

    private View templateCard(MessageTemplateStore.Template template) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.bg_soft_panel);

        boolean expanded = expandedTemplates.contains(template.id);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setClickable(true);
        header.setFocusable(true);
        header.setOnClickListener(v -> {
            if (expandedTemplates.contains(template.id)) expandedTemplates.remove(template.id);
            else expandedTemplates.add(template.id);
            renderTemplates();
        });

        TextView name = title(template.name, 16f);
        header.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (MessageTemplateStore.isDefault(this, template.id)) {
            header.addView(badge("기본"), badgeParams());
        }
        if (MessageAttachmentStore.exists(this, template.imageRef)) {
            LinearLayout.LayoutParams imageBadge = badgeParams();
            imageBadge.leftMargin = dp(5);
            header.addView(badge("이미지"), imageBadge);
        }
        TextView arrow = title(expanded ? "︿" : "﹀", 18f);
        arrow.setTextColor(getColor(R.color.text_muted));
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(
                dp(34), LinearLayout.LayoutParams.WRAP_CONTENT);
        arrowParams.leftMargin = dp(6);
        header.addView(arrow, arrowParams);
        card.addView(header, matchWrap());

        TextView category = body(safe(template.category).isEmpty() ? "기타" : template.category);
        card.addView(category, topMargin(3));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setVisibility(expanded ? View.VISIBLE : View.GONE);

        TextView preview = body(template.body);
        preview.setTextColor(getColor(R.color.text_primary));
        preview.setMaxLines(6);
        preview.setPadding(dp(12), dp(10), dp(12), dp(10));
        preview.setBackgroundResource(R.drawable.bg_card);
        details.addView(preview, topMargin(10));

        if (MessageAttachmentStore.exists(this, template.imageRef)) {
            Bitmap bitmap = MessageAttachmentStore.preview(this, template.imageRef);
            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(180));
            imageParams.topMargin = dp(8);
            details.addView(image, imageParams);
        }

        if (selectMode) {
            Button use = button("이 템플릿 사용", true);
            use.setOnClickListener(v -> choose(template));
            details.addView(use, actionParams(10));
        } else {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button edit = button("수정", false);
            edit.setOnClickListener(v -> openEditor(template));
            actions.addView(edit, new LinearLayout.LayoutParams(0, dp(48), 1f));

            Button makeDefault = button(MessageTemplateStore.isDefault(this, template.id)
                    ? "기본 사용 중" : "기본 지정", false);
            makeDefault.setOnClickListener(v -> setAsDefault(template));
            LinearLayout.LayoutParams defaultParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            defaultParams.leftMargin = dp(7);
            actions.addView(makeDefault, defaultParams);

            Button more = button("더보기", false);
            more.setOnClickListener(v -> showMore(template));
            LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            moreParams.leftMargin = dp(7);
            actions.addView(more, moreParams);
            details.addView(actions, topMargin(10));
        }
        card.addView(details, matchWrap());
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
        if (requestCode == REQUEST_EDITOR && resultCode == RESULT_OK) {
            MessageTemplateCleanup.runOnce(this);
            renderTemplates();
        }
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

    private void setAsDefault(MessageTemplateStore.Template template) {
        if (!safe(template.imageRef).isEmpty()) {
            Toast.makeText(this,
                    "이미지 템플릿은 자동문자 기본값으로 지정할 수 없습니다.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!fixedPurpose.isEmpty()) {
            MessageTemplateStore.setDefault(this, fixedPurpose, template.id);
            Toast.makeText(this, "기본 템플릿으로 지정했습니다.", Toast.LENGTH_SHORT).show();
            renderTemplates();
            return;
        }

        String[] labels = {"통화 후", "부재중", "후속 예약", "모두"};
        new AlertDialog.Builder(this)
                .setTitle("기본으로 사용할 위치")
                .setItems(labels, (dialog, which) -> {
                    if (which == 0 || which == 3) {
                        MessageTemplateStore.setDefault(this,
                                MessageTemplateStore.PURPOSE_INCOMING, template.id);
                        MessageTemplateStore.setDefault(this,
                                MessageTemplateStore.PURPOSE_OUTGOING, template.id);
                    }
                    if (which == 1 || which == 3) {
                        MessageTemplateStore.setDefault(this,
                                MessageTemplateStore.PURPOSE_MISSED, template.id);
                    }
                    if (which == 2 || which == 3) {
                        MessageTemplateStore.setDefault(this,
                                MessageTemplateStore.PURPOSE_FOLLOW_UP, template.id);
                    }
                    Toast.makeText(this, "기본 템플릿으로 지정했습니다.", Toast.LENGTH_SHORT).show();
                    renderTemplates();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showMore(MessageTemplateStore.Template template) {
        String[] actions = {"복제", "삭제"};
        new AlertDialog.Builder(this)
                .setTitle(template.name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        MessageTemplateStore.Template copied =
                                MessageTemplateStore.duplicate(this, template.id);
                        if (copied != null) {
                            Toast.makeText(this, "복제했습니다.", Toast.LENGTH_SHORT).show();
                            renderTemplates();
                        }
                    } else {
                        confirmDelete(template);
                    }
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void confirmDelete(MessageTemplateStore.Template template) {
        if (MessageTemplateStore.isDefault(this, template.id)) {
            Toast.makeText(this,
                    "기본 사용 중입니다. 다른 템플릿을 기본으로 지정한 뒤 삭제해주세요.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("템플릿 삭제")
                .setMessage("‘" + template.name + "’을 삭제합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    if (MessageTemplateStore.delete(this, template.id)) {
                        expandedTemplates.remove(template.id);
                        Toast.makeText(this, "삭제했습니다.", Toast.LENGTH_SHORT).show();
                        renderTemplates();
                    }
                })
                .show();
    }

    private TextView badge(String value) {
        TextView badge = new TextView(this);
        badge.setText(value);
        badge.setTextSize(11f);
        badge.setTextColor(getColor(R.color.primary));
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.bg_card);
        return badge;
    }

    private LinearLayout.LayoutParams badgeParams() {
        return new LinearLayout.LayoutParams(dp(52), dp(28));
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
