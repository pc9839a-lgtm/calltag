const ADMIN_ROOT = '/admin';
const ADMIN_API_PREFIX = '/admin/api/';
const ADMIN_API_BASE = 'https://inlet-8mr.pages.dev';

export async function handleCalltagAdmin(request, env) {
  const url = new URL(request.url);
  if (url.pathname !== ADMIN_ROOT && !url.pathname.startsWith(`${ADMIN_ROOT}/`)) return null;

  const accessAssertion = String(request.headers.get('CF-Access-Jwt-Assertion') || '').trim();
  if (!accessAssertion) {
    return url.pathname.startsWith(ADMIN_API_PREFIX)
      ? adminJson({ ok: false, error: 'Cloudflare Access 인증이 필요합니다.', code: 'CALLTAG_ADMIN_ACCESS_REQUIRED' }, 401)
      : adminText('관리자 접근이 차단되었습니다.', 403);
  }

  if (url.pathname === ADMIN_ROOT) {
    return hardenedResponse(Response.redirect(`${url.origin}${ADMIN_ROOT}/`, 302));
  }

  if (url.pathname.startsWith(ADMIN_API_PREFIX)) {
    return handleAdminApi(request, url, accessAssertion);
  }

  if (!['GET', 'HEAD'].includes(request.method)) {
    return adminText('Method not allowed.', 405, { allow: 'GET, HEAD' });
  }

  const assetUrl = new URL(request.url);
  if (assetUrl.pathname === `${ADMIN_ROOT}/`) assetUrl.pathname = `${ADMIN_ROOT}/index.html`;
  const assetRequest = new Request(assetUrl.toString(), request);
  const response = await env.ASSETS.fetch(assetRequest);
  return hardenedResponse(response);
}

async function handleAdminApi(request, url, accessAssertion) {
  if (request.method !== 'GET') {
    return adminJson({ ok: false, error: 'Method not allowed.', code: 'METHOD_NOT_ALLOWED' }, 405, { allow: 'GET' });
  }

  if (url.pathname === `${ADMIN_API_PREFIX}overview`) {
    return proxyRead(accessAssertion, '/api/call/admin/overview', sanitizeOverview);
  }

  if (url.pathname === `${ADMIN_API_PREFIX}member`) {
    const ownerId = String(url.searchParams.get('ownerId') || '').trim();
    if (!/^[A-Za-z0-9._:-]{3,120}$/.test(ownerId)) {
      return adminJson({ ok: false, error: '회원 식별자가 올바르지 않습니다.', code: 'CALLTAG_ADMIN_MEMBER_ID_INVALID' }, 400);
    }
    return proxyRead(
      accessAssertion,
      `/api/call/admin/member?ownerId=${encodeURIComponent(ownerId)}`,
      sanitizeMember,
    );
  }

  return adminJson({ ok: false, error: 'Not found.', code: 'NOT_FOUND' }, 404);
}

async function proxyRead(accessAssertion, path, sanitizer) {
  let response;
  try {
    response = await fetch(`${ADMIN_API_BASE}${path}`, {
      method: 'GET',
      headers: {
        accept: 'application/json',
        'cf-access-jwt-assertion': accessAssertion,
        'x-calllink-client': 'calltag-admin-gateway',
      },
      redirect: 'manual',
    });
  } catch {
    return adminJson({ ok: false, error: '관리자 API에 연결하지 못했습니다.', code: 'ADMIN_UPSTREAM_UNAVAILABLE' }, 502);
  }

  const data = await safeJson(response);
  if (!response.ok || data?.ok === false) {
    return adminJson({
      ok: false,
      error: safeErrorMessage(data?.error, '관리자 요청에 실패했습니다.'),
      code: safeCode(data?.code || 'ADMIN_REQUEST_FAILED'),
    }, normalizeUpstreamStatus(response.status));
  }

  return adminJson(sanitizer(data), 200);
}

function sanitizeOverview(data) {
  const metrics = data?.metrics || {};
  const rows = Array.isArray(data?.recentMembers) ? data.recentMembers.slice(0, 40) : [];
  return {
    ok: true,
    readOnly: true,
    admin: { email: safeMasked(data?.admin?.email, 320) },
    metrics: {
      totalMembers: safeNumber(metrics.totalMembers),
      newMembers7d: safeNumber(metrics.newMembers7d),
      trialMembers: safeNumber(metrics.trialMembers),
      activePaid: safeNumber(metrics.activePaid),
      paymentReview: safeNumber(metrics.paymentReview),
      partnerPending: safeNumber(metrics.partnerPending),
    },
    recentMembers: rows.map((row) => ({
      ownerId: safeOwnerId(row?.ownerId),
      email: safeMasked(row?.email, 320),
      phone: safeMasked(row?.phone, 40),
      createdAt: safeDate(row?.createdAt),
      updatedAt: safeDate(row?.updatedAt),
      trialEndsAt: safeDate(row?.trialEndsAt),
      referralBonusDays: safeNumber(row?.referralBonusDays),
      subscription: sanitizeSubscription(row?.subscription),
    })).filter((row) => row.ownerId),
    generatedAt: safeDate(data?.generatedAt),
  };
}

