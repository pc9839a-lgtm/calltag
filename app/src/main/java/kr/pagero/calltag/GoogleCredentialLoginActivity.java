package kr.pagero.calltag;

import android.app.Activity;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Google sign-in host. Explicit Google login never falls back to a browser. */
public final class GoogleCredentialLoginActivity extends Activity {
    private static final String TAG = "CallTagGoogleLogin";
    private static final long PROVIDER_TIMEOUT_MS = 12_000L;
    private static final long EXCHANGE_TIMEOUT_MS = 15_000L;

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CancellationSignal cancellationSignal;
    private TextView stateView;
    private TextView retryButton;
    private boolean started;
    private boolean finished;
    private boolean tokenExchangeStarted;
    private boolean secondaryNativeAttempted;
    private String activeNonce = "";
    private String activeServerClientId = "";

    private final Runnable providerTimeout = () -> {
        if (finished || tokenExchangeStarted) return;
        Log.e(TAG, "Credential Manager did not return within timeout");
        if (cancellationSignal != null) cancellationSignal.cancel();
        showRetry("Google 계정 선택이 지연되고 있습니다. 다시 시도해주세요.");
    };

    private final Runnable exchangeTimeout = () -> {
        if (finished || !tokenExchangeStarted) return;
        Log.e(TAG, "Google token exchange timed out");
        showRetry("Google 로그인 확인이 지연되고 있습니다. 다시 시도해주세요.");
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
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
        stateView.setText("Google 계정을 불러오는 중…");
        stateView.setTextColor(getColor(R.color.text_secondary));
        stateView.setTextSize(14f);
        stateView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        stateParams.topMargin = dp(14);
        root.addView(stateView, stateParams);

        retryButton = new TextView(this);
        retryButton.setText("다시 시도");
        retryButton.setTextSize(15f);
        retryButton.setTextColor(getColor(android.R.color.white));
        retryButton.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        retryButton.setGravity(Gravity.CENTER);
        retryButton.setBackgroundResource(R.drawable.bg_primary_button);
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> {
            retryButton.setVisibility(View.GONE);
            startGoogleCredentialFlow();
        });
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        retryParams.topMargin = dp(20);
        root.addView(retryButton, retryParams);
        return root;
    }

    private void startGoogleCredentialFlow() {
        if (started || finished) return;
        started = true;
        tokenExchangeStarted = false;
        secondaryNativeAttempted = false;
        setState("Google 계정을 선택해주세요.");

        activeServerClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID == null
                ? "" : BuildConfig.GOOGLE_SERVER_CLIENT_ID.trim();
        if (activeServerClientId.isEmpty()) {
            showRetry("Google 로그인을 시작하지 못했습니다. 다시 시도해주세요.");
            return;
        }
        activeNonce = secureNonce();
        requestExplicitGoogle();
    }

    private void requestExplicitGoogle() {
        try {
            GetSignInWithGoogleOption googleOption =
                    new GetSignInWithGoogleOption.Builder(activeServerClientId)
                            .setNonce(activeNonce)
                            .build();
            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build();
            requestNativeCredential(request, true);
        } catch (RuntimeException error) {
            Log.e(TAG, "Failed to build explicit Google sign-in option", error);
            requestAllGoogleAccounts();
        }
    }

    /**
     * Second native-only route. This still uses Credential Manager; it never opens a browser.
     * It asks for all Google accounts instead of relying on the explicit SIWG provider path.
     */
    private void requestAllGoogleAccounts() {
        if (finished || secondaryNativeAttempted) {
            showRetry("Google 계정을 확인하지 못했습니다. 다시 시도해주세요.");
            return;
        }
        secondaryNativeAttempted = true;
        setState("Google 계정을 다시 확인하고 있습니다…");
        try {
            GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                    .setServerClientId(activeServerClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .setNonce(activeNonce)
                    .build();
            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build();
            requestNativeCredential(request, false);
        } catch (RuntimeException error) {
            Log.e(TAG, "Failed to build secondary Google credential option", error);
            showRetry("Google 로그인을 시작하지 못했습니다. 다시 시도해주세요.");
        }
    }

    private void requestNativeCredential(GetCredentialRequest request, boolean allowSecondary) {
        CredentialManager manager = CredentialManager.create(this);
        cancellationSignal = new CancellationSignal();
        mainHandler.removeCallbacks(providerTimeout);
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
                        setState("로그인 중…");
                        handleCredential(result, activeNonce);
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
                        if (allowSecondary) {
                            requestAllGoogleAccounts();
                        } else {
                            showRetry("Google 계정을 확인하지 못했습니다. 다시 시도해주세요.");
                        }
                    }
                });
    }

    private void handleCredential(GetCredentialResponse result, String nonce) {
        try {
            Credential credential = result.getCredential();
            if (!(credential instanceof CustomCredential)) {
                Log.e(TAG, "Unexpected credential class: " + credential.getClass().getName());
                showRetry("Google 계정을 확인하지 못했습니다. 다시 시도해주세요.");
                return;
            }
            if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                Log.e(TAG, "Unexpected credential type: " + credential.getType());
                showRetry("Google 계정을 확인하지 못했습니다. 다시 시도해주세요.");
                return;
            }

            GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(
                    ((CustomCredential) credential).getData());
            String idToken = google.getIdToken();
            if (idToken == null || idToken.trim().isEmpty()) {
                showRetry("Google 계정을 확인하지 못했습니다. 다시 시도해주세요.");
                return;
            }
            exchangeToken(idToken, nonce);
        } catch (Exception error) {
            Log.e(TAG, "Failed to parse Google credential", error);
            showRetry("Google 계정을 확인하지 못했습니다. 다시 시도해주세요.");
        }
    }

    private void exchangeToken(String idToken, String nonce) {
        tokenExchangeStarted = true;
        setState("로그인 중…");
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
                    showRetry(errorMessage(error));
                });
            }
        });
    }

    private void completeAuth(JSONObject response) {
        if (finished) return;
        try {
            AuthSessionStore.save(this, response);
            boolean entitlementIncluded = response.optJSONObject("entitlement") != null;
            if (entitlementIncluded) {
                FeatureEntitlementStore.saveServerEntitlement(this, response);
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

            android.content.Intent destination = SetupRequirements.isReady(this)
                    ? new android.content.Intent(this, MainActivity.class)
                    : SetupRequirements.requiredSetupIntent(this);
            finished = true;
            startActivity(destination.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        } catch (Exception error) {
            Log.e(TAG, "Failed to persist Google login", error);
            showRetry("로그인을 완료하지 못했습니다. 다시 시도해주세요.");
        }
    }

    private String errorMessage(Exception error) {
        if (error instanceof AuthApiClient.ApiException) {
            AuthApiClient.ApiException api = (AuthApiClient.ApiException) error;
            if ("GOOGLE_NONCE_MISMATCH".equals(api.code)) {
                return "Google 로그인을 다시 시도해주세요.";
            }
            if ("GOOGLE_JWKS_NETWORK_FAILED".equals(api.code)
                    || "GOOGLE_JWKS_UNAVAILABLE".equals(api.code)) {
                return "Google 로그인 서버 연결이 지연되고 있습니다. 다시 시도해주세요.";
            }
            if ("GOOGLE_ID_TOKEN_AUDIENCE_INVALID".equals(api.code)) {
                return "Google 로그인 연결 정보를 확인하지 못했습니다.";
            }
            String message = error.getMessage();
            if (message != null && !message.trim().isEmpty()) return message;
        }
        return "Google 로그인을 완료하지 못했습니다. 다시 시도해주세요.";
    }

    private void showRetry(String message) {
        if (finished) return;
        mainHandler.removeCallbacks(providerTimeout);
        mainHandler.removeCallbacks(exchangeTimeout);
        if (cancellationSignal != null && !cancellationSignal.isCanceled()) {
            cancellationSignal.cancel();
        }
        started = false;
        tokenExchangeStarted = false;
        activeNonce = "";
        activeServerClientId = "";
        setState(message);
        if (retryButton != null) retryButton.setVisibility(View.VISIBLE);
    }

    private void setState(String message) {
        if (stateView != null) stateView.setText(message);
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
