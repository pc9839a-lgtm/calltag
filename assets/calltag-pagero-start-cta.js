(()=>{
  if(document.documentElement.dataset.ctPageroStartCtaV2)return;
  document.documentElement.dataset.ctPageroStartCtaV2='1';
  const mount=()=>{
    const section=document.querySelector('#ct-pagero-intro .ct-pagero-connect');
    const wrap=section?.querySelector(':scope > .wrap');
    const demo=wrap?.querySelector('.ct-connect-demo');
    if(!section||!wrap||!demo)return false;
    const captions=[...demo.querySelectorAll('.ct-connect-cap span')];
    if(captions[0])captions[0].textContent='페이지로 문의 접수';
    if(captions[1])captions[1].textContent='콜태그 고객관리';
    const transfer=demo.querySelector('.ct-connect-arrow small');
    if(transfer)transfer.textContent='고객정보 자동 전달';
    wrap.querySelectorAll(':scope > .ct-pagero-start-cta').forEach(el=>el.remove());
    let cta=wrap.querySelector(':scope > .ct-pagero-bottom-cta');
    if(!cta){cta=document.createElement('div');cta.className='ct-pagero-bottom-cta';demo.insertAdjacentElement('afterend',cta);}
    cta.innerHTML='<small>고객 문의를 받을 랜딩페이지가 필요하신가요?</small><a href="https://pagero.kr/app" class="ct-pagero-bottom-cta-btn" aria-label="페이지로 무료로 시작하기">페이지로 무료로 시작하기</a>';
    if(!document.querySelector('style[data-ct-pagero-bottom-cta]')){const style=document.createElement('style');style.dataset.ctPageroBottomCta='2';style.textContent=`.ct-pagero-bottom-cta{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:13px;margin:36px auto 0;text-align:center}.ct-pagero-bottom-cta>small{display:block;color:#aab4c6;font-size:14px;font-weight:750;letter-spacing:-.02em}.ct-pagero-bottom-cta-btn{display:inline-flex;align-items:center;justify-content:center;min-width:310px;min-height:60px;padding:0 30px;border:1px solid rgba(151,174,255,.66);border-radius:999px;background:linear-gradient(135deg,#3b6fff,#5b7cff);color:#fff!important;font-size:20px;font-weight:900;letter-spacing:-.035em;text-decoration:none!important;box-shadow:0 18px 44px rgba(41,77,196,.36);transition:transform .22s ease,box-shadow .22s ease,filter .22s ease}.ct-pagero-bottom-cta-btn:hover{transform:translateY(-2px);filter:brightness(1.06);box-shadow:0 22px 50px rgba(41,77,196,.44)}.ct-pagero-bottom-cta-btn:focus-visible{outline:3px solid rgba(144,168,255,.55);outline-offset:4px}@media(max-width:640px){.ct-pagero-bottom-cta{gap:11px;margin-top:28px}.ct-pagero-bottom-cta>small{max-width:280px;font-size:13px;line-height:1.5}.ct-pagero-bottom-cta-btn{width:min(100%,280px);min-width:0;min-height:56px;padding:0 22px;font-size:17px}}@media(prefers-reduced-motion:reduce){.ct-pagero-bottom-cta-btn{transition:none!important}}`;document.head.append(style);}
    return true;
  };
  const boot=()=>{if(mount())return;const observer=new MutationObserver(()=>{if(mount())observer.disconnect();});observer.observe(document.documentElement,{childList:true,subtree:true});const timer=setTimeout(()=>observer.disconnect(),5000);window.addEventListener('pagehide',()=>{clearTimeout(timer);observer.disconnect();},{once:true});};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();