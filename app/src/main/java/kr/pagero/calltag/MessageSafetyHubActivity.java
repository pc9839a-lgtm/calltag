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

/** 발송 내역과 발송 제외를 하나의 진입점으로 묶는다. */
public final class MessageSafetyHubActivity extends Activity {
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
        Button back = button("‹");
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("발송 관리", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        TextView history = actionCard("발송 내역", "보낸 문자와 예약 상태를 확인합니다.");
        history.setOnClickListener(v -> startActivity(
                new Intent(this, MessageHistoryActivity.class)));
        root.addView(history, topMargin(22));

        TextView exclusion = actionCard("발송 제외", "문자를 보내지 않을 고객과 번호를 관리합니다.");
        exclusion.setOnClickListener(v -> startActivity(
                new Intent(this, MessageExclusionActivity.class)));
        root.addView(exclusion, topMargin(12));
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

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(R.drawable.bg_secondary_button);
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
