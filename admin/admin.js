const elements = {
  dashboard: document.getElementById('dashboard'),
  loginLayer: document.getElementById('loginLayer'),
  loginForm: document.getElementById('loginForm'),
  emailInput: document.getElementById('emailInput'),
  passwordInput: document.getElementById('passwordInput'),
  loginButton: document.getElementById('loginButton'),
  loginError: document.getElementById('loginError'),
  refreshButton: document.getElementById('refreshButton'),
  logoutButton: document.getElementById('logoutButton'),
  statusBar: document.getElementById('statusBar'),
  adminIdentity: document.getElementById('adminIdentity'),
  generatedAt: document.getElementById('generatedAt'),
  memberRows: document.getElementById('memberRows'),
  emptyMembers: document.getElementById('emptyMembers'),
  metricTotal: document.getElementById('metricTotal'),
  metricNew: document.getElementById('metricNew'),
  metricTrial: document.getElementById('metricTrial'),
  metricPaid: document.getElementById('metricPaid'),
  metricPaymentReview: document.getElementById('metricPaymentReview'),
  metricPartner: document.getElementById('metricPartner'),
  detailBackdrop: document.getElementById('detailBackdrop'),
  memberDetail: document.getElementById('memberDetail'),
  detailOwnerId: document.getElementById('detailOwnerId'),
  detailBody: document.getElementById('detailBody'),
  detailClose: document.getElementById('detailClose'),
};

const PRODUCT_LABELS = {
  all_monthly: '통합권',
  call_monthly: '전화관리',
  message_monthly: '문자자동화',
};

const STATUS_LABELS = {
  active: '활성',
  grace: '유예',
  cancelled: '해지 예정',
  expired: '만료',
  pending: '대기',
  suspended: '정지',
  verified: '검증됨',
};

boot();

function boot() {
  elements.loginForm.addEventListener('submit', onLogin);
  elements.refreshButton.addEventListener('click', () => loadOverview(true));
  elements.logoutButton.addEventListener('click', onLogout);
  elements.detailClose.addEventListener('click', closeDetail);
  elements.detailBackdrop.addEventListener('click', closeDetail);
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') closeDetail();
  });
  loadOverview(false);
}

