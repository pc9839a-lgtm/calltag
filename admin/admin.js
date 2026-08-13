const $ = (id) => document.getElementById(id);

const el = {
  status: $('statusBar'), identity: $('adminIdentity'), badge: $('adminModeBadge'), refresh: $('refreshButton'),
  membersTab: $('membersTab'), partnersTab: $('partnersTab'), membersView: $('membersView'), partnersView: $('partnersView'),
  total: $('metricTotal'), new7: $('metricNew'), trial: $('metricTrial'), paid: $('metricPaid'), review: $('metricPaymentReview'), partner: $('metricPartner'),
  revenueGross: $('metricRevenueGross'), playFee: $('metricPlayFee'), revenueNet: $('metricRevenueNet'),
  generated: $('generatedAt'), rows: $('memberRows'), empty: $('emptyMembers'),
  partnerMonth: $('partnerMonth'), partnerCount: $('partnerCount'), partnerGross: $('partnerGross'), partnerEarned: $('partnerEarned'), partnerPayable: $('partnerPayable'), partnerPaid: $('partnerPaid'), partnerGenerated: $('partnerGeneratedAt'), partnerRows: $('partnerRows'), emptyPartners: $('emptyPartners'),
  backdrop: $('detailBackdrop'), drawer: $('memberDetail'), detailTitle: $('detailTitle'), detailId: $('detailOwnerId'), detailBody: $('detailBody'), close: $('detailClose'),
};

const products = {
  all_monthly: '통합권',
  call_monthly: '전화관리',
  message_monthly: '문자자동화',
  pagero_monthly: '페이지로 클래식',
  pagero_pro_monthly: '페이지로 프로',
  pagero_domain_monthly: 'SSL',
};
const channels = { google_play: 'Google Play', play: 'Google Play', web: '웹 결제' };
const state = { view: 'members', financeWriteEnabled: false, month: currentMonth() };

boot();

function boot() {
  el.partnerMonth.value = state.month;
  el.membersTab.addEventListener('click', () => switchView('members'));
  el.partnersTab.addEventListener('click', () => switchView('partners'));
  el.refresh.addEventListener('click', refreshCurrent);
  el.partnerMonth.addEventListener('change', () => {
    if (/^20\d{2}-(0[1-9]|1[0-2])$/.test(el.partnerMonth.value)) state.month = el.partnerMonth.value;
    loadPartners();
  });
  el.close.addEventListener('click', closeDetail);
  el.backdrop.addEventListener('click', closeDetail);
  document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeDetail(); });
  loadOverview();
}

function switchView(view) {
  state.view = view === 'partners' ? 'partners' : 'members';
  el.membersView.hidden = state.view !== 'members';
  el.partnersView.hidden = state.view !== 'partners';
  el.membersTab.classList.toggle('active', state.view === 'members');
  el.partnersTab.classList.toggle('active', state.view === 'partners');
  closeDetail();
  if (state.view === 'partners') loadPartners();
  else loadOverview();
}

function refreshCurrent() {
  if (state.view === 'partners') loadPartners();
  else loadOverview();
}

async function loadOverview() {
  el.refresh.disabled = true;
  const result = await get('/admin/api/overview');
  el.refresh.disabled = false;
  if (!result.ok) {
    setStatus(result.error || '운영 데이터 접근이 잠겨 있습니다.', false);
    el.identity.textContent = '';
    el.empty.hidden = false;
    return;
  }
  renderOverview(result.data);
}

