package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
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

/** 통화 직후와 부재중에 즉시 보내는 문자·사진 MMS를 설정한다. */
public final class PostCallAutomationActivity extends Activity {
    private static final int REQUEST_CONNECTED = 8801;
    private static final int REQUEST_MISSED = 8802;
    private static final int REQUEST_SMS = 8803;

    private Switch master;
    private Switch connected;
    private Switch missed;
    private TextView connectedTemplate;
    private TextView missedTemplate;
    private TextView commonSummary;

    private boolean businessHours;
    private int startHour;
    private int endHour;
    private int cooldownHours;
    private int subscriptionId;
    private final List<SimProfileManager.Profile> profiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageAutomationStore.ensureDefaults(this);
        MessageTemplateStore.ensureDefaults(this);
        profiles.addAll(SimProfileManager.activeProfiles(this));
        businessHours = MessageAutomationStore.businessHoursEnabled(this);
        startHour = MessageAutomationStore.startHour(this);
        endHour = MessageAutomationStore.endHour(this);
        cooldownHours = MessageAutomationStore.cooldownHours(this);
        subscriptionId = MessageAutomationStore.selectedSubscriptionId(this);
        setContentView(buildContent());
        render();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = title("‹", 30f);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView screenTitle = title("통화 후 자동문자", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(6);
        header.addView(screenTitle, titleParams);
        root.addView(header, matchWrap());

        root.addView(body("통화가 끝난 직후 보내는 문자와 부재중 문자를 설정합니다. "
                + "사진이 포함된 템플릿은 기본 메시지 앱을 열지 않고 MMS로 자동 발송합니다."), top(12));

        master = switchCard("통화 직후 자동문자 사용", "통화 후·부재중 문자 전체 켜기");
        root.addView(master, top(18));

        connected = new Switch(this);
        connectedTemplate = scenario(root, "통화 후 문자", "수신·발신 통화가 연결된 뒤 즉시 자동발송",
                connected, REQUEST_CONNECTED);
        missed = new Switch(this);
        missedTemplate = scenario(root, "부재중 문자", "받지 못하거나 거절한 전화에 즉시 자동발송",
                missed, REQUEST_MISSED);

        LinearLayout common = card();
        common.setClickable(true);
        common.setFocusable(true);
        common.setOnClickListener(v -> showCommonSettings());
        common.addView(title("발송 조건·회선", 16f), matchWrap());
        commonSummary = body("");
        common.addView(commonSummary, top(6));
        root.addView(common, top(12));

        Button save = button("저장", true);
        save.setOnClickListener(v -> save());
        root.addView(save, fixedTop(52, 22));
        return scroll;
    }

