package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 더보기 화면의 문자 관련 관리 메뉴. */
public final class MessageSettingsView extends LinearLayout {
    public MessageSettingsView(Context context) {
        super(context);
        init();
    }

    public MessageSettingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MessageSettingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        addRow("자동문자 설정", MessageAutomationSettingsActivity.class);
        addRow("문자 템플릿", MessageTemplateLibraryActivity.class);
        addRow("템플릿 이미지", TemplateImageLibraryActivity.class);
        addRow("발송 제외", MessageExclusionActivity.class);
        addRow("고객 그룹", MessageGroupActivity.class);
        addRow("발송 내역", MessageHistoryActivity.class);
    }

    private void addRow(String title, Class<?> destination) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(14), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), destination)));

        TextView label = new TextView(getContext());
        label.setText(title);
        label.setTextColor(getContext().getColor(R.color.text_primary));
        label.setTextSize(16f);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setIncludeFontPadding(false);
        row.addView(label, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(getContext());
        arrow.setText("›");
        arrow.setTextColor(getContext().getColor(R.color.text_muted));
        arrow.setTextSize(24f);
        row.addView(arrow);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(58));
        params.bottomMargin = dp(7);
        addView(row, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
