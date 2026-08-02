const INGEST_PATH = '/api/integrations/pagero/leads';
const LIST_PATH = '/api/call/pagero/leads';
const ACK_PATH = '/api/call/pagero/leads/ack';
const HEALTH_PATH = '/api/integrations/pagero/health';
const MAX_BODY_BYTES = 64 * 1024;
const MAX_CLOCK_SKEW_MS = 5 * 60 * 1000;
const DEFAULT_AUTH_BASES = [
  'https://inlet-8mr.pages.dev',
  'https://call.pagero.kr',
  'https://pagero.kr'
];

export async function handlePageroIntegration(request, env) {
  const url = new URL(request.url);
  if (![INGEST_PATH, LIST_PATH, ACK_PATH, HEALTH_PATH].includes(url.pathname)) return null;

  const requestId = request.headers.get('cf-ray') || crypto.randomUUID();
  try {
    if (url.pathname === HEALTH_PATH) {
      if (request.method !== 'GET') return methodNotAllowed(requestId, ['GET']);
      return json({
        ok: true,
        service: 'pagero-calltag-integration',
        storageConfigured: Boolean(env.DB),
        webhookSecretConfigured: Boolean(env.PAGERO_WEBHOOK_SECRET),
        requestId
      }, 200);
    }

    if (!env.DB) {
      return apiError('integration_storage_not_configured', '페이지로 연동 저장소가 아직 연결되지 않았습니다.', 503, requestId);
    }

    if (url.pathname === INGEST_PATH) {
      if (request.method !== 'POST') return methodNotAllowed(requestId, ['POST']);
      return ingestLead(request, env, requestId);
    }

    if (url.pathname === LIST_PATH) {
      if (request.method !== 'GET') return methodNotAllowed(requestId, ['GET']);
      return listLeads(request, env, url, requestId);
    }

    if (url.pathname === ACK_PATH) {
      if (request.method !== 'POST') return methodNotAllowed(requestId, ['POST']);
      return acknowledgeLeads(request, env, requestId);
    }

    return apiError('not_found', '요청한 연동 API를 찾을 수 없습니다.', 404, requestId);
  } catch (error) {
    console.error('pagero-integration-error', requestId, safeError(error));
    if (isMissingTableError(error)) {
      return apiError('integration_storage_not_ready', '페이지로 연동 DB 마이그레이션이 필요합니다.', 503, requestId);
    }
    return apiError('integration_internal_error', '페이지로 연동 요청을 처리하지 못했습니다.', 500, requestId);
  }
}

async function ingestLead(request, env, requestId) {
  const secret = clean(env.PAGERO_WEBHOOK_SECRET, 512);
  if (!secret) {
    return apiError('webhook_secret_not_configured', '페이지로 수신 보안키가 아직 설정되지 않았습니다.', 503, requestId);
  }

  const contentLength = Number(request.headers.get('content-length') || '0');
  if (contentLength > MAX_BODY_BYTES) {
    return apiError('payload_too_large', '문의 데이터 크기가 허용 범위를 초과했습니다.', 413, requestId);
  }

  const rawBody = await request.text();
  if (new TextEncoder().encode(rawBody).byteLength > MAX_BODY_BYTES) {
    return apiError('payload_too_large', '문의 데이터 크기가 허용 범위를 초과했습니다.', 413, requestId);
  }

  const timestamp = request.headers.get('x-pagero-timestamp') || '';
  const signature = request.headers.get('x-pagero-signature') || '';
  const signatureResult = await verifyWebhookSignature(secret, timestamp, rawBody, signature);
  if (!signatureResult.ok) {
    return apiError(signatureResult.code, signatureResult.message, 401, requestId);
  }

  let payload;
  try {
    payload = JSON.parse(rawBody);
  } catch {
    return apiError('invalid_json', '문의 데이터가 올바른 JSON 형식이 아닙니다.', 400, requestId);
  }

  const validation = validateLeadPayload(payload);
  if (!validation.ok) {
    return apiError('invalid_payload', validation.message, 400, requestId, validation.field);
  }

  const lead = validation.lead;
  const now = Date.now();
  const result = await env.DB.prepare(`
    INSERT INTO pagero_leads (
      event_id, workspace_key, site_id, customer_name, primary_phone,
      normalized_phone, email, inquiry_content, source_url, campaign,
      metadata_json, submitted_at, status, created_at, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
    ON CONFLICT(event_id) DO NOTHING
  `).bind(
    lead.eventId,
    lead.workspaceKey,
    lead.siteId,
    lead.customerName,
    lead.primaryPhone,
    lead.normalizedPhone,
    lead.email,
    lead.inquiryContent,
    lead.sourceUrl,
    lead.campaign,
    lead.metadataJson,
    lead.submittedAt,
    now,
    now
  ).run();

  const stored = await env.DB.prepare(
    'SELECT id, event_id, status, created_at FROM pagero_leads WHERE event_id = ? LIMIT 1'
  ).bind(lead.eventId).first();

  const created = Number(result?.meta?.changes || 0) > 0;
  return json({
    ok: true,
    created,
    duplicate: !created,
    lead: stored ? {
      id: stored.id,
      eventId: stored.event_id,
      status: stored.status,
      createdAt: stored.created_at
    } : null,
    requestId
  }, created ? 201 : 200);
}

