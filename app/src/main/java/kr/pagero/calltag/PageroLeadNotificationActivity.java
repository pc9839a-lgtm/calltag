package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 알림에서 방금 반영된 고객으로 바로 이동한다. */
public final class PageroLeadNotificationActivity extends Activity {
    public static final String EXTRA_CUSTOMER_IDS = "pagero_customer_ids";

    private CallTagDbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new CallTagDbHelper(this);

        long[] ids = PageroLeadNotificationManager.sanitizeCustomerIds(
                getIntent().getLongArrayExtra(EXTRA_CUSTOMER_IDS));
        List<Customer> customers = loadCustomers(ids);
        if (customers.isEmpty()) {
            openCustomerHome();
            return;
        }
        if (customers.size() == 1) {
            openSingleCustomer(customers.get(0).id);
            return;
        }
        renderCustomerList(customers);
    }

    private List<Customer> loadCustomers(long[] ids) {
        List<Customer> customers = new ArrayList<>();
        for (long id : ids) {
            Customer customer = db.findCustomerById(id);
            if (customer != null) customers.add(customer);
        }
        return customers;
    }

    private void openSingleCustomer(long customerId) {
        Intent home = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Intent detail = new Intent(this, CustomerDetailActivity.class)
                .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customerId);
        startActivities(new Intent[]{home, detail});
        finish();
    }

    private void openCustomerHome() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void renderCustomerList(List<Customer> customers) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(getColor(R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = new Button(this);
        back.setText("닫기");
        back.setAllCaps(false);
        back.setTextSize(14f);
        back.setBackgroundResource(R.drawable.bg_secondary_button);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(74), dp(44)));

        TextView title = new TextView(this);
        title.setText("방금 접수된 문의");
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(21f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(8);
        titleParams.rightMargin = dp(82);
        header.addView(title, titleParams);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView summary = new TextView(this);
        summary.setText(customers.size() + "명의 고객 문의가 콜태그에 반영되었습니다.");
        summary.setTextColor(getColor(R.color.text_secondary));
        summary.setTextSize(15f);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(14);
        root.addView(summary, summaryParams);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(14), 0, dp(16));
        scroll.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        for (Customer customer : customers) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            card.setBackgroundResource(R.drawable.bg_clickable_card);
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> openSingleCustomer(customer.id));

            TextView name = new TextView(this);
            name.setText(customer.displayName);
            name.setTextColor(getColor(R.color.text_primary));
            name.setTextSize(17f);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(name);

            TextView phone = new TextView(this);
            phone.setText(customer.primaryPhone);
            phone.setTextColor(getColor(R.color.text_secondary));
            phone.setTextSize(14f);
            LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            phoneParams.topMargin = dp(7);
            card.addView(phone, phoneParams);

            String memoText = customer.memo == null ? "" : customer.memo.trim();
            if (!memoText.isEmpty()) {
                TextView memo = new TextView(this);
                memo.setText(memoText.length() > 140 ? memoText.substring(0, 140) + "…" : memoText);
                memo.setTextColor(getColor(R.color.text_muted));
                memo.setTextSize(13f);
                memo.setMaxLines(3);
                LinearLayout.LayoutParams memoParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                memoParams.topMargin = dp(8);
                card.addView(memo, memoParams);
            }

            TextView action = new TextView(this);
            action.setText("고객 상세 보기  ›");
            action.setTextColor(getColor(R.color.primary));
            action.setTextSize(14f);
            action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            action.setGravity(Gravity.END);
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            actionParams.topMargin = dp(12);
            card.addView(action, actionParams);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(10);
            list.addView(card, cardParams);
        }

        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
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
