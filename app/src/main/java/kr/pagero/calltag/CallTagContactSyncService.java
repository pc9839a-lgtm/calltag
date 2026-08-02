package kr.pagero.calltag;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public final class CallTagContactSyncService extends Service {
    private static final Object LOCK = new Object();
    private static CallTagContactSyncAdapter adapter;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (LOCK) {
            if (adapter == null) adapter = new CallTagContactSyncAdapter(this, true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        synchronized (LOCK) {
            return adapter == null ? null : adapter.getSyncAdapterBinder();
        }
    }
}
