package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/** Minimal exported OAuth trampoline. The actual login UI remains non-exported. */
public final class GoogleAuthCallbackActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent source = getIntent();
        Uri uri = source == null ? null : source.getData();
        if (!isTrustedShape(source, uri) || !GoogleAuthFlowStore.consumeIfActive(this)) {
            GoogleAuthFlowStore.clear(this);
            Toast.makeText(this, "Google 로그인 요청이 만료되었거나 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
            return;
        }
        startActivity(new Intent(this, LoginActivity.class)
                .setData(uri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private boolean isTrustedShape(Intent intent, Uri uri) {
        if (intent == null || uri == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return false;
        if (!"calltag".equalsIgnoreCase(uri.getScheme())
                || !"auth".equalsIgnoreCase(uri.getHost())
                || !"/google".equals(uri.getPath())
                || uri.getPort() != -1
                || uri.getUserInfo() != null
                || uri.getFragment() != null) return false;
        String error = clean(uri.getQueryParameter("error"));
        String message = clean(uri.getQueryParameter("message"));
        String ticket = clean(uri.getQueryParameter("ticket"));
        if (error.length() > 80 || message.length() > 200 || ticket.length() > 256) return false;
        return !error.isEmpty() || !ticket.isEmpty();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
