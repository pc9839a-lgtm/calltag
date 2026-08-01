package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public final class ManualMessageActivity extends Activity {
    public static final String EXTRA_USE_TEMPLATE = "use_template";
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_CUSTOMER_ID = "customer_id";

    private static final int REQUEST_SMS = 8101;
    private static final int REQUEST_TEMPLATE = 8102;
    private static final int REQUEST_IMAGE = 8103;
    private static final int REQUEST_TEMPLATE_EDIT = 8104;

    private EditText phoneInput;
    private EditText bodyInput;
    private EditText delayDaysInput;
    private TextView selectedTemplateText;
    private TextView attachmentStatus;
    private ImageView attachmentPreview;
    private Button attachmentRemove;
    private Button templateEdit;
    private long customerId;
    private Customer linkedCustomer;
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
        if (customerId > 0L) {
            CallTagDbHelper db = new CallTagDbHelper(this);
            try {
                linkedCustomer = db.findCustomerById(customerId);
            } finally {
                db.close();
            }
        }
        setContentView(buildContent());
        String phone = getIntent().getStringExtra(EXTRA_PHONE);
        if (linkedCustomer != null) phone = linkedCustomer.primaryPhone;
        if (phone != null) phoneInput.setText(phone);
        if (getIntent().getBooleanExtra(EXTRA_USE_TEMPLATE, false)) {
            bodyInput.post(this::openTemplateLibrary);
        }
    }

    private View buildContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(getColor(R.color.background));
        page.setFitsSystemWindows(true);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(5), dp(16), dp(5));
        TextView back = title("‹", 31f);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView screenTitle = title("문자 보내기", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(8);
        header.addView(screenTitle, titleParams);
        page.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(6), dp(16), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        phoneInput = input("010-0000-0000", false);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        if (linkedCustomer != null) {
            LinearLayout recipient = new LinearLayout(this);
            recipient.setOrientation(LinearLayout.HORIZONTAL);
            recipient.setGravity(Gravity.CENTER_VERTICAL);
            recipient.setPadding(dp(15), dp(11), dp(15), dp(11));
            recipient.setBackgroundResource(R.drawable.bg_card);

            LinearLayout recipientText = new LinearLayout(this);
            recipientText.setOrientation(LinearLayout.VERTICAL);
            TextView name = title(linkedCustomer.displayName, 16f);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            recipientText.addView(name, matchWrap());
            recipientText.addView(body(linkedCustomer.primaryPhone), topMargin(3));
            recipient.addView(recipientText, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView linked = body("고객 연결");
            linked.setTextColor(getColor(R.color.primary));
            linked.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            recipient.addView(linked);
            root.addView(recipient, topMargin(6));
        } else {
            root.addView(label("받는 번호"), topMargin(8));
            root.addView(phoneInput, fixedHeight(50, 7));
        }

        LinearLayout templateRow = settingRow();
        TextView templateLabel = label("템플릿");
        templateRow.addView(templateLabel, new LinearLayout.LayoutParams(dp(62),
                LinearLayout.LayoutParams.WRAP_CONTENT));
        selectedTemplateText = body("선택 안 함");
        selectedTemplateText.setSingleLine(true);
        selectedTemplateText.setEllipsize(TextUtils.TruncateAt.END);
        selectedTemplateText.setTextColor(getColor(R.color.text_primary));
        templateRow.addView(selectedTemplateText, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        templateEdit = button("수정", false);
        templateEdit.setTextSize(12f);
        templateEdit.setVisibility(View.GONE);
        templateEdit.setOnClickListener(v -> editSelectedTemplate());
        templateRow.addView(templateEdit, new LinearLayout.LayoutParams(dp(58), dp(36)));
        TextView templateArrow = title("›", 23f);
        templateArrow.setTextColor(getColor(R.color.text_muted));
        templateArrow.setGravity(Gravity.CENTER);
        templateRow.addView(templateArrow, new LinearLayout.LayoutParams(dp(26), dp(42)));
        templateRow.setOnClickListener(v -> openTemplateLibrary());
        root.addView(templateRow, topMargin(12));

        root.addView(label("문자 내용"), topMargin(15));
        bodyInput = input("보낼 내용을 입력해주세요.", true);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setMinLines(5);
        root.addView(bodyInput, fixedHeight(150, 7));

        LinearLayout attachmentRow = settingRow();
        attachmentStatus = body("이미지 첨부");
        attachmentStatus.setTextColor(getColor(R.color.text_primary));
        attachmentRow.addView(attachmentStatus, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        attachmentRemove = button("삭제", false);
        attachmentRemove.setTextSize(12f);
        attachmentRemove.setTextColor(getColor(R.color.danger));
        attachmentRemove.setVisibility(View.GONE);
        attachmentRemove.setOnClickListener(v -> clearAttachment());
        attachmentRow.addView(attachmentRemove, new LinearLayout.LayoutParams(dp(58), dp(36)));
        TextView attachmentArrow = title("›", 23f);
        attachmentArrow.setTextColor(getColor(R.color.text_muted));
        attachmentArrow.setGravity(Gravity.CENTER);
        attachmentRow.addView(attachmentArrow, new LinearLayout.LayoutParams(dp(26), dp(42)));
        attachmentRow.setOnClickListener(v -> pickImage());
        root.addView(attachmentRow, topMargin(9));

        attachmentPreview = new ImageView(this);
        attachmentPreview.setAdjustViewBounds(true);
        attachmentPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        attachmentPreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(118));
        previewParams.topMargin = dp(8);
        root.addView(attachmentPreview, previewParams);

        delayDaysInput = input("3", false);
        delayDaysInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        delayDaysInput.setText(String.valueOf(MessageAutomationStore.delayDays(this)));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(16), dp(9), dp(16), dp(9));
        footer.setBackgroundColor(getColor(R.color.surface_soft));

        Button schedule = button("후속 예약", false);
        schedule.setOnClickListener(v -> showScheduleDialog());
        footer.addView(schedule, new LinearLayout.LayoutParams(0, dp(50), 1f));

        Button sendNow = button("지금 보내기", true);
        sendNow.setOnClickListener(v -> sendNow());
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(0, dp(50), 1.3f);
        sendParams.leftMargin = dp(8);
        footer.addView(sendNow, sendParams);
        page.addView(footer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(68)));
        return page;
    }

    private LinearLayout settingRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(7), 0);
        row.setMinimumHeight(dp(50));
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        return row;
    }

    private void showScheduleDialog() {
        EditText days = input("3", false);
        days.setInputType(InputType.TYPE_CLASS_NUMBER);
        days.setText(delayDaysInput.getText().toString());
        days.setSelectAllOnFocus(true);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(dp(20), dp(8), dp(20), 0);
        wrapper.addView(days, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        new AlertDialog.Builder(this)
                .setTitle("후속문자 예약")
                .setMessage("몇 일 후에 보낼까요?")
                .setView(wrapper)
                .setNegativeButton("취소", null)
                .setPositiveButton("예약", (dialog, which) -> {
                    delayDaysInput.setText(days.getText().toString());
                    schedule();
                })
                .show();
    }

    private void openTemplateLibrary() {
        if (!FeatureEntitlementStore.hasMessageAccess(this)) {
            Toast.makeText(this, "문자 기능 이용 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            return;
        }
        startActivityForResult(new Intent(this, MessageTemplateLibraryActivity.class)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_SELECT_MODE, true), REQUEST_TEMPLATE);
    }

    private void editSelectedTemplate() {
        if (selectedTemplateId.isEmpty()) return;
        startActivityForResult(new Intent(this, MessageTemplateEditorActivity.class)
                .putExtra(MessageTemplateEditorActivity.EXTRA_TEMPLATE_ID, selectedTemplateId),
                REQUEST_TEMPLATE_EDIT);
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
        if (resultCode != RESULT_OK) return;
        if (requestCode == REQUEST_TEMPLATE && data != null) {
            selectedTemplateId = safe(data.getStringExtra(
                    MessageTemplateLibraryActivity.EXTRA_TEMPLATE_ID));
            applySelectedTemplate();
            return;
        }
        if (requestCode == REQUEST_TEMPLATE_EDIT) {
            applySelectedTemplate();
            return;
        }
        if (requestCode == REQUEST_IMAGE && data != null) {
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

    private void applySelectedTemplate() {
        MessageTemplateStore.Template template = MessageTemplateStore.get(this, selectedTemplateId);
        if (template == null) {
            selectedTemplateId = "";
            selectedTemplateText.setText("선택 안 함");
            templateEdit.setVisibility(View.GONE);
            return;
        }
        selectedTemplateText.setText(template.name);
        templateEdit.setVisibility(View.VISIBLE);
        setAttachment(template.imageRef, false);
        applyTemplate(template.body);
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
                : "이미지 첨부");
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
        Toast.makeText(this, decision.reason + " 고객 상세의 문자 발송 설정을 확인해주세요.",
                Toast.LENGTH_LONG).show();
        return true;
    }

    private String validatedBody() {
        if (!FeatureEntitlementStore.hasMessageAccess(this)) {
            Toast.makeText(this, "문자 기능 이용 권한이 필요합니다.", Toast.LENGTH_LONG).show();
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
        Customer customer = linkedCustomer;
        if (customer == null && customerId > 0L) {
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
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setSingleLine(!multiline);
        return input;
    }

    private TextView label(String value) {
        TextView label = title(value, 13f);
        label.setTextColor(getColor(R.color.text_secondary));
        return label;
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

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        button.setMinWidth(0);
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

    private LinearLayout.LayoutParams fixedHeight(int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(top);
        return params;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
