(() => {
  const rowsEl = document.getElementById('memberRows');
  const emptyEl = document.getElementById('emptyMembers');
  const searchInput = document.getElementById('memberSearchInput');
  const searchButton = document.getElementById('memberSearchButton');
  const evidenceFilter = document.getElementById('memberEvidenceFilter');
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
  const channels = { google_play: 'Google Play', play: 'Google Play', web: '웹 결제', admin: '관리자 지급' };
  const entitlementScopes = { all: '전체 이용권', call: '통화관리', message: '문자자동화' };
  const filterToggle = document.getElementById('memberFilterToggle');
  const advancedPanel = document.getElementById('memberAdvancedPanel');
  const filterApply = document.getElementById('memberFilterApply');
  const filterReset = document.getElementById('memberFilterReset');
  const filterChips = document.getElementById('memberFilterChips');
  const filterError = document.getElementById('memberFilterError');
  const filterFields = {
    dateRange: document.getElementById('memberDateRange'),
    dateFrom: document.getElementById('memberDateFrom'),
    dateTo: document.getElementById('memberDateTo'),
    plan: document.getElementById('memberPlanFilter'),
    usage: document.getElementById('memberUsageFilter'),
    verification: document.getElementById('memberVerificationFilter'),
    billing: document.getElementById('memberBillingFilter'),
    channel: document.getElementById('memberChannelFilter'),
    history: document.getElementById('memberHistoryFilter'),
    sort: document.getElementById('memberSort'),
    evidence: evidenceFilter,
  };
  const emptyFilters = () => ({
    dateRange: 'all', dateFrom: '', dateTo: '', plan: 'all',
    usage: 'all', verification: 'all', billing: 'all', channel: 'all',
    history: 'all', sort: 'newest', evidence: 'all',
  });
  const quickFilterGroup = document.getElementById('memberQuickFilters');
  const LOCAL_PAGE_SIZE = 20;
  const MAX_SERVER_PAGES = 80;
  const state = {
    page: 1, totalPages: 1, query: '', filter: 'all',
    quick: 'all', localItems: [], localIncomplete: false, requestId: 0,
    filters: emptyFilters(), fullCount: null,
  };

  searchButton.addEventListener('click', () => runSearch());

  function isAdvancedActive() {
    return Object.entries(state.filters).some(([key, value]) =>
      value !== (key === 'sort' ? 'newest' : key === 'dateFrom' || key === 'dateTo' ? '' : 'all'));
  }
  function hasFilters() {
    return state.quick !== 'all' || !!state.query || isAdvancedActive();
  }
  function clearError() {
    if (filterError) { filterError.hidden = true; filterError.textContent = ''; }
  }
  function toggleDates() {
    const custom = filterFields.dateRange?.value === 'custom';
    if (filterFields.dateFrom) filterFields.dateFrom.disabled = !custom;
    if (filterFields.dateTo) filterFields.dateTo.disabled = !custom;
  }
  function showQuickMode() {
    for (const chip of quickFilterGroup?.querySelectorAll('[data-member-filter]') || []) {
      const selected = chip.dataset.memberFilter === state.quick;
      chip.classList.toggle('is-active', selected);
      chip.setAttribute('aria-pressed', String(selected));
    }
  }
  function filterValues() {
    const next = emptyFilters();
    for (const key of Object.keys(next)) {
      if (filterFields[key]) next[key] = String(filterFields[key].value || next[key]);
    }
    if (next.dateRange !== 'custom') { next.dateFrom = ''; next.dateTo = ''; }
    return next;
  }
  function applyAdvancedFilters() {
    const next = filterValues();
    if (next.dateRange === 'custom' && next.dateFrom && next.dateTo && next.dateFrom > next.dateTo) {
      if (filterError) { filterError.textContent = '시작일이 종료일보다 늦습니다.'; filterError.hidden = false; }
      return;
    }
    clearError();
    state.filters = next;
    state.filter = next.evidence;
    showActiveFilters();
    loadMembers(1);
  }
  function resetAllFilters() {
    state.quick = 'all';
    state.query = '';
    if (searchInput) searchInput.value = '';
    state.filter = 'all';
    state.filters = emptyFilters();
    for (const [key, value] of Object.entries(state.filters)) {
      if (filterFields[key]) filterFields[key].value = value;
    }
    toggleDates();
    clearError();
    showQuickMode();
    showActiveFilters();
    loadMembers(1);
  }
  function showActiveFilters() {
    if (!filterChips) return;
    filterChips.replaceChildren();
    const chips = [];
    if (state.quick !== 'all') {
      const names = { paid:'유료', trial:'체험중', review:'확인필요', admin:'관리자지급' };
      chips.push(['quick', '빠른필터: ' + (names[state.quick] || state.quick)]);
    }
    if (state.query) chips.push(['query', '검색: ' + state.query]);
    const names = {
      dateRange:'가입일', plan:'이용권', usage:'이용 상태', verification:'결제 검증',
      billing:'구독·결제', channel:'결제 채널', history:'결제 이력',
      evidence:'지급·구매 기록', sort:'정렬',
    };
    for (const [key, label] of Object.entries(names)) {
      const value = state.filters[key];
      if (!value || value === 'all' || (key === 'sort' && value === 'newest')) continue;
      let description;
      if (key === 'dateRange') {
        description = value === 'today' ? '오늘' : value === '7' ? '최근 7일'
          : value === '30' ? '최근 30일'
            : [state.filters.dateFrom || '시작 제한 없음', state.filters.dateTo || '종료 제한 없음'].join(' ~ ');
      } else {
        const option = filterFields[key]?.options
          ? Array.from(filterFields[key].options).find(x => x.value === value) : null;
        description = option?.textContent || value;
      }
      chips.push([key, label + ': ' + description]);
    }
    for (const [key, label] of chips) {
      const chip = document.createElement('button');
      chip.type = 'button';
      chip.textContent = label + ' ×';
      chip.setAttribute('aria-label', label + ' 필터 해제');
      chip.addEventListener('click', () => {
        if (key === 'quick') { state.quick = 'all'; showQuickMode(); }
        else if (key === 'query') { state.query = ''; searchInput.value = ''; }
        else {
          state.filters[key] = key === 'sort' ? 'newest' : 'all';
          if (key === 'dateRange') { state.filters.dateFrom = ''; state.filters.dateTo = ''; }
          if (filterFields[key]) filterFields[key].value = state.filters[key];
          if (key === 'dateRange') {
            if (filterFields.dateFrom) filterFields.dateFrom.value = '';
            if (filterFields.dateTo) filterFields.dateTo.value = '';
            toggleDates();
          }
          state.filter = state.filters.evidence;
        }
        showActiveFilters();
        loadMembers(1);
      });
      filterChips.append(chip);
    }
    filterChips.hidden = chips.length === 0;
  }
  filterToggle?.addEventListener('click', () => {
    if (!advancedPanel) return;
    advancedPanel.hidden = !advancedPanel.hidden;
    filterToggle.setAttribute('aria-expanded', String(!advancedPanel.hidden));
  });
  filterFields.dateRange?.addEventListener('change', toggleDates);
  filterApply?.addEventListener('click', applyAdvancedFilters);
  filterReset?.addEventListener('click', resetAllFilters);
  showActiveFilters();

  function purchaseEvidence(item) {
    return item?.purchaseHistory?.recorded === true
      || item?.purchaseHistory?.evidence === 'confirmed_charge'
      || item?.legacyEntitlement?.source === 'pagero_payment';
  }
  function membershipDay(value) {
    const d = new Date(String(value || ''));
    return Number.isFinite(d.getTime()) ? new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime() : null;
  }
  function dateFilterMatch(item) {
    const { dateRange, dateFrom, dateTo } = state.filters;
    if (dateRange === 'all') return true;
    const day = membershipDay(item.createdAt);
    if (day === null) return false;
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    if (dateRange === 'today') return day === today;
    if (dateRange === '7' || dateRange === '30') {
      const days = Number(dateRange);
      const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - days + 1).getTime();
      return day >= start && day <= today;
    }
    if (dateRange === 'custom') {
      if (dateFrom && day < membershipDay(dateFrom + 'T12:00:00')) return false;
      if (dateTo && day > membershipDay(dateTo + 'T12:00:00')) return false;
    }
    return true;
  }
  function advancedFilterMatch(item) {
    const f = state.filters;
    const subs = Array.isArray(item?.subscriptions) ? item.subscriptions : [];
    const current = subs.filter(isCurrentSubscription);
    const codes = current.map(sub => sub.productCode).filter(Boolean);
    const activeGift = item?.adminEntitlement?.active === true;
    if (!dateFilterMatch(item)) return false;
    if (f.plan !== 'all') {
      if (f.plan === 'trial') {
        if (!isTrial(item.trialEndsAt) || !!current.length || activeGift) return false;
      } else if (f.plan === 'admin') {
        if (!activeGift) return false;
      } else if (f.plan === 'other') {
        if (!codes.some(code => !Object.hasOwn(products, code)) && !item?.legacyEntitlement?.nonDefaultRecord) return false;
      } else if (!codes.includes(f.plan)) return false;
    }
    const usage = usageStatus(item, subs);
    if (f.usage !== 'all') {
      const names = { trial:'체험중', active:'활성', cancelled:'취소예정', expired:'만료', suspended:'정지', review:'확인필요' };
      if (f.usage === 'active' ? !usage.startsWith('활성') : usage !== names[f.usage]) return false;
    }
    if (f.verification !== 'all') {
      const names = { normal:'정상', review:'확인필요', failed:'실패', none:'해당없음' };
      if (verificationLabel(subs) !== names[f.verification]) return false;
    }
    if (f.billing !== 'all') {
      const bill = classifySubscription(item, subs);
      if (f.billing === 'paid' && !verifiedActiveSubscription(item)) return false;
      if (f.billing === 'trial' && bill.badgeClass !== 'member-billing-trial') return false;
      if (f.billing === 'admin' && !activeGift) return false;
      if (f.billing === 'past' && bill.badgeClass !== 'member-billing-history') return false;
      if (f.billing === 'missing' && bill.badgeClass !== 'member-billing-muted') return false;
      if (f.billing === 'review' && bill.badgeClass !== 'member-billing-review') return false;
    }
    if (f.channel !== 'all') {
      if (f.channel === 'admin') { if (!activeGift) return false; }
      else if (!subs.some(sub => (f.channel === 'google_play'
        ? ['google_play', 'play'].includes(String(sub.channel || '').toLowerCase())
        : String(sub.channel || '').toLowerCase() === 'web'))) return false;
    }
    if (f.history !== 'all' && purchaseEvidence(item) !== (f.history === 'yes')) return false;
    return true;
  }
  function sortMembers(items) {
    const mode = state.filters.sort;
    return items.sort((a,b) => {
      if (mode === 'paid' || mode === 'review') {
        const rank = x => mode === 'paid' ? Number(verifiedActiveSubscription(x))
          : Number(quickReviewMatch(x));
        const delta = rank(b) - rank(a);
        if (delta) return delta;
      }
      const first = Date.parse(String(a.createdAt || ''));
      const second = Date.parse(String(b.createdAt || ''));
      const aTime = Number.isFinite(first) ? first : (mode === 'oldest' ? Infinity : -Infinity);
      const bTime = Number.isFinite(second) ? second : (mode === 'oldest' ? Infinity : -Infinity);
      if (aTime !== bTime) return mode === 'oldest' ? aTime - bTime : bTime - aTime;
      return String(a.ownerId || '').localeCompare(String(b.ownerId || ''));
    });
  }
  function quickReviewMatch(item) {
    const subs = Array.isArray(item?.subscriptions) ? item.subscriptions : [];
    return classifySubscription(item, subs).badgeClass === 'member-billing-review'
      || subs.some(sub => ['failed','invalid','rejected'].includes(String(sub.verificationState || '').toLowerCase())
        || String(sub.status || '').toLowerCase() === 'pending');
  }
  function countLabel(count, filtered) {
    const suffix = state.localIncomplete ? ' · 일부만 확인됨' : '';
    if (!filtered) return '전체 ' + number(count) + '명';
    return '필터 결과 ' + number(count) + '명 / 전체 '
      + (state.fullCount === null ? '확인 전' : number(state.fullCount) + '명') + suffix;
  }

  searchInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') runSearch();
  });
  quickFilterGroup?.addEventListener('click', (event) => {
    const button = event.target.closest('[data-member-filter]');
    if (!button || !quickFilterGroup.contains(button)) return;
    const mode = button.dataset.memberFilter || 'all';
    if (!['all', 'paid', 'trial', 'review', 'admin'].includes(mode) || state.quick === mode) return;
    state.quick = mode;
    showQuickMode();
    showActiveFilters();
    loadMembers(1);
  });
  prevButton?.addEventListener('click', () => {
    if (state.page <= 1) return;
    if (!needsLocalFiltering()) loadMembers(state.page - 1);
    else renderQuickPage(state.page - 1);
  });
  nextButton?.addEventListener('click', () => {
    if (state.page >= state.totalPages) return;
    if (!needsLocalFiltering()) loadMembers(state.page + 1);
    else renderQuickPage(state.page + 1);
  });
  refreshButton?.addEventListener('click', () => setTimeout(() => loadMembers(state.page), 80));
  setTimeout(() => loadMembers(1), 60);

  function runSearch() {
    state.query = String(searchInput.value || '').trim().slice(0, 80);
    showActiveFilters();
    loadMembers(1);
  }

  function needsLocalFiltering() { return state.quick !== 'all' || isAdvancedActive(); }
  async function getMemberPage(page, filter = state.filter, query = state.query) {
    const url = '/admin/api/members?page=' + Math.max(1, page)
      + '&filter=' + encodeURIComponent(filter)
      + (query ? '&q=' + encodeURIComponent(query) : '');
    const response = await fetch(url, {
      method: 'GET', cache: 'no-store', credentials: 'same-origin',
      headers: { accept: 'application/json' },
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok || data?.ok === false) throw new Error(data?.error || '회원 목록을 불러오지 못했습니다.');
    return data;
  }

  // Current verified subscription status is not financial ledger proof.
  function verifiedActiveSubscription(item) {
    return (Array.isArray(item?.subscriptions) ? item.subscriptions : []).some(sub =>
      sub?.verificationState === 'verified'
      && ['active', 'grace', 'cancelled'].includes(String(sub.status || '').toLowerCase())
      && isCurrentSubscription(sub));
  }
  function quickFilterMatch(item) {
    const subscriptions = Array.isArray(item?.subscriptions) ? item.subscriptions : [];
    switch (state.quick) {
      case 'paid': return verifiedActiveSubscription(item);
      case 'trial':
        return isTrial(item?.trialEndsAt) && !verifiedActiveSubscription(item)
          && item?.adminEntitlement?.active !== true;
      case 'review': {
        const status = classifySubscription(item, subscriptions);
        return status.badgeClass === 'member-billing-review'
          || subscriptions.some(sub =>
            ['failed', 'invalid', 'rejected'].includes(String(sub?.verificationState || '').toLowerCase())
            || String(sub?.status || '').toLowerCase() === 'pending');
      }
      case 'admin': return item?.adminEntitlement?.active === true;
      default: return true;
    }
  }

  function updatePageControls() {
    if (pageInfo) pageInfo.textContent = state.page + ' / ' + state.totalPages;
    if (prevButton) prevButton.disabled = state.page <= 1;
    if (nextButton) nextButton.disabled = state.page >= state.totalPages;
  }
  function renderQuickPage(page) {
    state.totalPages = Math.max(1, Math.ceil(state.localItems.length / LOCAL_PAGE_SIZE));
    state.page = Math.min(Math.max(1, page), state.totalPages);
    const start = (state.page - 1) * LOCAL_PAGE_SIZE;
    renderMembers(state.localItems.slice(start, start + LOCAL_PAGE_SIZE));
    if (resultCount) resultCount.textContent = countLabel(state.localItems.length, true);
    updatePageControls();
  }

  async function loadMembers(page) {
    const requestId = ++state.requestId;
    searchButton.disabled = true;
    if (resultCount) resultCount.textContent = '조회 중';
    try {
      const local = needsLocalFiltering();
      const data = await getMemberPage(local ? 1 : page);
      if (requestId !== state.requestId) return;
      if (!hasFilters()) state.fullCount = Number(data.total || 0);
      else if (state.fullCount === null || state.query || state.filter !== 'all') {
        const baseline = await getMemberPage(1, 'all', '');
        if (requestId !== state.requestId) return;
        state.fullCount = Number(baseline.total || 0);
      }
      if (!local) {
        state.page = Number(data.page || 1);
        state.totalPages = Math.max(1, Number(data.totalPages || 1));
        renderMembers(Array.isArray(data.members) ? data.members : []);
        if (resultCount) resultCount.textContent = countLabel(Number(data.total || 0), hasFilters());
        updatePageControls();
        return;
      }
      // Scan all reachable server pages so filters apply beyond the visible page.
      const all = Array.isArray(data.members) ? [...data.members] : [];
      const pages = Math.max(1, Number(data.totalPages || 1));
      for (let index = 2; index <= Math.min(pages, MAX_SERVER_PAGES); index += 4) {
        const ids = Array.from({ length: Math.min(4, pages - index + 1, MAX_SERVER_PAGES - index + 1) }, (_, offset) => index + offset);
        const batches = await Promise.all(ids.map(id => getMemberPage(id)));
        if (requestId !== state.requestId) return;
        for (const batch of batches) all.push(...(Array.isArray(batch.members) ? batch.members : []));
      }
      if (requestId !== state.requestId) return;
      state.localItems = sortMembers(all.filter(item => quickFilterMatch(item) && advancedFilterMatch(item)));
      state.localIncomplete = pages > MAX_SERVER_PAGES;
      renderQuickPage(1);
    } catch (error) {
      if (requestId !== state.requestId) return;
      rowsEl.replaceChildren();
      if (resultCount) resultCount.textContent = '조회 실패';
      if (emptyEl) {
        emptyEl.hidden = false;
        emptyEl.textContent = String(error?.message || '회원 목록을 불러오지 못했습니다.');
      }
    } finally {
      if (requestId === state.requestId) searchButton.disabled = false;
    }
  }

  function compactBillingText(status) {
    const text = String(status.label || '-');
    const names = {
      '결제 유예 · 이용중': '유예 · 이용중',
      '해지 예정 · 이용중': '해지 예정',
      '구독 이용중 · 검증 확인필요': '구독 검증필요',
      '과거 결제 · 현재 미구독': '과거 결제',
      '구독 이력 · 현재 미구독': '이전 구독',
      '과거 결제 표기 · 확인필요': '과거 결제 확인필요',
      '관리자 지급 · 결제 아님': '관리자 지급',
      '무료체험 · 결제 아님': '무료체험',
      '결제 확인 자료 없음': '결제 자료 없음',
    };
    return names[text] || text;
  }

  function renderMembers(items) {
    rowsEl.replaceChildren();
    if (emptyEl) {
      emptyEl.hidden = items.length > 0;
      emptyEl.textContent = hasFilters()
        ? '조건에 맞는 회원이 없습니다.' : '회원 데이터가 없습니다.';
    }
    for (const item of items) {
      const subscriptions = Array.isArray(item.subscriptions) ? item.subscriptions : [];
      const payStatus = classifySubscription(item, subscriptions);
      const tr = document.createElement('tr');
      if (payStatus.rowClass) tr.classList.add(payStatus.rowClass);
      const billingCell = paymentStatusCell({ ...payStatus, label: compactBillingText(payStatus) });
      billingCell.title = payStatus.label;
      const ownerCell = cell(shortId(item.ownerId));
      ownerCell.title = String(item.ownerId || '');
      const emailCell = cell(item.email || '-');
      emailCell.title = String(item.email || '-');
      const planCell = cell(legacyAwareLabel(item, subscriptions));
      planCell.title = planCell.textContent;
      tr.append(
        ownerCell,
        emailCell,
        cell(dateOnly(item.createdAt)),
        planCell,
        pill(usageStatus(item, subscriptions)),
        pill(verificationLabel(subscriptions)),
        billingCell,
        actionButton('상세', () => openMember(item.ownerId)),
      );
      rowsEl.append(tr);
    }
  }

  async function openMember(ownerId) {
    const [memberResult, paymentResult, entitlementResult] = await Promise.all([
      fetchJson(`/admin/api/member?ownerId=${encodeURIComponent(ownerId)}`),
      fetchJson(`/admin/api/member-payments?ownerId=${encodeURIComponent(ownerId)}`),
      fetchJson(`/admin/api/entitlement?ownerId=${encodeURIComponent(ownerId)}`),
    ]);
    if (!memberResult.ok) return;
    const data = memberResult.data || {};
    const member = data.member || {};
    detailTitle.textContent = '회원 상세';
    detailId.textContent = shortId(member.ownerId);
    const subscriptions = Array.isArray(data.subscriptions) ? data.subscriptions : [];
    const entitlement = entitlementResult.ok ? entitlementResult.data : null;
    const content = document.createElement('div');
    content.className = 'member-detail-layout';
    content.append(
      memberSummary(member, data.trial, entitlement, subscriptions),
      entitlementSection(member.ownerId, entitlement),
      subscriptionSection(subscriptions),
      paymentSection(paymentResult.ok ? paymentResult.data : null),
      foldedDetail('기타 이력 · 이전 기록', [
        legacyEntitlementSection(data.legacyEntitlement),
        section('체험 기록', data.trial ? [
          ['시작', dateTime(data.trial.startedAt)],
          ['종료', dateTime(data.trial.endsAt)],
          ['추천 보너스', `${number(data.trial.referralBonusDays)}일`],
        ] : [['상태', '없음']]),
        section('추천', [
          ['추천 인원', `${number(data.referral?.referredCount)}명`],
          ['추천받음', data.referral?.wasReferred ? '예' : '아니오'],
        ]),
        section('파트너', [
          ['적립 건수', `${number(data.partner?.commissionCount)}건`],
          ['적립 대기', `${money(data.partner?.pendingAmountKrw)}원`],
          ['확정', `${money(data.partner?.confirmedAmountKrw)}원`],
        ]),
      ]),
    );
    detailBody.replaceChildren(content);
    if (backdrop) backdrop.hidden = false;
    if (drawer) drawer.hidden = false;
    document.body.classList.add('drawer-open');
  }

  function memberSummary(member, trial, entitlement, subscriptions) {
    const panel = document.createElement('section');
    panel.className = 'member-summary-card';
    const intro = document.createElement('div');
    intro.className = 'member-summary-head';
    const name = document.createElement('div');
    name.className = 'member-summary-name';
    const caption = document.createElement('span');
    caption.className = 'member-detail-eyebrow';
    caption.textContent = '회원 계정';
    const email = document.createElement('strong');
    email.className = 'member-summary-email';
    email.textContent = member.email || '이메일 미등록';
    name.append(caption, email);

    const report = classifySubscription({
      ...member,
      trialEndsAt: trial?.endsAt || member.trialEndsAt,
      adminEntitlement: entitlement?.entitlement,
    }, subscriptions);
    const badge = document.createElement('span');
    badge.className = 'member-summary-badge ' + report.badgeClass;
    badge.textContent = report.label;
    intro.append(name, badge);

    const list = document.createElement('div');
    list.className = 'member-summary-grid';
    list.append(
      summaryStat('전화번호', member.phone || '미등록'),
      summaryStat('가입일', dateOnly(member.createdAt)),
      summaryStat('현재 이용권', entitlementLabel({
        ...member, adminEntitlement: entitlement?.entitlement,
        trialEndsAt: trial?.endsAt || member.trialEndsAt,
      }, subscriptions)),
      summaryStat('최근 변경', dateOnly(member.updatedAt)),
    );
    panel.append(intro, list);
    return panel;
  }

  function summaryStat(label, value) {
    const item = document.createElement('div');
    item.className = 'member-summary-stat';
    const name = document.createElement('span');
    name.textContent = label;
    const content = document.createElement('strong');
    content.textContent = String(value || '-');
    item.append(name, content);
    return item;
  }

  function foldedDetail(label, sections) {
    const details = document.createElement('details');
    details.className = 'member-history-fold';
    const summary = document.createElement('summary');
    summary.textContent = label;
    const content = document.createElement('div');
    content.className = 'member-history-content';
    content.append(...sections);
    details.append(summary, content);
    return details;
  }

  function controlField(label, input) {
    const field = document.createElement('label');
    field.className = 'member-entitlement-field';
    const title = document.createElement('span');
    title.textContent = label;
    field.append(title, input);
    return field;
  }

  function entitlementSection(ownerId, data) {
    const sectionEl = document.createElement('section');
    sectionEl.className = 'section admin-entitlement';
    sectionEl.append(heading('관리자 이용권'));
    const entitlement = data?.entitlement || null;
    if (entitlement?.active) {
      sectionEl.append(
        detailRow('상태', '사용중'),
        detailRow('범위', entitlementScopes[entitlement.scope] || entitlement.scope || '-'),
        detailRow('만료', dateTime(entitlement.expiresAt)),
        detailRow('구분', '관리자 지급 · 결제 아님'),
      );
      if (entitlement.note) sectionEl.append(detailRow('사유', entitlement.note));
    } else if (entitlement) {
      sectionEl.append(
        detailRow('상태', entitlement.status === 'revoked' ? '회수됨' : '만료'),
        detailRow('이전 범위', entitlementScopes[entitlement.scope] || entitlement.scope || '-'),
        detailRow('종료', dateTime(entitlement.expiresAt)),
      );
    } else {
      sectionEl.append(detailRow('상태', '지급 내역 없음'));
    }

    const controls = document.createElement('div');
    controls.className = 'entitlement-controls';
    const scopeSelect = document.createElement('select');
    [['all', '전체 이용권'], ['call', '통화관리'], ['message', '문자자동화']].forEach(([value, label]) => {
      const option = document.createElement('option'); option.value = value; option.textContent = label; scopeSelect.append(option);
    });
    if (entitlement?.scope && entitlementScopes[entitlement.scope]) scopeSelect.value = entitlement.scope;
    const daysSelect = document.createElement('select');
    [[7, '7일'], [30, '30일'], [90, '90일'], [365, '365일']].forEach(([value, label]) => {
      const option = document.createElement('option'); option.value = String(value); option.textContent = label; daysSelect.append(option);
    });
    daysSelect.value = '30';
    const noteInput = document.createElement('input');
    noteInput.type = 'text';
    noteInput.maxLength = 300;
    noteInput.placeholder = '지급 사유 (선택)';
    const grantButton = document.createElement('button');
    grantButton.type = 'button';
    grantButton.className = 'row-action entitlement-primary';
    grantButton.textContent = entitlement?.active ? '이용권 연장' : '이용권 지급';
    grantButton.addEventListener('click', async () => {
      await runEntitlementAction(grantButton, {
        ownerId,
        action: 'grant',
        scope: scopeSelect.value,
        durationDays: Number(daysSelect.value),
        note: noteInput.value,
      });
    });
    controls.append(
      controlField('이용권 범위', scopeSelect),
      controlField('기간', daysSelect),
      controlField('지급 사유 (선택)', noteInput),
    );
    const actionBar = document.createElement('div');
    actionBar.className = 'member-entitlement-actions';
    actionBar.append(grantButton);

    if (entitlement?.active) {
      const revokeButton = document.createElement('button');
      revokeButton.type = 'button';
      revokeButton.className = 'row-action entitlement-revoke';
      revokeButton.textContent = '이용권 회수';
      revokeButton.addEventListener('click', async () => {
        if (!window.confirm('이 회원의 관리자 이용권을 회수하시겠습니까? 유료 구독이나 남은 무료체험은 유지됩니다.')) return;
        await runEntitlementAction(revokeButton, { ownerId, action: 'revoke' });
      });
      actionBar.append(revokeButton);
    }
    sectionEl.append(controls, actionBar);
    return sectionEl;
  }

  function legacyEntitlementSection(record) {
    const el = document.createElement('section');
    el.className = 'section';
    el.append(heading('이전 이용권 기록'));
    if (!record) {
      el.append(emptyLine('표시할 이전 기록이 없습니다.'));
      return el;
    }
    const baseline = record.planCode === 'beta_free' && record.source === 'beta';
    el.append(
      detailRow('기록 유형', baseline ? '기본 베타 기록' : '이전 이용권 레코드'),
      detailRow('상태', record.status || '-'),
      detailRow('이용권 코드', record.planCode || '-'),
      detailRow('등록 경로', record.source || '-'),
      detailRow('기록상 종료일', dateTime(record.paidUntil)),
      detailRow('최초 저장', dateTime(record.createdAt)),
      detailRow('최근 변경', dateTime(record.updatedAt)),
    );
    return el;
  }

  async function runEntitlementAction(button, payload) {
    const oldText = button.textContent;
    button.disabled = true;
    button.textContent = '처리중';
    try {
      const response = await fetch('/admin/api/entitlement', {
        method: 'POST',
        cache: 'no-store',
        credentials: 'same-origin',
        headers: { accept: 'application/json', 'content-type': 'application/json' },
        body: JSON.stringify(payload),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok || data?.ok === false) throw new Error(data?.error || '이용권 처리를 완료하지 못했습니다.');
      await openMember(payload.ownerId);
      setTimeout(() => loadMembers(state.page), 80);
    } catch (error) {
      window.alert(String(error?.message || '이용권 처리를 완료하지 못했습니다.'));
      button.disabled = false;
      button.textContent = oldText;
    }
  }

  function subscriptionSection(items) {
    const sectionEl = document.createElement('section');
    sectionEl.className = 'section';
    sectionEl.append(heading('구독'));
    if (!items.length) sectionEl.append(emptyLine('연결된 구독 내역이 없습니다.'));
    for (const item of items) {
      const record = document.createElement('div');
      record.className = 'member-subscription-record';
      record.append(
        detailRow('상품', products[item.productCode] || '기타 이용권'),
        detailRow('채널', channels[String(item.channel || '').toLowerCase()] || (item.channel ? '기타' : '-')),
        detailRow('이용 상태', subscriptionStatus(item.status, item.expiresAt)),
        detailRow('결제 검증', item.verificationState === 'verified' ? '정상' : item.verificationState ? '확인필요' : '해당없음'),
        detailRow('만료', dateTime(item.expiresAt)),
      );
      sectionEl.append(record);
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
      if (!data.exactHistoryAvailable) {
        const note = document.createElement('small');
        note.className = 'payment-note';
        note.textContent = '추정 금액 포함';
        sectionEl.append(note);
      }
      return sectionEl;
    }

    const snapshots = Array.isArray(data?.snapshots) ? data.snapshots : [];
    if (!snapshots.length) {
      sectionEl.append(emptyLine('결제 이력이 없습니다.'));
      return sectionEl;
    }

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

  function grantHistoryLabel(item) {
    if (item?.adminEntitlement?.active) return '지급 중';
    const history = item?.grantHistory;
    if (history?.recorded !== true) return '확인 안 됨';
    if (history.source === 'legacy_manual') return '과거 수동 지급';
    if (history.source === 'audit') return '지급 감사 기록';
    return '관리자 지급 이력';
  }

  // Admin uses the same current-subscription conditions as app access.
  // Paid amount verification is a financial ledger question, not an access-status test.
  function classifySubscription(item, subscriptions) {
    const current = subscriptions.filter(s => s.verificationState === 'verified'
      && ['active','grace','cancelled'].includes(String(s.status || '').toLowerCase())
      && isCurrentSubscription(s));
    if (current.some(s => s.status === 'active' && (s.autoRenewing === true || s.channel === 'web')))
      return { label: '결제중', badgeClass: 'member-billing-active', rowClass: 'member-billing-row' };
    if (current.some(s => s.status === 'grace'))
      return { label: '결제 유예 · 이용중', badgeClass: 'member-billing-grace', rowClass: 'member-billing-row' };
    if (current.length)
      return { label: '해지 예정 · 이용중', badgeClass: 'member-billing-ending', rowClass: 'member-billing-ending-row' };
    if (subscriptions.some(s => ['active','grace','cancelled'].includes(s.status) && isCurrentSubscription(s)))
      return { label: '구독 이용중 · 검증 확인필요', badgeClass: 'member-billing-review', rowClass: 'member-billing-ending-row' };
    if (subscriptions.some(s => s.status === 'pending'))
      return { label: '구독 검증중', badgeClass: 'member-billing-review' };
    if (item?.purchaseHistory?.evidence === 'confirmed_charge')
      return { label: '과거 결제 · 현재 미구독', badgeClass: 'member-billing-history' };
    if (item?.purchaseHistory?.recorded === true)
      return { label: '구독 이력 · 현재 미구독', badgeClass: 'member-billing-history' };
    if (item?.legacyEntitlement?.source === 'pagero_payment')
      return { label: '과거 결제 표기 · 확인필요', badgeClass: 'member-billing-review' };
    if (item?.adminEntitlement?.active === true)
      return { label: '관리자 지급 · 결제 아님', badgeClass: 'member-billing-admin' };
    if (isTrial(item?.trialEndsAt))
      return { label: '무료체험 · 결제 아님', badgeClass: 'member-billing-trial' };
    return { label: '결제 확인 자료 없음', badgeClass: 'member-billing-muted' };
  }

  function paymentStatusCell(status) {
    const td = document.createElement('td');
    const mark = document.createElement('span');
    mark.className = `member-billing-badge ${status.badgeClass}`;
    mark.textContent = status.label;
    td.append(mark);
    return td;
  }
  function legacyAwareLabel(item, subscriptions) {
    const current = entitlementLabel(item, subscriptions);
    const prior = item?.legacyEntitlement;
    if (!prior?.nonDefaultRecord) return current;
    // This is historical evidence, not confirmation of live app access.
    return current === '-' ? '이전 이용권 기록' : `${current} · 이전 이용권 기록`;
  }
  function entitlementLabel(item, subscriptions) {
    const active = subscriptions.filter(isCurrentSubscription);
    const codes = [...new Set(active.map((x) => x.productCode).filter(Boolean))];
    const labels = codes.map((code) => products[code] || code);
    const admin = item?.adminEntitlement;
    if (admin?.active) {
      const adminLabel = admin.scope === 'all'
        ? '관리자 전체'
        : admin.scope === 'call'
          ? '관리자 통화'
          : admin.scope === 'message'
            ? '관리자 문자'
            : '관리자 이용권';
      return labels.length ? `${labels.join(' + ')} · ${adminLabel}` : adminLabel;
    }
    if (codes.includes('all_monthly')) return codes.length > 1 ? '통합권 · 중복구독 확인' : '통합권';
    if (labels.length) return labels.join(' + ');
    if (isTrial(item.trialEndsAt)) return '무료체험';
    return '-';
  }

  function usageStatus(item, subscriptions) {
    const active = subscriptions.filter(isCurrentSubscription);
    if (item?.adminEntitlement?.active) return '활성 · 관리자';
    if (active.some((x) => x.status === 'suspended')) return '정지';
    if (active.some((x) => x.status === 'pending')) return '확인필요';
    if (active.some((x) => x.status === 'cancelled')) return '취소예정';
    if (active.some((x) => ['active', 'grace'].includes(x.status))) return '활성';
    if (isTrial(item.trialEndsAt)) return '체험중';
    return subscriptions.length ? '만료' : '비활성';
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
