import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const html = readFileSync('admin/shell.dat','utf8');
const js = readFileSync('admin/member-ops.js','utf8');
const css = readFileSync('admin/ops-dashboard.css','utf8');
for (const name of ['shell.dat','member-ops.js','ops-dashboard.css']) {
  assert.equal(readFileSync('admin/'+name,'utf8'),readFileSync('web/admin/'+name,'utf8'),'live asset parity: '+name);
}
for (const key of ['all','paid','trial','review','admin']) {
  assert(html.includes('data-member-filter="'+key+'"'),'required quick status: '+key);
}
assert.equal((html.match(/data-member-filter="/g)||[]).length,5);
const tableStart=html.indexOf('<table class="member-compact-table">');
const tableEnd=html.indexOf('</table>',tableStart);
const columns=(html.slice(tableStart,tableEnd).match(/<th>/g)||[]);
assert.equal(columns.length,8,'fit eight columns into one desktop viewport');
for(const hidden of ['<th>전화번호</th>','<th>지급 기록</th>']) {
  assert(!html.slice(tableStart,tableEnd).includes(hidden),'move details to drawer: '+hidden);
}
assert(css.includes('table-layout:fixed'),'compact table does not overflow desktop');
assert(css.includes('overflow-x:hidden'),'desktop table has no horizontal scroll');
assert(js.includes('MAX_SERVER_PAGES'),'filter scans all backend pages');
for (const id of ['memberAdvancedPanel','memberFilterToggle','memberDateRange','memberDateFrom','memberDateTo',
  'memberPlanFilter','memberUsageFilter','memberVerificationFilter','memberBillingFilter','memberChannelFilter',
  'memberHistoryFilter','memberSort','memberFilterApply','memberFilterReset','memberFilterChips']) {
  assert(html.includes('id="'+id+'"'),'advanced filter UI missing: '+id);
}
assert(js.includes('advancedFilterMatch'),'filters must affect rows, not just UI');
assert(js.includes('state.fullCount'),'result counts must include total member population');
assert(js.includes('state.localIncomplete'),'do not silently label partial scanning as complete');
assert(!/innerHTML\s*=/.test(js),'member rows must use safe DOM APIs');

class Node {
  constructor(tag='div') {
    this.tag=tag;
    this.children=[];
    this.listeners={};
    this.dataset={};
    this.className='';
    this.title='';
    this.textContent='';
    this.hidden=false;
    this.disabled=false;
    this.value='';
    this.style={};
    this.attrs={};
    this.classList={
      add:(x)=>{this.className+=' '+x;},
      toggle:(x,on)=>{if(on)this.classList.add(x);},
    };
  }
  addEventListener(type,fn){this.listeners[type]=fn;}
  setAttribute(key,value){this.attrs[key]=value;}
  replaceChildren(...nodes){this.children=[...nodes];}
  append(...nodes){this.children.push(...nodes);}
  closest(){return this;}
  contains(node){return this.items?.includes(node)||node===this;}
  querySelectorAll(){return this.items||[];}
  fire(type,event={}){const fn=this.listeners[type];if(fn)fn(event);}
}
const elements={};
for(const id of [
  'memberRows','emptyMembers','memberSearchInput','memberSearchButton',
  'memberEvidenceFilter','memberResultCount','memberPrev','memberNext',
  'memberPageInfo','refreshButton','memberQuickFilters',
  'memberFilterToggle','memberAdvancedPanel','memberFilterApply','memberFilterReset',
  'memberFilterChips','memberFilterError','memberDateRange','memberDateFrom','memberDateTo',
  'memberPlanFilter','memberUsageFilter','memberVerificationFilter','memberBillingFilter',
  'memberChannelFilter','memberHistoryFilter','memberSort'
]) elements[id]=new Node(id);
const group=elements.memberQuickFilters;
group.items=['all','paid','trial','review','admin'].map(x=>{
  const b=new Node('button');b.dataset.memberFilter=x;return b;
});
const paid={ownerId:'paid_on_page_two',email:'paid@example.com',phone:'123',
  createdAt:'2026-10-01',trialEndsAt:'',purchaseHistory:{recorded:true,evidence:'confirmed_charge'},subscriptions:[{
    productCode:'call_monthly',status:'active',verificationState:'verified',
    expiresAt:'2099-01-01T00:00:00Z',autoRenewing:true,channel:'google_play'
  }]};
const waiting={ownerId:'needs_proof',email:'review@example.com',createdAt:'2026-10-05',subscriptions:[{
  productCode:'message_monthly',status:'pending',verificationState:'pending',
  expiresAt:'2099-01-01T00:00:00Z'
}]};
const gifted={ownerId:'admin_gift',email:'admin@example.com',createdAt:'2026-10-08',adminEntitlement:{
  active:true,scope:'call'
}};
const firstPage=Array.from({length:40},(_,i)=>({ownerId:'trial_'+i,email:'trial'+i+'@example.com',
  createdAt:'2026-10-01',trialEndsAt:'2099-01-01T00:00:00Z',subscriptions:[]}));
const secondPage=[paid,waiting,gifted];
const pending=[];
const urls=[];
const doc={
  getElementById:(id)=>elements[id]||null,
  createElement:(tag)=>new Node(tag),
};
const context=vm.createContext({
  document:doc,
  window:{},
  Date,Intl,URLSearchParams,encodeURIComponent,Number,String,Math,Array,Set,Promise,
  setTimeout:fn=>{pending.push(fn);},
  fetch:async url=>{
    urls.push(url);
    const page=new URL(url,'https://example.com').searchParams.get('page');
    const members=page==='2'?secondPage:firstPage;
    return {ok:true,json:async()=>({
      ok:true,page:Number(page),pageSize:40,total:43,totalPages:2,members
    })};
  },
});
new vm.Script(js,{filename:'member-ops.js'}).runInContext(context);
const settle=async()=>{for(let i=0;i<8;i++) await new Promise(resolve=>setImmediate(resolve));};
const load=()=>{for(const fn of pending.splice(0))fn();};
load();
await settle();
assert.equal(elements.memberRows.children.length,40,'default is the server first page');
assert.equal(elements.memberRows.children[0].children.length,8,'row has eight columns');
assert.equal(elements.memberResultCount.textContent,'전체 43명','server total shown not current page count');
const select=async(mode)=>{
  const target=group.items.find(x=>x.dataset.memberFilter===mode);
  group.fire('click',{target});
  await settle();
  return elements.memberRows.children;
};
assert.equal((await select('paid')).length,1,'verified paid owner on second server page is included');
assert.equal(elements.memberResultCount.textContent,'필터 결과 1명 / 전체 43명');
assert.equal(elements.memberRows.children[0].children[0].textContent,'paid_on_page_two'.slice(0,6)+'…'+'paid_on_page_two'.slice(-5));
assert.equal((await select('review')).length,1,'unverified pending member detected');
assert.equal((await select('admin')).length,1,'free administrative grant is not treated as paid');
assert.equal((await select('trial')).length,20,'trial filter paginates 40 matching users');
assert.equal(elements.memberResultCount.textContent,'필터 결과 40명 / 전체 43명','matching total across server pages');
assert.equal(elements.memberPageInfo.textContent,'1 / 2');
elements.memberNext.fire('click');
assert.equal(elements.memberRows.children.length,20);
assert.equal(elements.memberPageInfo.textContent,'2 / 2');
assert(urls.some(url=>url.includes('page=2')),'quick filters fetched second server page');
await select('all');
assert.equal(elements.memberRows.children.length,40,'all resets to server pagination');
assert.equal(elements.memberResultCount.textContent,'전체 43명');

// Test combinable advanced criteria using records on the second backend page.
const apply=async(values)=>{
  for(const [id,value] of Object.entries(values))elements[id].value=value;
  elements.memberFilterApply.fire('click');
  await settle();
  return elements.memberRows.children;
};
const reset=async()=>{
  elements.memberFilterReset.fire('click');
  await settle();
};
assert.equal((await apply({memberPlanFilter:'call_monthly'})).length,1,'plan filter searches page two');
assert.equal(elements.memberRows.children[0].children[0].textContent,'paid_on_page_two'.slice(0,6)+'…'+'paid_on_page_two'.slice(-5));
assert.equal(elements.memberResultCount.textContent,'필터 결과 1명 / 전체 43명');
assert(elements.memberFilterChips.children.length===1,'active filter chip displayed');
await reset();
assert.equal((await apply({memberVerificationFilter:'review',memberUsageFilter:'review'})).length,1,'status filters intersect');
assert.equal(elements.memberRows.children[0].children[0].textContent,'needs_proof');
await reset();
assert.equal((await apply({memberDateRange:'custom',memberDateFrom:'2026-10-05',memberDateTo:'2026-10-08'})).length,2,'inclusive custom date range');
await reset();
assert.equal((await apply({memberHistoryFilter:'yes',memberChannelFilter:'google_play'})).length,1,'purchase-history and channel criteria intersect');
await reset();
assert.equal((await apply({memberSort:'paid'})).length,20,'sorting keeps local pagination size');
assert.equal(elements.memberRows.children[0].children[0].textContent,'paid_on_page_two'.slice(0,6)+'…'+'paid_on_page_two'.slice(-5),'paid-first sorts across pages');
await reset();
assert.equal((await apply({memberDateRange:'custom',memberDateFrom:'2026-10-08',memberDateTo:'2026-10-05'})).length,20,'invalid dates do not change filter');
assert.equal(elements.memberFilterError.hidden,false,'invalid range is explained');
await reset();
assert.equal(elements.memberResultCount.textContent,'전체 43명','reset restores full member list');
console.log('PASS: quick filters, eight advanced criteria, date range, history, sorting, chips and reset');
