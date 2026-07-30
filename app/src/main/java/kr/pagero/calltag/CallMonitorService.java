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
    private boolean listenerRegistered;
    private long callEventStartedAt;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        if (!canMonitor()) {
            SettingsStore.setMonitorEnabled(this, false);
            stopSelf();
            return;
        }
        startForegroundSafely();
        registerCallListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            SettingsStore.setMonitorEnabled(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!canMonitor()) {
            SettingsStore.setMonitorEnabled(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        SettingsStore.setMonitorEnabled(this, true);
        if (!listenerRegistered) registerCallListener();
        return START_STICKY;
    }

    private void startForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(MONITOR_NOTIFICATION_ID, monitorNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(MONITOR_NOTIFICATION_ID, monitorNotification());
            }
        } catch (RuntimeException e) {
            SettingsStore.setMonitorEnabled(this, false);
            stopSelf();
        }
    }

    private boolean canMonitor() {
        return AuthSessionStore.hasSession(this) && hasRequiredPermissions();
    }

    private boolean hasRequiredPermissions() {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
    }

    private void registerCallListener() {
        if (listenerRegistered || !canMonitor()) return;
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            SettingsStore.setMonitorEnabled(this, false);
            stopSelf();
            return;
        }

        try {
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
            listenerRegistered = true;
        } catch (RuntimeException e) {
            SettingsStore.setMonitorEnabled(this, false);
            stopSelf();
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
        if (attempt < 0 || attempt >= LOOKUP_DELAYS.length) {
            resetLookupState();
            return;
        }
        handler.postDelayed(() -> {
            if (!canMonitor()) {
                SettingsStore.setMonitorEnabled(this, false);
                resetLookupState();
                stopSelf();
                return;
            }

            CallRecord record = CallLogRepository.findLatest(this, callEventStartedAt - 120_000L);
            if (record == null && attempt + 1 < LOOKUP_DELAYS.length) {
                lookupCall(attempt + 1);
                return;
            }
            if (record != null && record.id != SettingsStore.lastCallId(this)) {
                SettingsStore.setLastCallId(this, record.id);
                onCallResolved(record);
            }
            resetLookupState();
        }, LOOKUP_DELAYS[attempt]);
    }

    private void resetLookupState() {
        sawCall = false;
        lookupRunning = false;
        callEventStartedAt = 0L;
    }

    private void onCallResolved(CallRecord record) {
        if (record == null || PhoneNumberNormalizer.normalize(record.phone).length() < 8) return;

        CallTagDbHelper db = new CallTagDbHelper(this);
        PendingCallStore pendingStore = new PendingCallStore(this);
        try {
            if (db.isExcluded(record.phone)) return;

            boolean deferred = needsDeferredHandling(record);
            boolean connected = !deferred && record.durationSec > 0L;
            if (deferred) {
                pendingStore.upsert(record);
                sendPendingChanged();
            } else if (connected) {
                pendingStore.markUnansweredHandledByPhone(record.phone, record.startedAt + 1L);
                TaskAutomation.completeNextCallTask(this, record.phone);
                sendPendingChanged();
            }

            Customer customer = db.findByPhone(record.phone);
            Intent review = new Intent(this, PostCallActivity.class)
                    .putExtra(PostCallActivity.EXTRA_PENDING_CALL_ID, deferred ? record.id : -1L)
                    .putExtra(PostCallActivity.EXTRA_PHONE, record.phone)
                    .putExtra(PostCallActivity.EXTRA_CACHED_NAME, record.cachedName)
                    .putExtra(PostCallActivity.EXTRA_CALL_TYPE, record.type)
                    .putExtra(PostCallActivity.EXTRA_STARTED_AT, record.startedAt)
                    .putExtra(PostCallActivity.EXTRA_ENDED_AT, Math.max(record.endedAt(), System.currentTimeMillis()))
                    .putExtra(PostCallActivity.EXTRA_DURATION_SEC, Math.max(0L, record.durationSec))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            if (deferred) {
                showReviewNotification(record, customer, review, db);
                return;
            }

            if (isAppInForeground()) {
                try {
                    startActivity(review);
                } catch (RuntimeException ignored) {
                    showReviewNotification(record, customer, review, db);
                }
                return;
            }
            showReviewNotification(record, customer, review, db);
        } finally {
            pendingStore.close();
            db.close();
        }
    }

    private boolean needsDeferredHandling(CallRecord record) {
        return record.type == CallLog.Calls.MISSED_TYPE
                || record.type == CallLog.Calls.REJECTED_TYPE
                || (record.type == CallLog.Calls.OUTGOING_TYPE && record.durationSec == 0L);
    }

    private void showReviewNotification(CallRecord record, Customer customer,
                                        Intent review, CallTagDbHelper db) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) (record.id & 0x7fffffff),
                review,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String callLabel = callTypeLabel(record);
        String title;
        String body;
        if (customer == null) {
            title = callLabel + " · " + (record.cachedName.trim().isEmpty() ? record.phone : record.cachedName);
            body = needsDeferredHandling(record)
                    ? "다시 전화하거나 할 일을 등록해주세요."
                    : "통화 결과와 메모를 남겨주세요.";
        } else {
            title = callLabel + " · " + customer.displayName;
            String memo = CustomerInsightResolver.latestMemo(db, customer);
            body = customer.relationStatus;
            if (!memo.isEmpty()) body += "\n최근 메모 · " + memo;
        }

        Notification notification = new Notification.Builder(this, REVIEW_CHANNEL)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(title)
                .setContentText(customer == null ? record.phone : customer.relationStatus)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .build();
        try {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(5000 + (int) (record.id % 100000), notification);
            }
        } catch (RuntimeException ignored) {
            // Notification permission or OEM restrictions can block delivery.
        }
    }

    private String callTypeLabel(CallRecord record) {
        if (record.type == CallLog.Calls.MISSED_TYPE) return "부재중";
        if (record.type == CallLog.Calls.REJECTED_TYPE) return "거절한 전화";
        if (record.type == CallLog.Calls.OUTGOING_TYPE && record.durationSec == 0L) {
            return "발신 · 연결 안 됨";
        }
        if (record.type == CallLog.Calls.OUTGOING_TYPE) return "발신 통화 완료";
        return "수신 통화 완료";
    }

    private void sendPendingChanged() {
        sendBroadcast(new Intent(PendingCallSectionView.ACTION_CHANGED).setPackage(getPackageName()));
    }

    private boolean isAppInForeground() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
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
        if (manager == null) return;
        NotificationChannel monitor = new NotificationChannel(
                MONITOR_CHANNEL, "통화 감지", NotificationManager.IMPORTANCE_LOW);
        monitor.setDescription("콜태그가 통화 종료를 감지하는 동안 표시됩니다.");
        manager.createNotificationChannel(monitor);

        NotificationChannel review = new NotificationChannel(
                REVIEW_CHANNEL, "통화 후 처리", NotificationManager.IMPORTANCE_HIGH);
        review.setDescription("발신·수신·부재중·거절 통화의 다음 행동을 알려줍니다.");
        review.enableVibration(true);
        manager.createNotificationChannel(review);
    }

    private Notification monitorNotification() {
        Intent open = new Intent(this, AuthGateActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                this, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, MONITOR_CHANNEL)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("콜태그 실행 중")
                .setContentText("통화와 고객 할 일을 자동으로 연결합니다.")
                .setContentIntent(pending)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (telephonyManager != null && listenerRegistered) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && callback31 != null) {
                    telephonyManager.unregisterTelephonyCallback(callback31);
                } else if (legacyListener != null) {
                    telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_NONE);
                }
            } catch (RuntimeException ignored) {
                // Listener may already be unregistered by the system.
            }
        }
        listenerRegistered = false;
        resetLookupState();
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
