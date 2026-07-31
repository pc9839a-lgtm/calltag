package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

public final class AccountLauncherView extends TextView {
    public AccountLauncherView(Context context) {
        super(context);
        init();
    }

    public AccountLauncherView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccountLauncherView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        String title = "계정 및 개인정보";
        String subtitle = "회원정보 · 개인정보처리방침 · 로그아웃 · 회원탈퇴";
        String combined = title + "\n" + subtitle;
        SpannableString styled = new SpannableString(combined);
        styled.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new RelativeSizeSpan(0.80f), title.length() + 1, combined.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new ForegroundColorSpan(getContext().getColor(R.color.text_secondary)),
                title.length() + 1, combined.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        setText(styled);
        setTextColor(getContext().getColor(R.color.text_primary));
        setTextSize(16f);
        setGravity(Gravity.CENTER_VERTICAL);
        setLineSpacing(dp(3), 1f);
        setPadding(dp(18), dp(10), dp(14), dp(10));
        setCompoundDrawablePadding(dp(12));
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0);
        setBackgroundResource(R.drawable.bg_clickable_row);
        setClickable(true);
        setFocusable(true);
        setMinHeight(dp(72));
        super.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), AccountActivity.class)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}