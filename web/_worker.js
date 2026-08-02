export default {
  async fetch(request, env) {
    const response = await env.ASSETS.fetch(request);
    const type = response.headers.get('content-type') || '';

    if (!type.includes('text/html')) {
      return response;
    }

    let body = await response.text();
    body = body
      .replace(/<title>[\s\S]*?<\/title>/i, '<title>페이지로와 콜태그 | 고객 접수부터 후속관리까지</title>')
      .replace(/<meta name="description" content="[^"]*"\s*\/?>/i, '<meta name="description" content="페이지로 랜딩페이지에서 받은 고객 문의를 콜태그 앱에 자동 등록하고, 통화·문자·일정·후속관리까지 한 번에 관리합니다." />')
      .replace(/<meta property="og:title" content="[^"]*"\s*\/?>/i, '<meta property="og:title" content="페이지로와 콜태그 | 고객 접수부터 후속관리까지" />')
      .replace(/<meta property="og:description" content="[^"]*"\s*\/?>/i, '<meta property="og:description" content="랜딩페이지 문의가 앱에 바로 등록되고, 통화 후 고객관리와 후속 일정까지 이어집니다." />')
      .replace(/<meta property="og:url" content="[^"]*"\s*\/?>/i, '<meta property="og:url" content="https://calltag.pagero.kr/" />');

    if (!body.includes('/assets/calltag-enhance.css')) {
      body = body.replace(
        '</head>',
        '<link rel="stylesheet" href="/assets/calltag-enhance.css?v=20260801-37" /></head>'
      );
    }

    if (!body.includes('/assets/calltag-enhance.js')) {
      body = body.replace(
        '</body>',
        '<script src="/assets/calltag-enhance.js?v=20260801-37"></script><script src="/assets/calltag-copy-fix.js?v=20260801-37"></script><script src="/assets/calltag-section-split.js?v=20260801-37"></script><script src="/assets/calltag-final-polish.js?v=20260801-37"></script><script src="/assets/calltag-interaction-fix.js?v=20260801-37"></script><script src="/assets/calltag-benefits-flow.js?v=20260801-37"></script><script src="/assets/calltag-message-simple.js?v=20260801-37"></script><script src="/assets/calltag-final-fix.js?v=20260801-37"></script><script src="/assets/calltag-suite-pricing.js?v=20260801-37"></script><script src="/assets/calltag-feature-copy-exact.js?v=20260801-37"></script><script src="/assets/calltag-steady-slider.js?v=20260801-37"></script><script src="/assets/calltag-strength-animation.js?v=20260801-37"></script><script src="/assets/calltag-pricing-redesign.js?v=20260801-37"></script><script src="/assets/calltag-pagero-intro-loader.js?v=20260801-37"></script><script src="/assets/calltag-pagero-reveal-fix.js?v=20260802-reveal3"></script><script src="/assets/calltag-pagero-connect-visual.js?v=20260802-connect1"></script><script src="/assets/calltag-pagero-industries.js?v=20260802-industries2"></script><script src="/assets/calltag-pagero-industries-two-column.js?v=20260802-three-card1"></script><script src="/assets/calltag-product-switcher.js?v=20260801-37"></script><script src="/assets/calltag-immersive-motion.js?v=20260802-immersive2"></script><script src="/assets/calltag-horizontal-guard.js?v=20260802-stability1"></script><script src="/assets/calltag-horizontal-story.js?v=20260802-horizontal1"></script><script src="/assets/calltag-horizontal-runtime-fix.js?v=20260802-horizontal2"></script><script src="/assets/calltag-mobile-system.js?v=20260802-mobile1"></script><script src="/assets/calltag-stability-fix.js?v=20260802-stability1"></script></body>'
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
    headers.set('x-calltag-worker', 'v39-mobile-system1');

    return new Response(body, {
      status: response.status,
      statusText: response.statusText,
      headers,
      encodeBody: 'automatic'
    });
  }
};