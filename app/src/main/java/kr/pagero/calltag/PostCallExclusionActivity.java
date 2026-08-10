package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class PostCallExclusionActivity extends Activity {
    private EditText phoneInput;
    private LinearLayout list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        String phone = getIntent() == null ? "" : safe(getIntent().getStringExtra("phone"));
        if (!phone.isEmpty()) {
            phoneInput.setText(phone);
            phoneInput.setSelection(phoneInput.length());
        }
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
        TextView title = title("통화 후 팝업 제외", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        TextView help = body("이 목록에 있는 번호는 통화가 끝나도 콜태그 팝업을 띄우지 않습니다. 수신 고객정보 표시와 문자 기능은 그대로 유지됩니다.");
        help.setBackgroundResource(R.drawable.bg_card);
        help.setPadding(dp(18), dp(15), dp(18), dp(15));
        root.addView(help, topMargin(18));

        LinearLayout addCard = new LinearLayout(this);
        addCard.setOrientation(LinearLayout.VERTICAL);
        addCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        addCard.setBackgroundResource(R.drawable.bg_card);

        TextView addTitle = title("번호 바로 추가", 16f);
        addCard.addView(addTitle, matchWrap());

        phoneInput = new EditText(this);
        phoneInput.setSingleLine(true);
        phoneInput.setHint("전화번호 입력");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setTextColor(getColor(R.color.text_primary));
        phoneInput.setHintTextColor(getColor(R.color.text_muted));
        phoneInput.setTextSize(15f);
        phoneInput.setBackgroundResource(R.drawable.bg_input);
        phoneInput.setPadding(dp(14), 0, dp(14), 0);
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        phoneParams.topMargin = dp(12);
        addCard.addView(phoneInput, phoneParams);

        Button add = button("팝업 제외에 추가", true);
        add.setOnClickListener(v -> addPhone());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        addParams.topMargin = dp(10);
        addCard.addView(add, addParams);
        root.addView(addCard, topMargin(16));

        TextView listTitle = title("제외 목록", 16f);
        root.addView(listTitle, topMargin(22));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, topMargin(10));

        empty = body("등록된 제외 번호가 없습니다.");
        empty.setGravity(Gravity.CENTER);
        empty.setBackgroundResource(R.drawable.bg_card);
        empty.setPadding(dp(18), dp(28), dp(18), dp(28));
        root.addView(empty, topMargin(10));
        return scroll;
    }

    private void addPhone() {
        String rawPhone = phoneInput.getText().toString().trim();
        String normalized = PhoneNumberNormalizer.normalize(rawPhone);
        if (normalized.length() < 8) {
            Toast.makeText(this, "전화번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName = "";
        CallTagDbHelper db = new CallTagDbHelper(this);
        try {
            Customer customer = db.findByPhone(rawPhone);
            if (customer != null) displayName = safe(customer.displayName).trim();
        } finally {
            db.close();
        }

        try {
            PostCallExclusionStore.add(this, displayName, rawPhone);
            phoneInput.setText("");
            Toast.makeText(this, "이 번호는 이제 통화 후 팝업이 뜨지 않습니다.", Toast.LENGTH_SHORT).show();
            render();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void render() {
        if (list == null || empty == null) return;
        list.removeAllViews();
        List<PostCallExclusionStore.Entry> entries = PostCallExclusionStore.list(this);
        empty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);

        for (PostCallExclusionStore.Entry entry : entries) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(15), dp(14), dp(15));
            card.setBackgroundResource(R.drawable.bg_card);

            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout textBox = new LinearLayout(this);
            textBox.setOrientation(LinearLayout.VERTICAL);
            String name = entry.displayName == null || entry.displayName.trim().isEmpty()
                    ? "등록 번호" : entry.displayName.trim();
            textBox.addView(title(name, 16f), matchWrap());
            textBox.addView(body(entry.phone), topMargin(4));
            row.addView(textBox, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button remove = button("해제", false);
            remove.setTextColor(getColor(R.color.danger));
            remove.setOnClickListener(v -> confirmRemove(entry));
            row.addView(remove, new LinearLayout.LayoutParams(dp(78), dp(42)));
            card.addView(row, matchWrap());

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(9);
            list.addView(card, params);
        }
    }

    private void confirmRemove(PostCallExclusionStore.Entry entry) {
        new AlertDialog.Builder(this)
                .setTitle("팝업 제외 해제")
                .setMessage(entry.phone + " 번호를 통화 후 팝업 제외 목록에서 해제합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("해제", (dialog, which) -> {
                    PostCallExclusionStore.remove(this, entry.phone);
                    Toast.makeText(this, "팝업 제외를 해제했습니다.", Toast.LENGTH_SHORT).show();
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
        text.setIncludeFontPadding(false);
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
