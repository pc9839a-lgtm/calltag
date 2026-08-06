package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 고객을 기준으로 시작하는 문자 허브. */
public final class MessageSectionView extends LinearLayout {
    public static final String ACTION_CHANGED = "kr.pagero.calltag.MESSAGE_CHANGED";

    public MessageSectionView(Context context) {
        super(context);
        init();
    }

    public MessageSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MessageSectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        MessageAutomationStore.ensureDefaults(getContext());
        MessageTemplateStore.ensureDefaults(getContext());
        FollowUpRuleStore.ensureMigrated(getContext());

        TextView primary = primaryAction("고객 선택 후 문자");
        primary.setOnClickListener(v -> {
            if (!requireMessageAccess()) return;
            getContext().startActivity(new Intent(
                    getContext(), CustomerMessagePickerActivity.class));
        });
        addView(primary, new LayoutParams(LayoutParams.MATCH_PARENT, dp(52)));

        TextView section = label("문자 관리");
        addView(section, topMargin(20));

        LinearLayout menu = new LinearLayout(getContext());
        menu.setOrientation(VERTICAL);
        menu.setPadding(dp(4), dp(4), dp(4), dp(4));
        menu.setBackgroundResource(R.drawable.bg_card);
        addMenuRow(menu, "문자 템플릿", MessageTemplateLibraryActivity.class, false);
        addMenuRow(menu, "통화 후 자동문자", PostCallAutomationActivity.class, true);
        addMenuRow(menu, "후속문자 자동화", FollowUpAutomationActivity.class, true);
        addMenuRow(menu, "그룹·단체문자", GroupCampaignHubActivity.class, true);
        addMenuRow(menu, "발송 내역", MessageHistoryActivity.class, false);
        addView(menu, topMargin(8));
    }

    private void addMenuRow(LinearLayout parent, String title,
                            Class<?> destination, boolean accessRequired) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(8), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            if (accessRequired && !requireMessageAccess()) return;
            getContext().startActivity(new Intent(getContext(), destination));
        });

        TextView titleView = text(title, 15f, true);
        row.addView(titleView, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = text("›", 23f, false);
        arrow.setTextColor(getContext().getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LayoutParams(dp(30), dp(48)));

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(54));
        params.bottomMargin = dp(2);
        parent.addView(row, params);
    }

    private boolean requireMessageAccess() {
        if (FeatureEntitlementStore.hasMessageAccess(getContext())) return true;
        Toast.makeText(getContext(), "문자 기능 이용 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        return false;
    }

    private TextView primaryAction(String label) {
        TextView view = text(label, 15f, true);
        view.setTextColor(getContext().getColor(android.R.color.white));
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(R.drawable.bg_primary_button);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 13f, true);
        view.setTextColor(getContext().getColor(R.color.text_secondary));
        return view;
    }

    private TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextColor(getContext().getColor(R.color.text_primary));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LayoutParams topMargin(int value) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(value);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
