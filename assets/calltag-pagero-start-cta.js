(()=>{
  if(document.documentElement.dataset.ctPageroStartCta)return;
  document.documentElement.dataset.ctPageroStartCta='1';

  const mount=()=>{
    const section=document.querySelector('#ct-pagero-intro .ct-pagero-connect');
    const wrap=section?.querySelector(':scope > .wrap');
    const heading=wrap?.querySelector('h2');
    if(!section||!wrap||!heading)return false;

    section.classList.add('ct-has-start-cta');
    let cta=wrap.querySelector('.ct-pagero-start-cta');
    if(!cta){
      cta=document.createElement('a');
      cta.className='ct-pagero-start-cta';
      cta.href='https://pagero.kr/app';
      cta.setAttribute('aria-label','페이지로 무료로 시작');
      cta.innerHTML='<span>페이지로 무료로 시작</span><i aria-hidden="true">→</i>';
      heading.insertAdjacentElement('afterend',cta);
    }

    if(!document.querySelector('style[data-ct-pagero-start-cta]')){
      const style=document.createElement('style');
      style.dataset.ctPageroStartCta='1';
      style.textContent=`
        .ct-pagero-start-cta{display:inline-flex;align-items:center;justify-content:center;gap:12px;min-height:58px;margin:30px auto 0;padding:0 28px;border:1px solid rgba(151,174,255,.7);border-radius:999px;background:linear-gradient(135deg,#3b6fff,#5c7cff);color:#fff!important;font-size:16px;font-weight:900;letter-spacing:-.025em;text-decoration:none!important;box-shadow:0 17px 40px rgba(41,77,196,.34);transition:transform .22s ease,box-shadow .22s ease,filter .22s ease}
        .ct-pagero-start-cta i{width:28px;height:28px;display:grid;place-items:center;border-radius:50%;background:rgba(255,255,255,.15);font-size:16px;font-style:normal;transition:transform .22s ease}
        .ct-pagero-start-cta:hover{transform:translateY(-2px);filter:brightness(1.07);box-shadow:0 21px 48px rgba(41,77,196,.43)}
        .ct-pagero-start-cta:hover i{transform:translateX(2px)}
        .ct-pagero-start-cta:focus-visible{outline:3px solid rgba(144,168,255,.55);outline-offset:4px}
        .ct-pagero-connect.ct-has-start-cta .ct-connect-demo{margin-top:48px}
        @media(max-width:640px){.ct-pagero-start-cta{width:min(100%,270px);min-height:54px;margin-top:25px;padding:0 21px;font-size:15px}.ct-pagero-connect.ct-has-start-cta .ct-connect-demo{margin-top:40px}}
        @media(prefers-reduced-motion:reduce){.ct-pagero-start-cta,.ct-pagero-start-cta i{transition:none!important}}
      `;
      document.head.append(style);
    }
    return true;
  };

  const boot=()=>{
    if(mount())return;
    const observer=new MutationObserver(()=>{if(mount())observer.disconnect();});
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),12000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
