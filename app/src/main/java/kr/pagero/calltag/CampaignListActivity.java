package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
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
        page.setPadding(dp(16), dp(10), dp(16), 0);
        page.setBackgroundColor(getColor(R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = text("단체문자", 21f, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);
        page.addView(header, matchWrap());

        TextView create = text("단체문자 만들기", 15f, true);
        create.setTextColor(getColor(android.R.color.white));
        create.setGravity(Gravity.CENTER);
        create.setBackgroundResource(R.drawable.bg_primary_button);
        create.setClickable(true);
        create.setFocusable(true);
        create.setOnClickListener(v -> startActivity(
                new Intent(this, CampaignComposerActivity.class)));
        page.addView(create, fixedHeight(52, 14));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, dp(14), 0, dp(36));
        scroll.addView(listContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void render() {
        listContainer.removeAllViews();
        List<CampaignStore.Campaign> campaigns = store.list();
        if (campaigns.isEmpty()) {
            TextView empty = body("등록된 단체문자가 없습니다");
            empty.setGravity(Gravity.CENTER);
            empty.setMinHeight(dp(64));
            empty.setBackgroundResource(R.drawable.bg_card);
            listContainer.addView(empty, matchWrap());
            return;
        }
        for (CampaignStore.Campaign original : campaigns) {
            CampaignStore.Campaign campaign = store.sync(this, original.id);
            if (campaign != null) listContainer.addView(campaignCard(campaign), bottomMargin(8));
        }
    }

    private View campaignCard(CampaignStore.Campaign campaign) {
        CampaignStore.Counts counts = store.counts(campaign.id);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(10), dp(13));
        card.setBackgroundResource(R.drawable.bg_clickable_card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> open(campaign.id));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(campaign.name, 15f, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView status = text(statusLabel(campaign.status), 12f, true);
        status.setTextColor(statusColor(campaign.status));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.leftMargin = dp(8);
        header.addView(status, statusParams);
        TextView arrow = text("›", 23f, false);
        arrow.setTextColor(getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        header.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(34)));
        card.addView(header, matchWrap());

        SimpleDateFormat date = new SimpleDateFormat("M/d a h:mm", Locale.KOREA);
        TextView meta = body(campaign.groupName + " · "
                + date.format(new Date(campaign.scheduledAt)) + " · "
                + SimProfileManager.labelForId(this, campaign.subscriptionId));
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(meta, topMargin(5));

        String progress = "전체 " + counts.total + " · 완료 " + counts.sent
                + " · 진행 " + counts.active + " · 실패 " + counts.failed
                + " · 제외 " + counts.skipped + " · 취소 " + counts.cancelled;
        TextView progressText = body(progress);
        progressText.setTextColor(getColor(R.color.text_primary));
        progressText.setSingleLine(true);
        progressText.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(progressText, topMargin(7));

        if (CampaignStore.STATUS_PAUSED.equals(campaign.status)
                && !campaign.pauseReason.trim().isEmpty()) {
            TextView pause = body(campaign.pauseReason);
            pause.setTextColor(getColor(R.color.danger));
            pause.setMaxLines(2);
            pause.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(pause, topMargin(6));
        }
        return card;
    }

    private void open(String id) {
        startActivity(new Intent(this, CampaignDetailActivity.class)
                .putExtra(CampaignDetailActivity.EXTRA_CAMPAIGN_ID, id));
    }

    private String statusLabel(String status) {
        if (CampaignStore.STATUS_SCHEDULED.equals(status)) return "예정";
        if (CampaignStore.STATUS_RUNNING.equals(status)) return "진행 중";
        if (CampaignStore.STATUS_PAUSED.equals(status)) return "일시정지";
        if (CampaignStore.STATUS_COMPLETED.equals(status)) return "완료";
        if (CampaignStore.STATUS_CANCELLED.equals(status)) return "취소";
        return "일부 완료";
    }

    private int statusColor(String status) {
        if (CampaignStore.STATUS_COMPLETED.equals(status)
                || CampaignStore.STATUS_RUNNING.equals(status)) return getColor(R.color.primary);
        if (CampaignStore.STATUS_PAUSED.equals(status)
                || CampaignStore.STATUS_PARTIAL.equals(status)) return getColor(R.color.danger);
        return getColor(R.color.text_secondary);
    }

    private TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView body(String value) {
        TextView text = text(value, 12f, false);
        text.setTextColor(getColor(R.color.text_secondary));
        return text;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fixedHeight(int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(top);
        return params;
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private LinearLayout.LayoutParams bottomMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(value);
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
