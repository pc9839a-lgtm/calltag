package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
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

/** 자동문자의 발송 유형·템플릿·후속 시점을 한 화면에서 설정한다. */
public final class MessageAutomationSettingsActivity extends Activity {
    private static final int REQUEST_SMS = 8201;
    private static final int REQUEST_CONNECTED = 8301;
    private static final int REQUEST_MISSED = 8302;
    private static final int REQUEST_FOLLOW_UP = 8303;

    private Switch enabled;
    private Switch connected;
    private Switch missed;
    private Switch delayed;
    private TextView connectedTemplate;
    private TextView missedTemplate;
    private TextView followTemplate;

    private boolean businessHoursValue;
    private int delayDaysValue;
    private int cooldownHoursValue;
    private int startHourValue;
    private int endHourValue;
    private int selectedSubscriptionId;
    private final List<SimProfileManager.Profile> profiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageAutomationStore.ensureDefaults(this);
        MessageTemplateStore.ensureDefaults(this);
        profiles.addAll(SimProfileManager.activeProfiles(this));
        loadValues();
        setContentView(buildContent());
        renderValues();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = title("‹", 30f);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView screenTitle = title("자동문자", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(8);
        header.addView(screenTitle, titleParams);
        root.addView(header, matchWrap());

        enabled = switchCard("자동문자 사용");
        root.addView(enabled, topMargin(14));

        root.addView(sectionLabel("언제 보낼까요?"), topMargin(20));
        LinearLayout rules = new LinearLayout(this);
        rules.setOrientation(LinearLayout.VERTICAL);
        rules.setBackgroundResource(R.drawable.bg_card);
        rules.setPadding(dp(12), dp(3), dp(12), dp(3));
        root.addView(rules, topMargin(8));
        connected = new Switch(this);
        connectedTemplate = addScenario(
                rules, "통화 후", connected, REQUEST_CONNECTED);
        missed = new Switch(this);
        missedTemplate = addScenario(
                rules, "부재중", missed, REQUEST_MISSED);
        delayed = new Switch(this);
        followTemplate = addScenario(
                rules, "며칠 뒤", delayed, REQUEST_FOLLOW_UP);

        LinearLayout common = new LinearLayout(this);
        common.setOrientation(LinearLayout.HORIZONTAL);
        common.setGravity(Gravity.CENTER_VERTICAL);
        common.setPadding(dp(14), dp(12), dp(10), dp(12));
        common.setBackgroundResource(R.drawable.bg_clickable_row);
        common.setClickable(true);
        common.setFocusable(true);
        common.setOnClickListener(v -> showCommonSettings());

        common.addView(title("시간·회선 설정", 15f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = body("›");
        arrow.setTextSize(24f);
        arrow.setGravity(Gravity.CENTER);
        common.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(44)));
        root.addView(common, topMargin(16));

        Button save = button("저장", true);
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        saveParams.topMargin = dp(22);
        root.addView(save, saveParams);
        return scroll;
    }

    private TextView addScenario(
            LinearLayout root, String label, Switch toggle, int requestCode) {
        if (root.getChildCount() > 0) {
            View divider = new View(this);
            divider.setBackgroundColor(getColor(R.color.border));
            root.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        }
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(4), dp(9), dp(8), dp(9));
        text.setFocusable(true);
        text.setOnClickListener(v -> {
            if (requestCode == REQUEST_FOLLOW_UP) showFollowUpOptions();
            else openTemplateSelector(requestCode);
        });
        text.addView(title(label, 15f), matchWrap());
        TextView template = body("");
        template.setMaxLines(2);
        template.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.addView(template, topMargin(3));
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toggle.setShowText(false);
        toggle.setContentDescription(label + " 자동문자 사용");
        toggle.setMinHeight(dp(48));
        row.addView(toggle, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(row, matchWrap());
        return template;
    }

