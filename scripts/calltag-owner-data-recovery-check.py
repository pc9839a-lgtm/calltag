#!/usr/bin/env python3
"""CallTag v2 data owner recovery/backup contract and SQLite rollback fixture.

This is not an Android device/emulator integration test. It verifies the Java
source safety contracts and repeats representative foreign-key copy/rollback
behaviour in SQLite. Device upgrade and backup round-trip remain release gates.
"""
from pathlib import Path
import re
import sqlite3
from tempfile import TemporaryDirectory

base=Path("app/src/main/java/kr/pagero/calltag")
def text(name): return (base/name).read_text(encoding="utf-8")
recovery=text("LegacyCrmRecoveryManager.java")
backup=text("CallTagBackupManager.java")
ui=text("BackupRestoreActivity.java")
scope=text("AccountDataScope.java")
for must in [
    "AccountDataScope.requireOwner(app)",
    "ownerId.equals(confirmedOwnerId)",
    "PageroLeadSyncManager.isRunning()",
    "UniversalLeadSyncManager.isRunning()",
    "CallTagSyncManager.beginMaintenance()",
    "CallTagSyncManager.endMaintenance()",
    "SQLiteDatabase.OPEN_READONLY",
    "post_call_save_receipts",
    "legacy_crm_recovery",
    "ensureFreshTarget(target)",
    "target.beginTransaction()",
    "target.setTransactionSuccessful()",
    "target.endTransaction()",
    'PRAGMA foreign_key_check',
    "ownerId.equals(AuthSessionStore.ownerId(app))",
    "session.equals(AuthSessionStore.session(app))",
    "SELECT COUNT(*) FROM universal_lead_import_events WHERE owner_id<>?",
    "account_key NOT LIKE '%|crm:v2'",
    '"owner:" + ownerId + "|crm:v2"',
    'target.insertOrThrow(table, null, values)',
]:
    assert must in recovery, "legacy recovery safety contract: "+must
assert "source.execSQL(" not in recovery
assert "deleteDatabase(" not in recovery and "source.delete(" not in recovery
assert '복구\".equals(confirmation.getText().toString().trim())' in ui
assert "LegacyCrmRecoveryManager.recoverAfterOwnerConfirmation(this, owner)" in ui
assert 'private static final int FORMAT_VERSION = 2;' in backup
for must in [
    '"ownerFingerprint"',
    '"owner-sqlite-v2"',
    '"imagesIncluded", false',
    "AccountDataScope.currentAccountDatabases(context)",
    "AccountDataScope.currentAccountPreferences(context)",
    "isAllowedOwnerPath(context, path)",
    "context.deleteDatabase(name)",
    "requireSameAccount(context, owner, session)",
    "invalidateOwnerSyncMappings(app)",
    "CallTagSyncManager.beginMaintenance()",
    "CallTagSyncManager.endMaintenance()",
]:
    assert must in backup, "v2 backup contract: "+must
assert "blockUnsafeLegacyBackup();" not in backup
assert "for (String name : context.databaseList())" not in backup
assert "deleteRecursively(currentImages)" not in backup
assert '"databases/calltag.db"' not in backup
assert 'getDatabasePath("calltag_messages.db")' not in backup
assert 'private static boolean isAllowedOwnerPath' in backup
assert 'manifest.optInt("formatVersion", 0) != FORMAT_VERSION' in backup
assert '"기존 공용 백업(v1)"' not in backup or "공용 백업(v1)" in backup
scheduled=text("ScheduledMessageReceiver.java")
scheduler=text("MessageScheduler.java")
sender=text("SmsSender.java")
status=text("SmsStatusReceiver.java")
assert "currentAccountPreferences" in scope
assert "workEpoch(Context context)" in scope
assert "rotateWorkEpoch(Context context)" in scope
assert "AccountDataScope.rotateWorkEpoch(app)" in backup
assert "AccountDataScope.setRestoreReviewPending(app, true)" in backup
assert "isRestoreReviewPending(Context context)" in scope
assert "setRestoreReviewPending(Context context, boolean pending)" in scope
assert "AccountDataScope.isRestoreReviewPending(context)" in scheduled
assert "AccountDataScope.isRestoreReviewPending(context)" in sender
assert "AccountDataScope.isRestoreReviewPending(app)" in text("MessageRecoveryManager.java")
assert "복원된 문자 작업 확인 후 재개" in ui
assert 'for (String name : AccountDataScope.currentAccountDatabases(context)) {' in backup
assert 'for (String preference : AccountDataScope.currentAccountPreferences(context)) {' in backup
assert "disableAndResetOwnerSync(app, ownerId, session)" in recovery
for name,code in (("alarm",scheduled),("sms callback",status)):
    assert 'EXTRA_WORK_EPOCH' in code,name
    assert "AccountDataScope.workEpoch(context)" in code,name
