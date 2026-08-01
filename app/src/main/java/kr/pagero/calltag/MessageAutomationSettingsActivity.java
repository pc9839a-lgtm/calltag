package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
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

/** 통화 후·부재중·후속 예약 세 가지로만 구성한 자동문자 설정. */
public final class MessageAutomationSettingsActivity extends Activity {
    private static final int REQUEST_SMS = 8201;
    private static final int REQUEST_CONNECTED = 8301;
    private static final int REQUEST_MISSED = 8302;
    private static final int REQUEST_FOLLOW_UP = 8303;

    private Switch enabled;
    private Switch connected;
    private Switch missed;
    private Switch delayed;
    private Switch businessHours;
    private EditText delayDays;
    private EditText cooldownHours;
    private EditText startHour;
    private EditText endHour;
    private Spinner lineSpinner;
    private TextView connectedTemplate;
    private TextView missedTemplate;
    private TextView followTemplate;
    private final List<SimProfileManager.Profile> profiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageAutomationStore.ensureDefaults(this);
        MessageTemplateStore.ensureDefaults(this);
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
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("통화 후 자동문자", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        enabled = switchView("자동문자 사용");
        root.addView(enabled, topMargin(18));

        connected = new Switch(this);
        connectedTemplate = addScenario(
                root, "통화 후", connected, REQUEST_CONNECTED, 16);

        missed = new Switch(this);
        missedTemplate = addScenario(
                root, "부재중", missed, REQUEST_MISSED, 12);

        delayed = new Switch(this);
        followTemplate = addScenario(
                root, "후속 예약", delayed, REQUEST_FOLLOW_UP, 12);

        LinearLayout delayRow = new LinearLayout(this);
        delayRow.setGravity(Gravity.CENTER_VERTICAL);
        delayRow.setPadding(dp(16), 0, dp(16), dp(14));
        delayDays = numberInput("3");
        delayRow.addView(delayDays, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView suffix = body("일 후 발송");
        suffix.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams suffixParams = new LinearLayout.LayoutParams(
                dp(108), dp(48));
        suffixParams.leftMargin = dp(10);
        delayRow.addView(suffix, suffixParams);
        root.addView(delayRow, matchWrap());

        root.addView(sectionLabel("발송 조건"), topMargin(24));

        businessHours = switchView("업무시간에만 보내기");
        root.addView(businessHours, topMargin(10));

        LinearLayout hoursRow = new LinearLayout(this);
        hoursRow.setGravity(Gravity.CENTER_VERTICAL);
        startHour = numberInput("9");
        hoursRow.addView(startHour, new LinearLayout.LayoutParams(0, dp(50), 1f));
        TextView from = body("시부터");
        from.setGravity(Gravity.CENTER);
        hoursRow.addView(from, new LinearLayout.LayoutParams(dp(62), dp(50)));
        endHour = numberInput("20");
        hoursRow.addView(endHour, new LinearLayout.LayoutParams(0, dp(50), 1f));
        TextView until = body("시까지");
        until.setGravity(Gravity.CENTER);
        hoursRow.addView(until, new LinearLayout.LayoutParams(dp(66), dp(50)));
        root.addView(hoursRow, topMargin(8));

        LinearLayout cooldownRow = new LinearLayout(this);
        cooldownRow.setGravity(Gravity.CENTER_VERTICAL);
        cooldownHours = numberInput("24");
        cooldownRow.addView(cooldownHours, new LinearLayout.LayoutParams(0, dp(50), 1f));
        TextView cooldownLabel = body("시간 동안 같은 번호 중복 발송 방지");
        cooldownLabel.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cooldownParams = new LinearLayout.LayoutParams(
                0, dp(50), 2.4f);
        cooldownParams.leftMargin = dp(10);
        cooldownRow.addView(cooldownLabel, cooldownParams);
        root.addView(cooldownRow, topMargin(8));

        root.addView(sectionLabel("발송 회선"), topMargin(22));
        lineSpinner = new Spinner(this);
        lineSpinner.setBackgroundResource(R.drawable.bg_secondary_button);
        lineSpinner.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        lineParams.topMargin = dp(8);
        root.addView(lineSpinner, lineParams);
        setupLines();

        Button save = button("설정 저장", true);
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        saveParams.topMargin = dp(26);
        root.addView(save, saveParams);
        return scroll;
    }

    private TextView addScenario(LinearLayout root, String label,
                                 Switch toggle, int requestCode, int topMargin) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);

