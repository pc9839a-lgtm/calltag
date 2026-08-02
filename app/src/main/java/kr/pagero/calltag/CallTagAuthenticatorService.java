package kr.pagero.calltag;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public final class CallTagAuthenticatorService extends Service {
    private CallTagAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        authenticator = new CallTagAccountAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}
