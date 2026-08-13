package kr.pagero.calltag;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LoginActivity extends Activity {
    private static final String TAG = "CallTagLogin";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private View tabs;
    private View loginForm;
    private View signupForm;
    private View resetForm;
    private TextView loginTab;
    private TextView signupTab;
    private TextView notice;
    private TextView loginButton;
    private TextView googleButton;
    private EditText loginEmail;
    private EditText loginPassword;
    private EditText signupReferral;
    private CheckBox privacyConsent;
    private CheckBox termsConsent;
    private boolean working;
    private boolean googleCredentialInFlight;
    private int googleAttemptId;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        bindViews();
        installGoogleButton();
        installSignupReferralField();
        bindActions();
        showLogin();
        handleGoogleCallback(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        PendingReferralStore.capture(this, intent);
        if (signupReferral != null && signupReferral.getText().toString().trim().isEmpty()) {
            String pending = PendingReferralStore.peek(this);
            if (!pending.isEmpty()) signupReferral.setText(pending);
        }
        handleGoogleCallback(intent);
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

    private void installGoogleButton() {
        LinearLayout form = (LinearLayout) loginForm;
        googleButton = new TextView(this);
        googleButton.setGravity(Gravity.CENTER);
        googleButton.setText("G  Google로 계속하기");
        googleButton.setTextSize(15f);
        googleButton.setTextColor(getColor(R.color.text_primary));
        googleButton.setBackgroundResource(R.drawable.bg_secondary_button);
        googleButton.setContentDescription("Google 계정으로 로그인");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        params.topMargin = dp(8);
        form.addView(googleButton, Math.min(3, form.getChildCount()), params);
    }

    private void installSignupReferralField() {
        LinearLayout form = (LinearLayout) signupForm;
        View privacyRow = privacyConsent == null ? null : (View) privacyConsent.getParent();
        int privacyIndex = privacyRow == null ? -1 : form.indexOfChild(privacyRow);
        int insertAt = privacyIndex > 0 ? privacyIndex - 1 : Math.max(0, form.getChildCount() - 4);

        TextView benefit = new TextView(this);
        benefit.setText("추천인 코드 입력 시 무료 7일 추가");
        benefit.setTextSize(12f);
        benefit.setTextColor(getColor(R.color.primary));
        benefit.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams benefitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        benefitParams.topMargin = dp(12);
        form.addView(benefit, insertAt, benefitParams);

        signupReferral = new EditText(this);
        signupReferral.setSingleLine(true);
        signupReferral.setHint("추천인 코드");
        signupReferral.setTextSize(15f);
        signupReferral.setTextColor(getColor(R.color.text_primary));
        signupReferral.setHintTextColor(getColor(R.color.text_muted));
        signupReferral.setBackgroundResource(R.drawable.bg_input);
        signupReferral.setPadding(dp(14), 0, dp(14), 0);
        signupReferral.setAllCaps(true);
        signupReferral.setMaxLines(1);
        signupReferral.setContentDescription("추천인 코드");
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        inputParams.topMargin = dp(6);
        form.addView(signupReferral, insertAt + 1, inputParams);

        String pending = PendingReferralStore.peek(this);
        if (!pending.isEmpty()) signupReferral.setText(pending);
    }

    private void bindActions() {
        loginTab.setOnClickListener(v -> showLogin());
        signupTab.setOnClickListener(v -> showSignup());
        findViewById(R.id.btnOpenPasswordReset).setOnClickListener(v -> showReset());
        loginButton.setOnClickListener(v -> submitLogin());
        googleButton.setOnClickListener(v -> startGoogleLogin());
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
        findViewById(R.id.btnClosePasswordReset).setOnClickListener(v -> showLogin());
        findViewById(R.id.btnPrivacyDetail).setOnClickListener(v ->
                openWeb("https://calltag.pagero.kr/privacy"));
        findViewById(R.id.btnTermsDetail).setOnClickListener(v ->
                openWeb("https://calltag.pagero.kr/terms"));

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
        if (working || googleCredentialInFlight) return;
        showForm(resetForm);
        tabs.setVisibility(View.GONE);
    }

    private void showForm(View target) {
        if (working || googleCredentialInFlight) return;
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

    /**
     * Run Credential Manager directly from the Activity that received the user's button tap.
     * Do not insert a bridge Activity and do not provide our own CancellationSignal. That keeps
     * the system account picker and its result bound to this visible LoginActivity task stack.
     */
    private void startGoogleLogin() {
        if (working || googleCredentialInFlight || isFinishing() || isDestroyed()) return;
        hideKeyboardAndClearFocus();

        String serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID == null
                ? "" : BuildConfig.GOOGLE_SERVER_CLIENT_ID.trim();
        if (serverClientId.isEmpty()) {
            showNotice("Google 로그인 연결 정보를 불러오지 못했습니다.", true);
            return;
        }

        final String nonce = secureNonce();
        final GetCredentialRequest request;
        try {
            GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(serverClientId)
                    .setNonce(nonce)
                    .build();
            request = new GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build();
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to create Google credential request", error);
            showNotice("Google 로그인을 시작하지 못했습니다. 다시 시도해주세요.", true);
            return;
        }

        final int currentAttempt = ++googleAttemptId;
        googleCredentialInFlight = true;
        setFormsEnabled(false);
        showNotice("Google 계정을 선택해주세요.", false);

        if (credentialManager == null) credentialManager = CredentialManager.create(this);
        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        if (!isGoogleAttemptActive(currentAttempt)) return;
                        handleGoogleCredential(result, nonce, currentAttempt);
                    }

                    @Override
                    public void onError(GetCredentialException error) {
                        if (!isGoogleAttemptActive(currentAttempt)) return;
                        googleCredentialInFlight = false;
                        setFormsEnabled(true);
                        Log.e(TAG, "Credential Manager failure: class="
                                + error.getClass().getSimpleName()
                                + " type=" + error.getType()
                                + " message=" + error.getMessage(), error);
                        if (error instanceof GetCredentialCancellationException) {
                            // Cancellation can also be returned when provider authorization could
                            // not complete for technical reasons; don't claim the user cancelled.
                            showNotice("Google 인증을 완료하지 못했습니다. 다시 시도해주세요.", true);
                        } else {
                            showNotice(credentialErrorMessage(error), true);
                        }
                    }
                });
    }

    private void handleGoogleCredential(
            GetCredentialResponse result,
            String nonce,
            int currentAttempt) {
        if (!isGoogleAttemptActive(currentAttempt)) return;
        try {
            Credential credential = result.getCredential();
            if (!(credential instanceof CustomCredential)) {
                Log.e(TAG, "Unexpected Google credential class: " + credential.getClass().getName());
                failGoogleAttempt(currentAttempt,
                        "Google 계정 응답을 확인하지 못했습니다. 다시 시도해주세요.");
                return;
            }

            String type = credential.getType();
            boolean supportedType = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(type)
                    || GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL.equals(type);
            if (!supportedType) {
                Log.e(TAG, "Unexpected Google credential type: " + type);
                failGoogleAttempt(currentAttempt,
                        "Google 계정 응답을 확인하지 못했습니다. 다시 시도해주세요.");
                return;
            }

            GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(
                    ((CustomCredential) credential).getData());
            String idToken = google.getIdToken();
            if (idToken == null || idToken.trim().isEmpty()) {
                failGoogleAttempt(currentAttempt,
                        "Google 로그인 토큰을 받지 못했습니다. 다시 시도해주세요.");
                return;
            }

            showNotice("Google 로그인 확인 중…", false);
            executor.execute(() -> {
                try {
                    JSONObject response = AuthApiClient.exchangeGoogleIdToken(idToken, nonce);
                    runOnUiThread(() -> {
                        if (!isGoogleAttemptActive(currentAttempt)) return;
                        googleCredentialInFlight = false;
                        setFormsEnabled(true);
                        clearNotice();
                        acceptAuth(response);
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Google ID token exchange failed", error);
                    runOnUiThread(() -> {
                        if (!isGoogleAttemptActive(currentAttempt)) return;
                        googleCredentialInFlight = false;
                        setFormsEnabled(true);
                        showNotice(googleServerErrorMessage(error), true);
                    });
                }
            });
        } catch (Exception error) {
            Log.e(TAG, "Failed to parse Google credential", error);
            failGoogleAttempt(currentAttempt,
                    "Google 로그인 응답을 처리하지 못했습니다. 다시 시도해주세요.");
        }
    }

    private void failGoogleAttempt(int currentAttempt, String message) {
        if (!isGoogleAttemptActive(currentAttempt)) return;
        googleCredentialInFlight = false;
        setFormsEnabled(true);
        showNotice(message, true);
    }

    private boolean isGoogleAttemptActive(int currentAttempt) {
        return currentAttempt == googleAttemptId
                && !isFinishing()
                && !isDestroyed();
    }

    private String credentialErrorMessage(GetCredentialException error) {
        String type = error == null || error.getType() == null ? "" : error.getType().toLowerCase();
        if (type.contains("configuration") || type.contains("provider_configuration")) {
            return "Google 로그인 앱 설정을 확인하지 못했습니다.";
        }
        if (type.contains("no_credential")) {
            return "사용 가능한 Google 계정을 찾지 못했습니다.";
        }
        if (type.contains("interrupted")) {
            return "Google 로그인 요청이 중단되었습니다. 다시 시도해주세요.";
        }
        return "Google 계정을 인증하지 못했습니다. 다시 시도해주세요.";
    }

    private String googleServerErrorMessage(Exception error) {
        if (error instanceof AuthApiClient.ApiException) {
            AuthApiClient.ApiException api = (AuthApiClient.ApiException) error;
            if ("GOOGLE_NONCE_MISMATCH".equals(api.code)) {
                return "Google 로그인 요청 확인에 실패했습니다. 다시 시도해주세요.";
            }
            if ("GOOGLE_ID_TOKEN_AUDIENCE_INVALID".equals(api.code)) {
                return "Google 로그인 연결 정보가 일치하지 않습니다.";
            }
            if ("GOOGLE_ID_TOKEN_INVALID".equals(api.code)
                    || "GOOGLE_ID_TOKEN_SIGNATURE_INVALID".equals(api.code)) {
                return "Google 로그인 인증값을 확인하지 못했습니다.";
            }
            if ("GOOGLE_JWKS_NETWORK_FAILED".equals(api.code)
                    || "GOOGLE_JWKS_UNAVAILABLE".equals(api.code)) {
                return "Google 로그인 서버 연결이 지연되고 있습니다. 다시 시도해주세요.";
            }
            String message = error.getMessage();
            if (message != null && !message.trim().isEmpty()) return message;
        }
        return "Google 로그인을 완료하지 못했습니다. 다시 시도해주세요.";
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
        EditText email = findViewById(R.id.editSignupEmail);
        if (text(email).isEmpty()) {
            showNotice("이메일을 입력해주세요.", true);
            return;
        }
        runTask(() -> AuthApiClient.requestVerification(text(email), "signup"), response ->
                showNotice("인증번호를 보냈습니다. 30분 안에 입력해주세요.", false));
    }

    private void signup() {
        EditText name = findViewById(R.id.editSignupName);
        EditText phone = findViewById(R.id.editSignupPhone);
        EditText email = findViewById(R.id.editSignupEmail);
        EditText code = findViewById(R.id.editSignupCode);
        EditText brand = findViewById(R.id.editSignupBrand);
        EditText industry = findViewById(R.id.editSignupIndustry);
        EditText password = findViewById(R.id.editSignupPassword);

        if (text(name).isEmpty() || text(phone).isEmpty() || text(email).isEmpty()
                || text(code).isEmpty() || text(password).isEmpty()) {
            showNotice("빨간 * 표시 항목을 모두 입력해주세요.", true);
            return;
        }
        if (!hasRequiredConsent()) return;

        String referral = text(signupReferral).toUpperCase(Locale.KOREA)
                .replaceAll("[^A-Z0-9]", "");
        if (!referral.isEmpty() && !referral.matches("[A-Z0-9]{4,20}")) {
            showNotice("추천인 코드를 정확히 입력하거나 비워주세요.", true);
            return;
        }

        String brandValue = text(brand).isEmpty() ? "개인" : text(brand);
        String industryValue = text(industry).isEmpty() ? "기타" : text(industry);
        runTask(() -> AuthApiClient.register(
                text(name), text(phone), text(email), text(code),
                brandValue, industryValue, text(password), referral), this::acceptAuth);
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
            showNotice("개인정보 수집·이용에 동의해주세요.", true);
            return false;
        }
        if (!termsConsent.isChecked()) {
            showNotice("서비스 이용약관에 동의해주세요.", true);
            return false;
        }
        return true;
    }

    private void acceptAuth(JSONObject response) {
        try {
            AuthSessionStore.save(this, response);
            boolean entitlementIncluded = response.optJSONObject("entitlement") != null;
            if (entitlementIncluded) {
                FeatureEntitlementStore.saveServerEntitlement(this, response);
            }
            if (response.optJSONObject("referral") != null) {
                PendingReferralStore.clear(this);
                Toast.makeText(this,
                        "추천 혜택 적용 · 통합권 총 14일 무료",
                        Toast.LENGTH_LONG).show();
            }
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
            if (!entitlementIncluded) {
                EntitlementRefreshManager.request(this, true);
            }

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
        if (working || googleCredentialInFlight) return;
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
        googleButton.setEnabled(enabled);
        googleButton.setAlpha(enabled ? 1f : 0.6f);
        privacyConsent.setEnabled(enabled);
        termsConsent.setEnabled(enabled);
        if (signupReferral != null) signupReferral.setEnabled(enabled);
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
            if ("CALL_PROFILE_REQUIRED".equals(code)) return "필수 가입 정보를 모두 입력해주세요.";
            if ("EMAIL_VERIFICATION_REQUIRED".equals(code)) return "이메일 인증이 완료되지 않은 계정입니다.";
            if ("AUTH_ACCOUNT_SUSPENDED".equals(code)) return "사용이 정지된 계정입니다.";
            if ("AUTH_ACCOUNT_DELETED".equals(code)) return "삭제 처리된 계정입니다.";
            if ("REFERRAL_CODE_NOT_FOUND".equals(code)) return "존재하지 않는 추천인 코드입니다.";
            if ("SELF_REFERRAL".equals(code)) return "본인 추천인 코드는 등록할 수 없습니다.";
            if ("REFERRAL_ALREADY_APPLIED".equals(code)) return "이미 추천 혜택이 적용된 계정입니다.";
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
        return editText == null || editText.getText() == null
                ? "" : editText.getText().toString().trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String secureNonce() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    @Override
    protected void onDestroy() {
        googleAttemptId++;
        executor.shutdownNow();
        super.onDestroy();
    }

    private interface Task { JSONObject run() throws Exception; }
    private interface Success { void accept(JSONObject response); }
}
