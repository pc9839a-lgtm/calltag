import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const read = path => readFileSync(path,'utf8');
const root = read('build.gradle');
const gradle = read('app/build.gradle');
const manifest = read('app/src/main/AndroidManifest.xml');
const app = read('app/src/main/java/kr/pagero/calltag/CallTagApplication.java');
const analytics = read('app/src/main/java/kr/pagero/calltag/CallTagAppAnalytics.java');
const ignore = read('.gitignore');
assert(root.includes("id 'com.google.gms.google-services' version"));
assert(gradle.includes("file('google-services.json').isFile()"),'missing optional config gate');
assert(gradle.includes("CALLTAG_ENABLE_ANALYTICS"),'missing explicit activation flag');
assert(gradle.includes("CALLTAG_ANALYTICS_ENABLED"),'missing build gate');
assert(gradle.includes("firebase-analytics"),'missing Analytics SDK');
assert(gradle.includes("firebase-messaging"),'existing FCM SDK must remain');
assert(!gradle.includes("firebase-analytics-ktx"),'removed KTX SDK must not be used');
assert(manifest.includes('firebase_analytics_collection_enabled'),'collection gate missing');
assert(manifest.includes('google_analytics_adid_collection_enabled'),'ad ID collection guard missing');
assert(ignore.includes('app/google-services.json'),'config must be gitignored');
assert(app.includes('startedActivities == 1'),'foreground boundary missing');
assert(app.includes('CallTagAppAnalytics.recordUserOpen(this, activity)'));
assert(analytics.includes('calltag_app_open'),'required event missing');
assert(analytics.includes('subscriber_status'),'subscriber cohort property missing');
assert(analytics.includes('subscriber_plan'),'plan cohort property missing');
assert(analytics.includes('"unknown"'),'unknown state required for unverified entitlements');
assert(analytics.includes('"trial"'),'trial segmentation missing');
assert(analytics.includes('"subscriber"'),'subscriber segmentation missing');
assert(analytics.includes('"granted"'),'admin/legacy grants must not be counted as paid');
assert(analytics.includes('MAX_ENTITLEMENT_AGE_MS'),'stale entitlement must not be assumed paid');
assert(analytics.includes('snapshot.serverChecked'),'subscriber state must be verified by backend');
assert(analytics.includes('setUserProperty'),'GA4 segment user property missing');
assert(read('app/src/main/java/kr/pagero/calltag/FeatureEntitlementStore.java').includes('CallTagAppAnalytics.onEntitlementUpdated(context)'),
  'analytics segment must refresh when server entitlement changes');
assert(read('app/src/main/java/kr/pagero/calltag/FeatureEntitlementStore.java').includes('CallTagAppAnalytics.clearSubscriptionSegment(context)'),
  'account switch must clear previous subscriber segment');
for (const ui of ['PostCallActivity','CallerInfoActivity','GoogleAuthCallbackActivity']) {
  assert(analytics.includes('activity instanceof '+ui),'non-user-launched activity counted');
}
for(const sensitive of ['ownerId','getPhoneNumber','getEmail','getMessage','getMemo','getCallLog']) {
  assert(!analytics.includes(sensitive),'sensitive analytics payload: ' + sensitive);
}
assert(!analytics.includes('setUserId('),'no account owner ID mapping');
console.log('PASS: optional Firebase config, no-PII app opens, retention events, FCM preserved.');
