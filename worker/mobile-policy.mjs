// Public, disable-only operational switches for previously shipped Android code.
// No user identity, pricing, billing entitlements, code execution or secrets.
export const FLAG_NAMES = Object.freeze([
  'smsSending', 'automaticSms', 'mmsCompose', 'leadImport', 'cloudSync'
]);

export function validateMobilePolicy(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
    && value.schemaVersion === 1
    && Number.isSafeInteger(value.revision) && value.revision >= 1
    && value.flags && typeof value.flags === 'object' && !Array.isArray(value.flags)
    && FLAG_NAMES.every(name => typeof value.flags[name] === 'boolean');
}

export async function serveMobilePolicy(request, env) {
  const url = new URL(request.url);
  if (url.pathname !== '/api/mobile-config/v1') return null;
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    return new Response('Method Not Allowed', {
      status: 405, headers: { allow: 'GET, HEAD', 'cache-control': 'no-store' }
    });
  }

  const headers = {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store, max-age=0',
    'x-content-type-options': 'nosniff'
  };
  try {
    if (!env.ASSETS) throw new Error('asset_binding_missing');
    const assetUrl = new URL('/remote-config/calltag-v1.json', request.url);
    const raw = await env.ASSETS.fetch(new Request(assetUrl.toString(), {
      method: 'GET', headers: { accept: 'application/json' }
    }));
    if (!raw.ok) throw new Error('policy_asset_unavailable');
    const body = await raw.text();
    if (new TextEncoder().encode(body).byteLength > 8192) {
      throw new Error('policy_asset_too_large');
    }
    const config = JSON.parse(body);
    if (!validateMobilePolicy(config)) throw new Error('policy_schema_invalid');
    const safeFlags = Object.fromEntries(FLAG_NAMES.map(key => [key, config.flags[key]]));
    const response = JSON.stringify({
      schemaVersion: 1, revision: config.revision, flags: safeFlags
    });
    return new Response(request.method === 'HEAD' ? null : response, {
      status: 200, headers
    });
  } catch (error) {
    console.error('mobile-policy-unavailable', String(error));
    return new Response(JSON.stringify({ ok: false, error: 'remote_policy_unavailable' }), {
      status: 503, headers
    });
  }
}
