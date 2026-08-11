package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

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

public final class GoogleCredentialLoginActivity extends Activity {
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private boolean started;
    private boolean finished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GoogleAuthFlowStore.clear(this);
        if (savedInstanceState == null) startGoogleCredentialFlow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) networkExecutor.shutdownNow();
    }

    private void startGoogleCredentialFlow() {
        if (started || finished) return;
        started = true;

        final String nonce = secureNonce();
        final GetSignInWithGoogleOption googleOption;
        try {
            googleOption = new GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                    .setNonce(nonce)
                    .build();
        } catch (RuntimeException error) {
            fail("Google 로그인 설정을 확인해주세요.");
            return;
        }

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();
        CredentialManager manager = CredentialManager.create(this);
        manager.getCredentialAsync(
                this,
                request,
                null,
                command -> runOnUiThread(command),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleCredential(result, nonce);
                    }

                    @Override
                    public void onError(GetCredentialException error) {
                        if (error instanceof GetCredentialCancellationException) {
                            finishQuietly();
                            return;
                        }
                        fail("Google 계정을 불러오지 못했습니다. 다시 시도해주세요.");
                    }
                });
    }

    private void handleCredential(GetCredentialResponse result, String nonce) {
        try {
            Credential credential = result.getCredential();
            if (!(credential instanceof CustomCredential)
                    || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                fail("Google 로그인 정보를 확인하지 못했습니다.");
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
            fail("Google 로그인 정보를 처리하지 못했습니다.");
        }
    }

    private void exchangeToken(String idToken, String nonce) {
        networkExecutor.execute(() -> {
            try {
                JSONObject response = AuthApiClient.exchangeGoogleIdToken(idToken, nonce);
                runOnUiThread(() -> completeAuth(response));
            } catch (Exception error) {
                runOnUiThread(() -> fail(errorMessage(error)));
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
            fail("로그인 정보 저장 오류입니다. 다시 시도해주세요.");
        }
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
            if (api.getMessage() != null && !api.getMessage().trim().isEmpty()) {
                return api.getMessage();
            }
        }
        return "Google 로그인을 완료하지 못했습니다. 다시 시도해주세요.";
    }

    private void fail(String message) {
        if (finished) return;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finishQuietly();
    }

    private void finishQuietly() {
        if (finished) return;
        finished = true;
        finish();
    }

    private static String secureNonce() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
