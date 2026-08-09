package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class TemplateImageActivity extends Activity {
    public static final String EXTRA_TEMPLATE_ID = "template_id";
    private static final int REQUEST_IMAGE = 8301;

    private String templateId;
    private MessageTemplateStore.Template template;
    private ImageView preview;
    private TextView status;
    private Button select;
    private Button remove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        templateId = safe(getIntent().getStringExtra(EXTRA_TEMPLATE_ID));
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTemplate();
    }

    private View buildContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(getColor(R.color.background));
        page.setPadding(dp(20), dp(18), dp(20), dp(36));

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
        page.addView(header, matchWrap());

        status = body("이미지를 확인하는 중입니다.");
        status.setBackgroundResource(R.drawable.bg_soft_panel);
        status.setPadding(dp(14), dp(12), dp(14), dp(12));
        page.addView(status, topMargin(16));

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(280));
        previewParams.topMargin = dp(14);
        page.addView(preview, previewParams);

        select = button("이미지 선택", true);
        select.setOnClickListener(v -> pickImage());
        LinearLayout.LayoutParams selectParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        selectParams.topMargin = dp(14);
        page.addView(select, selectParams);

        remove = button("첨부 이미지 제거", false);
        remove.setOnClickListener(v -> confirmRemove());
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        removeParams.topMargin = dp(8);
        page.addView(remove, removeParams);

        TextView guide = body("이미지는 앱 안에 복사해 보관하며 MMS 전송에 맞게 크기를 줄입니다. 이미지가 포함된 템플릿은 자동발송 기본값으로 사용할 수 없고, 수동 발송이나 예약 알림에서 메시지 앱으로 전달됩니다.");
        page.addView(guide, topMargin(14));
        return page;
    }

    private void loadTemplate() {
        template = MessageTemplateStore.get(this, templateId);
        if (template == null) {
            Toast.makeText(this, "템플릿을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        boolean attached = MessageAttachmentStore.exists(this, template.imageRef);
        Bitmap bitmap = attached ? MessageAttachmentStore.preview(this, template.imageRef) : null;
        preview.setImageBitmap(bitmap);
        preview.setVisibility(attached ? View.VISIBLE : View.GONE);
        status.setText(template.name + " · " + (attached
                ? "이미지 1장 · " + MessageAttachmentStore.sizeLabel(this, template.imageRef)
                : "첨부 이미지 없음"));
        select.setText(attached ? "이미지 교체" : "이미지 선택");
        remove.setVisibility(attached ? View.VISIBLE : View.GONE);
    }

    private void pickImage() {
        if (template == null) return;
        if (MessageTemplateStore.isDefault(this, template.id)) {
            Toast.makeText(this,
                    "자동발송 기본 템플릿에는 이미지를 첨부할 수 없습니다. 다른 템플릿을 기본으로 지정한 뒤 첨부해주세요.",
                    Toast.LENGTH_LONG).show();
            return;
        }
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
        if (uri == null || template == null) return;
        String oldRef = template.imageRef;
        try {
            String newRef = MessageAttachmentStore.importImage(
                    this, uri, "template-" + template.id);
            template.imageRef = newRef;
            MessageTemplateStore.save(this, template);
            if (!safe(oldRef).isEmpty()) MessageAttachmentStore.delete(this, oldRef);
            Toast.makeText(this, "템플릿 이미지를 저장했습니다.", Toast.LENGTH_SHORT).show();
            loadTemplate();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmRemove() {
        if (template == null || safe(template.imageRef).isEmpty()) return;
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("첨부 이미지 제거")
                .setMessage("‘" + template.name + "’ 템플릿의 이미지를 제거할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("제거", (dialog, which) -> {
                    String oldRef = template.imageRef;
                    template.imageRef = "";
                    MessageTemplateStore.save(this, template);
                    MessageAttachmentStore.delete(this, oldRef);
                    Toast.makeText(this, "첨부 이미지를 제거했습니다.", Toast.LENGTH_SHORT).show();
                    loadTemplate();
                })
                .show();
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15f);
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
        text.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
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
