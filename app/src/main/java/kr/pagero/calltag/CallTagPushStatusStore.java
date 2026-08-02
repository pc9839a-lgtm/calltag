package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public final class CallTagPushStatusStore {
    private static final String PREFS = "calltag_push_status";
    private static final String KEY_REGISTERED = "registered";
    private static final String KEY_REALTIME = "realtime";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_UPDATED_AT = "updated_at";

    private CallTagPushStatusStore() {}

    public static void save(Context context, JSONObject response) {
        JSONObject push = response == null ? null : response.optJSONObject("push");
        if (push == null) {
            save(context, false, false, "실시간 알림 상태를 확인하지 못했습니다.");
            return;
        }
        save(context,
                push.optBoolean("registered", false),
                push.optBoolean("realtime", false),
                push.optString("message", ""));
    }

    public static void save(Context context, boolean registered, boolean realtime, String message) {
        prefs(context).edit()
                .putBoolean(KEY_REGISTERED, registered)
                .putBoolean(KEY_REALTIME, realtime)
                .putString(KEY_MESSAGE, message == null ? "" : message.trim())
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public static Snapshot read(Context context) {
        SharedPreferences value = prefs(context);
        return new Snapshot(
                value.getBoolean(KEY_REGISTERED, false),
                value.getBoolean(KEY_REALTIME, false),
                value.getString(KEY_MESSAGE, defaultMessage()),
                value.getLong(KEY_UPDATED_AT, 0L));
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String defaultMessage() {
        return CallTagFirebaseInitializer.configured()
                ? "실시간 문의 알림 등록이 필요합니다."
                : "Firebase 운영 설정이 필요합니다. 기존 앱 실행·재진입 동기화는 계속 작동합니다.";
    }

    public static final class Snapshot {
        public final boolean registered;
        public final boolean realtime;
        public final String message;
        public final long updatedAt;

        Snapshot(boolean registered, boolean realtime, String message, long updatedAt) {
            this.registered = registered;
            this.realtime = realtime;
            this.message = message == null || message.trim().isEmpty() ? defaultMessage() : message;
            this.updatedAt = updatedAt;
        }
    }
}
