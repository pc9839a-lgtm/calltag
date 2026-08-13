import core from './_worker-core.js';
import { handleAdminOpsBridge } from './worker/admin-ops-bridge.js';
import { handleAdminPayoutBridge } from './worker/admin-payout-bridge.js';

const SETTLEMENT_FINALIZER='/web/settlement-finalize.js?v=20260813-final1';

export default {
  async fetch(request,env,context){
    const adminOpsResponse=await handleAdminOpsBridge(request);
    if(adminOpsResponse)return adminOpsResponse;

    const adminPayoutResponse=await handleAdminPayoutBridge(request);
    if(adminPayoutResponse)return adminPayoutResponse;

    const response=await core.fetch(request,env,context);
    const url=new URL(request.url);
    const isSettlement=/^\/web\/settlement(?:\.html)?\/?$/.test(url.pathname);
    const type=response.headers.get('content-type')||'';
    if(!isSettlement||!type.includes('text/html'))return response;

    const headers=new Headers(response.headers);
    ['content-encoding','content-length','etag','last-modified','content-md5','digest'].forEach(name=>headers.delete(name));
    headers.set('cache-control','no-cache, no-store, must-revalidate');
    headers.set('x-calltag-settlement-finalizer','20260813-final1');
    let body=await response.text();
    if(!body.includes('settlement-finalize.js')){
      body=body.replace('</body>',`<script src="${SETTLEMENT_FINALIZER}" defer></script></body>`);
    }
    return new Response(body,{status:response.status,statusText:response.statusText,headers,encodeBody:'automatic'});
  }
};