package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MoreMenuListView extends LinearLayout {
    public MoreMenuListView(Context context) {
        super(context);
    }

    public MoreMenuListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MoreMenuListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (child instanceof TextView) styleRow((TextView) child, params);
        super.addView(child, params);
    }

    private void styleRow(TextView row, ViewGroup.LayoutParams rawParams) {
        String raw = String.valueOf(row.getText());
        int arrowIndex = raw.indexOf('›');
        String title = (arrowIndex >= 0 ? raw.substring(0, arrowIndex) : raw)
                .replaceAll("\\s{2,}", " ")
                .trim();
        String subtitle = subtitleFor(title);
        String combined = title + "\n" + subtitle;
        SpannableString styled = new SpannableString(combined);
        styled.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new RelativeSizeSpan(0.80f), title.length() + 1, combined.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new ForegroundColorSpan(getContext().getColor(R.color.text_secondary)),
                title.length() + 1, combined.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        row.setText(styled);
        row.setTextColor(getContext().getColor(R.color.text_primary));
        row.setTextSize(16f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLineSpacing(dp(3), 1f);
        row.setPadding(dp(18), dp(10), dp(14), dp(10));
        row.setCompoundDrawablePadding(dp(12));
        row.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setMinHeight(dp(72));

        if (rawParams instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) rawParams;
            params.height = dp(72);
            params.bottomMargin = dp(8);
        }
    }

    private String subtitleFor(String title) {
        if (title.contains("통화 후 처리")) return "실제 통화 없이 정리 화면 확인";
        if (title.contains("통화 감지 끄기")) return "현재 켜짐 · 누르면 통화 감지 중지";
        if (title.contains("통화 감지 켜기")) return "현재 꺼짐 · 누르면 통화 감지 시작";
        if (title.contains("제외번호")) return "고객 분류에서 제외할 번호 관리";
        return "눌러서 설정";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}