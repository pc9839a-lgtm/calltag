import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {FLAG_NAMES, validateMobilePolicy, serveMobilePolicy} from '../worker/mobile-policy.mjs';

const value = JSON.parse(readFileSync(new URL('../remote-config/calltag-v1.json', import.meta.url),'utf8'));
assert.equal(validateMobilePolicy(value), true);
assert.deepEqual(FLAG_NAMES.sort(), Object.keys(value.flags).sort());

const request = (method='GET', path='/api/mobile-config/v1') =>
  new Request('https://calltag.pagero.kr'+path,{method});
const assets = body => ({
  fetch: async requested => {
    assert.equal(new URL(requested.url).pathname,'/remote-config/calltag-v1.json');
    return new Response(body,{status:200});
  }
});
const ok = await serveMobilePolicy(request(),{ASSETS:assets(JSON.stringify(value))});
assert.equal(ok.status,200);
assert.match(ok.headers.get('cache-control'),/no-store/);
assert.equal(ok.headers.get('x-content-type-options'),'nosniff');
const json = await ok.json();
assert.deepEqual(json, value);

const method = await serveMobilePolicy(request('POST'),{ASSETS:assets('{}')});
assert.equal(method.status,405);
assert.equal(method.headers.get('allow'),'GET, HEAD');
const head = await serveMobilePolicy(request('HEAD'),{ASSETS:assets(JSON.stringify(value))});
assert.equal(head.status,200);
assert.equal(await head.text(),'');
assert.equal(await serveMobilePolicy(request('GET','/no-config'),{}),null);

for (const broken of [
  '{"schemaVersion":2,"revision":1,"flags":{}}',
  JSON.stringify({...value,revision:0}),
  JSON.stringify({...value,flags:{...value.flags,smsSending:"true"}}),
  '{"broken_json":',
  'x'.repeat(8193)
]) {
  const rejected = await serveMobilePolicy(request(),{ASSETS:assets(broken)});
  assert.equal(rejected.status,503);
}
const unavailable = await serveMobilePolicy(request(),{ASSETS:{
  fetch: async()=>new Response('Not found',{status:404})
}});
assert.equal(unavailable.status,503);
const withUnexpectedFlag = await serveMobilePolicy(request(),{ASSETS:assets(JSON.stringify({
  ...value,flags:{...value.flags,unrecognizedDangerousSwitch:true}
}))});
assert.equal((await withUnexpectedFlag.json()).flags.unrecognizedDangerousSwitch,undefined);
console.log('CallTag Cloudflare server policy handler, strict JSON and no-store: PASS');
