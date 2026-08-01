package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

/** 템플릿 이름·내용·이미지만 편집한다. */
public final class MessageTemplateEditorActivity extends Activity {
    public static final String EXTRA_TEMPLATE_ID = "template_id";
    public static final String EXTRA_FIXED_PURPOSE = "fixed_purpose";

    private static final int REQUEST_IMAGE = 8401;

    private EditText nameInput;
    private EditText bodyInput;
    private TextView imageStatus;
    private ImageView imagePreview;
    private Button removeImage;

    private MessageTemplateStore.Template current;
    private String originalImageRef = "";
    private String selectedImageRef = "";
    private boolean ownsSelectedImage;
    private boolean saved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageTemplateStore.ensureDefaults(this);
        String id = safe(getIntent().getStringExtra(EXTRA_TEMPLATE_ID));
        current = id.isEmpty() ? null : MessageTemplateStore.get(this, id);
        if (current != null) {
            originalImageRef = safe(current.imageRef);
            selectedImageRef = originalImageRef;
        }
        setContentView(buildContent());
        bindValues();
        renderImage();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(27f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = title(current == null ? "새 템플릿" : "템플릿 수정", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        root.addView(label("이름"), topMargin(18));
        nameInput = input("예: 예약 안내", false);
        root.addView(nameInput, fixedHeight(50, 7));

        root.addView(label("문자 내용"), topMargin(18));
        bodyInput = input("문자 내용을 입력해주세요", true);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setMinLines(5);
        bodyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        root.addView(bodyInput, fixedHeight(170, 7));

        TextView variables = body("변수 · " + MessageTemplateEngine.supportedVariablesLabel());
        variables.setSingleLine(true);
        variables.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(variables, topMargin(7));

        root.addView(label("이미지"), topMargin(18));
        LinearLayout imageRow = new LinearLayout(this);
        imageRow.setGravity(Gravity.CENTER_VERTICAL);
        imageRow.setPadding(dp(14), 0, dp(8), 0);
        imageRow.setBackgroundResource(R.drawable.bg_clickable_row);
        imageRow.setClickable(true);
        imageRow.setFocusable(true);
        imageRow.setOnClickListener(v -> pickImage());

        imageStatus = body("첨부 이미지 없음");
        imageStatus.setTextColor(getColor(R.color.text_primary));
        imageStatus.setSingleLine(true);
        imageStatus.setEllipsize(TextUtils.TruncateAt.END);
        imageRow.addView(imageStatus, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button pick = button("추가", false);
        pick.setOnClickListener(v -> pickImage());
        imageRow.addView(pick, new LinearLayout.LayoutParams(dp(66), dp(40)));
        removeImage = button("삭제", false);
        removeImage.setTextColor(getColor(R.color.danger));
        removeImage.setOnClickListener(v -> clearImage());
        imageRow.addView(removeImage, new LinearLayout.LayoutParams(dp(66), dp(40)));
        root.addView(imageRow, fixedHeight(54, 7));

        imagePreview = new ImageView(this);
        imagePreview.setAdjustViewBounds(true);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(150));
        previewParams.topMargin = dp(8);
        root.addView(imagePreview, previewParams);

        Button save = button("저장", true);
        save.setOnClickListener(v -> saveTemplate());
        root.addView(save, fixedHeight(52, 22));
        return scroll;
    }

    private void bindValues() {
        if (current == null) return;
        nameInput.setText(current.name);
        bodyInput.setText(current.body);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMAGE || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        try {
            String ref = MessageAttachmentStore.importImage(
                    this, uri, "template-draft-" + UUID.randomUUID());
            clearOwnedImage();
            selectedImageRef = ref;
            ownsSelectedImage = true;
            renderImage();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void clearImage() {
        clearOwnedImage();
        selectedImageRef = "";
        ownsSelectedImage = false;
        renderImage();
    }

    private void clearOwnedImage() {
        if (ownsSelectedImage && !selectedImageRef.isEmpty()) {
            MessageAttachmentStore.delete(this, selectedImageRef);
        }
    }

    private void renderImage() {
        boolean attached = MessageAttachmentStore.exists(this, selectedImageRef);
        Bitmap bitmap = attached ? MessageAttachmentStore.preview(this, selectedImageRef) : null;
        imagePreview.setImageBitmap(bitmap);
        imagePreview.setVisibility(attached ? View.VISIBLE : View.GONE);
        removeImage.setVisibility(attached ? View.VISIBLE : View.GONE);
        imageStatus.setText(attached
                ? "이미지 1장 · " + MessageAttachmentStore.sizeLabel(this, selectedImageRef)
                : "첨부 이미지 없음");
    }

    private void saveTemplate() {
        String name = text(nameInput);
        String body = text(bodyInput);
        if (name.isEmpty()) {
            Toast.makeText(this, "템플릿 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (body.isEmpty()) {
            Toast.makeText(this, "문자 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String storedCategory = current == null || safe(current.category).isEmpty()
                ? "일반" : current.category;
        MessageTemplateStore.Template value = current == null
                ? MessageTemplateStore.Template.create(
                        name, body, storedCategory, MessageTemplateStore.PURPOSE_GENERAL)
                : current;
        value.name = name;
        value.category = storedCategory;
        value.body = body;
        value.favorite = false;
        value.imageRef = selectedImageRef;
        String fixedPurpose = safe(getIntent().getStringExtra(EXTRA_FIXED_PURPOSE));
        if (current == null && !fixedPurpose.isEmpty()) value.purpose = fixedPurpose;

        try {
            MessageTemplateStore.Template stored = MessageTemplateStore.save(this, value);
            saved = true;
            ownsSelectedImage = false;
            if (!originalImageRef.isEmpty()
                    && !originalImageRef.equals(stored.imageRef)
                    && MessageAttachmentStore.exists(this, originalImageRef)) {
                MessageAttachmentStore.delete(this, originalImageRef);
            }
            setResult(RESULT_OK, new Intent()
                    .putExtra(EXTRA_TEMPLATE_ID, stored.id));
            Toast.makeText(this, "저장했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
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

    private EditText input(String hint, boolean multiline) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextSize(14f);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), dp(multiline ? 12 : 0), dp(14), dp(multiline ? 12 : 0));
        input.setSingleLine(!multiline);
        return input;
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

    private TextView label(String value) {
        TextView text = title(value, 14f);
        text.setTextColor(getColor(R.color.text_secondary));
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

    private LinearLayout.LayoutParams fixedHeight(int height, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(topMargin);
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

    private String text(EditText input) {
        return input.getText().toString().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (!saved) clearOwnedImage();
        super.onDestroy();
    }
}
