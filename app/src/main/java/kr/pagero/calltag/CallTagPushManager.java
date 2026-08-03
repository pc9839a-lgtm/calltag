package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CallTagPushManager {
    private static final String PREFS = "calltag_push_device";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "calltag-push-registration");
        thread.setDaemon(true);
        return thread;
    });

    private CallTagPushManager() {}

    public static void registerIfAvailable(Context context) {
        Context app = context.getApplicationContext();
        if (!AuthSessionStore.hasSession(app)) return;
        if (!CallTagFirebaseInitializer.ensureInitialized(app)) {
            CallTagPushStatusStore.save(app, false, false,
                    "실시간 문의 알림을 준비하고 있습니다. 앱을 열면 문의를 자동으로 확인합니다.");
            return;
        }
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null || task.getResult().trim().isEmpty()) {
                    CallTagPushStatusStore.save(app, false, false,
                            "실시간 문의 알림을 준비하지 못했습니다. 앱을 열면 문의를 자동으로 확인합니다.");
                    return;
                }
                registerToken(app, task.getResult());
            });
        } catch (RuntimeException error) {
            CallTagPushStatusStore.save(app, false, false,
                    "실시간 문의 알림을 준비하지 못했습니다. 앱을 열면 문의를 자동으로 확인합니다.");
        }
    }

    public static void registerToken(Context context, String token) {
        Context app = context.getApplicationContext();
        String session = AuthSessionStore.session(app);
        if (session.isEmpty() || token == null || token.trim().isEmpty()) return;
        EXECUTOR.execute(() -> {
            try {
                JSONObject response = CallTagPushApiClient.register(
                        session,
                        deviceId(app),
                        token.trim(),
                        BuildConfig.VERSION_NAME);
                CallTagPushStatusStore.save(app, response);
            } catch (Exception error) {
                CallTagPushStatusStore.save(app, false, false,
                        "실시간 문의 알림 연결을 확인하지 못했습니다. 앱을 열면 문의를 자동으로 확인합니다.");
            }
        });
    }

    public static void refreshStatus(Context context) {
        Context app = context.getApplicationContext();
        String session = AuthSessionStore.session(app);
        if (session.isEmpty()) return;
        EXECUTOR.execute(() -> {
            try {
                CallTagPushStatusStore.save(app,
                        CallTagPushApiClient.status(session, deviceId(app)));
            } catch (Exception error) {
                if (!CallTagFirebaseInitializer.configured()) {
                    CallTagPushStatusStore.save(app, false, false,
                            "실시간 문의 알림을 준비하고 있습니다. 앱을 열면 문의를 자동으로 확인합니다.");
                }
            }
        });
    }

    public static void unregisterBestEffort(Context context, String session) {
        Context app = context.getApplicationContext();
        String safeSession = session == null ? "" : session.trim();
        if (safeSession.isEmpty()) {
            CallTagPushStatusStore.clear(app);
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                CallTagPushApiClient.unregister(safeSession, deviceId(app));
            } catch (Exception ignored) {
            } finally {
                CallTagPushStatusStore.clear(app);
            }
        });
    }

    public static String deviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString(KEY_DEVICE_ID, "");
        if (value != null && !value.trim().isEmpty()) return value.trim();
        String created = UUID.randomUUID().toString();
        prefs.edit().putString(KEY_DEVICE_ID, created).commit();
        return created;
    }
}
