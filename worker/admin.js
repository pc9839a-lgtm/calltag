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

  const response=await env.ASSETS.fetch(request);
  return hardened(response);
}

async function handleApi(request,url){
  if(request.method!=='GET')return json({ok:false,error:'Method not allowed.',code:'METHOD_NOT_ALLOWED'},405,{allow:'GET'});
  const assertion=String(request.headers.get('CF-Access-Jwt-Assertion')||'').trim();
  if(!assertion)return json({ok:false,error:'운영 데이터 접근이 잠겨 있습니다.',code:'CALLTAG_ADMIN_ACCESS_REQUIRED'},401);
  let path='';let sanitize=null;
  if(url.pathname===`${API_PREFIX}overview`){path='/api/call/admin/overview';sanitize=sanitizeOverview;}
  else if(url.pathname===`${API_PREFIX}member`){const ownerId=String(url.searchParams.get('ownerId')||'').trim();if(!/^[A-Za-z0-9._:-]{3,120}$/.test(ownerId))return json({ok:false,error:'회원 식별자가 올바르지 않습니다.',code:'CALLTAG_ADMIN_MEMBER_ID_INVALID'},400);path=`/api/call/admin/member?ownerId=${encodeURIComponent(ownerId)}`;sanitize=sanitizeMember;}
  else return json({ok:false,error:'Not found.',code:'NOT_FOUND'},404);
  try{
    const response=await fetch(`${API_BASE}${path}`,{method:'GET',headers:{accept:'application/json','cf-access-jwt-assertion':assertion,'x-calllink-client':'calltag-admin-gateway'},redirect:'manual'});
    const data=await safeJson(response);
    if(!response.ok||data?.ok===false)return json({ok:false,error:safeMessage(data?.error,'관리자 요청에 실패했습니다.'),code:safeCode(data?.code||'ADMIN_REQUEST_FAILED')},normalizeStatus(response.status));
    return json(sanitize(data),200);
  }catch{return json({ok:false,error:'관리자 API에 연결하지 못했습니다.',code:'ADMIN_UPSTREAM_UNAVAILABLE'},502);}
}

function sanitizeOverview(data){const m=data?.metrics||{};const rows=Array.isArray(data?.recentMembers)?data.recentMembers.slice(0,40):[];return{ok:true,readOnly:true,admin:{email:safeText(data?.admin?.email,320)},metrics:{totalMembers:num(m.totalMembers),newMembers7d:num(m.newMembers7d),trialMembers:num(m.trialMembers),activePaid:num(m.activePaid),paymentReview:num(m.paymentReview),partnerPending:num(m.partnerPending)},recentMembers:rows.map(x=>({ownerId:owner(x?.ownerId),email:safeText(x?.email,320),phone:safeText(x?.phone,40),createdAt:date(x?.createdAt),updatedAt:date(x?.updatedAt),trialEndsAt:date(x?.trialEndsAt),referralBonusDays:num(x?.referralBonusDays),subscription:sub(x?.subscription)})).filter(x=>x.ownerId),generatedAt:date(data?.generatedAt)}}
function sanitizeMember(data){const m=data?.member||{},t=data?.trial||null,r=data?.referral||{},p=data?.partner||{},subs=Array.isArray(data?.subscriptions)?data.subscriptions.slice(0,20):[];return{ok:true,readOnly:true,member:{ownerId:owner(m.ownerId),email:safeText(m.email,320),phone:safeText(m.phone,40),createdAt:date(m.createdAt),updatedAt:date(m.updatedAt)},trial:t?{startedAt:date(t.startedAt),endsAt:date(t.endsAt),referralBonusDays:num(t.referralBonusDays)}:null,subscriptions:subs.map(sub).filter(Boolean),referral:{referredCount:num(r.referredCount),wasReferred:r.wasReferred===true},partner:{commissionCount:num(p.commissionCount),pendingAmountKrw:num(p.pendingAmountKrw),confirmedAmountKrw:num(p.confirmedAmountKrw)}}}
function sub(x){if(!x||typeof x!=='object')return null;return{productCode:token(x.productCode,80),channel:token(x.channel,32),status:token(x.status,32),verificationState:token(x.verificationState,32),startedAt:date(x.startedAt),nextBillingAt:date(x.nextBillingAt),expiresAt:date(x.expiresAt),lastVerifiedAt:date(x.lastVerifiedAt),autoRenewing:x.autoRenewing===true}}
async function safeJson(response){const t=await response.text().catch(()=> '');if(!t||t.length>524288)return{};try{return JSON.parse(t)}catch{return{}}}
function json(body,status=200,extra={}){return hardened(new Response(JSON.stringify(body),{status,headers:{'content-type':'application/json; charset=utf-8',...extra}}))}
function text(body,status=200,extra={}){return hardened(new Response(String(body||''),{status,headers:{'content-type':'text/plain; charset=utf-8',...extra}}))}
function hardened(response){const h=new Headers(response.headers);h.set('cache-control','no-store, max-age=0');h.set('pragma','no-cache');h.set('content-security-policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");h.set('referrer-policy','no-referrer');h.set('x-content-type-options','nosniff');h.set('x-frame-options','DENY');h.set('x-robots-tag','noindex, nofollow, noarchive, nosnippet');h.set('permissions-policy','camera=(), microphone=(), geolocation=(), payment=(), usb=(), bluetooth=()');h.set('cross-origin-opener-policy','same-origin');h.set('cross-origin-resource-policy','same-origin');['etag','last-modified','content-length'].forEach(k=>h.delete(k));return new Response(response.body,{status:response.status,statusText:response.statusText,headers:h})}
function owner(v){const x=String(v||'').trim();return/^[A-Za-z0-9._:-]{3,120}$/.test(x)?x:''}function safeText(v,max){return String(v||'').replace(/[\r\n<>]/g,'').slice(0,max)}function token(v,max){return String(v||'').replace(/[^A-Za-z0-9._:-]/g,'').slice(0,max)}function date(v){const p=Date.parse(String(v||''));return Number.isFinite(p)?new Date(p).toISOString():''}function num(v){const x=Number(v||0);return Number.isFinite(x)&&x>0?Math.min(Number.MAX_SAFE_INTEGER,Math.trunc(x)):0}function safeCode(v){return String(v||'').replace(/[^A-Z0-9_:-]/gi,'').slice(0,80)||'ADMIN_REQUEST_FAILED'}function safeMessage(v,f){return String(v||'').replace(/[\r\n<>]/g,' ').trim().slice(0,160)||f}function normalizeStatus(v){const s=Number(v||500);return s>=400&&s<=599?s:502}