function renderOverview(data) {
  setStatus('보안 게이트 통과 · 최소정보 운영 데이터', true);
  el.identity.textContent = data?.admin?.email || '';
  const metrics = data?.metrics || {};
  el.total.textContent = number(metrics.totalMembers);
  el.new7.textContent = number(metrics.newMembers7d);
  el.trial.textContent = number(metrics.trialMembers);
  el.paid.textContent = number(metrics.activePaid);
  el.review.textContent = number(metrics.paymentReview);
  el.partner.textContent = number(metrics.partnerPending);

  const revenue = data?.revenueEstimate || {};
  el.revenueGross.textContent = `${money(revenue.grossMonthlyKrw)}원`;
  el.playFee.textContent = `${money(revenue.googlePlayFeeEstimateKrw)}원`;
  el.revenueNet.textContent = `${money(revenue.netAfterPlayFeeEstimateKrw)}원`;

  el.generated.textContent = data?.generatedAt ? `갱신 ${dateTime(data.generatedAt)}` : '민감정보는 서버에서 마스킹 후 표시';
  const rows = Array.isArray(data?.recentMembers) ? data.recentMembers : [];
  el.rows.replaceChildren();
  el.empty.hidden = rows.length > 0;
  for (const item of rows) {
    const subscriptions = memberSubscriptions(item);
    const tr = document.createElement('tr');
    tr.append(
      cell(shortId(item.ownerId)),
      cell(item.email || '-'),
      cell(item.phone || '-'),
      cell(dateOnly(item.createdAt)),
      cell(entitlementLabel(item)),
      pill(memberUsageStatus(item)),
      pill(paymentVerificationLabel(subscriptions)),
      actionButton('보기', () => openMember(item.ownerId)),
    );
    el.rows.append(tr);
  }
}

async function loadPartners() {
  el.refresh.disabled = true;
  const result = await get(`/admin/api/partners?month=${encodeURIComponent(state.month)}`);
  el.refresh.disabled = false;
  if (!result.ok) {
    setStatus(result.error || '파트너 정산 데이터를 불러오지 못했습니다.', false);
    el.partnerRows.replaceChildren();
    el.emptyPartners.hidden = false;
    return;
  }
  renderPartners(result.data || {});
}

function renderPartners(data) {
  state.financeWriteEnabled = data.financeWriteEnabled === true;
  el.badge.textContent = state.financeWriteEnabled ? 'FINANCE' : 'READ ONLY';
  setStatus(state.financeWriteEnabled ? '정산 변경 권한 활성 · 지급 전 금액 재검증' : '파트너 정산 조회 전용', true);
  const totals = data.totals || {};
  el.partnerCount.textContent = number(totals.partnerCount);
  el.partnerGross.textContent = `${money(totals.grossSalesKrw)}원`;
  el.partnerEarned.textContent = `${money(totals.earnedCommissionKrw)}원`;
  el.partnerPayable.textContent = `${money(totals.payableAmountKrw)}원`;
  el.partnerPaid.textContent = `${money(totals.paidAmountKrw)}원`;
  el.partnerGenerated.textContent = data.generatedAt ? `갱신 ${dateTime(data.generatedAt)} · 수수료 20% / 50%` : '수수료 20% / 50%';

  const rows = Array.isArray(data.partners) ? data.partners : [];
  el.partnerRows.replaceChildren();
  el.emptyPartners.hidden = rows.length > 0;
  for (const item of rows) {
    const month = item.month || {};
    const tr = document.createElement('tr');
    tr.append(
      stackedCell(item.email || shortId(item.ownerId), item.phone || shortId(item.ownerId)),
      cell(item.referralCode || '-'),
      cell(`${Number(item.commissionRatePercent) === 50 ? 50 : 20}%`),
      cell(`${number(item.referredCount)}명`),
      cell(`${number(item.activePaidCount)}명`),
      cell(`${money(month.grossSalesKrw)}원`),
      strongCell(`${money(month.earnedCommissionKrw)}원`),
      strongCell(`${money(month.payableAmountKrw)}원`),
      cell(`${money(month.paidAmountKrw)}원`),
      pill(settlementStatusLabel(month.status)),
      actionButton('보기', () => openPartner(item.ownerId)),
    );
    el.partnerRows.append(tr);
  }
}

