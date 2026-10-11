import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {
  LIMIT_CAPS, validateAutomationLimits, validateMobilePolicy, serveMobilePolicy
} from '../worker/mobile-policy.mjs';

const base=JSON.parse(readFileSync(
  new URL('../remote-config/calltag-v1.json',import.meta.url),'utf8'));
assert.equal(base.schemaVersion,1);
assert.equal(base.revision,2);
assert.deepEqual(base.automationLimits,{
  minCooldownHours:0,minLeadDelayMinutes:0,minFollowupDays:0
});
assert.equal(validateMobilePolicy(base),true);
assert.equal(validateAutomationLimits(base.automationLimits),true);
const request=new Request('https://calltag.pagero.kr/api/mobile-config/v1');
const assets=obj=>({fetch:async()=>new Response(JSON.stringify(obj),{status:200})});
const response=await serveMobilePolicy(request,{ASSETS:assets({
  ...base,automationLimits:{minCooldownHours:48,minLeadDelayMinutes:60,minFollowupDays:7}
})});
assert.equal(response.status,200);
assert.deepEqual((await response.json()).automationLimits,{
  minCooldownHours:48,minLeadDelayMinutes:60,minFollowupDays:7
});
// Old policy response remains accepted and defaults to no additional limits.
const older={schemaVersion:1,revision:1,flags:base.flags};
assert.equal(validateMobilePolicy(older),true);
const fallback=await serveMobilePolicy(request,{ASSETS:assets(older)});
assert.deepEqual((await fallback.json()).automationLimits,{
  minCooldownHours:0,minLeadDelayMinutes:0,minFollowupDays:0
});
// Restricting only: reject partial configs, negative values, too-large bounds,
// booleans, strings, unsafe and fractional values.
for(const key of Object.keys(LIMIT_CAPS)){
  for(const bad of [-1,LIMIT_CAPS[key]+1,0.5,"2",true,null,Number.MAX_SAFE_INTEGER+1]){
    const fake={...base,automationLimits:{...base.automationLimits,[key]:bad}};
    assert.equal(validateMobilePolicy(fake),false,`${key} accepted ${bad}`);
    const result=await serveMobilePolicy(request,{ASSETS:assets(fake)});
    assert.equal(result.status,503,`${key} server accepted ${bad}`);
  }
  const missing={...base,automationLimits:{...base.automationLimits}};
  delete missing.automationLimits[key];
  assert.equal(validateMobilePolicy(missing),false);
}
const extra={...base,automationLimits:{...base.automationLimits,unknownPolicy:999}};
const filtered=await serveMobilePolicy(request,{ASSETS:assets(extra)});
const body=await filtered.json();
assert.equal(body.automationLimits.unknownPolicy,undefined);
console.log('CallTag automation limit server validation, legacy default, bounds, sanitized payload: PASS');
