(()=>{
  if(document.documentElement.dataset.ctPageroIntroWrapper)return;
  document.documentElement.dataset.ctPageroIntroWrapper='1';

  const desired='페이지로에서 문의를 받으면<br>관리는 <em>콜태그 앱에서!</em>';

  const style=document.createElement('style');
  style.dataset.ctPageroCopyGuard='1';
  style.textContent='#ct-pagero-intro .ct-v8-head>strong{visibility:hidden!important}#ct-pagero-intro .ct-v8-head>strong[data-ct-pagero-copy="fixed"]{visibility:visible!important}';
  document.head.append(style);

  const apply=()=>{
    const target=document.querySelector('#ct-pagero-intro .ct-v8-head>strong');
    if(!target)return;
    if(target.innerHTML!==desired)target.innerHTML=desired;
    target.dataset.ctPageroCopy='fixed';
  };

  const watch=()=>{
    apply();
    const observer=new MutationObserver(apply);
    observer.observe(document.documentElement,{childList:true,subtree:true,characterData:true});
    [0,50,150,350,700,1200,2200,4000,7000].forEach(delay=>setTimeout(apply,delay));
    setTimeout(()=>observer.disconnect(),12000);
  };

  const core=document.createElement('script');
  core.src='/assets/calltag-pagero-intro-core.js?v=20260803-core1';
  core.defer=true;
  core.onload=watch;
  core.onerror=watch;
  document.head.append(core);
})();
