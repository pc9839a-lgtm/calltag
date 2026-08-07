package kr.pagero.calltag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
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
    public void callLogMemoDisplayName_isBoundedAndMemoOnly() {
        assertEquals("홍길동 · 견적 다시 연락",
                CallLogMemoSyncManager.buildDisplayName(
                        "홍길동", "견적 다시 연락", "01012345678"));
        assertEquals("고객 5678 · 메모",
                CallLogMemoSyncManager.buildDisplayName(
                        "이름없는고객", "메모", "01012345678"));
        String longAlias = CallLogMemoSyncManager.buildDisplayName(
                "아주아주긴고객이름테스트입니다", "아주아주긴메모를입력해서길이제한을검증합니다", "01012345678");
        assertTrue("call-log alias must remain compact", longAlias.length() <= 32);
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

    @Test
    public void homeTodayTasks_hidesTomorrowCards() {
        long todayAt = atDayOffset(0, 15, 10);
        long tomorrowAt = atDayOffset(1, 10, 20);
        long todayCustomer = seedCustomer("01054789012");
        long tomorrowCustomer = seedCustomer("01065890123");
        seedTask(todayCustomer, "오늘 회귀 0440", todayAt);
        seedTask(tomorrowCustomer, "내일 회귀 0440", tomorrowAt);

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        try {
            scenario.onActivity(activity -> {
                LinearLayout list = activity.findViewById(R.id.todayTaskList);
                assertNotNull(list);
                assertTrue("today task must stay visible", containsText(list, "오늘 회귀 0440"));
                assertFalse("tomorrow task must not appear in today section",
                        containsText(list, "내일 회귀 0440"));
            });
        } finally {
            scenario.close();
        }
    }

    @Test
    public void postCallPopup_isPartialAndDoesNotDimWholeScreen() {
        Intent intent = postCallIntent(987654321L, "01045678901");

        ActivityScenario<PostCallActivity> scenario = ActivityScenario.launch(intent);
        try {
            scenario.onActivity(activity -> {
                PostCallPopupWindowInstaller.install(activity);
                DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                WindowManager.LayoutParams params = activity.getWindow().getAttributes();

                assertTrue("popup width must be smaller than screen",
                        params.width > 0 && params.width < metrics.widthPixels);
                assertTrue("popup height must stay partial",
                        params.height > 0 && params.height < Math.round(metrics.heightPixels * 0.60f));
                assertEquals("whole-screen dim must be disabled", 0,
                        params.flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                assertEquals(View.GONE, activity.findViewById(R.id.postCallPhone).getVisibility());
                assertEquals(View.GONE, activity.findViewById(R.id.postCallMeta).getVisibility());
                assertNotNull(activity.findViewById(R.id.postCallName));
                assertNotNull(activity.findViewById(R.id.postCallNote));
                assertNotNull(activity.findViewById(R.id.postCallSaveOnly));

                View root = activity.findViewById(R.id.postCallRoot);
                View save = activity.findViewById(R.id.postCallSaveOnly);
                View scroll = activity.findViewById(R.id.postCallFieldsScroll);
                assertTrue("fields must scroll instead of clipping save button", scroll instanceof ScrollView);
                assertEquals("save button must stay fixed outside fields scroll", root, save.getParent());
            });
        } finally {
            scenario.close();
        }
    }

    @Test
    public void postCallNewIntent_marksSameCallVisibleAgain() {
        long callId = System.currentTimeMillis();
        Intent first = postCallIntent(callId, "01076901234");
        ActivityScenario<PostCallActivity> scenario = ActivityScenario.launch(first);
        try {
            scenario.onActivity(activity -> {
                Intent retry = postCallIntent(callId, "01076901234");
                PostCallLaunchReceipt.arm(activity, retry);
                assertFalse(PostCallLaunchReceipt.wasVisible(activity, callId));
                activity.onNewIntent(retry);
                assertTrue("same-instance reentry must acknowledge visibility",
                        PostCallLaunchReceipt.wasVisible(activity, callId));
            });
        } finally {
            scenario.close();
        }
    }

    @Test
    public void postCallLauncher_fromApplicationContext_reachesPopup() {
        long callId = System.currentTimeMillis() + 100_000L;
        Intent intent = postCallIntent(callId, "01087012345");
        assertTrue("background-style launcher request must be accepted",
                PostCallActivityLauncher.launch(app, intent));
        assertResumed(PostCallActivity.class);
        assertTrue("launcher must receive a visible receipt",
                PostCallLaunchReceipt.wasVisible(app, callId));
        finishResumed(PostCallActivity.class);
    }

    private Intent postCallIntent(long callId, String phone) {
        long now = System.currentTimeMillis();
        return new Intent(app, PostCallActivity.class)
                .putExtra(PostCallActivity.EXTRA_CALL_LOG_ID, callId)
                .putExtra(PostCallActivity.EXTRA_PHONE, phone)
                .putExtra(PostCallActivity.EXTRA_CACHED_NAME, "부분 팝업 테스트")
                .putExtra(PostCallActivity.EXTRA_STARTED_AT, now - 10_000L)
                .putExtra(PostCallActivity.EXTRA_ENDED_AT, now)
                .putExtra(PostCallActivity.EXTRA_DURATION_SEC, 10L);
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

    private void seedTask(long customerId, String title, long dueAt) {
        CallTagDbHelper db = new CallTagDbHelper(app);
        try {
            db.insertFollowUpTask(customerId, 0L, TaskTypeStore.TYPE_CALL, title, dueAt);
        } finally {
            db.close();
        }
    }

    private long atDayOffset(int days, int hour, int minute) {
        Calendar value = Calendar.getInstance();
        value.add(Calendar.DAY_OF_MONTH, days);
        value.set(Calendar.HOUR_OF_DAY, hour);
        value.set(Calendar.MINUTE, minute);
        value.set(Calendar.SECOND, 0);
        value.set(Calendar.MILLISECOND, 0);
        return value.getTimeInMillis();
    }

    private boolean containsText(View view, String target) {
        if (view instanceof TextView) {
            CharSequence value = ((TextView) view).getText();
            if (value != null && value.toString().contains(target)) return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                if (containsText(group.getChildAt(index), target)) return true;
            }
        }
        return false;
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
