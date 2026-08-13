const API_BASE='https://inlet-8mr.pages.dev';
const PREFIX='/admin/api/';
const READ_PATHS=new Set([`${PREFIX}partners`,`${PREFIX}partner`]);
const WRITE_PATH=`${PREFIX}settlement-pay`;

export async function handleAdminPayoutBridge(request){
  const url=new URL(request.url);
  const isRead=READ_PATHS.has(url.pathname);
  const isWrite=url.pathname===WRITE_PATH;
  if(!isRead&&!isWrite)return null;

  const assertion=String(request.headers.get('CF-Access-Jwt-Assertion')||'').trim();
  if(!assertion)return json({ok:false,error:'운영 데이터 접근이 잠겨 있습니다.',code:'CALLTAG_ADMIN_ACCESS_REQUIRED'},401);

  if(isRead&&request.method!=='GET')return json({ok:false,error:'Method not allowed.',code:'METHOD_NOT_ALLOWED'},405,{allow:'GET'});
  if(isWrite&&request.method!=='POST')return json({ok:false,error:'Method not allowed.',code:'METHOD_NOT_ALLOWED'},405,{allow:'POST'});

  if(url.pathname===`${PREFIX}partners`){
    const month=validMonth(url.searchParams.get('month'));
    const path=`/api/call/admin/partners${month?`?month=${encodeURIComponent(month)}`:''}`;
    return proxyRead(assertion,path,sanitizePartners);
  }
  if(url.pathname===`${PREFIX}partner`){
    const ownerId=validOwner(url.searchParams.get('ownerId'));
    const month=validMonth(url.searchParams.get('month'));
    if(!ownerId)return json({ok:false,error:'파트너 식별자가 올바르지 않습니다.',code:'CALLTAG_ADMIN_PARTNER_ID_INVALID'},400);
    const path=`/api/call/admin/partner?ownerId=${encodeURIComponent(ownerId)}${month?`&month=${encodeURIComponent(month)}`:''}`;
    return proxyRead(assertion,path,sanitizePartner);
  }
  return proxySettlementPay(request,assertion);
}

async function proxyRead(assertion,path,sanitize){
  try{
    const response=await fetch(`${API_BASE}${path}`,{
      method:'GET',
      headers:{accept:'application/json','cf-access-jwt-assertion':assertion,'x-calllink-client':'calltag-admin-payout-bridge'},
      redirect:'manual',
    });
    const data=await safeJson(response);
    if(!response.ok||data?.ok===false)return json(sanitizeError(data),normalizeStatus(response.status));
    return json(sanitize(data),200);
  }catch{return json({ok:false,error:'관리자 API에 연결하지 못했습니다.',code:'ADMIN_UPSTREAM_UNAVAILABLE'},502)}
}

