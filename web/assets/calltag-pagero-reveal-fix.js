(()=>{
  if(document.documentElement.dataset.ctPageroRevealFix)return;
  document.documentElement.dataset.ctPageroRevealFix='1';

  const style=document.createElement('style');
  style.dataset.ctPageroRevealFix='1';
  style.textContent=`
    .ct-v8-reveal-group{opacity:0;visibility:hidden;transform:translateY(12px) scale(.985);filter:blur(3px);will-change:opacity,transform,filter}
    .ct-v8-stage.is-running .ct-v8-reveal-group{animation:ctInquiryReveal 3.55s cubic-bezier(.22,1,.36,1) .18s both}
    .ct-v8-stage.is-running .ct-v8-complete{animation:ctComplete .44s ease .72s 2 alternate}
    .ct-v8-stage.is-running .ct-v8-dot{animation-delay:1.02s}
    .ct-v8-stage.is-running .ct-v8-push{animation-delay:1.58s}
    .ct-v8-stage.is-running .ct-v8-phone{animation-delay:1.62s}
    .ct-v8-stage.is-running .ct-v8-registered{animation-delay:2.08s}
    .ct-v8-nocode-grid{grid-template-columns:1fr!important;gap:58px!important;text-align:center}
    .ct-v8-nocode-copy{text-align:center}
    .ct-v8-nocode-copy h2{max-width:1050px;margin:0 auto!important}
    .ct-v8-nocode-copy>p{margin-bottom:20px!important}
    .ct-v8-flow{width:100%;max-width:1120px;margin:0 auto}
    @keyframes ctInquiryReveal{
      0%,10%{opacity:0;visibility:hidden;transform:translateY(12px) scale(.985);filter:blur(3px)}
      17%,89%{opacity:1;visibility:visible;transform:translateY(0) scale(1);filter:blur(0)}
      100%{opacity:0;visibility:hidden;transform:translateY(-5px) scale(.99);filter:blur(1px)}
    }
    @media(max-width:640px){.ct-v8-nocode-grid{gap:42px!important}.ct-v8-nocode-copy h2{font-size:39px!important;line-height:1.06!important}}
    @media(prefers-reduced-motion:reduce){.ct-v8-reveal-group{opacity:1!important;visibility:visible!important;transform:none!important;filter:none!important;animation:none!important}}
  `;
  document.head.append(style);

  const updateNoCode=()=>{
    const copy=document.querySelector('#ct-pagero-intro .ct-v8-nocode-copy');
    if(!copy)return false;
    const title=copy.querySelector('h2');
    const description=copy.querySelector(':scope > strong');
    if(title)title.innerHTML='코드를 몰라도 누구나<br><span>랜딩페이지 제작이 가능한 페이지로</span>';
    if(description)description.remove();
    return true;
  };

  const updateInquiry=()=>{
    const inquiry=document.querySelector('#ct-pagero-intro .ct-v8-inquiry');
    if(!inquiry)return false;
    if(inquiry.querySelector('.ct-v8-reveal-group'))return true;

    const details=inquiry.querySelector(':scope > dl');
    const complete=inquiry.querySelector(':scope > .ct-v8-complete');
    if(!details||!complete)return false;

    const group=document.createElement('div');
    group.className='ct-v8-reveal-group';
    details.before(group);
    group.append(details,complete);

    const stage=inquiry.closest('.ct-v8-stage');
    if(stage){
      stage.classList.remove('is-running');
      requestAnimationFrame(()=>{
        void stage.offsetWidth;
        stage.classList.add('is-running');
      });
    }
    return true;
  };

  const apply=()=>updateInquiry()&&updateNoCode();

  if(!apply()){
    const observer=new MutationObserver(()=>{
      if(apply())observer.disconnect();
    });
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),10000);
  }
})();