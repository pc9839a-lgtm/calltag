"""Check generated Firebase resources and the merged release manifest after the build."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
build = root / 'app' / 'build'
android = '{http://schemas.android.com/apk/res/android}'
manifest = next(build.glob('intermediates/merged_manifests/release/**/AndroidManifest.xml'))
tree = ET.parse(manifest).getroot()
permissions = {e.get(android + 'name') for e in tree.findall('uses-permission')}
assert 'android.permission.READ_CALL_LOG' in permissions
assert 'android.permission.SEND_SMS' in permissions
assert not any('AD_ID' in p or 'AD_SERVICES' in p or 'ADVERTISING' in p for p in permissions)
assert not any('ADServices' in p or 'ACCESS_ADSERVICES' in p for p in permissions)
app = tree.find('application')
meta = {e.get(android + 'name'): e.get(android + 'value') for e in app.findall('meta-data')}
assert meta['firebase_analytics_collection_enabled'] == 'true'
assert meta['firebase_crashlytics_collection_enabled'] == 'true'
assert meta['google_analytics_adid_collection_enabled'] == 'false'
for key in ('ad_storage', 'ad_user_data', 'ad_personalization_signals'):
    assert meta['google_analytics_default_allow_' + key] == 'false'
providers = {e.get(android + 'name') for e in app.findall('provider')}
assert 'com.google.firebase.provider.FirebaseInitProvider' in providers
assert meta.get('com.google.firebase.components:com.google.firebase.analytics.connector.internal.AnalyticsConnectorRegistrar') is not None or any(
    e.get(android + 'name') == 'com.google.firebase.components.ComponentDiscoveryService'
    and any('CrashlyticsRegistrar' in m.get(android + 'name', '') for m in e.findall('meta-data'))
    for e in app.findall('service'))
values = {}
for file in build.glob('generated/res/google-services/release/values/values.xml'):
    values.update({e.get('name'): e.text for e in ET.parse(file).getroot()})
assert values.get('google_app_id') == '1:352819510264:android:4417a574453ae5b289340d'
assert values.get('google_api_key') and values.get('project_id')
mapping = (build / 'outputs/mapping/release/mapping.txt').read_text(encoding='utf-8')
for name in ('com.google.firebase.analytics.FirebaseAnalytics',
             'com.google.firebase.crashlytics.FirebaseCrashlytics',
             'kr.pagero.calltag.CallTagFirebaseTelemetry'):
    assert name + ' -> ' in mapping, 'Missing release SDK class: ' + name
assert any('com.google.firebase.crashlytics.mapping_file_id' in p.read_text(encoding='utf-8')
           or 'com.crashlytics.android.build_id' in p.read_text(encoding='utf-8')
           for p in build.glob('generated/**/values/*.xml')), 'Missing Crashlytics build ID'
result = {'package': 'kr.pagero.calltag', 'analytics': True, 'crashlytics': True,
          'sameFirebaseAppAs04464': True, 'advertisingIdCollection': False,
          'advertisingPermissionsAbsent': True, 'releaseCollectionEnabled': True,
          'startupProviderPresent': True, 'mappingAndBuildIdPresent': True,
          'cloudEventReceiptVerified': False}
(build / 'outputs/firebase-release-verification.json').write_text(
    json.dumps(result, indent=2), encoding='utf-8')
print(json.dumps(result))
