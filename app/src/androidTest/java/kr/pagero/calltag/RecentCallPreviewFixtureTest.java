package kr.pagero.calltag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.provider.CallLog;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class RecentCallPreviewFixtureTest {
    @Test
    public void recentCustomerCallsAreScopedAndLatestNumberWins() throws Exception {
        Context app = ApplicationProvider.getApplicationContext();
        saveOwner(app, "qa-preview-a");
        SetupRequirements.markInitialFlowCompleted(app);
        long now = System.currentTimeMillis();
        RecentCustomerCallStore.remember(app,
                new CallRecord(10, "2025550143", "QA Old", CallLog.Calls.MISSED_TYPE, now - 60_000, 0));
        RecentCustomerCallStore.remember(app,
                new CallRecord(11, "2025550143", "QA Customer", CallLog.Calls.OUTGOING_TYPE, now, 6));
        RecentCustomerCallStore.remember(app,
                new CallRecord(12, "2025550144", "QA Expired", CallLog.Calls.MISSED_TYPE,
                        now - 8L * 24 * 60 * 60 * 1000, 0));
        assertEquals(1, RecentCustomerCallStore.recent(app).size());
        assertEquals("QA Customer", RecentCustomerCallStore.recent(app).get(0).cachedName);
        saveOwner(app, "qa-preview-b");
        assertTrue(RecentCustomerCallStore.recent(app).isEmpty());
        saveOwner(app, "qa-preview-a");
        assertEquals(1, RecentCustomerCallStore.recent(app).size());
    }

    private static void saveOwner(Context app, String owner) throws Exception {
        AuthSessionStore.save(app, new JSONObject()
                .put("session", "offline-test-fixture")
                .put("profile", new JSONObject().put("ownerId", owner)));
    }
}
