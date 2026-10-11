#!/usr/bin/env python3
"""Source wiring and representative persistence fixture for remote safety minimums.

Not an Android integration/real-SMS device test.
"""
from pathlib import Path
import json
import sqlite3
from tempfile import TemporaryDirectory

p=Path("app/src/main/java/kr/pagero/calltag")
def code(f): return (p/f).read_text(encoding="utf-8")
worker=Path("worker/mobile-policy.mjs").read_text(encoding="utf-8")
policy=code("CallTagRemotePolicy.java")
config=json.loads(Path("remote-config/calltag-v1.json").read_text(encoding="utf-8"))
assert config["revision"] >= 2
assert set(config["automationLimits"])=={"minCooldownHours","minLeadDelayMinutes","minFollowupDays"}
assert "validateAutomationLimits" in worker
assert "Number.isSafeInteger(limits[name])" in worker
for key in ("MIN_COOLDOWN_HOURS","MIN_LEAD_DELAY_MINUTES","MIN_FOLLOWUP_DAYS"):
    assert key in policy
for fragment in (
    'LIMIT_PREFIX + MIN_COOLDOWN_HOURS, cooldown',
    'LIMIT_PREFIX + MIN_LEAD_DELAY_MINUTES, leadDelay',
    'LIMIT_PREFIX + MIN_FOLLOWUP_DAYS, followup',
    'editor.putLong("revision", revision)',
    'if (!editor.commit())',
    'if (revision <= p.getLong("revision", 0L)) return;',
    'if (cooldown < 0 || leadDelay < 0 || followup < 0) return;',
    "checkedMinimum(limits, MIN_COOLDOWN_HOURS, 720)",
    "checkedMinimum(limits, MIN_LEAD_DELAY_MINUTES, 1440)",
    "checkedMinimum(limits, MIN_FOLLOWUP_DAYS, 30)",
):
    assert fragment in policy,fragment

assert 'Math.max(MessageAutomationStore.cooldownHours(appContext),' in code("MessageLogStore.java")
assert 'CallTagRemotePolicy.minimumCooldownHours(appContext)' in code("MessageLogStore.java")
assert 'Math.max(MessageAutomationStore.delayDays(context),' in code("MessageAutomationManager.java")
assert 'CallTagRemotePolicy.minimumFollowupDays(context)' in code("MessageAutomationManager.java")
assert 'Math.max(PageroLeadMessageSettings.delayMinutes(context),' in code("PageroLeadMessageAutomation.java")
assert 'CallTagRemotePolicy.minimumLeadDelayMinutes(context)' in code("PageroLeadMessageAutomation.java")
assert 'AccountDataScope.preferenceName(context, PREFS)' in code("PageroLeadMessageSettings.java")
assert '"calltag_pagero_lead_message"' in code("AccountDataScope.java")
assert 'if (preference.equals(AccountDataScope.preferenceName(' in code("CallTagBackupManager.java")

limits=code("CallTagRemoteAutomationLimits.java")
assert "MessageAutomationManager.TRIGGER_DELAYED.equals(record.triggerType)" in limits
assert "PageroLeadMessageAutomation.TRIGGER_PAGERO_LEAD_RECEIVED" in limits
assert "return 0L;" in limits # Do not touch manual/campaign messages
receiver=code("ScheduledMessageReceiver.java")
sender=code("SmsSender.java")
store=code("MessageLogStore.java")
assert 'CallTagRemoteAutomationLimits.earliestAutomatedSendAt(' in receiver
assert 'MessageScheduler.schedule(context, messageId, remoteMinimumAt)' in receiver
assert 'CallTagRemoteAutomationLimits.earliestAutomatedSendAt(' in sender
assert 'store.deferReadyForRemoteMinimum(messageId, earliest)' in sender
assert 'id=? AND status=?' in store
assert 'changes.put("status", STATUS_SCHEDULED)' in store
settings=code("MessageAutomationSettingsActivity.java")
assert 'serverMinimumNotice()' in settings
assert '더 긴 조건 우선' in settings

# Simulate the atomic state transition: after a server policy increase,
# a READY automatic job becomes SCHEDULED, not SENT, and no duplicate is created.
with TemporaryDirectory() as tmp:
    db=sqlite3.connect(f"{tmp}/message_jobs.db")
    db.execute("CREATE TABLE message_jobs (id INTEGER PRIMARY KEY, status TEXT, scheduled_at INTEGER, error TEXT)")
    db.execute("INSERT INTO message_jobs VALUES(1,'READY',1000,'')")
    db.execute("INSERT INTO message_jobs VALUES(2,'SENT',1000,'')")
    n=db.execute("UPDATE message_jobs SET status='SCHEDULED',scheduled_at=?,error=? WHERE id=? AND status=?",
                 (4600,"remote minimum",1,"READY")).rowcount
    assert n==1
    assert db.execute("SELECT status,scheduled_at FROM message_jobs WHERE id=1").fetchone()==("SCHEDULED",4600)
    assert db.execute("SELECT status FROM message_jobs WHERE id=2").fetchone()==("SENT",)
    # A second update must never rewrite an already scheduled or sent job.
    assert db.execute("UPDATE message_jobs SET status='SCHEDULED' WHERE id=? AND status='READY'",(1,)).rowcount==0
    db.close()
print("CallTag automation limits owner preference, clamp, stale scheduled/READY guards: PASS")
