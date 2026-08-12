const API_BASE='https://inlet-8mr.pages.dev';
const STATUS_PATH='/admin/api/partner-totp-status';
const RESET_PATH='/admin/api/partner-totp-reset';

export async function handlePartnerAdminSecurity(request){
  const url=new URL(request.url);
  if(url.pathname!==STATUS_PATH&&url.pathname!==RESET_PATH)return null;
  const assertion=String(request.headers.get('CF-Access-Jwt-Assertion')||'').trim();
  if(!assertion)return reply(401,{ok:false,error:'운영 데이터 접근이 잠겨 있습니다.',code:'CALLTAG_ADMIN_ACCESS_REQUIRED'});

  if(url.pathname===STATUS_PATH){
    if(request.method!=='GET')return reply(405,{ok:false,error:'Method not allowed.',code:'METHOD_NOT_ALLOWED'});
    const ownerId=validOwner(url.searchParams.get('ownerId'));
    if(!ownerId)return reply(400,{ok:false,error:'파트너 식별자가 올바르지 않습니다.',code:'CALLTAG_ADMIN_PARTNER_ID_INVALID'});
    return proxy(`/api/call/admin/partner-totp-status?ownerId=${encodeURIComponent(ownerId)}`,assertion,{method:'GET'});
  }

  if(request.method!=='POST')return reply(405,{ok:false,error:'Method not allowed.',code:'METHOD_NOT_ALLOWED'});
  const raw=await request.text().catch(()=> '');
  if(!raw||raw.length>2048)return reply(400,{ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'});
  let input={};try{input=JSON.parse(raw)}catch{return reply(400,{ok:false,error:'요청 데이터가 올바르지 않습니다.',code:'CALLTAG_ADMIN_BODY_INVALID'})}
  const ownerId=validOwner(input.ownerId);
  if(!ownerId)return reply(400,{ok:false,error:'파트너 식별자가 올바르지 않습니다.',code:'CALLTAG_ADMIN_PARTNER_ID_INVALID'});
  return proxy('/api/call/admin/partner-totp-reset',assertion,{method:'POST',body:JSON.stringify({ownerId})});
}

async function proxy(path,assertion,init={}){
  try{
    const response=await fetch(`${API_BASE}${path}`,{
      method:init.method||'GET',
      headers:{accept:'application/json','content-type':'application/json','cf-access-jwt-assertion':assertion,'x-calllink-client':'calltag-admin-gateway'},
      body:init.body,
      redirect:'manual',
    });
    const text=(await response.text().catch(()=> '')).slice(0,65536);
    let data={};try{data=text?JSON.parse(text):{}}catch{data={}}
    return reply(response.status,data?.ok===false?{ok:false,error:safe(data.error)||'관리자 요청에 실패했습니다.',code:safeCode(data.code)}:data);
  }catch{return reply(502,{ok:false,error:'관리자 API에 연결하지 못했습니다.',code:'ADMIN_UPSTREAM_UNAVAILABLE'});}
}

function reply(status,body){return new Response(JSON.stringify(body),{status,headers:{'content-type':'application/json; charset=utf-8','cache-control':'no-store, max-age=0','pragma':'no-cache','x-content-type-options':'nosniff','x-frame-options':'DENY','referrer-policy':'no-referrer'}})}
function validOwner(value){const owner=String(value||'').trim();return/^[A-Za-z0-9._:-]{3,120}$/.test(owner)?owner:''}
function safe(value){return String(value||'').replace(/[\r\n<>]/g,' ').trim().slice(0,160)}
function safeCode(value){return String(value||'ADMIN_REQUEST_FAILED').replace(/[^A-Z0-9_:-]/gi,'').slice(0,80)||'ADMIN_REQUEST_FAILED'}
