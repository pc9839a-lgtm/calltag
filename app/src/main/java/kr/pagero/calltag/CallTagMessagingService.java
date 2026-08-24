package kr.pagero.calltag;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** 문의 푸시 신호를 받으면 해당 서버 동기화를 즉시 실행한다. */
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
        if (!"pagero_lead_available".equals(type) && !"lead_available".equals(type)) return;
        if (!AuthSessionStore.hasSession(this)) return;

        // FCM은 개인정보가 없는 동기화 신호만 전달한다. 실제 고객/문의 데이터는
        // 반드시 로그인 세션으로 서버에서 pull한 뒤 로컬 CRM에 반영한다.
        if ("pagero_lead_available".equals(type)) {
            PageroLeadSyncManager.requestRealtimeSync(this);
            return;
        }
        UniversalLeadSyncManager.requestRealtimeSync(this);
    }
}
