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
assert gate.index("if (EntitlementNoticeActivity.shouldOpen(this))") < gate.index("if (!SetupRequirements.isReady(this))")

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
assert "ExternalLeadIntegrationApiClient.syncGoogleForms(session);" in sync

scheduler = (JAVA / "ExternalLeadSyncWorkScheduler.java").read_text(encoding="utf-8")
assert "PERIOD_MINUTES = 15L" in scheduler
assert ".setRequiredNetworkType(NetworkType.CONNECTED)" in scheduler
assert ".setRequiresBatteryNotLow(true)" not in scheduler
assert "ExistingWorkPolicy.KEEP" in scheduler
fcm = (JAVA / "CallTagMessagingService.java").read_text(encoding="utf-8")
assert 'UniversalLeadSyncManager.requestRealtimeSync(this);' in fcm
assert 'ExternalLeadSyncWorkScheduler.enqueueImmediate(this);' in fcm



print("CallTag 0.44.61 release origin / permission / expiry contract: PASS")
