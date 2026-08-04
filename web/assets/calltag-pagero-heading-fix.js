(()=>{
  if(document.documentElement.dataset.ctPageroHeadingFix)return;
  document.documentElement.dataset.ctPageroHeadingFix='1';

  const desired='랜딩페이지에서 받은 문의,<br><span>콜태그에서 바로 관리하세요.</span>';

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
