package kr.pagero.calltag;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
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
        root.setPadding(dp(20), dp(18), dp(20), dp(44));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("발송·예약 내역", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, topMargin(20));
        return scroll;
    }

    private void render() {
        list.removeAllViews();
        List<MessageRecord> records = store.listRecent(200);
        if (records.isEmpty()) {
            TextView empty = body("아직 발송하거나 예약한 문자가 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(28), dp(20), dp(28));
            empty.setBackgroundResource(R.drawable.bg_card);
            list.addView(empty, matchWrap());
            return;
        }

        SimpleDateFormat date = new SimpleDateFormat("M/d a h:mm", Locale.KOREA);
        for (MessageRecord record : records) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(15), dp(18), dp(15));
            card.setBackgroundResource(R.drawable.bg_card);

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(title(triggerLabel(record.triggerType), 15f),
                    new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView status = title(statusLabel(record.status), 13f);
            status.setTextColor(statusColor(record.status));
            header.addView(status);
            card.addView(header, matchWrap());

            card.addView(body(record.phone + " · " + date.format(new Date(
                    MessageLogStore.STATUS_SCHEDULED.equals(record.status)
                            ? record.scheduledAt : record.createdAt))), topMargin(7));

            TextView message = body(record.body);
            message.setTextColor(getColor(R.color.text_primary));
            message.setTextSize(14f);
            card.addView(message, topMargin(10));

            if (record.error != null && !record.error.trim().isEmpty()) {
                TextView error = body(record.error);
                error.setTextColor(getColor(R.color.danger));
                card.addView(error, topMargin(8));
            }

            if (MessageLogStore.STATUS_SCHEDULED.equals(record.status)) {
                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                Button sendNow = button("지금 보내기", true);
                sendNow.setOnClickListener(v -> sendScheduledNow(record));
                actions.addView(sendNow, new LinearLayout.LayoutParams(0, dp(46), 1f));
                Button cancel = button("예약 취소", false);
                cancel.setOnClickListener(v -> cancel(record));
                LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
                cancelParams.leftMargin = dp(8);
                actions.addView(cancel, cancelParams);
                card.addView(actions, topMargin(12));
            }

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            list.addView(card, params);
        }
    }

    private void sendScheduledNow(MessageRecord record) {
        MessageScheduler.cancel(this, record.id);
        store.markReady(record.id);
        SmsSender.sendExisting(this, record.id);
        Toast.makeText(this, "문자 발송을 요청했습니다.", Toast.LENGTH_SHORT).show();
        render();
    }

    private void cancel(MessageRecord record) {
        MessageScheduler.cancel(this, record.id);
        store.cancel(record.id, "사용자가 예약을 취소했습니다.");
        Toast.makeText(this, "예약을 취소했습니다.", Toast.LENGTH_SHORT).show();
        render();
    }

    private String triggerLabel(String trigger) {
        if (MessageAutomationManager.TRIGGER_CONNECTED.equals(trigger)) return "통화 종료 자동문자";
        if (MessageAutomationManager.TRIGGER_MISSED.equals(trigger)) return "부재중·거절 자동문자";
        if (MessageAutomationManager.TRIGGER_DELAYED.equals(trigger)) return "후속 예약문자";
        return "수동 문자";
    }

    private String statusLabel(String status) {
        if (MessageLogStore.STATUS_SCHEDULED.equals(status)) return "발송 예정";
        if (MessageLogStore.STATUS_READY.equals(status)) return "발송 준비";
        if (MessageLogStore.STATUS_SENDING.equals(status)) return "발송 중";
        if (MessageLogStore.STATUS_SENT.equals(status)) return "발송 완료";
        if (MessageLogStore.STATUS_FAILED.equals(status)) return "발송 실패";
        if (MessageLogStore.STATUS_SKIPPED.equals(status)) return "건너뜀";
        if (MessageLogStore.STATUS_CANCELLED.equals(status)) return "취소됨";
        return status;
    }

    private int statusColor(String status) {
        if (MessageLogStore.STATUS_SENT.equals(status)) return getColor(R.color.primary);
        if (MessageLogStore.STATUS_FAILED.equals(status)) return getColor(R.color.danger);
        if (MessageLogStore.STATUS_SCHEDULED.equals(status)) return getColor(R.color.text_primary);
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
