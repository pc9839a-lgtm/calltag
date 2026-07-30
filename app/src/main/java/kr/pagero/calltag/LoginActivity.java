package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LoginActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private View tabs;
    private View loginForm;
    private View signupForm;
    private View resetForm;
    private TextView loginTab;
    private TextView signupTab;
    private TextView notice;
    private TextView loginButton;
    private EditText loginEmail;
    private EditText loginPassword;
    private CheckBox privacyConsent;
    private CheckBox termsConsent;
    private boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        bindViews();
        bindActions();
        showLogin();
    }

    private void bindViews() {
        tabs = findViewById(R.id.authTabs);
        loginForm = findViewById(R.id.authLoginForm);
        signupForm = findViewById(R.id.authSignupForm);
        resetForm = findViewById(R.id.authResetForm);
        loginTab = findViewById(R.id.btnAuthLoginTab);
        signupTab = findViewById(R.id.btnAuthSignupTab);
        notice = findViewById(R.id.txtAuthNotice);
        loginButton = findViewById(R.id.btnAuthLogin);
        loginEmail = findViewById(R.id.editLoginEmail);
        loginPassword = findViewById(R.id.editLoginPassword);
        privacyConsent = findViewById(R.id.checkPrivacyConsent);
        termsConsent = findViewById(R.id.checkTermsConsent);
    }

    private void bindActions() {
        loginTab.setOnClickListener(v -> showLogin());
        signupTab.setOnClickListener(v -> showSignup());
        findViewById(R.id.btnOpenPasswordReset).setOnClickListener(v -> showReset());
        findViewById(R.id.btnClosePasswordReset).setOnClickListener(v -> showLogin());
        loginButton.setOnClickListener(v -> submitLogin());
        findViewById(R.id.btnSignupVerification).setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            requestSignupVerification();
        });
        findViewById(R.id.btnAuthSignup).setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            signup();
        });
        findViewById(R.id.btnResetVerification).setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            requestResetVerification();
        });
        findViewById(R.id.btnResetPassword).setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            resetPassword();
        });
        findViewById(R.id.btnPrivacyDetail).setOnClickListener(v ->
                openWeb("https://call.pagero.kr/privacy/"));
        findViewById(R.id.btnTermsDetail).setOnClickListener(v ->
                openWeb("https://call.pagero.kr/terms/"));

        loginEmail.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        loginEmail.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_NEXT) return false;
            loginPassword.requestFocus();
            loginPassword.setSelection(loginPassword.length());
            return true;
        });
        loginPassword.setImeOptions(EditorInfo.IME_ACTION_DONE);
        loginPassword.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE && actionId != EditorInfo.IME_ACTION_GO) return false;
            submitLogin();
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

    private void requestSignupVerification() {
        if (!hasRequiredConsent()) return;
        EditText email = findViewById(R.id.editSignupEmail);
        if (text(email).isEmpty()) {
            showNotice("이메일을 입력해주세요.", true);
            return;
        }
        runTask(() -> AuthApiClient.requestVerification(text(email), "signup"), response ->
                showNotice("이메일로 인증번호를 보냈습니다. 30분 안에 입력해주세요.", false));
    }

    private void signup() {
        if (!hasRequiredConsent()) return;
        EditText name = findViewById(R.id.editSignupName);
        EditText phone = findViewById(R.id.editSignupPhone);
        EditText email = findViewById(R.id.editSignupEmail);
        EditText code = findViewById(R.id.editSignupCode);
        EditText brand = findViewById(R.id.editSignupBrand);
        EditText industry = findViewById(R.id.editSignupIndustry);
        EditText password = findViewById(R.id.editSignupPassword);
        if (text(name).isEmpty() || text(phone).isEmpty() || text(email).isEmpty()
                || text(code).isEmpty() || text(brand).isEmpty()
                || text(industry).isEmpty() || text(password).isEmpty()) {
            showNotice("모든 가입 정보를 입력해주세요.", true);
            return;
        }
        runTask(() -> AuthApiClient.register(
                text(name), text(phone), text(email), text(code),
                text(brand), text(industry), text(password)), this::acceptAuth);
    }

    private void requestResetVerification() {
        EditText email = findViewById(R.id.editResetEmail);
        if (text(email).isEmpty()) {
            showNotice("가입 이메일을 입력해주세요.", true);
            return;
        }
        runTask(() -> AuthApiClient.requestVerification(text(email), "password_reset"), response ->
                showNotice("비밀번호 변경용 인증번호를 이메일로 보냈습니다.", false));
    }

    private void resetPassword() {
        EditText email = findViewById(R.id.editResetEmail);
        EditText code = findViewById(R.id.editResetCode);
        EditText password = findViewById(R.id.editResetPassword);
        if (text(email).isEmpty() || text(code).isEmpty() || text(password).isEmpty()) {
            showNotice("이메일, 인증번호, 새 비밀번호를 입력해주세요.", true);
            return;
        }
        runTask(() -> AuthApiClient.resetPassword(text(email), text(code), text(password)), response -> {
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
            Intent destination = SetupRequirements.isReady(this)
                    ? new Intent(this, MainActivity.class)
                    : SetupRequirements.requiredSetupIntent(this);
            startActivity(destination.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        } catch (Exception e) {
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
        loginButton.setEnabled(enabled);
        loginButton.setAlpha(enabled ? 1f : 0.6f);
        findViewById(R.id.btnAuthSignup).setEnabled(enabled);
        findViewById(R.id.btnSignupVerification).setEnabled(enabled);
        findViewById(R.id.btnResetVerification).setEnabled(enabled);
        findViewById(R.id.btnResetPassword).setEnabled(enabled);
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
            if ("AUTH_LOGIN_INVALID".equals(code)) return "이메일 또는 비밀번호가 올바르지 않습니다.";
            if ("AUTH_PASSWORD_POLICY".equals(code)) return "비밀번호는 영문과 숫자를 포함해 6자리 이상이어야 합니다.";
            if ("CALL_PROFILE_REQUIRED".equals(code)) return "이름, 휴대폰번호, 이메일, 브랜드명과 업종을 모두 입력해주세요.";
            if ("EMAIL_VERIFICATION_REQUIRED".equals(code)) return "이메일 인증이 완료되지 않은 계정입니다.";
            if ("AUTH_ACCOUNT_SUSPENDED".equals(code)) return "사용이 정지된 계정입니다.";
            if ("AUTH_ACCOUNT_DELETED".equals(code)) return "삭제 처리된 계정입니다.";
            String message = error.getMessage();
            if (message != null && !message.trim().isEmpty()) return message;
        }
        return "서버 연결을 확인해주세요.";
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
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        focused.clearFocus();
    }

    private void openWeb(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "웹페이지를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String text(EditText editText) {
        return editText.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private interface Task { JSONObject run() throws Exception; }
    private interface Success { void accept(JSONObject response); }
}
