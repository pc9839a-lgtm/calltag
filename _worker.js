export default {
  async fetch(request, env) {
    const response = await env.ASSETS.fetch(request);
    const type = response.headers.get('content-type') || '';

    if (!type.includes('text/html')) {
      return response;
    }

    let body = await response.text();
    body = body.replace(
      '<title>콜태그 | 통화 후 고객관리</title>',
      '<title>페이지로와 콜태그 | 고객 접수부터 후속관리까지</title>'
    );

    if (!body.includes('/assets/calltag-enhance.css')) {
      body = body.replace(
        '</head>',
        '<link rel="stylesheet" href="/assets/calltag-enhance.css?v=20260801-37" /></head>'
      );
    }

    if (!body.includes('/assets/calltag-enhance.js')) {
      body = body.replace(
        '</body>',
        '<script src="/assets/calltag-enhance.js?v=20260801-37"></script><script src="/assets/calltag-copy-fix.js?v=20260801-37"></script><script src="/assets/calltag-section-split.js?v=20260801-37"></script><script src="/assets/calltag-final-polish.js?v=20260801-37"></script><script src="/assets/calltag-interaction-fix.js?v=20260801-37"></script><script src="/assets/calltag-benefits-flow.js?v=20260801-37"></script><script src="/assets/calltag-message-simple.js?v=20260801-37"></script><script src="/assets/calltag-final-fix.js?v=20260801-37"></script><script src="/assets/calltag-suite-pricing.js?v=20260801-37"></script><script src="/assets/calltag-mobile-optimize.js?v=20260801-37"></script><script src="/assets/calltag-feature-copy-exact.js?v=20260801-37"></script><script src="/assets/calltag-mobile-hardfix.js?v=20260801-37"></script><script src="/assets/calltag-steady-slider.js?v=20260801-37"></script><script src="/assets/calltag-mobile-final.js?v=20260801-37"></script><script src="/assets/calltag-strength-animation.js?v=20260801-37"></script><script src="/assets/calltag-pricing-redesign.js?v=20260801-37"></script><script src="/assets/calltag-pagero-intro.js?v=20260801-37"></script><script src="/assets/calltag-pagero-reveal-fix.js?v=20260802-reveal3"></script><script src="/assets/calltag-pagero-connect-visual.js?v=20260802-connect1"></script><script src="/assets/calltag-pagero-industries.js?v=20260802-industries1"></script><script src="/assets/calltag-product-switcher.js?v=20260801-37"></script></body>'
      );
    }

    const headers = new Headers(response.headers);
    headers.delete('content-encoding');
    headers.delete('content-length');
    headers.delete('etag');
    headers.delete('last-modified');
    headers.delete('content-md5');
    headers.delete('digest');
    headers.set('content-type', 'text/html; charset=UTF-8');
    headers.set('cache-control', 'no-cache, no-store, must-revalidate');
    headers.set('x-calltag-worker', 'v37-safe-html-industries1');

    return new Response(body, {
      status: response.status,
      statusText: response.statusText,
      headers,
      encodeBody: 'automatic'
    });
  }
};