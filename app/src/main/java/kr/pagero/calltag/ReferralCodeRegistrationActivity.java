package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Locale;

/** 다른 사람의 추천인 코드를 등록하는 전용 화면. */
public final class ReferralCodeRegistrationActivity extends Activity {
    private EditText codeInput;
    private TextView state;
    private Button apply;
    private boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        String pending = PendingReferralStore.peek(this);
        if (!pending.isEmpty()) codeInput.setText(pending);
        render();
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = title("‹", 30f);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        header.addView(title("추천인 코드 등록", 22f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header, matchWrap());

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundResource(R.drawable.bg_card);
        card.addView(title("추천인 코드가 있나요?", 18f), matchWrap());
        card.addView(body("가입 후 첫 유료 결제 전에 한 번만 등록할 수 있으며, 등록하면 무료 이용기간이 5일 늘어납니다."), top(8));

        state = body("");
        state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(state, top(14));

        codeInput = new EditText(this);
        codeInput.setSingleLine(true);
        codeInput.setHint("추천인 코드 입력");
        codeInput.setAllCaps(true);
        codeInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        codeInput.setTextColor(getColor(R.color.text_primary));
        codeInput.setHintTextColor(getColor(R.color.text_muted));
        codeInput.setTextSize(16f);
        codeInput.setBackgroundResource(R.drawable.bg_input);
        codeInput.setPadding(dp(14), 0, dp(14), 0);
        card.addView(codeInput, fixedTop(52, 12));

        apply = button("추천 혜택 받기", true);
        apply.setOnClickListener(v -> applyCode());
        card.addView(apply, fixedTop(52, 10));
        root.addView(card, top(18));
        return scroll;
    }

    private void applyCode() {
        if (working) return;
        String value = codeInput.getText().toString().trim().toUpperCase(Locale.KOREA);
        if (!value.matches("[A-Z0-9]{4,20}")) {
            Toast.makeText(this, "추천인 코드를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        ReferralStateStore.Snapshot cached = ReferralStateStore.snapshot(this);
        if (!cached.code.isEmpty() && cached.code.equalsIgnoreCase(value)) {
            Toast.makeText(this, "본인 추천인 코드는 등록할 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard();
        setWorking(true);
        new Thread(() -> {
            try {
                JSONObject response = AuthApiClient.applyReferral(session, value);
                ReferralStateStore.saveMe(this, response);
                PendingReferralStore.clear(this);
                try {
                    FeatureEntitlementStore.saveServerEntitlement(
                            this, AuthApiClient.billingEntitlements(session));
                } catch (Exception ignored) {}
                runOnUiThread(() -> {
                    setWorking(false);
                    render();
                    new AlertDialog.Builder(this)
                            .setTitle("추천 혜택 적용 완료")
                            .setMessage("무료 이용기간이 5일 늘어났습니다.")
                            .setPositiveButton("확인", null)
                            .show();
                });
            } catch (AuthApiClient.ApiException error) {
                runOnUiThread(() -> {
                    setWorking(false);
                    showError(error.code);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setWorking(false);
                    Toast.makeText(this, "추천인 코드를 등록하지 못했습니다.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "calltag-referral-code-register").start();
    }

    private void render() {
        if (state == null) return;
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        if (value.applied) {
            state.setText("추천인 등록 완료 · 무료 이용 +" + Math.max(5, value.bonusDays) + "일");
            state.setTextColor(getColor(R.color.primary));
            codeInput.setVisibility(EditText.GONE);
            apply.setVisibility(Button.GONE);
        } else {
            state.setText("아직 등록된 추천인이 없습니다.");
            state.setTextColor(getColor(R.color.text_secondary));
            codeInput.setVisibility(EditText.VISIBLE);
            apply.setVisibility(Button.VISIBLE);
        }
    }

    private void showError(String code) {
        String message;
        if ("SELF_REFERRAL".equals(code)) message = "본인 추천인 코드는 등록할 수 없습니다.";
        else if ("REFERRAL_ALREADY_APPLIED".equals(code)) message = "이미 추천인 등록을 완료했습니다.";
        else if ("PAID_CONVERSION_COMPLETED".equals(code)) message = "첫 유료 결제 이후에는 등록할 수 없습니다.";
        else if ("REFERRAL_CODE_NOT_FOUND".equals(code)) message = "존재하지 않는 추천인 코드입니다.";
        else message = "추천인 코드를 등록하지 못했습니다. 코드를 확인해주세요.";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setWorking(boolean value) {
        working = value;
        apply.setEnabled(!value);
        apply.setAlpha(value ? 0.55f : 1f);
        apply.setText(value ? "등록 중…" : "추천 혜택 받기");
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(codeInput.getWindowToken(), 0);
    }

    private TextView title(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView body(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(14f);
        view.setTextColor(getColor(R.color.text_secondary));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.2f);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
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
