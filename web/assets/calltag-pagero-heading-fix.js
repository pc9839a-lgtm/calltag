(()=>{
  if(document.documentElement.dataset.ctPageroHeadingFix)return;
  document.documentElement.dataset.ctPageroHeadingFix='1';

  const desired='페이지로에서 문의를 받으면<br><span>관리는 콜태그 앱에서!</span>';

  const apply=()=>{
    const heading=document.querySelector('#ct-pagero-intro .ct-pagero-connect h2');
    if(heading&&heading.innerHTML!==desired)heading.innerHTML=desired;
  };

  const boot=()=>{
    apply();
    const observer=new MutationObserver(apply);
    observer.observe(document.documentElement,{childList:true,subtree:true,characterData:true});
    [0,50,150,300,600,1200,2500,5000].forEach(delay=>setTimeout(apply,delay));
    setTimeout(()=>observer.disconnect(),10000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
