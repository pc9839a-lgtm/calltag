(()=>{
  const desired='페이지로에서 문의를 받으면<br>관리는 콜태그 앱에서!';
  const legacy='페이지로에서 문의를 받고, 콜태그가 바로 알림·등록·후속관리합니다.';

  const apply=()=>{
    const target=document.querySelector('#ct-pagero-intro .ct-v8-head>strong');
    if(target&&target.innerHTML!==desired)target.innerHTML=desired;

    document.querySelectorAll('strong,p,div,span').forEach(element=>{
      if(element.children.length)return;
      const text=(element.textContent||'').replace(/\s+/g,' ').trim();
      if(text===legacy)element.innerHTML=desired;
    });
  };

  let queued=false;
  const queue=()=>{
    if(queued)return;
    queued=true;
    requestAnimationFrame(()=>{
      queued=false;
      apply();
    });
  };

  const boot=()=>{
    apply();
    const observer=new MutationObserver(queue);
    observer.observe(document.documentElement,{childList:true,subtree:true,characterData:true});
    [50,200,500,1000,2000,4000,8000].forEach(delay=>setTimeout(apply,delay));
    setTimeout(()=>observer.disconnect(),15000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();