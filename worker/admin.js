const ROOT='/admin';
const API_PREFIX='/admin/api/';
const API_BASE='https://inlet-8mr.pages.dev';
const SHELL_PATH='/admin/shell.dat';

export async function handleCalltagAdmin(request,env){
  const url=new URL(request.url);
  if(url.pathname!==ROOT&&!url.pathname.startsWith(`${ROOT}/`))return null;
  if(url.pathname.startsWith(API_PREFIX))return handleApi(request,url);
  if(!['GET','HEAD'].includes(request.method))return text('Method not allowed.',405,{allow:'GET, HEAD'});

  if(url.pathname===ROOT||url.pathname===`${ROOT}/`){
    const assetUrl=new URL(SHELL_PATH,url.origin);
    const asset=await env.ASSETS.fetch(new Request(assetUrl.toString(),{method:'GET'}));
    const headers=new Headers(asset.headers);
    headers.set('content-type','text/html; charset=UTF-8');
    const body=request.method==='HEAD'?null:asset.body;
    return hardened(new Response(body,{status:asset.status,statusText:asset.statusText,headers}));
  }

  return hardened(await env.ASSETS.fetch(request));
}

async function handleApi(request,url){
  const assertion=String(request.headers.get('CF-Access-Jwt-Assertion')||'').trim();
  if(!assertion)return json({ok:false,error:'운영 데이터 접근이 잠겨 있습니다.',code:'CALLTAG_ADMIN_ACCESS_REQUIRED'},401);

  if(request.method==='GET')return handleReadApi(url,assertion);
  if(request.method==='POST')return handleWriteApi(request,url,assertion);
  return json({ok:false,error:'Method not allowed.',code:'METHOD_NOT_ALLOWED'},405,{allow:'GET, POST'});
}

async function handleReadApi(url,assertion){
  let path='';let sanitize=null;
  if(url.pathname===`${API_PREFIX}overview`){path='/api/call/admin/overview';sanitize=sanitizeOverview;}
  else if(url.pathname===`${API_PREFIX}play-finance`){path='/api/call/admin/play-finance';sanitize=sanitizePlayFinance;}
  else if(url.pathname===`${API_PREFIX}member`){
    const ownerId=validOwner(url.searchParams.get('ownerId'));
    if(!ownerId)return json({ok:false,error:'회원 식별자가 올바르지 않습니다.',code:'CALLTAG_ADMIN_MEMBER_ID_INVALID'},400);
    path=`/api/call/admin/member?ownerId=${encodeURIComponent(ownerId)}`;sanitize=sanitizeMember;
  }else if(url.pathname===`${API_PREFIX}partners`){
    const month=validMonth(url.searchParams.get('month'));
    path=`/api/call/admin/partners${month?`?month=${encodeURIComponent(month)}`:''}`;sanitize=sanitizePartners;
  }else if(url.pathname===`${API_PREFIX}partner`){
    const ownerId=validOwner(url.searchParams.get('ownerId'));const month=validMonth(url.searchParams.get('month'));
    if(!ownerId)return json({ok:false,error:'파트너 식별자가 올바르지 않습니다.',code:'CALLTAG_ADMIN_PARTNER_ID_INVALID'},400);
    path=`/api/call/admin/partner?ownerId=${encodeURIComponent(ownerId)}${month?`&month=${encodeURIComponent(month)}`:''}`;sanitize=sanitizePartner;
  }else return json({ok:false,error:'Not found.',code:'NOT_FOUND'},404);
  return proxyRead(assertion,path,sanitize);
}

