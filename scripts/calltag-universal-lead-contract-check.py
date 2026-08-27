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

# Core CRM delivery contract.
require(db, 'values.put("source", source == null ? "" : source.trim());', "insertCustomer must persist source")
require(db, 'cursor.getString(cursor.getColumnIndexOrThrow("source"))', "readCustomer must hydrate source")
forbid(db, 'values.put("source", "");', "insertCustomer must not discard source")
require(api, 'excludeSourceType=pagero', "universal pull must exclude PageRo canonical copies")
require(pagero_sync, 'values.put("source", CustomerSourceResolver.PAGERO);', "PageRo source persistence missing")
require(fcm, '"lead_available".equals(type)', "generic FCM route missing")
require(fcm, '"pagero_lead_available".equals(type)', "PageRo FCM route must remain")
require(sync, 'UniversalLeadApiClient.list(session, after, PAGE_SIZE)', "generic pull missing")
require(sync, 'receipts.markImported(lead.eventId, lead.id, imported.customerId)', "local idempotency receipt missing")
require(sync, 'UniversalLeadApiClient.acknowledgeImported', "generic imported ACK missing")
require(sync, 'values.put("source", sourceLabel);', "generic sync must update customer source")
require(lead, 'E2E_TEST_SOURCE_TYPE = "calltag_e2e_test"', "historical E2E source classification missing")
require(sync, 'if (!e2eTest || created) {', "historical E2E lead isolation missing")

# CRM source UX remains visible/filterable.
for label in ["Meta 광고", "Google Forms", "Webhook", "Direct API", "페이지로"]:
    require(resolver, f'"{label}"', f"normalized CRM source label missing: {label}")
require(resolver, 'public static boolean isExternal(Context context, Customer customer)', "external-source classifier missing")
require(customer_list, 'filterButton("외부 문의")', "external inquiry filter missing")
require(customer_list, 'CustomerSourceBadge.create(getContext(), sourceLabel)', "source badge missing")
require(detail_layout, 'kr.pagero.calltag.CustomerSourceDetailView', "customer detail source summary missing")
require(source_detail, '"유입 채널"', "customer detail channel heading missing")

# Production More entry only; no test surface.
require(section_more, 'kr.pagero.calltag.MoreSettingsHubView', "visible More settings hub missing")
require(more_hub, 'service.addMenu("외부 문의 연동"', "external lead entry missing")
require(more_hub, 'ExternalLeadIntegrationActivity.class', "external lead entry destination missing")
forbid(more_hub, '외부 문의 수신 테스트', "test entry must not be exposed")
forbid(more_hub, 'ExternalLeadE2eActivity', "test activity must not be referenced")
forbid(manifest, '.ExternalLeadE2eActivity', "test activity must not be registered")
require(menu_installer, '"외부 문의 연동"', "legacy More fallback label missing")
require(application, 'ExternalLeadMenuInstaller.install((MainActivity) activity);', "legacy More fallback installer missing")

# Compact integration UI: PageRo, Meta, Google Forms and Webhook only.
for channel in ["PageRo", "Meta Lead Ads", "Google Forms", "Webhook"]:
    require(external_ui, f'"{channel}"', f"channel card missing: {channel}")
