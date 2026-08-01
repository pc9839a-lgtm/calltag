export default {
  async fetch(request, env) {
    const response = await env.ASSETS.fetch(request);
    const type = response.headers.get('content-type') || '';
    const headers = new Headers(response.headers);
    headers.delete('content-length');
    headers.set('cache-control', 'no-cache, no-store, must-revalidate');

    if (!type.includes('text/html')) {
      return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
    }

    let body = await response.text();
    body = body
      .replace('<title>콜태그 | 통화 후 고객관리</title>', '<title>콜태그 | 통화 후 고객관리와 문자자동화</title>')
      .replace('<meta name="description" content="통화가 끝난 뒤 고객을 태그하고 상담 상태, 다음 할 일, 재연락 일정을 관리하는 Android 고객관리 서비스 콜태그." />', '<meta name="description" content="통화 직후 고객정보, 다음 연락과 안내문자를 관리하는 Android 앱 콜태그." />')
      .replace('<meta property="og:title" content="콜태그 | 통화 후 고객관리, 1명의 고객도 놓치지 않습니다" />', '<meta property="og:title" content="콜태그 | 통화 직후 고객정보와 다음 연락을 남기는 앱" />')
      .replace('<meta property="og:description" content="통화 종료 후 태그만 하세요. 고객 상태와 다음 할 일이 바로 정리됩니다." />', '<meta property="og:description" content="고객 상태와 재연락 날짜를 남기고 안내문자를 바로 보내세요." />');

    if (!body.includes('/assets/calltag-enhance.css')) {
      body = body.replace('</head>', '<link rel="stylesheet" href="/assets/calltag-enhance.css?v=20260801-21" /></head>');
    }
    if (!body.includes('/assets/calltag-enhance.js')) {
      body = body.replace('</body>', '<script src="/assets/calltag-enhance.js?v=20260801-21"></script><script src="/assets/calltag-copy-fix.js?v=20260801-21"></script><script src="/assets/calltag-section-split.js?v=20260801-21"></script><script src="/assets/calltag-final-polish.js?v=20260801-21"></script><script src="/assets/calltag-interaction-fix.js?v=20260801-21"></script><script src="/assets/calltag-benefits-flow.js?v=20260801-21"></script><script src="/assets/calltag-message-simple.js?v=20260801-21"></script><script src="/assets/calltag-final-fix.js?v=20260801-21"></script><script src="/assets/calltag-suite-pricing.js?v=20260801-21"></script><script src="/assets/calltag-mobile-optimize.js?v=20260801-21"></script><script src="/assets/calltag-feature-copy-exact.js?v=20260801-21"></script><script src="/assets/calltag-mobile-hardfix.js?v=20260801-21"></script><script src="/assets/calltag-steady-slider.js?v=20260801-21"></script><script src="/assets/calltag-mobile-final.js?v=20260801-21"></script></body>');
    }

    return new Response(body, { status: response.status, statusText: response.statusText, headers });
  }
};