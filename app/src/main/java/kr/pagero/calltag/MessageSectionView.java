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

    private TextView plan;
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

        LinearLayout planCard = card();
        plan = title("", 18f);
        planCard.addView(plan, matchWrap());
        planCard.addView(body("전화관리 1,900원 · 문자자동화 990원 · 둘 다 2,500원"), topMargin(7));
        addView(planCard, matchWrap());

        TextView startLabel = title("문자 보내기", 15f);
        startLabel.setTextColor(getContext().getColor(R.color.text_secondary));
        addView(startLabel, topMargin(22));

        LinearLayout composeRow = new LinearLayout(getContext());
        composeRow.setOrientation(HORIZONTAL);
        Button template = button("템플릿으로 시작", true);
        template.setOnClickListener(v -> openCompose(true));
        composeRow.addView(template, new LayoutParams(0, dp(54), 1f));
        Button free = button("자유롭게 작성", false);
        free.setOnClickListener(v -> openCompose(false));
        LayoutParams freeParams = new LayoutParams(0, dp(54), 1f);
        freeParams.leftMargin = dp(8);
        composeRow.addView(free, freeParams);
        addView(composeRow, topMargin(10));

        LinearLayout managementRow = new LinearLayout(getContext());
        managementRow.setOrientation(HORIZONTAL);
        Button library = button("템플릿", false);
        library.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageTemplateLibraryActivity.class)));
        managementRow.addView(library, new LayoutParams(0, dp(52), 1f));
        Button images = button("템플릿 이미지", false);
        images.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), TemplateImageLibraryActivity.class)));
        LayoutParams imageParams = new LayoutParams(0, dp(52), 1f);
        imageParams.leftMargin = dp(8);
        managementRow.addView(images, imageParams);
        Button exclusion = button("발송 제외", false);
        exclusion.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageExclusionActivity.class)));
        LayoutParams exclusionParams = new LayoutParams(0, dp(52), 1f);
        exclusionParams.leftMargin = dp(8);
        managementRow.addView(exclusion, exclusionParams);
        addView(managementRow, topMargin(10));

        TextView imageGuide = body("이미지 템플릿은 수동 문자와 예약 알림에서 사용합니다. 자동발송 기본 템플릿은 텍스트 전용입니다.");
        addView(imageGuide, topMargin(8));

        TextView autoLabel = title("자동 발송", 15f);
        autoLabel.setTextColor(getContext().getColor(R.color.text_secondary));
        addView(autoLabel, topMargin(22));

        LinearLayout automation = card();
        connected = automationSwitch("통화 종료 후 자동 발송",
                "수신·발신 통화별 기본 템플릿으로 발송");
        missed = automationSwitch("부재중·거절 자동 발송",
                "받지 못한 전화에 부재중 기본 템플릿 발송");
        delayed = automationSwitch("후속문자 자동 예약",
                "통화 후 지정한 시점에 후속 템플릿 발송");
        automation.addView(connected, matchWrap());
        automation.addView(divider(), new LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        automation.addView(missed, matchWrap());
        automation.addView(divider(), new LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        automation.addView(delayed, matchWrap());
        addView(automation, topMargin(10));

        connected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rendering || !requireMessageAccess()) return;
            MessageAutomationStore.setConnectedEnabled(getContext(), isChecked);
        });
        missed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rendering || !requireMessageAccess()) return;
            MessageAutomationStore.setMissedEnabled(getContext(), isChecked);
        });
        delayed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rendering || !requireMessageAccess()) return;
            MessageAutomationStore.setDelayedEnabled(getContext(), isChecked);
            render();
        });

        Button settings = button("회선·업무시간·중복방지 설정", false);
        settings.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageAutomationSettingsActivity.class)));
        LayoutParams settingsParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(52));
        settingsParams.topMargin = dp(10);
        addView(settings, settingsParams);

        summary = body("");
        summary.setBackgroundResource(R.drawable.bg_card);
        summary.setPadding(dp(18), dp(15), dp(18), dp(15));
        addView(summary, topMargin(18));

        Button history = button("발송·예약 내역", false);
        history.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), MessageHistoryActivity.class)));
        LayoutParams historyParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(52));
        historyParams.topMargin = dp(10);
        addView(history, historyParams);
        render();
    }

    private void render() {
        rendering = true;
        boolean access = FeatureEntitlementStore.hasMessageAccess(getContext());
        plan.setText(FeatureEntitlementStore.planLabel(getContext()));
        connected.setEnabled(access);
        missed.setEnabled(access);
        delayed.setEnabled(access);
        connected.setAlpha(access ? 1f : 0.45f);
        missed.setAlpha(access ? 1f : 0.45f);
        delayed.setAlpha(access ? 1f : 0.45f);
        connected.setChecked(access && MessageAutomationStore.connectedEnabled(getContext()));
        missed.setChecked(access && MessageAutomationStore.missedEnabled(getContext()));
        delayed.setText("후속문자 자동 예약\n발송 시점 "
                + MessageAutomationStore.delayDays(getContext()) + "일 후 · 후속 기본 템플릿 사용");
        delayed.setChecked(access && MessageAutomationStore.delayedEnabled(getContext()));

        MessageLogStore store = new MessageLogStore(getContext());
        try {
            int scheduled = store.countByStatus(MessageLogStore.STATUS_SCHEDULED);
            int sent = store.countByStatus(MessageLogStore.STATUS_SENT);
            int failed = store.countByStatus(MessageLogStore.STATUS_FAILED);
            int exclusions = MessageExclusionStore.list(getContext()).size();
            summary.setText("발송 완료  " + sent + "건\n발송 예정  " + scheduled
                    + "건\n발송 실패  " + failed + "건\n발송 제외 고객  " + exclusions + "명");
        } finally {
            store.close();
        }
        rendering = false;
    }

    private boolean requireMessageAccess() {
        if (FeatureEntitlementStore.hasMessageAccess(getContext())) return true;
        Toast.makeText(getContext(), "문자자동화 이용권이 필요합니다.", Toast.LENGTH_SHORT).show();
        render();
        return false;
    }

    private void openCompose(boolean useTemplate) {
        if (!requireMessageAccess()) return;
        getContext().startActivity(new Intent(getContext(), ManualMessageActivity.class)
                .putExtra(ManualMessageActivity.EXTRA_USE_TEMPLATE, useTemplate));
    }

    private Switch automationSwitch(String title, String subtitle) {
        Switch view = new Switch(getContext());
        view.setText(title + "\n" + subtitle);
        view.setTextColor(getContext().getColor(R.color.text_primary));
        view.setTextSize(15f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setLineSpacing(dp(3), 1f);
        view.setPadding(0, dp(10), 0, dp(10));
        view.setMinHeight(dp(72));
        return view;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
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

    private TextView title(String value, float size) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextColor(getContext().getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextColor(getContext().getColor(R.color.text_secondary));
        text.setTextSize(14f);
        text.setLineSpacing(dp(3), 1f);
        return text;
    }

    private View divider() {
        View view = new View(getContext());
        view.setBackgroundColor(getContext().getColor(R.color.border));
        return view;
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
        if (visibility == VISIBLE && plan != null) render();
    }
}
