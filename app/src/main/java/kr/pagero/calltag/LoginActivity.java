package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LoginActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<View> actionViews = new ArrayList<>();
    private final List<EditText> inputs = new ArrayList<>();

    private LinearLayout tabs;
    private LinearLayout loginForm;
    private LinearLayout signupForm;
    private LinearLayout resetForm;
    private TextView loginTab;
    private TextView signupTab;
    private TextView notice;
    private TextView loginButton;
    private TextView googleButton;
    private TextView signupVerificationButton;
    private TextView signupButton;
    private TextView resetVerificationButton;
    private TextView resetButton;

    private EditText loginEmail;
    private EditText loginPassword;
    private EditText signupName;
    private EditText signupPhone;
    private EditText signupEmail;
    private EditText signupCode;
    private EditText signupPassword;
    private EditText signupBrand;
    private EditText signupIndustry;
    private EditText resetEmail;
    private EditText resetCode;
    private EditText resetPassword;
    private CheckBox privacyConsent;
    private CheckBox termsConsent;
    private boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        bindActions();
        configureKeyboardFlow();
        showLogin();
        handleGoogleCallback(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleGoogleCallback(intent);
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(30), dp(20), dp(42));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView logo = text("C", 24f, true, android.R.color.white);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundResource(R.drawable.bg_logo_mark);
        root.addView(logo, fixed(58));

        TextView title = text("콜태그", 28f, true, R.color.text_primary);
        title.setGravity(Gravity.CENTER);
        root.addView(title, topWrap(15));

        TextView subtitle = text("통화 후 고객관리, 놓치지 않게 정리하세요", 13f,
                false, R.color.text_secondary);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, topWrap(5));

        tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        loginTab = tab("로그인", true);
        signupTab = tab("회원가입", false);
        tabs.addView(loginTab, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams signupTabParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        signupTabParams.leftMargin = dp(8);
        tabs.addView(signupTab, signupTabParams);
        root.addView(tabs, topMatch(22));

        notice = text("", 13f, false, R.color.text_secondary);
        notice.setPadding(dp(13), dp(12), dp(13), dp(12));
        notice.setBackgroundResource(R.drawable.bg_preview);
        notice.setVisibility(View.GONE);
        root.addView(notice, topMatch(12));

        loginForm = card();
        buildLoginForm(loginForm);
        root.addView(loginForm, topMatch(14));

        signupForm = card();
        signupForm.setVisibility(View.GONE);
        buildSignupForm(signupForm);
        root.addView(signupForm, topMatch(14));

        resetForm = card();
        resetForm.setVisibility(View.GONE);
        buildResetForm(resetForm);
        root.addView(resetForm, topMatch(14));
        return scroll;
    }

    private void buildLoginForm(LinearLayout form) {
        form.addView(sectionTitle("로그인", "가입한 이메일과 비밀번호를 입력해주세요."), matchWrap());
        loginEmail = addField(form, "이메일", "name@example.com",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 14);
        loginPassword = addField(form, "비밀번호", "비밀번호",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, 10);

        loginButton = button("로그인", true);
        form.addView(loginButton, fixedTop(52, 14));
        actionViews.add(loginButton);

        googleButton = button("G  Google로 계속하기", false);
        form.addView(googleButton, fixedTop(52, 9));
        actionViews.add(googleButton);

        TextView reset = link("비밀번호를 잊으셨나요?");
        reset.setGravity(Gravity.CENTER);
        reset.setOnClickListener(v -> showReset());
        form.addView(reset, fixedTop(44, 8));
        actionViews.add(reset);
    }

    private void buildSignupForm(LinearLayout form) {
        form.addView(sectionTitle("회원가입", "필수 정보만 입력해도 바로 시작할 수 있습니다."), matchWrap());

        form.addView(groupTitle("필수 정보", "회원 식별과 로그인에 필요합니다."), topMatch(16));
        signupName = addField(form, "[필수] 이름", "이름",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, 10);
        signupPhone = addField(form, "[필수] 휴대폰번호", "01012345678",
                InputType.TYPE_CLASS_PHONE, 10);
        signupEmail = addField(form, "[필수] 이메일", "name@example.com",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 10);

        signupVerificationButton = button("인증번호 받기", false);
        form.addView(signupVerificationButton, fixedTop(48, 8));
        actionViews.add(signupVerificationButton);

        signupCode = addField(form, "[필수] 이메일 인증번호", "6자리 인증번호",
                InputType.TYPE_CLASS_NUMBER, 10);
        signupCode.setMaxLines(1);
        signupPassword = addField(form, "[필수] 비밀번호", "영문+숫자 6자리 이상",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, 10);

        form.addView(groupTitle("선택 정보", "지금 입력하지 않아도 가입 후 설정할 수 있습니다."), topMatch(20));
        signupBrand = addField(form, "[선택] 브랜드명", "예: 도윤마케팅",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 10);
        signupIndustry = addField(form, "[선택] 업종", "예: 보험, 부동산, 쇼핑몰",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 10);

        LinearLayout consent = new LinearLayout(this);
        consent.setOrientation(LinearLayout.VERTICAL);
        consent.setPadding(dp(13), dp(13), dp(13), dp(12));
        consent.setBackgroundResource(R.drawable.bg_preview);
        consent.addView(text("필수 동의", 14f, true, R.color.text_primary), matchWrap());
        consent.addView(text("회원가입 완료 전에 아래 두 항목에 동의해주세요.", 12f,
                false, R.color.text_secondary), topWrap(4));

        privacyConsent = checkbox("[필수] 개인정보 수집·이용 동의");
        consent.addView(privacyConsent, topMatch(8));
        TextView privacy = link("개인정보처리방침 자세히 보기");
        privacy.setOnClickListener(v -> openWeb("https://call.pagero.kr/privacy/"));
        consent.addView(privacy, fixedTop(38, 0));
        actionViews.add(privacy);

        termsConsent = checkbox("[필수] 서비스 이용약관 동의");
        consent.addView(termsConsent, topMatch(2));
        TextView terms = link("서비스 이용약관 자세히 보기");
        terms.setOnClickListener(v -> openWeb("https://call.pagero.kr/terms/"));
        consent.addView(terms, fixedTop(38, 0));
        actionViews.add(terms);
        form.addView(consent, topMatch(16));

        signupButton = button("회원가입 완료", true);
        form.addView(signupButton, fixedTop(54, 14));
        actionViews.add(signupButton);
    }

    private void buildResetForm(LinearLayout form) {
        form.addView(sectionTitle("비밀번호 재설정", "가입 이메일로 인증번호를 받은 뒤 변경합니다."), matchWrap());
        resetEmail = addField(form, "가입 이메일", "name@example.com",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 14);

        resetVerificationButton = button("인증번호 받기", false);
        form.addView(resetVerificationButton, fixedTop(48, 8));
        actionViews.add(resetVerificationButton);

        resetCode = addField(form, "인증번호", "6자리 인증번호",
                InputType.TYPE_CLASS_NUMBER, 10);
        resetPassword = addField(form, "새 비밀번호", "영문+숫자 6자리 이상",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, 10);

        resetButton = button("비밀번호 변경", true);
        form.addView(resetButton, fixedTop(52, 14));
        actionViews.add(resetButton);

        TextView back = link("로그인으로 돌아가기");
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> showLogin());
        form.addView(back, fixedTop(44, 8));
        actionViews.add(back);
    }

    private void bindActions() {
        loginTab.setOnClickListener(v -> showLogin());
        signupTab.setOnClickListener(v -> showSignup());
        loginButton.setOnClickListener(v -> submitLogin());
        googleButton.setOnClickListener(v -> startGoogleLogin());
        signupVerificationButton.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            requestSignupVerification();
        });
        signupButton.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            signup();
        });
        resetVerificationButton.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            requestResetVerification();
        });
        resetButton.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            resetPassword();
        });
    }

    private void configureKeyboardFlow() {
        next(loginEmail, loginPassword);
        done(loginPassword, this::submitLogin);

        next(signupName, signupPhone);
        next(signupPhone, signupEmail);
        next(signupEmail, signupCode);
        next(signupCode, signupPassword);
        next(signupPassword, signupBrand);
        next(signupBrand, signupIndustry);
        done(signupIndustry, this::signup);

        next(resetEmail, resetCode);
        next(resetCode, resetPassword);
        done(resetPassword, this::resetPassword);
    }

    private void next(EditText current, EditText target) {
        current.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        current.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_NEXT) return false;
            target.requestFocus();
            target.setSelection(target.length());
            return true;
        });
    }

    private void done(EditText current, Runnable action) {
        current.setImeOptions(EditorInfo.IME_ACTION_DONE);
        current.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE && actionId != EditorInfo.IME_ACTION_GO) {
                return false;
            }
            action.run();
            return true;
        });
    }

    private void showLogin() {
        showForm(loginForm);
        setTab(true);
    }

    private void showSignup() {
        showForm(signupForm);
        setTab(false);
    }

    private void showReset() {
        if (working) return;
        showForm(resetForm);
        tabs.setVisibility(View.GONE);
    }

    private void showForm(View target) {
        if (working) return;
        hideKeyboardAndClearFocus();
        clearNotice();
        tabs.setVisibility(View.VISIBLE);
        loginForm.setVisibility(target == loginForm ? View.VISIBLE : View.GONE);
        signupForm.setVisibility(target == signupForm ? View.VISIBLE : View.GONE);
        resetForm.setVisibility(target == resetForm ? View.VISIBLE : View.GONE);
    }

    private void setTab(boolean loginSelected) {
        loginTab.setBackgroundResource(loginSelected
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        loginTab.setTextColor(getColor(loginSelected
                ? android.R.color.white : R.color.primary));
        signupTab.setBackgroundResource(loginSelected
                ? R.drawable.bg_secondary_button : R.drawable.bg_primary_button);
        signupTab.setTextColor(getColor(loginSelected
                ? R.color.primary : android.R.color.white));
    }

    private void submitLogin() {
        hideKeyboardAndClearFocus();
        if (text(loginEmail).isEmpty() || text(loginPassword).isEmpty()) {
            showNotice("이메일과 비밀번호를 입력해주세요.", true);
            return;
        }
        runTask(() -> AuthApiClient.login(text(loginEmail), text(loginPassword)), this::acceptAuth);
    }

    private void startGoogleLogin() {
        if (working) return;
        hideKeyboardAndClearFocus();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AuthApiClient.googleLoginUrl())));
        } catch (RuntimeException error) {
            showNotice("Google 로그인 화면을 열지 못했습니다.", true);
        }
    }

    private void handleGoogleCallback(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri uri = intent.getData();
        if (!"calltag".equalsIgnoreCase(uri.getScheme())
                || !"auth".equalsIgnoreCase(uri.getHost())
                || !"/google".equals(uri.getPath())) return;
        intent.setData(null);
        showLogin();

        String providerError = clean(uri.getQueryParameter("error"));
        String providerMessage = clean(uri.getQueryParameter("message"));
        if (!providerError.isEmpty()) {
            showNotice(providerMessage.isEmpty()
                    ? "Google 로그인을 완료하지 못했습니다." : providerMessage, true);
            return;
        }

        String ticket = clean(uri.getQueryParameter("ticket"));
        if (ticket.isEmpty() || ticket.length() > 256) {
            showNotice("Google 로그인 확인값이 올바르지 않습니다.", true);
            return;
        }
        runTask(() -> AuthApiClient.exchangeGoogleTicket(ticket), this::acceptAuth);
    }

    private void requestSignupVerification() {
        if (text(signupEmail).isEmpty()) {
            showNotice("이메일을 입력해주세요.", true);
            signupEmail.requestFocus();
            return;
        }
        runTask(() -> AuthApiClient.requestVerification(text(signupEmail), "signup"), response ->
                showNotice("인증번호를 보냈습니다. 30분 안에 입력해주세요.", false));
    }

    private void signup() {
        if (text(signupName).isEmpty()) {
            required(signupName, "이름을 입력해주세요.");
            return;
        }
        if (text(signupPhone).isEmpty()) {
            required(signupPhone, "휴대폰번호를 입력해주세요.");
            return;
        }
        if (text(signupEmail).isEmpty()) {
            required(signupEmail, "이메일을 입력해주세요.");
            return;
        }
        if (text(signupCode).length() != 6) {
            required(signupCode, "이메일 인증번호 6자리를 입력해주세요.");
            return;
        }
        if (text(signupPassword).isEmpty()) {
            required(signupPassword, "비밀번호를 입력해주세요.");
            return;
        }
        if (!hasRequiredConsent()) return;

        runTask(() -> AuthApiClient.register(
                text(signupName), text(signupPhone), text(signupEmail), text(signupCode),
                text(signupBrand), text(signupIndustry), text(signupPassword)), this::acceptAuth);
    }

    private void required(EditText field, String message) {
        showNotice(message, true);
        field.requestFocus();
        field.setSelection(field.length());
    }

    private void requestResetVerification() {
        if (text(resetEmail).isEmpty()) {
            required(resetEmail, "가입 이메일을 입력해주세요.");
            return;
        }
        runTask(() -> AuthApiClient.requestVerification(text(resetEmail), "password_reset"), response ->
                showNotice("비밀번호 변경용 인증번호를 이메일로 보냈습니다.", false));
    }

    private void resetPassword() {
        if (text(resetEmail).isEmpty() || text(resetCode).isEmpty()
                || text(resetPassword).isEmpty()) {
            showNotice("이메일, 인증번호, 새 비밀번호를 입력해주세요.", true);
            return;
        }
        runTask(() -> AuthApiClient.resetPassword(
                text(resetEmail), text(resetCode), text(resetPassword)), response -> {
            Toast.makeText(this, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show();
            showLogin();
        });
    }

    private boolean hasRequiredConsent() {
        if (!privacyConsent.isChecked()) {
            showNotice("개인정보 수집·이용에 동의해야 회원가입할 수 있습니다.", true);
            return false;
        }
        if (!termsConsent.isChecked()) {
            showNotice("서비스 이용약관에 동의해야 회원가입할 수 있습니다.", true);
            return false;
        }
        return true;
    }

    private void acceptAuth(JSONObject response) {
        try {
            AuthSessionStore.save(this, response);
            if (response.optJSONObject("pageroConnection") != null
                    || response.optJSONObject("connection") != null) {
                PageroAccountStatusStore.save(this, response);
                PageroAccountStatusStore.Snapshot status = PageroAccountStatusStore.read(this);
                if (!status.connected()) {
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show();
                }
            } else {
                PageroAccountConnectionManager.refresh(this, true);
            }
            CallTagPushManager.registerIfAvailable(this);

            Intent destination = SetupRequirements.isReady(this)
                    ? new Intent(this, MainActivity.class)
                    : SetupRequirements.requiredSetupIntent(this);
            startActivity(destination.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        } catch (Exception error) {
            showNotice("로그인 정보 저장 오류입니다. 다시 시도해주세요.", true);
        }
    }

    private void runTask(Task task, Success success) {
        if (working) return;
        working = true;
        setFormsEnabled(false);
        showNotice("처리 중입니다.", false);
        executor.execute(() -> {
            try {
                JSONObject response = task.run();
                runOnUiThread(() -> {
                    working = false;
                    setFormsEnabled(true);
                    clearNotice();
                    success.accept(response);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    working = false;
                    setFormsEnabled(true);
                    showNotice(errorMessage(error), true);
                });
            }
        });
    }

    private void setFormsEnabled(boolean enabled) {
        loginTab.setEnabled(enabled);
        signupTab.setEnabled(enabled);
        for (View view : actionViews) {
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1f : 0.55f);
        }
        for (EditText input : inputs) input.setEnabled(enabled);
        privacyConsent.setEnabled(enabled);
        termsConsent.setEnabled(enabled);
    }

    private String errorMessage(Exception error) {
        if (error instanceof AuthApiClient.ApiException) {
            AuthApiClient.ApiException api = (AuthApiClient.ApiException) error;
            String code = api.code;
            if ("AUTH_EMAIL_DUPLICATE".equals(code)) return "이미 가입된 이메일입니다.";
            if ("AUTH_PHONE_DUPLICATE".equals(code)) return "이미 가입된 휴대폰번호입니다.";
            if ("EMAIL_VERIFICATION_INVALID".equals(code)) return "이메일 인증번호가 올바르지 않습니다.";
            if ("EMAIL_VERIFICATION_EXPIRED".equals(code)) return "인증번호가 만료되었습니다. 다시 받아주세요.";
            if ("EMAIL_VERIFICATION_COOLDOWN".equals(code)) return "인증메일을 이미 보냈습니다. 잠시 후 다시 시도해주세요.";
            if ("EMAIL_VERIFICATION_STORAGE_FAILED".equals(code)) return "인증번호 저장 오류입니다. 서버 상태를 확인해주세요.";
            if ("AUTH_LOGIN_INVALID".equals(code)) return "이메일 또는 비밀번호가 올바르지 않습니다.";
            if ("AUTH_PASSWORD_POLICY".equals(code)) return "비밀번호는 영문과 숫자를 포함해 6자리 이상이어야 합니다.";
            if ("CALL_PROFILE_REQUIRED".equals(code)) return "이름과 휴대폰번호를 확인해주세요.";
            if ("EMAIL_VERIFICATION_REQUIRED".equals(code)) return "이메일 인증이 완료되지 않은 계정입니다.";
            if ("AUTH_ACCOUNT_SUSPENDED".equals(code)) return "사용이 정지된 계정입니다.";
            if ("AUTH_ACCOUNT_DELETED".equals(code)) return "삭제 처리된 계정입니다.";
            if ("GOOGLE_LOGIN_NOT_CONFIGURED".equals(code)) return "Google 로그인 운영 설정이 아직 완료되지 않았습니다.";
            if ("GOOGLE_TICKET_EXPIRED".equals(code)) return "Google 로그인 시간이 만료되었습니다. 다시 시도해주세요.";
            if ("GOOGLE_TICKET_INVALID".equals(code) || "GOOGLE_TICKET_USED".equals(code)) {
                return "Google 로그인 확인값이 올바르지 않습니다. 다시 시도해주세요.";
            }
            String message = error.getMessage();
            if (message != null && !message.trim().isEmpty()) return message;
        }
        return "서버 연결을 확인해주세요.";
    }

    private LinearLayout card() {
        LinearLayout value = new LinearLayout(this);
        value.setOrientation(LinearLayout.VERTICAL);
        value.setPadding(dp(16), dp(16), dp(16), dp(17));
        value.setBackgroundResource(R.drawable.bg_card);
        return value;
    }

    private View sectionTitle(String title, String description) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.addView(text(title, 20f, true, R.color.text_primary), matchWrap());
        group.addView(text(description, 13f, false, R.color.text_secondary), topWrap(4));
        return group;
    }

    private View groupTitle(String title, String description) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(dp(1), 0, dp(1), 0);
        group.addView(text(title, 15f, true, R.color.text_primary), matchWrap());
        group.addView(text(description, 12f, false, R.color.text_secondary), topWrap(3));
        return group;
    }

    private EditText addField(LinearLayout parent, String label, String hint,
                              int inputType, int topMargin) {
        parent.addView(text(label, 13f, true,
                label.startsWith("[필수]") ? R.color.text_primary : R.color.text_secondary),
                topWrap(topMargin));
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setHint(hint);
        input.setTextSize(15f);
        input.setTextColor(getColor(R.color.text_primary));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), 0, dp(14), 0);
        parent.addView(input, fixedTop(52, 6));
        inputs.add(input);
        return input;
    }

    private TextView tab(String label, boolean selected) {
        TextView value = text(label, 15f, true,
                selected ? android.R.color.white : R.color.primary);
        value.setGravity(Gravity.CENTER);
        value.setBackgroundResource(selected
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        value.setClickable(true);
        value.setFocusable(true);
        return value;
    }

    private TextView button(String label, boolean primary) {
        TextView value = text(label, 15f, true,
                primary ? android.R.color.white : R.color.text_primary);
        value.setGravity(Gravity.CENTER);
        value.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        value.setClickable(true);
        value.setFocusable(true);
        return value;
    }

    private TextView link(String label) {
        TextView value = text(label, 13f, true, R.color.primary);
        value.setGravity(Gravity.CENTER_VERTICAL);
        value.setPadding(dp(4), 0, dp(4), 0);
        value.setClickable(true);
        value.setFocusable(true);
        return value;
    }

    private CheckBox checkbox(String label) {
        CheckBox value = new CheckBox(this);
        value.setText(label);
        value.setTextSize(13f);
        value.setTextColor(getColor(R.color.text_primary));
        value.setButtonTintList(getColorStateList(R.color.primary));
        value.setGravity(Gravity.CENTER_VERTICAL);
        value.setMinHeight(dp(42));
        return value;
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(color));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.2f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void showNotice(String message, boolean error) {
        notice.setVisibility(View.VISIBLE);
        notice.setText(message);
        notice.setTextColor(getColor(error ? R.color.danger : R.color.text_secondary));
    }

    private void clearNotice() {
        notice.setVisibility(View.GONE);
        notice.setText("");
    }

    private void hideKeyboardAndClearFocus() {
        View focused = getCurrentFocus();
        if (focused == null) return;
        InputMethodManager manager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        focused.clearFocus();
    }

    private void openWeb(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception error) {
            Toast.makeText(this, "웹페이지를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String text(EditText editText) {
        return editText == null ? "" : editText.getText().toString().trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topWrap(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams topMatch(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixed(int size) {
        return new LinearLayout.LayoutParams(dp(size), dp(size));
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

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private interface Task { JSONObject run() throws Exception; }
    private interface Success { void accept(JSONObject response); }
}
