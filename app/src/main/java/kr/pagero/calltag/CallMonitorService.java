package kr.pagero.calltag;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import java.util.List;

public final class CallMonitorService extends Service {
    public static final String ACTION_START = "kr.pagero.calltag.START_MONITOR";
    public static final String ACTION_STOP = "kr.pagero.calltag.STOP_MONITOR";

    private static final String MONITOR_CHANNEL = "calltag_monitor";
    private static final String REVIEW_CHANNEL = "calltag_post_call";
    private static final int MONITOR_NOTIFICATION_ID = 4101;
    private static final long[] LOOKUP_DELAYS = {1500L, 3500L, 7000L};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TelephonyManager telephonyManager;
    private TelephonyCallback callback31;
    private PhoneStateListener legacyListener;
    private boolean sawCall;
    private boolean lookupRunning;
    private long callEventStartedAt;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(MONITOR_NOTIFICATION_ID, monitorNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(MONITOR_NOTIFICATION_ID, monitorNotification());
        }
        registerCallListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            SettingsStore.setMonitorEnabled(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        SettingsStore.setMonitorEnabled(this, true);
        return START_STICKY;
    }

    private void registerCallListener() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callback31 = new CallStateCallback();
            telephonyManager.registerTelephonyCallback(getMainExecutor(), callback31);
        } else {
            legacyListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String phoneNumber) {
                    handleCallState(state);
                }
            };
            telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void handleCallState(int state) {
        if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
            if (!sawCall) callEventStartedAt = System.currentTimeMillis();
            sawCall = true;
            return;
        }

        if (state == TelephonyManager.CALL_STATE_IDLE && sawCall && !lookupRunning) {
            lookupRunning = true;
            lookupCall(0);
        }
    }

    private void lookupCall(int attempt) {
        handler.postDelayed(() -> {
            CallRecord record = CallLogRepository.findLatest(this, callEventStartedAt - 120_000L);
            if (record == null && attempt + 1 < LOOKUP_DELAYS.length) {
                lookupCall(attempt + 1);
                return;
            }
            if (record != null && record.id != SettingsStore.lastCallId(this)) {
                SettingsStore.setLastCallId(this, record.id);
                onCallResolved(record);
            }
            sawCall = false;
            lookupRunning = false;
        }, LOOKUP_DELAYS[attempt]);
    }

    private void onCallResolved(CallRecord record) {
        if (record.phone.trim().isEmpty()) return;
        CallTagDbHelper db = new CallTagDbHelper(this);
        if (db.isExcluded(record.phone)) return;
        Customer customer = db.findByPhone(record.phone);

        Intent review = new Intent(this, PostCallActivity.class)
                .putExtra(PostCallActivity.EXTRA_PHONE, record.phone)
                .putExtra(PostCallActivity.EXTRA_CACHED_NAME, record.cachedName)
                .putExtra(PostCallActivity.EXTRA_CALL_TYPE, record.type)
                .putExtra(PostCallActivity.EXTRA_STARTED_AT, record.startedAt)
                .putExtra(PostCallActivity.EXTRA_ENDED_AT, Math.max(record.endedAt(), System.currentTimeMillis()))
                .putExtra(PostCallActivity.EXTRA_DURATION_SEC, record.durationSec)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (isAppInForeground()) {
            startActivity(review);
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) (record.id & 0x7fffffff),
                review,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = customer == null ? "새 번호 통화가 끝났습니다" : customer.displayName + " 고객 통화가 끝났습니다";
        String body = customer == null
                ? record.phone + " · 신규/기존 고객을 분류해주세요."
                : "이전 상태: " + statusLabel(customer.relationStatus) + " · 상담 결과를 기록해주세요.";

        Notification notification = new Notification.Builder(this, REVIEW_CHANNEL)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(5000 + (int) (record.id % 100000), notification);
    }

    private boolean isAppInForeground() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
        if (processes == null) return false;
        String packageName = getPackageName();
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (packageName.equals(process.processName)
                    && process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    private void createChannels() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel monitor = new NotificationChannel(
                MONITOR_CHANNEL, "통화 감지", NotificationManager.IMPORTANCE_LOW);
        monitor.setDescription("콜태그가 통화 종료를 감지하는 동안 표시됩니다.");
        manager.createNotificationChannel(monitor);

        NotificationChannel review = new NotificationChannel(
                REVIEW_CHANNEL, "통화 후 처리", NotificationManager.IMPORTANCE_HIGH);
        review.setDescription("통화가 끝난 뒤 고객 분류와 다음 행동을 알려줍니다.");
        review.enableVibration(true);
        manager.createNotificationChannel(review);
    }

    private Notification monitorNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                this, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, MONITOR_CHANNEL)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("콜태그 실행 중")
                .setContentText("통화가 끝나면 고객 분류 화면을 안내합니다.")
                .setContentIntent(pending)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private String statusLabel(String status) {
        if (CallTagDbHelper.STATUS_EXISTING.equals(status)) return "기존 고객";
        if (CallTagDbHelper.STATUS_CONSULTING.equals(status)) return "상담 중";
        if (CallTagDbHelper.STATUS_VIP.equals(status)) return "VIP";
        if (CallTagDbHelper.STATUS_DORMANT.equals(status)) return "휴면";
        return "신규 고객";
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && callback31 != null) {
                telephonyManager.unregisterTelephonyCallback(callback31);
            } else if (legacyListener != null) {
                telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_NONE);
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class CallStateCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            handleCallState(state);
        }
    }
}
