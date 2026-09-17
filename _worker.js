import core from './_worker-core.js';
import { handleAdminOpsBridge } from './worker/admin-ops-bridge.js';
import { handleAdminPayoutBridge } from './worker/admin-payout-bridge.js';

const SETTLEMENT_FINALIZER='/web/settlement-finalize.js?v=20260813-final1';
const SETTLEMENT_POLISH_STYLE='/web/settlement-polish.css?v=20260813-polish1';
const SETTLEMENT_POLISH_SCRIPT='/web/settlement-polish.js?v=20260813-polish1';
const LANDING_STABILIZER='/assets/calltag-landing-stabilizer-v1.js?v=20260917-stable1';

export default {
  async fetch(request,env,context){
    const adminOpsResponse=await handleAdminOpsBridge(request);
    if(adminOpsResponse)return adminOpsResponse;

    const adminPayoutResponse=await handleAdminPayoutBridge(request);
    if(adminPayoutResponse)return adminPayoutResponse;

    const response=await core.fetch(request,env,context);
    const url=new URL(request.url);
    const isLanding=url.pathname==='/'||url.pathname==='/index.html';
    const isSettlement=/^\/web\/settlement(?:\.html)?\/?$/.test(url.pathname);
    const type=response.headers.get('content-type')||'';
    if((!isLanding&&!isSettlement)||!type.includes('text/html'))return response;

    const headers=new Headers(response.headers);
    ['content-encoding','content-length','etag','last-modified','content-md5','digest'].forEach(name=>headers.delete(name));
    headers.set('cache-control','no-cache, no-store, must-revalidate');
    let body=await response.text();

    if(isLanding){
      headers.set('x-calltag-landing-stabilizer','20260917-stable1');
      if(!body.includes('calltag-landing-stabilizer-v1.js')){
        body=body.replace('</body>',`<script src="${LANDING_STABILIZER}"></script></body>`);
      }
    }

    if(isSettlement){
      headers.set('x-calltag-settlement-finalizer','20260813-polish1');
      if(!body.includes('settlement-polish.css')){
        body=body.replace('</head>',`<link rel="stylesheet" href="${SETTLEMENT_POLISH_STYLE}"></head>`);
      }
      if(!body.includes('settlement-finalize.js')){
        body=body.replace('</body>',`<script src="${SETTLEMENT_FINALIZER}" defer></script></body>`);
      }
      if(!body.includes('settlement-polish.js')){
        body=body.replace('</body>',`<script src="${SETTLEMENT_POLISH_SCRIPT}" defer></script></body>`);
      }
    }

    return new Response(body,{status:response.status,statusText:response.statusText,headers,encodeBody:'automatic'});
  }
};
