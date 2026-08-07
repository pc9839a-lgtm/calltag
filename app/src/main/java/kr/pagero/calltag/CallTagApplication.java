package kr.pagero.calltag;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public final class CallTagApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final long FOREGROUND_TICK_MS = 5_000L;
    private static final long PAGERO_FALLBACK_SYNC_INTERVAL_MS = 30_000L;
    private static final long PAGERO_REALTIME_SAFETY_INTERVAL_MS = 5L * 60L * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable periodicForegroundWork = new Runnable() {
        @Override
        public void run() {
            if (startedActivities <= 0) return;
            maybeSyncPageroLeads();
            handler.postDelayed(this, FOREGROUND_TICK_MS);
        }
    };

    private boolean routingToSetup;
    private int startedActivities;
    private long lastPageroForegroundSyncAt;

    @Override
    public void onCreate() {
        super.onCreate();
        CrashTelemetryStore.install(this);
        registerActivityLifecycleCallbacks(this);
        MessageAutomationStore.ensureDefaults(this);
        PageroLeadNotificationManager.ensureChannel(this);
        CallTagSyncWorkScheduler.reconcile(this);

        // Upgrade migration only: remove CallTag-owned contacts created by older builds when the
        // already-installed app still has legacy WRITE_CONTACTS. v0.44.3 never creates aliases.
        ContactNameSyncManager.disableAndRestore(this);

        new Thread(() -> {
            MessageRecoveryManager.recoverNow(this,
                    MessageRecoveryManager.TRIGGER_APP_START);
            DataIntegrityManager.recoverNow(this,
                    DataIntegrityManager.TRIGGER_APP_START);
        }, "calltag-startup-recovery").start();

        if (AuthSessionStore.hasSession(this)) {
            SetupRequirements.refreshScreeningRoleState(this);
            EntitlementRefreshManager.request(this, true);
            PageroLeadSyncManager.requestSync(this, true);
            PageroAccountConnectionManager.refresh(this, false);
            CallTagPushManager.registerIfAvailable(this);
            CallTagSyncManager.request(this, false);
            CallTagSyncWorkScheduler.reconcile(this);
            if (SetupRequirements.isReady(this)) {
                SetupRequirements.startCallMonitoring(this);
            }
        }
    }

    private void maybeSyncPageroLeads() {
        if (!AuthSessionStore.hasSession(this)) return;
        CallTagPushStatusStore.Snapshot push = CallTagPushStatusStore.read(this);
        long interval = push.realtime
                ? PAGERO_REALTIME_SAFETY_INTERVAL_MS
                : PAGERO_FALLBACK_SYNC_INTERVAL_MS;
        long now = System.currentTimeMillis();
        if (now - lastPageroForegroundSyncAt < interval) return;
        lastPageroForegroundSyncAt = now;
        PageroLeadSyncManager.requestSyncAndNotify(this, false);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        SystemBarInsetsInstaller.install(activity);
        if (activity instanceof PostCallActivity) {
            CrashTelemetryStore.record(activity, "post_call", "visible", "");
            PostCallLaunchReceipt.markVisible(activity);
            PostCallRecoveryStore.markDelivered(activity,
                    activity.getIntent().getLongExtra(PostCallActivity.EXTRA_CALL_LOG_ID, -1L));
            PostCallPopupWindowInstaller.install(activity);
            routingToSetup = false;
            return;
        }
        if (AuthSessionStore.hasSession(activity)) {
            SetupRequirements.refreshScreeningRoleState(activity);
        }
        if (activity instanceof CustomerQuickEditActivity) {
            CrashTelemetryStore.record(activity, "customer_quick_edit", "visible", "");
        } else if (activity instanceof HomeTaskEditorActivity) {
            CrashTelemetryStore.record(activity, "home_task_editor", "visible", "");
        }
        if (activity instanceof MainActivity) {
            MainExitGuard.install(activity);
            MainActivityCardInteractionFix.install((MainActivity) activity);
            if (AuthSessionStore.hasSession(activity)) {
                EntitlementRefreshManager.request(activity, false);
                PageroAccountConnectionManager.refresh(activity, false);
                CallTagPushManager.registerIfAvailable(activity);
                CallTagPushManager.refreshStatus(activity);
                CallTagSyncManager.request(activity, false);
                CallTagSyncWorkScheduler.reconcile(activity);
                if (EntitlementNoticeActivity.shouldOpen(activity)) {
                    activity.startActivity(new Intent(activity, EntitlementNoticeActivity.class));
                }
                // If Android killed CallTag between call end and popup delivery, recover the newest
                // unresolved review as soon as the user returns to the app foreground.
                PostCallRecoveryStore.recoverLatest(activity, true);
            }
            if (HomeTaskRefreshStore.consume(activity)) {
                CrashTelemetryStore.record(activity, "home_today_tasks", "refresh_after_save", "");
                activity.getWindow().getDecorView().post(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) activity.recreate();
                });
                return;
            }
        }
        if (activity instanceof ManualMessageActivity) {
            ManualMessageUxEnhancer.enhance((ManualMessageActivity) activity);
        }
        if (activity instanceof CallerIdSetupActivity
                || activity instanceof InitialPermissionActivity
                || activity instanceof AuthGateActivity
                || activity instanceof LoginActivity
                || activity instanceof EntitlementNoticeActivity) {
            routingToSetup = false;
            return;
        }
        if (!isProtectedActivity(activity) || routingToSetup) return;
        if (!AuthSessionStore.hasSession(activity)) return;

        PageroLeadSyncManager.requestSync(activity);
        if (SetupRequirements.isReady(activity)) {
            SetupRequirements.startCallMonitoring(activity);
            return;
        }

        routingToSetup = true;
        activity.startActivity(SetupRequirements.requiredSetupIntent(activity));
    }

    @Override
    public void onActivityStarted(Activity activity) {
        startedActivities++;
        if (activity instanceof PostCallActivity) {
            PostCallLaunchReceipt.markVisible(activity);
            PostCallPopupWindowInstaller.install(activity);
        }
        if (activity instanceof MainActivity) {
            MainActivityCardInteractionFix.install((MainActivity) activity);
        }
        if (startedActivities == 1) {
            handler.removeCallbacks(periodicForegroundWork);
            handler.post(periodicForegroundWork);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // No system Contacts/CallLog writes are triggered from lifecycle callbacks.
    }

    @Override
    public void onActivityStopped(Activity activity) {
        startedActivities = Math.max(0, startedActivities - 1);
        if (startedActivities == 0) {
            handler.removeCallbacks(periodicForegroundWork);
            CallTagSyncWorkScheduler.enqueueImmediate(this, "app_background");
        }
    }

    private boolean isProtectedActivity(Activity activity) {
        return activity instanceof MainActivity
                || activity instanceof CustomerAddActivity
                || activity instanceof CustomerDetailActivity
                || activity instanceof CustomerQuickEditActivity
                || activity instanceof HomeTaskEditorActivity
                || activity instanceof CustomerMessagePickerActivity
                || activity instanceof StageSettingsActivity
                || activity instanceof TaskTypeSettingsActivity
                || activity instanceof MessageAutomationSettingsActivity
                || activity instanceof MessageTemplateLibraryActivity
                || activity instanceof MessageTemplateEditorActivity
                || activity instanceof ManualMessageActivity
                || activity instanceof MessageHistoryActivity
                || activity instanceof MessageGroupActivity
                || activity instanceof GroupCampaignHubActivity
                || activity instanceof MessageSafetyHubActivity
                || activity instanceof CampaignListActivity
                || activity instanceof PageroConnectionActivity
                || activity instanceof PageroUseGuideActivity
                || activity instanceof PartnerSettlementActivity
                || activity instanceof BillingEntitlementActivity
                || activity instanceof ReferralPartnerActivity
                || activity instanceof CallTagSyncStatusActivity
                || activity instanceof CallTagSyncDevicesActivity
                || activity instanceof AccountActivity
                || activity instanceof BackupRestoreActivity;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        SystemBarInsetsInstaller.install(activity);
        if (activity instanceof PostCallActivity) {
            PostCallLaunchReceipt.markVisible(activity);
            PostCallPopupWindowInstaller.install(activity);
        }
        if (activity instanceof MainActivity) {
            MainActivityCardInteractionFix.install((MainActivity) activity);
        }
    }

    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        MainExitGuard.uninstall(activity);
        PostCallPopupWindowInstaller.uninstall(activity);
        SystemBarInsetsInstaller.uninstall(activity);
        if (activity instanceof MainActivity) {
            MainActivityCardInteractionFix.uninstall((MainActivity) activity);
        }
    }
}