forbid(external_ui, '"Direct API"', "Direct API must not be exposed in integration UI")
forbid(external_ui, 'createDirectApiKey', "Direct API creation UI must be removed")
forbid(external_ui, 'script.new', "Apps Script editor must not be part of Google Forms UX")
forbid(external_ui, 'installCallTag', "Apps Script installer must be removed")
forbid(external_ui, 'googleFormsScript', "generated Apps Script must be removed")
forbid(external_ui, 'Google Form 선택\", 18f', "old manual form-link wizard must be removed")
require(external_ui, 'PageroConnectionCompactActivity.class', "PageRo must stay native")
require(external_ui, 'ExternalLeadIntegrationApiClient::startMetaOauth', "Meta OAuth start missing")
require(external_ui, 'loadMetaLeadForms', "Meta OAuth must continue into lead-form discovery")
require(external_ui, 'showMetaLeadFormPicker', "Meta lead-form picker missing")
require(external_ui, '"받을 Meta 리드폼 선택"', "Meta picker must be form-oriented, not page-oriented")
require(external_ui, 'form.optString("formId", form.optString("id", ""))', "Meta completion must submit selected lead-form IDs")
require(external_ui, 'ExternalLeadIntegrationApiClient::startGoogleFormsOauth', "Google Forms OAuth start missing")
require(external_ui, 'showGoogleFormPicker', "Google Forms account form picker missing")
require(external_ui, 'connectGoogleForm', "Google Forms direct connection missing")
require(external_ui, 'showGoogleFormsConnections', "Google Forms connection management missing")
require(external_ui, 'CustomTabsIntent', "OAuth must launch in browser custom tabs")
require(external_ui, 'transientSecret = ""', "one-time webhook secret cleanup missing")
forbid(external_ui, 'https://calltag.pagero.kr/connect', "integration UI must not use undeployed /connect")
forbid(external_ui, 'WebView', "provider OAuth must not run in WebView")

# Native API routes for Google OAuth -> forms list -> direct connect -> response sync.
require(external_api, 'X-Inlet-Session', "native integration API must be session scoped")
for route in [
    '/api/calltag/v1/connections',
    '/api/calltag/v1/meta/oauth/start',
    '/api/calltag/v1/meta/oauth/session',
    '/api/calltag/v1/meta/oauth/complete',
    '/api/calltag/v1/google-forms/oauth/start',
    '/api/calltag/v1/google-forms/oauth/session',
    '/api/calltag/v1/google-forms/forms',
    '/api/calltag/v1/google-forms/connect',
    '/api/calltag/v1/google-forms/connections',
    '/api/calltag/v1/google-forms/sync',
]:
    require(external_api, route, f"integration route missing: {route}")
require(external_api, 'GOOGLE_FORMS_ANDROID_RETURN_PATH', "Google Forms Android return path missing")
forbid(external_api, '/api/calltag/v1/keys', "Direct API route must be removed from native integration client")
forbid(external_api, 'SharedPreferences', "provider credentials must not be persisted by Android client")

# Provider callbacks are narrowly scoped to this exported singleTop activity.
require(manifest, 'android:name=".ExternalLeadIntegrationActivity" android:exported="true"', "callback activity must be exported")
require(manifest, 'android:launchMode="singleTop"', "callback must reuse integration activity")
require(manifest, 'android:scheme="calltag" android:host="external-lead" android:path="/meta"', "Meta callback filter missing")
require(manifest, 'android:scheme="calltag" android:host="external-lead" android:path="/google-forms"', "Google Forms callback filter missing")
require(external_ui, '"calltag".equalsIgnoreCase(uri.getScheme())', "deep-link scheme validation missing")
require(external_ui, '"external-lead".equalsIgnoreCase(uri.getHost())', "deep-link host validation missing")
require(external_ui, '"/google-forms".equals(path)', "Google Forms callback dispatch missing")

# Google Forms provider pull is best-effort and canonical queue remains source of truth for Android CRM import.
require(sync, 'ExternalLeadIntegrationApiClient.syncGoogleForms(session)', "Google Forms provider pre-sync missing")
require(sync, 'Google Forms pre-sync skipped', "Google Forms API failure isolation missing")
require(external_ui, 'UniversalLeadSyncManager.requestSync(this, true)', "manual lead refresh missing")
require(external_ui, 'UniversalLeadSyncManager.ACTION_LEADS_UPDATED', "sync result receiver missing")
require(external_ui, 'AuthSessionStore.hasSession(this)', "integration UI must respect login session")

# Meta lead-form picker + direct Google Forms OAuth release.
require(gradle, 'versionCode 2026082703', "Play versionCode must be bumped")
require(gradle, "versionName '0.44.54'", "Play versionName must be bumped")
require(gradle, "androidx.browser:browser:1.8.0", "browser dependency required for OAuth custom tabs")

print(
    "CallTag universal lead contract OK: PII-free pull/ACK, Meta lead-form picker + Google OAuth, "
    "Google Forms picker/API sync, no Apps Script, no test UI, no Direct API UI, v0.44.54"
)
