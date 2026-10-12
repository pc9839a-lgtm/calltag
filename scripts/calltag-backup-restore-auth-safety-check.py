#!/usr/bin/env python3
"""P0 Android account/backup fail-closed source contracts and SQLite owner fixtures.
This verifies logic and regression-sensitive ordering, not physical-device SAF.
"""
from pathlib import Path
import sqlite3
from tempfile import TemporaryDirectory

root=Path("app/src/main/java/kr/pagero/calltag")
auth=(root/"AuthSessionStore.java").read_text(encoding="utf-8")
backup=(root/"CallTagBackupManager.java").read_text(encoding="utf-8")
scope=(root/"AccountDataScope.java").read_text(encoding="utf-8")

assert 'String incomingOwnerId = profile.optString("ownerId", "").trim();' in auth
assert 'if (incomingOwnerId.isEmpty()) {' in auth
assert 'throw new IllegalStateException("로그인 응답에 서버 계정 ID가 없습니다.' in auth
assert 'profile.has("ownerId")' not in auth
assert auth.index('if (incomingOwnerId.isEmpty())') < auth.index('boolean saved = current.edit()')
assert 'cancelAccountNotifications(context);' in auth
assert 'CallTagSyncWorkScheduler.reconcile(context);' in auth
assert 'AccountDataScope.assertCurrent' in (root/"CallTagDbHelper.java").read_text(encoding="utf-8")
assert 'AccountDataScope.assertCurrent' in (root/"MessageLogStore.java").read_text(encoding="utf-8")
assert '"calltag.db", "calltag_groups.db", "calltag_messages.db"' in scope

assert 'File sealed = new File(context.getCacheDir(),' in backup
assert 'encryptDirectoryToFile(stage, sealed, password);' in backup
assert 'openOutputStream(target, "wt")' in backup
assert backup.index('encryptDirectoryToFile(stage, sealed, password)') < backup.index('openOutputStream(target, "wt")')
assert 'deleteIfExists(sealed);' in backup
assert 'new BufferedOutputStream(new FileOutputStream(sealed))' in backup
assert 'boolean preserveRollback = false;' in backup
assert 'preserveRollback = true;' in backup
assert 'if (!preserveRollback) deleteRecursively(rollback);' in backup
assert 'File rollback = new File(app.getNoBackupFilesDir()' in backup
assert 'AccountDataScope.setRestoreReviewPending(app, true)' in backup
assert 'replaceFromSnapshot(app, rollback, owner, session, true)' in backup
assert 'requireSameAccount(context, owner, session);' in backup
assert backup.index('if (rollbackError != null) {') < backup.index('if (!preserveRollback) deleteRecursively(rollback);')

with TemporaryDirectory() as d:
    a,b=Path(d)/'A.db',Path(d)/'B.db'
    for file,label in [(a,'owner A'),(b,'owner B')]:
        with sqlite3.connect(file) as conn:
            conn.execute("CREATE TABLE customers(id INTEGER PRIMARY KEY, label TEXT)")
            conn.execute("INSERT INTO customers VALUES(1,?)",(label,))
    assert sqlite3.connect(a).execute('SELECT label FROM customers').fetchone()[0]=='owner A'
    assert sqlite3.connect(b).execute('SELECT label FROM customers').fetchone()[0]=='owner B'
    # Simulate a transaction failing after an initial owner-specific change.
    with sqlite3.connect(a) as conn:
        try:
            conn.execute("BEGIN")
            conn.execute("UPDATE customers SET label='bad' WHERE id=1")
            conn.execute("INSERT INTO customers VALUES(1,'duplicate')")
            conn.commit()
        except sqlite3.IntegrityError:
            conn.rollback()
    assert sqlite3.connect(a).execute('SELECT label FROM customers').fetchone()[0]=='owner A'
    assert sqlite3.connect(b).execute('SELECT label FROM customers').fetchone()[0]=='owner B'

print('CallTag AuthSession owner fail-closed, encrypted staging and rollback retention: PASS')
