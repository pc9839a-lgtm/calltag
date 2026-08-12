package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
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

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Google sign-in host. */
public final class GoogleCredentialLoginActivity extends Activity {
    private static final String TAG = "CallTagGoogleLogin";
    private static final long PROVIDER_TIMEOUT_MS = 90_000L;
    private static final long EXCHANGE_TIMEOUT_MS = 25_000L;

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CancellationSignal cancellationSignal;
    private TextView stateView;
    private boolean started;
    private boolean finished;
    private boolean tokenExchangeStarted;

    private final Runnable providerTimeout = () -> {
        if (finished || tokenExchangeStarted) return;
        Log.e(TAG, "Credential Manager did not return within timeout");
        if (cancellationSignal != null) cancellationSignal.cancel();
        fail("Google 계정 선택 응답이 없습니다. 다시 시도해주세요.");
    };

    private final Runnable exchangeTimeout = () -> {
        if (finished || !tokenExchangeStarted) return;
        Log.e(TAG, "Google token exchange timed out");
        fail("Google 로그인 서버 응답이 없습니다. 다시 시도해주세요.");
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        GoogleAuthFlowStore.clear(this);
        // Process recreation can deliver a non-null savedInstanceState while the previous
        // Credential Manager request has already been lost. Always start a fresh flow for
        // this newly-created Activity instance; the per-instance started guard prevents duplicates.
        startGoogleCredentialFlow();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(providerTimeout);
        mainHandler.removeCallbacks(exchangeTimeout);
        if (cancellationSignal != null && !cancellationSignal.isCanceled()) {
            cancellationSignal.cancel();
        }
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    private LinearLayout buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(getColor(R.color.background));

        TextView title = new TextView(this);
        title.setText("Google 로그인");
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(21f);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        stateView = new TextView(this);
        stateView.setText("Google 계정 선택창을 여는 중…");
        stateView.setTextColor(getColor(R.color.text_secondary));
        stateView.setTextSize(14f);
        stateView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        stateParams.topMargin = dp(14);
        root.addView(stateView, stateParams);
        return root;
    }

