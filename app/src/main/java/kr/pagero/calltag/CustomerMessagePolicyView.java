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

/** 고객별 문자 발송 정책을 허용/비허용 두 상태로만 관리한다. */
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
        setMinimumHeight(dp(54));
        setBackgroundResource(R.drawable.bg_clickable_row);
        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> showPolicyDialog());

        TextView title = text("문자 발송", 14f, true);
        addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        summary = text("확인 중", 13f, true);
        summary.setTextColor(getContext().getColor(R.color.primary));
        addView(summary, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        TextView arrow = text("›", 22f, false);
        arrow.setTextColor(getContext().getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        addView(arrow, new LayoutParams(dp(28), dp(40)));
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
            summary.setText("확인 불가");
            summary.setTextColor(getContext().getColor(R.color.text_muted));
            return;
        }
        MessageExclusionStore.Rule rule = MessageExclusionStore.find(
                getContext(), customer.primaryPhone);
        boolean blocked = rule != null && rule.flags != 0;
        summary.setText(blocked ? "비허용" : "허용");
        summary.setTextColor(getContext().getColor(blocked
                ? R.color.danger : R.color.primary));
    }

    private void showPolicyDialog() {
        if (customer == null) return;
        MessageExclusionStore.Rule rule = MessageExclusionStore.find(
                getContext(), customer.primaryPhone);
        int checked = rule != null && rule.flags != 0 ? 1 : 0;
        String[] items = {"허용", "비허용"};
        new AlertDialog.Builder(getContext(), R.style.Theme_CallTag_Dialog)
                .setTitle("문자 발송")
                .setSingleChoiceItems(items, checked, null)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> {
                    AlertDialog alert = (AlertDialog) dialog;
                    int selected = alert.getListView().getCheckedItemPosition();
                    if (selected == 1) {
                        MessageExclusionStore.save(getContext(), customer.id,
                                customer.displayName, customer.primaryPhone,
                                MessageExclusionStore.FLAG_ALL);
                        Toast.makeText(getContext(), "이 고객에게 문자를 보내지 않습니다.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        MessageExclusionStore.remove(getContext(), customer.primaryPhone);
                        Toast.makeText(getContext(), "이 고객에게 문자를 보낼 수 있습니다.",
                                Toast.LENGTH_SHORT).show();
                    }
                    render();
                })
                .show();
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
