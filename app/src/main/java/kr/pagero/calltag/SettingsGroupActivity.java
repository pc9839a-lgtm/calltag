package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 더보기의 상위 메뉴를 짧게 유지하기 위한 목적별 설정 허브. */
public final class SettingsGroupActivity extends Activity {
    public static final String EXTRA_GROUP = "settings_group";
    public static final String GROUP_MESSAGE = "message";
    public static final String GROUP_CUSTOMER = "customer";
    public static final String GROUP_DATA = "data";

    public static Intent intent(Context context, String group) {
        return new Intent(context, SettingsGroupActivity.class).putExtra(EXTRA_GROUP, group);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        String group = getIntent().getStringExtra(EXTRA_GROUP);
        if (group == null) group = "";
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, true);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = text(groupTitle(group), 21f, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        titleParams.leftMargin = dp(8);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(4), dp(3), dp(4), dp(3));
        card.setBackgroundResource(R.drawable.bg_card);
        root.addView(card, topMargin(16));

        if (GROUP_MESSAGE.equals(group)) {
            addRow(card, "통화 후 자동문자", v -> FeatureAccessGate.open(
                    this, MessageAutomationSettingsActivity.class, FeatureAccessGate.MESSAGE));
            addRow(card, "페이지로 문의 자동문자", v -> startActivity(
                    new Intent(this, PageroLeadMessageSettingsActivity.class)));
            addRow(card, "문자 문구·이미지", v -> startActivity(
                    new Intent(this, MessageTemplateLibraryActivity.class)));
            addRow(card, "그룹·단체문자", v -> FeatureAccessGate.open(
                    this, GroupCampaignHubActivity.class, FeatureAccessGate.MESSAGE));
            addRow(card, "발송 관리", v -> startActivity(
                    new Intent(this, MessageSafetyHubActivity.class)));
        } else if (GROUP_CUSTOMER.equals(group)) {
            addRow(card, "고객 상태", v -> startActivity(new Intent(this, StageSettingsActivity.class)));
            addRow(card, "일정 종류", v -> startActivity(new Intent(this, TaskTypeSettingsActivity.class)));
            addRow(card, "통화 후 팝업 제외", v -> startActivity(new Intent(this, PostCallExclusionActivity.class)));
        } else if (GROUP_DATA.equals(group)) {
            addRow(card, "동기화 상태", v -> startActivity(new Intent(this, CallTagSyncStatusActivity.class)));
            addRow(card, "백업 및 복원", v -> startActivity(new Intent(this, BackupRestoreActivity.class)));
        } else {
            TextView error = text("설정 항목을 불러오지 못했습니다.", 15f, false);
            error.setTextColor(getColor(R.color.text_secondary));
            error.setPadding(dp(14), dp(18), dp(14), dp(18));
            card.addView(error, matchWrap());
        }
        return scroll;
    }

    private String groupTitle(String group) {
        if (GROUP_MESSAGE.equals(group)) return "문자 관리";
        if (GROUP_CUSTOMER.equals(group)) return "고객 관리";
        if (GROUP_DATA.equals(group)) return "데이터 관리";
        return "설정";
    }

    private void addRow(LinearLayout parent, String label, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(10), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);
        TextView title = text(label, 15f, true);
        row.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = text("›", 24f, false);
        arrow.setTextColor(getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(44)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        params.bottomMargin = dp(2);
        parent.addView(row, params);
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

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }
    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
