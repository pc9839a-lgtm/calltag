// Server-side GA4 reporting for Cloudflare Access administrators only.
// Report values are aggregates; service account credentials never reach the client.
const PATH='/admin/api/app-analytics';
const AUTH_URL='https://inlet-8mr.pages.dev/api/call/admin/overview';
const GA_URL='https://analyticsdata.googleapis.com/v1beta/properties/';
const OAUTH_URL='https://oauth2.googleapis.com/token';
const HEADERS={'content-type':'application/json; charset=utf-8','cache-control':'private, no-store','x-content-type-options':'nosniff'};
const TTL_MS=5*60*1000;
const REPORT_TIMEOUT_MS=8000;
let cached=null;
let inflight=null;

export async function handleAdminAppAnalytics(request,env) {
  const url=new URL(request.url);
  if(url.pathname!==PATH)return null;
  if(request.method!=='GET')return respond({ok:false,status:'invalid_method'},405);
  const jwt=String(request.headers.get('CF-Access-Jwt-Assertion')||'').trim();
  if(!jwt)return respond({ok:false,status:'unauthorized'},401);
  // The operational backend, not the presented JWT's mere presence, decides admin access.
  try{
    const auth=await fetchWithDeadline(AUTH_URL,{method:'GET',
      headers:{accept:'application/json','cf-access-jwt-assertion':jwt,
        'x-calllink-client':'calltag-admin-gateway'},redirect:'manual'});
    if(!auth.ok)return respond({ok:false,status:'unauthorized'},401);
    const body=await auth.json().catch(()=>null);
    if(body?.ok!==true)return respond({ok:false,status:'unauthorized'},401);
  }catch{
    return respond({ok:false,status:'verification_unavailable'},503);
  }

  const property=String(env?.CALLTAG_GA4_PROPERTY_ID||'').trim();
  const email=String(env?.CALLTAG_GA4_CLIENT_EMAIL||'').trim();
  const key=String(env?.CALLTAG_GA4_PRIVATE_KEY||'').trim().replace(/\\n/g,'\n');
  if(!/^[0-9]{5,}$/.test(property)||!email.endsWith('.gserviceaccount.com')||
     !key.includes('BEGIN PRIVATE KEY')){
    return respond({ok:true,available:false,status:'not_configured'});
  }
  const cacheKey=property+'|'+email;
  if(cached?.key===cacheKey&&Date.now()<cached.expiresAt)
    return respond(cached.body);
  if(inflight?.key===cacheKey){
    try{return respond(await inflight.promise);}
    catch{return respond({ok:true,available:false,status:'report_unavailable'});}
  }

  const promise=collectAnalytics(property,email,key);
  inflight={key:cacheKey,promise};
  try{
    const data=await promise;
    if(data.available===true)cached={key:cacheKey,body:data,expiresAt:Date.now()+TTL_MS};
    return respond(data);
  }catch{
    return respond({ok:true,available:false,status:'report_unavailable'});
  }finally{
    if(inflight?.promise===promise)inflight=null;
  }
}

