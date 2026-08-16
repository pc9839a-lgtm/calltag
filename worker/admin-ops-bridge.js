const API_BASE='https://inlet-8mr.pages.dev';
const PREFIX='/admin/api/';

export async function handleAdminOpsBridge(request){
  const url=new URL(request.url);
  const route=url.pathname;
  if(![`${PREFIX}members`,`${PREFIX}member-payments`,`${PREFIX}play-finance-monthly`,`${PREFIX}entitlement`].includes(route))return null;
  const assertion=String(request.headers.get('CF-Access-Jwt-Assertion')||'').trim();
  if(!assertion)return json({ok:false,error:'운영 데이터 접근이 잠겨 있습니다.',code:'CALLTAG_ADMIN_ACCESS_REQUIRED'},401);

  if(route===`${PREFIX}entitlement`)return handleEntitlement(request,url,assertion);
  if(request.method!=='GET')return json({ok:false,error:'Method not allowed.',code:'METHOD_NOT_ALLOWED'},405,{allow:'GET'});

  if(route===`${PREFIX}members`){
    const q=safeQuery(url.searchParams.get('q'));
    const page=pageNumber(url.searchParams.get('page'));
    const path=`/api/call/admin/members?page=${page}${q?`&q=${encodeURIComponent(q)}`:''}`;
    return proxy(assertion,path,sanitizeMembers);
  }
  if(route===`${PREFIX}member-payments`){
    const ownerId=validOwner(url.searchParams.get('ownerId'));
    if(!ownerId)return json({ok:false,error:'회원 식별자가 올바르지 않습니다.',code:'CALLTAG_ADMIN_MEMBER_ID_INVALID'},400);
    return proxy(assertion,`/api/call/admin/member-payments?ownerId=${encodeURIComponent(ownerId)}`,sanitizeMemberPayments);
  }
  const month=validMonth(url.searchParams.get('month'));
  const path=`/api/call/admin/play-finance-monthly${month?`?month=${encodeURIComponent(month)}`:''}`;
  return proxy(assertion,path,sanitizePlayFinanceMonthly);
}

