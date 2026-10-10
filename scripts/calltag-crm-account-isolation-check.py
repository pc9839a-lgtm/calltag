#!/usr/bin/env python3
"""Owner namespace contract, backup fail-closed, and SQLite separation fixture."""
from pathlib import Path
import re
import sqlite3
from tempfile import TemporaryDirectory
P=Path("app/src/main/java/kr/pagero/calltag")
def src(n): return (P/n).read_text(encoding="utf-8")
scope=src("AccountDataScope.java")
assert '"calltag.db", "calltag_groups.db", "calltag_messages.db"' in scope
assert 'AuthSessionStore.ownerId(context).trim()' in scope
assert 'AuthSessionStore.hasSession(context)' in scope
assert 'MessageDigest.getInstance("SHA-256")' in scope
assert 'nameForOwner(requireOwner(context), legacyName)' in scope
crm=src("CallTagDbHelper.java")
assert "super(context, AccountDataScope.currentCrmName(context), null, DB_VERSION)" in crm
assert crm.count('AccountDataScope.assertCurrent(appContext, openedDatabaseName, DB_NAME)') >= 2
for filename in ("MessageGroupStore.java","MessageLogStore.java","CampaignStore.java",
                 "TaskTypeStore.java","PendingCallStore.java"):
    code=src(filename)
    assert 'super(context.getApplicationContext(), AccountDataScope.name(context, DB_NAME), null, DB_VERSION)' in code, filename
    assert code.count("assertScope();") >= 2, filename
store=src("CallTagSyncLocalStore.java")
assert 'return "owner:" + owner + "|crm:v2";' in store
assert "email:" not in store[store.index("public static String accountKey"):], "email fallback must not open CRM"
prefs=src("CallTagSyncPreferenceStore.java")
assert "return prefs(context).getBoolean(key, false);" in prefs
assert "prefs(context).getBoolean(KEY_ENABLED, false)" not in prefs
a=src("AccountActivity.java")
assert 'AccountDataScope.currentAccountDatabases(this)' in a
assert 'for (String databaseName : ownedDatabases)' in a
assert 'for (String databaseName : databaseList())' not in a
backup=src("CallTagBackupManager.java")
assert backup.count("blockUnsafeLegacyBackup();") == 2
assert backup.count("throw new IllegalStateException(") >= 1
app=src("CallTagApplication.java")
assert "LegacyCrmReviewNotice.showOnce(activity);" in app
assert app.index("AuthSessionStore.hasSession(this)",app.index('new Thread(() ->')) < app.index('MessageRecoveryManager.recoverNow(this,')
notice=src("LegacyCrmReviewNotice.java")
assert 'SQLiteDatabase.OPEN_READONLY' in notice
assert '계정별 DB와 분리했습니다' in notice
# Semantics of separately named databases: A/B writes never meet; legacy is not moved/deleted.
with TemporaryDirectory() as root:
    root=Path(root)
    legacy=root/"calltag.db"
    owner_a=root/"calltag-owner-aaa.db"
    owner_b=root/"calltag-owner-bbb.db"
    for path in (legacy,owner_a,owner_b):
        with sqlite3.connect(path) as con:
            con.execute("CREATE TABLE customers(id INTEGER PRIMARY KEY, name TEXT)")
    with sqlite3.connect(legacy) as con:
        con.execute("INSERT INTO customers VALUES (77,'원본')")
    with sqlite3.connect(owner_a) as con:
        con.execute("INSERT INTO customers VALUES (1,'A')")
    with sqlite3.connect(owner_b) as con:
        con.execute("INSERT INTO customers VALUES (1,'B')")
    for path,expected in ((legacy,"원본"),(owner_a,"A"),(owner_b,"B")):
        with sqlite3.connect(path) as con:
            assert con.execute("SELECT name FROM customers LIMIT 1").fetchone()[0]==expected
print("CallTag account-scoped CRM / dependent DB routing, sync epoch, safe deletion and backup block: PASS")
