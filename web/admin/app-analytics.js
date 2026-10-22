// Renders only authenticated, nonempty GA4 reporting results.
(() => {
  'use strict';
  const panel=document.getElementById('appAnalyticsPanel');
  if(!panel)return;
  const ids={
    appOpens7d:'appOpens7d',newUsers7d:'appNewUsers7d',dau:'appDau',
    wau:'appWau',mau:'appMau',subscriberActive7d:'appSubscriberActive7d',
    trialActive7d:'appTrialActive7d',freeActive7d:'appFreeActive7d',
    grantedActive7d:'appGrantedActive7d',
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
      const timestamp=document.getElementById('appAnalyticsAsOf');
      if(timestamp)timestamp.textContent=data.asOf
        ? 'Firebase Analytics · '+data.asOf+' 기준' : 'Firebase Analytics';
      const note=document.getElementById('appRetentionBasis');
      if(note) {
        const days=[1,7,30].map(n=>{
          const date=data.retention?.cohortDates?.['d'+n];
          return date?'D'+n+' '+date:'';
        }).filter(Boolean);
        note.textContent=days.length
          ? '최초 사용 코호트: '+days.join(' · ')+' | 이용 상태는 실행 당시 구분이며 중복 상태 사용자는 합산하지 않음'
          : '전일 마감 데이터';
      }
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
