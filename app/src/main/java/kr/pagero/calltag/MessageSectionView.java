package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

/** 고객 화면 안에서 고객을 기준으로 시작하는 문자 허브. */
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

        Button customerMessage = button("고객 선택 후 문자", true);
        customerMessage.setTextSize(16f);
        customerMessage.setOnClickListener(v -> {
            if (!requireMessageAccess()) return;
            getContext().startActivity(new Intent(
                    getContext(), CustomerMessagePickerActivity.class));
        });
        addView(customerMessage, new LayoutParams(LayoutParams.MATCH_PARENT, dp(58)));

        LinearLayout firstRow = new LinearLayout(getContext());
        firstRow.setOrientation(HORIZONTAL);
        Button templates = button("문자 템플릿", false);
        templates.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageTemplateLibraryActivity.class)));
        firstRow.addView(templates, new LayoutParams(0, dp(54), 1f));

        Button groupCampaign = button("그룹·단체문자", false);
        groupCampaign.setOnClickListener(v -> {
            if (!requireMessageAccess()) return;
            getContext().startActivity(new Intent(
                    getContext(), GroupCampaignHubActivity.class));
        });
        LayoutParams groupParams = new LayoutParams(0, dp(54), 1f);
        groupParams.leftMargin = dp(8);
        firstRow.addView(groupCampaign, groupParams);
        addView(firstRow, topMargin(10));

        LinearLayout secondRow = new LinearLayout(getContext());
        secondRow.setOrientation(HORIZONTAL);
        Button automation = button("통화 후 자동문자", false);
        automation.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageAutomationSettingsActivity.class)));
        secondRow.addView(automation, new LayoutParams(0, dp(54), 1f));

        Button history = button("발송 내역", false);
        history.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageHistoryActivity.class)));
        LayoutParams historyParams = new LayoutParams(0, dp(54), 1f);
        historyParams.leftMargin = dp(8);
        secondRow.addView(history, historyParams);
        addView(secondRow, topMargin(10));
    }

    private boolean requireMessageAccess() {
        if (FeatureEntitlementStore.hasMessageAccess(getContext())) return true;
        Toast.makeText(getContext(), "문자 기능 이용 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        return false;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getContext().getColor(primary
                ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        button.setMinWidth(0);
        return button;
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
