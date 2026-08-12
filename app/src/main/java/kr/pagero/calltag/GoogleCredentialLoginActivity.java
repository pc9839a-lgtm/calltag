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
    private static final long EXCHANGE_TIMEOUT_MS = 20_000L;

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CancellationSignal cancellationSignal;
    private TextView stateView;
    private TextView retryButton;
    private boolean started;
    private boolean finished;
    private boolean tokenExchangeStarted;

    private final Runnable providerTimeout = () -> {
        if (finished || tokenExchangeStarted) return;
        Log.e(TAG, "Credential Manager did not return within timeout");
        if (cancellationSignal != null) cancellationSignal.cancel();
        showRetry("Google 계정 선택창을 열지 못했습니다. 다시 시도해주세요.");
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
        setState("Google 계정을 선택해주세요.");

        String serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID == null
                ? "" : BuildConfig.GOOGLE_SERVER_CLIENT_ID.trim();
        if (serverClientId.isEmpty()) {
            showRetry("Google 로그인을 시작하지 못했습니다. 다시 시도해주세요.");
            return;
        }

        final String nonce = secureNonce();
        final GetSignInWithGoogleOption googleOption;
        try {
            googleOption = new GetSignInWithGoogleOption.Builder(serverClientId)
                    .setNonce(nonce)
                    .build();
        } catch (RuntimeException error) {
            Log.e(TAG, "Failed to build Google sign-in option", error);
            showRetry("Google 로그인을 시작하지 못했습니다. 다시 시도해주세요.");
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
                        setState("로그인 중…");
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
                        showRetry("Google 계정 선택창을 열지 못했습니다. 다시 시도해주세요.");
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
            if (response.optJSONObject("entitlement") != null) {
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
            EntitlementRefreshManager.request(this, true);

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
                return "Google 로그인에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.";
            }
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
