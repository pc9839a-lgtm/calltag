package kr.pagero.calltag;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

public final class CallMonitorService extends Service {
    public static final String ACTION_START = "kr.pagero.calltag.START_MONITOR";
    public static final String ACTION_STOP = "kr.pagero.calltag.STOP_MONITOR";

    private static final String MONITOR_CHANNEL = "calltag_monitor";
    private static final int MONITOR_NOTIFICATION_ID = 4101;
    private static final long[] LOOKUP_DELAYS = {
            800L, 1_200L, 2_000L, 3_500L, 5_500L, 8_000L, 12_000L, 16_000L
    };
    private static final long POST_CALL_SETTLE_DELAY_MS = 1_800L;
    private static final long CALL_MATCH_TOLERANCE_MS = 20_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ContentObserver callLogObserver = new ContentObserver(handler) {
        @Override
        public void onChange(boolean selfChange) {
            if (!lookupRunning) return;
            int generation = lookupGeneration;
            handler.postDelayed(() -> lookupImmediately(generation), 250L);
        }
    };

    private TelephonyManager telephonyManager;
    private TelephonyCallback callback31;
    private PhoneStateListener legacyListener;
    private boolean sawCall;
    private boolean lookupRunning;
    private boolean listenerRegistered;
    private boolean observerRegistered;
    private long callEventStartedAt;
    private int lookupGeneration;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        MessageAutomationStore.ensureDefaults(this);
        if (!canMonitor()) {
            SettingsStore.setMonitorEnabled(this, false);
            stopSelf();
            return;
        }
        startForegroundSafely();
        registerCallListener();
        registerCallLogObserver();
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
        if (!observerRegistered) registerCallLogObserver();
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
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
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

    private void registerCallLogObserver() {
        if (observerRegistered || !hasRequiredPermissions()) return;
        try {
            getContentResolver().registerContentObserver(
                    CallLog.Calls.CONTENT_URI, true, callLogObserver);
            observerRegistered = true;
        } catch (RuntimeException ignored) {
            observerRegistered = false;
        }
    }

    private void handleCallState(int state) {
        if (state == TelephonyManager.CALL_STATE_RINGING
                || state == TelephonyManager.CALL_STATE_OFFHOOK) {
            if (!sawCall) callEventStartedAt = System.currentTimeMillis();
            sawCall = true;
            return;
        }

        if (state == TelephonyManager.CALL_STATE_IDLE) {
            CallerOverlayManager.hide(this);
            CallerOverlayCallStateWatcher.stop(this);

            if (sawCall && !lookupRunning) {
                lookupRunning = true;
                lookupGeneration++;
                lookupCall(0, lookupGeneration);
            }
        }
    }

    private void lookupCall(int attempt, int generation) {
        if (!lookupRunning || generation != lookupGeneration) return;
        if (attempt < 0 || attempt >= LOOKUP_DELAYS.length) {
            resetLookupState(generation);
            return;
        }
        handler.postDelayed(() -> {
            if (!lookupRunning || generation != lookupGeneration) return;
            if (!canMonitor()) {
                SettingsStore.setMonitorEnabled(this, false);
                resetLookupState(generation);
                stopSelf();
                return;
            }

            CallRecord record = findCurrentCallRecord();
            if (!matchesCurrentCall(record)) {
                if (attempt + 1 < LOOKUP_DELAYS.length) {
                    lookupCall(attempt + 1, generation);
                } else {
                    resetLookupState(generation);
                }
                return;
            }
            completeLookup(record, generation);
        }, LOOKUP_DELAYS[attempt]);
    }

    private void lookupImmediately(int generation) {
        if (!lookupRunning || generation != lookupGeneration || !canMonitor()) return;
        CallRecord record = findCurrentCallRecord();
        if (matchesCurrentCall(record)) completeLookup(record, generation);
    }

    private CallRecord findCurrentCallRecord() {
        return CallLogRepository.findLatest(this, callEventStartedAt - 120_000L);
    }

    private boolean matchesCurrentCall(CallRecord record) {
        if (record == null) return false;
        if (record.id == SettingsStore.lastCallId(this)) return false;
        long earliest = callEventStartedAt - CALL_MATCH_TOLERANCE_MS;
        long recordEndedAt = record.startedAt + Math.max(0L, record.durationSec) * 1000L;
        return record.startedAt >= earliest || recordEndedAt >= earliest;
    }

    private void completeLookup(CallRecord record, int generation) {
        if (!lookupRunning || generation != lookupGeneration || record == null) return;
        SettingsStore.setLastCallId(this, record.id);
        onCallResolved(record);
        resetLookupState(generation);
    }

