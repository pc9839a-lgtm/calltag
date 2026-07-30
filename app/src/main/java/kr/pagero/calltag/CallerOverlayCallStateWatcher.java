package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

public final class CallerOverlayCallStateWatcher {
    private static TelephonyManager telephonyManager;
    private static TelephonyCallback callback;
    private static PhoneStateListener legacyListener;
    private static boolean registered;

    private CallerOverlayCallStateWatcher() {}

    public static synchronized void start(Context context) {
        stop(context);
        Context app = context.getApplicationContext();
        if (app.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) return;
        telephonyManager = (TelephonyManager) app.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                callback = new CallStateCallback(app);
                telephonyManager.registerTelephonyCallback(app.getMainExecutor(), callback);
            } else {
                legacyListener = new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String phoneNumber) {
                        if (state == TelephonyManager.CALL_STATE_IDLE) {
                            CallerOverlayManager.hide(app);
                            stop(app);
                        }
                    }
                };
                telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
            registered = true;
        } catch (RuntimeException ignored) {
            registered = false;
        }
    }

    public static synchronized void stop(Context context) {
        if (telephonyManager == null || !registered) {
            callback = null;
            legacyListener = null;
            telephonyManager = null;
            registered = false;
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && callback != null) {
                telephonyManager.unregisterTelephonyCallback(callback);
            } else if (legacyListener != null) {
                telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_NONE);
            }
        } catch (RuntimeException ignored) {
            // 시스템이 이미 해제했을 수 있다.
        }
        callback = null;
        legacyListener = null;
        telephonyManager = null;
        registered = false;
    }

    private static final class CallStateCallback extends TelephonyCallback
            implements TelephonyCallback.CallStateListener {
        private final Context context;

        CallStateCallback(Context context) {
            this.context = context;
        }

        @Override
        public void onCallStateChanged(int state) {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                CallerOverlayManager.hide(context);
                CallerOverlayCallStateWatcher.stop(context);
            }
        }
    }
}
