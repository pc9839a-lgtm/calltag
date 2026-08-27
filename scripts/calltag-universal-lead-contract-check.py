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

# Customer source remains first-class CRM data.
require(db, 'values.put("source", source == null ? "" : source.trim());', "insertCustomer must persist source")
require(db, 'cursor.getString(cursor.getColumnIndexOrThrow("source"))', "readCustomer must hydrate source")
forbid(db, 'values.put("source", "");', "insertCustomer must not discard source")

# PageRo stays on its legacy path while universal pull excludes duplicate canonical copies.
require(api, 'excludeSourceType=pagero', "universal pull must exclude PageRo canonical copies")
require(pagero_sync, 'values.put("source", CustomerSourceResolver.PAGERO);', "PageRo source persistence missing")

# Generic external lead delivery is PII-free FCM + signed pull + local receipt + ACK.
require(fcm, '"lead_available".equals(type)', "generic FCM route missing")
require(fcm, '"pagero_lead_available".equals(type)', "PageRo FCM route must remain")
require(sync, 'UniversalLeadApiClient.list(session, after, PAGE_SIZE)', "generic pull missing")
require(sync, 'receipts.markImported(lead.eventId, lead.id, imported.customerId)', "local idempotency receipt missing")
require(sync, 'UniversalLeadApiClient.acknowledgeImported', "generic imported ACK missing")
require(sync, 'values.put("source", sourceLabel);', "generic sync must update customer source")

# Existing E2E-labelled historical leads must still be isolated if encountered.
require(lead, 'E2E_TEST_SOURCE_TYPE = "calltag_e2e_test"', "historical E2E source classification missing")
require(sync, 'if (!e2eTest || created) {', "historical E2E lead isolation missing")

# CRM source UX remains visible and filterable.
for label in ["Meta 광고", "Google Forms", "Webhook", "Direct API", "페이지로"]:
    require(resolver, f'"{label}"', f"normalized CRM source label missing: {label}")
require(resolver, 'public static boolean isExternal(Context context, Customer customer)', "external-source classifier missing")
require(customer_list, 'filterButton("외부 문의")', "external inquiry filter missing")
require(customer_list, 'CustomerSourceBadge.create(getContext(), sourceLabel)', "source badge missing")
require(detail_layout, 'kr.pagero.calltag.CustomerSourceDetailView', "customer detail source summary missing")
require(source_detail, '"유입 채널"', "customer detail channel heading missing")

# Visible More screen has one production integration entry and no test entry.
require(section_more, 'kr.pagero.calltag.MoreSettingsHubView', "visible More settings hub missing")
require(more_hub, 'service.addMenu("외부 문의 연동"', "external lead entry missing")
require(more_hub, 'ExternalLeadIntegrationActivity.class', "external lead entry destination missing")
forbid(more_hub, '외부 문의 수신 테스트', "test entry must not be exposed")
forbid(more_hub, 'ExternalLeadE2eActivity', "test activity must not be referenced")
forbid(manifest, '.ExternalLeadE2eActivity', "test activity must not be registered")
require(menu_installer, '"외부 문의 연동"', "legacy More fallback label missing")
require(application, 'ExternalLeadMenuInstaller.install((MainActivity) activity);', "legacy More fallback installer missing")

# Compact production integration hub: PageRo, Meta, Google Forms and Webhook only.
for channel in ["PageRo", "Meta Lead Ads", "Google Forms", "Webhook"]:
    require(external_ui, f'"{channel}"', f"channel card missing: {channel}")
forbid(external_ui, '"Direct API"', "Direct API must not be exposed in integration UI")
forbid(external_ui, 'createDirectApiKey', "Direct API creation UI must be removed")
require(external_ui, 'PageroConnectionCompactActivity.class', "PageRo must stay native")
require(external_ui, 'ExternalLeadIntegrationApiClient::startMetaOauth', "Meta must start from signed native API")
require(external_ui, 'CustomTabsIntent', "Meta must launch in a reliable browser custom tab")
require(external_ui, 'createWebhookConnection', "Google Forms/Webhook connection creation missing")
require(external_ui, 'webhookSamples', "Google Forms/Webhook status check missing")
require(external_ui, 'updateWebhookMapping', "Google Forms automatic phone mapping missing")
require(external_ui, 'extractGoogleFormId', "Google Forms edit URL parsing missing")
require(external_ui, 'FormApp.openById(CALLTAG_FORM_ID)', "Google Forms standalone Apps Script support missing")
require(external_ui, 'Uri.parse("https://script.new")', "Google Apps Script editor launch missing")
require(external_ui, 'transientSecret = ""', "one-time webhook secret cleanup missing")
for token in ["1 폼", "2 설정", "3 테스트", "4 완료", "Google Form 선택", "1. 코드 복사", "2. Apps Script 열기", "테스트 응답", "연결됨"]:
    require(external_ui, f'"{token}"', f"guided Google Forms step missing: {token}")
require(external_ui, '"e".equals(candidate)', "published/respondent Google Forms links must be rejected instead of misparsed")
forbid(external_ui, 'https://calltag.pagero.kr/connect', "integration UI must not use undeployed /connect")
forbid(external_ui, 'WebView', "integration UI must not embed provider login in WebView")

# Native API client only exposes routes needed by production UI.
require(external_api, 'X-Inlet-Session', "native integration API must be session scoped")
require(external_api, '/api/calltag/v1/connections', "Webhook route missing")
require(external_api, '/api/calltag/v1/meta/oauth/start', "Meta OAuth start route missing")
require(external_api, '/api/calltag/v1/meta/oauth/session', "Meta OAuth session route missing")
require(external_api, '/api/calltag/v1/meta/oauth/complete', "Meta OAuth completion route missing")
require(external_api, 'META_ANDROID_RETURN_PATH', "Meta Android return path missing")
forbid(external_api, '/api/calltag/v1/keys', "Direct API route must be removed from native integration client")
forbid(external_api, 'SharedPreferences', "one-time external integration secrets must not be persisted")

# Meta callback remains narrowly scoped to CallTag.
require(manifest, 'android:name=".ExternalLeadIntegrationActivity" android:exported="true"', "Meta callback activity must be exported")
require(manifest, 'android:launchMode="singleTop"', "Meta callback must reuse integration activity")
require(manifest, 'android:scheme="calltag" android:host="external-lead" android:path="/meta"', "Meta callback filter missing")
require(external_ui, '"calltag".equalsIgnoreCase(uri.getScheme())', "Meta deep-link scheme validation missing")
require(external_ui, '"external-lead".equalsIgnoreCase(uri.getHost())', "Meta deep-link host validation missing")
require(external_ui, '!"/meta".equals(uri.getPath())', "Meta deep-link path validation missing")

# Manual refresh remains wired to real production sync.
require(external_ui, 'UniversalLeadSyncManager.requestSync(this, true)', "manual lead refresh missing")
require(external_ui, 'UniversalLeadSyncManager.ACTION_LEADS_UPDATED', "sync result receiver missing")
require(external_ui, 'AuthSessionStore.hasSession(this)', "integration UI must respect login session")

# Guided Google Forms production release.
require(gradle, 'versionCode 2026082701', "Play versionCode must be bumped for guided Google Forms UX")
require(gradle, "versionName '0.44.52'", "Play versionName must be bumped for guided Google Forms UX")
require(gradle, "androidx.browser:browser:1.8.0", "browser dependency required for Meta custom tabs")

print(
    "CallTag universal lead contract OK: PII-free pull/ACK, compact production integrations, "
    "Meta custom-tab OAuth, guided Google Forms setup, no test UI, no Direct API UI, v0.44.52"
)
