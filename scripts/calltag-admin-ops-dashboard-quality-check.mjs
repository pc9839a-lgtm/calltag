import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { Script } from 'node:vm';

const files = [
  'shell.dat','admin.js','payer-audit.js',
  'play-finance-monthly.js','ops-dashboard.js','ops-dashboard.css',
];
for (const name of files) {
  const source=readFileSync('admin/'+name,'utf8');
  const production=readFileSync('web/admin/'+name,'utf8');
  assert.equal(source,production,'production admin bundle parity: '+name);
}
const html=readFileSync('admin/shell.dat','utf8');
const ops=readFileSync('admin/ops-dashboard.js','utf8');
const admin=readFileSync('admin/admin.js','utf8');
const audit=readFileSync('admin/payer-audit.js','utf8');
const finance=readFileSync('admin/play-finance-monthly.js','utf8');
for(const file of ['admin/ops-dashboard.js','admin/admin.js','admin/payer-audit.js','admin/play-finance-monthly.js']) {
  new Script(readFileSync(file,'utf8'),{filename:file});
}
for(const id of [
  'opsHealthPanel','opsVerifiedSubscribers','opsLedgerAccounts',
  'opsFinanceStatus','opsAttention','opsCheckedAt','opsVerifiedSubscribersNote',
  'opsLedgerAccountsNote','opsFinanceStatusNote','payerAuditRefresh',
  'memberSearchInput','playFinancePanel','membersPanel',
]) assert(html.includes('id="'+id+'"'),'required operational target missing: '+id);
assert(html.includes('/admin/ops-dashboard.js?v='),'health script must be shipped');
assert(html.includes('/admin/ops-dashboard.css?v='),'health stylesheet must be shipped');
for(const event of ['calltag:ops-overview','calltag:ops-payer-audit','calltag:ops-finance']) {
  assert(ops.includes(event),'ops dashboard must subscribe '+event);
}
assert(admin.includes("new CustomEvent('calltag:ops-overview'"),'overview aggregate event missing');
assert(audit.includes("new CustomEvent('calltag:ops-payer-audit'"),'payer audit aggregate event missing');
assert(finance.includes("new CustomEvent('calltag:ops-finance'"),'Play finance availability event missing');
assert(ops.includes('실매출 0원이 아닌'),'unavailable earnings must not become zero sales');
assert(ops.includes('서버에서 인식한 유료 이용 회원'),'subscriber scope must be explicit');
assert(ops.includes('결제 원장 추가확인 필요'),'unverified ledger differences must be visible');
assert(ops.includes('조회 상한에 도달했습니다.'),'truncated audit must not imply completeness');
// The health panel may only navigate or present existing protected endpoint data.
// No privileged writes, credential storage, or raw customer/payment identifiers.
assert(!/\bfetch\s*\(|XMLHttpRequest|localStorage|sessionStorage/.test(ops),'no new admin network or persistent tokens');
assert(!/\b(ownerId|email|purchaseToken|orderId|privateKey)\b/.test(ops),'no direct customer/billing identifiers');
assert(!/PUT|PATCH|DELETE|POST\s*\(/.test(ops),'never mutate production records via overview');
console.log(JSON.stringify({ ok:true, name:'calltag-admin-ops-dashboard', checks:32, productionDataWrites:false }));
