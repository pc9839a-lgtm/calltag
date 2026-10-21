import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

for (const name of ['shell.dat','admin.js','payer-audit.js','play-finance-monthly.js','ops-dashboard.js','ops-dashboard.css']) {
  assert.equal(readFileSync('admin/' + name,'utf8'),readFileSync('web/admin/' + name,'utf8'),
    'live admin parity ' + name);
}
const html=readFileSync('admin/shell.dat','utf8');
const ops=readFileSync('admin/ops-dashboard.js','utf8');
const css=readFileSync('admin/ops-dashboard.css','utf8');
const admin=readFileSync('admin/admin.js','utf8');
const audit=readFileSync('admin/payer-audit.js','utf8');
const finance=readFileSync('admin/play-finance-monthly.js','utf8');
new vm.Script(ops,{filename:'admin/ops-dashboard.js'});
for (const id of ['opsHealthPanel','opsCheckedAt','opsTotalMembers','opsNewMembers','opsTrialMembers',
  'opsVerifiedSubscribers','opsNonPaidMembers','opsPaidShare','memberSearchInput','membersPanel','payerAuditDetails','financeDetails',
  'playFinancePanel','payerAuditRefresh']) {
  assert(html.includes('id="' + id + '"'),'missing required dashboard or detailed control: ' + id);
}
for (const removed of ['opsFinanceStatus','opsFinanceStatusNote','opsAttention','opsLedgerAccounts',
  'ops-has-alerts','확인할 항목','실정산 연결','권한 확인 필요']) {
  const dashboard = html.slice(html.indexOf('id="opsHealthPanel"'),html.indexOf('id="membersPanel"'));
  assert(!dashboard.includes(removed),'legacy finance/alert UI in dashboard: ' + removed);
  assert(!ops.includes(removed),'removed finance/alert logic in dashboard: ' + removed);
}
assert.equal((html.slice(html.indexOf('<div class="ops-health-grid">'),
  html.indexOf('id="appAnalyticsPanel"')).match(/class="ops-indicator"/g)||[]).length,6,
  'six member/subscriber KPI cards required');
assert(html.includes('/admin/ops-dashboard.js?v=20261021-subscribers1'));
assert(html.includes('/admin/ops-dashboard.css?v=20261021-subscribers1'));
assert(css.includes('.ops-member-dashboard .ops-health-grid'),'dashboard responsive grid styles required');
for (const key of ['totalMembers','newMembers7d','trialMembers','activePaid']) {
  assert(ops.includes(key),'missing member KPI: ' + key);
}
assert(admin.includes("new CustomEvent('calltag:ops-overview'"),'existing overview event missing');
assert(audit.includes("new CustomEvent('calltag:ops-payer-audit'"),'billing audit must remain available');
assert(finance.includes("new CustomEvent('calltag:ops-finance'"),'detailed finance functionality must remain');
assert(!/\bfetch\s*\(|XMLHttpRequest|localStorage|sessionStorage/.test(ops),'dashboard must not fetch or store tokens');
assert(!/\b(ownerId|email|purchaseToken|orderId|privateKey)\b/.test(ops),'dashboard must not expose billing IDs');
assert(!/PUT|PATCH|DELETE|POST\s*\(/.test(ops),'dashboard must not write production data');
for (const name of ['payerAuditDetails','extraStatsDetails','financeDetails']) {
  assert(html.includes('<details id="' + name + '"'),'existing advanced menu removed ' + name);
}
const nodes={};
for(const id of ['opsHealthPanel','opsCheckedAt','opsTotalMembers','opsNewMembers','opsTrialMembers','opsVerifiedSubscribers','opsNonPaidMembers','opsPaidShare']){
  nodes[id]={textContent:''};
}
const handlers={};
const context=vm.createContext({
  document:{
    getElementById:id=>nodes[id]||null,
    addEventListener:(type,fn)=>{handlers[type]=fn;},
  }, Date,Number,String,Intl,
});
new vm.Script(ops).runInContext(context);
assert(typeof handlers['calltag:ops-overview']==='function','overview listener missing');
const send = detail => handlers['calltag:ops-overview']({detail});
send({ok:true,metrics:{totalMembers:38,newMembers7d:5,trialMembers:29,activePaid:4}});
assert.equal(nodes.opsTotalMembers.textContent,'38명');
assert.equal(nodes.opsNewMembers.textContent,'5명');
assert.equal(nodes.opsTrialMembers.textContent,'29명');
assert.equal(nodes.opsVerifiedSubscribers.textContent,'4명');
assert.equal(nodes.opsNonPaidMembers.textContent,'34명');
assert.equal(nodes.opsPaidShare.textContent,'10.5%');
send({ok:true,metrics:{totalMembers:0,newMembers7d:0,trialMembers:0,activePaid:0}});
assert.equal(nodes.opsPaidShare.textContent,'—','zero denominator has no paid share');
send({ok:true,metrics:{totalMembers:4,newMembers7d:0,trialMembers:0,activePaid:5}});
assert.equal(nodes.opsPaidShare.textContent,'—','inconsistent source totals must not show a percentage');
send({ok:false});
for(const id of ['opsTotalMembers','opsNewMembers','opsTrialMembers','opsVerifiedSubscribers']){
  assert.equal(nodes[id].textContent,'—','failed response must not become zero: '+id);
}
console.log('PASS: six member/subscriber KPIs, safe subscription share, detailed billing preserved');
