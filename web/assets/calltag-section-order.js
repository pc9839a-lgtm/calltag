(()=>{
  if(document.documentElement.dataset.ctSectionOrder)return;
  document.documentElement.dataset.ctSectionOrder='1';

  const apply=()=>{
    const intro=document.querySelector('#ct-pagero-intro');
    const connect=intro?.querySelector('.ct-pagero-connect');
    const nocode=intro?.querySelector('.ct-v8-nocode');
    const calltag=document.querySelector('#app');
    if(!intro||!connect||!nocode||!calltag)return false;

    if(calltag.nextElementSibling!==nocode)nocode.before(calltag);
    return true;
  };

  const boot=()=>{
    if(apply())return;
    const observer=new MutationObserver(()=>{
      if(apply())observer.disconnect();
    });
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),12000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
