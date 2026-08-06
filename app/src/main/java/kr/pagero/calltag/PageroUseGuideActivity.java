package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 페이지로를 처음 사용하는 사람을 위한 별도 안내 화면. */
public final class PageroUseGuideActivity extends Activity {
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
        root.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = title("‹", 30f);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        header.addView(title("페이지로 사용하기", 22f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header, matchWrap());

        root.addView(title("랜딩페이지를 만들고\n문의까지 바로 받으세요.", 24f), top(18));
        root.addView(body("페이지로에서 문구와 이미지를 바꿔 랜딩페이지를 공개하면, "
                + "접수된 고객정보를 콜태그에서 이어서 관리할 수 있습니다."), top(10));

        LinearLayout steps = card();
        steps.addView(step("1", "페이지로에서 같은 이메일 계정으로 로그인합니다."), matchWrap());
        steps.addView(step("2", "업종에 맞는 랜딩페이지를 만들고 공개합니다."), top(10));
        steps.addView(step("3", "고객이 문의를 남기면 콜태그에 고객으로 등록됩니다."), top(10));
        steps.addView(step("4", "콜태그에서 통화·문자·후속 일정을 관리합니다."), top(10));
        root.addView(steps, top(20));

        Button open = button("페이지로에서 랜딩페이지 만들기", true);
        open.setOnClickListener(v -> openPagero());
        root.addView(open, fixedTop(52, 18));

        Button connection = button("문의 동기화 상태 확인", false);
        connection.setOnClickListener(v -> startActivity(new Intent(this, PageroSyncActivity.class)));
        root.addView(connection, fixedTop(50, 8));
        return scroll;
    }

    private LinearLayout step(String number, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = title(number, 14f);
        badge.setTextColor(getColor(R.color.primary));
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.bg_soft_panel);
        row.addView(badge, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView text = body(value);
        text.setTextColor(getColor(R.color.text_primary));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(12);
        row.addView(text, textParams);
        return row;
    }

    private void openPagero() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://pagero.kr/app")));
        } catch (RuntimeException error) {
            Toast.makeText(this, "페이지로 관리화면을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView title(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView body(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(14f);
        view.setTextColor(getColor(R.color.text_secondary));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.2f);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