async function openMember(ownerId) {
  const result = await get(`/admin/api/member?ownerId=${encodeURIComponent(ownerId)}`);
  if (!result.ok) { setStatus(result.error || '회원 상세를 불러오지 못했습니다.', false); return; }
  const data = result.data || {};
  const member = data.member || {};
  el.detailTitle.textContent = '회원 상세';
  el.detailId.textContent = shortId(member.ownerId);
  el.detailBody.replaceChildren(
    section('계정', [['이메일', member.email || '-'], ['전화번호', member.phone || '-'], ['가입일', dateTime(member.createdAt)], ['수정일', dateTime(member.updatedAt)]]),
    section('체험', data.trial ? [['시작', dateTime(data.trial.startedAt)], ['종료', dateTime(data.trial.endsAt)], ['추천 보너스', `${number(data.trial.referralBonusDays)}일`]] : [['상태', '없음']]),
    section('추천', [['추천 인원', `${number(data.referral?.referredCount)}명`], ['추천받음', data.referral?.wasReferred ? '예' : '아니오']]),
    section('파트너', [['적립 건수', `${number(data.partner?.commissionCount)}건`], ['적립 대기', `${money(data.partner?.pendingAmountKrw)}원`], ['확정', `${money(data.partner?.confirmedAmountKrw)}원`]]),
  );
  const subscriptions = Array.isArray(data.subscriptions) ? data.subscriptions : [];
  const subSection = document.createElement('section');
  subSection.className = 'section';
  subSection.append(heading('구독'));
  if (!subscriptions.length) subSection.append(detailRow('이용 상태', '구독 내역 없음'));
  for (const item of subscriptions) {
    subSection.append(
      detailRow('상품', products[item.productCode] || '기타 이용권'),
      detailRow('채널', channels[String(item.channel || '').toLowerCase()] || (item.channel ? '기타' : '-')),
      detailRow('이용 상태', subscriptionStatusLabel(item.status, item.expiresAt)),
      detailRow('결제 검증', paymentVerificationLabel(item)),
      detailRow('만료', dateTime(item.expiresAt)),
    );
  }
  el.detailBody.append(subSection);
  showDetail();
}

async function openPartner(ownerId) {
  const result = await get(`/admin/api/partner?ownerId=${encodeURIComponent(ownerId)}&month=${encodeURIComponent(state.month)}`);
  if (!result.ok) { setStatus(result.error || '파트너 상세를 불러오지 못했습니다.', false); return; }
  renderPartnerDetail(result.data || {});
  showDetail();
}

function renderPartnerDetail(data) {
  const partner = data.partner || {};
  const month = data.month || {};
  const financeEnabled = data.financeWriteEnabled === true;
  el.detailTitle.textContent = '파트너 정산 상세';
  el.detailId.textContent = shortId(partner.ownerId);
  el.detailBody.replaceChildren(
    section('파트너', [
      ['이메일', partner.email || '-'], ['전화번호', partner.phone || '-'], ['추천코드', partner.referralCode || '-'],
      ['추천 인원', `${number(partner.referredCount)}명`], ['유료 전환', `${number(partner.activePaidCount)}명`],
    ]),
    section(`${month.value || state.month} 정산`, [
      ['결제 매출', `${money(month.grossSalesKrw)}원`], ['발생 수수료', `${money(month.earnedCommissionKrw)}원`],
      ['지급완료', `${money(month.paidAmountKrw)}원`], ['미지급', `${money(month.payableAmountKrw)}원`],
    ]),
  );

  el.detailBody.append(rateControl(partner, financeEnabled));
  if (numberRaw(month.payableAmountKrw) > 0) el.detailBody.append(payControl(partner.ownerId, month, financeEnabled));
  el.detailBody.append(commissionSection(Array.isArray(data.commissions) ? data.commissions : []));
  el.detailBody.append(settlementSection(Array.isArray(data.settlements) ? data.settlements : []));
}