    private void resetLookupState(int generation) {
        if (generation != lookupGeneration) return;
        sawCall = false;
        lookupRunning = false;
        callEventStartedAt = 0L;
        lookupGeneration++;
    }

    private void resetLookupState() {
        sawCall = false;
        lookupRunning = false;
        callEventStartedAt = 0L;
        lookupGeneration++;
    }

    private void onCallResolved(CallRecord record) {
        if (record == null || PhoneNumberNormalizer.normalize(record.phone).length() < 8) return;

        boolean phoneAccess = FeatureEntitlementStore.hasPhoneAccess(this);
        boolean messageAccess = FeatureEntitlementStore.hasMessageAccess(this);
        if (!phoneAccess && !messageAccess) return;

        CallTagDbHelper db = new CallTagDbHelper(this);
        PendingCallStore pendingStore = phoneAccess ? new PendingCallStore(this) : null;
        try {
            if (db.isExcluded(record.phone)) return;

            boolean deferred = needsDeferredHandling(record);
            boolean connected = !deferred && record.durationSec > 0L;
            if (phoneAccess && pendingStore != null) {
                if (deferred) {
                    pendingStore.upsert(record);
                    sendPendingChanged();
                } else if (connected) {
                    pendingStore.markUnansweredHandledByPhone(record.phone, record.startedAt + 1L);
                    TaskAutomation.completeNextCallTask(this, record.phone);
                    sendPendingChanged();
                }
            }

            Customer customer = db.findByPhone(record.phone);
            if (messageAccess) {
                MessageAutomationManager.onCallResolved(this, record, customer);
            }

            if (phoneAccess) {
                Intent review = new Intent(this, PostCallActivity.class)
                        .putExtra(PostCallActivity.EXTRA_PENDING_CALL_ID, deferred ? record.id : -1L)
                        .putExtra(PostCallActivity.EXTRA_CALL_LOG_ID, record.id)
                        .putExtra(PostCallActivity.EXTRA_PHONE, record.phone)
                        .putExtra(PostCallActivity.EXTRA_CACHED_NAME, record.cachedName)
                        .putExtra(PostCallActivity.EXTRA_CALL_TYPE, record.type)
                        .putExtra(PostCallActivity.EXTRA_STARTED_AT, record.startedAt)
                        .putExtra(PostCallActivity.EXTRA_ENDED_AT,
                                Math.max(record.endedAt(), System.currentTimeMillis()))
                        .putExtra(PostCallActivity.EXTRA_DURATION_SEC,
                                Math.max(0L, record.durationSec))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

                String memo = customer == null
                        ? "" : CustomerInsightResolver.latestMemo(db, customer);
                openPostCallAfterPhoneUiSettles(record, customer, review, memo);
            }
        } finally {
            if (pendingStore != null) pendingStore.close();
            db.close();
        }
    }

    private void openPostCallAfterPhoneUiSettles(CallRecord record, Customer customer,
                                                  Intent review, String memo) {
        handler.postDelayed(() -> {
            boolean launched = PostCallActivityLauncher.launch(this, review);
            if (!launched) {
                CallPopupNotificationManager.showPostCall(
                        this, record, customer, review, memo);
            }
        }, POST_CALL_SETTLE_DELAY_MS);
    }

    private boolean needsDeferredHandling(CallRecord record) {
        return record.type == CallLog.Calls.MISSED_TYPE
                || record.type == CallLog.Calls.REJECTED_TYPE
                || (record.type == CallLog.Calls.OUTGOING_TYPE && record.durationSec == 0L);
    }

    private void sendPendingChanged() {
        sendBroadcast(new Intent(PendingCallSectionView.ACTION_CHANGED)
                .setPackage(getPackageName()));
    }

    private void createChannels() {
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel monitor = new NotificationChannel(
                MONITOR_CHANNEL, "통화·문자 자동화", NotificationManager.IMPORTANCE_LOW);
        monitor.setDescription("콜태그가 통화 종료와 자동문자 규칙을 처리하는 동안 표시됩니다.");
        manager.createNotificationChannel(monitor);
        CallPopupNotificationManager.ensureChannels(this);
    }

    private Notification monitorNotification() {
        Intent open = new Intent(this, AuthGateActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, MONITOR_CHANNEL)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("콜태그 실행 중")
                .setContentText("통화와 고객관리·문자 자동화를 연결합니다.")
                .setContentIntent(pending)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        CallerOverlayCallStateWatcher.stop(this);
        if (observerRegistered) {
            try {
                getContentResolver().unregisterContentObserver(callLogObserver);
            } catch (RuntimeException ignored) {
                // Observer may already be gone during process shutdown.
            }
            observerRegistered = false;
        }
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

    private final class CallStateCallback extends TelephonyCallback
            implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            handleCallState(state);
        }
    }
}
