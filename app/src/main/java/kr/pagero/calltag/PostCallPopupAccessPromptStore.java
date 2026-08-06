package kr.pagero.calltag;

import android.content.Context;

/** 버전 업데이트 후 통화 종료 팝업 권한이 빠진 사용자에게 한 번 안내한다. */
public final class PostCallPopupAccessPromptStore {
    private static final String PREFS = "calltag_post_call_popup_prompt";
    private static final String KEY_VERSION = "prompted_version";

    private PostCallPopupAccessPromptStore() {}

    public static boolean shouldPrompt(Context context) {
        if (CallPopupNotificationManager.canUsePostCallFullScreen(context)) return false;
        String prompted = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_VERSION, "");
        return !BuildConfig.VERSION_NAME.equals(prompted);
    }

    public static void markPrompted(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_VERSION, BuildConfig.VERSION_NAME).apply();
    }
}