async function handleEntitlement(request,url,assertion){
  if(request.method==='GET'){
    const ownerId=validOwner(url.searchParams.get('ownerId'));
    if(!ownerId)return json({ok:false,error:'회원 식별자가 올바르지 않습니다.',code:'CALLTAG_ADMIN_MEMBER_ID_INVALID'},400);
    return proxy(assertion,`/api/call/admin/entitlement?ownerId=${encodeURIComponent(ownerId)}`,sanitizeEntitlement);
  }
  if(request.method!=='POST')return json({ok:false,error:'Method not allowed.',code:'METHOD_NOT_ALLOWED'},405,{allow:'GET, POST'});
  const raw=await request.text().catch(()=> '');
  if(!raw||raw.length>4096)return json({ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'},400);
  let body={};try{body=JSON.parse(raw)}catch{return json({ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'},400)}
  const ownerId=validOwner(body?.ownerId);
  const action=String(body?.action||'').trim().toLowerCase();
  if(!ownerId||!['grant','revoke'].includes(action))return json({ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'},400);
  const safeBody={ownerId,action};
  if(action==='grant'){
    const scope=String(body?.scope||'').trim().toLowerCase();
    const durationDays=Math.trunc(Number(body?.durationDays||0));
    if(!['call','message','all'].includes(scope)||!Number.isFinite(durationDays)||durationDays<1||durationDays>3660){
      return json({ok:false,error:'이용권 범위 또는 기간이 올바르지 않습니다.',code:'CALLTAG_ADMIN_ENTITLEMENT_INVALID'},400);
    }
    safeBody.scope=scope;
    safeBody.durationDays=durationDays;
    safeBody.note=safeText(body?.note,300);
  }
  return proxyWrite(assertion,'/api/call/admin/entitlement',safeBody,sanitizeEntitlement);
}

async function proxy(assertion,path,sanitize){
  try{
    const response=await fetch(`${API_BASE}${path}`,{
      method:'GET',
      headers:{accept:'application/json','cf-access-jwt-assertion':assertion,'x-calllink-client':'calltag-admin-ops-bridge'},
      redirect:'manual',
    });
    const data=await safeJson(response);
    if(!response.ok||data?.ok===false)return json(sanitizeError(data),normalizeStatus(response.status));
    return json(sanitize(data),200);
  }catch{return json({ok:false,error:'관리자 API에 연결하지 못했습니다.',code:'ADMIN_UPSTREAM_UNAVAILABLE'},502)}
}

async function proxyWrite(assertion,path,body,sanitize){
  try{
    const response=await fetch(`${API_BASE}${path}`,{
      method:'POST',
      headers:{accept:'application/json','content-type':'application/json','cf-access-jwt-assertion':assertion,'x-calllink-client':'calltag-admin-ops-bridge'},
      body:JSON.stringify(body),
      redirect:'manual',
    });
    const data=await safeJson(response);
    if(!response.ok||data?.ok===false)return json(sanitizeError(data),normalizeStatus(response.status));
    return json(sanitize(data),200);
  }catch{return json({ok:false,error:'관리자 API에 연결하지 못했습니다.',code:'ADMIN_UPSTREAM_UNAVAILABLE'},502)}
}

function sanitizeMembers(data){
  const rows=Array.isArray(data?.members)?data.members.slice(0,40):[];
  return{
    ok:true,
    query:safeQuery(data?.query),
    page:pageNumber(data?.page),
    pageSize:Math.min(40,Math.max(1,num(data?.pageSize)||40)),
    total:num(data?.total),
    totalPages:Math.max(1,num(data?.totalPages)),
    members:rows.map(x=>({
      ownerId:owner(x?.ownerId),email:safeText(x?.email,320),phone:safeText(x?.phone,40),createdAt:date(x?.createdAt),updatedAt:date(x?.updatedAt),
      trialEndsAt:date(x?.trialEndsAt),referralBonusDays:num(x?.referralBonusDays),
      subscriptions:(Array.isArray(x?.subscriptions)?x.subscriptions.slice(0,6):[]).map(sub).filter(Boolean),
      adminEntitlement:adminGrant(x?.adminEntitlement),
    })).filter(x=>x.ownerId),
    generatedAt:date(data?.generatedAt),
  };
}

function sanitizeMemberPayments(data){
  const payments=Array.isArray(data?.payments)?data.payments.slice(0,100):[];
  const snapshots=Array.isArray(data?.snapshots)?data.snapshots.slice(0,20):[];
  return{
    ok:true,ownerId:owner(data?.ownerId),exactHistoryAvailable:data?.exactHistoryAvailable===true,
    payments:payments.map(x=>({
      productCode:token(x?.productCode,80),channel:token(x?.channel,32),eventType:token(x?.eventType,24),amountKrw:signedNum(x?.amountKrw),
      amountSource:token(x?.amountSource,48),status:token(x?.status,32),paidAt:date(x?.paidAt),month:validMonth(x?.month),
    })),
    snapshots:snapshots.map(x=>({
      productCode:token(x?.productCode,80),channel:token(x?.channel,32),status:token(x?.status,32),verificationState:token(x?.verificationState,32),
      amountKrw:num(x?.amountKrw),amountSource:token(x?.amountSource,48),startedAt:date(x?.startedAt),expiresAt:date(x?.expiresAt),updatedAt:date(x?.updatedAt),
    })),
    generatedAt:date(data?.generatedAt),
  };
}

function sanitizePlayFinanceMonthly(data){
  const r=data?.report||null;
  return{
    ok:true,available:data?.available===true,status:token(data?.status,40),code:safeCode(data?.code||''),month:validMonth(data?.month),
    months:(Array.isArray(data?.months)?data.months.slice(0,120):[]).map(validMonth).filter(Boolean),backfillRemaining:num(data?.backfillRemaining),
    report:r?{
      month:validMonth(r?.month),currency:token(r?.currency,12),customerNetKrw:num(r?.customerNetKrw),googleFeeKrw:num(r?.googleFeeKrw),playNetKrw:num(r?.playNetKrw),
      partnerConfirmedKrw:num(r?.partnerConfirmedKrw),partnerPaidKrw:num(r?.partnerPaidKrw),partnerUnpaidKrw:num(r?.partnerUnpaidKrw),finalAfterPartnerKrw:num(r?.finalAfterPartnerKrw),
      transactionCount:num(r?.transactionCount),syncedAt:date(r?.syncedAt),basis:token(r?.basis,80),finalBankPayout:r?.finalBankPayout===true,
    }:null,
    generatedAt:date(data?.generatedAt),
  };
}

function sanitizeEntitlement(data){
  return{
    ok:true,
    ownerId:owner(data?.ownerId),
    entitlement:adminGrant(data?.entitlement),
  };
}

function adminGrant(e){
  if(!e||typeof e!=='object')return null;
  return{
    active:e?.active===true,
    status:token(e?.status,24),
    scope:['call','message','all'].includes(String(e?.scope||''))?String(e.scope):'',
    startsAt:date(e?.startsAt),
    expiresAt:date(e?.expiresAt),
    note:safeText(e?.note,300),
    grantedAt:date(e?.grantedAt),
    revokedAt:date(e?.revokedAt),
    updatedAt:date(e?.updatedAt),
  };
}

function sub(x){if(!x||typeof x!=='object')return null;return{productCode:token(x?.productCode,80),channel:token(x?.channel,32),status:token(x?.status,32),verificationState:token(x?.verificationState,32),startedAt:date(x?.startedAt),nextBillingAt:date(x?.nextBillingAt),expiresAt:date(x?.expiresAt),lastVerifiedAt:date(x?.lastVerifiedAt),autoRenewing:x?.autoRenewing===true}}
function sanitizeError(data){return{ok:false,error:safeMessage(data?.error,'관리자 요청에 실패했습니다.'),code:safeCode(data?.code||'ADMIN_REQUEST_FAILED')}}
async function safeJson(response){const text=await response.text().catch(()=> '');if(!text||text.length>1048576)return{};try{return JSON.parse(text)}catch{return{}}}
function json(body,status=200,extra={}){return hardened(new Response(JSON.stringify(body),{status,headers:{'content-type':'application/json; charset=utf-8',...extra}}))}
function hardened(response){const h=new Headers(response.headers);h.set('cache-control','no-store, max-age=0');h.set('pragma','no-cache');h.set('referrer-policy','no-referrer');h.set('x-content-type-options','nosniff');h.set('x-frame-options','DENY');h.set('x-robots-tag','noindex, nofollow, noarchive, nosnippet');return new Response(response.body,{status:response.status,statusText:response.statusText,headers:h})}
function normalizeStatus(value){const n=Number(value||500);return n>=400&&n<=599?n:500}
function safeQuery(v){return String(v||'').trim().replace(/[\r\n<>]/g,'').slice(0,80)}
function pageNumber(v){const n=Math.trunc(Number(v||1));return Number.isFinite(n)?Math.max(1,Math.min(2500,n)):1}
function validOwner(v){const x=String(v||'').trim();return/^[A-Za-z0-9._:-]{3,120}$/.test(x)?x:''}
function owner(v){return validOwner(v)}
function validMonth(v){const x=String(v||'').trim();return/^20\d{2}-(0[1-9]|1[0-2])$/.test(x)?x:''}
function safeText(v,max){return String(v||'').replace(/[\r\n<>]/g,'').slice(0,max)}
function token(v,max){const x=String(v||'').trim();return/^[A-Za-z0-9._:+-]*$/.test(x)?x.slice(0,max):''}
function num(v){const n=Number(v||0);return Number.isFinite(n)&&n>0?Math.min(Number.MAX_SAFE_INTEGER,Math.trunc(n)):0}
function signedNum(v){const n=Number(v||0);return Number.isFinite(n)?Math.max(-Number.MAX_SAFE_INTEGER,Math.min(Number.MAX_SAFE_INTEGER,Math.trunc(n))):0}
function date(v){const n=Date.parse(String(v||''));return Number.isFinite(n)?new Date(n).toISOString():''}
function safeCode(v){return token(v,100)||'ADMIN_REQUEST_FAILED'}
function safeMessage(v,fallback){const x=safeText(v,240).trim();return x||fallback}
