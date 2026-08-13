package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 고객 문자 화면의 핵심 진입점을 3개로 고정한다. */
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

        TextView section = label("문자 보내기");
        addView(section, topMargin(2));

        LinearLayout primaryMenu = new LinearLayout(getContext());
        primaryMenu.setOrientation(VERTICAL);
        primaryMenu.setPadding(dp(4), dp(4), dp(4), dp(4));
        primaryMenu.setBackgroundResource(R.drawable.bg_card);
        addPrimaryRow(primaryMenu,
                "고객선택후 문자",
                "고객을 선택하고 바로 문자 작성",
                CustomerMessagePickerActivity.class);
        addPrimaryRow(primaryMenu,
                "통화후 자동문자",
                "통화 종료 후 조건에 맞춰 자동 발송",
                MessageAutomationSettingsActivity.class);
        addPrimaryRow(primaryMenu,
                "페이지로 문의접수문자",
                "페이지로 문의가 접수되면 자동 발송",
                PageroLeadMessageSettingsActivity.class);
        addView(primaryMenu, topMargin(8));

        TextView management = label("문자 관리");
        addView(management, topMargin(20));

        LinearLayout menu = new LinearLayout(getContext());
        menu.setOrientation(VERTICAL);
        menu.setPadding(dp(4), dp(4), dp(4), dp(4));
        menu.setBackgroundResource(R.drawable.bg_card);
        addMenuRow(menu, "문자 템플릿", "자주 쓰는 문구와 이미지", MessageTemplateLibraryActivity.class, false);
        addMenuRow(menu, "그룹·단체문자", "여러 고객에게 한 번에 발송", GroupCampaignHubActivity.class, true);
        addMenuRow(menu, "발송 내역", "보낸 문자와 처리 상태 확인", MessageHistoryActivity.class, false);
        addView(menu, topMargin(8));
    }

    private void addPrimaryRow(LinearLayout parent, String title, String subtitle,
                               Class<?> destination) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(15), 0, dp(8), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            if (!requireMessageAccess()) return;
            getContext().startActivity(new Intent(getContext(), destination));
        });

        LinearLayout labels = new LinearLayout(getContext());
        labels.setOrientation(VERTICAL);
        labels.addView(text(title, 15.5f, true), new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView sub = text(subtitle, 12f, false);
        sub.setTextColor(getContext().getColor(R.color.text_secondary));
        LayoutParams subParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(4);
        labels.addView(sub, subParams);
        row.addView(labels, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 24f, false);
        arrow.setTextColor(getContext().getColor(R.color.primary));
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LayoutParams(dp(30), dp(48)));

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(68));
        params.bottomMargin = dp(3);
        parent.addView(row, params);
    }

    private void addMenuRow(LinearLayout parent, String title, String subtitle,
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

        LinearLayout labels = new LinearLayout(getContext());
        labels.setOrientation(VERTICAL);
        labels.addView(text(title, 15f, true), new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView sub = text(subtitle, 12f, false);
        sub.setTextColor(getContext().getColor(R.color.text_secondary));
        LayoutParams subParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(3);
        labels.addView(sub, subParams);
        row.addView(labels, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 23f, false);
        arrow.setTextColor(getContext().getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LayoutParams(dp(30), dp(48)));

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(62));
        params.bottomMargin = dp(2);
        parent.addView(row, params);
    }

    private boolean requireMessageAccess() {
        if (FeatureEntitlementStore.hasMessageAccess(getContext())) return true;
        Toast.makeText(getContext(), "문자자동화 이용권이 필요합니다.", Toast.LENGTH_SHORT).show();
        getContext().startActivity(new Intent(getContext(), BillingEntitlementActivity.class));
        return false;
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
