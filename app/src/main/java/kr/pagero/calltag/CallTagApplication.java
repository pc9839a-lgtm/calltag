package kr.pagero.calltag;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public final class CallTagApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final long CONTACT_SYNC_INTERVAL_MS = 5_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable periodicContactSync = new Runnable() {
        @Override
        public void run() {
            if (startedActivities <= 0) return;
            if (FeatureEntitlementStore.hasPhoneAccess(CallTagApplication.this)
                    && SettingsStore.isContactNameSyncEnabled(CallTagApplication.this)) {
                ContactNameSyncManager.requestSyncAll(CallTagApplication.this);
            }
            handler.postDelayed(this, CONTACT_SYNC_INTERVAL_MS);
        }
    };

    private boolean routingToSetup;
    private int startedActivities;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        MessageAutomationStore.ensureDefaults(this);

        new Thread(() -> {
            MessageRecoveryManager.recoverNow(this,
                    MessageRecoveryManager.TRIGGER_APP_START);
            DataIntegrityManager.recoverNow(this,
                    DataIntegrityManager.TRIGGER_APP_START);
        }, "calltag-startup-recovery").start();

        if (FeatureEntitlementStore.hasPhoneAccess(this)) {
            ContactNameSyncManager.requestSyncAll(this);
        }
        if (AuthSessionStore.hasSession(this)) {
            PageroLeadSyncManager.requestSync(this, true);
            PageroAccountConnectionManager.refresh(this, false);
            CallTagPushManager.registerIfAvailable(this);
            if (SetupRequirements.isReady(this)) {
                SetupRequirements.startCallMonitoring(this);
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof PostCallActivity) {
            PostCallPopupWindowInstaller.install(activity);
            routingToSetup = false;
            return;
        }
        if (activity instanceof MainActivity) {
            MainExitGuard.install(activity);
            if (AuthSessionStore.hasSession(activity)) {
                PageroAccountConnectionManager.refresh(activity, false);
                CallTagPushManager.registerIfAvailable(activity);
                CallTagPushManager.refreshStatus(activity);
            }
        }
        if (activity instanceof ManualMessageActivity) {
            ManualMessageUxEnhancer.enhance((ManualMessageActivity) activity);
        }
        if (activity instanceof CallerIdSetupActivity
                || activity instanceof InitialPermissionActivity
                || activity instanceof AuthGateActivity
                || activity instanceof LoginActivity) {
            routingToSetup = false;
            return;
        }
        if (!isProtectedActivity(activity) || routingToSetup) return;
        if (!AuthSessionStore.hasSession(activity)) return;

        PageroLeadSyncManager.requestSync(activity);
        if (SetupRequirements.isReady(activity)) {
            SetupRequirements.startCallMonitoring(activity);
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
            PostCallPopupWindowInstaller.install(activity);
        }
        if (startedActivities == 1) {
            handler.removeCallbacks(periodicContactSync);
            handler.post(periodicContactSync);
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
            handler.removeCallbacks(periodicContactSync);
            if (FeatureEntitlementStore.hasPhoneAccess(this)) {
                ContactNameSyncManager.requestSyncAll(this);
            }
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
                || activity instanceof MessageTemplateLibraryActivity
                || activity instanceof MessageTemplateEditorActivity
                || activity instanceof ManualMessageActivity
                || activity instanceof MessageHistoryActivity
                || activity instanceof MessageGroupActivity
                || activity instanceof GroupCampaignHubActivity
                || activity instanceof MessageSafetyHubActivity
                || activity instanceof CampaignListActivity
                || activity instanceof PageroConnectionActivity
                || activity instanceof AccountActivity
                || activity instanceof BackupRestoreActivity;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof PostCallActivity) {
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
