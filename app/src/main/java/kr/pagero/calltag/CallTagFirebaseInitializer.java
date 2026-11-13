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
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) return true;
            // google-services resources initialize the default app before Application.onCreate.
            if (FirebaseApp.initializeApp(context.getApplicationContext()) != null) return true;
            if (!configured()) return false;
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
