package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 단체문자 생성과 그룹·캠페인 관리를 콜태그 공통 카드 UI로 묶는다. */
public final class GroupCampaignHubActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(36));
        root.setBackgroundColor(getColor(R.color.background));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = text("그룹·단체문자", 21f, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, dp(46), 1f);
        titleParams.leftMargin = dp(8);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        LinearLayout createCard = new LinearLayout(this);
        createCard.setOrientation(LinearLayout.HORIZONTAL);
        createCard.setGravity(Gravity.CENTER_VERTICAL);
        createCard.setPadding(dp(18), 0, dp(12), 0);
        createCard.setBackgroundResource(R.drawable.bg_selected_row);
        createCard.setClickable(true);
        createCard.setFocusable(true);
        createCard.setOnClickListener(v -> startActivity(
                new Intent(this, CampaignComposerActivity.class)));

        LinearLayout createLabels = new LinearLayout(this);
        createLabels.setOrientation(LinearLayout.VERTICAL);
        TextView createTitle = text("단체문자 만들기", 17f, true);
        createLabels.addView(createTitle, matchWrap());
        TextView createSub = text("고객 또는 그룹을 선택해 문자를 보냅니다.", 12.5f, false);
        createSub.setTextColor(getColor(R.color.text_secondary));
        createLabels.addView(createSub, topMargin(5));
        createCard.addView(createLabels, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView createArrow = text("›", 27f, false);
        createArrow.setTextColor(getColor(R.color.primary));
        createArrow.setGravity(Gravity.CENTER);
        createCard.addView(createArrow, new LinearLayout.LayoutParams(dp(34), dp(58)));
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(82));
        createParams.topMargin = dp(16);
        root.addView(createCard, createParams);

        TextView label = text("관리", 13f, true);
        label.setTextColor(getColor(R.color.text_secondary));
        root.addView(label, topMargin(22));

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(4), dp(4), dp(4), dp(4));
        menu.setBackgroundResource(R.drawable.bg_card);
        addRow(menu, "고객 그룹", "수동·스마트 그룹 관리", MessageGroupActivity.class);
        addRow(menu, "단체문자 목록", "보낸 단체문자와 진행상태 확인", CampaignListActivity.class);
        root.addView(menu, topMargin(8));
        return scroll;
    }

    private void addRow(LinearLayout parent, String title, String subtitle, Class<?> destination) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(8), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> startActivity(new Intent(this, destination)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 15f, true), matchWrap());
        TextView sub = text(subtitle, 12f, false);
        sub.setTextColor(getColor(R.color.text_secondary));
        labels.addView(sub, topMargin(4));
        row.addView(labels, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 23f, false);
        arrow.setTextColor(getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(52)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64));
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
