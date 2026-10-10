#!/usr/bin/env python3
"""Read-only legacy CRM ownership / account-switch guard regression.

SQLite fixtures test the exact query shapes in CrmOwnershipPreflight.
Source contract checks assert sync suspends when current owner or session changes.
No real Android device or original CRM data are modified.
"""
from pathlib import Path
import sqlite3

java=Path("app/src/main/java/kr/pagero/calltag")
guard=(java/"CrmOwnershipPreflight.java").read_text(encoding="utf-8")
manager=(java/"CallTagSyncManager.java").read_text(encoding="utf-8")
adapter=(java/"CallTagSyncDataAdapter.java").read_text(encoding="utf-8")
for token in (
    "SQLiteDatabase.OPEN_READONLY",
    "SELECT COUNT(*) FROM universal_lead_import_events WHERE owner_id<>?",
    "AND entity_type IN ('customer','interaction','task') AND deleted=0",
    "AND entity_type IN ('customer','interaction','task') ",
    "throw new IllegalStateException(",
    "foreignMappings > 0 || foreignLeadJournal > 0",
    "liveRows > 0 && ownMappings == 0",
):
    assert token in guard, token
assert 'public static void requireScopedForSync(' in guard
assert '"|crm:v2"' in guard
assert "deleteDatabase(" not in guard
assert "INSERT INTO" not in guard
assert "UPDATE " not in guard
assert "DELETE FROM" not in guard
assert manager.count("CrmOwnershipPreflight.requireScopedForSync(context, accountKey);") >= 4
assert "requireSameAccount(context, accountKey, session);" in manager
assert manager.count("requireSameAccount(context, accountKey, session);") >= 7
assert "requireSameAccount(context, store);" in adapter
assert adapter.count("requireSameAccount(context, store);") >= 6
assert manager.index("CrmOwnershipPreflight.requireScopedForSync(context, accountKey)") < manager.index(
    "JSONObject statusResponse = CallTagSyncApiClient.status(session, deviceId);"
)

crm=sqlite3.connect(":memory:")
crm.executescript("""
 CREATE TABLE customers (id INTEGER PRIMARY KEY);
 CREATE TABLE interactions (id INTEGER PRIMARY KEY);
 CREATE TABLE follow_up_tasks (id INTEGER PRIMARY KEY);
 CREATE TABLE universal_lead_import_events (owner_id TEXT, event_id TEXT);
 INSERT INTO customers VALUES(11);
 INSERT INTO interactions VALUES(12);
 INSERT INTO universal_lead_import_events VALUES('A', 'event-1');
""")
mapping=sqlite3.connect(":memory:")
mapping.executescript("""
CREATE TABLE entity_map(account_key TEXT,entity_type TEXT,deleted INTEGER DEFAULT 0);
INSERT INTO entity_map VALUES('owner:A','customer',0);
INSERT INTO entity_map VALUES('owner:A','interaction',0);
""")
def inspect(account, owner):
    own=mapping.execute(
        "SELECT COUNT(*) FROM entity_map WHERE account_key=? "
        "AND entity_type IN ('customer','interaction','task') AND deleted=0",(account,)
    ).fetchone()[0]
    foreign=mapping.execute(
        "SELECT COUNT(*) FROM entity_map WHERE account_key<>? "
        "AND entity_type IN ('customer','interaction','task') AND deleted=0",(account,)
    ).fetchone()[0]
    journal=crm.execute(
        "SELECT COUNT(*) FROM universal_lead_import_events WHERE owner_id<>?",(owner,)
    ).fetchone()[0]
    rows=sum(crm.execute("SELECT COUNT(*) FROM "+table).fetchone()[0] for table in (
        "customers","interactions","follow_up_tasks"
    ))
    return rows>0 and (foreign>0 or journal>0),rows>0 and own==0
assert inspect("owner:A","A") == (False,False)
assert inspect("owner:B","B") == (True,True)   # no cross-user upload
mapping.execute("DELETE FROM entity_map")
assert inspect("owner:A","A") == (False,True)  # unknown provenance: hold sync
mapping.execute("INSERT INTO entity_map VALUES('owner:A','customer',0)")
crm.execute("INSERT INTO universal_lead_import_events VALUES('B','event-2')")
assert inspect("owner:A","A") == (True,False) # foreign lead journal alone blocks
# No fixture data was modified by the read-only ownership queries.
assert crm.execute("SELECT COUNT(*) FROM customers").fetchone()[0] == 1
assert crm.execute("SELECT COUNT(*) FROM universal_lead_import_events").fetchone()[0] == 2
print("CallTag legacy CRM owner preflight, foreign/unknown hold, account-switch guard: PASS")
