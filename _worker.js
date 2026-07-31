export default {
  async fetch(request, env) {
    const response = await env.ASSETS.fetch(request);
    const contentType = response.headers.get('content-type') || '';
    const headers = new Headers(response.headers);
    headers.delete('content-length');
    headers.set('cache-control', 'no-cache, no-store, must-revalidate');

    if (!contentType.includes('text/html')) {
      return new Response(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers
      });
    }

    let html = await response.text();

    html = html
      .replace(
        '<meta name="description" content="통화가 끝난 뒤 고객을 태그하고 상담 상태, 다음 할 일, 재연락 일정을 관리하는 Android 고객관리 서비스 콜태그." />',
        '<meta name="description" content="통화가 끝난 뒤 고객을 태그하고 다음 할 일과 안내문자까지 처리하는 Android 고객관리 서비스 콜태그." />'
      )
      .replace(
        '<title>콜태그 | 통화 후 고객관리</title>',
        '<title>콜태그 | 통화 후 고객관리와 문자자동화</title>'
      )
      .replace(
        '<h2 class="step-title">네 단계면<br><span>정리가 끝납니다.</span></h2>\n            <p class="step-sub">통화 종료 후 태그만 하세요.</p>',
        '<h2 class="step-title">통화 종료 후<br><span>태그만 하세요.</span></h2>'
      );

    if (!html.includes('href="#messages"')) {
      html = html.replace(
        '<a href="#tasks">기능</a><a href="#reviews">후기</a>',
        '<a href="#tasks">기능</a><a href="#messages">문자</a><a href="#reviews">후기</a>'
      );
    }

    if (!html.includes('.message-mode-grid')) {
      const extraCss = String.raw`
    .message-copy{max-width:470px}.message-copy .section-copy{max-width:430px}
    .message-mode-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.message-mode{min-height:82px;display:flex;flex-direction:column;justify-content:center;padding:14px;border:1px solid var(--line);border-radius:13px;background:#191c22;transition:.3s ease}.message-mode.active{border-color:rgba(59,111,255,.52);background:var(--blue-soft);transform:translateY(-4px)}.message-mode span{color:var(--muted-2);font-size:9px;font-weight:800}.message-mode strong{margin-top:8px;font-size:15px;letter-spacing:-.03em}.message-mode.active span,.message-mode.active strong{color:#fff}
    .message-flow{display:grid;grid-template-columns:1fr 28px 1fr 28px 1fr;align-items:center;margin-top:17px;padding:18px;border:1px solid var(--line);border-radius:15px;background:#13161c}.message-flow-step{min-height:68px;display:flex;flex-direction:column;justify-content:center;padding:0 13px;border-radius:11px;background:#1b1e25}.message-flow-step b{color:var(--blue-2);font-size:10px}.message-flow-step strong{margin-top:7px;font-size:14px}.message-flow-arrow{text-align:center;color:#535965;font-size:18px}
    .message-preview{margin-top:12px;padding:18px;border:1px solid rgba(59,111,255,.28);border-radius:15px;background:linear-gradient(145deg,rgba(59,111,255,.1),#15181e)}.message-preview-top{display:flex;align-items:center;justify-content:space-between;gap:12px}.message-preview-top strong{font-size:14px}.message-preview-top span{padding:5px 8px;border-radius:999px;background:rgba(59,111,255,.16);color:#afbdff;font-size:9px;font-weight:850}.message-bubble{max-width:84%;margin:15px 0 0 auto;padding:14px 16px;border-radius:14px 14px 4px 14px;background:var(--blue);font-size:12px;line-height:1.6}.message-preview-meta{display:flex;justify-content:flex-end;gap:9px;margin-top:10px;color:var(--muted-2);font-size:9px}
    .message-safety{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:12px}.message-safety-item{min-height:64px;display:flex;align-items:center;gap:10px;padding:0 13px;border:1px solid var(--line);border-radius:12px;background:#191c22}.message-safety-item i{width:8px;height:8px;flex:0 0 auto;border-radius:50%;background:var(--green);box-shadow:0 0 0 5px rgba(50,200,121,.09)}.message-safety-item strong{font-size:12px}.message-safety-item span{display:block;margin-top:3px;color:var(--muted-2);font-size:9px}
    @media(max-width:900px){.message-mode-grid{grid-template-columns:repeat(2,1fr)}.message-safety{grid-template-columns:1fr}.message-flow{grid-template-columns:1fr;gap:7px}.message-flow-arrow{transform:rotate(90deg)}}
    @media(max-width:560px){.message-mode-grid{grid-template-columns:repeat(2,1fr)}.message-mode{min-height:72px}.message-bubble{max-width:100%}}
`;
      html = html.replace('</style>', extraCss + '\n  </style>');
    }

    if (!html.includes('id="messages"')) {
      const messageSection = String.raw`
    <section class="section" id="messages">
      <div class="wrap">
        <article class="feature-block reveal">
          <div class="feature-copy message-copy">
            <p class="section-kicker">문자자동화</p>
            <h3>통화 끝.<br>문자도 끝.</h3>
            <p class="section-copy">저장한 문구를 바로 보내거나 필요한 때 예약합니다.</p>
            <div class="feature-points">
              <div class="point-card active"><div class="point-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 5h16v12H8l-4 3V5Z"/></svg></div><div><strong>상황별 자동문자</strong><span>수신 · 발신 · 부재중 · 후속</span></div></div>
              <div class="point-card"><div class="point-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 4h12v16H6zM9 8h6M9 12h6"/></svg></div><div><strong>템플릿 보관함</strong><span>자주 쓰는 문구를 한 번만 저장</span></div></div>
              <div class="point-card"><div class="point-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5v14"/></svg></div><div><strong>안전하게 발송</strong><span>제외 · 중복방지 · 그룹문자</span></div></div>
            </div>
          </div>

          <div class="product-panel">
            <div class="today-header"><h4>문자</h4><span>통화 상황에 맞춰 자동 선택</span></div>
            <div class="message-mode-grid">
              <div class="message-mode active"><span>받은 전화</span><strong>수신</strong></div>
              <div class="message-mode"><span>내가 건 전화</span><strong>발신</strong></div>
              <div class="message-mode"><span>못 받은 전화</span><strong>부재중</strong></div>
              <div class="message-mode"><span>나중에 발송</span><strong>후속</strong></div>
            </div>

            <div class="message-flow">
              <div class="message-flow-step"><b>01</b><strong>통화 종료</strong></div>
              <div class="message-flow-arrow">›</div>
              <div class="message-flow-step"><b>02</b><strong>템플릿 선택</strong></div>
              <div class="message-flow-arrow">›</div>
              <div class="message-flow-step"><b>03</b><strong>즉시 · 예약</strong></div>
            </div>

            <div class="message-preview">
              <div class="message-preview-top"><strong>상담 완료 안내</strong><span>수신 기본</span></div>
              <div class="message-bubble">김민수 고객님, 요청하신 견적 자료를 보내드립니다.</div>
              <div class="message-preview-meta"><span>{고객명} 적용</span><span>보내기 전 확인</span></div>
            </div>

            <div class="message-safety">
              <div class="message-safety-item"><i></i><div><strong>발송 제외</strong><span>보내지 않을 고객</span></div></div>
              <div class="message-safety-item"><i></i><div><strong>중복 방지</strong><span>같은 문자는 한 번</span></div></div>
              <div class="message-safety-item"><i></i><div><strong>그룹 문자</strong><span>대상 확인 후 발송</span></div></div>
            </div>
          </div>
        </article>
      </div>
    </section>
`;
      html = html.replace('<section class="section" id="reviews">', messageSection + '\n    <section class="section" id="reviews">');
    }

    return new Response(html, {
      status: response.status,
      statusText: response.statusText,
      headers
    });
  }
};
