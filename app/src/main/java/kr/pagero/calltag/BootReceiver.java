package kr.pagero.calltag;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SettingsStore.isMonitorEnabled(context)) return;
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return;

        Intent service = new Intent(context, CallMonitorService.class)
                .setAction(CallMonitorService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (RuntimeException ignored) {
            // The app will restart monitoring the next time the user opens it.
        }
    }
}
