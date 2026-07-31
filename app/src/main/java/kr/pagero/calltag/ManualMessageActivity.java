package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class ManualMessageActivity extends Activity {
    public static final String EXTRA_USE_TEMPLATE = "use_template";
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_CUSTOMER_ID = "customer_id";

    private static final int REQUEST_SMS = 8101;
    private static final int REQUEST_TEMPLATE = 8102;

    private EditText phoneInput;
    private EditText bodyInput;
    private EditText delayDaysInput;
    private TextView selectedTemplateText;
    private long customerId;
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

        TextView previewNotice = body("템플릿 선택 시 현재 고객·계정·일정 정보로 치환합니다. 지원하지 않거나 치환되지 않은 변수는 발송 전에 차단됩니다.");
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

        TextView notice = body("실제 문자요금은 선택한 SIM·eSIM 회선의 통신사 요금제에 따라 부과됩니다.");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TEMPLATE || resultCode != RESULT_OK || data == null) return;
        String body = data.getStringExtra(MessageTemplateLibraryActivity.EXTRA_TEMPLATE_BODY);
        String name = data.getStringExtra(MessageTemplateLibraryActivity.EXTRA_TEMPLATE_NAME);
        selectedTemplateText.setText((name == null || name.trim().isEmpty())
                ? "선택한 템플릿" : name);
        applyTemplate(body == null ? "" : body);
    }

    private void applyTemplate(String template) {
        MessageTemplateEngine.RenderResult rendered = render(template);
        bodyInput.setText(rendered.body);
        bodyInput.setSelection(bodyInput.getText().length());
        if (!rendered.unsupportedVariables.isEmpty()) {
            Toast.makeText(this,
                    "지원하지 않는 변수가 있습니다: "
                            + MessageTemplateEngine.describeVariables(rendered.unsupportedVariables),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void sendNow() {
        String body = validatedBody();
        if (body == null || blockedByPolicy(MessageAutomationManager.TRIGGER_MANUAL)) return;
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pendingSend = true;
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
            return;
        }
        long id = SmsSender.queueAndSend(
                this, customerId, 0L, phoneInput.getText().toString(), body,
                MessageAutomationManager.TRIGGER_MANUAL,
                MessageAutomationStore.selectedSubscriptionId(this));
        Toast.makeText(this, "문자 발송을 요청했습니다. 내역에서 결과를 확인하세요.", Toast.LENGTH_LONG).show();
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
        try {
            long id = store.createJob(
                    customerId, 0L, phoneInput.getText().toString(), body,
                    MessageAutomationManager.TRIGGER_DELAYED,
                    MessageLogStore.STATUS_SCHEDULED, when,
                    MessageAutomationStore.selectedSubscriptionId(this));
            MessageScheduler.schedule(this, id, when);
        } finally {
            store.close();
        }
        Toast.makeText(this, days + "일 후 후속문자로 예약했습니다.", Toast.LENGTH_LONG).show();
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
            Toast.makeText(this,
                    "지원하지 않는 변수가 있습니다: "
                            + MessageTemplateEngine.describeVariables(rendered.unsupportedVariables),
                    Toast.LENGTH_LONG).show();
            return null;
        }
        if (!rendered.unresolvedVariables.isEmpty()) {
            Toast.makeText(this,
                    "치환되지 않은 변수가 있습니다: "
                            + MessageTemplateEngine.describeVariables(rendered.unresolvedVariables)
                            + ". 고객·계정·일정 정보를 확인해주세요.",
                    Toast.LENGTH_LONG).show();
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
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
