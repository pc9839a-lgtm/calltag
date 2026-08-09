package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class MessageExclusionActivity extends Activity {
    private LinearLayout list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(44));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("문자 발송 제외", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        TextView help = body("자동문자 제외와 전체 문자 제외는 서로 다릅니다. 예약·후속문자도 실제 발송 직전에 다시 검사합니다.");
        help.setBackgroundResource(R.drawable.bg_card);
        help.setPadding(dp(18), dp(15), dp(18), dp(15));
        root.addView(help, topMargin(18));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, topMargin(16));

        empty = body("문자 발송 제외 고객이 없습니다.");
        empty.setGravity(Gravity.CENTER);
        empty.setBackgroundResource(R.drawable.bg_card);
        empty.setPadding(dp(18), dp(28), dp(18), dp(28));
        root.addView(empty, topMargin(12));
        return scroll;
    }

    private void render() {
        list.removeAllViews();
        List<MessageExclusionStore.Rule> rules = MessageExclusionStore.list(this);
        empty.setVisibility(rules.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);

        for (MessageExclusionStore.Rule rule : rules) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            card.setBackgroundResource(R.drawable.bg_card);

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            String name = rule.displayName == null || rule.displayName.trim().isEmpty()
                    ? "이름 없는 고객" : rule.displayName.trim();
            TextView nameView = title(name, 16f);
            header.addView(nameView, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            Button remove = button("해제", false);
            remove.setTextColor(getColor(R.color.danger));
            remove.setOnClickListener(v -> confirmRemove(rule));
            header.addView(remove, new LinearLayout.LayoutParams(dp(82), dp(42)));
            card.addView(header, matchWrap());

            card.addView(body(rule.phone), topMargin(6));
            TextView policy = body(MessageExclusionStore.summary(rule.flags));
            policy.setTextColor(getColor(R.color.primary));
            policy.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(policy, topMargin(8));

            if (rule.customerId > 0L) {
                card.setOnClickListener(v -> startActivity(
                        new Intent(this, CustomerDetailActivity.class)
                                .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, rule.customerId)));
            }
            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(10);
            list.addView(card, params);
        }
    }

    private void confirmRemove(MessageExclusionStore.Rule rule) {
        new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("문자 발송 제외 해제")
                .setMessage((rule.displayName == null || rule.displayName.trim().isEmpty()
                        ? rule.phone : rule.displayName) + " 고객의 문자 제외 설정을 해제합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("해제", (dialog, which) -> {
                    MessageExclusionStore.remove(this, rule.phone);
                    Toast.makeText(this, "제외 설정을 해제했습니다.", Toast.LENGTH_SHORT).show();
                    render();
                })
                .show();
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(14f);
        text.setLineSpacing(dp(2), 1f);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
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
}
