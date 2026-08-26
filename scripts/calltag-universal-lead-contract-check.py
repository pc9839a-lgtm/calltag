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
application = read("CallTagApplication.java")
external_ui = read("ExternalLeadIntegrationActivity.java")
menu_installer = read("ExternalLeadMenuInstaller.java")
more_hub = read("MoreSettingsHubView.java")
section_more = (ROOT / "app/src/main/res/layout/section_more.xml").read_text(encoding="utf-8")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")

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

# The actual visible More screen is MoreSettingsHubView. moreMenuList is a hidden 1dp legacy fallback,
# so a label only in ExternalLeadMenuInstaller is NOT sufficient proof that users can see it.
require(section_more, 'kr.pagero.calltag.MoreSettingsHubView', "visible More settings hub missing")
require(more_hub, 'Section service = section("서비스")', "visible service section missing")
require(more_hub, 'service.addMenu("외부 문의 연동"', "external lead entry missing from visible More service section")
require(more_hub, 'ExternalLeadIntegrationActivity.class', "visible More entry must open external lead screen")
require(more_hub, '외부 문의 자동수신', "external lead entry must be searchable in settings")

# Keep the legacy installer harmless as a fallback for old layout paths, but do not rely on it as visibility proof.
require(menu_installer, '"외부 문의 연동"', "legacy More fallback label missing")
require(application, 'ExternalLeadMenuInstaller.install((MainActivity) activity);', "legacy More fallback installer missing")
require(manifest, 'android:name=".ExternalLeadIntegrationActivity"', "external lead activity missing from manifest")

for channel in ["PageRo", "Meta Lead Ads", "Google Forms", "Generic Webhook", "Direct API"]:
    require(external_ui, f'"{channel}"', f"channel card missing: {channel}")
require(external_ui, 'UniversalLeadSyncManager.requestSync(this, true)', "manual lead refresh must call real sync manager")
require(external_ui, 'UniversalLeadSyncManager.ACTION_LEADS_UPDATED', "sync result feedback must use real broadcast")
require(external_ui, 'AuthSessionStore.hasSession(this)', "external lead UI must respect login session")
require(external_ui, 'https://calltag.pagero.kr/connect', "external setup must use HTTPS CallTag Connect")
forbid(external_ui, 'WebView', "external settings must open trusted browser instead of embedding secrets in a WebView")

# 0.44.46 was already built before the visible-hub bug was found; this fix must be a new Play update.
require(gradle, 'versionCode 2026082602', "Play versionCode must be bumped after visible More fix")
require(gradle, "versionName '0.44.47'", "Play versionName must be bumped after visible More fix")

print(
    "CallTag universal lead contract OK: source persistence/hydration, PageRo exclusion, "
    "generic FCM pull/ACK, E2E customer isolation, visible More hub external-integration entry and release bump"
)
