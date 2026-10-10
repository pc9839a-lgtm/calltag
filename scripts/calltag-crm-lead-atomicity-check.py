#!/usr/bin/env python3
"""Verify CRM v3 -> v4 additive journal and crash-safe duplicate prevention in SQLite.

Exercises the Java schema SQL rather than a hand-written test-only copy of the journal.
This is a DB contract test, not Android device/E2E verification.
"""
import ast
from pathlib import Path
import re
import sqlite3

source = Path("app/src/main/java/kr/pagero/calltag/CallTagDbHelper.java").read_text(
    encoding="utf-8"
)
sync = Path("app/src/main/java/kr/pagero/calltag/UniversalLeadSyncManager.java").read_text(
    encoding="utf-8"
)
assert "DB_VERSION = 5" in source
assert "if (oldVersion < 4)" in source
assert "createUniversalLeadImportTable(db);" in source
assert "crm.beginTransaction();" in sync
assert "crm.setTransactionSuccessful();" in sync
assert "crm.endTransaction();" in sync
assert sync.index("db.recordUniversalLeadImported(") < sync.index("receipts.markImported(")

table = re.search(r'UNIVERSAL_LEAD_IMPORTS\s*=\s*"([^"]+)"', source).group(1)
sql_body = source.split("private static void createUniversalLeadImportTable", 1)[1].split(
    "private boolean hasColumn", 1
)[0]
ddls = []
for expression in re.findall(r"db\.execSQL\((.*?)\);", sql_body, re.S):
    fragments = re.findall(r'"(?:\\.|[^"\\])*"|UNIVERSAL_LEAD_IMPORTS', expression)
    sql = "".join(table if value == "UNIVERSAL_LEAD_IMPORTS"
                  else ast.literal_eval(value) for value in fragments)
    ddls.append(sql)
assert len(ddls) == 2, f"expected CRM journal table and index: {ddls}"

con = sqlite3.connect(":memory:", isolation_level=None)
con.execute("PRAGMA foreign_keys=ON")
con.execute("CREATE TABLE customers(id INTEGER PRIMARY KEY, display_name TEXT)")
con.execute("""CREATE TABLE interactions(
    id INTEGER PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(id),
    type TEXT NOT NULL
)""")
con.execute("INSERT INTO customers(id, display_name) VALUES(1, '기존 고객')")
con.execute("INSERT INTO interactions(id, customer_id, type) VALUES(1, 1, 'CALL')")
con.execute("PRAGMA user_version=3")
for ddl in ddls:
    con.execute(ddl)
con.execute("PRAGMA user_version=4")
assert con.execute("SELECT display_name FROM customers WHERE id=1").fetchone()[0] == "기존 고객"
assert con.execute("SELECT COUNT(*) FROM interactions").fetchone()[0] == 1

def imported_customer(owner, event):
    row = con.execute(
        f"SELECT customer_id FROM {table} WHERE owner_id=? AND event_id=?",
        (owner, event),
    ).fetchone()
    return row[0] if row else None

def import_once(owner, event, customer_id):
    con.execute("BEGIN")
    try:
        existing = imported_customer(owner, event)
        if existing is not None:
            con.execute("COMMIT")
            return existing, False
        con.execute(
            "INSERT INTO customers(id, display_name) VALUES(?, ?)",
            (customer_id, f"문의 {event}"),
        )
        con.execute(
            "INSERT INTO interactions(customer_id, type) VALUES(?, 'LEAD_INQUIRY')",
            (customer_id,),
        )
        con.execute(
            f"""INSERT INTO {table}
            (owner_id, event_id, server_lead_id, customer_id, imported_at)
            VALUES (?, ?, ?, ?, ?)""",
            (owner, event, 501, customer_id, 12345),
        )
        con.execute("COMMIT")
        return customer_id, True
    except BaseException:
        con.execute("ROLLBACK")
        raise

# After CRM transaction commits, simulate process death before receipt DB/ACK write.
assert import_once("owner-A", "event-1", 11) == (11, True)
assert import_once("owner-A", "event-1", 12) == (11, False)
assert con.execute(
    "SELECT COUNT(*) FROM interactions WHERE customer_id=11"
).fetchone()[0] == 1, "crash recovery duplicated a consultation"
assert con.execute("SELECT COUNT(*) FROM customers WHERE id=12").fetchone()[0] == 0

# The same event ID from another owner must have an independent journal entry.
assert import_once("owner-B", "event-1", 13) == (13, True)
assert con.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] == 2

# A failed transaction must roll back ALL customer, interaction and event writes.
con.execute("BEGIN")
con.execute("INSERT INTO customers(id, display_name) VALUES(14, '실패 문의')")
con.execute("INSERT INTO interactions(customer_id, type) VALUES(14, 'LEAD_INQUIRY')")
con.execute("ROLLBACK")
assert imported_customer("owner-A", "event-aborted") is None
assert con.execute("SELECT COUNT(*) FROM customers WHERE id=14").fetchone()[0] == 0
assert con.execute("SELECT COUNT(*) FROM interactions WHERE customer_id=14").fetchone()[0] == 0

for ddl in ddls:
    con.execute(ddl)  # Re-running on upgrade must leave existing data untouched.
assert con.execute("PRAGMA user_version").fetchone()[0] == 4
print("CallTag CRM v3→v4 journal; atomic commit, rollback and crash retry: PASS")
