package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
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

public final class MessageAutomationSettingsActivity extends Activity {
    private static final int REQUEST_SMS = 8201;

    private Switch enabled;
    private Switch connected;
    private Switch missed;
    private Switch delayed;
    private Switch businessHours;
    private EditText connectedTemplate;
    private EditText missedTemplate;
    private EditText delayedTemplate;
    private EditText delayDays;
    private EditText cooldownHours;
    private EditText startHour;
    private EditText endHour;
    private Spinner lineSpinner;
    private final List<SimProfileManager.Profile> profiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageAutomationStore.ensureDefaults(this);
        setContentView(buildContent());
        load();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(44));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("자동 발송 설정", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        TextView plan = body(FeatureEntitlementStore.planLabel(this));
        plan.setBackgroundResource(R.drawable.bg_card);
        plan.setPadding(dp(18), dp(14), dp(18), dp(14));
        root.addView(plan, topMargin(20));

        enabled = switchView("문자자동화 사용", "자동·수동·예약 문자 기능 전체 켜기");
        root.addView(enabled, topMargin(16));

        connected = switchView("통화 종료 후 자동 발송", "연결된 수신·발신 통화 종료 후 발송");
        root.addView(connected, topMargin(12));
        connectedTemplate = multilineInput("통화 종료 후 보낼 템플릿");
        root.addView(connectedTemplate, topMargin(8));

        missed = switchView("부재중·거절 자동 발송", "받지 못한 수신 전화에 자동 안내");
        root.addView(missed, topMargin(18));
        missedTemplate = multilineInput("부재중일 때 보낼 템플릿");
        root.addView(missedTemplate, topMargin(8));

        delayed = switchView("후속 문자 자동 예약", "통화 종료 후 지정한 날짜가 지나면 발송");
        root.addView(delayed, topMargin(18));
        delayedTemplate = multilineInput("후속문자 템플릿");
        root.addView(delayedTemplate, topMargin(8));

        LinearLayout delayRow = new LinearLayout(this);
        delayRow.setGravity(Gravity.CENTER_VERTICAL);
        delayDays = numberInput("3");
        delayRow.addView(delayDays, new LinearLayout.LayoutParams(0, dp(52), 1f));
        TextView daySuffix = body("일 후 발송");
        daySuffix.setGravity(Gravity.CENTER);
        delayRow.addView(daySuffix, new LinearLayout.LayoutParams(dp(100), dp(52)));
        cooldownHours = numberInput("24");
        delayRow.addView(cooldownHours, new LinearLayout.LayoutParams(0, dp(52), 1f));
        TextView cooldownSuffix = body("시간 중복 방지");
        cooldownSuffix.setGravity(Gravity.CENTER);
        delayRow.addView(cooldownSuffix, new LinearLayout.LayoutParams(dp(118), dp(52)));
        root.addView(delayRow, topMargin(8));

        businessHours = switchView("업무시간에만 자동 발송", "시간 밖 통화는 자동문자를 보내지 않음");
        root.addView(businessHours, topMargin(18));
        LinearLayout hoursRow = new LinearLayout(this);
        hoursRow.setGravity(Gravity.CENTER_VERTICAL);
        startHour = numberInput("9");
        hoursRow.addView(startHour, new LinearLayout.LayoutParams(0, dp(52), 1f));
        TextView from = body("시부터");
        from.setGravity(Gravity.CENTER);
        hoursRow.addView(from, new LinearLayout.LayoutParams(dp(66), dp(52)));
        endHour = numberInput("20");
        hoursRow.addView(endHour, new LinearLayout.LayoutParams(0, dp(52), 1f));
        TextView until = body("시까지");
        until.setGravity(Gravity.CENTER);
        hoursRow.addView(until, new LinearLayout.LayoutParams(dp(72), dp(52)));
        root.addView(hoursRow, topMargin(8));

        root.addView(label("발송 회선"), topMargin(20));
        lineSpinner = new Spinner(this);
        lineSpinner.setBackgroundResource(R.drawable.bg_secondary_button);
        lineSpinner.setPadding(dp(12), 0, dp(12), 0);
        root.addView(lineSpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)) {{ topMargin = dp(8); }});
        setupLines();

        TextView variableHelp = body("사용 가능: " + MessageTemplateEngine.supportedVariablesLabel()
                + "\n치환할 정보가 없거나 지원하지 않는 변수가 남으면 발송하지 않습니다.");
        root.addView(variableHelp, topMargin(14));

        Button save = button("설정 저장", true);
        save.setOnClickListener(v -> save());
        root.addView(save, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58)) {{ topMargin = dp(24); }});
        return scroll;
    }

    private void setupLines() {
        profiles.clear();
        profiles.addAll(SimProfileManager.activeProfiles(this));
        List<String> labels = new ArrayList<>();
        for (SimProfileManager.Profile profile : profiles) labels.add(profile.label());
        if (labels.isEmpty()) labels.add("기본 문자 회선");
        lineSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void load() {
        enabled.setChecked(MessageAutomationStore.isEnabled(this));
        connected.setChecked(MessageAutomationStore.connectedEnabled(this));
        missed.setChecked(MessageAutomationStore.missedEnabled(this));
        delayed.setChecked(MessageAutomationStore.delayedEnabled(this));
        businessHours.setChecked(MessageAutomationStore.businessHoursEnabled(this));
        connectedTemplate.setText(MessageAutomationStore.connectedTemplate(this));
        missedTemplate.setText(MessageAutomationStore.missedTemplate(this));
        delayedTemplate.setText(MessageAutomationStore.delayedTemplate(this));
        delayDays.setText(String.valueOf(MessageAutomationStore.delayDays(this)));
        cooldownHours.setText(String.valueOf(MessageAutomationStore.cooldownHours(this)));
        startHour.setText(String.valueOf(MessageAutomationStore.startHour(this)));
        endHour.setText(String.valueOf(MessageAutomationStore.endHour(this)));
        int selected = MessageAutomationStore.selectedSubscriptionId(this);
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).subscriptionId == selected) {
                lineSpinner.setSelection(i);
                break;
            }
        }
    }

    private void save() {
        if (!FeatureEntitlementStore.hasMessageAccess(this)) {
            Toast.makeText(this, "문자자동화 이용권이 필요합니다.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!validateTemplateField(connectedTemplate, "통화 종료 템플릿")) return;
        if (!validateTemplateField(missedTemplate, "부재중 템플릿")) return;
        if (!validateTemplateField(delayedTemplate, "후속문자 템플릿")) return;

        MessageAutomationStore.setEnabled(this, enabled.isChecked());
        MessageAutomationStore.setConnectedEnabled(this, connected.isChecked());
        MessageAutomationStore.setMissedEnabled(this, missed.isChecked());
        MessageAutomationStore.setDelayedEnabled(this, delayed.isChecked());
        MessageAutomationStore.setBusinessHoursEnabled(this, businessHours.isChecked());
        MessageAutomationStore.setConnectedTemplate(this, connectedTemplate.getText().toString());
        MessageAutomationStore.setMissedTemplate(this, missedTemplate.getText().toString());
        MessageAutomationStore.setDelayedTemplate(this, delayedTemplate.getText().toString());
        MessageAutomationStore.setDelayDays(this, parse(delayDays, 3));
        MessageAutomationStore.setCooldownHours(this, parse(cooldownHours, 24));
        MessageAutomationStore.setBusinessHours(this, parse(startHour, 9), parse(endHour, 20));
        if (!profiles.isEmpty() && lineSpinner.getSelectedItemPosition() < profiles.size()) {
            MessageAutomationStore.setSelectedSubscriptionId(
                    this, profiles.get(lineSpinner.getSelectedItemPosition()).subscriptionId);
        }
        if ((connected.isChecked() || missed.isChecked() || delayed.isChecked())
                && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
        }
        Toast.makeText(this, "자동 발송 설정을 저장했습니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean validateTemplateField(EditText input, String label) {
        MessageTemplateEngine.ValidationResult result = MessageTemplateEngine.validateTemplate(
                input.getText().toString());
        if (!result.isValid()) {
            input.requestFocus();
            Toast.makeText(this,
                    label + "에 지원하지 않는 변수가 있습니다: "
                            + MessageTemplateEngine.describeVariables(result.unsupportedVariables),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        input.setText(result.normalizedTemplate);
        input.setSelection(input.getText().length());
        return true;
    }

    private int parse(EditText input, int fallback) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Switch switchView(String title, String subtitle) {
        Switch view = new Switch(this);
        view.setText(title + "\n" + subtitle);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTextSize(15f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(16), dp(10), dp(12), dp(10));
        view.setMinHeight(dp(72));
        view.setBackgroundResource(R.drawable.bg_card);
        return view;
    }

    private EditText multilineInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setTextSize(14f);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setBackgroundResource(R.drawable.bg_secondary_button);
        input.setMinLines(4);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        return input;
    }

    private EditText numberInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextSize(15f);
        input.setGravity(Gravity.CENTER);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setBackgroundResource(R.drawable.bg_secondary_button);
        return input;
    }

    private TextView label(String value) {
        TextView view = title(value, 15f);
        view.setTextColor(getColor(R.color.text_secondary));
        return view;
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
