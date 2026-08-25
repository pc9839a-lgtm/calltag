from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/kr/pagero/calltag"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def require(text: str, token: str, message: str) -> None:
    if token not in text:
        raise SystemExit(f"contract failed: {message}: {token}")


def forbid(text: str, token: str, message: str) -> None:
    if token in text:
        raise SystemExit(f"contract failed: {message}: {token}")


db = read("CallTagDbHelper.java")
api = read("UniversalLeadApiClient.java")
lead = read("UniversalLead.java")
sync = read("UniversalLeadSyncManager.java")
pagero_sync = read("PageroLeadSyncManager.java")
fcm = read("CallTagMessagingService.java")
resolver = read("CustomerSourceResolver.java")

# Customer source must be first-class local CRM data. This protects both PageRo and generic leads.
require(
    db,
    'values.put("source", source == null ? "" : source.trim());',
    "insertCustomer must persist the supplied source",
)
require(
    db,
    'cursor.getString(cursor.getColumnIndexOrThrow("source"))',
    "readCustomer must hydrate source from SQLite",
)
forbid(
    db,
    'values.put("source", "");',
    "insertCustomer must not silently discard source",
)

# PageRo remains on the legacy queue/automation path while canonical dual-write is enabled.
require(
    api,
    'excludeSourceType=pagero',
    "universal pull must exclude PageRo canonical copies",
)

# Generic delivery is a PII-free signal; the app must pull details from the signed API.
require(fcm, '"lead_available".equals(type)', "generic FCM route missing")
require(fcm, '"pagero_lead_available".equals(type)', "legacy PageRo FCM route must remain")
require(sync, 'UniversalLeadApiClient.list(session, after, PAGE_SIZE)', "generic pull missing")
require(sync, 'receipts.markImported(lead.eventId, lead.id, imported.customerId)', "local idempotency receipt missing")
require(sync, 'UniversalLeadApiClient.acknowledgeImported', "generic imported ACK missing")
require(sync, 'values.put("source", sourceLabel);', "generic sync must update customer source")
require(pagero_sync, 'values.put("source", CustomerSourceResolver.PAGERO);', "PageRo sync must preserve PageRo source")

# E2E probes must exercise the real import/ACK path without corrupting an existing real customer.
require(lead, 'E2E_TEST_SOURCE_TYPE = "calltag_e2e_test"', "E2E source type must be explicit")
require(lead, 'public boolean isE2eTest()', "UniversalLead must expose E2E classification")
require(sync, 'boolean e2eTest = lead.isE2eTest();', "sync must classify E2E imports")
require(
    sync,
    'if (!e2eTest || created) {',
    "existing customers must not be mutated by E2E probes",
)
require(
    sync,
    'e2eTest ? "CALLTAG_E2E_TEST" : "CALLTAG_LEAD"',
    "E2E interaction history must be distinguishable from real leads",
)

# Resolver fallback is retained for old on-device rows/builds, but normal reads now carry source directly.
require(resolver, 'storedSource(context, customer.id)', "legacy source fallback missing")

print(
    "CallTag universal lead contract OK: source persistence/hydration, PageRo exclusion, "
    "generic FCM pull/ACK, E2E customer isolation and legacy compatibility"
)
