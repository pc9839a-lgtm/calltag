package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 고객 화면 안에서 사용하는 문자 발송 허브. 설정 메뉴는 더보기로 분리한다. */
public final class MessageSectionView extends LinearLayout {
    public static final String ACTION_CHANGED = "kr.pagero.calltag.MESSAGE_CHANGED";

    private TextView summary;

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

        Button newMessage = button("새 문자 보내기", true);
        newMessage.setTextSize(16f);
        newMessage.setOnClickListener(v -> openCompose(false));
        addView(newMessage, new LayoutParams(LayoutParams.MATCH_PARENT, dp(56)));

        Button templateMessage = button("템플릿으로 보내기", false);
        templateMessage.setOnClickListener(v -> openCompose(true));
        addView(templateMessage, topMarginHeight(9, 52));

        LinearLayout actionRow = new LinearLayout(getContext());
        actionRow.setOrientation(HORIZONTAL);

        Button campaigns = button("단체문자", false);
        campaigns.setOnClickListener(v -> {
            if (!requireMessageAccess()) return;
            getContext().startActivity(new Intent(getContext(), CampaignListActivity.class));
        });
        actionRow.addView(campaigns, new LayoutParams(0, dp(52), 1f));

        Button history = button("발송 내역", false);
        history.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageHistoryActivity.class)));
        LayoutParams historyParams = new LayoutParams(0, dp(52), 1f);
        historyParams.leftMargin = dp(8);
        actionRow.addView(history, historyParams);
        addView(actionRow, topMargin(9));

        summary = new TextView(getContext());
        summary.setGravity(Gravity.CENTER);
        summary.setTextColor(getContext().getColor(R.color.text_secondary));
        summary.setTextSize(13f);
        summary.setIncludeFontPadding(false);
        summary.setBackgroundResource(R.drawable.bg_soft_panel);
        summary.setPadding(dp(12), dp(12), dp(12), dp(12));
        addView(summary, topMargin(16));

        render();
    }

    private void render() {
        MessageLogStore store = new MessageLogStore(getContext());
        try {
            int scheduled = store.countByStatus(MessageLogStore.STATUS_SCHEDULED);
            int sent = store.countByStatus(MessageLogStore.STATUS_SENT);
            int failed = store.countByStatus(MessageLogStore.STATUS_FAILED);
            summary.setText("예정 " + scheduled + "   ·   완료 " + sent + "   ·   실패 " + failed);
        } finally {
            store.close();
        }
    }

    private boolean requireMessageAccess() {
        if (FeatureEntitlementStore.hasMessageAccess(getContext())) return true;
        Toast.makeText(getContext(), "문자자동화 이용권이 필요합니다.", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void openCompose(boolean useTemplate) {
        if (!requireMessageAccess()) return;
        getContext().startActivity(new Intent(getContext(), ManualMessageActivity.class)
                .putExtra(ManualMessageActivity.EXTRA_USE_TEMPLATE, useTemplate));
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

    private LayoutParams topMarginHeight(int margin, int height) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && summary != null) render();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus && summary != null) render();
    }
}
