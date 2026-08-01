package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 단체문자 생성과 그룹·캠페인 관리를 한 화면에 묶는다. */
public final class GroupCampaignHubActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(36));
        root.setBackgroundColor(getColor(R.color.background));
        root.setFitsSystemWindows(true);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = text("그룹·단체문자", 21f, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        TextView create = text("단체문자 만들기", 15f, true);
        create.setTextColor(getColor(android.R.color.white));
        create.setGravity(Gravity.CENTER);
        create.setBackgroundResource(R.drawable.bg_primary_button);
        create.setClickable(true);
        create.setFocusable(true);
        create.setOnClickListener(v -> startActivity(
                new Intent(this, CampaignComposerActivity.class)));
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        createParams.topMargin = dp(18);
        root.addView(create, createParams);

        TextView label = text("관리", 13f, true);
        label.setTextColor(getColor(R.color.text_secondary));
        root.addView(label, topMargin(22));

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(4), dp(4), dp(4), dp(4));
        menu.setBackgroundResource(R.drawable.bg_card);
        addRow(menu, "고객 그룹", MessageGroupActivity.class);
        addRow(menu, "단체문자 목록", CampaignListActivity.class);
        root.addView(menu, topMargin(8));
        return root;
    }

    private void addRow(LinearLayout parent, String title, Class<?> destination) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(8), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> startActivity(new Intent(this, destination)));
        row.addView(text(title, 15f, true), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = text("›", 23f, false);
        arrow.setTextColor(getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(48)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        params.bottomMargin = dp(2);
        parent.addView(row, params);
    }

    private TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
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
