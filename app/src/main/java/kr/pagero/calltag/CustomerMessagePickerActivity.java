package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/** 고객을 먼저 선택해야 문자 작성으로 넘어가는 화면. */
public final class CustomerMessagePickerActivity extends Activity {
    private CallTagDbHelper db;
    private EditText search;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new CallTagDbHelper(this);
        setContentView(buildContent());
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (list != null) render();
    }

    private View buildContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(getColor(R.color.background));
        page.setPadding(dp(20), dp(16), dp(20), dp(28));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("문자 보낼 고객", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        page.addView(header, matchWrap());

        search = new EditText(this);
        search.setHint("고객명·전화번호 검색");
        search.setSingleLine(true);
        search.setTextSize(15f);
        search.setTextColor(getColor(R.color.text_primary));
        search.setHintTextColor(getColor(R.color.text_muted));
        search.setBackgroundResource(R.drawable.bg_input);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        searchParams.topMargin = dp(14);
        page.addView(search, searchParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(12), 0, dp(40));
        scroll.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void render() {
        list.removeAllViews();
        String raw = search == null ? "" : search.getText().toString().trim();
        String nameQuery = raw.toLowerCase(Locale.KOREA);
        String phoneQuery = PhoneNumberNormalizer.normalize(raw);
        int shown = 0;
        for (Customer customer : db.listCustomers(null)) {
            boolean match = raw.isEmpty()
                    || customer.displayName.toLowerCase(Locale.KOREA).contains(nameQuery)
                    || (!phoneQuery.isEmpty()
                    && PhoneNumberNormalizer.normalize(customer.primaryPhone).contains(phoneQuery));
            if (!match) continue;
            list.addView(customerCard(customer), topMargin(8));
            shown++;
        }
        if (shown == 0) {
            TextView empty = body(raw.isEmpty()
                    ? "등록된 고객이 없습니다. 먼저 고객을 추가해주세요."
                    : "검색 결과가 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(34), dp(18), dp(34));
            empty.setBackgroundResource(R.drawable.bg_card);
            list.addView(empty, topMargin(8));
        }
    }

    private View customerCard(Customer customer) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openCompose(customer));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title(customer.displayName, 16f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView source = CustomerSourceBadge.create(
                this, CustomerSourceResolver.label(this, customer));
        top.addView(source, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(28)));
        card.addView(top, matchWrap());

        card.addView(body(customer.primaryPhone), topMargin(7));
        TextView action = title("문자 작성  ›", 14f);
        action.setTextColor(getColor(R.color.primary));
        action.setGravity(Gravity.END);
        card.addView(action, topMargin(10));
        return card;
    }

    private void openCompose(Customer customer) {
        startActivity(new Intent(this, ManualMessageActivity.class)
                .putExtra(ManualMessageActivity.EXTRA_PHONE, customer.primaryPhone)
                .putExtra(ManualMessageActivity.EXTRA_CUSTOMER_ID, customer.id));
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        button.setMinWidth(0);
        return button;
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(13f);
        text.setIncludeFontPadding(false);
        return text;
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
