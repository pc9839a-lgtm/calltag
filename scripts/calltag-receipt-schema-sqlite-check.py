#!/usr/bin/env python3
"""Exercise the actual Java v2 receipt-table DDL against SQLite, with a legacy v1 DB."""
import ast
from pathlib import Path
import re
import sqlite3

source = Path("app/src/main/java/kr/pagero/calltag/UniversalLeadReceiptStore.java").read_text(
    encoding="utf-8"
)
assert 'DB_VERSION = 2' in source
assert 'if (oldVersion < 2)' in source
assert 'createAccountTable(db);' in source
assert 'DROP TABLE' not in source
table = re.search(r'ACCOUNT_TABLE\s*=\s*"([^"]+)"', source).group(1)
start = source.index("private static void createAccountTable")
end = source.index("public boolean isImported", start)
ddl_method = source[start:end]

# Reconstruct each SQL statement from Java's adjacent string fragments plus table constant.
statements = []
for expression in re.findall(r"db\.execSQL\((.*?)\);", ddl_method, flags=re.S):
    parts = re.findall(r'"(?:\\.|[^"\\])*"|ACCOUNT_TABLE', expression)
    sql = "".join(table if part == "ACCOUNT_TABLE" else ast.literal_eval(part)
                  for part in parts)
    statements.append(sql)
assert len(statements) == 3, f"expected table and two indexes: {statements}"

conn = sqlite3.connect(":memory:")
# Simulate an installed v1 database containing an unacknowledged customer lead.
conn.execute("""CREATE TABLE lead_receipts (
    event_id TEXT PRIMARY KEY,
    server_lead_id INTEGER NOT NULL,
    customer_id INTEGER NOT NULL,
    status TEXT NOT NULL,
    received_at INTEGER NOT NULL,
    acked_at INTEGER
)""")
conn.execute(
    "INSERT INTO lead_receipts VALUES (?, ?, ?, ?, ?, ?)",
    ("legacy-event", 42, 100, "IMPORTED", 123456789, None),
)
conn.execute("PRAGMA user_version=1")
for statement in statements:
    conn.execute(statement)
conn.execute("PRAGMA user_version=2")

assert conn.execute("SELECT event_id, customer_id, status FROM lead_receipts").fetchone() == (
    "legacy-event", 100, "IMPORTED"
), "upgrade destroyed or changed an unattributed legacy receipt"
assert conn.execute("PRAGMA user_version").fetchone()[0] == 2

insert = f"""INSERT OR IGNORE INTO {table}
(owner_id, event_id, server_lead_id, customer_id, status, received_at)
VALUES (?, ?, ?, ?, ?, ?)"""
conn.execute(insert, ("owner-A", "same-event", 77, 1, "IMPORTED", 1))
conn.execute(insert, ("owner-B", "same-event", 77, 2, "IMPORTED", 2))
assert conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] == 2, (
    "same event ID under different owners must not deduplicate"
)
conn.execute(
    f"UPDATE {table} SET status='ACKED', acked_at=3 WHERE owner_id=? AND server_lead_id=?",
    ("owner-A", 77),
)
assert conn.execute(
    f"SELECT owner_id, status FROM {table} ORDER BY owner_id"
).fetchall() == [("owner-A", "ACKED"), ("owner-B", "IMPORTED")], (
    "ACK must not change a different owner's receipt even with identical lead IDs"
)
conn.execute(insert, ("owner-A", "same-event", 77, 1, "IMPORTED", 4))
assert conn.execute(
    f"SELECT status FROM {table} WHERE owner_id='owner-A'"
).fetchone()[0] == "ACKED", "retry must not reset an acknowledged receipt"

for statement in statements:
    conn.execute(statement)  # DDL is idempotent on a subsequent open/upgrade check.
assert conn.execute("SELECT COUNT(*) FROM lead_receipts").fetchone()[0] == 1
print("CallTag receipt SQLite v1 preservation, v2 owner isolation, ACK and retry: PASS")
