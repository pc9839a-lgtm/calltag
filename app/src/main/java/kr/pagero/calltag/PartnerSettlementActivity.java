package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

/** 파트너 수익과 계좌·세금 정산정보를 별도 관리한다. */
public final class PartnerSettlementActivity extends Activity {
    private Spinner payoutType;
    private EditText bankName;
    private EditText accountHolder;
    private EditText accountNumber;
    private EditText settlementEmail;
    private EditText businessNumber;
    private TextView estimated;
    private TextView confirmed;
    private TextView profileState;
    private Button refresh;
    private boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        loadProfile();
        renderRevenue();
        refreshSummary(false);
    }

    private ScrollView buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(40));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = title("‹", 30f);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        header.addView(title("정산", 22f), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        refresh = button("새로고침", false);
        refresh.setOnClickListener(v -> refreshSummary(true));
        header.addView(refresh, new LinearLayout.LayoutParams(dp(88), dp(42)));
        root.addView(header, matchWrap());

        LinearLayout revenue = card();
        estimated = metric(revenue, "이번 달 예상 수익", true);
        confirmed = metric(revenue, "누적 확정 수익", false);
        revenue.addView(body("환불·할인·부가세가 반영된 확정 결제금액을 기준으로 정산됩니다."), top(14));
        root.addView(revenue, top(18));

        root.addView(title("정산정보", 17f), top(24));
        profileState = body("");
        profileState.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(profileState, top(8));

        root.addView(label("정산 유형"), top(16));
        payoutType = new Spinner(this);
        payoutType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"개인", "개인사업자", "법인사업자"}));
        payoutType.setBackgroundResource(R.drawable.bg_secondary_button);
        payoutType.setPadding(dp(12), 0, dp(12), 0);
        root.addView(payoutType, fixedTop(52, 7));

        bankName = addField(root, "은행", "예: 국민은행", InputType.TYPE_CLASS_TEXT);
        accountHolder = addField(root, "예금주", "예금주명", InputType.TYPE_CLASS_TEXT);
        accountNumber = addField(root, "계좌번호", "숫자만 입력", InputType.TYPE_CLASS_NUMBER);
        settlementEmail = addField(root, "정산 연락 이메일", "settlement@example.com",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        businessNumber = addField(root, "사업자등록번호", "사업자인 경우 입력",
                InputType.TYPE_CLASS_NUMBER);

        Button save = button("정산정보 저장", true);
        save.setOnClickListener(v -> saveProfile());
        root.addView(save, fixedTop(52, 22));

        Button clear = button("저장된 계좌정보 삭제", false);
        clear.setTextColor(getColor(R.color.danger));
        clear.setOnClickListener(v -> confirmClear());
        root.addView(clear, fixedTop(50, 8));

        root.addView(body("계좌번호는 Android Keystore로 암호화되어 현재 계정의 이 기기에 저장됩니다. "
                + "실제 송금 전에는 서버 정산 검증 절차를 추가로 거칩니다."), top(16));
        return scroll;
    }

    private EditText addField(LinearLayout root, String label, String hint, int inputType) {
        root.addView(label(label), top(16));
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setTextSize(15f);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), 0, dp(14), 0);
        root.addView(input, fixedTop(52, 7));
        return input;
    }

    private TextView metric(LinearLayout parent, String label, boolean first) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(body(label), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = title("0원", 18f);
        row.addView(value);
        parent.addView(row, first ? matchWrap() : top(16));
        return value;
    }

    private void loadProfile() {
        PartnerSettlementStore.Profile profile = PartnerSettlementStore.read(this);
        String[] types = {"개인", "개인사업자", "법인사업자"};
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(profile.payoutType)) payoutType.setSelection(i);
        }
        bankName.setText(profile.bankName);
        accountHolder.setText(profile.accountHolder);
        accountNumber.setText(profile.accountNumber);
        settlementEmail.setText(profile.settlementEmail);
        businessNumber.setText(profile.businessNumber);
        renderProfileState(profile);
    }

    private void saveProfile() {
        String type = String.valueOf(payoutType.getSelectedItem());
        PartnerSettlementStore.Profile profile = new PartnerSettlementStore.Profile(
                type,
                bankName.getText().toString(),
                accountHolder.getText().toString(),
                accountNumber.getText().toString(),
                settlementEmail.getText().toString(),
                businessNumber.getText().toString(),
                System.currentTimeMillis());
        if (profile.bankName.isEmpty() || profile.accountHolder.isEmpty()) {
            Toast.makeText(this, "은행과 예금주를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (profile.accountNumber.replaceAll("[^0-9]", "").length() < 8) {
            Toast.makeText(this, "계좌번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!profile.settlementEmail.contains("@")) {
            Toast.makeText(this, "정산 연락 이메일을 정확히 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!"개인".equals(type)
                && profile.businessNumber.replaceAll("[^0-9]", "").length() != 10) {
            Toast.makeText(this, "사업자등록번호 10자리를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            PartnerSettlementStore.save(this, profile);
            renderProfileState(PartnerSettlementStore.read(this));
            Toast.makeText(this, "정산정보를 안전하게 저장했습니다.", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException error) {
            Toast.makeText(this, "정산정보를 저장하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("계좌정보 삭제")
                .setMessage("이 기기에 저장된 계좌·정산정보를 삭제합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    PartnerSettlementStore.clear(this);
                    loadProfile();
                    Toast.makeText(this, "저장된 정산정보를 삭제했습니다.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void renderProfileState(PartnerSettlementStore.Profile profile) {
        if (profile.isComplete()) {
            profileState.setText("정산정보 등록 완료 · " + profile.bankName + " " + profile.maskedAccount());
            profileState.setTextColor(getColor(R.color.primary));
        } else {
            profileState.setText("정산을 받을 계좌정보를 등록해주세요.");
            profileState.setTextColor(getColor(R.color.danger));
        }
    }

    private void refreshSummary(boolean notify) {
        if (working) return;
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) return;
        working = true;
        refresh.setEnabled(false);
        new Thread(() -> {
            boolean success = false;
            try {
                JSONObject result = AuthApiClient.referralSummary(session);
                ReferralStateStore.saveSummary(this, result);
                success = true;
            } catch (Exception ignored) {}
            boolean loaded = success;
            runOnUiThread(() -> {
                working = false;
                refresh.setEnabled(true);
                renderRevenue();
                if (notify) Toast.makeText(this,
                        loaded ? "정산 현황을 갱신했습니다." : "정산 현황을 확인하지 못했습니다.",
                        loaded ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            });
        }, "calltag-settlement-summary").start();
    }

    private void renderRevenue() {
        if (estimated == null) return;
        ReferralStateStore.Snapshot value = ReferralStateStore.snapshot(this);
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.KOREA);
        estimated.setText(format.format(Math.max(0L, value.estimatedRevenueKrw)) + "원");
        confirmed.setText(format.format(Math.max(0L, value.confirmedRevenueKrw)) + "원");
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView label(String value) {
        TextView view = body(value);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
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