async function listLeads(request, env, url, requestId) {
  const identity = await requireCallTagIdentity(request, env, requestId);
  if (identity.response) return identity.response;

  const after = clampInteger(url.searchParams.get('after'), 0, Number.MAX_SAFE_INTEGER, 0);
  const limit = clampInteger(url.searchParams.get('limit'), 1, 100, 50);
  const rows = await env.DB.prepare(`
    SELECT id, event_id, site_id, customer_name, primary_phone, normalized_phone,
           email, inquiry_content, source_url, campaign, metadata_json,
           submitted_at, status, created_at, updated_at
      FROM pagero_leads
     WHERE workspace_key = ?
       AND id > ?
       AND status IN ('PENDING', 'DELIVERED')
     ORDER BY id ASC
     LIMIT ?
  `).bind(identity.workspaceKey, after, limit).all();

  const leads = (rows?.results || []).map(row => ({
    id: row.id,
    eventId: row.event_id,
    siteId: row.site_id,
    customer: {
      name: row.customer_name,
      phone: row.primary_phone,
      normalizedPhone: row.normalized_phone,
      email: row.email || ''
    },
    inquiry: {
      content: row.inquiry_content || '',
      sourceUrl: row.source_url || '',
      campaign: row.campaign || ''
    },
    metadata: parseMetadata(row.metadata_json),
    submittedAt: row.submitted_at,
    status: row.status,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  }));

  if (leads.length > 0) {
    const now = Date.now();
    await env.DB.batch(leads.map(lead => env.DB.prepare(`
      UPDATE pagero_leads
         SET status = CASE WHEN status = 'PENDING' THEN 'DELIVERED' ELSE status END,
             delivered_at = COALESCE(delivered_at, ?),
             updated_at = ?
       WHERE id = ? AND workspace_key = ?
    `).bind(now, now, lead.id, identity.workspaceKey)));
  }

  return json({
    ok: true,
    workspaceKey: identity.workspaceKey,
    leads,
    nextAfter: leads.length ? leads[leads.length - 1].id : after,
    hasMore: leads.length === limit,
    requestId
  }, 200);
}

async function acknowledgeLeads(request, env, requestId) {
  const identity = await requireCallTagIdentity(request, env, requestId);
  if (identity.response) return identity.response;

  const bodyResult = await readJsonBody(request);
  if (!bodyResult.ok) return apiError(bodyResult.code, bodyResult.message, bodyResult.status, requestId);

  const ids = Array.isArray(bodyResult.value.leadIds)
    ? [...new Set(bodyResult.value.leadIds.map(Number).filter(Number.isSafeInteger))]
    : [];
  if (!ids.length || ids.length > 100) {
    return apiError('invalid_lead_ids', '처리 완료할 문의 ID를 1~100개 전달해야 합니다.', 400, requestId, 'leadIds');
  }

  const status = clean(bodyResult.value.status, 20).toUpperCase();
  if (!['IMPORTED', 'REJECTED'].includes(status)) {
    return apiError('invalid_ack_status', '처리 상태는 IMPORTED 또는 REJECTED만 가능합니다.', 400, requestId, 'status');
  }

  const resultText = clean(bodyResult.value.result, 500);
  const now = Date.now();
  const results = await env.DB.batch(ids.map(id => env.DB.prepare(`
    UPDATE pagero_leads
       SET status = ?, imported_at = CASE WHEN ? = 'IMPORTED' THEN ? ELSE imported_at END,
           import_result = ?, updated_at = ?
     WHERE id = ?
       AND workspace_key = ?
       AND status IN ('PENDING', 'DELIVERED', 'REJECTED')
  `).bind(status, status, now, resultText, now, id, identity.workspaceKey)));

  const updated = results.reduce((sum, item) => sum + Number(item?.meta?.changes || 0), 0);
  return json({ ok: true, status, requested: ids.length, updated, requestId }, 200);
}

