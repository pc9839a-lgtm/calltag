export default {
  async fetch(request, env) {
    try {
      const response = await env.ASSETS.fetch(request);
      const type = response.headers.get('content-type') || '';
      const headers = new Headers(response.headers);
      headers.delete('content-length');

      if (!type.includes('text/html')) {
        return new Response(response.body, {
          status: response.status,
          statusText: response.statusText,
          headers
        });
      }

      headers.set('cache-control', 'no-cache, no-store, must-revalidate');
      let body = await response.text();

      if (!body.includes('/assets/calltag-enhance.css')) {
        body = body.replace('</head>', '<link rel="stylesheet" href="/assets/calltag-enhance.css?v=20260802-40" /></head>');
      }

      if (!body.includes('/assets/calltag-enhance.js')) {
        body = body.replace(
          '</body>',
          '<script src="/assets/calltag-enhance.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-copy-fix.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-section-split.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-final-polish.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-interaction-fix.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-benefits-flow.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-message-simple.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-final-fix.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-suite-pricing.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-feature-copy-exact.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-mobile-hardfix.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-steady-slider.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-mobile-final.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-strength-animation.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-pricing-redesign.js?v=20260802-40"></script>' +
          '<script src="/assets/calltag-product-switcher.js?v=20260802-40"></script>' +
          '</body>'
        );
      }

      return new Response(body, {
        status: response.status,
        statusText: response.statusText,
        headers
      });
    } catch (error) {
      return new Response('Temporary service recovery in progress.', {
        status: 503,
        headers: { 'content-type': 'text/plain; charset=utf-8', 'cache-control': 'no-store' }
      });
    }
  }
};