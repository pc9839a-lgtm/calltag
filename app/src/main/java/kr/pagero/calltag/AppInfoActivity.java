package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 더보기 > 앱 정보. 법적 문서, 버전, 고객센터만 모은다. */
public final class AppInfoActivity extends Activity {
    private static final String TERMS_URL = "https://call.pagero.kr/terms/";
    private static final String PRIVACY_URL = "https://call.pagero.kr/privacy/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, true);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = text("앱 정보", 21f, true);
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

        addValueRow(card, "버전 정보", "v" + BuildConfig.VERSION_NAME);
        addActionRow(card, "서비스 이용약관", v -> openWeb(TERMS_URL));
        addActionRow(card, "개인정보처리방침", v -> openWeb(PRIVACY_URL));
        addActionRow(card, "고객센터", v -> startActivity(
                new Intent(this, CustomerSupportActivity.class)));
        return scroll;
    }

    private void addValueRow(LinearLayout parent, String label, String value) {
        LinearLayout row = rowBase();
        TextView title = text(label, 15f, true);
        row.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView valueView = text(value, 14f, false);
        valueView.setTextColor(getColor(R.color.text_secondary));
        row.addView(valueView);
        parent.addView(row, rowParams());
    }

    private void addActionRow(LinearLayout parent, String label, View.OnClickListener listener) {
        LinearLayout row = rowBase();
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);
        TextView title = text(label, 15f, true);
        row.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = text("›", 24f, false);
        arrow.setTextColor(getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(44)));
        parent.addView(row, rowParams());
    }

    private LinearLayout rowBase() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(10), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        return row;
    }

    private void openWeb(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException error) {
            Toast.makeText(this, "페이지를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
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

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        params.bottomMargin = dp(2);
        return params;
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
