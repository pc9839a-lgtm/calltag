package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 더보기 > 페이지로 서비스 안내. 계정 연동 화면과 설명을 분리한다. */
public final class PageroUseGuideActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private ScrollView buildScreen() {
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
        TextView back = text("‹", 30f, R.color.text_primary, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));
        TextView title = text("페이지로 안내", 21f, R.color.text_primary, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(header);

        LinearLayout card = card();
        card.addView(text("랜딩페이지 문의를 콜태그로", 20f, R.color.text_primary, true));
        card.addView(text(
                "페이지로에서 만든 랜딩페이지에 문의가 들어오면 콜태그 고객으로 연결해 후속관리를 이어갈 수 있습니다.",
                14f, R.color.text_secondary, false), top(9));
        card.addView(text("• 페이지로는 콜태그와 별도로도 이용할 수 있습니다.\n• 계정 연결은 별도의 ‘페이지로 연동’ 메뉴에서 설정합니다.\n• 연결 후 문의 고객을 콜태그에서 확인할 수 있습니다.",
                14f, R.color.text_primary, false), top(14));
        root.addView(card, top(14));

        TextView open = button("페이지로 열기");
        open.setOnClickListener(v -> openWeb("https://pagero.kr/app"));
        root.addView(open, fixedTop(52, 14));
        return scroll;
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(18), dp(18), dp(18), dp(18));
        view.setBackgroundResource(R.drawable.bg_card);
        return view;
    }

    private TextView button(String value) {
        TextView view = text(value, 15f, android.R.color.white, true);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(R.drawable.bg_primary_button);
        return view;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(color));
        view.setLineSpacing(0f, 1.22f);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private void openWeb(String address) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(address)));
        } catch (RuntimeException error) {
            Toast.makeText(this, "페이지로를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
