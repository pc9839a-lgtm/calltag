package kr.pagero.calltag;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public final class CallTagApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final long CONTACT_SYNC_INTERVAL_MS = 5_000L;
    private static final long PAGERO_FALLBACK_SYNC_INTERVAL_MS = 30_000L;
    private static final long PAGERO_REALTIME_SAFETY_INTERVAL_MS = 5L * 60L * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable periodicForegroundWork = new Runnable() {
        @Override
        public void run() {
            if (startedActivities <= 0) return;
            if (FeatureEntitlementStore.hasPhoneAccess(CallTagApplication.this)
                    && SettingsStore.isContactNameSyncEnabled(CallTagApplication.this)) {
                ContactNameSyncManager.requestSyncAll(CallTagApplication.this);
            }
            maybeSyncPageroLeads();
            if (ExternalCalendarSyncStore.isEnabled(CallTagApplication.this)) {
                ExternalCalendarSyncManager.requestSync(
                        CallTagApplication.this, false, null);
            }
            handler.postDelayed(this, CONTACT_SYNC_INTERVAL_MS);
        }
    };

    private boolean routingToSetup;
    private int startedActivities;
    private long lastPageroForegroundSyncAt;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        MessageAutomationStore.ensureDefaults(this);
        MessageTemplateStore.ensureDefaults(this);
        FollowUpRuleStore.ensureMigrated(this);
        PageroLeadNotificationManager.ensureChannel(this);
        CallPopupNotificationManager.ensureChannels(this);
        CallTagSyncWorkScheduler.reconcile(this);

        new Thread(() -> {
            MessageRecoveryManager.recoverNow(this,
                    MessageRecoveryManager.TRIGGER_APP_START);
            DataIntegrityManager.recoverNow(this,
                    DataIntegrityManager.TRIGGER_APP_START);
        }, "calltag-startup-recovery").start();

        if (FeatureEntitlementStore.hasPhoneAccess(this)) {
            ContactNameSyncManager.requestSyncAll(this);
        }
        if (ExternalCalendarSyncStore.isEnabled(this)) {
            ExternalCalendarSyncManager.requestSync(this, false, null);
        }
        if (AuthSessionStore.hasSession(this)) {
            ReferralAutoApplyManager.applyIfNeeded(this);
            EntitlementRefreshManager.request(this, true);
            PageroLeadSyncManager.requestSync(this, true);
            PageroAccountConnectionManager.refresh(this, false);
            CallTagPushManager.registerIfAvailable(this);
            CallTagSyncManager.request(this, false);
            CallTagSyncWorkScheduler.reconcile(this);
            ensureCallMonitoring(this);
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

    private void ensureCallMonitoring(android.content.Context context) {
        if (!AuthSessionStore.hasSession(context)) return;
        if (!SetupRequirements.hasPhoneState(context)
                || !SetupRequirements.hasCallLog(context)) return;
        SetupRequirements.startCallMonitoring(context);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof PostCallActivity) {
            PostCallLaunchReceipt.markVisible(activity);
            PostCallPopupWindowInstaller.install(activity);
            routingToSetup = false;
            return;
        }
        if (activity instanceof MainActivity) {
            MainExitGuard.install(activity);
            if (AuthSessionStore.hasSession(activity)) {
                ensureCallMonitoring(activity);
                ReferralAutoApplyManager.applyIfNeeded(activity);
                EntitlementRefreshManager.request(activity, false);
                PageroAccountConnectionManager.refresh(activity, false);
                CallTagPushManager.registerIfAvailable(activity);
                CallTagPushManager.refreshStatus(activity);
                CallTagSyncManager.request(activity, false);
                CallTagSyncWorkScheduler.reconcile(activity);
                if (ExternalCalendarSyncStore.isEnabled(activity)) {
                    ExternalCalendarSyncManager.requestSync(activity, false, null);
                }
                if (SetupRequirements.isReady(activity)
                        && FeatureEntitlementStore.hasPhoneAccess(activity)
                        && PostCallPopupAccessPromptStore.shouldPrompt(activity)) {
                    PostCallPopupAccessPromptStore.markPrompted(activity);
                    activity.startActivity(new Intent(activity, PostCallPopupAccessActivity.class));
                    return;
                }
                if (EntitlementNoticeActivity.shouldOpen(activity)) {
                    activity.startActivity(new Intent(activity, EntitlementNoticeActivity.class));
                }
            }
        }
        if (activity instanceof ManualMessageActivity) {
            ManualMessageUxEnhancer.enhance((ManualMessageActivity) activity);
        }
        if (activity instanceof CallerIdSetupActivity
                || activity instanceof InitialPermissionActivity
                || activity instanceof AuthGateActivity
                || activity instanceof LoginActivity
                || activity instanceof EntitlementNoticeActivity
                || activity instanceof PostCallPopupAccessActivity) {
            routingToSetup = false;
            return;
        }
        if (!isProtectedActivity(activity) || routingToSetup) return;
        if (!AuthSessionStore.hasSession(activity)) return;

        ensureCallMonitoring(activity);
        PageroLeadSyncManager.requestSync(activity);
        if (SetupRequirements.isReady(activity)) {
            if (FeatureEntitlementStore.hasPhoneAccess(activity)) {
                ContactNameSyncManager.requestSyncAll(activity);
            }
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
        if (startedActivities == 1) {
            handler.removeCallbacks(periodicForegroundWork);
            handler.post(periodicForegroundWork);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (FeatureEntitlementStore.hasPhoneAccess(activity)) {
            ContactNameSyncManager.requestSyncAll(activity);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        startedActivities = Math.max(0, startedActivities - 1);
        if (startedActivities == 0) {
            handler.removeCallbacks(periodicForegroundWork);
            if (FeatureEntitlementStore.hasPhoneAccess(this)) {
                ContactNameSyncManager.requestSyncAll(this);
            }
            if (ExternalCalendarSyncStore.isEnabled(this)) {
                ExternalCalendarSyncManager.requestSync(this, false, null);
            }
            CallTagSyncWorkScheduler.enqueueImmediate(this, "app_background");
        }
    }

    private boolean isProtectedActivity(Activity activity) {
        return activity instanceof MainActivity
                || activity instanceof CustomerAddActivity
                || activity instanceof CustomerDetailActivity
                || activity instanceof CustomerMessagePickerActivity
                || activity instanceof StageSettingsActivity
                || activity instanceof TaskTypeSettingsActivity
                || activity instanceof MessageAutomationSettingsActivity
                || activity instanceof PostCallAutomationActivity
                || activity instanceof FollowUpAutomationActivity
                || activity instanceof FollowUpRuleEditorActivity
                || activity instanceof MessageTemplateLibraryActivity
                || activity instanceof MessageTemplateEditorActivity
                || activity instanceof ManualMessageActivity
                || activity instanceof MessageHistoryActivity
                || activity instanceof MessageGroupActivity
                || activity instanceof GroupCampaignHubActivity
                || activity instanceof MessageSafetyHubActivity
                || activity instanceof CampaignListActivity
                || activity instanceof CalendarSharePickerActivity
                || activity instanceof PageroConnectionActivity
                || activity instanceof PageroSyncActivity
                || activity instanceof PageroUseGuideActivity
                || activity instanceof BillingEntitlementActivity
                || activity instanceof ReferralPartnerActivity
                || activity instanceof ReferralInviteActivity
                || activity instanceof ReferralCodeRegistrationActivity
                || activity instanceof PartnerSettlementActivity
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
    }

    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        MainExitGuard.uninstall(activity);
        PostCallPopupWindowInstaller.uninstall(activity);
    }
}