async function proxySettlementPay(request,assertion){
  const raw=await request.text().catch(()=> '');
  if(!raw||raw.length>4096)return json({ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'},400);
  let body={};try{body=JSON.parse(raw)}catch{return json({ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'},400)}
  const ownerId=validOwner(body?.ownerId);
  const month=validMonth(body?.month);
  const requestId=validRequestId(body?.requestId);
  const expectedAmountKrw=num(body?.expectedAmountKrw);
  if(!ownerId||!month||!requestId||!expectedAmountKrw){
    return json({ok:false,error:'지급요청 정보를 확인해주세요.',code:'CALLTAG_ADMIN_BODY_INVALID'},400);
  }
  try{
    const response=await fetch(`${API_BASE}/api/call/admin/settlement-pay`,{
      method:'POST',
      headers:{
        accept:'application/json',
        'content-type':'application/json',
        origin:'https://calltag.pagero.kr',
        'cf-access-jwt-assertion':assertion,
        'x-calltag-admin-action':'partner.settlement.pay',
        'x-calllink-client':'calltag-admin-payout-bridge',
      },
      body:JSON.stringify({ownerId,month,requestId,expectedAmountKrw}),
      redirect:'manual',
    });
    const data=await safeJson(response);
    if(!response.ok||data?.ok===false)return json(sanitizeError(data),normalizeStatus(response.status));
    return json(sanitizeSettlementResult(data),200);
  }catch{return json({ok:false,error:'관리자 API에 연결하지 못했습니다.',code:'ADMIN_UPSTREAM_UNAVAILABLE'},502)}
}

function sanitizePartners(data){
  const totals=data?.totals||{};
  const rows=Array.isArray(data?.partners)?data.partners.slice(0,500):[];
  return{
    ok:true,
    readOnly:data?.readOnly===true,
    financeWriteEnabled:data?.financeWriteEnabled===true,
    month:validMonth(data?.month),
    totals:{
      partnerCount:num(totals.partnerCount),
      grossSalesKrw:num(totals.grossSalesKrw),
      earnedCommissionKrw:num(totals.earnedCommissionKrw),
      payableAmountKrw:num(totals.payableAmountKrw),
      paidAmountKrw:num(totals.paidAmountKrw),
      payoutRequestCount:num(totals.payoutRequestCount),
      requestedAmountKrw:num(totals.requestedAmountKrw),
    },
    partners:rows.map(x=>{
      const m=x?.month||{};
      return{
        ownerId:owner(x?.ownerId),
        email:safeText(x?.email,320),
        phone:safeText(x?.phone,40),
        referralCode:token(x?.referralCode,20),
        commissionRatePercent:Number(x?.commissionRatePercent)===50?50:20,
        referredCount:num(x?.referredCount),
        activePaidCount:num(x?.activePaidCount),
        month:{
          confirmedCount:num(m.confirmedCount),
          grossSalesKrw:num(m.grossSalesKrw),
          earnedCommissionKrw:num(m.earnedCommissionKrw),
          estimatedCommissionKrw:num(m.estimatedCommissionKrw),
          paidAmountKrw:num(m.paidAmountKrw),
          payableAmountKrw:num(m.payableAmountKrw),
          payoutRequestCount:num(m.payoutRequestCount),
          requestedAmountKrw:num(m.requestedAmountKrw),
          lastRequestedAt:date(m.lastRequestedAt),
          settlementCount:num(m.settlementCount),
          lastPaidAt:date(m.lastPaidAt),
          status:token(m.status,20),
        },
      };
    }).filter(x=>x.ownerId),
    generatedAt:date(data?.generatedAt),
  };
}

function sanitizePartner(data){
  const p=data?.partner||{},m=data?.month||{},profile=data?.payoutProfile||{};
  const commissions=Array.isArray(data?.commissions)?data.commissions.slice(0,300):[];
  const settlements=Array.isArray(data?.settlements)?data.settlements.slice(0,12):[];
  const requests=Array.isArray(data?.payoutRequests)?data.payoutRequests.slice(0,20):[];
  const finance=data?.financeWriteEnabled===true;
  return{
    ok:true,
    financeWriteEnabled:finance,
    partner:{
      ownerId:owner(p.ownerId),email:safeText(p.email,320),phone:safeText(p.phone,40),referralCode:token(p.referralCode,20),
      commissionRatePercent:Number(p.commissionRatePercent)===50?50:20,status:token(p.status,20),referredCount:num(p.referredCount),activePaidCount:num(p.activePaidCount),
    },
    month:{
      value:validMonth(m.value),grossSalesKrw:num(m.grossSalesKrw),earnedCommissionKrw:num(m.earnedCommissionKrw),
      paidAmountKrw:num(m.paidAmountKrw),payableAmountKrw:num(m.payableAmountKrw),payoutRequestCount:num(m.payoutRequestCount),requestedAmountKrw:num(m.requestedAmountKrw),
    },
    payoutRequests:requests.map(x=>({
      requestId:validRequestId(x?.requestId),month:validMonth(x?.month),service:service(x?.service),amountKrw:num(x?.amountKrw),
      status:requestStatus(x?.status),settlementId:token(x?.settlementId,120),requestedAt:date(x?.requestedAt),processedAt:date(x?.processedAt),
    })).filter(x=>x.requestId),
    payoutProfile:profile?.configured===true?finance?{
      configured:true,payoutType:payoutType(profile?.payoutType),accountHolder:safeText(profile?.accountHolder,80),bankName:safeText(profile?.bankName,60),
      accountNumberMasked:maskedAccount(profile?.accountNumberMasked),settlementEmail:safeText(profile?.settlementEmail,320),phone:safeText(profile?.phone,40),
      businessName:safeText(profile?.businessName,120),businessNumberMasked:safeText(profile?.businessNumberMasked,24),taxEmail:safeText(profile?.taxEmail,320),updatedAt:date(profile?.updatedAt),
    }:{configured:true}:{configured:false},
    commissions:commissions.map(x=>({
      id:num(x?.id),referredOwnerId:owner(x?.referredOwnerId),referredEmail:safeText(x?.referredEmail,320),referredPhone:safeText(x?.referredPhone,40),
      productCode:token(x?.productCode,80),baseAmountKrw:num(x?.baseAmountKrw),commissionAmountKrw:num(x?.commissionAmountKrw),
      effectiveRatePercent:num(x?.effectiveRatePercent),status:token(x?.status,24),paid:x?.paid===true,confirmedAt:date(x?.confirmedAt),createdAt:date(x?.createdAt),
    })),
    settlements:settlements.map(x=>({month:validMonth(x?.month),settlementCount:num(x?.settlementCount),commissionCount:num(x?.commissionCount),paidAmountKrw:num(x?.paidAmountKrw),lastPaidAt:date(x?.lastPaidAt)})),
  };
}

function sanitizeSettlementResult(data){
  const s=data?.settlement||{},r=data?.request||{};
  return{ok:true,request:{requestId:validRequestId(r.requestId),service:service(r.service),status:r.status==='paid'?'paid':'',processedAt:date(r.processedAt)},settlement:{
    settlementId:token(s.settlementId,120),partnerOwnerId:owner(s.partnerOwnerId),month:validMonth(s.month),commissionCount:num(s.commissionCount),
    grossSalesKrw:num(s.grossSalesKrw),paidAmountKrw:num(s.paidAmountKrw),status:s.status==='paid'?'paid':'',paidAt:date(s.paidAt),
  }};
}

function sanitizeError(data){
  const out={ok:false,error:safeMessage(data?.error,'관리자 요청에 실패했습니다.'),code:safeCode(data?.code||'ADMIN_REQUEST_FAILED')};
  if(data?.currentAmountKrw!=null)out.currentAmountKrw=num(data.currentAmountKrw);
  if(data?.requestedAmountKrw!=null)out.requestedAmountKrw=num(data.requestedAmountKrw);
  if(data?.requestStatus)out.requestStatus=requestStatus(data.requestStatus);
  if(data?.requestId)out.requestId=validRequestId(data.requestId);
  if(data?.settlementId)out.settlementId=token(data.settlementId,120);
  return out;
}

async function safeJson(response){const text=await response.text().catch(()=> '');if(!text||text.length>524288)return{};try{return JSON.parse(text)}catch{return{}}}
function json(body,status=200,extra={}){return hardened(new Response(JSON.stringify(body),{status,headers:{'content-type':'application/json; charset=utf-8',...extra}}))}
function hardened(response){const h=new Headers(response.headers);h.set('cache-control','no-store, max-age=0');h.set('pragma','no-cache');h.set('referrer-policy','no-referrer');h.set('x-content-type-options','nosniff');h.set('x-frame-options','DENY');h.set('x-robots-tag','noindex, nofollow, noarchive, nosnippet');return new Response(response.body,{status:response.status,statusText:response.statusText,headers:h})}
function normalizeStatus(value){const n=Number(value||500);return n>=400&&n<=599?n:500}
function validOwner(v){const x=String(v||'').trim();return/^[A-Za-z0-9._:-]{3,120}$/.test(x)?x:''}
function owner(v){return validOwner(v)}
function validMonth(v){const x=String(v||'').trim();return/^20\d{2}-(0[1-9]|1[0-2])$/.test(x)?x:''}
function validRequestId(v){const x=String(v||'').trim();return/^ptr_[a-z0-9]+_[a-f0-9]{20}$/i.test(x)?x:''}
function service(v){const x=String(v||'').trim().toUpperCase();return['ALL','CALLTAG','PAGERO'].includes(x)?x:'ALL'}
function requestStatus(v){const x=String(v||'').trim().toLowerCase();return['requested','processing','paid','cancelled','review'].includes(x)?x:'review'}
function payoutType(v){const x=String(v||'').trim().toUpperCase();return['INDIVIDUAL','SOLE_PROPRIETOR','CORPORATION'].includes(x)?x:'INDIVIDUAL'}
function maskedAccount(v){const x=String(v||'').trim();return/^\*{4}-\*{4}-\d{4}$/.test(x)?x:''}
function safeText(v,max){return String(v||'').replace(/[\r\n<>]/g,'').slice(0,max)}
function token(v,max){const x=String(v||'').trim();return/^[A-Za-z0-9._:+-]*$/.test(x)?x.slice(0,max):''}
function num(v){const n=Number(v||0);return Number.isFinite(n)&&n>0?Math.min(Number.MAX_SAFE_INTEGER,Math.trunc(n)):0}
function date(v){const n=Date.parse(String(v||''));return Number.isFinite(n)?new Date(n).toISOString():''}
function safeCode(v){return token(v,100)||'ADMIN_REQUEST_FAILED'}
function safeMessage(v,fallback){const x=safeText(v,240).trim();return x||fallback}
