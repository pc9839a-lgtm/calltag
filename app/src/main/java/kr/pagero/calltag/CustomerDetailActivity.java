package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public final class CustomerDetailActivity extends Activity {
    public static final String EXTRA_CUSTOMER_ID = "customer_id";

    private CallTagDbHelper db;
    private long customerId;
    private Customer customer;
    private TextView nameView;
    private TextView phoneView;
    private TextView statusView;
    private EditText memoInput;
    private LinearLayout interactionList;
    private TextView interactionEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);
        db = new CallTagDbHelper(this);
        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, -1L);
        bindViews();
        bindActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomer();
    }

    private void bindViews() {
        nameView = findViewById(R.id.detailName);
        phoneView = findViewById(R.id.detailPhone);
        statusView = findViewById(R.id.detailStatus);
        memoInput = findViewById(R.id.detailMemo);
        interactionList = findViewById(R.id.detailInteractionList);
        interactionEmpty = findViewById(R.id.detailInteractionEmpty);
    }

    private void bindActions() {
        findViewById(R.id.detailBack).setOnClickListener(v -> finish());
        findViewById(R.id.detailCall).setOnClickListener(v -> dial());
        findViewById(R.id.detailSchedule).setOnClickListener(v -> showScheduleDialog());
        findViewById(R.id.detailChangeStatus).setOnClickListener(v -> showStatusDialog());
        findViewById(R.id.detailSaveMemo).setOnClickListener(v -> saveMemo());
    }

    private void loadCustomer() {
        customer = db.findCustomerById(customerId);
        if (customer == null) {
            Toast.makeText(this, "고객 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        nameView.setText(customer.displayName);
        phoneView.setText(customer.primaryPhone);
        statusView.setText(statusLabel(customer.relationStatus));
        statusView.setTextColor(getColor(statusColor(customer.relationStatus)));
        if (!memoInput.hasFocus()) memoInput.setText(customer.memo);
        renderInteractions();
    }

    private void renderInteractions() {
        interactionList.removeAllViews();
        List<InteractionRecord> records = db.listInteractionsForCustomer(customerId);
        interactionEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        for (InteractionRecord record : records) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            card.setBackgroundResource(R.drawable.bg_card);

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView result = text(resultLabel(record.result), 16f, R.color.text_primary, true);
            header.addView(result, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView date = text(formatter.format(new Date(record.startedAt)), 12f, R.color.text_muted, false);
            header.addView(date);
            card.addView(header, matchWrap());

            TextView meta = text(typeLabel(record.type) + " · " + durationLabel(record.durationSec),
                    13f, R.color.text_secondary, false);
            card.addView(meta, topMargin(7));

            if (record.note != null && !record.note.trim().isEmpty()) {
                TextView note = text(record.note.trim(), 14f, R.color.text_primary, false);
                note.setLineSpacing(0f, 1.25f);
                card.addView(note, topMargin(10));
            }

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            interactionList.addView(card, params);
        }
    }

    private void saveMemo() {
        if (customer == null) return;
        db.updateCustomerProfile(customer.id, customer.displayName,
                customer.relationStatus, memoInput.getText().toString());
        Toast.makeText(this, "저장했습니다.", Toast.LENGTH_SHORT).show();
        memoInput.clearFocus();
        loadCustomer();
    }

    private void showStatusDialog() {
        if (customer == null) return;
        String[] labels = {"신규", "상담 중", "기존", "VIP", "휴면"};
        String[] values = {
                CallTagDbHelper.STATUS_NEW,
                CallTagDbHelper.STATUS_CONSULTING,
                CallTagDbHelper.STATUS_EXISTING,
                CallTagDbHelper.STATUS_VIP,
                CallTagDbHelper.STATUS_DORMANT
        };
        new AlertDialog.Builder(this)
                .setTitle("고객 상태")
                .setItems(labels, (dialog, which) -> {
                    db.updateCustomerProfile(customer.id, customer.displayName,
                            values[which], memoInput.getText().toString());
                    loadCustomer();
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void showScheduleDialog() {
        if (customer == null) return;
        String[] labels = {"내일", "3일 후", "다음 주"};
        long[] days = {1L, 3L, 7L};
        new AlertDialog.Builder(this)
                .setTitle("다음 연락")
                .setItems(labels, (dialog, which) -> {
                    db.insertFollowUpTask(customer.id, 0L, "CALL",
                            "다시 연락하기", dueAt(days[which]));
                    Toast.makeText(this, labels[which] + " 일정에 추가했습니다.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private long dueAt(long days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void dial() {
        if (customer == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.primaryPhone)));
        } catch (RuntimeException e) {
            Toast.makeText(this, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String statusLabel(String status) {
        if (CallTagDbHelper.STATUS_CONSULTING.equals(status)) return "상담 중";
        if (CallTagDbHelper.STATUS_EXISTING.equals(status)) return "기존";
        if (CallTagDbHelper.STATUS_VIP.equals(status)) return "VIP";
        if (CallTagDbHelper.STATUS_DORMANT.equals(status)) return "휴면";
        return "신규";
    }

    private int statusColor(String status) {
        if (CallTagDbHelper.STATUS_NEW.equals(status)) return R.color.primary;
        if (CallTagDbHelper.STATUS_EXISTING.equals(status)) return R.color.text_primary;
        return R.color.text_secondary;
    }

    private String resultLabel(String result) {
        if ("QUOTE".equals(result)) return "견적·자료 발송";
        if ("CALLBACK".equals(result)) return "다시 연락";
        if ("CONTRACT".equals(result)) return "계약·거래 완료";
        if ("HOLD".equals(result)) return "보류";
        if ("CLOSED".equals(result)) return "상담 종료";
        return "관심 있음";
    }

    private String typeLabel(String type) {
        if ("OUTGOING_CALL".equals(type)) return "발신";
        if ("MISSED_CALL".equals(type)) return "부재중";
        if ("REJECTED_CALL".equals(type)) return "거절";
        return "수신";
    }

    private String durationLabel(long seconds) {
        long minutes = seconds / 60L;
        long remain = seconds % 60L;
        return minutes > 0 ? minutes + "분 " + remain + "초" : remain + "초";
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
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
        if (db != null) db.close();
        super.onDestroy();
    }
}
