#!/usr/bin/env python3
"""Check real Java v5 post-call journal DDL, rollback and account-qualified idempotency.

SQLite/schema and Java source contract test only; not a device instrumentation test.
"""
import ast
from pathlib import Path
import re
import sqlite3

root = Path("app/src/main/java/kr/pagero/calltag")
helper = (root / "CallTagDbHelper.java").read_text(encoding="utf-8")
activity = (root / "PostCallActivity.java").read_text(encoding="utf-8")
overlay = (root / "PostCallOverlayManager.java").read_text(encoding="utf-8")
settings = (root / "SettingsStore.java").read_text(encoding="utf-8")
assert "DB_VERSION = 5" in helper
assert "if (oldVersion < 5)" in helper
assert helper.count("createPostCallSaveReceiptsTable(db);") == 2
assert "database.beginTransaction();" in helper
assert "database.insertOrThrow(POST_CALL_SAVES, null, receipt);" in helper
assert "database.setTransactionSuccessful();" in helper
assert "database.endTransaction();" in helper
assert helper.index("CallInteractionDeduper.insertOnce(") < helper.index(
    "database.insertOrThrow(POST_CALL_SAVES, null, receipt);"
)
assert "CallTagSyncLocalStore.accountKey(appContext)" in helper
assert "db.savePostCallMemo(" in activity
assert "db.savePostCallMemo(" in overlay
assert "accountKey + \"|\" + fingerprint" in settings
assert 'fingerprint.equals(prefs(context).getString(KEY_LAST_PROCESSED_CALL, ""))' not in settings

table = re.search(r'POST_CALL_SAVES\s*=\s*"([^"]+)"', helper).group(1)
ddl_method = helper.split("private static void createPostCallSaveReceiptsTable", 1)[1].split(
    "private boolean hasColumn", 1
)[0]
ddls = []
for expression in re.findall(r"db\.execSQL\((.*?)\);", ddl_method, re.S):
    parts = re.findall(r'"(?:\\.|[^"\\])*"|POST_CALL_SAVES', expression)
    ddls.append("".join(
        table if part == "POST_CALL_SAVES" else ast.literal_eval(part)
        for part in parts
    ))
assert len(ddls) == 2, ddls

db = sqlite3.connect(":memory:", isolation_level=None)
db.execute("PRAGMA foreign_keys=ON")
db.execute("CREATE TABLE customers(id INTEGER PRIMARY KEY, normalized_phone TEXT UNIQUE)")
db.execute("""CREATE TABLE interactions(
    id INTEGER PRIMARY KEY,
    customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    type TEXT NOT NULL
)""")
db.execute("INSERT INTO customers VALUES (1, '01011112222')")
db.execute("INSERT INTO interactions VALUES (10, 1, 'CALL')")
db.execute("PRAGMA user_version=4")
for ddl in ddls:
    db.execute(ddl)
db.execute("PRAGMA user_version=5")

assert db.execute("SELECT COUNT(*) FROM interactions").fetchone()[0] == 1
assert db.execute("PRAGMA user_version").fetchone()[0] == 5

def save(owner, fingerprint, customer, interaction, *, crash_before_receipt=False):
    db.execute("BEGIN")
    try:
        old = db.execute(
            f"SELECT interaction_id FROM {table} WHERE account_key=? AND call_fingerprint=?",
            (owner, fingerprint)
        ).fetchone()
        if old is not None:
            db.execute("COMMIT")
            return old[0], False
        db.execute("INSERT INTO customers VALUES (?, ?)",
                   (customer, str(customer).zfill(11)))
        db.execute("INSERT INTO interactions VALUES (?, ?, 'CALL')",
                   (interaction, customer))
        if crash_before_receipt:
            raise RuntimeError("simulated Android crash during CRM save")
        db.execute(f"""INSERT INTO {table}
            (account_key,call_fingerprint,interaction_id,saved_at) VALUES(?,?,?,?)""",
                   (owner, fingerprint, interaction, 12345))
        db.execute("COMMIT")
        return interaction, True
    except BaseException:
        db.execute("ROLLBACK")
        raise

assert save("owner:A", "call_log:77", 2, 20) == (20, True)
# Simulate death after database commit but before the SettingsStore preference write.
assert save("owner:A", "call_log:77", 3, 21) == (20, False)
assert db.execute("SELECT COUNT(*) FROM interactions").fetchone()[0] == 2
assert db.execute("SELECT COUNT(*) FROM customers WHERE id=3").fetchone()[0] == 0

assert save("owner:B", "call_log:77", 4, 40) == (40, True)
assert db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] == 2
assert db.execute(
    f"SELECT interaction_id FROM {table} WHERE account_key='owner:A'"
).fetchone()[0] == 20

try:
    save("owner:A", "call_log:99", 5, 50, crash_before_receipt=True)
except RuntimeError:
    pass
else:
    raise AssertionError("crash did not interrupt save")
assert db.execute("SELECT COUNT(*) FROM customers WHERE id=5").fetchone()[0] == 0
assert db.execute("SELECT COUNT(*) FROM interactions WHERE id=50").fetchone()[0] == 0
assert db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] == 2

# Deleting the owning interaction cannot leave a dangling fingerprint receipt.
db.execute("DELETE FROM customers WHERE id=2")
assert db.execute(
    f"SELECT COUNT(*) FROM {table} WHERE account_key='owner:A'"
).fetchone()[0] == 0
for ddl in ddls:
    db.execute(ddl)
assert db.execute("SELECT COUNT(*) FROM interactions WHERE id=10").fetchone()[0] == 1
print("CallTag CRM v4→v5 post-call memo receipt, rollback, owner-qualified replay: PASS")
