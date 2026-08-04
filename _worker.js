export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const legalRoutes = {
      '/terms': '/terms.html',
      '/privacy': '/privacy.html',
      '/refund': '/refund.html',
      '/support': '/support.html'
    };
    const legalPaths = new Set([
      '/terms','/privacy','/refund','/support',
      '/terms.html','/privacy.html','/refund.html','/support.html'
    ]);
    const isLegal = legalPaths.has(url.pathname);

    let assetRequest = request;
    if (legalRoutes[url.pathname]) {
      const assetUrl = new URL(legalRoutes[url.pathname], url.origin);
      assetRequest = new Request(assetUrl.toString(), {
        method: 'GET',
        headers: request.headers,
        redirect: 'follow'
      });
    }

    const response = await env.ASSETS.fetch(assetRequest);
    const type = response.headers.get('content-type') || '';
    if (!type.includes('text/html')) return response;

    let body = await response.text();
    const headers = new Headers(response.headers);
    ['content-encoding','content-length','etag','last-modified','content-md5','digest'].forEach(name=>headers.delete(name));
    headers.set('content-type','text/html; charset=UTF-8');
    headers.set('cache-control','no-cache, no-store, must-revalidate');

    if (isLegal) {
      body = body
        .replace(/href="\/terms"/g, 'href="/terms.html"')
        .replace(/href="\/privacy"/g, 'href="/privacy.html"')
        .replace(/href="\/refund"/g, 'href="/refund.html"')
        .replace(/href="mailto:roadfor@kakao\.com">고객센터/g, 'href="/support.html">고객센터')
        .replace(/<a[^>]*href="tel:01057669839"[^>]*>[^<]*<\/a>/gi, '')
        .replace(/고객센터:\s*010-5766-9839\s*\/\s*roadfor@kakao\.com/g, '고객센터: roadfor@kakao.com')
        .replace(/전화:\s*010-5766-9839<br>/g, '')
        .replace(/전화:\s*010-5766-9839/g, '')
        .replace(/010[-\s]?5766[-\s]?9839/g, '');
      headers.set('x-calltag-worker','v70-static-legal-links');
      return new Response(body, {
        status: response.status,
        statusText: response.statusText,
        headers,
        encodeBody: 'automatic'
      });
    }

    body = body
      .replace(/<title>[\s\S]*?<\/title>/i, '<title>페이지로와 콜태그 | 고객 접수부터 후속관리까지</title>')
      .replace(/<meta name="description" content="[^"]*"\s*\/?>/i, '<meta name="description" content="페이지로 랜딩페이지에서 받은 고객 문의를 콜태그 앱에 자동 등록하고, 통화·문자·일정·후속관리까지 한 번에 관리합니다." />')
      .replace(/<meta property="og:title" content="[^"]*"\s*\/?>/i, '<meta property="og:title" content="페이지로와 콜태그 | 고객 접수부터 후속관리까지" />')
      .replace(/<meta property="og:description" content="[^"]*"\s*\/?>/i, '<meta property="og:description" content="랜딩페이지 문의가 앱에 바로 등록되고, 통화 후 고객관리와 후속 일정까지 이어집니다." />')
      .replace(/<meta property="og:url" content="[^"]*"\s*\/?>/i, '<meta property="og:url" content="https://calltag.pagero.kr/" />');

    if (!body.includes('/assets/calltag-enhance.css')) {
      body = body.replace('</head>', '<link rel="stylesheet" href="/assets/calltag-enhance.css?v=20260801-37" /></head>');
    }

    if (!body.includes('/assets/calltag-enhance.js')) {
      const scripts = [
        'calltag-enhance.js?v=20260801-37',
        'calltag-copy-fix.js?v=20260801-37',
        'calltag-section-split.js?v=20260801-37',
        'calltag-final-polish.js?v=20260801-37',
        'calltag-interaction-fix.js?v=20260801-37',
        'calltag-benefits-flow.js?v=20260801-37',
        'calltag-message-simple.js?v=20260801-37',
        'calltag-final-fix.js?v=20260801-37',
        'calltag-suite-pricing.js?v=20260801-37',
        'calltag-steady-slider.js?v=20260801-37',
        'calltag-strength-animation.js?v=20260801-37',
        'calltag-pricing-redesign.js?v=20260801-37',
        'calltag-pagero-intro.js?v=20260803-intro11',
        'calltag-pagero-reveal-fix.js?v=20260802-reveal3',
        'calltag-pagero-heading-fix.js?v=20260803-heading1',
        'calltag-pagero-connect-visual.js?v=20260802-connect1',
        'calltag-pagero-industries.js?v=20260802-industries2',
        'calltag-pagero-industries-two-column.js?v=20260802-three-card1',
        'calltag-product-switcher.js?v=20260801-37',
        'calltag-cta-system.js?v=20260802-cta1',
        'calltag-stability-fix.js?v=20260802-stability1',
        'calltag-horizontal-clean.js?v=20260803-compact1',
        'calltag-horizontal-guard.js?v=20260803-pin4',
        'calltag-industry-visual-v5.js?v=20260803-v5',
        'calltag-horizontal-impact.js?v=20260803-impact2',
        'calltag-feature-copy-exact.js?v=20260803-copy3',
        'calltag-section-motion.js?v=20260803-motion2',
        'calltag-site-final-cleanup.js?v=20260804-footer2',
        'calltag-horizontal-live-fix.js?v=20260803-live1',
        'calltag-mobile-clean-v2.js?v=20260804-clean2',
        'calltag-footer-links-v3.js?v=20260804-links3'
      ];
      body = body.replace('</body>', scripts.map(src => `<script src="/assets/${src}"></script>`).join('') + '</body>');
    }

    if (!body.includes('calltag-copy-hard-fix.js')) {
      body = body.replace('</body>', '<script src="/assets/calltag-copy-hard-fix.js?v=20260803-hard1"></script></body>');
    }

    headers.set('x-calltag-worker','v70-static-legal-links');
    return new Response(body, {
      status: response.status,
      statusText: response.statusText,
      headers,
      encodeBody: 'automatic'
    });
  }
};