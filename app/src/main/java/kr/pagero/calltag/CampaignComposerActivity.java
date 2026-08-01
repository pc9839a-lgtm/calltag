package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CampaignComposerActivity extends Activity {
    public static final String EXTRA_GROUP_ID = "group_id";
    private static final int REQUEST_TEMPLATE = 8701;
    private static final int REQUEST_SMS = 8702;

    private MessageGroupStore groups;
    private String selectedGroupId = "";
    private String selectedTemplateId = "";
    private String selectedTemplateName = "";
    private Button groupButton;
    private Button templateButton;
    private EditText nameInput;
    private EditText bodyInput;
    private EditText delayInput;
    private TextView groupMeta;
    private boolean pendingCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groups = new MessageGroupStore(this);
        selectedGroupId = safe(getIntent().getStringExtra(EXTRA_GROUP_ID));
        MessageTemplateStore.ensureDefaults(this);
        setContentView(buildContent());
        renderGroup();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(36));
        root.setFocusableInTouchMode(true);
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(27f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = title("단체문자 만들기", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        root.addView(label("이름"), topMargin(18));
        nameInput = input("예: 8월 예약 안내", false);
        nameInput.setText(new SimpleDateFormat("M월 d일 단체문자", Locale.KOREA)
                .format(new Date()));
        root.addView(nameInput, fixedHeight(50, 7));

        root.addView(label("수신자 그룹"), topMargin(18));
        groupButton = selectionButton("그룹 선택");
        groupButton.setOnClickListener(v -> chooseGroup());
        root.addView(groupButton, fixedHeight(52, 7));
        groupMeta = body("그룹을 선택해주세요");
        groupMeta.setSingleLine(true);
        groupMeta.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(groupMeta, topMargin(5));

        root.addView(label("템플릿"), topMargin(18));
        templateButton = selectionButton("템플릿 선택");
        templateButton.setOnClickListener(v -> openTemplates());
        root.addView(templateButton, fixedHeight(52, 7));

        root.addView(label("문자 내용"), topMargin(18));
        bodyInput = input("템플릿을 선택하거나 문구를 입력해주세요", true);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setMinLines(5);
        bodyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        root.addView(bodyInput, fixedHeight(170, 7));
        TextView variables = body("변수 · " + MessageTemplateEngine.supportedVariablesLabel());
        variables.setSingleLine(true);
        variables.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(variables, topMargin(6));

        root.addView(label("발송 시작"), topMargin(18));
        LinearLayout delayRow = new LinearLayout(this);
        delayRow.setOrientation(LinearLayout.HORIZONTAL);
        delayRow.setGravity(Gravity.CENTER_VERTICAL);
        delayInput = input("0", false);
        delayInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        delayInput.setText("0");
        delayInput.setGravity(Gravity.CENTER);
        delayRow.addView(delayInput, new LinearLayout.LayoutParams(0, dp(50), 1f));
        TextView suffix = body("분 후 시작");
        suffix.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams suffixParams = new LinearLayout.LayoutParams(
                0, dp(50), 1.4f);
        suffixParams.leftMargin = dp(10);
        delayRow.addView(suffix, suffixParams);
        root.addView(delayRow, topMargin(7));

        Button start = button("단체문자 만들기", true);
        start.setOnClickListener(v -> confirmCreate());
        root.addView(start, fixedHeight(52, 22));
        root.requestFocus();
        return scroll;
    }

    private void chooseGroup() {
        List<MessageGroupStore.Group> rows = groups.list();
        if (rows.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("고객 그룹이 없습니다")
                    .setMessage("먼저 고객 그룹을 만들어주세요.")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("그룹 만들기", (dialog, which) ->
                            startActivity(new Intent(this, MessageGroupActivity.class)))
                    .show();
            return;
        }
        String[] labels = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            MessageGroupStore.Group group = rows.get(i);
            labels[i] = group.name + " · " + groups.countMembers(this, group) + "명";
        }
        new AlertDialog.Builder(this)
                .setTitle("수신자 그룹")
                .setItems(labels, (dialog, which) -> {
                    selectedGroupId = rows.get(which).id;
                    renderGroup();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void renderGroup() {
        if (groupButton == null) return;
        MessageGroupStore.Group group = groups.find(selectedGroupId);
        if (group == null) {
            groupButton.setText("그룹 선택    ›");
            groupMeta.setText("그룹을 선택해주세요");
            return;
        }
        int count = groups.countMembers(this, group);
        groupButton.setText(group.name + " · " + count + "명    ›");
        groupMeta.setText(MessageGroupStore.describe(group));
    }

    private void openTemplates() {
        startActivityForResult(new Intent(this, MessageTemplateLibraryActivity.class)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_SELECT_MODE, true), REQUEST_TEMPLATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TEMPLATE || resultCode != RESULT_OK || data == null) return;
        String templateId = safe(data.getStringExtra(
                MessageTemplateLibraryActivity.EXTRA_TEMPLATE_ID));
        MessageTemplateStore.Template template = MessageTemplateStore.get(this, templateId);
        if (template != null && MessageAttachmentStore.exists(this, template.imageRef)) {
            Toast.makeText(this, "이미지 템플릿은 단체문자에 사용할 수 없습니다.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        selectedTemplateId = templateId;
        selectedTemplateName = safe(data.getStringExtra(
                MessageTemplateLibraryActivity.EXTRA_TEMPLATE_NAME));
        templateButton.setText((selectedTemplateName.isEmpty()
                ? "선택한 템플릿" : selectedTemplateName) + "    ›");
        bodyInput.setText(safe(data.getStringExtra(
                MessageTemplateLibraryActivity.EXTRA_TEMPLATE_BODY)));
        bodyInput.setSelection(bodyInput.getText().length());
    }

    private void confirmCreate() {
        MessageGroupStore.Group group = groups.find(selectedGroupId);
        if (group == null) {
            Toast.makeText(this, "수신자 그룹을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        int count = groups.countMembers(this, group);
        if (count <= 0) {
            Toast.makeText(this, "현재 그룹에 고객이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bodyInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "문자 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("단체문자를 만들까요?")
                .setMessage(group.name + "의 현재 고객 " + count
                        + "명을 기준으로 발송 작업을 만듭니다. 제외·중복·변수 오류 고객은 발송하지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("만들기", (dialog, which) -> createCampaign())
                .show();
    }

    private void createCampaign() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingCreate = true;
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
            return;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(delayInput.getText().toString().trim());
        } catch (NumberFormatException ignored) {
            minutes = 0;
        }
        minutes = Math.max(0, Math.min(10080, minutes));
        long startAt = System.currentTimeMillis() + minutes * 60L * 1000L;
        try {
            String campaignId = CampaignManager.create(this,
                    nameInput.getText().toString(), selectedGroupId,
                    selectedTemplateId, selectedTemplateName,
                    bodyInput.getText().toString(), startAt);
            startActivity(new Intent(this, CampaignDetailActivity.class)
                    .putExtra(CampaignDetailActivity.EXTRA_CAMPAIGN_ID, campaignId));
            finish();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_SMS) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingCreate) {
            pendingCreate = false;
            createCampaign();
        } else {
            pendingCreate = false;
            Toast.makeText(this, "단체문자 발송 권한이 필요합니다.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private EditText input(String hint, boolean multiline) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setTextSize(14f);
        input.setPadding(dp(14), dp(multiline ? 12 : 0), dp(14), dp(multiline ? 12 : 0));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setSingleLine(!multiline);
        return input;
    }

    private Button selectionButton(String value) {
        Button button = button(value + "    ›", false);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setPadding(dp(14), 0, dp(12), 0);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        return button;
    }

    private TextView label(String value) {
        TextView text = title(value, 14f);
        text.setTextColor(getColor(R.color.text_secondary));
        return text;
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
        text.setTextSize(12f);
        text.setIncludeFontPadding(false);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams fixedHeight(int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
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
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (groups != null) groups.close();
        super.onDestroy();
    }
}
