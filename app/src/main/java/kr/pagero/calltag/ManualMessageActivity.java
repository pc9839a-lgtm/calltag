package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
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

public final class ManualMessageActivity extends Activity {
    public static final String EXTRA_USE_TEMPLATE = "use_template";
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_CUSTOMER_ID = "customer_id";

    private static final int REQUEST_SMS = 8101;
    private static final int REQUEST_TEMPLATE = 8102;
    private static final int REQUEST_IMAGE = 8103;

    private EditText phoneInput;
    private EditText bodyInput;
    private EditText delayDaysInput;
    private TextView selectedTemplateText;
    private TextView attachmentStatus;
    private ImageView attachmentPreview;
    private Button attachmentRemove;
    private long customerId;
    private String selectedTemplateId = "";
    private String selectedImageRef = "";
    private boolean ownsSelectedImage;
    private boolean attachmentCommitted;
    private boolean pendingSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageAutomationStore.ensureDefaults(this);
        MessageTemplateStore.ensureDefaults(this);
        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, 0L);
        setContentView(buildContent());
        String phone = getIntent().getStringExtra(EXTRA_PHONE);
        if (phone != null) phoneInput.setText(phone);
        if (getIntent().getBooleanExtra(EXTRA_USE_TEMPLATE, false)) {
            bodyInput.post(this::openTemplateLibrary);
        }
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("문자 보내기", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        root.addView(label("받는 번호"), topMargin(24));
        phoneInput = input("010-0000-0000", false);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        phoneParams.topMargin = dp(8);
        root.addView(phoneInput, phoneParams);

        root.addView(label("템플릿"), topMargin(20));
        LinearLayout templateCard = new LinearLayout(this);
        templateCard.setOrientation(LinearLayout.VERTICAL);
        templateCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        templateCard.setBackgroundResource(R.drawable.bg_card);
        selectedTemplateText = body("선택한 템플릿 없음");
        templateCard.addView(selectedTemplateText, matchWrap());
        Button selectTemplate = button("템플릿 선택", false);
        selectTemplate.setOnClickListener(v -> openTemplateLibrary());
        LinearLayout.LayoutParams selectParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        selectParams.topMargin = dp(8);
        templateCard.addView(selectTemplate, selectParams);
        root.addView(templateCard, topMargin(8));

        root.addView(label("문자 내용"), topMargin(20));
        bodyInput = input("템플릿을 선택하거나 보낼 내용을 입력해주세요.", true);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setMinLines(6);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(190));
        bodyParams.topMargin = dp(8);
        root.addView(bodyInput, bodyParams);

        root.addView(label("이미지 첨부"), topMargin(20));
        LinearLayout attachmentCard = new LinearLayout(this);
        attachmentCard.setOrientation(LinearLayout.VERTICAL);
        attachmentCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        attachmentCard.setBackgroundResource(R.drawable.bg_card);
        attachmentStatus = body("첨부 이미지 없음");
        attachmentCard.addView(attachmentStatus, matchWrap());
        attachmentPreview = new ImageView(this);
        attachmentPreview.setAdjustViewBounds(true);
        attachmentPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        attachmentPreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(220));
        previewParams.topMargin = dp(10);
        attachmentCard.addView(attachmentPreview, previewParams);

        LinearLayout attachmentActions = new LinearLayout(this);
        attachmentActions.setOrientation(LinearLayout.HORIZONTAL);
        Button attachmentSelect = button("이미지 추가·교체", false);
        attachmentSelect.setOnClickListener(v -> pickImage());
        attachmentActions.addView(attachmentSelect,
                new LinearLayout.LayoutParams(0, dp(48), 1f));
        attachmentRemove = button("제거", false);
        attachmentRemove.setVisibility(View.GONE);
        attachmentRemove.setOnClickListener(v -> clearAttachment());
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        removeParams.leftMargin = dp(8);
        attachmentActions.addView(attachmentRemove, removeParams);
        LinearLayout.LayoutParams actionParams = matchWrap();
        actionParams.topMargin = dp(10);
        attachmentCard.addView(attachmentActions, actionParams);
        root.addView(attachmentCard, topMargin(8));

        TextView previewNotice = body("텍스트 문자는 콜태그에서 바로 발송합니다. 이미지 문자는 기본 메시지 앱에 번호·본문·이미지를 채워 열며 사용자가 전송 버튼을 누릅니다.");
        root.addView(previewNotice, topMargin(10));

        Button sendNow = button("지금 보내기", true);
        sendNow.setOnClickListener(v -> sendNow());
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        sendParams.topMargin = dp(20);
        root.addView(sendNow, sendParams);

        root.addView(label("후속문자 예약"), topMargin(26));
        LinearLayout scheduleRow = new LinearLayout(this);
        scheduleRow.setOrientation(LinearLayout.HORIZONTAL);
        delayDaysInput = input("3", false);
        delayDaysInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        delayDaysInput.setText(String.valueOf(MessageAutomationStore.delayDays(this)));
        scheduleRow.addView(delayDaysInput, new LinearLayout.LayoutParams(0, dp(54), 1f));
        TextView suffix = body("일 후");
        suffix.setGravity(Gravity.CENTER);
        scheduleRow.addView(suffix, new LinearLayout.LayoutParams(dp(64), dp(54)));
        Button schedule = button("예약", false);
        schedule.setOnClickListener(v -> schedule());
        scheduleRow.addView(schedule, new LinearLayout.LayoutParams(dp(110), dp(54)));
        root.addView(scheduleRow, topMargin(8));

        TextView notice = body("이미지 예약은 지정 시각에 알림을 띄웁니다. 알림을 눌러 메시지 앱에서 최종 전송해주세요. 실제 문자요금은 통신사 요금제에 따라 부과됩니다.");
        root.addView(notice, topMargin(14));
        return scroll;
    }

    private void openTemplateLibrary() {
        if (!FeatureEntitlementStore.hasMessageAccess(this)) {
            Toast.makeText(this, "문자자동화 이용권이 필요합니다.", Toast.LENGTH_LONG).show();
            return;
        }
        startActivityForResult(new Intent(this, MessageTemplateLibraryActivity.class)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_SELECT_MODE, true), REQUEST_TEMPLATE);
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
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQUEST_TEMPLATE) {
            selectedTemplateId = safe(data.getStringExtra(
                    MessageTemplateLibraryActivity.EXTRA_TEMPLATE_ID));
            String body = data.getStringExtra(MessageTemplateLibraryActivity.EXTRA_TEMPLATE_BODY);
            String name = data.getStringExtra(MessageTemplateLibraryActivity.EXTRA_TEMPLATE_NAME);
            selectedTemplateText.setText((name == null || name.trim().isEmpty())
                    ? "선택한 템플릿" : name);
            MessageTemplateStore.Template template = MessageTemplateStore.get(this, selectedTemplateId);
            setAttachment(template == null ? "" : template.imageRef, false);
            applyTemplate(body == null ? "" : body);
            return;
        }
        if (requestCode == REQUEST_IMAGE) {
            Uri uri = data.getData();
            if (uri == null) return;
            try {
                String ref = MessageAttachmentStore.importImage(this, uri,
                        "draft-" + UUID.randomUUID());
                clearOwnedAttachment();
                setAttachment(ref, true);
            } catch (IllegalArgumentException error) {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setAttachment(String imageRef, boolean owned) {
        if (ownsSelectedImage && !attachmentCommitted
                && !safe(selectedImageRef).equals(safe(imageRef))) {
            MessageAttachmentStore.delete(this, selectedImageRef);
        }
        selectedImageRef = safe(imageRef);
        ownsSelectedImage = owned;
        attachmentCommitted = false;
        renderAttachment();
    }

    private void clearAttachment() {
        clearOwnedAttachment();
        selectedImageRef = "";
        ownsSelectedImage = false;
        attachmentCommitted = false;
        renderAttachment();
    }

    private void clearOwnedAttachment() {
        if (ownsSelectedImage && !attachmentCommitted && !selectedImageRef.isEmpty()) {
            MessageAttachmentStore.delete(this, selectedImageRef);
        }
    }

    private void renderAttachment() {
        boolean attached = MessageAttachmentStore.exists(this, selectedImageRef);
        Bitmap bitmap = attached ? MessageAttachmentStore.preview(this, selectedImageRef) : null;
        attachmentPreview.setImageBitmap(bitmap);
        attachmentPreview.setVisibility(attached ? View.VISIBLE : View.GONE);
        attachmentRemove.setVisibility(attached ? View.VISIBLE : View.GONE);
        attachmentStatus.setText(attached
                ? "이미지 1장 · " + MessageAttachmentStore.sizeLabel(this, selectedImageRef)
                : "첨부 이미지 없음");
    }

    private void applyTemplate(String template) {
        MessageTemplateEngine.RenderResult rendered = render(template);
        bodyInput.setText(rendered.body);
        bodyInput.setSelection(bodyInput.getText().length());
        if (!rendered.unsupportedVariables.isEmpty()) {
            Toast.makeText(this, "지원하지 않는 변수가 있습니다: "
                    + MessageTemplateEngine.describeVariables(rendered.unsupportedVariables),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void sendNow() {
        String body = validatedBody();
        if (body == null || blockedByPolicy(MessageAutomationManager.TRIGGER_MANUAL)) return;
        if (MessageAttachmentStore.exists(this, selectedImageRef)) {
            composeImageNow(body);
            return;
        }
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pendingSend = true;
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
            return;
        }
        long id = SmsSender.queueAndSend(
                this, customerId, 0L, 0L, "", selectedTemplateId,
                phoneInput.getText().toString(), body,
                MessageAutomationManager.TRIGGER_MANUAL,
                MessageAutomationStore.selectedSubscriptionId(this), false);
        showQueueResult(id, "문자 발송을 요청했습니다.");
    }

    private void composeImageNow(String body) {
        MessageLogStore store = new MessageLogStore(this);
        long id;
        try {
            id = store.createJobAdvanced(customerId, 0L, 0L, "", selectedTemplateId,
                    phoneInput.getText().toString(), body,
                    MessageAutomationManager.TRIGGER_MANUAL,
                    MessageLogStore.STATUS_READY, System.currentTimeMillis(),
                    MessageAutomationStore.selectedSubscriptionId(this), false);
            MessageRecord record = store.find(id);
            if (record == null || !MessageLogStore.STATUS_READY.equals(record.status)) {
                showQueueResult(id, "이미지 문자 준비를 요청했습니다.");
                return;
            }
            MmsComposer.remember(this, id, selectedImageRef);
            attachmentCommitted = true;
        } finally {
            store.close();
        }
        if (!MmsComposer.openComposer(this, id)) {
            Toast.makeText(this, "이미지 문자 작성창을 열지 못했습니다.", Toast.LENGTH_LONG).show();
        }
        startActivity(new Intent(this, MessageHistoryActivity.class)
                .putExtra("focus_message_id", id));
        finish();
    }

    private void schedule() {
        String body = validatedBody();
        if (body == null || blockedByPolicy(MessageAutomationManager.TRIGGER_DELAYED)) return;
        int days;
        try {
            days = Integer.parseInt(delayDaysInput.getText().toString().trim());
        } catch (NumberFormatException error) {
            days = 3;
        }
        days = Math.max(1, Math.min(30, days));
        long when = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L;
        MessageLogStore store = new MessageLogStore(this);
        long id;
        try {
            id = store.createJobAdvanced(
                    customerId, 0L, 0L, "", selectedTemplateId,
                    phoneInput.getText().toString(), body,
                    MessageAutomationManager.TRIGGER_DELAYED,
                    MessageLogStore.STATUS_SCHEDULED, when,
                    MessageAutomationStore.selectedSubscriptionId(this), false);
            MessageRecord created = store.find(id);
            if (created != null && MessageLogStore.STATUS_SCHEDULED.equals(created.status)) {
                if (MessageAttachmentStore.exists(this, selectedImageRef)) {
                    MmsComposer.remember(this, id, selectedImageRef);
                    attachmentCommitted = true;
                }
                MessageScheduler.schedule(this, id, when);
            }
        } finally {
            store.close();
        }
        showQueueResult(id, MessageAttachmentStore.exists(this, selectedImageRef)
                ? days + "일 후 이미지 문자 전송 알림으로 예약했습니다."
                : days + "일 후 후속문자로 예약했습니다.");
    }

    private void showQueueResult(long id, String successMessage) {
        MessageLogStore store = new MessageLogStore(this);
        MessageRecord record;
        try {
            record = store.find(id);
        } finally {
            store.close();
        }
        if (record != null && MessageLogStore.STATUS_SKIPPED.equals(record.status)
                && MessageDedupeEngine.isDuplicateReason(record.error)) {
            Toast.makeText(this, record.error, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, successMessage + " 내역에서 결과를 확인하세요.",
                    Toast.LENGTH_LONG).show();
        }
        startActivity(new Intent(this, MessageHistoryActivity.class)
                .putExtra("focus_message_id", id));
        finish();
    }

    private boolean blockedByPolicy(String triggerType) {
        MessageExclusionStore.Decision decision = MessageExclusionStore.evaluate(
                this, customerId, phoneInput.getText().toString(), triggerType);
        if (!decision.blocked) return false;
        Toast.makeText(this, decision.reason + " 고객 상세의 발송 제외 설정을 확인해주세요.",
                Toast.LENGTH_LONG).show();
        return true;
    }

    private String validatedBody() {
        if (!FeatureEntitlementStore.hasMessageAccess(this)) {
            Toast.makeText(this, "문자자동화 이용권이 필요합니다.", Toast.LENGTH_LONG).show();
            return null;
        }
        if (PhoneNumberNormalizer.normalize(phoneInput.getText().toString()).length() < 8) {
            Toast.makeText(this, "받는 번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (bodyInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "문자 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return null;
        }
        MessageTemplateEngine.RenderResult rendered = render(bodyInput.getText().toString());
        bodyInput.setText(rendered.body);
        bodyInput.setSelection(bodyInput.getText().length());
        if (!rendered.unsupportedVariables.isEmpty()) {
            Toast.makeText(this, "지원하지 않는 변수가 있습니다: "
                    + MessageTemplateEngine.describeVariables(rendered.unsupportedVariables),
                    Toast.LENGTH_LONG).show();
            return null;
        }
        if (!rendered.unresolvedVariables.isEmpty()) {
            Toast.makeText(this, "치환되지 않은 변수가 있습니다: "
                    + MessageTemplateEngine.describeVariables(rendered.unresolvedVariables)
                    + ". 고객·계정·일정 정보를 확인해주세요.", Toast.LENGTH_LONG).show();
            return null;
        }
        return rendered.body;
    }

    private MessageTemplateEngine.RenderResult render(String template) {
        Customer customer = null;
        if (customerId > 0L) {
            CallTagDbHelper db = new CallTagDbHelper(this);
            try {
                customer = db.findCustomerById(customerId);
            } finally {
                db.close();
            }
        }
        String phone = phoneInput == null ? "" : phoneInput.getText().toString();
        String cachedName = customer == null ? "" : customer.displayName;
        CallRecord currentContext = new CallRecord(
                0L, phone, cachedName, 0, System.currentTimeMillis(), 0L);
        return MessageTemplateEngine.render(this, template, customer, currentContext);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_SMS) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingSend) {
            pendingSend = false;
            sendNow();
        } else {
            pendingSend = false;
            Toast.makeText(this, "문자 발송 권한이 필요합니다.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        clearOwnedAttachment();
        super.onDestroy();
    }

    private EditText input(String hint, boolean multiline) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setTextSize(15f);
        input.setPadding(dp(16), dp(10), dp(16), dp(10));
        input.setBackgroundResource(R.drawable.bg_secondary_button);
        input.setSingleLine(!multiline);
        return input;
    }

    private TextView label(String value) {
        TextView label = title(value, 15f);
        label.setTextColor(getColor(R.color.text_secondary));
        return label;
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

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
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
}
