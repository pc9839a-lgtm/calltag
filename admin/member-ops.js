(() => {
  const rowsEl = document.getElementById('memberRows');
  const emptyEl = document.getElementById('emptyMembers');
  const searchInput = document.getElementById('memberSearchInput');
  const searchButton = document.getElementById('memberSearchButton');
  const resultCount = document.getElementById('memberResultCount');
  const prevButton = document.getElementById('memberPrev');
  const nextButton = document.getElementById('memberNext');
  const pageInfo = document.getElementById('memberPageInfo');
  const refreshButton = document.getElementById('refreshButton');
  const drawer = document.getElementById('memberDetail');
  const backdrop = document.getElementById('detailBackdrop');
  const detailTitle = document.getElementById('detailTitle');
  const detailId = document.getElementById('detailOwnerId');
  const detailBody = document.getElementById('detailBody');
  if (!rowsEl || !searchInput || !searchButton) return;

  const products = {
    all_monthly: '통합권',
    call_monthly: '전화관리',
    message_monthly: '문자자동화',
    pagero_monthly: '페이지로 클래식',
    pagero_pro_monthly: '페이지로 프로',
    pagero_domain_monthly: 'SSL',
  };
  const channels = { google_play: 'Google Play', play: 'Google Play', web: '웹 결제' };
  const state = { page: 1, totalPages: 1, query: '', loading: false };

  searchButton.addEventListener('click', () => runSearch());
  searchInput.addEventListener('keydown', (event) => { if (event.key === 'Enter') runSearch(); });
  prevButton?.addEventListener('click', () => { if (state.page > 1) loadMembers(state.page - 1); });
  nextButton?.addEventListener('click', () => { if (state.page < state.totalPages) loadMembers(state.page + 1); });
  refreshButton?.addEventListener('click', () => setTimeout(() => loadMembers(state.page), 80));

  setTimeout(() => loadMembers(1), 60);

  function runSearch() {
    state.query = String(searchInput.value || '').trim().slice(0, 80);
    loadMembers(1);
  }

  async function loadMembers(page) {
    if (state.loading) return;
    state.loading = true;
    searchButton.disabled = true;
    try {
      const url = `/admin/api/members?page=${Math.max(1, page)}${state.query ? `&q=${encodeURIComponent(state.query)}` : ''}`;
      const response = await fetch(url, { method: 'GET', cache: 'no-store', credentials: 'same-origin', headers: { accept: 'application/json' } });
      const data = await response.json().catch(() => ({}));
      if (!response.ok || data?.ok === false) throw new Error(data?.error || '회원 목록을 불러오지 못했습니다.');
      state.page = Number(data.page || 1);
      state.totalPages = Math.max(1, Number(data.totalPages || 1));
      renderMembers(Array.isArray(data.members) ? data.members : []);
      if (resultCount) resultCount.textContent = `${number(data.total)}명`;
      if (pageInfo) pageInfo.textContent = `${state.page} / ${state.totalPages}`;
      if (prevButton) prevButton.disabled = state.page <= 1;
      if (nextButton) nextButton.disabled = state.page >= state.totalPages;
    } catch (error) {
      rowsEl.replaceChildren();
      if (emptyEl) { emptyEl.hidden = false; emptyEl.textContent = String(error?.message || '회원 목록을 불러오지 못했습니다.'); }
    } finally {
      state.loading = false;
      searchButton.disabled = false;
    }
  }

  function renderMembers(items) {
    rowsEl.replaceChildren();
    if (emptyEl) { emptyEl.hidden = items.length > 0; emptyEl.textContent = state.query ? '검색 결과가 없습니다.' : '회원 데이터가 없습니다.'; }
    for (const item of items) {
      const subscriptions = Array.isArray(item.subscriptions) ? item.subscriptions : [];
      const tr = document.createElement('tr');
      tr.append(
        cell(shortId(item.ownerId)),
        cell(item.email || '-'),
        cell(item.phone || '-'),
        cell(dateOnly(item.createdAt)),
        cell(entitlementLabel(item, subscriptions)),
        pill(usageStatus(item, subscriptions)),
        pill(verificationLabel(subscriptions)),
        actionButton('보기', () => openMember(item.ownerId)),
      );
      rowsEl.append(tr);
    }
  }

  async function openMember(ownerId) {
    const [memberResult, paymentResult] = await Promise.all([
      fetchJson(`/admin/api/member?ownerId=${encodeURIComponent(ownerId)}`),
      fetchJson(`/admin/api/member-payments?ownerId=${encodeURIComponent(ownerId)}`),
    ]);
    if (!memberResult.ok) return;
    const data = memberResult.data || {};
    const member = data.member || {};
    detailTitle.textContent = '회원 상세';
    detailId.textContent = shortId(member.ownerId);
    detailBody.replaceChildren(
      section('계정', [['이메일', member.email || '-'], ['전화번호', member.phone || '-'], ['가입일', dateTime(member.createdAt)], ['수정일', dateTime(member.updatedAt)]]),
      section('체험', data.trial ? [['시작', dateTime(data.trial.startedAt)], ['종료', dateTime(data.trial.endsAt)], ['추천 보너스', `${number(data.trial.referralBonusDays)}일`]] : [['상태', '없음']]),
      section('추천', [['추천 인원', `${number(data.referral?.referredCount)}명`], ['추천받음', data.referral?.wasReferred ? '예' : '아니오']]),
      section('파트너', [['적립 건수', `${number(data.partner?.commissionCount)}건`], ['적립 대기', `${money(data.partner?.pendingAmountKrw)}원`], ['확정', `${money(data.partner?.confirmedAmountKrw)}원`]]),
      subscriptionSection(Array.isArray(data.subscriptions) ? data.subscriptions : []),
      paymentSection(paymentResult.ok ? paymentResult.data : null),
    );
    if (backdrop) backdrop.hidden = false;
    if (drawer) drawer.hidden = false;
    document.body.classList.add('drawer-open');
  }

  function subscriptionSection(items) {
    const sectionEl = document.createElement('section');
    sectionEl.className = 'section';
    sectionEl.append(heading('구독'));
    if (!items.length) sectionEl.append(detailRow('이용 상태', '구독 내역 없음'));
    for (const item of items) {
      sectionEl.append(
        detailRow('상품', products[item.productCode] || '기타 이용권'),
        detailRow('채널', channels[String(item.channel || '').toLowerCase()] || (item.channel ? '기타' : '-')),
        detailRow('이용 상태', subscriptionStatus(item.status, item.expiresAt)),
        detailRow('결제 검증', item.verificationState === 'verified' ? '정상' : item.verificationState ? '확인필요' : '해당없음'),
        detailRow('만료', dateTime(item.expiresAt)),
      );
    }
    return sectionEl;
  }

  function paymentSection(data) {
    const sectionEl = document.createElement('section');
    sectionEl.className = 'section payment-history';
    sectionEl.append(heading('결제 이력'));
    const payments = Array.isArray(data?.payments) ? data.payments : [];
    if (payments.length) {
      for (const item of payments) {
        const row = document.createElement('div');
        row.className = 'payment-item';
        const amount = Number(item.amountKrw || 0);
        const sign = item.eventType === 'refund' ? '-' : '';
        row.append(
          textSpan(dateOnly(item.paidAt) || item.month || '-'),
          textSpan(`${products[item.productCode] || item.productCode || '이용권'} · ${channels[item.channel] || item.channel || '-'}`),
          textSpan(`${sign}${money(Math.abs(amount))}원`, 'payment-amount'),
          textSpan(paymentStatus(item.status)),
        );
        sectionEl.append(row);
      }
      const note = document.createElement('small');
      note.className = 'payment-note';
      note.textContent = data.exactHistoryAvailable ? '확정 결제 원장 기준' : '일부 금액은 추정치일 수 있습니다.';
      sectionEl.append(note);
      return sectionEl;
    }

    const snapshots = Array.isArray(data?.snapshots) ? data.snapshots : [];
    if (!snapshots.length) {
      sectionEl.append(emptyLine('결제 이력이 없습니다.'));
      return sectionEl;
    }
    const warning = document.createElement('small');
    warning.className = 'payment-note';
    warning.textContent = '기존 결제 원장 생성 전 데이터라 금액은 정가 기준 추정입니다.';
    sectionEl.append(warning);
    for (const item of snapshots) {
      const row = document.createElement('div');
      row.className = 'payment-item estimated';
      row.append(
        textSpan(dateOnly(item.startedAt)),
        textSpan(`${products[item.productCode] || item.productCode || '이용권'} · ${channels[item.channel] || item.channel || '-'}`),
        textSpan(`약 ${money(item.amountKrw)}원`, 'payment-amount'),
        textSpan(subscriptionStatus(item.status, item.expiresAt)),
      );
      sectionEl.append(row);
    }
    return sectionEl;
  }

  async function fetchJson(url) {
    try {
      const response = await fetch(url, { method: 'GET', cache: 'no-store', credentials: 'same-origin', headers: { accept: 'application/json' } });
      const data = await response.json().catch(() => ({}));
      return { ok: response.ok && data?.ok !== false, data, error: data?.error || '' };
    } catch { return { ok: false, data: {}, error: '' }; }
  }

  function entitlementLabel(item, subscriptions) {
    const active = subscriptions.filter(isCurrentSubscription);
    const codes = [...new Set(active.map((x) => x.productCode).filter(Boolean))];
    if (codes.includes('all_monthly')) return codes.length > 1 ? '통합권 · 중복구독 확인' : '통합권';
    const labels = codes.map((code) => products[code] || code);
    if (labels.length) return labels.join(' + ');
    if (isTrial(item.trialEndsAt)) return '무료체험';
    return '-';
  }

  function usageStatus(item, subscriptions) {
    const active = subscriptions.filter(isCurrentSubscription);
    if (active.some((x) => x.status === 'suspended')) return '정지';
    if (active.some((x) => x.status === 'pending')) return '확인필요';
    if (active.some((x) => x.status === 'cancelled')) return '취소예정';
    if (active.some((x) => ['active', 'grace'].includes(x.status))) return '활성';
    return isTrial(item.trialEndsAt) ? '체험중' : '비활성';
  }

  function verificationLabel(subscriptions) {
    const active = subscriptions.filter(isCurrentSubscription);
    if (!active.length) return '해당없음';
    if (active.every((x) => x.verificationState === 'verified')) return '정상';
    if (active.some((x) => ['failed', 'invalid', 'rejected'].includes(String(x.verificationState || '').toLowerCase()))) return '실패';
    return '확인필요';
  }

  function isCurrentSubscription(item) {
    const status = String(item?.status || '').toLowerCase();
    if (!['active', 'grace', 'cancelled', 'pending', 'suspended'].includes(status)) return false;
    const expires = Date.parse(String(item?.expiresAt || ''));
    return !Number.isFinite(expires) || expires > Date.now();
  }

  function isTrial(value) { const end = Date.parse(String(value || '')); return Number.isFinite(end) && end > Date.now(); }
  function subscriptionStatus(status, expiresAt) {
    const value = String(status || '').toLowerCase();
    const expired = Number.isFinite(Date.parse(String(expiresAt || ''))) && Date.parse(String(expiresAt || '')) <= Date.now();
    if (expired || value === 'expired') return '만료';
    if (value === 'cancelled') return '취소예정';
    if (value === 'suspended') return '정지';
    if (value === 'pending') return '확인필요';
    if (value === 'grace' || value === 'active') return '활성';
    if (value === 'refunded') return '환불';
    return value || '-';
  }
  function paymentStatus(status) {
    const value = String(status || '').toLowerCase();
    if (value === 'paid') return '결제완료';
    if (value === 'refunded') return '환불';
    if (value === 'partial_refund') return '부분환불';
    if (value === 'reversed') return '취소';
    return '확인중';
  }

  function cell(value) { const td = document.createElement('td'); td.textContent = String(value ?? '-'); return td; }
  function pill(value) { const td = document.createElement('td'); const span = document.createElement('span'); span.className = 'pill'; span.textContent = String(value || '-'); td.append(span); return td; }
  function actionButton(label, handler) { const td = document.createElement('td'); const button = document.createElement('button'); button.type = 'button'; button.className = 'row-action'; button.textContent = label; button.addEventListener('click', handler); td.append(button); return td; }
  function section(title, rows) { const el = document.createElement('section'); el.className = 'section'; el.append(heading(title)); for (const [label, value] of rows) el.append(detailRow(label, value)); return el; }
  function heading(value) { const h = document.createElement('h2'); h.textContent = value; return h; }
  function detailRow(label, value) { const row = document.createElement('div'); row.className = 'detailrow'; const a = document.createElement('span'); const b = document.createElement('strong'); a.textContent = label; b.textContent = String(value ?? '-'); row.append(a, b); return row; }
  function textSpan(value, className = '') { const span = document.createElement('span'); if (className) span.className = className; span.textContent = String(value ?? '-'); return span; }
  function emptyLine(value) { const div = document.createElement('div'); div.className = 'empty-line'; div.textContent = value; return div; }
  function shortId(value) { const text = String(value || ''); return text.length > 14 ? `${text.slice(0, 6)}…${text.slice(-5)}` : text || '-'; }
  function number(value) { const n = Number(value || 0); return Number.isFinite(n) ? Math.max(0, Math.trunc(n)).toLocaleString('ko-KR') : '0'; }
  function money(value) { const n = Number(value || 0); return (Number.isFinite(n) ? Math.max(0, Math.round(n)) : 0).toLocaleString('ko-KR'); }
  function dateOnly(value) { const d = new Date(String(value || '')); return Number.isFinite(d.getTime()) ? new Intl.DateTimeFormat('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit' }).format(d) : '-'; }
  function dateTime(value) { const d = new Date(String(value || '')); return Number.isFinite(d.getTime()) ? new Intl.DateTimeFormat('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(d) : '-'; }
})();
