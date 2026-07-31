package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CampaignDetailActivity extends Activity {
    public static final String EXTRA_CAMPAIGN_ID = "campaign_id";

    private CampaignStore store;
    private String campaignId;
    private TextView pageTitle;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        campaignId = safe(getIntent().getStringExtra(EXTRA_CAMPAIGN_ID));
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
        pageTitle = title("캠페인 상세", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(pageTitle, titleParams);
        Button refresh = button("새로고침", false);
        refresh.setOnClickListener(v -> render());
        header.addView(refresh, new LinearLayout.LayoutParams(dp(104), dp(48)));
        page.addView(header, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(2), dp(20), dp(42));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void render() {
        content.removeAllViews();
        CampaignStore.Campaign campaign = store.sync(this, campaignId);
        if (campaign == null) {
            TextView missing = body("캠페인 정보를 찾을 수 없습니다.");
            missing.setGravity(Gravity.CENTER);
            content.addView(missing, topMargin(30));
            return;
        }
        pageTitle.setText(campaign.name);
        CampaignStore.Counts counts = store.counts(campaignId);
        content.addView(summaryCard(campaign, counts), matchWrap());
        content.addView(actionRow(campaign, counts), topMargin(10));

        TextView label = title("수신자별 상태", 16f);
        content.addView(label, topMargin(24));
        List<CampaignStore.Recipient> recipients = store.recipients(campaignId);
        for (CampaignStore.Recipient recipient : recipients) {
            content.addView(recipientCard(recipient), topMargin(10));
        }
    }

    private View summaryCard(CampaignStore.Campaign campaign, CampaignStore.Counts counts) {
        LinearLayout card = card();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title(campaign.groupName, 16f),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView status = title(campaignStatus(campaign.status), 13f);
        status.setTextColor(CampaignStore.STATUS_COMPLETED.equals(campaign.status)
                ? getColor(R.color.primary) : getColor(R.color.text_secondary));
        header.addView(status);
        card.addView(header, matchWrap());

        SimpleDateFormat date = new SimpleDateFormat("M/d a h:mm", Locale.KOREA);
        card.addView(body("시작 " + date.format(new Date(campaign.scheduledAt))
                + (campaign.templateName.isEmpty() ? "" : " · " + campaign.templateName)), topMargin(6));

        TextView numbers = title("전체 " + counts.total + "명", 20f);
        card.addView(numbers, topMargin(14));
        card.addView(body("발송 완료 " + counts.sent + " · 진행 중 " + counts.active
                + " · 실패 " + counts.failed + " · 건너뜀 " + counts.skipped
                + " · 취소 " + counts.cancelled), topMargin(6));

        TextView bodySnapshot = body(campaign.bodyTemplate);
        bodySnapshot.setTextColor(getColor(R.color.text_primary));
        bodySnapshot.setBackgroundResource(R.drawable.bg_soft_panel);
        bodySnapshot.setPadding(dp(12), dp(10), dp(12), dp(10));
        bodySnapshot.setMaxLines(5);
        card.addView(bodySnapshot, topMargin(12));
        return card;
    }

    private View actionRow(CampaignStore.Campaign campaign, CampaignStore.Counts counts) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel = button("남은 발송 취소", false);
        cancel.setEnabled(counts.active > 0);
        cancel.setAlpha(counts.active > 0 ? 1f : 0.45f);
        cancel.setOnClickListener(v -> confirmCancel());
        row.addView(cancel, new LinearLayout.LayoutParams(0, dp(50), 1f));
        Button retry = button("실패·건너뜀 재시도", true);
        boolean retryable = counts.failed + counts.skipped + counts.cancelled > 0;
        retry.setEnabled(retryable);
        retry.setAlpha(retryable ? 1f : 0.45f);
        retry.setOnClickListener(v -> retry());
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(0, dp(50), 1f);
        retryParams.leftMargin = dp(8);
        row.addView(retry, retryParams);
        wrapper.addView(row, matchWrap());

        Button delete = button("캠페인 내역 삭제", false);
        delete.setEnabled(counts.active == 0);
        delete.setAlpha(counts.active == 0 ? 1f : 0.45f);
        delete.setOnClickListener(v -> confirmDelete());
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        deleteParams.topMargin = dp(8);
        wrapper.addView(delete, deleteParams);
        return wrapper;
    }

    private View recipientCard(CampaignStore.Recipient recipient) {
        LinearLayout card = card();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        String name = recipient.customerName.isEmpty() ? "이름 없는 고객" : recipient.customerName;
        header.addView(title(name, 15f),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView status = title(MessageDedupeEngine.statusLabel(recipient.status), 13f);
        status.setTextColor(statusColor(recipient.status));
        header.addView(status);
        card.addView(header, matchWrap());

        SimpleDateFormat date = new SimpleDateFormat("M/d a h:mm:ss", Locale.KOREA);
        card.addView(body(recipient.phone + " · " + date.format(new Date(recipient.scheduledAt))), topMargin(5));
        TextView message = body(recipient.body);
        message.setTextColor(getColor(R.color.text_primary));
        message.setMaxLines(3);
        card.addView(message, topMargin(9));
        if (!recipient.reason.trim().isEmpty()) {
            TextView reason = body(recipient.reason);
            reason.setTextColor(MessageLogStore.STATUS_FAILED.equals(recipient.status)
                    ? getColor(R.color.danger) : getColor(R.color.text_secondary));
            reason.setBackgroundResource(R.drawable.bg_soft_panel);
            reason.setPadding(dp(10), dp(8), dp(10), dp(8));
            card.addView(reason, topMargin(8));
        }
        return card;
    }

    private void confirmCancel() {
        new AlertDialog.Builder(this)
                .setTitle("남은 발송 취소")
                .setMessage("아직 전송되지 않은 수신자의 예약만 취소합니다. 이미 발송된 문자는 되돌릴 수 없습니다.")
                .setNegativeButton("닫기", null)
                .setPositiveButton("취소 실행", (dialog, which) -> {
                    CampaignManager.cancel(this, campaignId);
                    render();
                })
                .show();
    }

    private void retry() {
        try {
            int count = CampaignManager.retryFailed(this, campaignId);
            Toast.makeText(this, count > 0
                    ? count + "명의 발송을 다시 예약했습니다."
                    : "현재 다시 예약할 수신자가 없습니다.", Toast.LENGTH_LONG).show();
            render();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("캠페인 내역 삭제")
                .setMessage("캠페인과 수신자별 진행 내역을 삭제할까요? 개별 문자 발송내역은 유지됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    store.delete(campaignId);
                    finish();
                })
                .show();
    }

    private String campaignStatus(String status) {
        if (CampaignStore.STATUS_SCHEDULED.equals(status)) return "발송 예정";
        if (CampaignStore.STATUS_RUNNING.equals(status)) return "진행 중";
        if (CampaignStore.STATUS_COMPLETED.equals(status)) return "완료";
        if (CampaignStore.STATUS_CANCELLED.equals(status)) return "취소";
        return "일부 완료";
    }

    private int statusColor(String status) {
        if (MessageLogStore.STATUS_SENT.equals(status)) return getColor(R.color.primary);
        if (MessageLogStore.STATUS_FAILED.equals(status)) return getColor(R.color.danger);
        return getColor(R.color.text_secondary);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
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
