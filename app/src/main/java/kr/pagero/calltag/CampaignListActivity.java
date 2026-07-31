package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CampaignListActivity extends Activity {
    private CampaignStore store;
    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new CampaignStore(this);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private View buildContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(getColor(R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(16), dp(20), dp(12));
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("단체문자 캠페인", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        page.addView(header, matchWrap());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(20), 0, dp(20), 0);
        Button groups = button("그룹 관리", false);
        groups.setOnClickListener(v -> startActivity(new Intent(this, MessageGroupActivity.class)));
        actions.addView(groups, new LinearLayout.LayoutParams(0, dp(52), 1f));
        Button create = button("새 캠페인", true);
        create.setOnClickListener(v -> startActivity(new Intent(this, CampaignComposerActivity.class)));
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        createParams.leftMargin = dp(8);
        actions.addView(create, createParams);
        page.addView(actions, matchWrap());

        TextView guide = body("한 번에 한 캠페인만 실행합니다. 회선 변경이나 연속 통신 오류가 감지되면 남은 발송이 자동 일시정지됩니다.");
        guide.setPadding(dp(20), dp(10), dp(20), dp(10));
        page.addView(guide, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(20), dp(4), dp(20), dp(40));
        scroll.addView(listContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void render() {
        listContainer.removeAllViews();
        List<CampaignStore.Campaign> campaigns = store.list();
        if (campaigns.isEmpty()) {
            TextView empty = body("아직 단체문자 캠페인이 없습니다.\n그룹을 선택해 첫 캠페인을 만들어주세요.");
            empty.setGravity(Gravity.CENTER);
            empty.setBackgroundResource(R.drawable.bg_card);
            empty.setPadding(dp(18), dp(34), dp(18), dp(34));
            listContainer.addView(empty, matchWrap());
            return;
        }
        for (CampaignStore.Campaign original : campaigns) {
            CampaignStore.Campaign campaign = store.sync(this, original.id);
            if (campaign != null) listContainer.addView(campaignCard(campaign), topMargin(10));
        }
    }

    private View campaignCard(CampaignStore.Campaign campaign) {
        CampaignStore.Counts counts = store.counts(campaign.id);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_clickable_card);
        card.setOnClickListener(v -> open(campaign.id));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title(campaign.name, 17f),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView status = title(statusLabel(campaign.status), 13f);
        status.setTextColor(statusColor(campaign.status));
        header.addView(status);
        card.addView(header, matchWrap());

        SimpleDateFormat date = new SimpleDateFormat("M/d a h:mm", Locale.KOREA);
        card.addView(body(campaign.groupName + " · 시작 "
                + date.format(new Date(campaign.scheduledAt))), topMargin(6));
        card.addView(body("회선 · "
                + SimProfileManager.labelForId(this, campaign.subscriptionId)), topMargin(4));
        if (CampaignStore.STATUS_PAUSED.equals(campaign.status)
                && !campaign.pauseReason.trim().isEmpty()) {
            TextView pause = body(campaign.pauseReason);
            pause.setTextColor(getColor(R.color.danger));
            card.addView(pause, topMargin(6));
        }

        String progress = "전체 " + counts.total + "명 · 완료 " + counts.sent
                + " · 진행 " + counts.active + " · 실패 " + counts.failed
                + " · 건너뜀 " + counts.skipped + " · 취소 " + counts.cancelled;
        TextView progressText = body(progress);
        progressText.setTextColor(getColor(R.color.text_primary));
        progressText.setBackgroundResource(R.drawable.bg_soft_panel);
        progressText.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.addView(progressText, topMargin(10));

        Button detail = button(CampaignStore.STATUS_PAUSED.equals(campaign.status)
                ? "회선 확인하고 발송 재개" : "수신자별 상태 보기", false);
        detail.setOnClickListener(v -> open(campaign.id));
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        detailParams.topMargin = dp(10);
        card.addView(detail, detailParams);
        return card;
    }

    private void open(String id) {
        startActivity(new Intent(this, CampaignDetailActivity.class)
                .putExtra(CampaignDetailActivity.EXTRA_CAMPAIGN_ID, id));
    }

    private String statusLabel(String status) {
        if (CampaignStore.STATUS_SCHEDULED.equals(status)) return "발송 예정";
        if (CampaignStore.STATUS_RUNNING.equals(status)) return "진행 중";
        if (CampaignStore.STATUS_PAUSED.equals(status)) return "일시정지";
        if (CampaignStore.STATUS_COMPLETED.equals(status)) return "완료";
        if (CampaignStore.STATUS_CANCELLED.equals(status)) return "취소";
        return "일부 완료";
    }

    private int statusColor(String status) {
        if (CampaignStore.STATUS_COMPLETED.equals(status)) return getColor(R.color.primary);
        if (CampaignStore.STATUS_PAUSED.equals(status)
                || CampaignStore.STATUS_PARTIAL.equals(status)) return getColor(R.color.danger);
        return getColor(R.color.text_secondary);
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(13f);
        text.setLineSpacing(dp(3), 1f);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (store != null) store.close();
        super.onDestroy();
    }
}