assert "AccountDataScope.workEpoch(context)" in scheduler
assert "AccountDataScope.workEpoch(context)" in sender
assert "partStatusKey(context, messageId)" in status
assert "구버전 고객·상담 기록 복구" in ui
assert "createButton.setEnabled(!value)" in ui
assert "restoreButton.setEnabled(!value)" in ui

with TemporaryDirectory() as root:
    root=Path(root)
    src=root/"calltag.db"
    a=root/"calltag-owner-a.db"
    b=root/"calltag-owner-b.db"
    schema="""
    PRAGMA foreign_keys=ON;
    CREATE TABLE customers(id INTEGER PRIMARY KEY, display_name TEXT NOT NULL);
    CREATE TABLE interactions(id INTEGER PRIMARY KEY, customer_id INTEGER NOT NULL,
      note TEXT, FOREIGN KEY(customer_id) REFERENCES customers(id));
    CREATE TABLE follow_up_tasks(id INTEGER PRIMARY KEY, customer_id INTEGER NOT NULL,
      interaction_id INTEGER, FOREIGN KEY(customer_id) REFERENCES customers(id),
      FOREIGN KEY(interaction_id) REFERENCES interactions(id));
    """
    for p in (src,a,b):
        with sqlite3.connect(p) as d: d.executescript(schema)
    with sqlite3.connect(src) as d:
        d.execute("INSERT INTO customers VALUES(17,'legacy owner')")
        d.execute("INSERT INTO interactions VALUES(71,17,'note')")
        d.execute("INSERT INTO follow_up_tasks VALUES(3,17,71)")
    with sqlite3.connect(b) as d:
        d.execute("INSERT INTO customers VALUES(17,'different owner')")
    # A's owner DB starts empty. All linked rows copy in one transaction.
    with sqlite3.connect(src) as old,sqlite3.connect(a) as new:
        new.execute("BEGIN")
        for t in ("customers","interactions","follow_up_tasks"):
            records=old.execute("SELECT * FROM "+t).fetchall()
            cols=len(records[0]) if records else 0
            for row in records:
                new.execute("INSERT INTO "+t+" VALUES("+",".join("?" for _ in range(cols))+")",row)
        assert new.execute("PRAGMA foreign_key_check").fetchall()==[]
        new.commit()
    with sqlite3.connect(a) as d:
        assert d.execute("SELECT display_name FROM customers").fetchone()[0]=="legacy owner"
        assert d.execute("SELECT COUNT(*) FROM interactions").fetchone()[0]==1
        assert d.execute("SELECT COUNT(*) FROM follow_up_tasks").fetchone()[0]==1
    with sqlite3.connect(b) as d:
        assert d.execute("SELECT display_name FROM customers").fetchone()[0]=="different owner"
    with sqlite3.connect(src) as d:
        assert d.execute("SELECT display_name FROM customers").fetchone()[0]=="legacy owner"
    # A second restore to an occupied owner DB fails; source remains unchanged.
    with sqlite3.connect(a) as d:
        try:
            d.execute("BEGIN")
            d.execute("INSERT INTO customers VALUES(17,'should fail')")
        except sqlite3.IntegrityError:
            d.rollback()
        else:
            raise AssertionError("non-pristine target did not reject collisions")
    with sqlite3.connect(a) as d:
        assert d.execute("SELECT COUNT(*) FROM customers").fetchone()[0]==1
    # Simulated copy failure before commit must roll back every inserted row.
    c=root/"failure.db"
    with sqlite3.connect(c) as d:
        d.executescript(schema)
        try:
            d.execute("BEGIN")
            d.execute("INSERT INTO customers VALUES(1,'crash')")
            d.execute("INSERT INTO interactions VALUES(1,999,'bad FK')")
            d.commit()
        except sqlite3.IntegrityError:
            d.rollback()
        assert d.execute("SELECT COUNT(*) FROM customers").fetchone()[0]==0

print("CallTag owner recovery + v2 backup source contract and rollback fixtures: PASS")
