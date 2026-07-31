package kr.pagero.calltag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class PendingCallSectionView extends LinearLayout {
    public static final String ACTION_CHANGED = "kr.pagero.calltag.PENDING_CALLS_CHANGED";

    private boolean receiverRegistered;
    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    public PendingCallSectionView(Context context) {
        super(context);
        init();
    }

    public PendingCallSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PendingCallSectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setVisibility(GONE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerRefreshReceiver();
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (receiverRegistered) {
            try {
                getContext().unregisterReceiver(refreshReceiver);
            } catch (RuntimeException ignored) {
                // Receiver may already be removed with the activity.
            }
            receiverRegistered = false;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) refresh();
    }

    private void registerRefreshReceiver() {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter(ACTION_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getContext().registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getContext().registerReceiver(refreshReceiver, filter);
        }
        receiverRegistered = true;
    }

    public void refresh() {
        removeAllViews();
        PendingCallStore store = new PendingCallStore(getContext());
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            List<PendingCallRecord> calls = store.listPending(20);
            if (calls.isEmpty()) {
                setVisibility(GONE);
                return;
            }
            setVisibility(VISIBLE);
            addView(sectionTitle("확인할 통화", calls.size()), matchWrap());
            addView(text("부재중·거절·연결되지 않은 발신만 표시됩니다.",
                    13f, R.color.text_secondary, false), topMargin(6));
            for (PendingCallRecord call : calls) {
                addView(callCard(call, db), cardParams());
            }
        } finally {
            db.close();
            store.close();
        }
    }

    private View sectionTitle(String title, int count) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(title, 18f, R.color.text_primary, true);
        row.addView(name, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView badge = text(count + "건", 13f, R.color.primary, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(11), dp(5), dp(11), dp(5));
        badge.setBackgroundResource(R.drawable.bg_badge);
        row.addView(badge);
        return row;
    }

    private View callCard(PendingCallRecord call, CallTagDbHelper db) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);

        Customer customer = db.findByPhone(call.phone);
        String title = customer != null ? customer.displayName
                : !call.cachedName.trim().isEmpty() ? call.cachedName.trim() : call.phone;

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text(title, 17f, R.color.text_primary, true),
                new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView type = text(typeLabel(call), 12f, typeColor(call), true);
        type.setGravity(Gravity.CENTER);
        type.setPadding(dp(10), dp(5), dp(10), dp(5));
        type.setBackgroundResource(R.drawable.bg_badge);
        header.addView(type);
        card.addView(header, matchWrap());

        if (customer != null) {
            card.addView(text(customer.relationStatus, 14f, R.color.primary, true), topMargin(9));
            String memo = CustomerInsightResolver.latestMemo(db, customer);
            if (!memo.isEmpty()) {
                TextView memoView = text("최근 메모 · " + memo, 14f, R.color.text_primary, false);
                memoView.setMaxLines(2);
                memoView.setLineSpacing(0f, 1.2f);
                card.addView(memoView, topMargin(7));
            }
        } else {
            card.addView(text("등록되지 않은 번호", 14f, R.color.text_secondary, false), topMargin(9));
        }

        SimpleDateFormat formatter = new SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA);
        String meta = call.phone + " · " + formatter.format(new Date(call.startedAt));
        card.addView(text(meta, 13f, R.color.text_muted, false), topMargin(7));

        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(HORIZONTAL);

        Button callButton = actionButton("다시 전화", true);
        callButton.setOnClickListener(v -> dial(call.phone));
        actions.addView(callButton, new LayoutParams(0, dp(46), 1f));

        Button reviewButton = actionButton("할 일 등록", false);
        reviewButton.setOnClickListener(v -> openReview(call));
        LayoutParams reviewParams = new LayoutParams(0, dp(46), 1f);
        reviewParams.leftMargin = dp(8);
        actions.addView(reviewButton, reviewParams);
        card.addView(actions, topMargin(14));
        return card;
    }

    private void openReview(PendingCallRecord call) {
        Intent review = new Intent(getContext(), PostCallActivity.class)
                .putExtra(PostCallActivity.EXTRA_PENDING_CALL_ID, call.callLogId)
                .putExtra(PostCallActivity.EXTRA_PHONE, call.phone)
                .putExtra(PostCallActivity.EXTRA_CACHED_NAME, call.cachedName)
                .putExtra(PostCallActivity.EXTRA_CALL_TYPE, call.type)
                .putExtra(PostCallActivity.EXTRA_STARTED_AT, call.startedAt)
                .putExtra(PostCallActivity.EXTRA_ENDED_AT,
                        call.startedAt + Math.max(0L, call.durationSec) * 1000L)
                .putExtra(PostCallActivity.EXTRA_DURATION_SEC, call.durationSec);
        getContext().startActivity(review);
    }

    private void dial(String phone) {
        try {
            getContext().startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
        } catch (RuntimeException error) {
            Toast.makeText(getContext(), "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String typeLabel(PendingCallRecord call) {
        if (call.type == CallLog.Calls.MISSED_TYPE) return "부재중";
        if (call.type == CallLog.Calls.REJECTED_TYPE) return "거절";
        return "연결 안 됨";
    }

    private int typeColor(PendingCallRecord call) {
        if (call.type == CallLog.Calls.MISSED_TYPE || call.type == CallLog.Calls.REJECTED_TYPE) {
            return R.color.danger;
        }
        return R.color.primary;
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(getContext().getColor(R.color.text_primary));
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinWidth(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextColor(getContext().getColor(color));
        view.setTextSize(size);
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

    private LayoutParams cardParams() {
        LayoutParams params = matchWrap();
        params.topMargin = dp(12);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
