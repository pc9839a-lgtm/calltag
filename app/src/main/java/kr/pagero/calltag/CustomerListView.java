package kr.pagero.calltag;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 고객 카드에 유입 출처 배지와 외부 문의 전용 필터를 함께 제공한다. */
public final class CustomerListView extends LinearLayout {
    private static final String TAG_FILTER_ROW = "customer_source_filter_row";
    private static final String TAG_FILTER_EMPTY = "customer_source_filter_empty";
    private static final String TAG_EXTERNAL_CARD = "customer_external_card";
    private static final String TAG_LOCAL_CARD = "customer_local_card";

    private boolean externalOnly;
    private Button allSourceButton;
    private Button externalSourceButton;
    private TextView externalEmpty;

    public CustomerListView(Context context) { super(context); }
    public CustomerListView(Context context, AttributeSet attrs) { super(context, attrs); }
    public CustomerListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        allSourceButton = null;
        externalSourceButton = null;
        externalEmpty = null;
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        ensureSourceFilter();
        styleChild(child, params);
        super.addView(child, params);
        applySourceFilter();
    }

    private void ensureSourceFilter() {
        if (allSourceButton != null && allSourceButton.getParent() == this) return;

        LinearLayout row = new LinearLayout(getContext());
        row.setTag(TAG_FILTER_ROW);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(8));

        TextView label = new TextView(getContext());
        label.setText("유입");
        label.setTextSize(12f);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextColor(getContext().getColor(R.color.text_secondary));
        row.addView(label, new LinearLayout.LayoutParams(0, dp(36), 1f));

        allSourceButton = filterButton("전체");
        allSourceButton.setOnClickListener(v -> setExternalOnly(false));
        row.addView(allSourceButton, new LinearLayout.LayoutParams(dp(72), dp(36)));

        externalSourceButton = filterButton("외부 문의");
        externalSourceButton.setOnClickListener(v -> setExternalOnly(true));
        LinearLayout.LayoutParams externalParams = new LinearLayout.LayoutParams(dp(92), dp(36));
        externalParams.leftMargin = dp(6);
        row.addView(externalSourceButton, externalParams);

        super.addView(row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        externalEmpty = new TextView(getContext());
        externalEmpty.setTag(TAG_FILTER_EMPTY);
        externalEmpty.setText("외부 문의로 들어온 고객이 없습니다.");
        externalEmpty.setTextSize(13f);
        externalEmpty.setTextColor(getContext().getColor(R.color.text_secondary));
        externalEmpty.setGravity(Gravity.CENTER);
        externalEmpty.setPadding(dp(14), dp(20), dp(14), dp(20));
        externalEmpty.setBackgroundResource(R.drawable.bg_card);
        externalEmpty.setVisibility(GONE);
        LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        emptyParams.bottomMargin = dp(8);
        super.addView(externalEmpty, emptyParams);
        styleSourceButtons();
    }

    private Button filterButton(String label) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(12f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private void setExternalOnly(boolean enabled) {
        if (externalOnly == enabled) return;
        externalOnly = enabled;
        styleSourceButtons();
        applySourceFilter();
    }

    private void styleSourceButtons() {
        if (allSourceButton == null || externalSourceButton == null) return;
        styleFilterButton(allSourceButton, !externalOnly);
        styleFilterButton(externalSourceButton, externalOnly);
    }

    private void styleFilterButton(Button button, boolean selected) {
        button.setBackgroundResource(selected
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        button.setTextColor(getContext().getColor(selected
                ? android.R.color.white : R.color.text_primary));
    }

    private void applySourceFilter() {
        if (externalEmpty == null) return;
        int externalCards = 0;
        boolean hasParentEmptyState = false;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Object tag = child.getTag();
            if (TAG_EXTERNAL_CARD.equals(tag)) {
                externalCards++;
                child.setVisibility(VISIBLE);
            } else if (TAG_LOCAL_CARD.equals(tag)) {
                child.setVisibility(externalOnly ? GONE : VISIBLE);
            } else if (child instanceof TextView && child != externalEmpty) {
                hasParentEmptyState = true;
            }
        }
        externalEmpty.setVisibility(externalOnly && externalCards == 0 && !hasParentEmptyState
                ? VISIBLE : GONE);
    }

    private void styleChild(View child, ViewGroup.LayoutParams params) {
        if (child instanceof TextView) {
            TextView empty = (TextView) child;
            empty.setMinHeight(dp(72));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(14), dp(12), dp(14), dp(12));
            return;
        }
        if (!(child instanceof LinearLayout)) return;
        LinearLayout card = (LinearLayout) child;
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackgroundResource(R.drawable.bg_card);
        card.setMinimumHeight(dp(128));
        decorateCustomerCard(card);
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).bottomMargin = dp(8);
        }
    }

    private void decorateCustomerCard(LinearLayout card) {
        String phone = "";
        TextView phoneView = null;
        TextView recentView = null;
        LinearLayout actions = null;
        LinearLayout header = null;

        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (i == 0 && child instanceof LinearLayout) header = (LinearLayout) child;
            if (child instanceof TextView) {
                TextView text = (TextView) child;
                String candidate = String.valueOf(text.getText()).trim();
                if (PhoneNumberNormalizer.normalize(candidate).length() >= 8) {
                    phone = candidate;
                    phoneView = text;
                } else if (candidate.startsWith("최근 연락")) {
                    recentView = text;
                }
            }
            if (child instanceof LinearLayout
                    && ((LinearLayout) child).getOrientation() == LinearLayout.HORIZONTAL) {
                LinearLayout row = (LinearLayout) child;
                if (row.getChildCount() >= 2 && row.getChildAt(0) instanceof Button) actions = row;
            }
        }
        if (phone.isEmpty() || phoneView == null) return;

        Customer customer;
        String memo = "";
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            customer = db.findByPhone(phone);
            if (customer != null) memo = CustomerInsightResolver.latestMemo(db, customer);
        } finally {
            db.close();
        }
        if (customer == null) return;

        final long customerId = customer.id;
        final String customerName = customer.displayName;
        final String customerPhone = phone;
        final boolean pagero = CustomerSourceResolver.isPagero(customer);
        final String sourceLabel = CustomerSourceResolver.label(getContext(), customer);
        final boolean external = CustomerSourceResolver.isExternal(getContext(), customer);
        card.setTag(external ? TAG_EXTERNAL_CARD : TAG_LOCAL_CARD);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openCustomer(customerId));

        removeOldSourceBadges(header);
        if (phoneView.getParent() == card) card.removeView(phoneView);
        if (recentView != null && recentView.getParent() == card) card.removeView(recentView);

        LinearLayout contactRow = new LinearLayout(getContext());
        contactRow.setOrientation(HORIZONTAL);
        contactRow.setGravity(Gravity.CENTER_VERTICAL);
        phoneView.setSingleLine(true);
        phoneView.setEllipsize(TextUtils.TruncateAt.END);
        phoneView.setTextSize(14f);
        contactRow.addView(phoneView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        if (!sourceLabel.isEmpty()) {
            TextView source = CustomerSourceBadge.create(getContext(), sourceLabel);
            LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, dp(26));
            sourceParams.leftMargin = dp(8);
            contactRow.addView(source, sourceParams);
        }
        card.addView(contactRow, Math.min(1, card.getChildCount()), topMargin(8));

        TextView memoView = new TextView(getContext());
        String compactMemo = compact(memo);
        memoView.setText(compactMemo.isEmpty() ? "메모 없음" : compactMemo);
        memoView.setTextColor(getContext().getColor(compactMemo.isEmpty()
                ? R.color.text_muted : R.color.text_secondary));
        memoView.setTextSize(13f);
        memoView.setSingleLine(true);
        memoView.setEllipsize(TextUtils.TruncateAt.END);
        memoView.setIncludeFontPadding(false);
        card.addView(memoView, Math.min(2, card.getChildCount()), topMargin(6));

        int contentIndex = 3;
        if (pagero) {
            PageroLeadSmsStatusResolver.State sms = PageroLeadSmsStatusResolver.latest(
                    getContext(), customerId);
            if (sms != null) {
                TextView state = new TextView(getContext());
                state.setText(sms.label + (sms.reason.isEmpty() ? "" : " · " + compact(sms.reason)));
                state.setTextSize(12f);
                state.setSingleLine(true);
                state.setEllipsize(TextUtils.TruncateAt.END);
                state.setIncludeFontPadding(false);
                state.setTextColor(getContext().getColor(
                        PageroLeadReceiptStore.SMS_FAILED.equals(sms.code)
                                ? R.color.danger : R.color.text_secondary));
                card.addView(state, Math.min(contentIndex++, card.getChildCount()), topMargin(5));
            }
        }

        if (recentView != null) {
            recentView.setSingleLine(true);
            recentView.setEllipsize(TextUtils.TruncateAt.END);
            recentView.setTextSize(12f);
            card.addView(recentView, Math.min(contentIndex, card.getChildCount()), topMargin(5));
        }

        if (actions == null) return;
        Button status = findActionButton(actions, "상태 변경");
        if (status == null && actions.getChildCount() > 0 && actions.getChildAt(0) instanceof Button) {
            status = (Button) actions.getChildAt(0);
        }
        if (status == null) return;

        actions.removeAllViews();
        actions.setGravity(Gravity.CENTER_VERTICAL);

        configureTextButton(status, "상태 변경", false);
        actions.addView(status, weightedButtonParams(0));

        Button message = textButton("문자 보내기", true);
        message.setOnClickListener(v -> openCustomerMessage(customerPhone));
        actions.addView(message, weightedButtonParams(7));

        ImageButton delete = deleteIconButton("고객 삭제");
        delete.setOnClickListener(v -> confirmDelete(card, customerId, customerName));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        deleteParams.leftMargin = dp(7);
        actions.addView(delete, deleteParams);

        ViewGroup.LayoutParams raw = actions.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) raw;
            lp.height = dp(44);
            lp.topMargin = dp(9);
            actions.setLayoutParams(lp);
        }
    }

    private Button findActionButton(LinearLayout row, String text) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child instanceof Button && text.contentEquals(((Button) child).getText())) {
                return (Button) child;
            }
        }
        return null;
    }

    private Button textButton(String label, boolean primary) {
        Button button = new Button(getContext());
        configureTextButton(button, label, primary);
        return button;
    }

    private void configureTextButton(Button button, String label, boolean primary) {
        button.setText(label);
        button.setContentDescription(label);
        button.setAllCaps(false);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getContext().getColor(primary
                ? android.R.color.white : R.color.text_primary));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(7), 0, dp(7), 0);
        button.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
    }

    private LinearLayout.LayoutParams weightedButtonParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
        params.leftMargin = dp(leftMargin);
        return params;
    }

    private ImageButton deleteIconButton(String description) {
        ImageButton button = new ImageButton(getContext());
        button.setContentDescription(description);
        button.setBackgroundResource(R.drawable.bg_secondary_button);
        button.setImageResource(R.drawable.ic_customer_delete);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        return button;
    }

    private void confirmDelete(LinearLayout card, long customerId, String customerName) {
        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.Theme_CallTag_Dialog)
                .setTitle("고객 삭제")
                .setMessage(customerName + " 고객과 상담·할 일을 삭제합니다.\n문자 발송 이력은 유지됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (d, which) -> deleteCustomer(card, customerId))
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.applyDanger(dialog));
        dialog.show();
    }

    private void deleteCustomer(LinearLayout card, long customerId, String customerName) {
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        int removed;
        try {
            removed = db.getWritableDatabase().delete(
                    "customers", "id=?", new String[]{String.valueOf(customerId)});
        } catch (RuntimeException error) {
            Toast.makeText(getContext(), "고객을 삭제하지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        } finally {
            db.close();
        }
        if (removed > 0) {
            if (card.getParent() == this) removeView(card);
            applySourceFilter();
            Toast.makeText(getContext(), "고객을 삭제했습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "이미 삭제된 고객입니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeOldSourceBadges(LinearLayout row) {
        if (row == null) return;
        for (int i = row.getChildCount() - 1; i >= 0; i--) {
            if ("customer_source_badge".equals(row.getChildAt(i).getTag())) row.removeViewAt(i);
        }
    }

    private String compact(String value) {
        if (value == null) return "";
        String safe = value.trim().replaceAll("\\s+", " ");
        return safe.length() <= 52 ? safe : safe.substring(0, 49) + "…";
    }

    private void openCustomer(long customerId) {
        getContext().startActivity(new Intent(getContext(), CustomerDetailActivity.class)
                .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customerId));
    }

    private void openCustomerMessage(String phone) {
        if (!FeatureAccessGate.require(getContext(), FeatureAccessGate.MESSAGE)) return;
        long customerId = 0L;
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            Customer customer = db.findByPhone(phone);
            if (customer != null) customerId = customer.id;
        } finally {
            db.close();
        }
        getContext().startActivity(new Intent(getContext(), ManualMessageActivity.class)
                .putExtra(ManualMessageActivity.EXTRA_PHONE, phone)
                .putExtra(ManualMessageActivity.EXTRA_CUSTOMER_ID, customerId));
    }

    private LayoutParams topMargin(int value) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(value);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
