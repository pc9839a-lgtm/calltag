package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 고객 카드: 상태 변경/문자 보내기/연락처 저장/삭제를 카드에서 바로 실행한다. */
public final class CustomerListView extends LinearLayout {
    public CustomerListView(Context context) { super(context); }
    public CustomerListView(Context context, AttributeSet attrs) { super(context, attrs); }
    public CustomerListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        styleChild(child, params);
        super.addView(child, params);
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
        card.setMinimumHeight(dp(138));
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
        final String customerPhone = phone;
        final String customerName = customer.displayName;
        final String customerEmail = extractEmail(customer.memo);
        final boolean pagero = CustomerSourceResolver.isPagero(customer);
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
        if (pagero) {
            TextView source = CustomerSourceBadge.create(getContext(), "페이지로");
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
        removeDetailButton(actions);
        if (!hasMessageButton(actions)) {
            Button message = compactButton("문자 보내기", true);
            message.setTag("customer_message_button");
            message.setOnClickListener(v -> openCustomerMessage(customerPhone));
            actions.addView(message, new LinearLayout.LayoutParams(0, dp(42), 1f));
        }
        normalizeActionRow(actions);

        LinearLayout secondRow = new LinearLayout(getContext());
        secondRow.setOrientation(HORIZONTAL);
        Button saveContact = compactButton("연락처 저장", false);
        saveContact.setOnClickListener(v -> openContactInsert(customerName, customerPhone, customerEmail));
        secondRow.addView(saveContact, new LinearLayout.LayoutParams(0, dp(42), 1f));

        Button delete = compactButton("삭제", false);
        delete.setTextColor(getContext().getColor(R.color.danger));
        delete.setOnClickListener(v -> getContext().startActivity(
                new Intent(getContext(), CustomerDeleteActivity.class)
                        .putExtra(CustomerDeleteActivity.EXTRA_CUSTOMER_ID, customerId)));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        deleteParams.leftMargin = dp(7);
        secondRow.addView(delete, deleteParams);
        card.addView(secondRow, topMargin(7));
    }

    private void normalizeActionRow(LinearLayout actions) {
        for (int i = 0; i < actions.getChildCount(); i++) {
            View child = actions.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setTextSize(13f);
                button.setMinWidth(0);
            }
            if (child.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams item = (LinearLayout.LayoutParams) child.getLayoutParams();
                item.height = dp(42);
                item.leftMargin = i == 0 ? 0 : dp(7);
                child.setLayoutParams(item);
            }
        }
    }

    private void openContactInsert(String name, String phone, String email) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                    .putExtra(ContactsContract.Intents.Insert.NAME, name)
                    .putExtra(ContactsContract.Intents.Insert.PHONE, phone);
            if (!email.isEmpty()) intent.putExtra(ContactsContract.Intents.Insert.EMAIL, email);
            getContext().startActivity(intent);
        } catch (RuntimeException error) {
            Toast.makeText(getContext(), "연락처 저장 화면을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private Button compactButton(String label, boolean primary) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getContext().getColor(primary
                ? android.R.color.white : R.color.text_primary));
        button.setMinWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private void removeDetailButton(LinearLayout row) {
        for (int i = row.getChildCount() - 1; i >= 0; i--) {
            View child = row.getChildAt(i);
            if (child instanceof Button && "고객 상세".contentEquals(((Button) child).getText())) {
                row.removeViewAt(i);
            }
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

    private String extractEmail(String memo) {
        if (memo == null || memo.trim().isEmpty()) return "";
        for (String line : memo.split("\\r?\\n")) {
            String value = line.trim();
            if (value.startsWith("이메일:")) return value.substring(4).trim();
        }
        return "";
    }

    private boolean hasMessageButton(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            if ("customer_message_button".equals(row.getChildAt(i).getTag())) return true;
        }
        return false;
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
