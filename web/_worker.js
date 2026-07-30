export default {
  async fetch(request, env) {
    const response = await env.ASSETS.fetch(request);
    const contentType = response.headers.get("content-type") || "";

    if (!contentType.includes("text/html")) {
      return response;
    }

    let html = await response.text();

    html = html
      .replace(
        '<h2 class="step-title">네 단계면<br><span>정리가 끝납니다.</span></h2><p class="step-sub">통화 종료 후 태그만 하세요.</p>',
        '<h2 class="step-title">통화 종료 후<br><span>태그만 하세요.</span></h2>'
      )
      .replace(
        '<h2 class="step-title">네 단계면<br><span>정리가 끝납니다.</span></h2>',
        '<h2 class="step-title">통화 종료 후<br><span>태그만 하세요.</span></h2>'
      )
      .replace('<p class="step-sub">통화 종료 후 태그만 하세요.</p>', '');

    const headers = new Headers(response.headers);
    headers.delete("content-length");

    return new Response(html, {
      status: response.status,
      statusText: response.statusText,
      headers
    });
  }
};
