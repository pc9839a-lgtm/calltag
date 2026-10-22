import assert from 'node:assert/strict';
import{readFileSync}from'node:fs';
import{Script}from'node:vm';
const read=p=>readFileSync(p,'utf8');
for(const p of ['worker/admin-app-analytics.js','admin/app-analytics.js']){
 assert.equal(read(p),read('web/'+p),'production parity '+p);
}
for(const p of ['_worker.js','web/_worker.js']){
 assert(read(p).includes("handleAdminAppAnalytics(request,env)"),'GA4 route before generic admin handler');
}
const html=read('web/admin/shell.dat');
assert(html.includes('id="appAnalyticsPanel"')&&html.includes('id="appRetentionD30"'));
assert(html.includes('id="appSubscriberActive7d"'));
assert(html.includes('app-analytics.js?v=20261022-ga4-2'));
const front=read('admin/app-analytics.js');
assert(front.includes('panel.hidden=true'),'unconfigured GA4 should be invisible');
const server=read('worker/admin-app-analytics.js');
assert(server.includes("CF-Access-Jwt-Assertion"),'Access guard');
assert(server.includes("auth.ok"),'verify actual upstream admin session');
for(const name of ['CALLTAG_GA4_PROPERTY_ID','CALLTAG_GA4_CLIENT_EMAIL','CALLTAG_GA4_PRIVATE_KEY'])
 assert(server.includes(name),'missing runtime secret '+name);
assert(server.includes('cohortNthDay')&&server.includes('subscriber_status'));
assert(server.includes("metric:'newUsers'"),'newUsers should be a GA4 distinct-user metric');
assert(server.includes("name:'cohortRetentionFraction'"),'official cohort retention metric alias required');
assert(server.includes('retention.cohortDates'),'independent mature cohorts required');
assert(server.includes("AbortSignal.timeout"),'outbound timeouts required');
assert(server.includes('TTL_MS'),'bounded report cache required');
for (const id of ['appTrialActive7d','appFreeActive7d','appGrantedActive7d','appAnalyticsAsOf']) {
  assert(html.includes('id="'+id+'"'),'missing subscriber segment: '+id);
}
assert(front.includes('trialActive7d')&&front.includes('freeActive7d')&&front.includes('grantedActive7d'));
assert(!server.includes('localStorage'),'never store private credentials');
assert(!front.includes('PRIVATE_KEY'),'no API credential in browser');
new Script(front);
new Script(server.replace('export async function handleAdminAppAnalytics','async function handleAdminAppAnalytics'));
console.log('PASS: authenticated GA4 reporting bridge, privacy boundaries, hidden empty UI, asset parity');