    private void startGoogleCredentialFlow() {
        if (started || finished) return;
        started = true;
        setState("Google 계정을 선택해주세요.");

        String serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID == null
                ? "" : BuildConfig.GOOGLE_SERVER_CLIENT_ID.trim();
        if (serverClientId.isEmpty()) {
            Log.e(TAG, "GOOGLE_SERVER_CLIENT_ID is empty");
            fail("Google 로그인 설정을 확인해주세요.");
            return;
        }

        final String nonce = secureNonce();
        final GetGoogleIdOption googleOption;
        try {
            googleOption = new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .setServerClientId(serverClientId)
                    .setNonce(nonce)
                    .build();
        } catch (RuntimeException error) {
            Log.e(TAG, "Failed to build Google credential option", error);
            fail("Google 로그인 설정을 확인해주세요.");
            return;
        }

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        CredentialManager manager = CredentialManager.create(this);
        cancellationSignal = new CancellationSignal();
        mainHandler.postDelayed(providerTimeout, PROVIDER_TIMEOUT_MS);

        manager.getCredentialAsync(
                this,
                request,
                cancellationSignal,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        mainHandler.removeCallbacks(providerTimeout);
                        setState("선택한 Google 계정을 확인하고 있습니다…");
                        handleCredential(result, nonce);
                    }

                    @Override
                    public void onError(GetCredentialException error) {
                        mainHandler.removeCallbacks(providerTimeout);
                        Log.e(TAG, "Credential Manager error: " + error.getClass().getSimpleName()
                                + " / " + error.getType(), error);
                        if (error instanceof GetCredentialCancellationException) {
                            finishQuietly();
                            return;
                        }
                        fail(credentialErrorMessage(error));
                    }
                });
    }

    private void handleCredential(GetCredentialResponse result, String nonce) {
        try {
            Credential credential = result.getCredential();
            if (!(credential instanceof CustomCredential)) {
                Log.e(TAG, "Unexpected credential class: " + credential.getClass().getName());
                fail("Google 로그인 정보를 확인하지 못했습니다.");
                return;
            }
            if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                Log.e(TAG, "Unexpected credential type: " + credential.getType());
                fail("Google 로그인 정보 형식이 올바르지 않습니다.");
                return;
            }

            GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(
                    ((CustomCredential) credential).getData());
            String idToken = google.getIdToken();
            if (idToken == null || idToken.trim().isEmpty()) {
                fail("Google 로그인 토큰을 확인하지 못했습니다.");
                return;
            }
            exchangeToken(idToken, nonce);
        } catch (Exception error) {
            Log.e(TAG, "Failed to parse Google credential", error);
            fail("Google 로그인 정보를 처리하지 못했습니다.");
        }
    }

    private void exchangeToken(String idToken, String nonce) {
        tokenExchangeStarted = true;
        setState("콜태그 로그인 정보를 확인하고 있습니다…");
        mainHandler.postDelayed(exchangeTimeout, EXCHANGE_TIMEOUT_MS);
        networkExecutor.execute(() -> {
            try {
                JSONObject response = AuthApiClient.exchangeGoogleIdToken(idToken, nonce);
                runOnUiThread(() -> {
                    mainHandler.removeCallbacks(exchangeTimeout);
                    completeAuth(response);
                });
            } catch (Exception error) {
                Log.e(TAG, "Google token exchange failed", error);
                runOnUiThread(() -> {
                    mainHandler.removeCallbacks(exchangeTimeout);
                    fail(errorMessage(error));
                });
            }
        });
    }

    private void completeAuth(JSONObject response) {
        if (finished) return;
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
            EntitlementRefreshManager.request(this, true);

            Intent destination = SetupRequirements.isReady(this)
                    ? new Intent(this, MainActivity.class)
                    : SetupRequirements.requiredSetupIntent(this);
            finished = true;
            startActivity(destination.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        } catch (Exception error) {
            Log.e(TAG, "Failed to persist Google login", error);
            fail("로그인 정보 저장 오류입니다. 다시 시도해주세요.");
        }
    }

    private String credentialErrorMessage(GetCredentialException error) {
        String type = error == null || error.getType() == null ? "" : error.getType();
        if (type.contains("configuration") || type.contains("provider_configuration")) {
            return "Google 로그인 앱 설정이 일치하지 않습니다. 앱 서명 정보를 확인해주세요.";
        }
        if (type.contains("no_credential")) {
            return "사용 가능한 Google 계정을 찾지 못했습니다.";
        }
        return "Google 계정 선택을 완료하지 못했습니다. 다시 시도해주세요.";
    }

    private String errorMessage(Exception error) {
        if (error instanceof AuthApiClient.ApiException) {
            AuthApiClient.ApiException api = (AuthApiClient.ApiException) error;
            if ("GOOGLE_ID_TOKEN_AUDIENCE_INVALID".equals(api.code)) {
                return "Google 로그인 클라이언트 설정이 일치하지 않습니다.";
            }
            if ("GOOGLE_NONCE_MISMATCH".equals(api.code)) {
                return "Google 로그인 요청을 다시 시작해주세요.";
            }
            if ("GOOGLE_JWKS_NETWORK_FAILED".equals(api.code)
                    || "GOOGLE_JWKS_UNAVAILABLE".equals(api.code)) {
                return "Google 로그인 인증 서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.";
            }
            if (api.getMessage() != null && !api.getMessage().trim().isEmpty()) {
                return api.getMessage();
            }
        }
        return "Google 로그인을 완료하지 못했습니다. 다시 시도해주세요.";
    }

    private void setState(String message) {
        if (stateView != null) stateView.setText(message);
    }

    private void fail(String message) {
        if (finished) return;
        setState(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finishQuietly();
    }

    private void finishQuietly() {
        if (finished) return;
        finished = true;
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String secureNonce() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
