package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Read-only recent call history rendered from CallTag's own interactions table.
 * It never writes to ContactsContract or the system CallLog provider.
 */
public final class RecentCallHistoryView extends LinearLayout {
    private static final int MAX_VISIBLE = 8;

    public RecentCallHistoryView(Context context) {
        super(context);
        init();
    }

    public RecentCallHistoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecentCallHistoryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refresh();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) refresh();
    }

    private void refresh() {
        removeAllViews();

        TextView title = text("최근 통화", 18f, R.color.text_primary, true);
        addView(title, matchWrap());
        TextView description = text("전화앱이나 연락처를 바꾸지 않고 콜태그에 저장된 통화·메모만 보여줍니다.",
                12f, R.color.text_muted, false);
        description.setLineSpacing(0f, 1.2f);
        addView(description, topMargin(5));

        CallTagDbHelper db = new CallTagDbHelper(getContext());
        int shown = 0;
        try {
            List<InteractionRecord> records = db.listRecentInteractions(40);
            for (InteractionRecord record : records) {
                if (!isCall(record.type)) continue;
                addView(callCard(record), topMargin(9));
                shown++;
                if (shown >= MAX_VISIBLE) break;
            }
        } catch (RuntimeException error) {
            CrashTelemetryStore.record(getContext(), "recent_call_history", "render_failed",
                    error.getClass().getSimpleName());
        } finally {
            db.close();
        }

        if (shown == 0) {
            TextView empty = text("아직 저장된 통화 이력이 없습니다.", 13f,
                    R.color.text_secondary, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(14), dp(16), dp(14), dp(16));
            empty.setBackgroundResource(R.drawable.bg_card);
            addView(empty, topMargin(9));
        }
    }

    private View callCard(InteractionRecord record) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openCustomer(record.customerId));

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(record.customerName, 15f, R.color.text_primary, true);
        header.addView(name, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView type = text(callTypeLabel(record.type), 12f, R.color.primary, true);
        header.addView(type);
        card.addView(header, matchWrap());

        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String meta = formatter.format(new Date(record.startedAt));
        if (record.durationSec > 0L) meta += " · " + durationLabel(record.durationSec);
        card.addView(text(meta, 12f, R.color.text_muted, false), topMargin(5));

        String note = record.note == null ? "" : record.note.trim();
        if (!note.isEmpty()) {
            TextView memo = text(note, 13f, R.color.text_primary, false);
            memo.setMaxLines(2);
            card.addView(memo, topMargin(8));
        }
        return card;
    }

    private void openCustomer(long customerId) {
        if (customerId <= 0L) return;
        try {
            getContext().startActivity(new Intent(getContext(), CustomerDetailActivity.class)
                    .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customerId));
        } catch (RuntimeException error) {
            CrashTelemetryStore.record(getContext(), "recent_call_history", "open_failed",
                    error.getClass().getSimpleName());
        }
    }

    private boolean isCall(String type) {
        return type != null && (type.equals("INCOMING_CALL")
                || type.equals("OUTGOING_CALL")
                || type.equals("MISSED_CALL")
                || type.equals("REJECTED_CALL"));
    }

    private String callTypeLabel(String type) {
        if ("OUTGOING_CALL".equals(type)) return "발신";
        if ("MISSED_CALL".equals(type)) return "부재중";
        if ("REJECTED_CALL".equals(type)) return "거절";
        return "수신";
    }

    private String durationLabel(long seconds) {
        long safe = Math.max(0L, seconds);
        long minutes = safe / 60L;
        long remain = safe % 60L;
        return minutes > 0L ? minutes + "분 " + remain + "초" : remain + "초";
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value == null ? "" : value);
        view.setTextSize(size);
        view.setTextColor(getContext().getColor(color));
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LayoutParams matchWrap() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams topMargin(int margin) {
        LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