    private TextView scenario(LinearLayout root, String label, String description,
                              Switch toggle, int requestCode) {
        LinearLayout card = card();
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(title(label, 16f), matchWrap());
        texts.addView(body(description), top(4));
        row.addView(texts, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(toggle);
        card.addView(row, matchWrap());

        TextView selected = body("");
        selected.setTextColor(getColor(R.color.text_primary));
        selected.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selected.setSingleLine(true);
        card.addView(selected, top(12));

        Button choose = button("템플릿 선택", false);
        choose.setOnClickListener(v -> openTemplate(requestCode));
        card.addView(choose, fixedTop(48, 10));
        root.addView(card, top(10));
        return selected;
    }

    private void openTemplate(int requestCode) {
        String purpose = requestCode == REQUEST_CONNECTED
                ? MessageTemplateStore.PURPOSE_INCOMING
                : MessageTemplateStore.PURPOSE_MISSED;
        startActivityForResult(new Intent(this, MessageTemplateLibraryActivity.class)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_SELECT_MODE, true)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_PURPOSE_FILTER, purpose), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode != REQUEST_CONNECTED && requestCode != REQUEST_MISSED) return;
        String id = safe(data.getStringExtra(MessageTemplateLibraryActivity.EXTRA_TEMPLATE_ID));
        MessageTemplateStore.Template template = MessageTemplateStore.get(this, id);
        if (template == null) return;
        if (requestCode == REQUEST_CONNECTED) {
            AutomationTemplateSelectionStore.set(
                    this, MessageTemplateStore.PURPOSE_INCOMING, id);
            AutomationTemplateSelectionStore.set(
                    this, MessageTemplateStore.PURPOSE_OUTGOING, id);
        } else {
            AutomationTemplateSelectionStore.set(
                    this, MessageTemplateStore.PURPOSE_MISSED, id);
        }
        Toast.makeText(this,
                safe(template.imageRef).isEmpty()
                        ? "텍스트 자동문자 템플릿을 선택했습니다."
                        : "사진 포함 MMS 자동발송 템플릿을 선택했습니다.",
                Toast.LENGTH_SHORT).show();
        render();
    }

    private void showCommonSettings() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(4), dp(18), dp(4));

        Switch business = switchCard("업무시간에만 보내기", "설정한 시간 밖에는 발송하지 않음");
        business.setChecked(businessHours);
        panel.addView(business, matchWrap());

        panel.addView(body("발송 시간"), top(14));
        LinearLayout hourRow = new LinearLayout(this);
        hourRow.setGravity(Gravity.CENTER_VERTICAL);
        EditText start = number(String.valueOf(startHour));
        EditText end = number(String.valueOf(endHour));
        hourRow.addView(start, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView divider = body("시  ~");
        divider.setGravity(Gravity.CENTER);
        hourRow.addView(divider, new LinearLayout.LayoutParams(dp(58), dp(48)));
        hourRow.addView(end, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView suffix = body("시");
        suffix.setGravity(Gravity.CENTER);
        hourRow.addView(suffix, new LinearLayout.LayoutParams(dp(34), dp(48)));
        panel.addView(hourRow, top(6));

        panel.addView(body("같은 번호 중복 발송 방지"), top(14));
        EditText cooldown = number(String.valueOf(cooldownHours));
        panel.addView(cooldown, fixedTop(48, 6));

        panel.addView(body("발송 회선"), top(14));
        Spinner line = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (SimProfileManager.Profile profile : profiles) labels.add(profile.label());
        if (labels.isEmpty()) labels.add("기본 문자 회선");
        line.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
        line.setBackgroundResource(R.drawable.bg_secondary_button);
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).subscriptionId == subscriptionId) line.setSelection(i);
        }
        panel.addView(line, fixedTop(50, 6));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("발송 조건·회선")
                .setView(panel)
                .setNegativeButton("취소", null)
                .setPositiveButton("적용", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int startValue = clamp(parse(start, 9), 0, 23);
                    int endValue = clamp(parse(end, 20), 1, 24);
                    if (business.isChecked() && startValue >= endValue) {
                        Toast.makeText(this, "종료 시간은 시작 시간보다 늦어야 합니다.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    businessHours = business.isChecked();
                    startHour = startValue;
                    endHour = endValue;
                    cooldownHours = clamp(parse(cooldown, 24), 1, 720);
                    if (!profiles.isEmpty() && line.getSelectedItemPosition() < profiles.size()) {
                        subscriptionId = profiles.get(line.getSelectedItemPosition()).subscriptionId;
                    }
                    render();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void save() {
        MessageAutomationStore.setEnabled(this, master.isChecked());
        MessageAutomationStore.setConnectedEnabled(this, connected.isChecked());
        MessageAutomationStore.setMissedEnabled(this, missed.isChecked());
        MessageAutomationStore.setBusinessHoursEnabled(this, businessHours);
        MessageAutomationStore.setBusinessHours(this, startHour, endHour);
        MessageAutomationStore.setCooldownHours(this, cooldownHours);
        MessageAutomationStore.setSelectedSubscriptionId(this, subscriptionId);
        if (master.isChecked()
                && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
        }
        Toast.makeText(this, "통화 후 자동문자 설정을 저장했습니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void render() {
        if (master == null) return;
        master.setChecked(MessageAutomationStore.isEnabled(this));
        connected.setChecked(MessageAutomationStore.connectedEnabled(this));
        missed.setChecked(MessageAutomationStore.missedEnabled(this));
        connectedTemplate.setText("선택된 템플릿 · " + AutomationTemplateSelectionStore.name(
                this, MessageTemplateStore.PURPOSE_INCOMING));
        missedTemplate.setText("선택된 템플릿 · " + AutomationTemplateSelectionStore.name(
                this, MessageTemplateStore.PURPOSE_MISSED));
        commonSummary.setText((businessHours
                ? startHour + "시~" + endHour + "시" : "시간 제한 없음")
                + " · " + cooldownHours + "시간 중복 방지"
                + " · " + selectedLineLabel());
    }

    private String selectedLineLabel() {
        for (SimProfileManager.Profile profile : profiles) {
            if (profile.subscriptionId == subscriptionId) return profile.label();
        }
        return "기본 회선";
    }

    private Switch switchCard(String label, String description) {
        Switch value = new Switch(this);
        value.setText(label + "\n" + description);
        value.setTextSize(15f);
        value.setTextColor(getColor(R.color.text_primary));
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setPadding(dp(16), dp(12), dp(12), dp(12));
        value.setBackgroundResource(R.drawable.bg_card);
        return value;
    }

    private LinearLayout card() {
        LinearLayout value = new LinearLayout(this);
        value.setOrientation(LinearLayout.VERTICAL);
        value.setPadding(dp(16), dp(15), dp(16), dp(15));
        value.setBackgroundResource(R.drawable.bg_card);
        return value;
    }

    private EditText number(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setTextColor(getColor(R.color.text_primary));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), 0, dp(14), 0);
        return input;
    }

    private TextView title(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView body(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(14f);
        view.setTextColor(getColor(R.color.text_secondary));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.2f);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private int parse(EditText input, int fallback) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
