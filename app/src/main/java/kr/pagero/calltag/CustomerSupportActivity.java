package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 앱 내 고객센터. 문의는 서버를 통해 운영 메일로 전달한다. */
public final class CustomerSupportActivity extends Activity {
    private static final String[] TYPES = {"일반문의", "결제", "오류", "기타"};

    private String selectedType = TYPES[0];
    private TextView typeButton;
    private EditText nameInput;
    private EditText contactInput;
    private EditText emailInput;
    private EditText messageInput;
    private TextView sendButton;
    private boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30f, true);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> {
            if (!working) finish();
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = text("고객센터", 21f, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        titleParams.leftMargin = dp(8);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        typeButton = formButton(selectedType);
        typeButton.setOnClickListener(v -> chooseType());
        root.addView(label("문의 유형"), topMargin(18));
        root.addView(typeButton, fixedTop(52, 7));

        nameInput = input("이름", false, 1);
        nameInput.setText(AuthSessionStore.name(this));
        root.addView(label("이름"), topMargin(18));
        root.addView(nameInput, fixedTop(52, 7));

        contactInput = input("연락 가능한 번호", false, 1);
        contactInput.setInputType(InputType.TYPE_CLASS_PHONE);
        contactInput.setText(AuthSessionStore.phone(this));
        root.addView(label("연락처"), topMargin(18));
        root.addView(contactInput, fixedTop(52, 7));

        emailInput = input("답변 받을 이메일", false, 1);
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(AuthSessionStore.email(this));
        root.addView(label("이메일"), topMargin(18));
        root.addView(emailInput, fixedTop(52, 7));

        messageInput = input("문의 내용을 입력해주세요.", true, 6);
        messageInput.setGravity(Gravity.TOP | Gravity.START);
        root.addView(label("문의 내용"), topMargin(18));
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(164));
        messageParams.topMargin = dp(7);
        root.addView(messageInput, messageParams);

        sendButton = text("문의 보내기", 15f, true);
        sendButton.setGravity(Gravity.CENTER);
        sendButton.setTextColor(getColor(android.R.color.white));
        sendButton.setBackgroundResource(R.drawable.bg_primary_button);
        sendButton.setClickable(true);
        sendButton.setFocusable(true);
        sendButton.setOnClickListener(v -> submit());
        root.addView(sendButton, fixedTop(54, 22));
        return scroll;
    }

    private void chooseType() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("문의 유형")
                .setItems(TYPES, (value, which) -> {
                    selectedType = TYPES[which];
                    typeButton.setText(selectedType + "    ›");
                })
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private void submit() {
        if (working) return;
        String session = AuthSessionStore.session(this);
        String name = value(nameInput);
        String contact = value(contactInput);
        String email = value(emailInput).toLowerCase();
        String message = value(messageInput);

        if (session.isEmpty()) {
            Toast.makeText(this, "로그인 정보를 다시 확인해주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        if (name.isEmpty()) {
            nameInput.requestFocus();
            Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.requestFocus();
            Toast.makeText(this, "답변 받을 이메일을 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.length() < 5) {
            messageInput.requestFocus();
            Toast.makeText(this, "문의 내용을 조금 더 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        setWorking(true);
        new Thread(() -> {
            try {
                SupportApiClient.send(
                        session,
                        selectedType,
                        name,
                        contact,
                        email,
                        message,
                        BuildConfig.VERSION_NAME);
                runOnUiThread(() -> {
                    setWorking(false);
                    Toast.makeText(this, "문의가 접수되었습니다.", Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setWorking(false);
                    Toast.makeText(this,
                            "문의를 보내지 못했습니다. 잠시 후 다시 시도해주세요.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "calltag-support-submit").start();
    }

    private void setWorking(boolean value) {
        working = value;
        sendButton.setEnabled(!value);
        sendButton.setAlpha(value ? 0.55f : 1f);
        sendButton.setText(value ? "전송 중…" : "문의 보내기");
    }

    private EditText input(String hint, boolean multiline, int lines) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setTextSize(15f);
        view.setTextColor(getColor(R.color.text_primary));
        view.setHintTextColor(getColor(R.color.text_muted));
        view.setBackgroundResource(R.drawable.bg_input);
        view.setPadding(dp(14), multiline ? dp(13) : 0, dp(14), multiline ? dp(13) : 0);
        view.setSingleLine(!multiline);
        if (multiline) {
            view.setMinLines(lines);
            view.setMaxLines(lines + 2);
            view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        return view;
    }

    private TextView formButton(String value) {
        TextView view = text(value + "    ›", 15f, true);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(14), 0, dp(14), 0);
        view.setBackgroundResource(R.drawable.bg_input);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 13f, true);
        view.setTextColor(getColor(R.color.text_secondary));
        return view;
    }

    private String value(EditText view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    private TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.text_primary));
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

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
