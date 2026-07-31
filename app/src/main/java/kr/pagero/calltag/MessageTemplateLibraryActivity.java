package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class MessageTemplateLibraryActivity extends Activity {
    public static final String EXTRA_SELECT_MODE = "select_mode";
    public static final String EXTRA_PURPOSE_FILTER = "purpose_filter";
    public static final String EXTRA_DEFAULT_PURPOSE = "default_purpose";
    public static final String EXTRA_TEMPLATE_ID = "template_id";
    public static final String EXTRA_TEMPLATE_BODY = "template_body";
    public static final String EXTRA_TEMPLATE_NAME = "template_name";

    private final List<String> filterValues = new ArrayList<>();
    private EditText searchInput;
    private Spinner purposeFilter;
    private LinearLayout listContainer;
    private TextView countText;
    private boolean selectMode;
    private String fixedPurpose = "";
    private String defaultPurpose = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageTemplateStore.ensureDefaults(this);
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
        Button add = button("새 템플릿", true);
        add.setOnClickListener(v -> showEditor(null));
        header.addView(add, new LinearLayout.LayoutParams(dp(116), dp(50)));
        page.addView(header, matchWrap());

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.VERTICAL);
        tools.setPadding(dp(20), 0, dp(20), dp(12));

        searchInput = input("이름·내용·카테고리 검색", false);
        searchInput.setSingleLine(true);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderTemplates();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        tools.addView(searchInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        purposeFilter = new Spinner(this);
        purposeFilter.setBackgroundResource(R.drawable.bg_secondary_button);
        purposeFilter.setPadding(dp(12), 0, dp(12), 0);
        setupPurposeFilter();
        purposeFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                renderTemplates();
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        filterRow.addView(purposeFilter, new LinearLayout.LayoutParams(0, dp(50), 1f));
        countText = body("");
        countText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(dp(112), dp(50));
        countParams.leftMargin = dp(10);
        filterRow.addView(countText, countParams);
        tools.addView(filterRow, topMargin(10));

        TextView help = body(selectMode
                ? "템플릿을 선택하면 현재 고객 정보로 치환된 문자 작성 화면으로 이동합니다."
                : "즐겨찾기와 최근 사용 순으로 정렬됩니다. 기본 템플릿은 삭제 전에 다른 템플릿으로 변경해야 합니다.");
        tools.addView(help, topMargin(8));
        page.addView(tools, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(20), dp(4), dp(20), dp(40));
        scroll.addView(listContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void setupPurposeFilter() {
        List<String> labels = new ArrayList<>();
        filterValues.clear();
        if (fixedPurpose.isEmpty()) {
            labels.add("전체 템플릿");
            filterValues.add("");
        }
        for (String purpose : MessageTemplateStore.purposeValues()) {
            if (!fixedPurpose.isEmpty()
                    && !fixedPurpose.equals(purpose)
                    && !MessageTemplateStore.PURPOSE_GENERAL.equals(purpose)) continue;
            labels.add(MessageTemplateStore.purposeLabel(purpose));
            filterValues.add(purpose);
        }
        purposeFilter.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
        if (!fixedPurpose.isEmpty()) {
            for (int i = 0; i < filterValues.size(); i++) {
                if (fixedPurpose.equals(filterValues.get(i))) {
                    purposeFilter.setSelection(i);
                    break;
                }
            }
            purposeFilter.setEnabled(false);
            purposeFilter.setAlpha(0.8f);
        }
    }

    private void renderTemplates() {
        if (listContainer == null || purposeFilter == null) return;
        String purpose = "";
        int position = purposeFilter.getSelectedItemPosition();
        if (position >= 0 && position < filterValues.size()) purpose = filterValues.get(position);
        List<MessageTemplateStore.Template> templates = MessageTemplateStore.list(
                this,
                searchInput == null ? "" : searchInput.getText().toString(),
                purpose);
        countText.setText(templates.size() + "개");
        listContainer.removeAllViews();
        if (templates.isEmpty()) {
            TextView empty = body("조건에 맞는 템플릿이 없습니다.\n새 템플릿을 만들어주세요.");
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
        if (selectMode) card.setOnClickListener(v -> choose(template));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = title(template.name, 16f);
        titleRow.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button favorite = button(template.favorite ? "★" : "☆", false);
        favorite.setTextSize(21f);
        favorite.setOnClickListener(v -> {
            MessageTemplateStore.setFavorite(this, template.id, !template.favorite);
            renderTemplates();
        });
        titleRow.addView(favorite, new LinearLayout.LayoutParams(dp(52), dp(44)));
        card.addView(titleRow, matchWrap());

        String defaultLabel = MessageTemplateStore.defaultUsageLabel(this, template.id);
        String metaText = MessageTemplateStore.purposeLabel(template.purpose)
                + " · " + template.category;
        if (!defaultLabel.isEmpty()) metaText += " · " + defaultLabel;
        if (template.useCount > 0) metaText += " · 최근 사용 " + template.useCount + "회";
        TextView meta = body(metaText);
        card.addView(meta, topMargin(3));

        TextView preview = body(template.body);
        preview.setTextColor(getColor(R.color.text_primary));
        preview.setMaxLines(4);
        preview.setBackgroundResource(R.drawable.bg_soft_panel);
        preview.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.addView(preview, topMargin(10));

        LinearLayout firstActions = new LinearLayout(this);
        firstActions.setOrientation(LinearLayout.HORIZONTAL);
        if (selectMode) {
            Button choose = button("이 템플릿 사용", true);
            choose.setOnClickListener(v -> choose(template));
            firstActions.addView(choose, new LinearLayout.LayoutParams(0, dp(48), 1f));
        } else {
            Button makeDefault = button(defaultLabel.isEmpty() ? "기본 지정" : defaultLabel, false);
            makeDefault.setOnClickListener(v -> setAsDefault(template));
            firstActions.addView(makeDefault, new LinearLayout.LayoutParams(0, dp(48), 1f));
        }
        Button edit = button("수정", false);
        edit.setOnClickListener(v -> showEditor(template));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        editParams.leftMargin = dp(8);
        firstActions.addView(edit, editParams);
        card.addView(firstActions, topMargin(10));

        LinearLayout secondActions = new LinearLayout(this);
        secondActions.setOrientation(LinearLayout.HORIZONTAL);
        Button duplicate = button("복제", false);
        duplicate.setOnClickListener(v -> {
            MessageTemplateStore.Template copied = MessageTemplateStore.duplicate(this, template.id);
            if (copied != null) {
                Toast.makeText(this, "템플릿을 복제했습니다.", Toast.LENGTH_SHORT).show();
                renderTemplates();
            }
        });
        secondActions.addView(duplicate, new LinearLayout.LayoutParams(0, dp(46), 1f));
        Button delete = button("삭제", false);
        delete.setOnClickListener(v -> confirmDelete(template));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        deleteParams.leftMargin = dp(8);
        secondActions.addView(delete, deleteParams);
        card.addView(secondActions, topMargin(8));
        return card;
    }

    private void choose(MessageTemplateStore.Template template) {
        if (!defaultPurpose.isEmpty()) {
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
        if (!fixedPurpose.isEmpty()) {
            MessageTemplateStore.setDefault(this, fixedPurpose, template.id);
            Toast.makeText(this,
                    MessageTemplateStore.purposeLabel(fixedPurpose) + " 기본 템플릿으로 지정했습니다.",
                    Toast.LENGTH_SHORT).show();
            renderTemplates();
            return;
        }
        List<String> purposes = new ArrayList<>();
        purposes.add(MessageTemplateStore.PURPOSE_INCOMING);
        purposes.add(MessageTemplateStore.PURPOSE_OUTGOING);
        purposes.add(MessageTemplateStore.PURPOSE_MISSED);
        purposes.add(MessageTemplateStore.PURPOSE_FOLLOW_UP);
        String[] labels = new String[purposes.size()];
        for (int i = 0; i < purposes.size(); i++) {
            labels[i] = MessageTemplateStore.purposeLabel(purposes.get(i));
        }
        new AlertDialog.Builder(this)
                .setTitle("기본 사용 위치")
                .setItems(labels, (dialog, which) -> {
                    String purpose = purposes.get(which);
                    MessageTemplateStore.setDefault(this, purpose, template.id);
                    Toast.makeText(this, labels[which] + " 기본 템플릿으로 지정했습니다.",
                            Toast.LENGTH_SHORT).show();
                    renderTemplates();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showEditor(MessageTemplateStore.Template current) {
        boolean editing = current != null;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(4), dp(20), 0);

        EditText name = input("템플릿 이름", false);
        name.setText(editing ? current.name : "");
        form.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        EditText category = input("카테고리 예: 예약, 상담, 결제", false);
        category.setText(editing ? current.category : "");
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        categoryParams.topMargin = dp(10);
        form.addView(category, categoryParams);

        Spinner purpose = new Spinner(this);
        purpose.setBackgroundResource(R.drawable.bg_secondary_button);
        purpose.setPadding(dp(12), 0, dp(12), 0);
        List<String> purposeValues = MessageTemplateStore.purposeValues();
        List<String> purposeLabels = new ArrayList<>();
        for (String value : purposeValues) purposeLabels.add(MessageTemplateStore.purposeLabel(value));
        purpose.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, purposeLabels));
        String selectedPurpose = editing ? current.purpose
                : (!fixedPurpose.isEmpty() ? fixedPurpose : MessageTemplateStore.PURPOSE_GENERAL);
        for (int i = 0; i < purposeValues.size(); i++) {
            if (selectedPurpose.equals(purposeValues.get(i))) purpose.setSelection(i);
        }
        LinearLayout.LayoutParams purposeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        purposeParams.topMargin = dp(10);
        form.addView(purpose, purposeParams);

        EditText body = input("문자 내용을 입력해주세요.", true);
        body.setGravity(Gravity.TOP | Gravity.START);
        body.setMinLines(6);
        body.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        body.setText(editing ? current.body : "");
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(180));
        bodyParams.topMargin = dp(10);
        form.addView(body, bodyParams);

        Switch favorite = new Switch(this);
        favorite.setText("즐겨찾기");
        favorite.setTextColor(getColor(R.color.text_primary));
        favorite.setChecked(editing && current.favorite);
        form.addView(favorite, topMargin(8));

        TextView variables = body("사용 가능: " + MessageTemplateEngine.supportedVariablesLabel());
        form.addView(variables, topMargin(8));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "템플릿 수정" : "새 템플릿")
                .setView(scroll)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    MessageTemplateStore.Template value = new MessageTemplateStore.Template(
                            editing ? current.id : "",
                            name.getText().toString(),
                            body.getText().toString(),
                            category.getText().toString(),
                            purposeValues.get(purpose.getSelectedItemPosition()),
                            favorite.isChecked(),
                            editing ? current.createdAt : 0L,
                            editing ? current.updatedAt : 0L,
                            editing ? current.lastUsedAt : 0L,
                            editing ? current.useCount : 0);
                    try {
                        MessageTemplateStore.save(this, value);
                        dialog.dismiss();
                        Toast.makeText(this, editing ? "템플릿을 수정했습니다." : "템플릿을 저장했습니다.",
                                Toast.LENGTH_SHORT).show();
                        renderTemplates();
                    } catch (IllegalArgumentException error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));
        dialog.show();
    }

    private void confirmDelete(MessageTemplateStore.Template template) {
        if (MessageTemplateStore.isDefault(this, template.id)) {
            Toast.makeText(this,
                    "기본 템플릿입니다. 다른 템플릿을 기본으로 지정한 뒤 삭제해주세요.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("템플릿 삭제")
                .setMessage("‘" + template.name + "’ 템플릿을 삭제할까요?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    if (MessageTemplateStore.delete(this, template.id)) {
                        Toast.makeText(this, "템플릿을 삭제했습니다.", Toast.LENGTH_SHORT).show();
                        renderTemplates();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private EditText input(String hint, boolean multiline) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setTextSize(15f);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setBackgroundResource(R.drawable.bg_secondary_button);
        input.setSingleLine(!multiline);
        return input;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
