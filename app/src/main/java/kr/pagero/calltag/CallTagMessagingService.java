package kr.pagero.calltag;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public final class CallTagMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "pagero_realtime_leads";
    private static final int NOTIFICATION_ID = 4201;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        CallTagPushManager.registerToken(this, token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        String type = message.getData().get("type");
        if (!"pagero_lead_available".equals(type)) return;
        if (!AuthSessionStore.hasSession(this)) return;

        PageroLeadSyncManager.requestSync(this, true);
        showLeadNotification();
    }

    private void showLeadNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "페이지로 신규 문의",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("페이지로에 새 문의가 접수되면 콜태그 고객정보를 즉시 동기화합니다.");
            manager.createNotificationChannel(channel);
        }

        Intent destination = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                this,
                4201,
                destination,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("페이지로 신규 문의")
                .setContentText("새 문의를 콜태그 고객목록에 반영하고 있습니다.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
