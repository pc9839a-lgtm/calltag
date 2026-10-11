#!/usr/bin/env python3
"""Android remote policy contract: disable-only flags, persistent monotonic cache, wired safety gates.
No Android handset or live Cloudflare deployment is exercised.
"""
import json
from pathlib import Path

root=Path("app/src/main/java/kr/pagero/calltag")
def source(name): return (root/name).read_text(encoding="utf-8")
remote=source("CallTagRemotePolicy.java")
flags=["smsSending","automaticSms","mmsCompose","leadImport","cloudSync"]
policy=json.loads(Path("remote-config/calltag-v1.json").read_text(encoding="utf-8"))
assert policy["schemaVersion"] == 1
assert isinstance(policy["revision"],int) and policy["revision"] >= 1
assert set(policy["flags"]) == set(flags)
assert all(type(v) is bool for v in policy["flags"].values())
for k in flags: assert f'"{k}"' in remote
assert 'https://calltag.pagero.kr/api/mobile-config/v1' in remote
assert 'connection.setRequestMethod("GET")' in remote
assert 'X-Inlet-Session' not in remote and "Bearer " not in remote
assert 'MAX_RESPONSE_BYTES = 8192' in remote
assert 'MIN_REFRESH_MS = 60_000L' in remote
assert 'if (revision <= p.getLong("revision", 0L)) return;' in remote
assert 'flags.opt(key) instanceof Boolean' in remote
assert 'editor.putBoolean(PREFIX + key, flags.getBoolean(key));' in remote
assert 'editor.putLong("revision", revision);' in remote
assert 'if (!editor.commit())' in remote
assert 'PREFS = "calltag_remote_safety_policy_v1"' in remote
assert 'return prefs(context).getBoolean(PREFIX + flag, true);' in remote
assert 'if (context == null || !isKnownFlag(flag)) return false;' in remote
assert 'public static boolean allowed(Context context, String flag)' in remote
app=source("CallTagApplication.java")
assert 'CallTagRemotePolicy.refreshAsync(this, true)' in app
assert 'CallTagRemotePolicy.refreshAsync(this, false)' in app
sms=source("SmsSender.java")
assert sms.count('CallTagRemotePolicy.allowed(context, CallTagRemotePolicy.SMS)') >= 2
assert sms.index('if (!CallTagRemotePolicy.allowed(context, CallTagRemotePolicy.SMS)) return;') < sms.index('MessageLogStore store = new MessageLogStore(context);',sms.index('public static void sendExisting'))
auto=source("MessageAutomationManager.java")
assert 'CallTagRemotePolicy.AUTOMATIC_SMS' in auto
assert 'FeatureEntitlementStore.hasMessageAccess(context)' in auto
assert 'MessageAutomationStore.isEnabled(context)' in auto
mms=source("MmsComposer.java")
assert mms.count('CallTagRemotePolicy.MMS') >= 2
assert 'AccountDataScope.isRestoreReviewPending(context)' in mms
pagero=source("PageroLeadSyncManager.java")
universal=source("UniversalLeadSyncManager.java")
assert pagero.count('CallTagRemotePolicy.LEAD_IMPORT') >= 2
assert universal.count('CallTagRemotePolicy.LEAD_IMPORT') >= 3
sync=source("CallTagSyncManager.java")
assert sync.count('CallTagRemotePolicy.CLOUD_SYNC') >= 2
assert 'CallTagSyncPreferenceStore.isEnabled(app)' in sync
assert 'requireSameAccount(context, accountKey, session);' in sync
worker=Path("_worker.js").read_text(encoding="utf-8")
assert "serveMobilePolicy(request, env)" in worker
assert worker.index("serveMobilePolicy(request, env)") < worker.index("handlePageroIntegration(request, env)")
assert 'new Function(' not in remote and 'eval(' not in remote
print("CallTag remote-policy Android gates, cached revision, server routing: PASS")
