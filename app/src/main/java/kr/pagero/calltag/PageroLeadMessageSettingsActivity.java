package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** 페이지로 문의 접수 자동문자 설정. */
public final class PageroLeadMessageSettingsActivity extends Activity {
    private static final int REQUEST_SMS = 8451;

    private Switch enabled;
    private EditText template;
    private EditText delayMinutes;
    private EditText siteId;
    private EditText siteTemplate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(32));
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, true);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = text("페이지로 문의접수문자", 21f, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        titleParams.leftMargin = dp(8);
        header.addView(title, titleParams);
        root.addView(header);

        enabled = new Switch(this);
        enabled.setText("사용");
        enabled.setTextSize(15f);
        enabled.setTextColor(getColor(R.color.text_primary));
        enabled.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        enabled.setGravity(Gravity.CENTER_VERTICAL);
        enabled.setPadding(dp(14), dp(8), dp(10), dp(8));
        enabled.setBackgroundResource(R.drawable.bg_card);
        enabled.setChecked(PageroLeadMessageSettings.enabled(this));
        root.addView(enabled, top(14));

        root.addView(label("문자 내용"), top(20));
        template = input(true);
        template.setText(PageroLeadMessageSettings.defaultTemplate(this));
        root.addView(template, fixed(128, 7));

        root.addView(label("발송 지연(분)"), top(18));
        delayMinutes = input(false);
        delayMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        delayMinutes.setText(String.valueOf(PageroLeadMessageSettings.delayMinutes(this)));
        root.addView(delayMinutes, fixed(50, 7));

        root.addView(label("페이지별 설정 (선택)"), top(22));
        siteId = input(false);
        siteId.setHint("페이지 ID / slug");
        root.addView(siteId, fixed(50, 7));
        siteTemplate = input(true);
        siteTemplate.setHint("이 페이지에서 사용할 문자 내용");
        root.addView(siteTemplate, fixed(110, 7));

        Button save = new Button(this);
        save.setText("저장");
        save.setAllCaps(false);
        save.setTextColor(getColor(android.R.color.white));
        save.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        save.setBackgroundResource(R.drawable.bg_primary_button);
        save.setOnClickListener(v -> save());
        root.addView(save, fixed(52, 22));
        return scroll;
    }

    private void save() {
        if (enabled.isChecked() && !FeatureAccessGate.require(this, FeatureAccessGate.MESSAGE)) return;
        int delay;
        try {
            delay = Integer.parseInt(delayMinutes.getText().toString().trim());
        } catch (NumberFormatException error) {
            Toast.makeText(this, "발송 지연은 0~1440분으로 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (delay < 0 || delay > 1440) {
            Toast.makeText(this, "발송 지연은 0~1440분으로 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        String body = template.getText().toString().trim();
        if (body.isEmpty()) {
            Toast.makeText(this, "문자 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        PageroLeadMessageSettings.setEnabled(this, enabled.isChecked());
        PageroLeadMessageSettings.setDefaultTemplate(this, body);
        PageroLeadMessageSettings.setDelayMinutes(this, delay);
        String page = siteId.getText().toString().trim();
        if (!page.isEmpty()) {
            PageroLeadMessageSettings.setTemplateFor(this, page,
                    siteTemplate.getText().toString().trim());
        }

        if (enabled.isChecked() && checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                    .setTitle("문자 권한 필요")
                    .setMessage("자동문자를 보내려면 문자 권한이 필요합니다.")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("권한 허용", (d, w) -> requestPermissions(
                            new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS))
                    .create();
            dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
            dialog.show();
            return;
        }
        Toast.makeText(this, "저장했습니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_SMS) return;
        boolean granted = checkSelfPermission(Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            Toast.makeText(this, "문자 권한을 허용했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            PageroLeadMessageSettings.setEnabled(this, false);
            enabled.setChecked(false);
            Toast.makeText(this, "문자 권한이 필요합니다.", Toast.LENGTH_LONG).show();
        }
    }

    private EditText input(boolean multiline) {
        EditText value = new EditText(this);
        value.setTextColor(getColor(R.color.text_primary));
        value.setHintTextColor(getColor(R.color.text_muted));
        value.setTextSize(14f);
        value.setBackgroundResource(R.drawable.bg_input);
        value.setPadding(dp(13), dp(10), dp(13), dp(10));
        value.setSingleLine(!multiline);
        if (multiline) value.setGravity(Gravity.TOP | Gravity.START);
        return value;
    }

    private TextView label(String value) {
        TextView view = text(value, 13f, true);
        view.setTextColor(getColor(R.color.text_secondary));
        return view;
    }

    private TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.text_primary));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixed(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
