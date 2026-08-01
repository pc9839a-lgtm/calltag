export default {
  async fetch(request,env){
    const response=await env.ASSETS.fetch(request);
    const type=response.headers.get('content-type')||'';
    const headers=new Headers(response.headers);
    headers.delete('content-length');
    headers.set('cache-control','no-cache, no-store, must-revalidate');
    if(!type.includes('text/html'))return new Response(response.body,{status:response.status,statusText:response.statusText,headers});
    let body=await response.text();
    body=body.replace('<title>콜태그 | 통화 후 고객관리</title>','<title>페이지로와 콜태그 | 고객 접수부터 후속관리까지</title>');
    if(!body.includes('/assets/calltag-enhance.css'))body=body.replace('</head>','<link rel="stylesheet" href="/assets/calltag-enhance.css?v=20260801-35" /></head>');
    if(!body.includes('/assets/calltag-enhance.js'))body=body.replace('</body>','<script src="/assets/calltag-enhance.js?v=20260801-35"></script><script src="/assets/calltag-copy-fix.js?v=20260801-35"></script><script src="/assets/calltag-section-split.js?v=20260801-35"></script><script src="/assets/calltag-final-polish.js?v=20260801-35"></script><script src="/assets/calltag-interaction-fix.js?v=20260801-35"></script><script src="/assets/calltag-benefits-flow.js?v=20260801-35"></script><script src="/assets/calltag-message-simple.js?v=20260801-35"></script><script src="/assets/calltag-final-fix.js?v=20260801-35"></script><script src="/assets/calltag-suite-pricing.js?v=20260801-35"></script><script src="/assets/calltag-mobile-optimize.js?v=20260801-35"></script><script src="/assets/calltag-feature-copy-exact.js?v=20260801-35"></script><script src="/assets/calltag-mobile-hardfix.js?v=20260801-35"></script><script src="/assets/calltag-steady-slider.js?v=20260801-35"></script><script src="/assets/calltag-mobile-final.js?v=20260801-35"></script><script src="/assets/calltag-strength-animation.js?v=20260801-35"></script><script src="/assets/calltag-pricing-redesign.js?v=20260801-35"></script><script src="/assets/calltag-pagero-intro-loader.js?v=20260801-35"></script><script src="/assets/calltag-product-switcher.js?v=20260801-35"></script></body>');
    return new Response(body,{status:response.status,statusText:response.statusText,headers});
  }
};