    private void showFollowUpOptions() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("며칠 뒤")
                .setItems(new String[]{"문구 바꾸기", "발송일 바꾸기"}, (prompt, which) -> {
                    if (which == 0) openTemplateSelector(REQUEST_FOLLOW_UP);
                    else showDelayPicker();
                })
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private void showDelayPicker() {
        String[] choices = {"1일 후", "3일 후", "5일 후", "7일 후", "직접 입력"};
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("후속문자 발송 시점")
                .setItems(choices, (dialog, which) -> {
                    if (which == 4) {
                        showCustomDelayInput();
                        return;
                    }
                    int[] days = {1, 3, 5, 7};
                    delayDaysValue = days[which];
                    renderValues();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showCustomDelayInput() {
        EditText input = numberInput(String.valueOf(delayDaysValue));
        input.setText(String.valueOf(delayDaysValue));
        input.setSelectAllOnFocus(true);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(dp(22), dp(8), dp(22), dp(12));
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("며칠 후에 보낼까요?")
                .setMessage("1~30일")
                .setView(container)
                .setNegativeButton("취소", null)
                .setPositiveButton("적용", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            CallTagDialogStyler.apply(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    int value = parse(input, -1);
                    if (value < 1 || value > 30) {
                        Toast.makeText(this, "1~30일 사이로 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    delayDaysValue = value;
                    renderValues();
                    dialog.dismiss();
                });
        });
        dialog.show();
    }

    private void showCommonSettings() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(6), dp(18), dp(12));

        Switch business = switchCard("업무시간만 발송");
        business.setChecked(businessHoursValue);
        panel.addView(business, matchWrap());

        panel.addView(fieldLabel("발송 시간"), topMargin(14));
        LinearLayout hours = new LinearLayout(this);
        hours.setGravity(Gravity.CENTER_VERTICAL);
        EditText start = numberInput(String.valueOf(startHourValue));
        start.setContentDescription("시작 시간");
        start.setText(String.valueOf(startHourValue));
        hours.addView(start, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView from = body("~");
        from.setGravity(Gravity.CENTER);
        hours.addView(from, new LinearLayout.LayoutParams(dp(28), dp(48)));
        EditText end = numberInput(String.valueOf(endHourValue));
        end.setContentDescription("종료 시간");
        end.setText(String.valueOf(endHourValue));
        hours.addView(end, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView until = body("시");
        until.setGravity(Gravity.CENTER);
        hours.addView(until, new LinearLayout.LayoutParams(dp(28), dp(48)));
        panel.addView(hours, topMargin(6));

        panel.addView(fieldLabel("중복 발송 방지"), topMargin(14));
        LinearLayout cooldown = new LinearLayout(this);
        cooldown.setGravity(Gravity.CENTER_VERTICAL);
        EditText cooldownInput = numberInput(String.valueOf(cooldownHoursValue));
        cooldownInput.setText(String.valueOf(cooldownHoursValue));
        cooldown.addView(cooldownInput, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView cooldownSuffix = body("시간");
        cooldownSuffix.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cooldownSuffixParams = new LinearLayout.LayoutParams(
                0, dp(48), 2.2f);
        cooldownSuffixParams.leftMargin = dp(9);
        cooldown.addView(cooldownSuffix, cooldownSuffixParams);
        panel.addView(cooldown, topMargin(6));

        panel.addView(fieldLabel("발송 회선"), topMargin(14));
        Spinner line = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (SimProfileManager.Profile profile : profiles) labels.add(profile.label());
        if (labels.isEmpty()) labels.add("기본 문자 회선");
        line.setAdapter(new CallTagSpinnerAdapter(this, labels));
        line.setBackground(null);
        line.setPadding(0, 0, 0, 0);
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).subscriptionId == selectedSubscriptionId) {
                line.setSelection(i);
                break;
            }
        }
        panel.addView(line, topMarginHeight(6, 50));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(panel, matchWrap());
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("발송 조건·회선")
                .setView(scroll)
                .setNegativeButton("취소", null)
                .setPositiveButton("적용", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            CallTagDialogStyler.apply(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int startValue = clamp(parse(start, 9), 0, 23);
                int endValue = clamp(parse(end, 20), 1, 24);
                if (business.isChecked() && startValue >= endValue) {
                    Toast.makeText(this, "종료 시간은 시작 시간보다 늦어야 합니다.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                businessHoursValue = business.isChecked();
                startHourValue = startValue;
                endHourValue = endValue;
                cooldownHoursValue = clamp(parse(cooldownInput, 24), 1, 168);
                int selectedLine = line.getSelectedItemPosition();
                if (selectedLine >= 0 && selectedLine < profiles.size()) {
                    selectedSubscriptionId = profiles.get(selectedLine).subscriptionId;
                }
                renderValues();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void openTemplateSelector(int requestCode) {
        String purpose;
        if (requestCode == REQUEST_CONNECTED) {
            purpose = MessageTemplateStore.PURPOSE_INCOMING;
        } else if (requestCode == REQUEST_MISSED) {
            purpose = MessageTemplateStore.PURPOSE_MISSED;
        } else {
            purpose = MessageTemplateStore.PURPOSE_FOLLOW_UP;
        }
        startActivityForResult(new Intent(this, MessageTemplateLibraryActivity.class)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_SELECT_MODE, true)
                .putExtra(MessageTemplateLibraryActivity.EXTRA_PURPOSE_FILTER, purpose), requestCode);
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
            MessageTemplateStore.setDefault(this, MessageTemplateStore.PURPOSE_INCOMING, template.id);
            MessageTemplateStore.setDefault(this, MessageTemplateStore.PURPOSE_OUTGOING, template.id);
        } else if (requestCode == REQUEST_MISSED) {
            MessageTemplateStore.setDefault(this, MessageTemplateStore.PURPOSE_MISSED, template.id);
        } else {
            MessageTemplateStore.setDefault(this, MessageTemplateStore.PURPOSE_FOLLOW_UP, template.id);
        }
        renderValues();
    }

    private void loadValues() {
        businessHoursValue = MessageAutomationStore.businessHoursEnabled(this);
        delayDaysValue = MessageAutomationStore.delayDays(this);
        cooldownHoursValue = MessageAutomationStore.cooldownHours(this);
        startHourValue = MessageAutomationStore.startHour(this);
        endHourValue = MessageAutomationStore.endHour(this);
        selectedSubscriptionId = MessageAutomationStore.selectedSubscriptionId(this);
    }

    private void renderValues() {
        if (enabled == null) return;
        enabled.setChecked(MessageAutomationStore.isEnabled(this));
        connected.setChecked(MessageAutomationStore.connectedEnabled(this));
        missed.setChecked(MessageAutomationStore.missedEnabled(this));
        delayed.setChecked(MessageAutomationStore.delayedEnabled(this));
        renderTemplate(connectedTemplate, MessageTemplateStore.PURPOSE_INCOMING, "");
        renderTemplate(missedTemplate, MessageTemplateStore.PURPOSE_MISSED, "");
        renderTemplate(followTemplate, MessageTemplateStore.PURPOSE_FOLLOW_UP, delayDaysValue + "일 후 · ");
    }

    private void renderTemplate(TextView view, String purpose, String prefix) {
        String name = MessageTemplateStore.defaultName(this, purpose);
        view.setText(prefix + (name == null || name.trim().isEmpty() ? "문구 선택" : name) + "  ›");
    }

    private void save() {
        if (!FeatureEntitlementStore.hasMessageAccess(this)) {
            Toast.makeText(this, "문자 기능 이용 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            return;
        }
        boolean wantsAutomaticSms = enabled.isChecked()
                && (connected.isChecked() || missed.isChecked() || delayed.isChecked());
        if (wantsAutomaticSms && checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                    .setTitle("자동문자 권한")
                    .setMessage("설정한 통화 조건에 맞으면 상대방에게 SMS를 보냅니다. 통신사 요금이 발생할 수 있습니다.")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("SMS 권한 요청", (prompt, which) ->
                            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS))
                    .create();
            dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
            dialog.show();
            return;
        }
        saveApprovedSettings();
    }

    private void saveApprovedSettings() {
        MessageAutomationStore.setEnabled(this, enabled.isChecked());
        MessageAutomationStore.setConnectedEnabled(this, connected.isChecked());
        MessageAutomationStore.setMissedEnabled(this, missed.isChecked());
        MessageAutomationStore.setDelayedEnabled(this, delayed.isChecked());
        MessageAutomationStore.setBusinessHoursEnabled(this, businessHoursValue);
        MessageAutomationStore.setDelayDays(this, delayDaysValue);
        MessageAutomationStore.setCooldownHours(this, cooldownHoursValue);
        MessageAutomationStore.setBusinessHours(this, startHourValue, endHourValue);
        MessageAutomationStore.setSelectedSubscriptionId(this, selectedSubscriptionId);
        Toast.makeText(this, "저장했습니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_SMS) return;
        if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            saveApprovedSettings();
        } else {
            Toast.makeText(this, "SMS 권한이 없어 자동문자 설정을 활성화하지 않았습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private Switch switchCard(String text) {
        Switch view = new Switch(this);
        view.setText(text);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTextSize(15f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(14), dp(6), dp(10), dp(6));
        view.setMinHeight(dp(56));
        view.setBackgroundResource(R.drawable.bg_card);
        return view;
    }

    private EditText numberInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextSize(14f);
        input.setGravity(Gravity.CENTER);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setBackgroundResource(R.drawable.bg_secondary_button);
        return input;
    }

    private TextView sectionLabel(String value) {
        TextView view = title(value, 13f);
        view.setTextColor(getColor(R.color.text_secondary));
        return view;
    }

    private TextView fieldLabel(String value) {
        TextView view = title(value, 13f);
        view.setTextColor(getColor(R.color.text_secondary));
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
        text.setTextSize(12.5f);
        text.setIncludeFontPadding(false);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        button.setMinWidth(0);
        return button;
    }

    private int parse(EditText input, int fallback) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    private LinearLayout.LayoutParams topMarginHeight(int margin, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
