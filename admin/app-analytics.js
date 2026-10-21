// Renders only authenticated, nonempty GA4 reporting results.
(() => {
  'use strict';
  const panel=document.getElementById('appAnalyticsPanel');
  if(!panel)return;
  const ids={
    appOpens7d:'appOpens7d',newUsers7d:'appNewUsers7d',dau:'appDau',
    wau:'appWau',mau:'appMau',subscriberActive7d:'appSubscriberActive7d',
  };
  const count=v=>Number.isSafeInteger(v)&&v>=0?v.toLocaleString('ko-KR'):'—';
  const percentage=v=>typeof v==='number'&&Number.isFinite(v)&&v>=0&&v<=100?v.toFixed(1)+'%':'—';
  let inflight=false;
  async function refresh(){
    if(inflight)return;
    inflight=true;
    try {
      const res=await fetch('/admin/api/app-analytics',{credentials:'same-origin',cache:'no-store'});
      const data=await res.json();
      if(!res.ok||data?.available!==true||data?.status!=='ready'){
        panel.hidden=true;return;
      }
      for(const [key,id] of Object.entries(ids)) {
        const node=document.getElementById(id);
        if(node)node.textContent=count(data.metrics?.[key]);
      }
      for(const n of [1,7,30]){
        const node=document.getElementById('appRetentionD'+n);
        if(node)node.textContent=percentage(data.retention?.['d'+n]);
      }
      const note=document.getElementById('appRetentionBasis');
      if(note)note.textContent=data.retention?.cohortDate
        ? '유지율: '+data.retention.cohortDate+' 최초 사용자 코호트 · D1/D7/D30 | 구독자 활동은 수집 구간의 유료 상태 기준'
        : '전일 마감 데이터';
      panel.hidden=false;
    } catch {
      panel.hidden=true;
    } finally {
      inflight=false;
    }
  }
  document.getElementById('refreshButton')?.addEventListener('click',refresh);
  refresh();
})();
