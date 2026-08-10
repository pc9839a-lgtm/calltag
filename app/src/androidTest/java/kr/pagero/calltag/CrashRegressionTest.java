package kr.pagero.calltag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.provider.CallLog;
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
    public void restrictedWriteCallLog_isNotDeclared() throws Exception {
        PackageInfo info = app.getPackageManager().getPackageInfo(
                app.getPackageName(), PackageManager.GET_PERMISSIONS);
        String[] requested = info.requestedPermissions == null
                ? new String[0] : info.requestedPermissions;
        for (String permission : requested) {
            assertFalse("WRITE_CALL_LOG must not ship in the Play build",
                    Manifest.permission.WRITE_CALL_LOG.equals(permission));
        }
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
        seedTask(todayCustomer, "오늘 회귀 0442", todayAt);
        seedTask(tomorrowCustomer, "내일 회귀 0442", tomorrowAt);

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        try {
            scenario.onActivity(activity -> {
                LinearLayout list = activity.findViewById(R.id.todayTaskList);
                assertNotNull(list);
                assertTrue("today task must stay visible", containsText(list, "오늘 회귀 0442"));
                assertFalse("tomorrow task must not appear in today section",
                        containsText(list, "내일 회귀 0442"));
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
    public void postCallLauncher_fromApplicationContext_defersToNotification() {
        long callId = System.currentTimeMillis() + 100_000L;
        String phone = "01087012345";
        PostCallExclusionStore.remove(app, phone);
        Intent intent = postCallIntent(callId, phone);

        assertFalse("background launcher must defer to the compact notification",
                PostCallActivityLauncher.launch(app, intent));
        SystemClock.sleep(150L);
        Activity resumed = resumedActivity();
        assertFalse("post-call Activity must not auto-open from the background",
                resumed instanceof PostCallActivity);
        assertFalse("notification-only routing must not forge an Activity visibility receipt",
                PostCallLaunchReceipt.wasVisible(app, callId));
    }

    @Test
    public void postCallExclusion_blocksBackgroundDeliveryAndClearsRecovery() {
        PostCallRecoveryStore.clearForTests(app);
        String phone = "01087012346";
        long callId = System.currentTimeMillis() + 150_000L;
        CallRecord record = new CallRecord(
                callId, phone, "제외 테스트", CallLog.Calls.INCOMING_TYPE,
                System.currentTimeMillis() - 15_000L, 10L);
        PostCallExclusionStore.add(app, "제외 테스트", phone);
        try {
            PostCallRecoveryStore.arm(app, record, -1L);
            assertTrue(PostCallRecoveryStore.hasPending(app, callId));
            assertTrue("excluded number must be treated as intentionally delivered",
                    PostCallActivityLauncher.launch(app, postCallIntent(callId, phone)));
            assertFalse("excluded number must not remain in recovery queue",
                    PostCallRecoveryStore.hasPending(app, callId));
        } finally {
            PostCallExclusionStore.remove(app, phone);
            PostCallRecoveryStore.clearForTests(app);
        }
    }

    @Test
    public void postCallNotification_blockedChannelKeepsRecoveryPending() {
        PostCallRecoveryStore.clearForTests(app);
        String phone = "01087012347";
        long callId = System.currentTimeMillis() + 175_000L;
        PostCallExclusionStore.remove(app, phone);
        NotificationManager manager = app.getSystemService(NotificationManager.class);
        assertNotNull(manager);
        manager.deleteNotificationChannel(CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
        manager.createNotificationChannel(new NotificationChannel(
                CallPopupNotificationManager.POST_CALL_CHANNEL_ID,
                "회귀 테스트 차단 채널",
                NotificationManager.IMPORTANCE_NONE));

        CallRecord record = new CallRecord(
                callId, phone, "알림 차단 테스트", CallLog.Calls.INCOMING_TYPE,
                System.currentTimeMillis() - 12_000L, 8L);
        PostCallRecoveryStore.arm(app, record, -1L);
        try {
            assertFalse("blocked notification channel must not count as delivered",
                    CallPopupNotificationManager.showPostCall(
                            app, record, null, postCallIntent(callId, phone), ""));
            assertTrue("undeliverable post-call review must remain recoverable",
                    PostCallRecoveryStore.hasPending(app, callId));
        } finally {
            manager.deleteNotificationChannel(CallPopupNotificationManager.POST_CALL_CHANNEL_ID);
            CallPopupNotificationManager.ensureChannels(app);
            PostCallRecoveryStore.clearForTests(app);
        }
    }

    @Test
    public void postCallRecovery_foregroundOpensCompactPopup() {
        PostCallRecoveryStore.clearForTests(app);
        String phone = "01087012348";
        long callId = System.currentTimeMillis() + 190_000L;
        PostCallExclusionStore.remove(app, phone);
        CallRecord record = new CallRecord(
                callId, phone, "전경 복구 테스트", CallLog.Calls.INCOMING_TYPE,
                System.currentTimeMillis() - 20_000L, 15L);
        PostCallRecoveryStore.arm(app, record, -1L);

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        try {
            scenario.onActivity(activity -> assertTrue(
                    PostCallRecoveryStore.recoverLatest(activity, true)));
            assertResumed(PostCallActivity.class);
            assertFalse("foreground popup visibility must clear the recovery queue",
                    PostCallRecoveryStore.hasPending(app, callId));
        } finally {
            finishResumed(PostCallActivity.class);
            PostCallRecoveryStore.clearForTests(app);
            scenario.close();
        }
    }

    @Test
    public void callProcessingLedger_keepsMultipleResolvedCalls() {
        CallProcessingLedger.clearForTests(app);
        long first = System.currentTimeMillis() + 200_000L;
        long second = first + 1L;
        CallProcessingLedger.markResolved(app, first);
        CallProcessingLedger.markResolved(app, second);
        assertTrue(CallProcessingLedger.wasResolved(app, first));
        assertTrue(CallProcessingLedger.wasResolved(app, second));
    }

    @Test
    public void postCallRecoveryStore_roundTripsUndeliveredCall() {
        PostCallRecoveryStore.clearForTests(app);
        long callId = System.currentTimeMillis() + 300_000L;
        CallRecord record = new CallRecord(
                callId, "01098123456", "복구 테스트",
                CallLog.Calls.INCOMING_TYPE,
                System.currentTimeMillis() - 20_000L,
                12L);
        PostCallRecoveryStore.arm(app, record, -1L);
        assertTrue(PostCallRecoveryStore.hasPending(app, callId));
        assertEquals(1, PostCallRecoveryStore.pendingCount(app));
        PostCallRecoveryStore.markDelivered(app, callId);
        assertFalse(PostCallRecoveryStore.hasPending(app, callId));
        assertEquals(0, PostCallRecoveryStore.pendingCount(app));
    }

    @Test
    public void callInteractionDeduper_preventsDuplicateInteractionRows() {
        long customerId = seedCustomer("01019234567");
        long startedAt = System.currentTimeMillis() + 400_000L;
        long endedAt = startedAt + 11_000L;
        CallTagDbHelper db = new CallTagDbHelper(app);
        try {
            long first = CallInteractionDeduper.insertOnce(
                    db, customerId, "INCOMING_CALL", startedAt, endedAt,
                    11L, "MEMO_SAVED", "중복 방지");
            long second = CallInteractionDeduper.insertOnce(
                    db, customerId, "INCOMING_CALL", startedAt, endedAt,
                    11L, "MEMO_SAVED", "중복 방지");
            assertTrue(first > 0L);
            assertEquals("same call must reuse existing interaction", first, second);
        } finally {
            db.close();
        }
    }

    @Test
    public void callDisposition_separatesConnectedAndUnansweredCalls() {
        long now = System.currentTimeMillis();
        CallRecord missed = new CallRecord(1L, "01011112222", "",
                CallLog.Calls.MISSED_TYPE, now, 0L);
        CallRecord rejected = new CallRecord(2L, "01011112223", "",
                CallLog.Calls.REJECTED_TYPE, now, 0L);
        CallRecord outgoingZero = new CallRecord(3L, "01011112224", "",
                CallLog.Calls.OUTGOING_TYPE, now, 0L);
        CallRecord outgoingConnected = new CallRecord(4L, "01011112225", "",
                CallLog.Calls.OUTGOING_TYPE, now, 15L);
        CallRecord incomingConnected = new CallRecord(5L, "01011112226", "",
                CallLog.Calls.INCOMING_TYPE, now, 15L);

        assertTrue(CallDisposition.needsFollowUp(missed));
        assertTrue(CallDisposition.needsFollowUp(rejected));
        assertTrue(CallDisposition.needsFollowUp(outgoingZero));
        assertFalse(CallDisposition.needsFollowUp(outgoingConnected));
        assertFalse(CallDisposition.needsFollowUp(incomingConnected));
        assertTrue(CallDisposition.isConnected(outgoingConnected));
        assertTrue(CallDisposition.isConnected(incomingConnected));
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
