import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

for (const file of ['shell.dat','member-ops.js','member-detail.css']) {
  assert.equal(fs.readFileSync('admin/'+file,'utf8'),fs.readFileSync('web/admin/'+file,'utf8'),
    'production static bundle parity '+file);
}
const html=fs.readFileSync('admin/shell.dat','utf8');
const css=fs.readFileSync('admin/member-detail.css','utf8');
const source=fs.readFileSync('admin/member-ops.js','utf8');
assert(html.includes('/admin/member-detail.css?v=20261020-detail-v2'));
assert(html.includes('/admin/member-ops.js?v=20261021-clean1'));
assert(css.includes('.member-detail-layout .detailrow') && css.includes('display:grid'),'labels and values must be separated');
assert(css.includes('grid-template-columns:repeat(2,minmax(0,1fr))'),'two-column summary');
assert(css.includes('member-entitlement-actions'),'distinct grant/revoke action row');
assert(css.includes('member-history-fold'),'fold historical data');
assert(css.includes(':has(.member-detail-layout)'),'drawer styles must not interfere with partner drawer');
new vm.Script(source,{filename:'admin/member-ops.js'});

class Element {
  constructor(tag='div') {
    this.tag=tag;this.children=[];this.listeners={};this.className='';this.textContent='';
    this.value='';this.title='';this.hidden=true;this.disabled=false;this.style={};this.dataset={};
    this.classList={add:(v)=>{this.className+=' '+v;},toggle:()=>{}};
  }
  append(...children){this.children.push(...children);}
  replaceChildren(...children){this.children=[...children];}
  addEventListener(name,handler){this.listeners[name]=handler;}
  setAttribute(name,value){this[name]=value;}
  fire(name,arg){return this.listeners[name]?.(arg);}
}
const els={};
for(const id of ['memberRows','emptyMembers','memberSearchInput','memberSearchButton',
 'memberEvidenceFilter','memberResultCount','memberPrev','memberNext','memberPageInfo',
 'refreshButton','memberDetail','detailBackdrop','detailTitle','detailOwnerId','detailBody']) {
  els[id]=new Element();
}
const sub={productCode:'call_monthly',channel:'google_play',status:'active',
  verificationState:'verified',autoRenewing:true,expiresAt:'2099-01-01T00:00:00Z'};
const member={ownerId:'user_test_account',email:'masked@example.com',phone:'-',
  createdAt:'2026-10-15T00:00:00Z',updatedAt:'2026-10-20T00:00:00Z',subscriptions:[sub],
  trialEndsAt:'2099-01-01T00:00:00Z'};
const paths=[];
const fetch=async (url,options={})=>{
  paths.push({url,method:options.method||'GET'});
  let data;
  if(url.startsWith('/admin/api/members?')) data={ok:true,members:[member],total:1,page:1,totalPages:1};
  else if(url.startsWith('/admin/api/member?')) data={ok:true,member,subscriptions:[sub],
    trial:{startedAt:'2026-10-15T00:00:00Z',endsAt:'2099-01-01T00:00:00Z',referralBonusDays:0},
    referral:{referredCount:0,wasReferred:false},
    partner:{commissionCount:0,pendingAmountKrw:0,confirmedAmountKrw:0},
    legacyEntitlement:{planCode:'beta_free',source:'beta',status:'active'}};
  else if(url.startsWith('/admin/api/member-payments?')) data={ok:true,payments:[],snapshots:[]};
  else if(url.startsWith('/admin/api/entitlement?')) data={ok:true,entitlement:null};
  else throw new Error('unexpected endpoint '+url);
  return {ok:true,json:async()=>data};
};
const context=vm.createContext({
  document:{getElementById:(id)=>els[id]||null,createElement:(name)=>new Element(name),body:{classList:{add:()=>{}}}},
  window:{confirm:()=>true,alert:()=>{}},
  fetch,setTimeout:()=>0,Date,Intl,Promise,Math,Number,String,Array,Set,encodeURIComponent,
});
new vm.Script(source).runInContext(context);
els.memberSearchButton.fire('click');
for(let i=0;i<8;i++)await new Promise(resolve=>setImmediate(resolve));
assert.equal(els.memberRows.children.length,1);
const action=els.memberRows.children[0].children[7].children[0];
assert.equal(action.textContent,'상세');
await action.fire('click');
for(let i=0;i<10;i++)await new Promise(resolve=>setImmediate(resolve));
const root=els.detailBody.children[0];
assert.equal(root.className,'member-detail-layout');
assert.equal(root.children.length,5,'summary, grants, subscriptions, payments, collapsed history');
assert.equal(root.children[0].className,'member-summary-card');
assert.equal(root.children[1].className,'section admin-entitlement');
assert.equal(root.children[2].className,'section');
assert.equal(root.children[3].className,'section payment-history');
const fold=root.children[4];
assert.equal(fold.tag,'details');
assert.equal(fold.className,'member-history-fold');
assert.equal(fold.open,undefined,'history is not expanded by default');
const deep=(parent,predicate)=>{
  if(predicate(parent))return parent;
  for(const child of parent.children||[]) {
    const result=deep(child,predicate);if(result)return result;
  }
  return null;
};
assert(deep(root,x=>x.textContent==='masked@example.com'),'member email visible and spaced');
assert(deep(root,x=>x.className==='member-summary-badge' || x.className?.startsWith('member-summary-badge ')),
  'clear subscription status chip visible');
assert(deep(root,x=>x.textContent==='이용권 범위'),'label is visible');
assert(deep(root,x=>x.textContent==='이용권 지급'),'original grant action retained');
assert(deep(root,x=>x.textContent==='구독'),'subscriptions section retained');
assert(deep(root,x=>x.textContent==='결제 이력'),'payment section retained');
assert(deep(fold,x=>x.textContent==='이전 이용권 기록'),'historical data preserved');
assert(paths.every(x=>x.method==='GET'),'opening detail must not modify production data');
assert(els.memberDetail.hidden===false && els.detailBackdrop.hidden===false,'member drawer still opens');
console.log('PASS member detail responsive redesign: source parity, hierarchy, status, actions, historical fold and readonly open');