function rateControl(partner, financeEnabled) {
  const box = document.createElement('div');
  box.className = 'financebox';
  const title = document.createElement('strong');
  title.textContent = '파트너 수수료';
  const controls = document.createElement('div');
  controls.className = 'ratebuttons';
  const current = Number(partner.commissionRatePercent) === 50 ? 50 : 20;
  for (const rate of [20, 50]) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `ratebtn${current === rate ? ' selected' : ''}`;
    button.textContent = `${rate}%`;
    button.disabled = !financeEnabled || current === rate;
    button.addEventListener('click', () => updatePartnerRate(partner.ownerId, rate));
    controls.append(button);
  }
  const note = document.createElement('div');
  note.className = 'rate-note';
  note.textContent = financeEnabled ? '변경한 비율은 다음 신규 결제 커미션부터 적용됩니다.' : '정산 변경 권한이 있는 관리자만 수정할 수 있습니다.';
  box.append(title, controls, note);
  return box;
}

function payControl(ownerId, month, financeEnabled) {
  const box = document.createElement('div');
  box.className = 'paybox';
  const note = document.createElement('p');
  note.textContent = '실제 송금을 확인한 뒤 지급 처리하세요. 버튼은 은행 송금을 실행하지 않고 지급완료 원장을 기록합니다.';
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'paybtn';
  button.disabled = !financeEnabled;
  button.textContent = `정산 지급 · ${money(month.payableAmountKrw)}원`;
  button.addEventListener('click', () => paySettlement(ownerId, month.value || state.month, numberRaw(month.payableAmountKrw)));
  box.append(note, button);
  return box;
}

function commissionSection(items) {
  const sectionEl = document.createElement('section');
  sectionEl.className = 'section';
  sectionEl.append(heading('월별 수수료 내역'));
  const list = document.createElement('div');
  list.className = 'commission-list';
  if (!items.length) list.append(emptyLine('해당 월 수수료 내역이 없습니다.'));
  for (const item of items) {
    const row = document.createElement('div');
    row.className = 'commission-item';
    row.append(
      textSpan(item.referredEmail || shortId(item.referredOwnerId)),
      textSpan(`${products[item.productCode] || item.productCode || '-'} · ${money(item.baseAmountKrw)}원`),
      textSpan(`${number(item.effectiveRatePercent)}% · ${money(item.commissionAmountKrw)}원`, 'money-strong'),
      textSpan(item.paid ? '지급완료' : item.status === 'confirmed' ? '미지급' : item.status || '-'),
    );
    list.append(row);
  }
  sectionEl.append(list);
  return sectionEl;
}

function settlementSection(items) {
  const sectionEl = document.createElement('section');
  sectionEl.className = 'section';
  sectionEl.append(heading('정산 지급 내역'));
  const list = document.createElement('div');
  list.className = 'settlement-list';
  if (!items.length) list.append(emptyLine('지급완료 내역이 없습니다.'));
  for (const item of items) {
    const row = document.createElement('div');
    row.className = 'settlement-item';
    row.append(
      textSpan(item.month || '-'),
      textSpan(`${number(item.commissionCount)}건`),
      textSpan(`${money(item.paidAmountKrw)}원`, 'money-strong'),
      textSpan(dateTime(item.lastPaidAt)),
    );
    list.append(row);
  }
  sectionEl.append(list);
  return sectionEl;
}

async function updatePartnerRate(ownerId, rate) {
  if (!window.confirm(`이 파트너의 수수료를 ${rate}%로 변경할까요?\n기존 발생 수수료는 바뀌지 않고 다음 신규 결제부터 적용됩니다.`)) return;
  const result = await post('/admin/api/partner-rate', { ownerId, ratePercent: rate });
  if (!result.ok) { setStatus(result.error || '수수료 변경에 실패했습니다.', false); return; }
  setStatus(`파트너 수수료를 ${rate}%로 변경했습니다.`, true);
  await loadPartners();
  await openPartner(ownerId);
}

