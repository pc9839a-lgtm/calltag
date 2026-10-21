import core from './_worker-core.js';
import { serveMobilePolicy } from './worker/mobile-policy.mjs';
import { serveMobileTemplates } from './worker/mobile-templates.mjs';
import { handleAdminOpsBridge } from './worker/admin-ops-bridge.js';
import { handleAdminAppAnalytics } from './worker/admin-app-analytics.js';
import { handleAdminPayoutBridge } from './worker/admin-payout-bridge.js';

const SETTLEMENT_FINALIZER='/web/settlement-finalize.js?v=20260813-final1';
const SETTLEMENT_POLISH_STYLE='/web/settlement-polish.css?v=20260813-polish1';
const SETTLEMENT_POLISH_SCRIPT='/web/settlement-polish.js?v=20260813-polish1';
const LANDING_STABILIZER='/assets/calltag-landing-stabilizer-v1.js?v=20260918-stable2';

export default {
  async fetch(request,env,context){
    const sitemapPath=new URL(request.url).pathname;
    if(/^\/sitemap\/?$/i.test(sitemapPath))return Response.redirect(new URL('/sitemap.xml',request.url).toString(),301);
    // Independent remote policy routes. Preserve all existing admin and
    // settlement/landing handlers for other paths.
    const remotePolicy=await serveMobilePolicy(request,env);
    if(remotePolicy)return remotePolicy;
    const remoteTemplates=await serveMobileTemplates(request,env);
    if(remoteTemplates)return remoteTemplates;
    const appAnalyticsResponse=await handleAdminAppAnalytics(request,env);
    if(appAnalyticsResponse)return appAnalyticsResponse;
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
      headers.set('x-calltag-landing-stabilizer','20260918-stable2');
      if(!body.includes('calltag-landing-stabilizer-v1.js')){
        body=body.replace('</body>',`<script src="${LANDING_STABILIZER}" defer></script></body>`);
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
