#!/usr/bin/env python3
"""CallTag 0.44.61 release guard: production API origin, caller-role copy, expiry routing."""
from pathlib import Path
import re

JAVA = Path("app/src/main/java/kr/pagero/calltag")
CLIENTS = (
    "ExternalLeadIntegrationApiClient.java",
    "PageroLeadApiClient.java",
    "CallTagSyncApiClient.java",
    "AuthApiClient.java",
    "CallTagPushApiClient.java",
    "UniversalLeadApiClient.java",
)
for name in CLIENTS:
    source = (JAVA / name).read_text(encoding="utf-8")
    match = re.search(r"private static final String\[\] BASE_URLS\s*=\s*\{([^}]+)\}", source, re.S)
    assert match, f"{name}: missing BASE_URLS"
    actual = [x.strip() for x in match.group(1).split(",") if x.strip()]
    expected = ['PRODUCTION_API_BASE'] if name == "AuthApiClient.java" else ['"https://pagero.kr"']
    assert actual == expected, f"{name}: production must use canonical pagero.kr only ({actual})"
    assert "pages.dev" not in source, f"{name}: legacy pages.dev reference"
auth = (JAVA / "AuthApiClient.java").read_text(encoding="utf-8")
assert 'PRODUCTION_API_BASE = "https://pagero.kr"' in auth

role = (JAVA / "CallerIdSetupActivity.java").read_text(encoding="utf-8")
assert "RoleManager.ROLE_CALL_SCREENING" in role
assert "RoleManager.ROLE_DIALER" not in role
assert "기본 전화 앱은 변경되지 않습니다" in role
assert "지금은 건너뛰기" in role

notice = (JAVA / "EntitlementNoticeActivity.java").read_text(encoding="utf-8")
gate = (JAVA / "AuthGateActivity.java").read_text(encoding="utf-8")
assert 'if ("TRIAL_EXPIRED".equals(code)) return true;' in notice
assert '"TRIAL_EXPIRED".equalsIgnoreCase(value.noticeCode)' in notice
assert 'if (value.phoneSubscribed || value.messageSubscribed) return "";' in notice
assert gate.index("if (EntitlementNoticeActivity.shouldOpen(this))") < gate.index("if (!SetupRequirements.isReady(this))")

# A single restore tap must be honored after Play reconnect even when ProductDetails
# lookup fails; the expired notice must remain navigable until verification succeeds.
billing = (JAVA / "BillingEntitlementActivity.java").read_text(encoding="utf-8")
assert "private boolean restoreRequested;" in billing
assert "restoreRequested = true;" in billing
assert "private void restoreAfterReconnectIfRequested()" in billing
assert billing.count("restoreAfterReconnectIfRequested();") == 2
assert "if (billing != null && billing.isReady())" in billing
assert "if (restoreRequested) {" in billing
assert "setEnabled(restoreButton, !working && !restoreRequested);" in billing
assert "private boolean returnedFromBilling;" in notice
assert "if (!returnedFromBilling || isFinishing() || isDestroyed()) return;" in notice
assert "private void routeToCrmIfEntitled()" in notice
assert "ContextCompat.RECEIVER_NOT_EXPORTED" in notice
assert "KEY_RETURNED_FROM_BILLING" in notice
assert "EXTRA_RETURN_AFTER_VERIFICATION" in notice
assert "startActivity(new Intent(this, BillingEntitlementActivity.class));\n            finish();" not in notice

# Play subscription reconciliation must be account-scoped, not device-global.
# Failed attempts must retry sooner than the six-hour successful-check cadence.
reconcile = (JAVA / "PlaySubscriptionReconcileManager.java").read_text(encoding="utf-8")
assert 'KEY_OWNER_ID = "owner_id"' in reconcile
assert 'RETRY_INTERVAL_MS = 5L * 60L * 1000L' in reconcile
assert 'lastSuccess >= lastAttempt' in reconcile
assert 'prefs.edit().putString(KEY_OWNER_ID, ownerId)' in reconcile
assert reconcile.count("matchesAccount(app, ownerId, session)") >= 3

# Purchase and restore callbacks must not overwrite entitlement after logout/account switch.
play = (JAVA / "PlayBillingManager.java").read_text(encoding="utf-8")
assert 'private boolean matchesAccount(String ownerId, String session)' in play
assert play.count('if (!matchesAccount(ownerId, session)) return;') >= 4
assert play.count('if (!closed) listener.onServerVerified();') == 2

assert play.count('FeatureEntitlementStore.saveServerEntitlement(activity, response);') == 2

# Expired notice must be informed when verification finishes after billing screen closes.
assert play.count("EntitlementNoticeActivity.ACTION_ENTITLEMENT_VERIFIED") == 2
assert 'EXTRA_RETURN_AFTER_VERIFICATION = "return_after_verification"' in billing
assert "lastCheckedAt <= requestedAt" in billing
assert "sameAccount = session.equals(AuthSessionStore.session(this))" in billing
assert "if (isFinishing() || isDestroyed()) return;" in billing

entitlement = (JAVA / "FeatureEntitlementStore.java").read_text(encoding="utf-8")
assert "isTrial() && remainingDays >= 0 && remainingDays <= 1" in entitlement




# Server rotates session tokens on some refreshes; all follow-up entitlement calls use stored new token.
assert "String currentSession = AuthSessionStore.session(this);" in gate
assert "refreshEntitlement(currentSession);" in gate
assert "refreshEntitlement(session);" not in gate

# FCM should never block canonical lead pull while polling Google Forms.
sync = (JAVA / "UniversalLeadSyncManager.java").read_text(encoding="utf-8")
assert "return requestSyncInternal(context, true, true, false);" in sync
assert "SyncResult result = syncNow(appContext, pollGoogleForms);" in sync
assert "SyncResult result = syncNow(appContext, true);" in sync
assert "if (pollGoogleForms) {" in sync
assert sync.count("UniversalLeadNotificationManager.showImported(") >= 2
assert "ExternalLeadIntegrationApiClient.syncGoogleForms(session);" in sync
# A re-login or account change during provider polling must stop local CRM writes and ACK.
assert "String ownerId = AuthSessionStore.ownerId(context);" in sync
assert "private static void assertSameAccount(Context context, String session, String ownerId)" in sync
assert sync.count("assertSameAccount(context, session, ownerId);") >= 5
assert "ownerId.equals(AuthSessionStore.ownerId(context))" in sync


scheduler = (JAVA / "ExternalLeadSyncWorkScheduler.java").read_text(encoding="utf-8")
assert "PERIOD_MINUTES = 15L" in scheduler
assert ".setRequiredNetworkType(NetworkType.CONNECTED)" in scheduler
assert ".setRequiresBatteryNotLow(true)" not in scheduler
assert "ExistingWorkPolicy.KEEP" in scheduler
fcm = (JAVA / "CallTagMessagingService.java").read_text(encoding="utf-8")
assert 'UniversalLeadSyncManager.requestRealtimeSync(this);' in fcm
assert 'ExternalLeadSyncWorkScheduler.enqueueImmediate(this);' in fcm



print("CallTag 0.44.61 release origin / permission / expiry contract: PASS")
