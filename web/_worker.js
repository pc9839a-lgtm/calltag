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
        '<meta name="description" content="통화가 끝난 뒤 고객을 태그하고 상담 상태, 다음 할 일, 안내문자와 후속문자까지 관리하는 Android 고객관리 서비스 콜태그." />'
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
        '<a href="#tasks">기능</a><a href="#messages">문자자동화</a><a href="#reviews">후기</a>'
      );
    }

    if (!html.includes('.message-type-grid')) {
      const extraCss = String.raw`
    .message-type-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}.message-type-card{min-height:122px;padding:20px;border:1px solid var(--line);border-radius:16px;background:#1a1d24;transition:.3s ease}.message-type-card:first-child{border-color:rgba(59,111,255,.48);background:var(--blue-soft)}.message-type-card span{display:block;color:var(--blue-2);font-size:11px;font-weight:900}.message-type-card strong{display:block;margin-top:13px;font-size:20px}.message-type-card p{margin:8px 0 0;color:var(--muted-2);font-size:12px;line-height:1.55}.message-preview{margin-top:14px;padding:21px;border:1px solid var(--line);border-radius:16px;background:#13161c}.message-preview-head{display:flex;align-items:center;justify-content:space-between;gap:16px}.message-preview-head strong{font-size:15px}.message-preview-head span{padding:6px 9px;border-radius:999px;background:var(--blue-soft);color:#aab8ff;font-size:10px;font-weight:850}.message-bubble{margin-top:17px;padding:17px;border-radius:14px 14px 4px 14px;background:var(--blue);color:#fff;font-size:13px;line-height:1.65}.message-meta{display:flex;justify-content:space-between;gap:16px;margin-top:12px;color:var(--muted-2);font-size:10px}.automation-tools{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}.automation-tool{min-height:155px;padding:21px;border:1px solid var(--line);border-radius:16px;background:#191c22}.automation-tool b{width:38px;height:38px;display:grid;place-items:center;border-radius:11px;background:var(--blue-soft);color:#aab8ff;font-size:13px}.automation-tool strong{display:block;margin-top:17px;font-size:18px}.automation-tool p{margin:8px 0 0;color:var(--muted-2);font-size:12px;line-height:1.55}.message-note{margin-top:18px;padding:15px 17px;border:1px solid rgba(50,200,121,.23);border-radius:13px;background:rgba(50,200,121,.07);color:#8cdfb2;font-size:12px;line-height:1.6}
    @media(max-width:820px){.message-type-grid,.automation-tools{grid-template-columns:1fr 1fr}}
    @media(max-width:560px){.message-type-grid,.automation-tools{grid-template-columns:1fr}.message-type-card,.automation-tool{min-height:auto}}
`;
      html = html.replace('</style>', extraCss + '\n  </style>');
    }

    if (!html.includes('id="messages"')) {
      const messageSection = String.raw`
    <section class="section" id="messages">
      <div class="wrap">
        <article class="feature-block reveal">
          <div class="feature-copy">
            <p class="section-kicker">문자자동화</p>
            <h3>통화가 끝나면<br>안내문자까지.</h3>
            <p>고객 정리를 마친 뒤 메시지 앱을 다시 열 필요 없이, 저장한 문구를 바로 보내거나 필요한 시점에 후속문자로 연결합니다.</p>
            <div class="feature-points">
              <div class="point-card active"><div class="point-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 5h16v12H8l-4 3V5Z"/></svg></div><div><strong>수신·발신·부재중·후속</strong><span>통화 상황별로 다른 문자 설정</span></div></div>
              <div class="point-card"><div class="point-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 4h12v16H6zM9 8h6M9 12h6"/></svg></div><div><strong>저장형 템플릿</strong><span>자주 쓰는 문구를 저장해 반복 사용</span></div></div>
              <div class="point-card"><div class="point-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 7v5l3 2"/><circle cx="12" cy="12" r="9"/></svg></div><div><strong>즉시 또는 후속문자</strong><span>1일 후·3일 후·직접 선택</span></div></div>
            </div>
          </div>
          <div class="product-panel">
            <div class="today-header"><h4>문자 자동화</h4><span>상황별 기본 템플릿</span></div>
            <div class="message-type-grid">
              <div class="message-type-card"><span>수신</span><strong>상담 완료 안내</strong><p>전화를 받아 상담한 뒤 보내는 기본 안내문자</p></div>
              <div class="message-type-card"><span>발신</span><strong>통화 후 자료 안내</strong><p>내가 건 전화가 연결된 뒤 보내는 메시지</p></div>
              <div class="message-type-card"><span>부재중</span><strong>확인 후 연락 안내</strong><p>받지 못했거나 거절한 전화에 적용</p></div>
              <div class="message-type-card"><span>후속</span><strong>재연락·일정 안내</strong><p>고객 일정과 연결해 정한 시점에 발송</p></div>
            </div>
            <div class="message-preview">
              <div class="message-preview-head"><strong>실제 발송 미리보기</strong><span>수신 템플릿</span></div>
              <div class="message-bubble">김민수 고객님, 오늘 상담드린 콜태그입니다. 요청하신 견적 자료를 보내드립니다. 다음 일정은 8월 3일 오전 10시입니다.</div>
              <div class="message-meta"><span>고객명·상호명·다음일정 자동 치환</span><span>보내기 전 확인</span></div>
            </div>
          </div>
        </article>

        <article class="feature-block reverse reveal" style="margin-top:170px">
          <div class="product-panel">
            <div class="today-header"><h4>발송 전에 자동 확인</h4><span>실수와 중복 방지</span></div>
            <div class="automation-tools">
              <div class="automation-tool"><b>01</b><strong>발송 제외</strong><p>전체 문자 제외, 자동문자 제외, 유형별 제외 고객을 발송 전에 걸러냅니다.</p></div>
              <div class="automation-tool"><b>02</b><strong>중복발송 방지</strong><p>같은 통화·일정·캠페인에서 같은 고객에게 두 번 보내지 않도록 확인합니다.</p></div>
              <div class="automation-tool"><b>03</b><strong>그룹·단체문자</strong><p>고객 그룹을 고르고 중복번호와 제외 대상을 제거한 뒤 최종 인원을 확인합니다.</p></div>
              <div class="automation-tool"><b>04</b><strong>예약·후속문자</strong><p>일정 변경·완료·삭제에 맞춰 연결된 후속문자를 변경하거나 취소합니다.</p></div>
            </div>
            <div class="message-note">문자자동화 기능은 콜태그 안에서 제공됩니다. 별도의 콜링크 앱이나 별도 웹사이트로 분리하지 않습니다.</div>
          </div>
          <div class="feature-copy">
            <p class="section-kicker">한 앱에서 연결</p>
            <h3>고객 기록과 문자가<br>따로 놀지 않습니다.</h3>
            <p>통화 결과, 고객 상태, 다음 일정, 보낼 문구를 같은 고객 타임라인에 연결합니다. 발송 여부와 실패 기록도 고객별로 다시 확인할 수 있습니다.</p>
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
