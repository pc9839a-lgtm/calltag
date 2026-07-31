package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AuthGateActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_gate);
        status = findViewById(R.id.txtAuthGateStatus);
        checkAccount();
    }

    private void checkAccount() {
        String session = AuthSessionStore.session(this);
        if (session.isEmpty()) {
            openLogin();
            return;
        }
        status.setText("로그인 상태를 확인하고 있습니다.");
        executor.execute(() -> {
            try {
                JSONObject response = AuthApiClient.refresh(session);
                AuthSessionStore.save(this, response);
                runOnUiThread(this::openDestination);
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (AuthSessionStore.hasSession(this)) openDestination();
                    else openLogin();
                });
            }
        });
    }

    private void openDestination() {
        if (!SetupRequirements.isReady(this)) {
            startActivity(SetupRequirements.requiredSetupIntent(this)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
