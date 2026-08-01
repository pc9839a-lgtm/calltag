package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 고객 그룹과 단체문자를 하나의 흐름으로 묶은 화면. */
public final class GroupCampaignHubActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(36));
        root.setBackgroundColor(getColor(R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("그룹·단체문자", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        TextView group = actionCard(
                "고객 그룹",
                "단체문자를 보낼 고객을 묶고 관리합니다.");
        group.setOnClickListener(v -> startActivity(
                new Intent(this, MessageGroupActivity.class)));
        root.addView(group, topMargin(22));

        TextView campaign = actionCard(
                "단체문자 보내기",
                "선택한 고객 그룹으로 발송을 시작합니다.");
        campaign.setOnClickListener(v -> startActivity(
                new Intent(this, CampaignListActivity.class)));
        root.addView(campaign, topMargin(12));
        return root;
    }

    private TextView actionCard(String title, String subtitle) {
        TextView card = new TextView(this);
        card.setText(title + "\n" + subtitle + "    ›");
        card.setTextColor(getColor(R.color.text_primary));
        card.setTextSize(16f);
        card.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setLineSpacing(0f, 1.35f);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        card.setBackgroundResource(R.drawable.bg_secondary_button);
        card.setClickable(true);
        card.setFocusable(true);
        return card;
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

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
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