async function onLogin(event) {
  event.preventDefault();
  setLoginError('');
  const email = elements.emailInput.value.trim();
  const password = elements.passwordInput.value;
  if (!email || !password) {
    setLoginError('이메일과 비밀번호를 입력해주세요.');
    return;
  }

  elements.loginButton.disabled = true;
  try {
    const result = await requestJson('/admin/api/login', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    if (!result.ok) {
      setLoginError(result.error || '로그인에 실패했습니다.');
      return;
    }
    elements.passwordInput.value = '';
    await loadOverview(false);
  } finally {
    elements.loginButton.disabled = false;
    elements.passwordInput.value = '';
  }
}

async function onLogout() {
  elements.logoutButton.disabled = true;
  try {
    await requestJson('/admin/api/logout', { method: 'POST' });
  } finally {
    elements.logoutButton.disabled = false;
    closeDetail();
    showLogin();
  }
}

async function loadOverview(manual) {
  if (manual) setStatus('');
  elements.refreshButton.disabled = true;
  const result = await requestJson('/admin/api/overview');
  elements.refreshButton.disabled = false;

  if (!result.ok) {
    if (result.status === 401 || result.code === 'CALLTAG_ADMIN_SESSION_REQUIRED') {
      showLogin();
      return;
    }
    elements.dashboard.hidden = true;
    elements.loginLayer.hidden = true;
    setStatus(result.error || '관리자 데이터를 불러오지 못했습니다.');
    return;
  }

  hideLogin();
  setStatus('');
  renderOverview(result.data);
}

function renderOverview(data) {
  elements.dashboard.hidden = false;
  const metrics = data?.metrics || {};
  elements.metricTotal.textContent = number(metrics.totalMembers);
  elements.metricNew.textContent = number(metrics.newMembers7d);
  elements.metricTrial.textContent = number(metrics.trialMembers);
  elements.metricPaid.textContent = number(metrics.activePaid);
  elements.metricPaymentReview.textContent = number(metrics.paymentReview);
  elements.metricPartner.textContent = number(metrics.partnerPending);
  elements.adminIdentity.textContent = String(data?.admin?.email || '');
  elements.generatedAt.textContent = data?.generatedAt ? `갱신 ${formatDateTime(data.generatedAt)}` : '';
  renderMembers(Array.isArray(data?.recentMembers) ? data.recentMembers : []);
}

function renderMembers(rows) {
  elements.memberRows.replaceChildren();
  elements.emptyMembers.hidden = rows.length > 0;

  for (const row of rows) {
    const tr = document.createElement('tr');
    tr.append(
      cell(shortOwner(row.ownerId), 'mono'),
      cell(row.email || '-'),
      cell(row.phone || '-'),
      cell(formatDate(row.createdAt)),
      cell(productLabel(row.subscription?.productCode)),
      pillCell(statusLabel(row.subscription?.status || trialStatus(row)), statusClass(row.subscription?.status || trialStatus(row))),
      pillCell(verificationLabel(row.subscription?.verificationState), verificationClass(row.subscription?.verificationState)),
      actionCell(row.ownerId),
    );
    elements.memberRows.append(tr);
  }
}

function actionCell(ownerId) {
  const td = document.createElement('td');
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'button-link';
  button.textContent = '보기';
  button.addEventListener('click', () => openMember(ownerId));
  td.append(button);
  return td;
}

async function openMember(ownerId) {
  if (!ownerId) return;
  setStatus('');
  const result = await requestJson(`/admin/api/member?ownerId=${encodeURIComponent(ownerId)}`);
  if (!result.ok) {
    if (result.status === 401) showLogin();
    else setStatus(result.error || '회원 상세를 불러오지 못했습니다.');
    return;
  }
  renderMemberDetail(result.data);
  elements.detailBackdrop.hidden = false;
  elements.memberDetail.hidden = false;
}

function renderMemberDetail(data) {
  const member = data?.member || {};
  elements.detailOwnerId.textContent = shortOwner(member.ownerId);
  elements.detailBody.replaceChildren();

  elements.detailBody.append(section('계정', [
    ['이메일', member.email || '-'],
    ['전화번호', member.phone || '-'],
    ['가입일', formatDateTime(member.createdAt)],
    ['수정일', formatDateTime(member.updatedAt)],
  ]));

  const trial = data?.trial;
  elements.detailBody.append(section('체험', trial ? [
    ['시작', formatDateTime(trial.startedAt)],
    ['종료', formatDateTime(trial.endsAt)],
    ['추천 보너스', `${number(trial.referralBonusDays)}일`],
  ] : [['상태', '없음']]));

  const subscriptions = Array.isArray(data?.subscriptions) ? data.subscriptions : [];
  const subSection = document.createElement('section');
  subSection.className = 'detail-section';
  const subTitle = document.createElement('h2');
  subTitle.textContent = '구독';
  subSection.append(subTitle);
  if (!subscriptions.length) {
    subSection.append(detailRow('상태', '구독 내역 없음'));
  } else {
    for (const subscription of subscriptions) {
      const box = document.createElement('div');
      box.className = 'subscription-box';
      box.append(
        detailRow('상품', productLabel(subscription.productCode)),
        detailRow('채널', subscription.channel || '-'),
        detailRow('상태', statusLabel(subscription.status)),
        detailRow('검증', verificationLabel(subscription.verificationState)),
        detailRow('만료', formatDateTime(subscription.expiresAt)),
        detailRow('최근 검증', formatDateTime(subscription.lastVerifiedAt)),
      );
      subSection.append(box);
    }
  }
  elements.detailBody.append(subSection);

  const referral = data?.referral || {};
  elements.detailBody.append(section('추천', [
    ['추천 인원', `${number(referral.referredCount)}명`],
    ['추천받음', referral.wasReferred ? '예' : '아니오'],
    ['지급 보너스', `${number(referral.issuedBonusDays)}일`],
  ]));

  const partner = data?.partner || {};
  elements.detailBody.append(section('파트너', [
    ['적립 건수', `${number(partner.commissionCount)}건`],
    ['적립 대기', `${currency(partner.pendingAmountKrw)}원`],
    ['확정', `${currency(partner.confirmedAmountKrw)}원`],
  ]));
}

function section(title, rows) {
  const wrapper = document.createElement('section');
  wrapper.className = 'detail-section';
  const heading = document.createElement('h2');
  heading.textContent = title;
  wrapper.append(heading);
  for (const [label, value] of rows) wrapper.append(detailRow(label, value));
  return wrapper;
}

function detailRow(label, value) {
  const row = document.createElement('div');
  row.className = 'detail-row';
  const key = document.createElement('span');
  const val = document.createElement('span');
  key.textContent = String(label || '');
  val.textContent = String(value || '-');
  row.append(key, val);
  return row;
}

function cell(value, className = '') {
  const td = document.createElement('td');
  td.textContent = String(value ?? '');
  if (className) td.className = className;
  return td;
}

function pillCell(value, type = '') {
  const td = document.createElement('td');
  const span = document.createElement('span');
  span.className = `status-pill${type ? ` ${type}` : ''}`;
  span.textContent = String(value || '-');
  td.append(span);
  return td;
}

async function requestJson(url, options = {}) {
  try {
    const response = await fetch(url, {
      credentials: 'same-origin',
      cache: 'no-store',
      ...options,
    });
    let data = {};
    try { data = await response.json(); } catch { data = {}; }
    return {
      ok: response.ok && data?.ok !== false,
      status: response.status,
      error: String(data?.error || ''),
      code: String(data?.code || ''),
      data,
    };
  } catch {
    return { ok: false, status: 0, error: '네트워크 연결을 확인해주세요.', code: 'NETWORK_ERROR', data: {} };
  }
}

function showLogin() {
  elements.dashboard.hidden = true;
  elements.loginLayer.hidden = false;
  elements.adminIdentity.textContent = '';
  setStatus('');
  setLoginError('');
  window.setTimeout(() => elements.emailInput.focus(), 0);
}

function hideLogin() {
  elements.loginLayer.hidden = true;
  setLoginError('');
}

function closeDetail() {
  elements.detailBackdrop.hidden = true;
  elements.memberDetail.hidden = true;
  elements.detailBody.replaceChildren();
}

function setLoginError(message) {
  elements.loginError.textContent = String(message || '');
  elements.loginError.hidden = !message;
}

function setStatus(message) {
  elements.statusBar.textContent = String(message || '');
  elements.statusBar.hidden = !message;
}

function productLabel(code) {
  const value = String(code || '');
  return PRODUCT_LABELS[value] || (value ? value : '-');
}

function statusLabel(status) {
  const value = String(status || '');
  return STATUS_LABELS[value] || (value ? value : '-');
}

function verificationLabel(status) {
  const value = String(status || '');
  if (!value) return '-';
  return value === 'verified' ? '검증됨' : '확인 필요';
}

function trialStatus(row) {
  const end = Date.parse(String(row?.trialEndsAt || ''));
  return Number.isFinite(end) && end > Date.now() ? 'trial' : 'inactive';
}

function statusClass(status) {
  if (['active', 'grace', 'verified', 'trial'].includes(status)) return 'good';
  if (['pending', 'cancelled'].includes(status)) return 'warn';
  if (['expired', 'suspended', 'inactive'].includes(status)) return 'bad';
  return '';
}

function verificationClass(status) {
  if (!status) return '';
  return status === 'verified' ? 'good' : 'warn';
}

function shortOwner(value) {
  const owner = String(value || '');
  if (owner.length <= 18) return owner || '-';
  return `${owner.slice(0, 10)}…${owner.slice(-5)}`;
}

function formatDate(value) {
  const date = new Date(String(value || ''));
  if (!Number.isFinite(date.getTime())) return '-';
  return new Intl.DateTimeFormat('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit' }).format(date);
}

function formatDateTime(value) {
  const date = new Date(String(value || ''));
  if (!Number.isFinite(date.getTime())) return '-';
  return new Intl.DateTimeFormat('ko-KR', {
    year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  }).format(date);
}

function number(value) {
  const parsed = Number(value || 0);
  return Number.isFinite(parsed) ? Math.max(0, Math.trunc(parsed)).toLocaleString('ko-KR') : '0';
}

function currency(value) {
  const parsed = Number(value || 0);
  return Number.isFinite(parsed) ? Math.max(0, Math.trunc(parsed)).toLocaleString('ko-KR') : '0';
}
