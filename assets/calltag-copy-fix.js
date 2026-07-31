(()=>{
  const q=(selector,root=document)=>root.querySelector(selector);
  const qa=(selector,root=document)=>[...root.querySelectorAll(selector)];

  const apply=()=>{
    const kicker=q('.hero-kicker');
    const title=q('.hero-heading h1');
    const description=q('.hero-heading > p:last-of-type');
    const heroActions=q('.hero-heading .ad-actions');
    const offer=q('.hero-heading .ad-offer');

    if(kicker) kicker.remove();
    if(title) title.innerHTML='통화 후<br><span>고객관리.</span>';
    if(description) description.textContent='고객·일정·문자를 통화 직후 남기세요.';
    if(offer) offer.remove();

    if(heroActions){
      const buttons=qa('a',heroActions);
      buttons.slice(1).forEach(button=>button.remove());
      if(buttons[0]) buttons[0].textContent='7일 무료체험';
    }

    qa('.ad-copy').forEach(el=>el.remove());
    qa('.ad-target p,.ad-benefit p,.ad-strength p,.ad-plan-copy').forEach(el=>el.remove());
    qa('.section-copy').forEach((el,index)=>{ if(index>0) el.remove(); });

    const targetTitle=q('#targets .ad-title');
    if(targetTitle) targetTitle.innerHTML='이런 업종에<br>필요합니다.';

    const style=document.createElement('style');
    style.textContent=`
      .hero-app{padding:118px 0 72px!important}
      .hero h1{font-size:clamp(54px,7.4vw,96px)!important;line-height:.96!important}
      .hero-heading>p{margin-top:20px!important;font-size:clamp(16px,1.4vw,19px)!important}
      .hero-heading .ad-actions{margin-top:22px!important}
      .hero-heading .ad-btn{min-width:170px}
      .ad-section{padding:88px 0!important}
      .ad-head{margin-bottom:34px!important}
      .ad-target{min-height:150px!important}
      .ad-benefit{min-height:135px!important}
      .ad-strength{min-height:175px!important}
      @media(max-width:700px){
        .hero-app{padding:98px 0 58px!important}
        .hero h1{font-size:52px!important}
        .hero-heading .ad-actions{display:block!important}
        .hero-heading .ad-btn{width:auto!important;min-width:170px}
      }
    `;
    document.head.appendChild(style);

    const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
    let textNode;
    while((textNode=walker.nextNode())){
      textNode.nodeValue=textNode.nodeValue
        .replaceAll('이어집니다.','')
        .replaceAll('흐름입니다.','')
        .replaceAll('정리됩니다.','')
        .replaceAll('한눈에 볼 수 있습니다.','');
    }
  };

  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',apply,{once:true});
  else requestAnimationFrame(apply);
})();