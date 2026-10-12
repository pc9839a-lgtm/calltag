#!/usr/bin/env python3
"""Simulate legacy CRM account provenance and foreign owner rejection with SQLite.

Android physical-device verification remains required.
"""
import sqlite3
from pathlib import Path
from tempfile import TemporaryDirectory

base=Path("app/src/main/java/kr/pagero/calltag")
source=(base/"LegacyCrmRecoveryManager.java").read_text(encoding="utf-8")
assert "account_key NOT IN (?,?)" in source
assert "account_key IN (?,?)" in source
assert '"entity_map", "sync_meta"' in source
assert "if (!provenOwner)" in source
assert "NOT LIKE '%|crm:v2'" not in source

with TemporaryDirectory() as d:
    db=sqlite3.connect(Path(d)/"account-migration.db")
    db.executescript("""
       CREATE TABLE universal_lead_import_events(owner_id TEXT NOT NULL);
       CREATE TABLE post_call_save_receipts(account_key TEXT NOT NULL);
       CREATE TABLE entity_map(account_key TEXT NOT NULL);
       CREATE TABLE sync_meta(account_key TEXT NOT NULL);
    """)
    def status(owner):
        keys=("owner:"+owner,"owner:"+owner+"|crm:v2")
        other=0
        own=0
        for name,column in [
            ("universal_lead_import_events","owner_id"),
            ("post_call_save_receipts","account_key"),
            ("entity_map","account_key"),
            ("sync_meta","account_key")
        ]:
            if name == "universal_lead_import_events":
                other+=db.execute(f"SELECT COUNT(*) FROM {name} WHERE {column}<>?",(owner,)).fetchone()[0]
                own+=db.execute(f"SELECT COUNT(*) FROM {name} WHERE {column}=?",(owner,)).fetchone()[0]
            else:
                other+=db.execute(f"SELECT COUNT(*) FROM {name} WHERE {column} NOT IN (?,?)",keys).fetchone()[0]
                own+=db.execute(f"SELECT COUNT(*) FROM {name} WHERE {column} IN (?,?)",keys).fetchone()[0]
        return other,own>0
    assert status("A")==(0,False), "unowned source must not be claimed"
    db.execute("INSERT INTO post_call_save_receipts VALUES('owner:A|crm:v2')")
    assert status("A")==(0,True), "own v2 receipt is valid proof"
    db.execute("INSERT INTO entity_map VALUES('owner:B|crm:v2')")
    assert status("A")==(1,True), "foreign v2 entity map must block"
    db.execute("DELETE FROM entity_map")
    db.execute("INSERT INTO sync_meta VALUES('owner:B|crm:v2')")
    assert status("A")==(1,True), "foreign sync meta must block"
    db.execute("DELETE FROM sync_meta")
    db.execute("INSERT INTO post_call_save_receipts VALUES('owner:B')")
    assert status("A")==(1,True), "foreign old receipt must block"
    db.execute("DELETE FROM post_call_save_receipts")
    db.execute("INSERT INTO entity_map VALUES('owner:A')")
    assert status("A")==(0,True), "owner A legacy map allowed"
    db.execute("INSERT INTO sync_meta VALUES('owner:A|crm:v2')")
    assert status("A")==(0,True), "same-owner old/v2 mixed is allowed"
    db.execute("INSERT INTO universal_lead_import_events VALUES('B')")
    assert status("A")[0]==1, "foreign lead ownership must block"
    db.close()
print("CallTag foreign-owner-v2 and legacy provenance SQLite scenarios: PASS")