        toggle.setText(label);
        toggle.setTextColor(getColor(R.color.text_primary));
        toggle.setTextSize(16f);
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(toggle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));

        LinearLayout templateRow = new LinearLayout(this);
        templateRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView selected = body("");
        selected.setTextColor(getColor(R.color.text_primary));
        selected.setTextSize(14f);
        selected.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        templateRow.addView(selected, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button change = button("변경", false);
        change.setOnClickListener(v -> openTemplateSelector(requestCode));
        LinearLayout.LayoutParams changeParams = new LinearLayout.LayoutParams(dp(92), dp(46));
        changeParams.leftMargin = dp(10);
        templateRow.addView(change, changeParams);
        card.addView(templateRow, topMargin(4));
        root.addView(card, topMargin(topMargin));
        return selected;
    }

    private void openTemplateSelector(int requestCode) {
        startActivityForResult(new Intent(this, MessageTemplateLibraryActivity.class)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_SELECT_MODE, true), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode < REQUEST_CONNECTED || requestCode > REQUEST_FOLLOW_UP) return;

        String templateId = safe(data.getStringExtra(
                MessageTemplateLibraryActivity.EXTRA_TEMPLATE_ID));
        MessageTemplateStore.Template template = MessageTemplateStore.get(this, templateId);
        if (template == null) return;
        if (!safe(template.imageRef).isEmpty()) {
            Toast.makeText(this,
                    "이미지가 있는 템플릿은 자동 발송에 사용할 수 없습니다.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (requestCode == REQUEST_CONNECTED) {
            MessageTemplateStore.setDefault(
                    this, MessageTemplateStore.PURPOSE_INCOMING, template.id);
            MessageTemplateStore.setDefault(
                    this, MessageTemplateStore.PURPOSE_OUTGOING, template.id);
        } else if (requestCode == REQUEST_MISSED) {
            MessageTemplateStore.setDefault(
                    this, MessageTemplateStore.PURPOSE_MISSED, template.id);
        } else {
            MessageTemplateStore.setDefault(
                    this, MessageTemplateStore.PURPOSE_FOLLOW_UP, template.id);
        }
        loadTemplateLabels();
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
        delayDays.setText(String.valueOf(MessageAutomationStore.delayDays(this)));
        cooldownHours.setText(String.valueOf(MessageAutomationStore.cooldownHours(this)));
        startHour.setText(String.valueOf(MessageAutomationStore.startHour(this)));
        endHour.setText(String.valueOf(MessageAutomationStore.endHour(this)));
        loadTemplateLabels();

        int selected = MessageAutomationStore.selectedSubscriptionId(this);
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).subscriptionId == selected) {
                lineSpinner.setSelection(i);
                break;
            }
        }
    }

    private void loadTemplateLabels() {
        connectedTemplate.setText("템플릿 · " + MessageTemplateStore.defaultName(
                this, MessageTemplateStore.PURPOSE_INCOMING));
        missedTemplate.setText("템플릿 · " + MessageTemplateStore.defaultName(
                this, MessageTemplateStore.PURPOSE_MISSED));
        followTemplate.setText("템플릿 · " + MessageTemplateStore.defaultName(
                this, MessageTemplateStore.PURPOSE_FOLLOW_UP));
    }

    private void save() {
        if (!FeatureEntitlementStore.hasMessageAccess(this)) {
            Toast.makeText(this, "문자 기능 이용 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            return;
        }
        MessageAutomationStore.setEnabled(this, enabled.isChecked());
        MessageAutomationStore.setConnectedEnabled(this, connected.isChecked());
        MessageAutomationStore.setMissedEnabled(this, missed.isChecked());
        MessageAutomationStore.setDelayedEnabled(this, delayed.isChecked());
        MessageAutomationStore.setBusinessHoursEnabled(this, businessHours.isChecked());
        MessageAutomationStore.setDelayDays(this, parse(delayDays, 3));
        MessageAutomationStore.setCooldownHours(this, parse(cooldownHours, 24));
        MessageAutomationStore.setBusinessHours(
                this, parse(startHour, 9), parse(endHour, 20));
        if (!profiles.isEmpty() && lineSpinner.getSelectedItemPosition() < profiles.size()) {
            MessageAutomationStore.setSelectedSubscriptionId(
                    this, profiles.get(lineSpinner.getSelectedItemPosition()).subscriptionId);
        }
        if ((connected.isChecked() || missed.isChecked() || delayed.isChecked())
                && checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
        }
        Toast.makeText(this, "저장했습니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private int parse(EditText input, int fallback) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Switch switchView(String title) {
        Switch view = new Switch(this);
        view.setText(title);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTextSize(16f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(16), dp(8), dp(12), dp(8));
        view.setMinHeight(dp(62));
        view.setBackgroundResource(R.drawable.bg_card);
        return view;
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

    private TextView sectionLabel(String value) {
        TextView view = title(value, 17f);
        return view;
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
