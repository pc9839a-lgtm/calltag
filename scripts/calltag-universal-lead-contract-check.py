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
customer_list = read("CustomerListView.java")
source_detail = read("CustomerSourceDetailView.java")
application = read("CallTagApplication.java")
external_ui = read("ExternalLeadIntegrationActivity.java")
external_api = read("ExternalLeadIntegrationApiClient.java")
menu_installer = read("ExternalLeadMenuInstaller.java")
more_hub = read("MoreSettingsHubView.java")
section_more = (ROOT / "app/src/main/res/layout/section_more.xml").read_text(encoding="utf-8")
detail_layout = (ROOT / "app/src/main/res/layout/activity_customer_detail.xml").read_text(encoding="utf-8")
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
require(sync, 'if (!e2eTest || created) {', "existing customers must not be mutated by E2E probes")
require(sync, 'e2eTest ? "CALLTAG_E2E_TEST" : "CALLTAG_LEAD"', "E2E interaction history must be distinguishable")

# Resolver fallback is retained for old on-device rows/builds, but normal reads now carry source directly.
require(resolver, 'storedSource(context, customer.id)', "legacy source fallback missing")

# CRM must make the originating channel visible and filterable after import.
for label in ["Meta 광고", "Google Forms", "Webhook", "Direct API", "페이지로"]:
    require(resolver, f'"{label}"', f"normalized CRM source label missing: {label}")
require(resolver, 'public static boolean isExternal(Context context, Customer customer)', "external-source classifier missing")
require(customer_list, 'filterButton("외부 문의")', "external inquiry customer filter missing")
require(customer_list, 'CustomerSourceBadge.create(getContext(), sourceLabel)', "customer source badge must be generic")
require(customer_list, 'TAG_EXTERNAL_CARD', "external customer cards must be distinguishable")
require(detail_layout, 'kr.pagero.calltag.CustomerSourceDetailView', "customer detail source summary missing")
require(source_detail, '"유입 채널"', "customer detail channel heading missing")
require(source_detail, '"원본 · " + compact(raw)', "customer detail must show original server source")
require(lead, 'new StringBuilder("유입 채널: ").append(sourceLabel())', "interaction history must retain inquiry channel")
require(lead, 'out.append("\\n접수: ").append(submittedLabel());', "interaction history must retain inquiry receipt time")

# The actual visible More screen is MoreSettingsHubView. moreMenuList is a hidden legacy fallback.
require(section_more, 'kr.pagero.calltag.MoreSettingsHubView', "visible More settings hub missing")
require(more_hub, 'Section service = section("서비스")', "visible service section missing")
require(more_hub, 'service.addMenu("외부 문의 연동"', "external lead entry missing from visible More service section")
require(more_hub, 'ExternalLeadIntegrationActivity.class', "visible More entry must open external lead screen")
require(more_hub, '외부 문의 자동수신', "external lead entry must be searchable in settings")
require(menu_installer, '"외부 문의 연동"', "legacy More fallback label missing")
require(application, 'ExternalLeadMenuInstaller.install((MainActivity) activity);', "legacy More fallback installer missing")

# Native integration management: do not throw the user into an undeployed /connect web page.
for channel in ["PageRo", "Meta Lead Ads", "Google Forms", "Generic Webhook", "Direct API"]:
    require(external_ui, f'"{channel}"', f"channel card missing: {channel}")
require(external_ui, 'PageroConnectionCompactActivity.class', "PageRo must stay native")
require(external_ui, 'ExternalLeadIntegrationApiClient::startMetaOauth', "Meta must start from signed native API")
require(external_ui, 'createWebhookConnection', "Google Forms/Webhook must create connections from the app")
require(external_ui, 'webhookSamples', "Google Forms/Webhook status must be checkable from the app")
require(external_ui, 'updateWebhookMapping', "Google Forms recommended phone mapping must save from the app")
require(external_ui, 'createApiKey', "Direct API key must be issuable from the app")
require(external_ui, 'rotateApiKey', "Direct API key rotation must be available from the app")
require(external_ui, 'revokeApiKey', "Direct API key revoke must be available from the app")
require(external_ui, 'googleFormsScript(endpoint)', "Google Forms Apps Script must be generated with the one-time URL")
require(external_ui, 'transientSecret = ""', "one-time secret must be cleared from activity memory")
forbid(external_ui, 'https://calltag.pagero.kr/connect', "native integration must not send users to the undeployed Connect page")
forbid(external_ui, 'WebView', "external integration must not embed provider credentials in WebView")

# Native API client must reuse the signed CallTag session and exact server routes.
require(external_api, 'X-Inlet-Session', "native Connect API calls must be session scoped")
require(external_api, '/api/calltag/v1/connections', "Webhook connection route missing")
require(external_api, '/api/calltag/v1/keys', "Direct API key route missing")
require(external_api, '/api/calltag/v1/meta/oauth/start', "Meta OAuth start route missing")
require(external_api, '/api/calltag/v1/meta/oauth/session', "Meta OAuth session route missing")
require(external_api, '/api/calltag/v1/meta/oauth/complete', "Meta OAuth completion route missing")
require(external_api, 'META_ANDROID_RETURN_PATH', "Meta OAuth must use fixed Android return path")
forbid(external_api, 'SharedPreferences', "one-time external integration secrets must not be persisted by API client")

# Meta OAuth callback must return to the exported, narrowly-scoped custom scheme handler.
require(manifest, 'android:name=".ExternalLeadIntegrationActivity" android:exported="true"', "Meta callback activity must be exported")
require(manifest, 'android:launchMode="singleTop"', "Meta callback must reuse the integration activity")
require(manifest, 'android:scheme="calltag" android:host="external-lead" android:path="/meta"', "Meta callback intent filter missing")
require(external_ui, '"calltag".equalsIgnoreCase(uri.getScheme())', "Meta deep-link scheme must be validated")
require(external_ui, '"external-lead".equalsIgnoreCase(uri.getHost())', "Meta deep-link host must be validated")
require(external_ui, '!"/meta".equals(uri.getPath())', "Meta deep-link path must be validated")

# Manual lead refresh remains wired to the real receiver.
require(external_ui, 'UniversalLeadSyncManager.requestSync(this, true)', "manual lead refresh must call real sync manager")
require(external_ui, 'UniversalLeadSyncManager.ACTION_LEADS_UPDATED', "sync result feedback must use real broadcast")
require(external_ui, 'AuthSessionStore.hasSession(this)', "external lead UI must respect login session")

# CRM source visibility is a new Play update after the native integration-management build.
require(gradle, 'versionCode 2026082604', "Play versionCode must be bumped for CRM source visibility")
require(gradle, "versionName '0.44.49'", "Play versionName must be bumped for CRM source visibility")

print(
    "CallTag universal lead contract OK: PII-free pull/ACK, E2E isolation, native integration management, "
    "external inquiry source badges/filter/detail history and 0.44.49 release bump"
)
