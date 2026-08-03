package kr.pagero.calltag;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** 페이지로 문의 푸시 신호를 받으면 즉시 서버 동기화를 실행한다. */
public final class CallTagMessagingService extends FirebaseMessagingService {
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

        // 푸시는 개인정보가 없는 신호만 전달한다. 고객정보를 서버에서 받은 뒤
        // 실제 등록 건수가 있을 때 PageroLeadNotificationManager가 알림을 표시한다.
        PageroLeadSyncManager.requestRealtimeSync(this);
    }
}
