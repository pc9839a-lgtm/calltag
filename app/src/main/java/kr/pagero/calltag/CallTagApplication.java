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
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof CallerIdSetupActivity
                || activity instanceof AuthGateActivity
                || activity instanceof LoginActivity) {
            routingToSetup = false;
            return;
        }
        if (!isProtectedActivity(activity) || routingToSetup) return;
        if (!AuthSessionStore.hasSession(activity)) return;

        SetupRequirements.invalidateTestWhenPrerequisitesMissing(activity);
        if (SetupRequirements.isReady(activity)) return;

        routingToSetup = true;
        activity.startActivity(SetupRequirements.requiredSetupIntent(activity));
    }

    private boolean isProtectedActivity(Activity activity) {
        return activity instanceof MainActivity
                || activity instanceof CustomerAddActivity
                || activity instanceof CustomerDetailActivity
                || activity instanceof PostCallActivity
                || activity instanceof StageSettingsActivity
                || activity instanceof TaskTypeSettingsActivity
                || activity instanceof AccountActivity;
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
