package kr.pagero.calltag;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public final class CallTagApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private boolean routingToSetup;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        MessageAutomationStore.ensureDefaults(this);

        // v0.40.3: stop contact alias synchronization permanently and remove rows created
        // by the previous implementation. No new contact is created or renamed.
        SettingsStore.setContactNameSyncEnabled(this, false);
        ContactNameSyncManager.disableAndRestore(this);

        new Thread(() -> {
            MessageRecoveryManager.recoverNow(this,
                    MessageRecoveryManager.TRIGGER_APP_START);
            DataIntegrityManager.recoverNow(this,
                    DataIntegrityManager.TRIGGER_APP_START);
        }, "calltag-startup-recovery").start();
        if (AuthSessionStore.hasSession(this)) {
            PageroLeadSyncManager.requestSync(this, true);
            PageroAccountConnectionManager.refresh(this, false);
            CallTagPushManager.registerIfAvailable(this);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof PostCallActivity) {
            PostCallPopupWindowInstaller.install(activity);
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
        if (SetupRequirements.isReady(activity)) return;

        routingToSetup = true;
        activity.startActivity(SetupRequirements.requiredSetupIntent(activity));
    }

    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}

    private boolean isProtectedActivity(Activity activity) {
        return activity instanceof MainActivity
                || activity instanceof CustomerAddActivity
                || activity instanceof CustomerDetailActivity
                || activity instanceof CustomerMessagePickerActivity
                || activity instanceof PostCallActivity
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
    @Override public void onActivityDestroyed(Activity activity) {
        MainExitGuard.uninstall(activity);
    }
}
