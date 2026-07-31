package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MessageAutomationSettingsActivity extends Activity {
    private static final int REQUEST_SMS = 8201;
    private static final int REQUEST_INCOMING = 8301;
    private static final int REQUEST_OUTGOING = 8302;
    private static final int REQUEST_MISSED = 8303;
    private static final int REQUEST_FOLLOW_UP = 8304;

    private final Map<String, TextView> defaultLabels = new LinkedHashMap<>();
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
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("자동 발송 설정", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        TextView plan = body(FeatureEntitlementStore.planLabel(this));
        plan.setBackgroundResource(R.drawable.bg_card);
        plan.setPadding(dp(18), dp(14), dp(18), dp(14));
        root.addView(plan, topMargin(20));

        enabled = switchView("문자자동화 사용", "자동·수동·예약 문자 기능 전체 켜기");
        root.addView(enabled, topMargin(16));

        Button library = button("템플릿 보관함 관리", false);
        library.setOnClickListener(v -> startActivity(
                new Intent(this, MessageTemplateLibraryActivity.class)));
        LinearLayout.LayoutParams libraryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        libraryParams.topMargin = dp(14);
        root.addView(library, libraryParams);

        root.addView(label("기본 템플릿"), topMargin(24));
        root.addView(body("수신·발신·부재중·후속 상황별로 보관함의 템플릿을 연결합니다."), topMargin(6));
        addDefaultSelector(root, "수신 통화", MessageTemplateStore.PURPOSE_INCOMING, REQUEST_INCOMING);
        addDefaultSelector(root, "발신 통화", MessageTemplateStore.PURPOSE_OUTGOING, REQUEST_OUTGOING);
        addDefaultSelector(root, "부재중", MessageTemplateStore.PURPOSE_MISSED, REQUEST_MISSED);
        addDefaultSelector(root, "후속문자", MessageTemplateStore.PURPOSE_FOLLOW_UP, REQUEST_FOLLOW_UP);

        connected = switchView("통화 종료 후 자동 발송", "수신·발신 통화별 기본 템플릿으로 발송");
        root.addView(connected, topMargin(22));
        missed = switchView("부재중·거절 자동 발송", "받지 못한 수신 전화에 기본 템플릿 발송");
        root.addView(missed, topMargin(12));
        delayed = switchView("후속문자 자동 예약", "통화 종료 후 지정한 시점에 후속 템플릿 발송");
        root.addView(delayed, topMargin(12));

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
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        lineParams.topMargin = dp(8);
        root.addView(lineSpinner, lineParams);
        setupLines();

        TextView variableHelp = body("사용 가능: " + MessageTemplateEngine.supportedVariablesLabel()
                + "\n템플릿 저장과 실제 발송 직전에 다시 검사합니다.");
        root.addView(variableHelp, topMargin(14));

        Button save = button("설정 저장", true);
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        saveParams.topMargin = dp(24);
        root.addView(save, saveParams);
        return scroll;
    }

    private void addDefaultSelector(LinearLayout root, String titleText,
                                    String purpose, int requestCode) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setBackgroundResource(R.drawable.bg_card);
        TextView title = title(titleText, 15f);
        row.addView(title, matchWrap());
        TextView selected = body("");
        selected.setTextColor(getColor(R.color.text_primary));
        row.addView(selected, topMargin(5));
        defaultLabels.put(purpose, selected);
        Button choose = button("템플릿 선택", false);
        choose.setOnClickListener(v -> openDefaultSelector(purpose, requestCode));
        LinearLayout.LayoutParams chooseParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        chooseParams.topMargin = dp(8);
        row.addView(choose, chooseParams);
        root.addView(row, topMargin(10));
    }

    private void openDefaultSelector(String purpose, int requestCode) {
        startActivityForResult(new Intent(this, MessageTemplateLibraryActivity.class)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_SELECT_MODE, true)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_PURPOSE_FILTER, purpose)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_DEFAULT_PURPOSE, purpose), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode >= REQUEST_INCOMING
                && requestCode <= REQUEST_FOLLOW_UP) loadDefaultLabels();
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
        loadDefaultLabels();
        int selected = MessageAutomationStore.selectedSubscriptionId(this);
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).subscriptionId == selected) {
                lineSpinner.setSelection(i);
                break;
            }
        }
    }

    private void loadDefaultLabels() {
        for (Map.Entry<String, TextView> entry : defaultLabels.entrySet()) {
            entry.getValue().setText(MessageTemplateStore.defaultName(this, entry.getKey()));
        }
    }

    private void save() {
        if (!FeatureEntitlementStore.hasMessageAccess(this)) {
            Toast.makeText(this, "문자자동화 이용권이 필요합니다.", Toast.LENGTH_LONG).show();
            return;
        }
        MessageAutomationStore.setEnabled(this, enabled.isChecked());
        MessageAutomationStore.setConnectedEnabled(this, connected.isChecked());
        MessageAutomationStore.setMissedEnabled(this, missed.isChecked());
        MessageAutomationStore.setDelayedEnabled(this, delayed.isChecked());
        MessageAutomationStore.setBusinessHoursEnabled(this, businessHours.isChecked());
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