async function requireCallTagIdentity(request, env, requestId) {
  const session = extractSession(request);
  if (!session) {
    return { response: apiError('session_required', '콜태그 로그인이 필요합니다.', 401, requestId) };
  }

  const bases = authBases(env);
  let lastError = null;
  for (const base of bases) {
    try {
      const response = await fetch(`${base}/api/call/session`, {
        method: 'POST',
        headers: {
          'content-type': 'application/json; charset=utf-8',
          'accept': 'application/json',
          'x-inlet-session': session,
          'x-pagero-product': 'calltag',
          'x-calllink-client': 'cloudflare-worker'
        },
        body: JSON.stringify({ session })
      });
      const text = await response.text();
      let data = {};
      try { data = text ? JSON.parse(text) : {}; } catch { data = {}; }
      if (!response.ok || data.ok === false) {
        lastError = new Error(`auth_${response.status}`);
        if (![404, 405, 408].includes(response.status) && response.status < 500) break;
        continue;
      }
      const workspaceKey = deriveWorkspaceKey(data);
      if (!workspaceKey) {
        return { response: apiError('workspace_identity_missing', '로그인 계정의 연동 식별자를 확인하지 못했습니다.', 409, requestId) };
      }
      return { workspaceKey, profile: data.profile || data.user || {} };
    } catch (error) {
      lastError = error;
    }
  }

  console.warn('pagero-auth-validation-failed', requestId, safeError(lastError));
  return { response: apiError('invalid_session', '콜태그 로그인 세션을 확인하지 못했습니다.', 401, requestId) };
}

function validateLeadPayload(payload) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return invalid('payload', '문의 데이터가 객체 형식이 아닙니다.');
  }

  const customer = payload.customer && typeof payload.customer === 'object' ? payload.customer : {};
  const inquiry = payload.inquiry && typeof payload.inquiry === 'object' ? payload.inquiry : {};
  const eventId = clean(payload.event_id || payload.eventId, 128);
  const workspaceKey = normalizeWorkspaceKey(
    payload.workspace_key || payload.workspaceKey || payload.workspace_id || payload.workspaceId || payload.owner_email
  );
  const siteId = clean(payload.site_id || payload.siteId, 128);
  const customerName = clean(customer.name || payload.name, 80) || '이름 없는 고객';
  const primaryPhone = clean(customer.phone || payload.phone, 40);
  const normalizedPhone = normalizePhone(primaryPhone);
  const email = normalizeEmail(customer.email || payload.email);
  const inquiryContent = clean(inquiry.content || payload.inquiry_content || payload.inquiryContent, 2000);
  const sourceUrl = clean(inquiry.source_url || inquiry.sourceUrl || payload.source_url, 1000);
  const campaign = clean(inquiry.campaign || payload.campaign, 200);
  const submittedAt = parseSubmittedAt(payload.submitted_at || payload.submittedAt);
  const metadataJson = safeMetadata(payload.metadata);

  if (eventId.length < 8) return invalid('event_id', '페이지로 이벤트 ID가 필요합니다.');
  if (!workspaceKey) return invalid('workspace_key', '콜태그 계정과 연결할 workspace_key가 필요합니다.');
  if (!siteId) return invalid('site_id', '페이지로 랜딩페이지 site_id가 필요합니다.');
  if (normalizedPhone.length < 8 || normalizedPhone.length > 15) {
    return invalid('customer.phone', '고객 전화번호를 정확히 전달해야 합니다.');
  }

  return {
    ok: true,
    lead: {
      eventId, workspaceKey, siteId, customerName, primaryPhone, normalizedPhone,
      email, inquiryContent, sourceUrl, campaign, submittedAt, metadataJson
    }
  };
}

async function verifyWebhookSignature(secret, timestampValue, rawBody, providedValue) {
  const rawTimestamp = clean(timestampValue, 32);
  const timestampNumber = Number(rawTimestamp);
  if (!rawTimestamp || !Number.isFinite(timestampNumber)) {
    return { ok: false, code: 'timestamp_required', message: '페이지로 요청 시간이 필요합니다.' };
  }
  const timestampMs = timestampNumber > 10_000_000_000 ? timestampNumber : timestampNumber * 1000;
  if (Math.abs(Date.now() - timestampMs) > MAX_CLOCK_SKEW_MS) {
    return { ok: false, code: 'stale_request', message: '페이지로 요청 유효시간이 지났습니다.' };
  }

  const provided = clean(providedValue, 256).replace(/^sha256=/i, '').toLowerCase();
  if (!/^[a-f0-9]{64}$/.test(provided)) {
    return { ok: false, code: 'signature_required', message: '올바른 페이지로 서명이 필요합니다.' };
  }

  const key = await crypto.subtle.importKey(
    'raw', new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']
  );
  const signed = await crypto.subtle.sign(
    'HMAC', key, new TextEncoder().encode(`${rawTimestamp}.${rawBody}`)
  );
  const expected = [...new Uint8Array(signed)].map(byte => byte.toString(16).padStart(2, '0')).join('');
  return timingSafeEqual(expected, provided)
    ? { ok: true }
    : { ok: false, code: 'invalid_signature', message: '페이지로 요청 서명이 일치하지 않습니다.' };
}