async function paySettlement(ownerId, month, expectedAmountKrw) {
  if (!expectedAmountKrw) return;
  const confirmed = window.confirm(`${month} 미지급 정산금 ${money(expectedAmountKrw)}원을 지급완료 처리할까요?\n\n실제 계좌 송금 확인 후 진행하세요. 같은 커미션은 다시 지급되지 않습니다.`);
  if (!confirmed) return;
  const result = await post('/admin/api/settlement-pay', { ownerId, month, expectedAmountKrw });
  if (!result.ok) {
    const suffix = result.data?.currentAmountKrw ? ` 현재 미지급 ${money(result.data.currentAmountKrw)}원` : '';
    setStatus(`${result.error || '정산 지급 처리에 실패했습니다.'}${suffix}`, false);
    return;
  }
  setStatus(`${month} 정산 ${money(result.data?.settlement?.paidAmountKrw)}원 지급완료 처리했습니다.`, true);
  await loadPartners();
  await openPartner(ownerId);
}

function memberSubscriptions(item) {
  if (Array.isArray(item?.subscriptions) && item.subscriptions.length) return item.subscriptions.filter((sub) => sub?.productCode);
  return item?.subscription?.productCode ? [item.subscription] : [];
}

function entitlementLabel(item) {
  const subscriptions = memberSubscriptions(item);
  if (!subscriptions.length) return trialIsActive(item) ? '무료체험' : '-';
  const codes = subscriptions.map((sub) => String(sub.productCode || '')).filter(Boolean);
  const uniqueCodes = [...new Set(codes)];
  const hasAll = uniqueCodes.includes('all_monthly');
  const labels = uniqueCodes.map((code) => products[code] || '기타 이용권');
  const duplicate = subscriptions.length > uniqueCodes.length || (hasAll && uniqueCodes.length > 1);
  if (hasAll) return duplicate ? '통합권 · 중복구독 확인' : '통합권';
  const base = labels.join(' + ') || '-';
  return duplicate ? `${base} · 중복구독 확인` : base;
}

function memberUsageStatus(item) {
  const subscriptions = memberSubscriptions(item);
  if (!subscriptions.length) return trialIsActive(item) ? '체험중' : '만료';
  const labels = subscriptions.map((sub) => subscriptionStatusLabel(sub.status, sub.expiresAt));
  if (labels.includes('활성')) return '활성';
  if (labels.includes('취소예정')) return '취소예정';
  if (labels.includes('정지')) return '정지';
  if (labels.includes('확인필요')) return '확인필요';
  if (labels.includes('만료')) return '만료';
  return labels[0] || '해당없음';
}

function subscriptionStatusLabel(status, expiresAt = '') {
  const value = String(status || '').trim().toLowerCase();
  const expiry = Date.parse(String(expiresAt || ''));
  const stillValid = !Number.isFinite(expiry) || expiry > Date.now();
  if (value === 'trial') return '체험중';
  if (value === 'active' || value === 'grace') return '활성';
  if (value === 'cancelled' || value === 'canceled') return stillValid ? '취소예정' : '만료';
  if (value === 'expired') return '만료';
  if (['suspended', 'paused', 'hold', 'on_hold'].includes(value)) return '정지';
  if (value === 'inactive') return '비활성';
  if (value === 'pending') return '확인필요';
  return value ? '확인필요' : '해당없음';
}

function paymentVerificationLabel(input) {
  let subscriptions = [];
  if (Array.isArray(input)) subscriptions = input.filter((sub) => sub?.productCode);
  else if (input?.productCode) subscriptions = [input];
  else if (Array.isArray(input?.subscriptions)) subscriptions = input.subscriptions.filter((sub) => sub?.productCode);
  if (!subscriptions.length) return '해당없음';
  const values = subscriptions.map((sub) => String(sub.verificationState || '').trim().toLowerCase());
  if (values.some((value) => ['failed', 'invalid', 'rejected'].includes(value))) return '실패';
  if (values.every((value) => value === 'verified')) return '정상';
  return '확인필요';
}

function trialIsActive(item) {
  const end = Date.parse(String(item?.trialEndsAt || ''));
  return Number.isFinite(end) && end > Date.now();
}

