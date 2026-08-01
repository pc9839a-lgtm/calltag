package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MessageHistoryActivity extends Activity {
    private MessageLogStore store;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new MessageLogStore(this);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(27f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = title("발송 내역", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, topMargin(14));
        return scroll;
    }

    private void render() {
        list.removeAllViews();
        List<MessageRecord> records = store.listRecent(200);
        if (records.isEmpty()) {
            TextView empty = body("발송하거나 예약한 문자가 없습니다");
            empty.setGravity(Gravity.CENTER);
            empty.setMinHeight(dp(64));
            empty.setBackgroundResource(R.drawable.bg_card);
            list.addView(empty, matchWrap());
            return;
        }

        SimpleDateFormat date = new SimpleDateFormat("M/d a h:mm", Locale.KOREA);
        for (MessageRecord record : records) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(13), dp(14), dp(13));
            card.setBackgroundResource(R.drawable.bg_card);

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView trigger = title(triggerLabel(record.triggerType), 14f);
            trigger.setSingleLine(true);
            trigger.setEllipsize(TextUtils.TruncateAt.END);
            header.addView(trigger, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView status = title(statusLabel(record), 12f);
            status.setTextColor(statusColor(record));
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            statusParams.leftMargin = dp(8);
            header.addView(status, statusParams);
            card.addView(header, matchWrap());

            boolean scheduled = MessageLogStore.STATUS_SCHEDULED.equals(record.status);
            String attachment = MmsComposer.hasAttachment(this, record.id) ? " · 이미지" : "";
            TextView meta = body(record.phone + attachment + " · " + date.format(new Date(
                    scheduled ? record.scheduledAt : record.createdAt)));
            meta.setSingleLine(true);
            meta.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(meta, topMargin(6));

            TextView message = body(record.body);
            message.setTextColor(getColor(R.color.text_primary));
            message.setTextSize(14f);
            message.setMaxLines(3);
            message.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(message, topMargin(9));

            boolean duplicateBlocked = MessageDedupeEngine.isDuplicateReason(record.error);
            if (record.error != null && !record.error.trim().isEmpty()) {
                TextView error = body(record.error);
                error.setMaxLines(2);
                error.setEllipsize(TextUtils.TruncateAt.END);
                boolean neutral = duplicateBlocked
                        || MmsComposer.isComposeRequired(record.error)
                        || MmsComposer.isComposerOpened(record.error);
                error.setTextColor(getColor(neutral
                        ? R.color.text_secondary : R.color.danger));
                if (neutral) {
                    error.setBackgroundResource(R.drawable.bg_soft_panel);
                    error.setPadding(dp(10), dp(8), dp(10), dp(8));
                }
                card.addView(error, topMargin(8));
            }

            if (scheduled) {
                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                Button sendNow = button(MmsComposer.hasAttachment(this, record.id)
                        ? "메시지 앱 열기" : "지금 보내기", true);
                sendNow.setOnClickListener(v -> sendScheduledNow(record));
                actions.addView(sendNow, new LinearLayout.LayoutParams(0, dp(44), 1f));
                Button cancel = button("예약 취소", false);
                cancel.setOnClickListener(v -> cancel(record));
                LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
                cancelParams.leftMargin = dp(7);
                actions.addView(cancel, cancelParams);
                card.addView(actions, topMargin(10));
            } else if (MmsComposer.isComposeRequired(record.error)) {
                Button compose = button("메시지 앱에서 보내기", true);
                compose.setOnClickListener(v -> openMms(record));
                LinearLayout.LayoutParams composeParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
                composeParams.topMargin = dp(10);
                card.addView(compose, composeParams);
            } else if (MessageLogStore.STATUS_SKIPPED.equals(record.status)
                    && duplicateBlocked) {
                Button force = button("중복 확인 후 다시 보내기", false);
                force.setOnClickListener(v -> confirmForceResend(record));
                LinearLayout.LayoutParams forceParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
                forceParams.topMargin = dp(10);
                card.addView(force, forceParams);
            }

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(8);
            list.addView(card, params);
        }
    }

    private void sendScheduledNow(MessageRecord record) {
        MessageScheduler.cancel(this, record.id);
        if (MmsComposer.hasAttachment(this, record.id)) {
            openMms(record);
            return;
        }
        store.markReady(record.id);
        SmsSender.sendExisting(this, record.id);
        Toast.makeText(this, "문자 발송을 요청했습니다.", Toast.LENGTH_SHORT).show();
        render();
    }

    private void openMms(MessageRecord record) {
        if (MmsComposer.openComposer(this, record.id)) {
            Toast.makeText(this, "메시지 앱에서 전송 버튼을 눌러주세요.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "이미지 문자 작성창을 열지 못했습니다.",
                    Toast.LENGTH_LONG).show();
        }
        render();
    }

    private void cancel(MessageRecord record) {
        MessageScheduler.cancel(this, record.id);
        MmsComposer.cancelNotification(this, record.id);
        MmsComposer.forget(this, record.id);
        store.cancel(record.id, "사용자가 예약을 취소했습니다.");
        Toast.makeText(this, "예약을 취소했습니다.", Toast.LENGTH_SHORT).show();
        render();
    }

    private void confirmForceResend(MessageRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("다시 보내기")
                .setMessage("중복방지를 무시하고 다시 발송합니다. 문자요금이 다시 발생할 수 있습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("발송", (dialog, which) -> {
                    long id = SmsSender.forceResend(this, record);
                    MessageRecord created = store.find(id);
                    if (created != null && MessageLogStore.STATUS_SKIPPED.equals(created.status)) {
                        Toast.makeText(this, created.error, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "재발송을 요청했습니다.", Toast.LENGTH_LONG).show();
                    }
                    render();
                })
                .show();
    }

    private String triggerLabel(String trigger) {
        if (MessageAutomationManager.TRIGGER_INCOMING.equals(trigger)) return "수신 통화 자동문자";
        if (MessageAutomationManager.TRIGGER_OUTGOING.equals(trigger)) return "발신 통화 자동문자";
        if (MessageAutomationManager.TRIGGER_CONNECTED.equals(trigger)) return "통화 후 자동문자";
        if (MessageAutomationManager.TRIGGER_MISSED.equals(trigger)) return "부재중 자동문자";
        if (MessageAutomationManager.TRIGGER_DELAYED.equals(trigger)) return "후속 예약문자";
        if (MessageAutomationManager.TRIGGER_CAMPAIGN.equals(trigger)) return "단체문자";
        return "고객 문자";
    }

    private String statusLabel(MessageRecord record) {
        if (MmsComposer.isComposeRequired(record.error)) return "전송 필요";
        if (MmsComposer.isComposerOpened(record.error)) return "메시지 앱 열림";
        return MessageDedupeEngine.statusLabel(record.status);
    }

    private int statusColor(MessageRecord record) {
        if (MmsComposer.isComposeRequired(record.error)) return getColor(R.color.danger);
        if (MmsComposer.isComposerOpened(record.error)) return getColor(R.color.primary);
        if (MessageLogStore.STATUS_SENT.equals(record.status)) return getColor(R.color.primary);
        if (MessageLogStore.STATUS_FAILED.equals(record.status)) return getColor(R.color.danger);
        if (MessageLogStore.STATUS_SCHEDULED.equals(record.status)) return getColor(R.color.text_primary);
        return getColor(R.color.text_secondary);
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(13f);
        text.setIncludeFontPadding(false);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (store != null) store.close();
        super.onDestroy();
    }
}
