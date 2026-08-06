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
    public static final String ACTION_CUSTOMER_DATA_CHANGED =
            "kr.pagero.calltag.CUSTOMER_DATA_CHANGED";

    private static final String MONITOR_CHANNEL = "calltag_monitor";
    private static final int MONITOR_NOTIFICATION_ID = 4101;
    private static final long[] LOOKUP_DELAYS = {
            500L, 900L, 1_500L, 2_500L, 4_000L, 6_500L, 10_000L, 15_000L
    };
    private static final long POST_CALL_SETTLE_DELAY_MS = 1_200L;
    private static final long CALL_MATCH_TOLERANCE_MS = 45_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ContentObserver callLogObserver = new ContentObserver(handler) {
        @Override
        public void onChange(boolean selfChange) {
            handler.postDelayed(CallMonitorService.this::handleCallLogChanged, 180L);
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
    private int currentCallState = TelephonyManager.CALL_STATE_IDLE;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        MessageAutomationStore.ensureDefaults(this);
        FollowUpRuleStore.ensureMigrated(this);
        if (!canMonitor()) {
            SettingsStore.setMonitorEnabled(this, false);
            stopSelf();
            return;
        }
        startForegroundSafely();
        registerCallListener();
        registerCallLogObserver();
        DiagnosticEventStore.record(this, "통화 감지 시작", 0L,
                "전화 상태와 통화기록 이중 감지 활성화");
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
            DiagnosticEventStore.record(this, "통화 감지 실패", 0L,
                    "포그라운드 서비스 시작 실패");
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
            listenerRegistered = false;
            DiagnosticEventStore.record(this, "전화 상태 감지 실패", 0L,
                    "통화기록 보조 감지는 계속 유지");
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
        currentCallState = state;
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
                beginLookup(callEventStartedAt > 0L
                        ? callEventStartedAt : System.currentTimeMillis());
            }
        }
    }

    /** 제조사 전화 앱이 상태 콜백을 누락해도 통화기록 변경으로 종료 처리를 복구한다. */
    private void handleCallLogChanged() {
        if (!canMonitor()) return;
        if (lookupRunning) {
            int generation = lookupGeneration;
            handler.postDelayed(() -> lookupImmediately(generation), 120L);
            return;
        }
        if (currentCallState != TelephonyManager.CALL_STATE_IDLE) return;
        beginLookup(System.currentTimeMillis());
        DiagnosticEventStore.record(this, "통화기록 보조 감지", 0L,
                "전화 상태 콜백과 독립적으로 최근 종료 통화 확인");
    }

    private void beginLookup(long eventAt) {
        if (lookupRunning) return;
        sawCall = true;
        lookupRunning = true;
        callEventStartedAt = eventAt > 0L ? eventAt : System.currentTimeMillis();
        lookupGeneration++;
        lookupCall(0, lookupGeneration);
    }

    private void lookupCall(int attempt, int generation) {
        if (!lookupRunning || generation != lookupGeneration) return;
        if (attempt < 0 || attempt >= LOOKUP_DELAYS.length) {
            DiagnosticEventStore.record(this, "통화 종료 탐색 실패", 0L,
                    "통화기록이 제한 시간 안에 저장되지 않음");
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
                    DiagnosticEventStore.record(this, "통화 종료 탐색 실패", 0L,
                            "최근 통화기록과 현재 종료 이벤트가 일치하지 않음");
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
        long eventAt = callEventStartedAt > 0L
                ? callEventStartedAt : System.currentTimeMillis();
        return CallLogRepository.findLatestEndingAfter(
                this, eventAt - CALL_MATCH_TOLERANCE_MS);
    }

    private boolean matchesCurrentCall(CallRecord record) {
        if (record == null) return false;
        if (record.id == SettingsStore.lastCallId(this)) return false;
        long earliest = callEventStartedAt - CALL_MATCH_TOLERANCE_MS;
        long resolvedAt = Math.max(record.startedAt, record.endedAt());
        return record.startedAt >= earliest || resolvedAt >= earliest;
    }

    private void completeLookup(CallRecord record, int generation) {
        if (!lookupRunning || generation != lookupGeneration || record == null) return;
        boolean handled = false;
        try {
            handled = onCallResolved(record);
        } catch (RuntimeException error) {
            DiagnosticEventStore.record(this, "통화 종료 처리 오류", record.id,
                    error.getClass().getSimpleName());
        }
        if (handled) {
            SettingsStore.setLastCallId(this, record.id);
            DiagnosticEventStore.record(this, "통화 종료 처리 완료", record.id,
                    "고객등록·팝업·자동화 연결 완료");
        }
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

    private boolean onCallResolved(CallRecord record) {
        if (record == null || PhoneNumberNormalizer.normalize(record.phone).length() < 8) {
            return false;
        }

        boolean phoneAccess = FeatureEntitlementStore.hasPhoneAccess(this);
        boolean messageAccess = FeatureEntitlementStore.hasMessageAccess(this);
        if (!phoneAccess && !messageAccess) {
            DiagnosticEventStore.record(this, "통화 종료 처리 보류", record.id,
                    "이용권 확인 필요");
            return false;
        }

        CallTagDbHelper db = new CallTagDbHelper(this);
        PendingCallStore pendingStore = phoneAccess ? new PendingCallStore(this) : null;
        try {
            if (db.isExcluded(record.phone)) return true;

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
            if (phoneAccess && customer == null) {
                customer = createCustomerFromCall(db, record);
            }
            if (customer != null) {
                sendBroadcast(new Intent(ACTION_CUSTOMER_DATA_CHANGED)
                        .setPackage(getPackageName()));
                ContactNameSyncManager.requestSyncAll(this);
            }

            if (messageAccess) {
                MessageAutomationManager.onCallResolved(this, record, customer);
                if (connected) FollowUpAutomationManager.onConnectedCall(this, record, customer);
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
                                | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                String memo = customer == null
                        ? "" : CustomerInsightResolver.latestMemo(db, customer);
                openPostCallAfterPhoneUiSettles(record, customer, review, memo);
            }
            return true;
        } finally {
            if (pendingStore != null) pendingStore.close();
            db.close();
        }
    }

    private Customer createCustomerFromCall(CallTagDbHelper db, CallRecord record) {
        String name = safeCustomerName(record.cachedName, record.phone);
        try {
            long customerId = db.insertCustomer(
                    name, record.phone, db.firstStage(), "통화 자동등록");
            return db.findCustomerById(customerId);
        } catch (IllegalArgumentException | android.database.SQLException race) {
            return db.findByPhone(record.phone);
        }
    }

    private String safeCustomerName(String cachedName, String phone) {
        String name = cachedName == null ? "" : cachedName.trim();
        if (!name.isEmpty()
                && !"이름없는고객".equals(name)
                && !"알 수 없음".equals(name)
                && !name.equals(phone)) {
            return name;
        }
        String normalized = PhoneNumberNormalizer.normalize(phone);
        String suffix = normalized.length() >= 4
                ? normalized.substring(normalized.length() - 4) : normalized;
        return suffix.isEmpty() ? "새 고객" : "고객 " + suffix;
    }

    private void openPostCallAfterPhoneUiSettles(CallRecord record, Customer customer,
                                                  Intent review, String memo) {
        PostCallLaunchReceipt.arm(this, review);
        boolean notificationShown = CallPopupNotificationManager.showPostCall(
                this, record, customer, review, memo);
        handler.postDelayed(() -> {
            if (PostCallLaunchReceipt.wasVisible(this, record.id)) return;
            boolean launched = PostCallActivityLauncher.launch(this, review);
            if (!launched && !notificationShown) {
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
