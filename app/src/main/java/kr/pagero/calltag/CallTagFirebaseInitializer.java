package kr.pagero.calltag;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public final class CallTagFirebaseInitializer {
    private CallTagFirebaseInitializer() {}

    public static boolean configured() {
        return !clean(BuildConfig.FIREBASE_APPLICATION_ID).isEmpty()
                && !clean(BuildConfig.FIREBASE_API_KEY).isEmpty()
                && !clean(BuildConfig.FIREBASE_PROJECT_ID).isEmpty()
                && !clean(BuildConfig.FIREBASE_SENDER_ID).isEmpty();
    }

    public static boolean ensureInitialized(Context context) {
        if (!configured()) return false;
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) return true;
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId(clean(BuildConfig.FIREBASE_APPLICATION_ID))
                    .setApiKey(clean(BuildConfig.FIREBASE_API_KEY))
                    .setProjectId(clean(BuildConfig.FIREBASE_PROJECT_ID))
                    .setGcmSenderId(clean(BuildConfig.FIREBASE_SENDER_ID))
                    .build();
            return FirebaseApp.initializeApp(context.getApplicationContext(), options) != null;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
