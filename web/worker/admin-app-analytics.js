// Read-only GA4 bridge. Only Cloudflare Access approved administrators may use it.
// Secrets remain on the Worker and never enter rendered HTML or analytics responses.
const ROUTE = '/admin/api/app-analytics';
const UPSTREAM = 'https://inlet-8mr.pages.dev/api/call/admin/overview';
const GA_API = 'https://analyticsdata.googleapis.com/v1beta/properties/';
const TOKEN_URL = 'https://oauth2.googleapis.com/token';
const HEADERS = { 'content-type':'application/json; charset=utf-8', 'cache-control':'private, no-store', 'x-content-type-options':'nosniff' };

export async function handleAdminAppAnalytics(request, env) {
  const url = new URL(request.url);
  if (url.pathname !== ROUTE) return null;
  if (request.method !== 'GET') return reply({ok:false,status:'invalid_method'},405);
  const assertion = String(request.headers.get('CF-Access-Jwt-Assertion') || '');
  if (!assertion) return reply({ok:false,status:'unauthorized'},401);
  // Existing backend validates the actual Cloudflare Access assertion.
  // A forged request header by itself must not authorize Google reporting.
  try {
    const auth = await fetch(UPSTREAM,{
      method:'GET',headers:{accept:'application/json',
        'cf-access-jwt-assertion':assertion,'x-calllink-client':'calltag-admin-gateway'},
      redirect:'manual',
    });
    if (!auth.ok) return reply({ok:false,status:'unauthorized'},401);
    const body = await auth.json().catch(()=>null);
    if (body?.ok !== true) return reply({ok:false,status:'unauthorized'},401);
  } catch {
    return reply({ok:false,status:'verification_unavailable'},503);
  }
  const property = String(env?.CALLTAG_GA4_PROPERTY_ID||'').trim();
  const email = String(env?.CALLTAG_GA4_CLIENT_EMAIL||'').trim();
  const key = String(env?.CALLTAG_GA4_PRIVATE_KEY||'').trim().replace(/\\n/g,'\n');
  if (!/^\d{5,}$/.test(property) || !email.endsWith('.gserviceaccount.com') || !key.includes('BEGIN PRIVATE KEY')) {
    return reply({ok:true,available:false,status:'not_configured'});
  }
  try {
    const token = await googleToken(email,key);
    const today = new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Seoul',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
    const reports = [
      {label:'dau',from:'yesterday',to:'yesterday',metric:'activeUsers'},
      {label:'wau',from:'7daysAgo',to:'yesterday',metric:'activeUsers'},
      {label:'mau',from:'30daysAgo',to:'yesterday',metric:'activeUsers'},
      {label:'appOpens7d',from:'7daysAgo',to:'yesterday',metric:'eventCount',event:'calltag_app_open'},
      {label:'newUsers7d',from:'7daysAgo',to:'yesterday',metric:'eventCount',event:'first_open'},
    ];
    const endpoint = GA_API + property + ':runReport';
    const results = await Promise.all(reports.map(async item => {
      const body={dateRanges:[{startDate:item.from,endDate:item.to}],metrics:[{name:item.metric}]};
      if(item.event)body.dimensionFilter={filter:{fieldName:'eventName',stringFilter:{value:item.event,matchType:'EXACT'}}};
      const result=await gaReport(endpoint,token,body);
      return [item.label,safeNumber(result.rows?.[0]?.metricValues?.[0]?.value)];
    }));
    const values=Object.fromEntries(results);
    // Cohort uses a fully matured one-day first-session group. No estimate
    // is shown for missing days or for groups too small to be reported.
    const cohortDate = daysAgoKst(32);
    const cohortReport=await gaReport(endpoint,token,{
      dimensions:[{name:'cohort'},{name:'cohortNthDay'}],
      metrics:[{name:'retentionFraction',expression:'cohortActiveUsers/cohortTotalUsers'}],
      cohortSpec:{cohorts:[{dimension:'firstSessionDate',
        dateRange:{startDate:cohortDate,endDate:cohortDate}}],
        cohortsRange:{startOffset:0,endOffset:30,granularity:'DAILY'}},
    }).catch(()=>null);
    const retention={d1:null,d7:null,d30:null,cohortDate};
    for(const row of cohortReport?.rows||[]){
      const n=Number(row.dimensionValues?.[1]?.value);
      if(![1,7,30].includes(n))continue;
      const fraction=Number(row.metricValues?.[0]?.value);
      if(Number.isFinite(fraction)&&fraction>=0&&fraction<=1)retention['d'+n]=Math.round(fraction*1000)/10;
    }
    // Registered event-scope custom dimension required. Failure is independent
    // of core app usage and deliberately does not fabricate subscriber figures.
    let subscriberActive7d=null;
    try {
      const subscriberReport=await gaReport(endpoint,token,{
        dateRanges:[{startDate:'7daysAgo',endDate:'yesterday'}],
        metrics:[{name:'activeUsers'}],
        dimensionFilter:{andGroup:{expressions:[
          {filter:{fieldName:'eventName',stringFilter:{value:'calltag_app_open',matchType:'EXACT'}}},
          {filter:{fieldName:'customEvent:subscriber_status',stringFilter:{value:'subscriber',matchType:'EXACT'}}},
        ]}},
      });
      subscriberActive7d=safeNumber(subscriberReport.rows?.[0]?.metricValues?.[0]?.value);
    }catch{/* Custom dimension not yet registered, or property restricted. */}
    return reply({ok:true,available:true,status:'ready',asOf:today,timezone:'Asia/Seoul',
      metrics:{...values,subscriberActive7d},retention});
  }catch {
    return reply({ok:true,available:false,status:'report_unavailable'});
  }
}
function reply(body,status=200){return new Response(JSON.stringify(body),{status,headers:HEADERS});}
function safeNumber(raw){
  if(raw===undefined||raw===null||raw==='')return null;
  const n=Number(raw);
  return Number.isSafeInteger(n)&&n>=0?n:null;
}
function daysAgoKst(n){
  const parts=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Seoul',
    year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
  const x=new Date(parts+'T00:00:00Z');
  x.setUTCDate(x.getUTCDate()-n);
  return x.toISOString().slice(0,10);
}
async function googleToken(email,key){
  const header={alg:'RS256',typ:'JWT'};
  const now=Math.floor(Date.now()/1000);
  const claims={iss:email,scope:'https://www.googleapis.com/auth/analytics.readonly',
    aud:TOKEN_URL,iat:now,exp:now+3300};
  const data=base64url(JSON.stringify(header))+'.'+base64url(JSON.stringify(claims));
  const bytes=Uint8Array.from(atob(key.replace(/-----[^-]+-----|\s/g,'')),c=>c.charCodeAt(0));
  const cryptoKey=await crypto.subtle.importKey('pkcs8',bytes,{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['sign']);
  const sig=await crypto.subtle.sign('RSASSA-PKCS1-v1_5',cryptoKey,new TextEncoder().encode(data));
  const assertion=data+'.'+base64url(new Uint8Array(sig));
  const response=await fetch(TOKEN_URL,{method:'POST',
    headers:{'content-type':'application/x-www-form-urlencoded'},
    body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion}).toString()});
  if(!response.ok)throw Error('token_unavailable');
  const json=await response.json();
  if(!json.access_token)throw Error('token_missing');
  return json.access_token;
}
function base64url(data){
  const bytes=typeof data==='string'?new TextEncoder().encode(data):data;
  let text='';
  for(const byte of bytes)text+=String.fromCharCode(byte);
  return btoa(text).replace(/\+/g,'-').replace(/\//g,'_').replace(/=/g,'');
}
async function gaReport(endpoint,token,body){
  const r=await fetch(endpoint,{method:'POST',
    headers:{authorization:'Bearer '+token,'content-type':'application/json'},
    body:JSON.stringify(body)});
  if(!r.ok)throw Error('analytics_report_unavailable');
  return r.json();
}