function sanitizeMember(data) {
  const member = data?.member || {};
  const trial = data?.trial || null;
  const referral = data?.referral || {};
  const partner = data?.partner || {};
  const subscriptions = Array.isArray(data?.subscriptions) ? data.subscriptions.slice(0, 20) : [];
  return {
    ok: true,
    readOnly: true,
    member: {
      ownerId: safeOwnerId(member.ownerId),
      email: safeMasked(member.email, 320),
      phone: safeMasked(member.phone, 40),
      createdAt: safeDate(member.createdAt),
      updatedAt: safeDate(member.updatedAt),
    },
    trial: trial ? {
      startedAt: safeDate(trial.startedAt),
      endsAt: safeDate(trial.endsAt),
      referralBonusDays: safeNumber(trial.referralBonusDays),
    } : null,
    subscriptions: subscriptions.map(sanitizeSubscription).filter(Boolean),
    referral: {
      referredCount: safeNumber(referral.referredCount),
      wasReferred: referral.wasReferred === true,
      issuedBonusDays: safeNumber(referral.issuedBonusDays),
    },
    partner: {
      commissionCount: safeNumber(partner.commissionCount),
      pendingAmountKrw: safeNumber(partner.pendingAmountKrw),
      confirmedAmountKrw: safeNumber(partner.confirmedAmountKrw),
    },
  };
}

function sanitizeSubscription(row) {
  if (!row || typeof row !== 'object') return null;
  return {
    productCode: safeToken(row.productCode, 80),
    channel: safeToken(row.channel, 32),
    status: safeToken(row.status, 32),
    verificationState: safeToken(row.verificationState, 32),
    startedAt: safeDate(row.startedAt),
    nextBillingAt: safeDate(row.nextBillingAt),
    expiresAt: safeDate(row.expiresAt),
    lastVerifiedAt: safeDate(row.lastVerifiedAt),
    autoRenewing: row.autoRenewing === true,
  };
}

async function safeJson(response) {
  const text = await response.text().catch(() => '');
  if (!text || text.length > 512 * 1024) return {};
  try { return JSON.parse(text); } catch { return {}; }
}

function adminJson(body, status = 200, extraHeaders = {}) {
  const headers = new Headers({
    'content-type': 'application/json; charset=utf-8',
    ...extraHeaders,
  });
  return hardenedResponse(new Response(JSON.stringify(body), { status, headers }));
}

function adminText(text, status = 200, extraHeaders = {}) {
  const headers = new Headers({ 'content-type': 'text/plain; charset=utf-8', ...extraHeaders });
  return hardenedResponse(new Response(String(text || ''), { status, headers }));
}

function hardenedResponse(response) {
  const headers = new Headers(response.headers);
  headers.set('cache-control', 'no-store, max-age=0');
  headers.set('pragma', 'no-cache');
  headers.set('content-security-policy', "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
  headers.set('referrer-policy', 'no-referrer');
  headers.set('x-content-type-options', 'nosniff');
  headers.set('x-frame-options', 'DENY');
  headers.set('x-robots-tag', 'noindex, nofollow, noarchive, nosnippet');
  headers.set('permissions-policy', 'camera=(), microphone=(), geolocation=(), payment=(), usb=(), bluetooth=()');
  headers.set('cross-origin-opener-policy', 'same-origin');
  headers.set('cross-origin-resource-policy', 'same-origin');
  headers.delete('etag');
  headers.delete('last-modified');
  headers.delete('content-length');
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers,
  });
}

function safeOwnerId(value) {
  const text = String(value || '').trim();
  return /^[A-Za-z0-9._:-]{3,120}$/.test(text) ? text : '';
}

function safeMasked(value, max) {
  return String(value || '').replace(/[\r\n<>]/g, '').slice(0, max);
}

function safeToken(value, max) {
  return String(value || '').replace(/[^A-Za-z0-9._:-]/g, '').slice(0, max);
}

function safeDate(value) {
  const parsed = Date.parse(String(value || ''));
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : '';
}

function safeNumber(value) {
  const parsed = Number(value || 0);
  if (!Number.isFinite(parsed) || parsed < 0) return 0;
  return Math.min(Number.MAX_SAFE_INTEGER, Math.trunc(parsed));
}

function safeCode(value) {
  const code = String(value || '').replace(/[^A-Z0-9_:-]/gi, '').slice(0, 80);
  return code || 'ADMIN_REQUEST_FAILED';
}

function safeErrorMessage(value, fallback) {
  const message = String(value || '').replace(/[\r\n<>]/g, ' ').trim().slice(0, 160);
  return message || fallback;
}

function normalizeUpstreamStatus(value) {
  const status = Number(value || 500);
  return status >= 400 && status <= 599 ? status : 502;
}
