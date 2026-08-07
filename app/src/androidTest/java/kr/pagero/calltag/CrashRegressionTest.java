package kr.pagero.calltag;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public final class CrashRegressionTest {
    private Context app;

    @Before
    public void prepareContext() {
        app = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void popupCustomerRoute_recoversWithPhoneFallback() {
        String phone = "01012345678";
        seedCustomer(phone);
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        try {
            scenario.onActivity(activity -> assertTrue(CustomerLaunchRouter.openForEdit(
                    activity, Long.MAX_VALUE, phone, "regression_popup")));
            assertResumed(CustomerQuickEditActivity.class);
            assertTrue(CrashTelemetryStore.snapshot(app).contains("regression_popup|launch_accepted"));
        } finally {
            finishResumed(CustomerQuickEditActivity.class);
            scenario.close();
        }
    }

    @Test
    public void homeTaskButton_opensIsolatedEditorWithoutClosingMain() {
        seedCustomer("01023456789");
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        try {
            scenario.onActivity(activity -> {
                View add = activity.findViewById(R.id.homeTaskAddButton);
                assertNotNull(add);
                assertTrue(add.callOnClick());
            });
            assertResumed(HomeTaskEditorActivity.class);
            assertTrue(CrashTelemetryStore.snapshot(app).contains("home_task_editor|launch_accepted"));
        } finally {
            finishResumed(HomeTaskEditorActivity.class);
            scenario.close();
        }
    }

    @Test
    public void homeCustomerCard_opensDirectQuickEdit() {
        seedCustomer("01034567890");
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        try {
            scenario.onActivity(activity -> {
                MainActivityCardInteractionFix.install(activity);
                LinearLayout list = activity.findViewById(R.id.customerList);
                assertNotNull(list);
                assertTrue("customer card must render", list.getChildCount() > 0);
                assertTrue(list.getChildAt(0).callOnClick());
            });
            assertResumed(CustomerQuickEditActivity.class);
            assertTrue(CrashTelemetryStore.snapshot(app).contains("home_customer_card|launch_accepted"));
        } finally {
            finishResumed(CustomerQuickEditActivity.class);
            scenario.close();
        }
    }

    private long seedCustomer(String phone) {
        CallTagDbHelper db = new CallTagDbHelper(app);
        try {
            Customer existing = db.findByPhone(phone);
            return existing == null ? db.insertNewLead("회귀 테스트", phone) : existing.id;
        } finally {
            db.close();
        }
    }

    private void assertResumed(Class<? extends Activity> expected) {
        long deadline = SystemClock.elapsedRealtime() + 4_000L;
        while (SystemClock.elapsedRealtime() < deadline) {
            Activity resumed = resumedActivity();
            if (expected.isInstance(resumed)) return;
            SystemClock.sleep(80L);
        }
        Activity current = resumedActivity();
        assertTrue("expected resumed " + expected.getSimpleName() + " but was "
                        + (current == null ? "none" : current.getClass().getSimpleName()),
                expected.isInstance(current));
    }

    private Activity resumedActivity() {
        AtomicReference<Activity> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED);
            for (Activity activity : resumed) {
                result.set(activity);
                break;
            }
        });
        return result.get();
    }

    private void finishResumed(Class<? extends Activity> type) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED);
            for (Activity activity : resumed) {
                if (type.isInstance(activity) && !activity.isFinishing()) activity.finish();
            }
        });
        SystemClock.sleep(100L);
    }
}
