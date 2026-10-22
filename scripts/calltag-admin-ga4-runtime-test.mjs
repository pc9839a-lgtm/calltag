import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {generateKeyPairSync,webcrypto} from 'node:crypto';

const source=readFileSync('worker/admin-app-analytics.js','utf8');
const mod=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'));
const {privateKey}=generateKeyPairSync('rsa',{modulusLength:2048,
  privateKeyEncoding:{type:'pkcs8',format:'pem'},
  publicKeyEncoding:{type:'spki',format:'pem'}});
if(!globalThis.crypto)globalThis.crypto=webcrypto;
const endpoint='https://analyticsdata.googleapis.com/';
let authCalls=0,tokenCalls=0,gaCalls=0,gaQueries=[];
let denyAuth=false,failReports=false;
globalThis.fetch=async (url,opts={})=>{
  if(String(url).includes('/api/call/admin/overview')){
    authCalls++;
    return new Response(JSON.stringify({ok:!denyAuth}),{status:denyAuth?403:200});
  }
  if(String(url).includes('/token')){
    tokenCalls++;
    assert(opts.body.includes('grant_type='),'OAuth JWT grant expected');
    return new Response(JSON.stringify({access_token:'mock-oauth-token'}),{status:200});
  }
  if(String(url).startsWith(endpoint)){
    gaCalls++;
    assert.equal(opts.headers.authorization,'Bearer mock-oauth-token');
    const query=JSON.parse(opts.body);
    gaQueries.push(query);
    if(failReports)return new Response('{}',{status:503});
    const metric=query.metrics[0].name;
    if(query.cohortSpec){
      assert.equal(metric,'cohortRetentionFraction','Google documented metric alias');
      const day=query.cohortSpec.cohortsRange.endOffset;
      const rows=[
        {dimensionValues:[{value:'cohort_0'},{value:'0000'}],metricValues:[{value:'1'}]},
        {dimensionValues:[{value:'cohort_0'},{value:String(day).padStart(4,'0')}],
          metricValues:[{value:String(day===1?0.51:day===7?0.27:0.125)}]},
      ];
      return new Response(JSON.stringify({rows}),{status:200});
    }
    if(query.dimensions?.[0]?.name==='customEvent:subscriber_status'){
      return new Response(JSON.stringify({rows:[
        {dimensionValues:[{value:'subscriber'}],metricValues:[{value:'11'}]},
        {dimensionValues:[{value:'trial'}],metricValues:[{value:'6'}]},
        {dimensionValues:[{value:'free'}],metricValues:[{value:'12'}]},
        {dimensionValues:[{value:'granted'}],metricValues:[{value:'3'}]},
      ]}),{status:200});
    }
    const count=metric==='newUsers'?'7':metric==='eventCount'?'83'
      :query.dateRanges[0].startDate==='yesterday'?'15'
      :query.dateRanges[0].startDate==='7daysAgo'?'42':'100';
    return new Response(JSON.stringify({rows:[{metricValues:[{value:count}]}]}),{status:200});
  }
  throw new Error('Unexpected external request: '+url);
};

const url='https://calltag.pagero.kr/admin/api/app-analytics';
const anonymous=new Request(url);
const session=new Request(url,{headers:{'CF-Access-Jwt-Assertion':'mock-cf-jwt'}});
const emptyEnv={};
const env={CALLTAG_GA4_PROPERTY_ID:'1234567',
  CALLTAG_GA4_CLIENT_EMAIL:'reader@example.iam.gserviceaccount.com',
  CALLTAG_GA4_PRIVATE_KEY:privateKey};
const read=async (request,configured=env)=>{
  const response=await mod.handleAdminAppAnalytics(request,configured);
  const json=await response.json();
  assert.equal(response.headers.get('cache-control'),'private, no-store');
  assert(!JSON.stringify(json).includes('BEGIN PRIVATE KEY'));
  assert(!JSON.stringify(json).includes('mock-oauth-token'));
  return [response,json];
};

let [res,data]=await read(anonymous);
assert.equal(res.status,401); assert.equal(authCalls,0);
[res,data]=await read(new Request(url,{method:'POST',headers:{'CF-Access-Jwt-Assertion':'jwt'}}));
assert.equal(res.status,405); assert.equal(authCalls,0);
denyAuth=true;
[res,data]=await read(session);
assert.equal(res.status,401); assert.equal(tokenCalls,0);
denyAuth=false;
[res,data]=await read(session,emptyEnv);
assert.equal(data.status,'not_configured');assert.equal(data.available,false);
assert.equal(tokenCalls,0);
[res,data]=await read(session);
assert.equal(res.status,200);assert.equal(data.available,true);assert.equal(data.status,'ready');
assert.equal(data.metrics.newUsers7d,7,'distinct new users should not be first_open event count');
assert.equal(data.metrics.appOpens7d,83,'foreground opens from explicit custom event');
assert.equal(data.metrics.dau,15);assert.equal(data.metrics.wau,42);assert.equal(data.metrics.mau,100);
assert.equal(data.metrics.subscriberActive7d,11);assert.equal(data.metrics.trialActive7d,6);
assert.equal(data.metrics.freeActive7d,12);assert.equal(data.metrics.grantedActive7d,3);
assert.equal(data.retention.d1,51);assert.equal(data.retention.d7,27);
assert.equal(data.retention.d30,12.5);
assert.match(data.asOf,/^\d{4}-\d{2}-\d{2}$/);
assert.equal(Object.keys(data.retention.cohortDates).length,3);
assert(gaQueries.some(x=>x.metrics[0].name==='newUsers'));
assert(!gaQueries.some(x=>x.metrics[0].name==='eventCount'&&x.dimensionFilter?.filter?.stringFilter?.value==='first_open'));
const count=gaCalls;
[res,data]=await read(session);
assert.equal(data.available,true);
assert.equal(gaCalls,count,'authenticated 5-minute cache should avoid duplicate Google API calls');
assert.equal(authCalls,4,'Access must be checked on EVERY request');
failReports=true;
[res,data]=await read(session,{...env,CALLTAG_GA4_PROPERTY_ID:'7654321'});
assert.equal(data.available,false,'no report data must not become zeroes');
assert.equal(data.status,'no_data');
console.log('PASS: denied admin, no configuration, GA4 verified metrics, 4 subscriber segments, D1/D7/D30, cache and no-data');
