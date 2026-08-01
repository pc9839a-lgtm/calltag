package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class MessageSectionView extends LinearLayout {
    public static final String ACTION_CHANGED = "kr.pagero.calltag.MESSAGE_CHANGED";

    private TextView summary;
    private Switch connected;
    private Switch missed;
    private Switch delayed;
    private boolean rendering;

    public MessageSectionView(Context context) { super(context); init(); }
    public MessageSectionView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public MessageSectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        setOrientation(VERTICAL);
        MessageAutomationStore.ensureDefaults(getContext());
        MessageTemplateStore.ensureDefaults(getContext());

        addView(sectionTitle("문자 보내기"), matchWrap());

        LinearLayout composeRow = horizontalRow();
        Button templateCompose = button("템플릿으로 보내기", true);
        templateCompose.setOnClickListener(v -> openCompose(true));
        composeRow.addView(templateCompose, weightedButton());

        Button freeCompose = button("직접 쓰기", false);
        freeCompose.setOnClickListener(v -> openCompose(false));
        composeRow.addView(freeCompose, weightedButtonWithStartMargin());
        addView(composeRow, topMargin(10));

        addView(sectionTitle("빠른 메뉴"), topMargin(24));

        LinearLayout firstMenuRow = horizontalRow();
        Button library = button("템플릿 관리", false);
        library.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageTemplateLibraryActivity.class)));
        firstMenuRow.addView(library, weightedButton());

        Button campaigns = button("단체문자", false);
        campaigns.setOnClickListener(v -> {
            if (!requireMessageAccess()) return;
            getContext().startActivity(new Intent(getContext(), CampaignListActivity.class));
        });
        firstMenuRow.addView(campaigns, weightedButtonWithStartMargin());
        addView(firstMenuRow, topMargin(10));

        LinearLayout secondMenuRow = horizontalRow();
        Button history = button("발송내역", false);
        history.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageHistoryActivity.class)));
        secondMenuRow.addView(history, weightedButton());

        Button settings = button("자동문자 설정", false);
        settings.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageAutomationSettingsActivity.class)));
        secondMenuRow.addView(settings, weightedButtonWithStartMargin());
        addView(secondMenuRow, topMargin(8));

        addView(sectionTitle("자동문자"), topMargin(24));

        LinearLayout automation = card();
        connected = automationSwitch("통화 후 자동문자");
        missed = automationSwitch("부재중 자동문자");
        delayed = automationSwitch("후속문자 예약");
        automation.addView(connected, matchWrap());
        automation.addView(divider(), dividerParams());
        automation.addView(missed, matchWrap());
        automation.addView(divider(), dividerParams());
        automation.addView(delayed, matchWrap());
        addView(automation, topMargin(10));

        connected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rendering) return;
            if (!requireMessageAccess()) {
                render();
                return;
            }
            MessageAutomationStore.setConnectedEnabled(getContext(), isChecked);
        });
        missed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rendering) return;
            if (!requireMessageAccess()) {
                render();
                return;
            }
            MessageAutomationStore.setMissedEnabled(getContext(), isChecked);
        });
        delayed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rendering) return;
            if (!requireMessageAccess()) {
                render();
                return;
            }
            MessageAutomationStore.setDelayedEnabled(getContext(), isChecked);
            render();
        });

        LinearLayout utilityRow = horizontalRow();
        Button groups = compactButton("고객그룹");
        groups.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageGroupActivity.class)));
        utilityRow.addView(groups, compactWeightedButton());

        Button exclusion = compactButton("발송제외");
        exclusion.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageExclusionActivity.class)));
        utilityRow.addView(exclusion, compactWeightedButtonWithMargin());

        Button images = compactButton("이미지");
        images.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), TemplateImageLibraryActivity.class)));
        utilityRow.addView(images, compactWeightedButtonWithMargin());
        addView(utilityRow, topMargin(10));

        summary = body("");
        summary.setGravity(Gravity.CENTER);
        summary.setBackgroundResource(R.drawable.bg_soft_panel);
        summary.setPadding(dp(14), dp(12), dp(14), dp(12));
        addView(summary, topMargin(16));

        render();
    }

    private void render() {
        rendering = true;
        boolean access = FeatureEntitlementStore.hasMessageAccess(getContext());

        connected.setEnabled(access);
        missed.setEnabled(access);
        delayed.setEnabled(access);
        connected.setAlpha(access ? 1f : 0.45f);
        missed.setAlpha(access ? 1f : 0.45f);
        delayed.setAlpha(access ? 1f : 0.45f);

        connected.setText("통화 후 자동문자");
        missed.setText("부재중 자동문자");
        delayed.setText("후속문자 예약 · "
                + MessageAutomationStore.delayDays(getContext()) + "일 후");

        connected.setChecked(access && MessageAutomationStore.connectedEnabled(getContext()));
        missed.setChecked(access && MessageAutomationStore.missedEnabled(getContext()));
        delayed.setChecked(access && MessageAutomationStore.delayedEnabled(getContext()));

        MessageLogStore store = new MessageLogStore(getContext());
        try {
            int scheduled = store.countByStatus(MessageLogStore.STATUS_SCHEDULED);
            int sent = store.countByStatus(MessageLogStore.STATUS_SENT);
            int failed = store.countByStatus(MessageLogStore.STATUS_FAILED);
            summary.setText("예정 " + scheduled + "   ·   완료 " + sent + "   ·   실패 " + failed);
        } finally {
            store.close();
        }
        rendering = false;
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

    private Switch automationSwitch(String label) {
        Switch view = new Switch(getContext());
        view.setText(label);
        view.setTextColor(getContext().getColor(R.color.text_primary));
        view.setTextSize(15f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(0, dp(8), 0, dp(8));
        view.setMinHeight(dp(58));
        return view;
    }

    private LinearLayout horizontalRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        return row;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(18), dp(8), dp(18), dp(8));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getContext().getColor(R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private Button compactButton(String label) {
        Button button = button(label, false);
        button.setTextSize(13f);
        button.setMinWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        return button;
    }

    private TextView sectionTitle(String value) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextColor(getContext().getColor(R.color.text_primary));
        text.setTextSize(18f);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextColor(getContext().getColor(R.color.text_secondary));
        text.setTextSize(13f);
        text.setIncludeFontPadding(false);
        return text;
    }

    private View divider() {
        View view = new View(getContext());
        view.setBackgroundColor(getContext().getColor(R.color.border));
        return view;
    }

    private LayoutParams dividerParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, dp(1));
    }

    private LayoutParams weightedButton() {
        return new LayoutParams(0, dp(54), 1f);
    }

    private LayoutParams weightedButtonWithStartMargin() {
        LayoutParams params = weightedButton();
        params.leftMargin = dp(8);
        return params;
    }

    private LayoutParams compactWeightedButton() {
        return new LayoutParams(0, dp(46), 1f);
    }

    private LayoutParams compactWeightedButtonWithMargin() {
        LayoutParams params = compactWeightedButton();
        params.leftMargin = dp(7);
        return params;
    }

    private LayoutParams matchWrap() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams topMargin(int value) {
        LayoutParams params = matchWrap();
        params.topMargin = dp(value);
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
}
