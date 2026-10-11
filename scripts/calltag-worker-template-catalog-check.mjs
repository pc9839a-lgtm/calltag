import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {validateTemplateCatalog,serveMobileTemplates} from '../worker/mobile-templates.mjs';
const valid=JSON.parse(readFileSync(new URL('../remote-config/calltag-templates-v1.json',import.meta.url),'utf8'));
assert.equal(validateTemplateCatalog(valid),true);
const req=(method='GET',path='/api/mobile-templates/v1')=>
  new Request('https://calltag.pagero.kr'+path,{method});
const assets=body=>({fetch:async r=>{
  assert.equal(new URL(r.url).pathname,'/remote-config/calltag-templates-v1.json');
  return new Response(body,{status:200});
}});
const ok=await serveMobileTemplates(req(),{ASSETS:assets(JSON.stringify(valid))});
assert.equal(ok.status,200);
assert.match(ok.headers.get('cache-control'),/no-store/);
assert.equal(ok.headers.get('x-content-type-options'),'nosniff');
assert.deepEqual(await ok.json(),valid);
assert.equal((await serveMobileTemplates(req('HEAD'),{ASSETS:assets(JSON.stringify(valid))})).status,200);
const method=await serveMobileTemplates(req('POST'),{ASSETS:assets('{}')});
assert.equal(method.status,405);
assert.equal(method.headers.get('allow'),'GET, HEAD');
assert.equal(await serveMobileTemplates(req('GET','/bad'),{}),null);
const mutations=[
  {...valid,revision:0},
  {...valid,schemaVersion:2},
  {...valid,templates:[]},
  {...valid,templates:[...valid.templates,...valid.templates,...valid.templates,...valid.templates]},
  {...valid,templates:[{...valid.templates[0],body:'Click {unknown_variable}'}]},
  {...valid,templates:[{...valid.templates[0],body:'Click {고객명'}]},
  {...valid,templates:[{...valid.templates[0],body:'x'.repeat(501)}]},
  {...valid,templates:[{...valid.templates[0],id:'../invalid'}]},
  {...valid,templates:[valid.templates[0],valid.templates[0]]}
];
for (const item of mutations) {
  assert.equal(validateTemplateCatalog(item),false);
  const response=await serveMobileTemplates(req(),{ASSETS:assets(JSON.stringify(item))});
  assert.equal(response.status,503);
}
const offline=await serveMobileTemplates(req(),{ASSETS:{fetch:async()=>new Response('',{status:503})}});
assert.equal(offline.status,503);
const tooLarge=await serveMobileTemplates(req(),{ASSETS:assets('z'.repeat(8193))});
assert.equal(tooLarge.status,503);
console.log('CallTag remote template catalog request/schema/placeholder/size tests: PASS');