async function handleWriteApi(request,url,assertion){
  let path='';let action='';let sanitize=null;
  if(url.pathname===`${API_PREFIX}partner-rate`){path='/api/call/admin/partner-rate';action='partner.rate.update';sanitize=sanitizeRateResult;}
  else if(url.pathname===`${API_PREFIX}settlement-pay`){path='/api/call/admin/settlement-pay';action='partner.settlement.pay';sanitize=sanitizeSettlementResult;}
  else return json({ok:false,error:'Not found.',code:'NOT_FOUND'},404);

  const raw=await request.text().catch(()=> '');
  if(!raw||raw.length>4096)return json({ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'},400);
  let body={};try{body=JSON.parse(raw)}catch{return json({ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'},400)}
  const safeBody=sanitizeWriteBody(action,body);
  if(!safeBody)return json({ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'},400);

  try{
    const response=await fetch(`${API_BASE}${path}`,{
      method:'POST',
      headers:{
        accept:'application/json',
        'content-type':'application/json',
        origin:'https://calltag.pagero.kr',
        'cf-access-jwt-assertion':assertion,
        'x-calltag-admin-action':action,
        'x-calllink-client':'calltag-admin-gateway',
      },
      body:JSON.stringify(safeBody),
      redirect:'manual',
    });
    const data=await safeJson(response);
    if(!response.ok||data?.ok===false)return json(sanitizeError(data),normalizeStatus(response.status));
    return json(sanitize(data),200);
  }catch{return json({ok:false,error:'관리자 API에 연결하지 못했습니다.',code:'ADMIN_UPSTREAM_UNAVAILABLE'},502);}
}

async function proxyRead(assertion,path,sanitize){
  try{
    const response=await fetch(`${API_BASE}${path}`,{method:'GET',headers:{accept:'application/json','cf-access-jwt-assertion':assertion,'x-calllink-client':'calltag-admin-gateway'},redirect:'manual'});
    const data=await safeJson(response);
    if(!response.ok||data?.ok===false)return json(sanitizeError(data),normalizeStatus(response.status));
    return json(sanitize(data),200);
  }catch{return json({ok:false,error:'관리자 API에 연결하지 못했습니다.',code:'ADMIN_UPSTREAM_UNAVAILABLE'},502);}
}

function sanitizeWriteBody(action,body){
  const ownerId=validOwner(body?.ownerId);if(!ownerId)return null;
  if(action==='partner.rate.update'){
    const rate=Number(body?.ratePercent||0);if(rate!==20&&rate!==50)return null;
    return{ownerId,ratePercent:rate};
  }
  if(action==='partner.settlement.pay'){
    const month=validMonth(body?.month);const amount=num(body?.expectedAmountKrw);
    if(!month||!amount)return null;
    return{ownerId,month,expectedAmountKrw:amount};
  }
  return null;
}

function sanitizeOverview(data){
  const m=data?.metrics||{};const rows=Array.isArray(data?.recentMembers)?data.recentMembers.slice(0,40):[];const r=data?.revenueEstimate||{};
  return{ok:true,readOnly:true,admin:{email:safeText(data?.admin?.email,320)},metrics:{totalMembers:num(m.totalMembers),newMembers7d:num(m.newMembers7d),trialMembers:num(m.trialMembers),activePaid:num(m.activePaid),paymentReview:num(m.paymentReview),partnerPending:num(m.partnerPending)},revenueEstimate:{grossMonthlyKrw:num(r.grossMonthlyKrw),googlePlayGrossMonthlyKrw:num(r.googlePlayGrossMonthlyKrw),googlePlayFeeRatePercent:Number(r.googlePlayFeeRatePercent)===15?15:0,googlePlayFeeEstimateKrw:num(r.googlePlayFeeEstimateKrw),netAfterPlayFeeEstimateKrw:num(r.netAfterPlayFeeEstimateKrw),basis:token(r.basis,80),exactSettlement:r.exactSettlement===true},recentMembers:rows.map(x=>{const subscriptions=Array.isArray(x?.subscriptions)?x.subscriptions.slice(0,6).map(sub).filter(Boolean):[];const primary=sub(x?.subscription)||subscriptions[0]||null;return{ownerId:owner(x?.ownerId),email:safeText(x?.email,320),phone:safeText(x?.phone,40),createdAt:date(x?.createdAt),updatedAt:date(x?.updatedAt),trialEndsAt:date(x?.trialEndsAt),referralBonusDays:num(x?.referralBonusDays),subscriptions,subscription:primary}}).filter(x=>x.ownerId),generatedAt:date(data?.generatedAt)};
}
function sanitizePlayFinance(data){const r=data?.report||null;return{ok:true,available:data?.available===true,status:token(data?.status,40),code:safeCode(data?.code||''),report:r?{month:validMonth(r.month),currency:token(r.currency,12),customerNetKrw:num(r.customerNetKrw),googleFeeKrw:num(r.googleFeeKrw),playNetKrw:num(r.playNetKrw),partnerConfirmedKrw:num(r.partnerConfirmedKrw),partnerPaidKrw:num(r.partnerPaidKrw),partnerUnpaidKrw:num(r.partnerUnpaidKrw),finalAfterPartnerKrw:num(r.finalAfterPartnerKrw),transactionCount:num(r.transactionCount),syncedAt:date(r.syncedAt),basis:token(r.basis,80),finalBankPayout:r.finalBankPayout===true}:null,generatedAt:date(data?.generatedAt)}}
function sanitizeMember(data){const m=data?.member||{},t=data?.trial||null,r=data?.referral||{},p=data?.partner||{},subs=Array.isArray(data?.subscriptions)?data.subscriptions.slice(0,20):[];return{ok:true,readOnly:true,member:{ownerId:owner(m.ownerId),email:safeText(m.email,320),phone:safeText(m.phone,40),createdAt:date(m.createdAt),updatedAt:date(m.updatedAt)},trial:t?{startedAt:date(t.startedAt),endsAt:date(t.endsAt),referralBonusDays:num(t.referralBonusDays)}:null,subscriptions:subs.map(sub).filter(Boolean),referral:{referredCount:num(r.referredCount),wasReferred:r.wasReferred===true},partner:{commissionCount:num(p.commissionCount),pendingAmountKrw:num(p.pendingAmountKrw),confirmedAmountKrw:num(p.confirmedAmountKrw)}}}
function sanitizePartners(data){const totals=data?.totals||{};const rows=Array.isArray(data?.partners)?data.partners.slice(0,500):[];return{ok:true,readOnly:data?.readOnly===true,financeWriteEnabled:data?.financeWriteEnabled===true,month:validMonth(data?.month),totals:{partnerCount:num(totals.partnerCount),grossSalesKrw:num(totals.grossSalesKrw),earnedCommissionKrw:num(totals.earnedCommissionKrw),payableAmountKrw:num(totals.payableAmountKrw),paidAmountKrw:num(totals.paidAmountKrw)},partners:rows.map(x=>{const m=x?.month||{};return{ownerId:owner(x?.ownerId),email:safeText(x?.email,320),phone:safeText(x?.phone,40),referralCode:token(x?.referralCode,20),commissionRatePercent:Number(x?.commissionRatePercent)===50?50:20,referredCount:num(x?.referredCount),activePaidCount:num(x?.activePaidCount),month:{confirmedCount:num(m.confirmedCount),grossSalesKrw:num(m.grossSalesKrw),earnedCommissionKrw:num(m.earnedCommissionKrw),estimatedCommissionKrw:num(m.estimatedCommissionKrw),paidAmountKrw:num(m.paidAmountKrw),payableAmountKrw:num(m.payableAmountKrw),settlementCount:num(m.settlementCount),lastPaidAt:date(m.lastPaidAt),status:token(m.status,20)}}}).filter(x=>x.ownerId),generatedAt:date(data?.generatedAt)}}
function sanitizePartner(data){const p=data?.partner||{},m=data?.month||{};const commissions=Array.isArray(data?.commissions)?data.commissions.slice(0,300):[];const settlements=Array.isArray(data?.settlements)?data.settlements.slice(0,12):[];return{ok:true,financeWriteEnabled:data?.financeWriteEnabled===true,partner:{ownerId:owner(p.ownerId),email:safeText(p.email,320),phone:safeText(p.phone,40),referralCode:token(p.referralCode,20),commissionRatePercent:Number(p.commissionRatePercent)===50?50:20,status:token(p.status,20),referredCount:num(p.referredCount),activePaidCount:num(p.activePaidCount)},month:{value:validMonth(m.value),grossSalesKrw:num(m.grossSalesKrw),earnedCommissionKrw:num(m.earnedCommissionKrw),paidAmountKrw:num(m.paidAmountKrw),payableAmountKrw:num(m.payableAmountKrw)},commissions:commissions.map(x=>({id:num(x?.id),referredOwnerId:owner(x?.referredOwnerId),referredEmail:safeText(x?.referredEmail,320),referredPhone:safeText(x?.referredPhone,40),productCode:token(x?.productCode,80),baseAmountKrw:num(x?.baseAmountKrw),commissionAmountKrw:num(x?.commissionAmountKrw),effectiveRatePercent:[20,50].includes(Number(x?.effectiveRatePercent))?Number(x.effectiveRatePercent):num(x?.effectiveRatePercent),status:token(x?.status,24),paid:x?.paid===true,confirmedAt:date(x?.confirmedAt),createdAt:date(x?.createdAt)})),settlements:settlements.map(x=>({month:validMonth(x?.month),settlementCount:num(x?.settlementCount),commissionCount:num(x?.commissionCount),paidAmountKrw:num(x?.paidAmountKrw),lastPaidAt:date(x?.lastPaidAt)}))}}
function sanitizeRateResult(data){return{ok:true,partnerOwnerId:owner(data?.partnerOwnerId),oldRatePercent:Number(data?.oldRatePercent)===50?50:20,commissionRatePercent:Number(data?.commissionRatePercent)===50?50:20,appliesTo:'future_commissions',updatedAt:date(data?.updatedAt)}}
function sanitizeSettlementResult(data){const s=data?.settlement||{};return{ok:true,settlement:{settlementId:token(s.settlementId,120),partnerOwnerId:owner(s.partnerOwnerId),month:validMonth(s.month),commissionCount:num(s.commissionCount),grossSalesKrw:num(s.grossSalesKrw),paidAmountKrw:num(s.paidAmountKrw),status:s.status==='paid'?'paid':'',paidAt:date(s.paidAt)}}}
function sanitizeError(data){const out={ok:false,error:safeMessage(data?.error,'관리자 요청에 실패했습니다.'),code:safeCode(data?.code||'ADMIN_REQUEST_FAILED')};if(data?.currentAmountKrw!=null)out.currentAmountKrw=num(data.currentAmountKrw);if(data?.settlementId)out.settlementId=token(data.settlementId,120);return out}
function sub(x){if(!x||typeof x!=='object')return null;return{productCode:token(x.productCode,80),channel:token(x.channel,32),status:token(x.status,32),verificationState:token(x.verificationState,32),startedAt:date(x.startedAt),nextBillingAt:date(x.nextBillingAt),expiresAt:date(x.expiresAt),lastVerifiedAt:date(x.lastVerifiedAt),autoRenewing:x.autoRenewing===true}}
async function safeJson(response){const t=await response.text().catch(()=> '');if(!t||t.length>524288)return{};try{return JSON.parse(t)}catch{return{}}}
function json(body,status=200,extra={}){return hardened(new Response(JSON.stringify(body),{status,headers:{'content-type':'application/json; charset=utf-8',...extra}}))}
function text(body,status=200,extra={}){return hardened(new Response(String(body||''),{status,headers:{'content-type':'text/plain; charset=utf-8',...extra}}))}
function hardened(response){const h=new Headers(response.headers);h.set('cache-control','no-store, max-age=0');h.set('pragma','no-cache');h.set('content-security-policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");h.set('referrer-policy','no-referrer');h.set('x-content-type-options','nosniff');h.set('x-frame-options','DENY');h.set('x-robots-tag','noindex, nofollow, noarchive, nosnippet');h.set('permissions-policy','camera=(), microphone=(), geolocation=(), payment=(), usb=(), bluetooth=()');h.set('cross-origin-opener-policy','same-origin');h.set('cross-origin-resource-policy','same-origin');['etag','last-modified','content-length'].forEach(k=>h.delete(k));return new Response(response.body,{status:response.status,statusText:response.statusText,headers:h})}
function validOwner(v){const x=String(v||'').trim();return/^[A-Za-z0-9._:-]{3,120}$/.test(x)?x:''}function owner(v){return validOwner(v)}function validMonth(v){const x=String(v||'').trim();return/^20\d{2}-(0[1-9]|1[0-2])$/.test(x)?x:''}function safeText(v,max){return String(v||'').replace(/[\r\n<>]/g,'').slice(0,max)}function token(v,max){return String(v||'').replace(/[^A-Za-z0-9._:-]/g,'').slice(0,max)}function date(v){const p=Date.parse(String(v||''));return Number.isFinite(p)?new Date(p).toISOString():''}function num(v){const x=Number(v||0);return Number.isFinite(x)&&x>0?Math.min(Number.MAX_SAFE_INTEGER,Math.trunc(x)):0}function safeCode(v){return String(v||'').replace(/[^A-Z0-9_:-]/gi,'').slice(0,80)||'ADMIN_REQUEST_FAILED'}function safeMessage(v,f){return String(v||'').replace(/[\r\n<>]/g,' ').trim().slice(0,160)||f}function normalizeStatus(v){const s=Number(v||500);return s>=400&&s<=599?s:502}