package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
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
        page.setPadding(dp(16), dp(10), dp(16), 0);
        page.setBackgroundColor(getColor(R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        pageTitle = text("단체문자 상세", 21f, true);
        pageTitle.setSingleLine(true);
        pageTitle.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(10);
        header.addView(pageTitle, titleParams);
        TextView refresh = text("새로고침", 12f, true);
        refresh.setTextColor(getColor(R.color.primary));
        refresh.setGravity(Gravity.CENTER);
        refresh.setOnClickListener(v -> render());
        header.addView(refresh, new LinearLayout.LayoutParams(dp(72), dp(42)));
        page.addView(header, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(14), 0, dp(36));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void render() {
        content.removeAllViews();
        CampaignStore.Campaign campaign = store.sync(this, campaignId);
        if (campaign == null) {
            TextView missing = body("단체문자 정보를 찾을 수 없습니다");
            missing.setGravity(Gravity.CENTER);
            missing.setMinHeight(dp(64));
            missing.setBackgroundResource(R.drawable.bg_card);
            content.addView(missing, matchWrap());
            return;
        }
        pageTitle.setText(campaign.name);
        CampaignStore.Counts counts = store.counts(campaignId);
        content.addView(summaryCard(campaign, counts), matchWrap());

        content.addView(sectionLabel("발송 제어"), topMargin(20));
        content.addView(actionArea(campaign, counts), topMargin(8));

        TextView label = sectionLabel("수신자 상태");
        List<CampaignStore.Recipient> recipients = store.recipients(campaignId);
        label.setText("수신자 상태 · " + recipients.size() + "명");
        content.addView(label, topMargin(22));
        for (CampaignStore.Recipient recipient : recipients) {
            content.addView(recipientCard(recipient), topMargin(7));
        }
    }

    private View summaryCard(CampaignStore.Campaign campaign, CampaignStore.Counts counts) {
        LinearLayout card = card();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView group = text(campaign.groupName, 15f, true);
        group.setSingleLine(true);
        group.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(group, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView status = text(campaignStatus(campaign.status), 12f, true);
        status.setTextColor(statusColor(campaign.status));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.leftMargin = dp(8);
        header.addView(status, statusParams);
        card.addView(header, matchWrap());

        SimpleDateFormat date = new SimpleDateFormat("M/d a h:mm", Locale.KOREA);
        TextView meta = body(date.format(new Date(campaign.scheduledAt))
                + " · " + SimProfileManager.labelForId(this, campaign.subscriptionId)
                + (campaign.templateName.isEmpty() ? "" : " · " + campaign.templateName));
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(meta, topMargin(5));

        TextView countsText = text("전체 " + counts.total + " · 완료 " + counts.sent
                + " · 진행 " + counts.active + " · 실패 " + counts.failed
                + " · 제외 " + counts.skipped + " · 취소 " + counts.cancelled, 13f, true);
        countsText.setSingleLine(true);
        countsText.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(countsText, topMargin(9));

        if (CampaignStore.STATUS_PAUSED.equals(campaign.status)
                && !campaign.pauseReason.trim().isEmpty()) {
            TextView reason = body(campaign.pauseReason);
            reason.setTextColor(getColor(R.color.danger));
            reason.setMaxLines(2);
            reason.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(reason, topMargin(6));
        }

        TextView bodySnapshot = body(campaign.bodyTemplate);
        bodySnapshot.setTextColor(getColor(R.color.text_primary));
        bodySnapshot.setBackgroundResource(R.drawable.bg_soft_panel);
        bodySnapshot.setPadding(dp(10), dp(8), dp(10), dp(8));
        bodySnapshot.setMaxLines(3);
        bodySnapshot.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(bodySnapshot, topMargin(9));
        return card;
    }

    private View actionArea(CampaignStore.Campaign campaign, CampaignStore.Counts counts) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        boolean paused = CampaignStore.STATUS_PAUSED.equals(campaign.status);
        LinearLayout controlRow = new LinearLayout(this);
        controlRow.setOrientation(LinearLayout.HORIZONTAL);

        Button pauseResume = button(paused ? "발송 재개" : "일시정지", paused);
        boolean canControl = counts.active > 0;
        pauseResume.setEnabled(canControl);
        pauseResume.setAlpha(canControl ? 1f : 0.45f);
        pauseResume.setOnClickListener(v -> {
            if (paused) resume(); else confirmPause();
        });
        controlRow.addView(pauseResume, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button cancel = button("남은 발송 취소", false);
        cancel.setEnabled(counts.active > 0);
        cancel.setAlpha(counts.active > 0 ? 1f : 0.45f);
        cancel.setOnClickListener(v -> confirmCancel());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        cancelParams.leftMargin = dp(7);
        controlRow.addView(cancel, cancelParams);
        wrapper.addView(controlRow, matchWrap());

        Button retry = button("실패·제외·취소 다시 보내기", true);
        boolean retryable = !paused && counts.active == 0
                && counts.failed + counts.skipped + counts.cancelled > 0;
        retry.setEnabled(retryable);
        retry.setAlpha(retryable ? 1f : 0.45f);
        retry.setOnClickListener(v -> retry());
        wrapper.addView(retry, fixedHeight(48, 7));

        Button delete = button("단체문자 내역 삭제", false);
        delete.setTextColor(getColor(R.color.danger));
        delete.setEnabled(counts.active == 0);
        delete.setAlpha(counts.active == 0 ? 1f : 0.45f);
        delete.setOnClickListener(v -> confirmDelete());
        wrapper.addView(delete, fixedHeight(46, 7));
        return wrapper;
    }

    private View recipientCard(CampaignStore.Recipient recipient) {
        LinearLayout card = card();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        String name = recipient.customerName.isEmpty() ? "이름 없는 고객" : recipient.customerName;
        TextView nameView = text(name, 14f, true);
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(nameView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView status = text(MessageDedupeEngine.statusLabel(recipient.status), 12f, true);
        status.setTextColor(recipientStatusColor(recipient.status));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.leftMargin = dp(8);
        header.addView(status, statusParams);
        card.addView(header, matchWrap());

        SimpleDateFormat date = new SimpleDateFormat("M/d a h:mm", Locale.KOREA);
        TextView meta = body(recipient.phone + " · " + date.format(new Date(recipient.scheduledAt)));
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(meta, topMargin(4));

        TextView message = body(recipient.body);
        message.setTextColor(getColor(R.color.text_primary));
        message.setMaxLines(2);
        message.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(message, topMargin(7));
        if (!recipient.reason.trim().isEmpty()) {
            TextView reason = body(recipient.reason);
            reason.setTextColor(MessageLogStore.STATUS_FAILED.equals(recipient.status)
                    ? getColor(R.color.danger) : getColor(R.color.text_secondary));
            reason.setMaxLines(2);
            reason.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(reason, topMargin(5));
        }
        return card;
    }

    private void confirmPause() {
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("단체문자 일시정지")
                .setMessage("아직 발송되지 않은 예약을 해제합니다. 발송 중인 한 건은 중단되지 않을 수 있습니다.")
                .setNegativeButton("닫기", null)
                .setPositiveButton("일시정지", (dialog, which) -> {
                    int count = CampaignManager.pause(this, campaignId);
                    Toast.makeText(this, count + "명의 남은 발송을 일시정지했습니다.",
                            Toast.LENGTH_LONG).show();
                    render();
                })
                .show();
    }

    private void resume() {
        try {
            int count = CampaignManager.resume(this, campaignId);
            Toast.makeText(this, count > 0
                    ? count + "명의 남은 발송을 다시 예약했습니다."
                    : "재개할 예약 작업이 없습니다.", Toast.LENGTH_LONG).show();
            render();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmCancel() {
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("남은 발송 취소")
                .setMessage("아직 전송되지 않은 수신자의 예약만 취소합니다.")
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
                    : "다시 예약할 수신자가 없습니다.", Toast.LENGTH_LONG).show();
            render();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("단체문자 내역 삭제")
                .setMessage("단체문자와 수신자별 진행 내역을 삭제할까요? 개별 문자 발송내역은 유지됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    try {
                        CampaignManager.delete(this, campaignId);
                        finish();
                    } catch (IllegalArgumentException error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                        render();
                    }
                })
                .show();
    }

    private String campaignStatus(String status) {
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

    private int recipientStatusColor(String status) {
        if (MessageLogStore.STATUS_SENT.equals(status)) return getColor(R.color.primary);
        if (MessageLogStore.STATUS_FAILED.equals(status)) return getColor(R.color.danger);
        return getColor(R.color.text_secondary);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView sectionLabel(String value) {
        TextView view = text(value, 13f, true);
        view.setTextColor(getColor(R.color.text_secondary));
        return view;
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

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
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

    private LinearLayout.LayoutParams fixedHeight(int height, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(topMargin);
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
