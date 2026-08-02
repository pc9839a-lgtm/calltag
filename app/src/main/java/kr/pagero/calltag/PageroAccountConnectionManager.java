package kr.pagero.calltag;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PageroAccountConnectionManager {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "calltag-pagero-account-check");
        thread.setDaemon(true);
        return thread;
    });

    private PageroAccountConnectionManager() {}

    public static void refresh(Context context, boolean notifyWhenNotConnected) {
        Context app = context.getApplicationContext();
        String session = AuthSessionStore.session(app);
        if (session.isEmpty()) return;
        EXECUTOR.execute(() -> {
            try {
                JSONObject response = AuthApiClient.pageroConnection(session);
                PageroAccountStatusStore.save(app, response);
                PageroAccountStatusStore.Snapshot status = PageroAccountStatusStore.read(app);
                if (notifyWhenNotConnected && !status.connected()) {
                    showToast(app, status.message);
                }
            } catch (Exception error) {
                String message = "페이지로 계정 연결 여부를 확인하지 못했습니다. 콜태그는 계속 사용할 수 있으며 더보기 > 페이지로 연결에서 나중에 확인할 수 있습니다.";
                PageroAccountStatusStore.saveUnknown(app, message);
                if (notifyWhenNotConnected) showToast(app, message);
            }
        });
    }

    private static void showToast(Context context, String message) {
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
