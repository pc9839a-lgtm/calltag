package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 고객 상세에서 문자 작성과 문자 제외 정책을 관리하는 독립 카드. */
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
        setOrientation(VERTICAL);
        setPadding(dp(18), dp(16), dp(18), dp(16));
        setBackgroundResource(R.drawable.bg_card);

        TextView title = text("문자", 16f, true);
        addView(title, matchWrap());

        summary = text("문자 발송 제외 설정을 확인하는 중입니다.", 13f, false);
        summary.setTextColor(getContext().getColor(R.color.text_secondary));
        LayoutParams summaryParams = matchWrap();
        summaryParams.topMargin = dp(6);
        addView(summary, summaryParams);

        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(HORIZONTAL);
        Button send = button("문자 보내기", true);
        send.setOnClickListener(v -> openMessage());
        actions.addView(send, new LayoutParams(0, dp(48), 1f));

        Button policy = button("발송 제외 설정", false);
        policy.setOnClickListener(v -> showPolicyDialog());
        LayoutParams policyParams = new LayoutParams(0, dp(48), 1f);
        policyParams.leftMargin = dp(8);
        actions.addView(policy, policyParams);

        LayoutParams actionsParams = matchWrap();
        actionsParams.topMargin = dp(12);
        addView(actions, actionsParams);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        loadCustomer();
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
            summary.setText("고객 정보를 불러올 수 없습니다.");
            return;
        }
        MessageExclusionStore.Rule rule = MessageExclusionStore.find(
                getContext(), customer.primaryPhone);
        int flags = rule == null ? 0 : rule.flags;
        if (flags == 0) {
            summary.setText("현재 모든 문자 유형을 발송할 수 있습니다.");
        } else {
            summary.setText(MessageExclusionStore.summary(flags)
                    + " · 실제 발송 직전에 다시 검사합니다.");
        }
    }

    private void openMessage() {
        if (customer == null) return;
        getContext().startActivity(new Intent(getContext(), ManualMessageActivity.class)
                .putExtra(ManualMessageActivity.EXTRA_CUSTOMER_ID, customer.id)
                .putExtra(ManualMessageActivity.EXTRA_PHONE, customer.primaryPhone)
                .putExtra(ManualMessageActivity.EXTRA_USE_TEMPLATE, true));
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
                .setMessage("자동문자 제외는 자동·후속·단체문자를 막고 고객 상세의 수동 발송은 허용합니다. 전체 문자 제외는 수동 발송도 차단합니다.")
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("취소", null)
                .setNeutralButton("제외 해제", null)
                .setPositiveButton("저장", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                MessageExclusionStore.remove(getContext(), customer.primaryPhone);
                Toast.makeText(getContext(), "문자 발송 제외를 해제했습니다.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                render();
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int flags = 0;
                for (int i = 0; i < bits.length; i++) {
                    if (checked[i]) flags |= bits[i];
                }
                MessageExclusionStore.save(getContext(), customer.id,
                        customer.displayName, customer.primaryPhone, flags);
                Toast.makeText(getContext(), flags == 0
                        ? "문자 발송 제외를 해제했습니다."
                        : "문자 발송 제외 설정을 저장했습니다.", Toast.LENGTH_SHORT).show();
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
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLineSpacing(dp(2), 1f);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getContext().getColor(R.color.text_primary));
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LayoutParams matchWrap() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