async function collectAnalytics(property,email,key){
  const token=await googleToken(email,key);
  const endpoint=GA_URL+property+':runReport';
  const reports=[
    {field:'dau',from:'yesterday',metric:'activeUsers'},
    {field:'wau',from:'7daysAgo',metric:'activeUsers'},
    {field:'mau',from:'30daysAgo',metric:'activeUsers'},
    {field:'appOpens7d',from:'7daysAgo',metric:'eventCount',event:'calltag_app_open'},
    {field:'newUsers7d',from:'7daysAgo',metric:'newUsers'},
  ];
  const metrics={dau:null,wau:null,mau:null,appOpens7d:null,newUsers7d:null,
    subscriberActive7d:null,trialActive7d:null,freeActive7d:null,grantedActive7d:null};
  await Promise.all(reports.map(async row=>{
    const query={dateRanges:[{startDate:row.from,endDate:'yesterday'}],
      metrics:[{name:row.metric}]};
    if(row.event)query.dimensionFilter={filter:{fieldName:'eventName',
      stringFilter:{value:row.event,matchType:'EXACT'}}};
    try{
      const result=await gaReport(endpoint,token,query);
      metrics[row.field]=finiteCount(result.rows?.[0]?.metricValues?.[0]?.value);
    }catch{/* Missing data or quota failure must never be treated as a zero. */}
  }));
  const retention={d1:null,d7:null,d30:null,cohortDates:{}};
  await Promise.all([1,7,30].map(async day=>{
    const date=kstDaysAgo(day+1);
    retention.cohortDates['d'+day]=date;
    try{
      const result=await gaReport(endpoint,token,{
        dimensions:[{name:'cohort'},{name:'cohortNthDay'}],
        metrics:[{name:'cohortRetentionFraction',
          expression:'cohortActiveUsers/cohortTotalUsers'}],
        cohortSpec:{cohorts:[{dimension:'firstSessionDate',
          dateRange:{startDate:date,endDate:date}}],
          cohortsRange:{endOffset:day,granularity:'DAILY'}},
      });
      const row=(result.rows||[]).find(x=>Number(x.dimensionValues?.[1]?.value)===day);
      const raw=row?.metricValues?.[0]?.value;
      if(raw===undefined||raw===null||raw==='')return;
      const value=Number(raw);
      if(Number.isFinite(value)&&value>=0&&value<=1)
        retention['d'+day]=Math.round(value*1000)/10;
    }catch{/* A report failure cannot change an unknown cohort into 0%. */}
  }));
  try{
    const result=await gaReport(endpoint,token,{
      dateRanges:[{startDate:'7daysAgo',endDate:'yesterday'}],
      dimensions:[{name:'customEvent:subscriber_status'}],
      metrics:[{name:'activeUsers'}],
      dimensionFilter:{filter:{fieldName:'eventName',
        stringFilter:{value:'calltag_app_open',matchType:'EXACT'}}},
    });
    const segments={subscriber:'subscriberActive7d',trial:'trialActive7d',
      free:'freeActive7d',granted:'grantedActive7d'};
    for(const row of result.rows||[]){
      const category=row.dimensionValues?.[0]?.value;
      const target=segments[category];
      if(target)metrics[target]=finiteCount(row.metricValues?.[0]?.value);
    }
  }catch{/* Requires the event-scoped GA4 subscriber_status custom dimension. */}
  if(Object.entries(metrics).filter(([key])=>!key.endsWith('Active7d')||key==='appOpens7d').every(([,value])=>value===null))
    return {ok:true,available:false,status:'no_data'};
  return {ok:true,available:true,status:'ready',timezone:'Asia/Seoul',
    asOf:kstDaysAgo(1),metrics,retention};
}

function respond(data,status=200){return new Response(JSON.stringify(data),{status,headers:HEADERS});}
function finiteCount(raw){
  if(raw===undefined||raw===null||raw==='')return null;
  const n=Number(raw);
  return Number.isSafeInteger(n)&&n>=0?n:null;
}
function kstDaysAgo(days){
  const pieces=new Intl.DateTimeFormat('en-US',{timeZone:'Asia/Seoul',
    year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());
  const lookup=Object.fromEntries(pieces.map(p=>[p.type,p.value]));
  const date=new Date(Date.UTC(Number(lookup.year),Number(lookup.month)-1,Number(lookup.day)));
  date.setUTCDate(date.getUTCDate()-days);
  return date.toISOString().slice(0,10);
}
async function fetchWithDeadline(url,options){
  return fetch(url,{...options,signal:AbortSignal.timeout(REPORT_TIMEOUT_MS)});
}
async function googleToken(email,key){
  const now=Math.floor(Date.now()/1000);
  const header=base64url(JSON.stringify({alg:'RS256',typ:'JWT'}));
  const claims=base64url(JSON.stringify({iss:email,scope:'https://www.googleapis.com/auth/analytics.readonly',
    aud:OAUTH_URL,iat:now,exp:now+3300}));
  const signed=header+'.'+claims;
  const pem=key.replace(/-----[^-]+-----|\s/g,'');
  const bytes=Uint8Array.from(atob(pem),character=>character.charCodeAt(0));
  const privateKey=await crypto.subtle.importKey('pkcs8',bytes,{name:'RSASSA-PKCS1-v1_5',
    hash:'SHA-256'},false,['sign']);
  const signature=await crypto.subtle.sign('RSASSA-PKCS1-v1_5',privateKey,
    new TextEncoder().encode(signed));
  const assertion=signed+'.'+base64url(new Uint8Array(signature));
  const result=await fetchWithDeadline(OAUTH_URL,{method:'POST',
    headers:{'content-type':'application/x-www-form-urlencoded'},
    body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion}).toString()});
  if(!result.ok)throw Error('google_token_unavailable');
  const body=await result.json();
  if(typeof body.access_token!=='string'||!body.access_token)throw Error('google_token_missing');
  return body.access_token;
}
function base64url(data){
  const bytes=typeof data==='string'?new TextEncoder().encode(data):data;
  let text='';
  for(const byte of bytes)text+=String.fromCharCode(byte);
  return btoa(text).replace(/\+/g,'-').replace(/\//g,'_').replace(/=/g,'');
}
async function gaReport(endpoint,token,body){
  const response=await fetchWithDeadline(endpoint,{method:'POST',
    headers:{authorization:'Bearer '+token,'content-type':'application/json'},
    body:JSON.stringify(body)});
  if(!response.ok)throw Error('ga4_report_unavailable');
  return response.json();
}