function showDetail() { el.backdrop.hidden = false; el.drawer.hidden = false; }
function closeDetail() { el.backdrop.hidden = true; el.drawer.hidden = true; el.detailBody.replaceChildren(); }

function section(title, rows) {
  const wrapper = document.createElement('section');
  wrapper.className = 'section';
  wrapper.append(heading(title));
  for (const [label, value] of rows) wrapper.append(detailRow(label, value));
  return wrapper;
}
function heading(value) { const h = document.createElement('h2'); h.textContent = value; return h; }
function detailRow(label, value) { const row = document.createElement('div'); row.className = 'row'; row.append(textSpan(label), textSpan(value || '-')); return row; }
function textSpan(value, className = '') { const span = document.createElement('span'); span.textContent = String(value ?? ''); if (className) span.className = className; return span; }
function emptyLine(value) { const div = document.createElement('div'); div.className = 'empty'; div.textContent = value; return div; }
function cell(value) { const td = document.createElement('td'); td.textContent = String(value ?? ''); return td; }
function strongCell(value) { const td = cell(value); td.className = 'money-strong'; return td; }
function stackedCell(primary, secondary) { const td = document.createElement('td'); const a = document.createElement('div'); const b = document.createElement('div'); a.textContent = primary || '-'; a.className = 'money-strong'; b.textContent = secondary || ''; b.className = 'rate-note'; td.append(a, b); return td; }
function pill(value) { const td = document.createElement('td'); const span = document.createElement('span'); const text = String(value || '-'); span.className = `pill ${pillClass(text)}`; span.textContent = text; td.append(span); return td; }
function actionButton(label, handler) { const td = document.createElement('td'); const button = document.createElement('button'); button.type = 'button'; button.className = 'viewbtn'; button.textContent = label; button.addEventListener('click', handler); td.append(button); return td; }
function pillClass(value) {
  if (['활성', '체험중', '정상', '지급완료'].includes(value)) return 'good';
  if (['취소예정', '확인필요', '미지급', '부분지급'].includes(value)) return 'warn';
  if (['만료', '정지', '비활성', '실패'].includes(value)) return 'bad';
  return '';
}
function settlementStatusLabel(value) { return value === 'paid' ? '지급완료' : value === 'partial' ? '부분지급' : value === 'pending' ? '미지급' : '내역없음'; }

async function get(url) { return request(url, { method: 'GET' }); }
async function post(url, body) { return request(url, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) }); }
async function request(url, init) {
  try {
    const response = await fetch(url, { ...init, cache: 'no-store', credentials: 'same-origin', headers: { accept: 'application/json', ...(init.headers || {}) } });
    let data = {}; try { data = await response.json(); } catch {}
    return { ok: response.ok && data?.ok !== false, status: response.status, error: String(data?.error || ''), data };
  } catch { return { ok: false, status: 0, error: '네트워크 연결을 확인해주세요.', data: {} }; }
}

function setStatus(value, ok) { el.status.textContent = value; el.status.classList.toggle('ok', !!ok); }
function numberRaw(value) { const parsed = Number(value || 0); return Number.isFinite(parsed) ? Math.max(0, Math.trunc(parsed)) : 0; }
function number(value) { return numberRaw(value).toLocaleString('ko-KR'); }
function money(value) { return number(value); }
function shortId(value) { const text = String(value || ''); return text.length > 18 ? `${text.slice(0,10)}…${text.slice(-5)}` : text || '-'; }
function dateOnly(value) { const date = new Date(String(value || '')); return Number.isFinite(date.getTime()) ? new Intl.DateTimeFormat('ko-KR', { year:'2-digit', month:'2-digit', day:'2-digit' }).format(date) : '-'; }
function dateTime(value) { const date = new Date(String(value || '')); return Number.isFinite(date.getTime()) ? new Intl.DateTimeFormat('ko-KR', { year:'2-digit', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }).format(date) : '-'; }
function currentMonth() { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`; }
