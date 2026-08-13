const PARTNER_API_ORIGIN = 'https://inlet-8mr.pages.dev';

export async function handlePartnerApi(request) {
  const url = new URL(request.url);
  if (!url.pathname.startsWith('/api/partner/')) return null;

  const target = new URL(`${url.pathname}${url.search}`, PARTNER_API_ORIGIN);
  const headers = new Headers(request.headers);
  headers.delete('host');
  headers.set('x-calltag-partner-proxy', '1');

  const init = {
    method: request.method,
    headers,
    redirect: 'manual',
  };
  if (!['GET', 'HEAD'].includes(request.method)) init.body = request.body;

  let upstream;
  try {
    upstream = await fetch(target.toString(), init);
  } catch {
    return new Response(JSON.stringify({
      ok: false,
      error: '정산 서버에 연결하지 못했습니다.',
      code: 'PARTNER_API_UNAVAILABLE',
    }), {
      status: 502,
      headers: {
        'content-type': 'application/json; charset=utf-8',
        'cache-control': 'no-store',
      },
    });
  }

  const responseHeaders = new Headers(upstream.headers);
  responseHeaders.set('cache-control', 'no-store');
  responseHeaders.set('x-calltag-partner-api', 'inlet');
  responseHeaders.delete('access-control-allow-origin');
  responseHeaders.delete('access-control-allow-credentials');
  responseHeaders.delete('access-control-allow-methods');
  responseHeaders.delete('access-control-allow-headers');

  return new Response(upstream.body, {
    status: upstream.status,
    statusText: upstream.statusText,
    headers: responseHeaders,
  });
}
