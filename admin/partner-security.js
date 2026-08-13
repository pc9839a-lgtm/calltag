(()=>{
  'use strict';

  const nativeFetch=window.fetch.bind(window);
  let activeOwnerId='';
  let mountTimer=0;

  window.fetch=async function(input,init){
    const urlText=typeof input==='string'?input:String(input?.url||'');
    let partnerOwnerId='';
    try{
      const url=new URL(urlText,location.origin);
      if(url.origin===location.origin&&url.pathname==='/admin/api/partner'){
        partnerOwnerId=validOwner(url.searchParams.get('ownerId'));
        if(partnerOwnerId)activeOwnerId=partnerOwnerId;
      }
    }catch{}
    const response=await nativeFetch(input,init);
    if(partnerOwnerId&&response.ok)scheduleMount(partnerOwnerId);
    return response;
  };

  function scheduleMount(ownerId){
    clearTimeout(mountTimer);
    mountTimer=setTimeout(()=>mount(ownerId),80);
  }

  async function mount(ownerId){
    if(ownerId!==activeOwnerId)return;
    const drawer=document.getElementById('memberDetail');
    const body=document.getElementById('detailBody');
    const title=document.getElementById('detailTitle');
    if(!body||!drawer||drawer.hidden||String(title?.textContent||'').indexOf('파트너')<0){
      scheduleMount(ownerId);
      return;
    }

    document.getElementById('partnerTotpSecurity')?.remove();
    const box=document.createElement('div');
    box.id='partnerTotpSecurity';
    box.className='financebox';

    const heading=document.createElement('strong');
    heading.textContent='정산 2차 인증';
    const note=document.createElement('div');
    note.className='rate-note';
    note.textContent='인증 앱 상태 확인 중…';
    const button=document.createElement('button');
    button.type='button';
    button.className='paybtn';
    button.textContent='2차 인증 초기화';
    button.disabled=true;
    box.append(heading,note,button);
    body.insertBefore(box,body.children[2]||null);

    const result=await request(`/admin/api/partner-totp-status?ownerId=${encodeURIComponent(ownerId)}`,{method:'GET'});
    if(ownerId!==activeOwnerId||!box.isConnected)return;
    if(!result.ok){
      note.textContent=result.error||'2차 인증 상태를 불러오지 못했습니다.';
      return;
    }

    renderStatus(result.data?.security||{},note,button);
    button.addEventListener('click',async()=>{
      if(button.disabled)return;
      const confirmed=window.confirm('이 파트너의 정산 2차 인증을 초기화할까요?\n\n기존 인증 앱은 즉시 무효화되고 다음 정산 로그인에서 새 인증 앱을 다시 등록해야 합니다.');
      if(!confirmed)return;
      button.disabled=true;
      const previous=note.textContent;
      note.textContent='2차 인증 초기화 중…';
      const reset=await request('/admin/api/partner-totp-reset',{
        method:'POST',
        headers:{'content-type':'application/json'},
        body:JSON.stringify({ownerId}),
      });
      if(!reset.ok){
        note.textContent=reset.error||previous||'2차 인증 초기화에 실패했습니다.';
        button.disabled=false;
        setGlobalStatus(reset.error||'2차 인증 초기화에 실패했습니다.',false);
        return;
      }
      note.textContent='미등록 · 초기화 완료';
      button.disabled=true;
      setGlobalStatus('파트너 정산 2차 인증을 초기화했습니다.',true);
    });
  }

  function renderStatus(security,note,button){
    if(security?.enrolled!==true){
      note.textContent='미등록 · 다음 정산 로그인에서 인증 앱 등록 필요';
      button.disabled=true;
      return;
    }
    const parts=['등록됨'];
    if(security.enabledAt)parts.push(`등록 ${dateTime(security.enabledAt)}`);
    if(security.lockedUntil)parts.push(`입력 잠금 ${dateTime(security.lockedUntil)}까지`);
    note.textContent=parts.join(' · ');
    button.disabled=false;
  }

  async function request(url,init={}){
    try{
      const response=await nativeFetch(url,{...init,cache:'no-store',credentials:'same-origin',headers:{accept:'application/json',...(init.headers||{})}});
      let data={};try{data=await response.json()}catch{}
      return{ok:response.ok&&data?.ok!==false,status:response.status,error:String(data?.error||''),data};
    }catch{return{ok:false,status:0,error:'네트워크 연결을 확인해주세요.',data:{}}}
  }

  function setGlobalStatus(text,ok){
    const bar=document.getElementById('statusBar');
    if(!bar)return;
    bar.textContent=text;
    bar.classList.toggle('ok',!!ok);
  }

  function validOwner(value){const owner=String(value||'').trim();return/^[A-Za-z0-9._:-]{3,120}$/.test(owner)?owner:''}
  function dateTime(value){const date=new Date(String(value||''));return Number.isFinite(date.getTime())?new Intl.DateTimeFormat('ko-KR',{year:'2-digit',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}).format(date):'-'}
})();