function deriveWorkspaceKey(data) {
  const profile = data?.profile || data?.user || {};
  const workspace = data?.workspace || {};
  const candidates = [
    workspace.id, workspace.key, data?.workspaceId, data?.workspace_id,
    profile.workspaceId, profile.workspace_id, profile.userId, profile.user_id,
    profile.id, profile.email
  ];
  for (const candidate of candidates) {
    const value = normalizeWorkspaceKey(candidate);
    if (value) return value;
  }
  return '';
}

function authBases(env) {
  const configured = clean(env.CALLTAG_AUTH_BASE_URL, 500).replace(/\/$/, '');
  return [...new Set([configured, ...DEFAULT_AUTH_BASES].filter(Boolean))];
}

function extractSession(request) {
  const direct = clean(request.headers.get('x-inlet-session'), 4096);
  if (direct) return direct;
  const authorization = clean(request.headers.get('authorization'), 4096);
  return authorization.toLowerCase().startsWith('bearer ') ? authorization.slice(7).trim() : '';
}

async function readJsonBody(request) {
  const contentLength = Number(request.headers.get('content-length') || '0');
  if (contentLength > MAX_BODY_BYTES) {
    return { ok: false, code: 'payload_too_large', message: '요청 데이터가 너무 큽니다.', status: 413 };
  }
  const text = await request.text();
  if (new TextEncoder().encode(text).byteLength > MAX_BODY_BYTES) {
    return { ok: false, code: 'payload_too_large', message: '요청 데이터가 너무 큽니다.', status: 413 };
  }
  try {
    return { ok: true, value: text ? JSON.parse(text) : {} };
  } catch {
    return { ok: false, code: 'invalid_json', message: '요청 데이터가 올바른 JSON 형식이 아닙니다.', status: 400 };
  }
}

function normalizeWorkspaceKey(value) {
  return clean(value, 180).toLowerCase();
}

function normalizeEmail(value) {
  const email = clean(value, 254).toLowerCase();
  return email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? email : '';
}

function normalizePhone(value) {
  let digits = clean(value, 40).replace(/\D/g, '');
  if (digits.startsWith('82') && digits.length >= 10) digits = `0${digits.slice(2)}`;
  return digits;
}

function parseSubmittedAt(value) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value > 10_000_000_000 ? Math.round(value) : Math.round(value * 1000);
  }
  const parsed = Date.parse(clean(value, 80));
  return Number.isFinite(parsed) ? parsed : Date.now();
}

function safeMetadata(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return '{}';
  try {
    const text = JSON.stringify(value);
    return text.length <= 8192 ? text : JSON.stringify({ truncated: true });
  } catch {
    return '{}';
  }
}

function parseMetadata(value) {
  try { return value ? JSON.parse(value) : {}; } catch { return {}; }
}

function clean(value, maxLength = 1000) {
  return String(value ?? '').trim().replace(/\u0000/g, '').slice(0, maxLength);
}

function clampInteger(value, min, max, fallback) {
  const number = Number(value);
  return Number.isSafeInteger(number) ? Math.max(min, Math.min(max, number)) : fallback;
}

function timingSafeEqual(left, right) {
  if (left.length !== right.length) return false;
  let difference = 0;
  for (let index = 0; index < left.length; index += 1) {
    difference |= left.charCodeAt(index) ^ right.charCodeAt(index);
  }
  return difference === 0;
}

function invalid(field, message) {
  return { ok: false, field, message };
}

function methodNotAllowed(requestId, allow) {
  const response = apiError('method_not_allowed', '허용되지 않은 요청 방식입니다.', 405, requestId);
  response.headers.set('allow', allow.join(', '));
  return response;
}

function apiError(code, message, status, requestId, field = '') {
  return json({ ok: false, error: message, details: { code, field }, requestId }, status);
}

function json(value, status) {
  return new Response(JSON.stringify(value), {
    status,
    headers: {
      'content-type': 'application/json; charset=UTF-8',
      'cache-control': 'no-store',
      'x-content-type-options': 'nosniff'
    }
  });
}

function isMissingTableError(error) {
  const message = safeError(error).toLowerCase();
  return message.includes('no such table') || message.includes('d1_error') && message.includes('pagero_leads');
}

function safeError(error) {
  return error instanceof Error ? `${error.name}: ${error.message}` : String(error || 'unknown');
}
