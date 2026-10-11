// Public, static catalog. Template suggestions are NEVER activated for SMS
// without a user explicitly importing them into their private local library.
const PURPOSES = new Set(['incoming', 'outgoing', 'missed', 'follow_up', 'general']);
const VARIABLES = new Set([
  '{고객명}', '{전화번호}', '{통화일자}', '{통화시간}',
  '{내이름}', '{상호명}', '{다음일정}'
]);
const IDENTIFIER = /^[a-z0-9_-]{3,40}$/;
const BRACES = /\{[^{}\r\n]+\}/g;

export function validateTemplateCatalog(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)
      || value.schemaVersion !== 1 || !Number.isSafeInteger(value.revision)
      || value.revision < 1 || !Array.isArray(value.templates)
      || value.templates.length < 1 || value.templates.length > 12) return false;
  const ids = new Set();
  for (const item of value.templates) {
    if (!item || typeof item !== 'object' || Array.isArray(item)) return false;
    if (typeof item.id !== 'string' || !IDENTIFIER.test(item.id)
        || ids.has(item.id) || !PURPOSES.has(item.purpose)
        || typeof item.name !== 'string' || !item.name.trim()
        || item.name.length > 60 || typeof item.body !== 'string'
        || !item.body.trim() || item.body.length > 500) return false;
    ids.add(item.id);
    // Reject unsupported placeholders or unbalanced brace syntax.
    const withoutVars = item.body.replace(BRACES, value =>
      VARIABLES.has(value) ? '' : '__UNSUPPORTED_VARIABLE__');
    if (withoutVars.includes('__UNSUPPORTED_VARIABLE__')
        || withoutVars.includes('{') || withoutVars.includes('}')) return false;
  }
  return true;
}

export async function serveMobileTemplates(request, env) {
  if (new URL(request.url).pathname !== '/api/mobile-templates/v1') return null;
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    return new Response('Method Not Allowed', {
      status: 405,
      headers: {'allow':'GET, HEAD', 'cache-control':'no-store'}
    });
  }
  const headers = {
    'content-type':'application/json; charset=utf-8',
    'cache-control':'no-store, max-age=0',
    'x-content-type-options':'nosniff'
  };
  try {
    if (!env.ASSETS) throw new Error('missing_assets');
    const asset = await env.ASSETS.fetch(new Request(
      new URL('/remote-config/calltag-templates-v1.json', request.url).toString(),
      {headers:{accept:'application/json'}}
    ));
    if (!asset.ok) throw new Error('missing_templates');
    const body = await asset.text();
    if (new TextEncoder().encode(body).byteLength > 8192) throw new Error('oversize');
    const input = JSON.parse(body);
    if (!validateTemplateCatalog(input)) throw new Error('invalid_catalog');
    const cleaned = {
      schemaVersion:1,
      revision:input.revision,
      templates:input.templates.map(x => ({
        id:x.id, name:x.name.trim(), body:x.body.trim(), purpose:x.purpose
      }))
    };
    return new Response(request.method === 'HEAD' ? null : JSON.stringify(cleaned), {
      status:200,headers
    });
  } catch (error) {
    console.error('mobile-template-catalog-unavailable', String(error));
    return new Response(JSON.stringify({ok:false,error:'template_catalog_unavailable'}),{
      status:503,headers
    });
  }
}
