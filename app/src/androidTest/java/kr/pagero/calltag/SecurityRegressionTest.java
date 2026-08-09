package kr.pagero.calltag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.security.NetworkSecurityPolicy;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class SecurityRegressionTest {
    private Context app;

    @Before
    public void prepare() {
        app = ApplicationProvider.getApplicationContext();
        PendingReferralStore.clear(app);
        GoogleAuthFlowStore.clear(app);
    }

    @Test
    public void dangerousContactWritePermission_isNotDeclared() throws Exception {
        PackageInfo info = app.getPackageManager().getPackageInfo(
                app.getPackageName(), PackageManager.GET_PERMISSIONS);
        String[] requested = info.requestedPermissions == null
                ? new String[0] : info.requestedPermissions;
        for (String permission : requested) {
            assertFalse("WRITE_CONTACTS must not ship in code88",
                    Manifest.permission.WRITE_CONTACTS.equals(permission));
            assertFalse("WRITE_CALL_LOG must not ship in the Play build",
                    Manifest.permission.WRITE_CALL_LOG.equals(permission));
        }
    }

    @Test
    public void internalAuthAndBootComponents_areNotExported() throws Exception {
        PackageManager pm = app.getPackageManager();
        ActivityInfo login = pm.getActivityInfo(
                new ComponentName(app, LoginActivity.class), 0);
        ActivityInfo callback = pm.getActivityInfo(
                new ComponentName(app, GoogleAuthCallbackActivity.class), 0);
        android.content.pm.ActivityInfo receiver = pm.getReceiverInfo(
                new ComponentName(app, BootReceiver.class), 0);
        assertFalse("LoginActivity must be internal-only", login.exported);
        assertTrue("OAuth trampoline must remain reachable by the browser", callback.exported);
        assertFalse("BootReceiver must reject third-party broadcasts", receiver.exported);
    }

    @Test
    public void cleartextTraffic_isDeniedByRuntimePolicy() {
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted());
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("pagero.kr"));
    }

    @Test
    public void referralCapture_rejectsExtraAndHttp_butAcceptsTrustedHttpsPath() {
        Intent injected = new Intent().putExtra("referralCode", "ABCD1234");
        assertFalse(PendingReferralStore.capture(app, injected));
        assertEquals("", PendingReferralStore.peek(app));

        Intent cleartext = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://pagero.kr/r/ABCD1234"));
        assertFalse(PendingReferralStore.capture(app, cleartext));
        assertEquals("", PendingReferralStore.peek(app));

        Intent wrongHost = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://evil.example/r/ABCD1234"));
        assertFalse(PendingReferralStore.capture(app, wrongHost));
        assertEquals("", PendingReferralStore.peek(app));

        Intent trusted = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://pagero.kr/r/ABCD1234"));
        assertTrue(PendingReferralStore.capture(app, trusted));
        assertEquals("ABCD1234", PendingReferralStore.peek(app));
    }

    @Test
    public void googleAuthFlowMarker_isOneShot() {
        GoogleAuthFlowStore.begin(app);
        assertTrue(GoogleAuthFlowStore.consumeIfActive(app));
        assertFalse(GoogleAuthFlowStore.consumeIfActive(app));
    }

    @Test
    public void oauthCallbackFilter_doesNotResolveReferralCustomScheme() {
        Intent legacyReferral = new Intent(Intent.ACTION_VIEW,
                Uri.parse("calltag://referral?code=ABCD1234"));
        legacyReferral.setPackage(app.getPackageName());
        ResolveInfo resolved = app.getPackageManager().resolveActivity(legacyReferral, 0);
        assertTrue("legacy referral custom scheme must not resolve",
                resolved == null || !app.getPackageName().equals(resolved.activityInfo.packageName));
    }
}
