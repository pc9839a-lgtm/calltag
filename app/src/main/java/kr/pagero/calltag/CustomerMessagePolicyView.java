package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 고객 상세의 문자 발송 제외 정책을 한 줄에서 관리한다. */
public final class CustomerMessagePolicyView extends LinearLayout {
    private Customer customer;
    private TextView summary;

    public CustomerMessagePolicyView(Context context) {
        super(context);
        init();
    }

    public CustomerMessagePolicyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomerMessagePolicyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(14), dp(10), dp(8), dp(10));
        setMinimumHeight(dp(58));
        setBackgroundResource(R.drawable.bg_clickable_row);
        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> showPolicyDialog());

        LinearLayout labels = new LinearLayout(getContext());
        labels.setOrientation(VERTICAL);
        TextView title = text("문자 발송 설정", 14f, true);
        labels.addView(title, matchWrap());
        summary = text("설정 확인 중", 12f, false);
        summary.setTextColor(getContext().getColor(R.color.text_secondary));
        LayoutParams summaryParams = matchWrap();
        summaryParams.topMargin = dp(3);
        labels.addView(summary, summaryParams);
        addView(labels, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 23f, false);
        arrow.setTextColor(getContext().getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        addView(arrow, new LayoutParams(dp(30), dp(42)));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        loadCustomer();
    }

    @Override
    protected void onVisibilityChanged(android.view.View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && summary != null) loadCustomer();
    }

    private void loadCustomer() {
        Activity activity = activity();
        if (activity == null) return;
        long customerId = activity.getIntent().getLongExtra(
                CustomerDetailActivity.EXTRA_CUSTOMER_ID, -1L);
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            customer = db.findCustomerById(customerId);
        } finally {
            db.close();
        }
        render();
    }

    private void render() {
        if (customer == null) {
            summary.setText("고객 정보를 불러올 수 없음");
            return;
        }
        MessageExclusionStore.Rule rule = MessageExclusionStore.find(
                getContext(), customer.primaryPhone);
        int flags = rule == null ? 0 : rule.flags;
        summary.setText(flags == 0 ? "모든 문자 허용" : MessageExclusionStore.summary(flags));
    }

    private void showPolicyDialog() {
        if (customer == null) return;
        String[] labels = {
                "자동문자 제외",
                "전체 문자 제외",
                "수신 자동문자 제외",
                "발신 자동문자 제외",
                "부재중 문자 제외",
                "후속문자 제외"
        };
        int[] bits = {
                MessageExclusionStore.FLAG_AUTO,
                MessageExclusionStore.FLAG_ALL,
                MessageExclusionStore.FLAG_INCOMING,
                MessageExclusionStore.FLAG_OUTGOING,
                MessageExclusionStore.FLAG_MISSED,
                MessageExclusionStore.FLAG_FOLLOW_UP
        };
        MessageExclusionStore.Rule existing = MessageExclusionStore.find(
                getContext(), customer.primaryPhone);
        int currentFlags = existing == null ? 0 : existing.flags;
        boolean[] checked = new boolean[bits.length];
        for (int i = 0; i < bits.length; i++) checked[i] = (currentFlags & bits[i]) != 0;

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("문자 발송 제외")
                .setMultiChoiceItems(labels, checked,
                        (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("취소", null)
                .setNeutralButton("모두 허용", null)
                .setPositiveButton("저장", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                MessageExclusionStore.remove(getContext(), customer.primaryPhone);
                Toast.makeText(getContext(), "모든 문자를 허용했습니다.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                render();
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int flags = 0;
                for (int i = 0; i < bits.length; i++) if (checked[i]) flags |= bits[i];
                MessageExclusionStore.save(getContext(), customer.id,
                        customer.displayName, customer.primaryPhone, flags);
                Toast.makeText(getContext(), "문자 발송 설정을 저장했습니다.",
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                render();
            });
        });
        dialog.show();
    }

    private Activity activity() {
        Context context = getContext();
        return context instanceof Activity ? (Activity) context : null;
    }

    private TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextColor(getContext().getColor(R.color.text_primary));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LayoutParams matchWrap() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
