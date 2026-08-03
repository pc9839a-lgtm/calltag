(()=>{
  if(document.documentElement.dataset.ctHorizontalPinFix)return;
  document.documentElement.dataset.ctHorizontalPinFix='1';

  const style=document.createElement('style');
  style.dataset.ctHorizontalPinFix='1';
  style.textContent=`
    :root{--ct-horizontal-header:68px}
    @media(min-width:901px){
      .ct-horizontal-clean{position:relative!important;overflow:visible!important;transform:none!important;filter:none!important;opacity:1!important;contain:layout style!important}
      .ct-horizontal-clean__sticky{position:sticky!important;top:var(--ct-horizontal-header)!important;height:calc(100svh - var(--ct-horizontal-header))!important;min-height:560px!important;padding-top:0!important;overflow:hidden!important;transform:none!important}
      .ct-horizontal-clean__track{height:100%!important;align-items:stretch!important}
      .ct-horizontal-clean__panel{height:100%!important;min-height:0!important;padding-top:52px!important;padding-bottom:54px!important}
      .ct-horizontal-clean__top{top:20px!important}
      .ct-horizontal-clean__progress{bottom:22px!important}
    }
  `;
  document.head.append(style);

  const mount=()=>{
    const industry=document.querySelector('.ct-horizontal-industries-clean');
    const journey=document.querySelector('.ct-journey-clean');
    const intro=document.querySelector('#ct-pagero-intro');
    if(!industry||!journey||!intro)return false;

    const topLevel=[...document.body.children].find(node=>node===intro||node.contains(intro))||intro;
    if(industry.parentElement!==document.body)topLevel.insertAdjacentElement('afterend',industry);
    if(journey.previousElementSibling!==industry)industry.insertAdjacentElement('afterend',journey);

    const header=document.querySelector('.header,.site-header,body>header,header');
    const headerHeight=Math.max(0,Math.min(96,Math.ceil(header?.getBoundingClientRect().height||68)));
    document.documentElement.style.setProperty('--ct-horizontal-header',`${headerHeight}px`);

    const industryCount=industry.querySelectorAll('.ct-horizontal-clean__panel').length||3;
    const journeyCount=journey.querySelectorAll('.ct-horizontal-clean__panel').length||4;
    industry.style.height=`calc(${industryCount*100}svh - ${headerHeight}px)`;
    journey.style.height=`calc(${journeyCount*100}svh - ${headerHeight}px)`;

    [industry,journey].forEach(section=>{
      section.classList.remove('ct-motion-section','ct-motion-enter','is-inview');
      section.style.transform='none';
      section.style.filter='none';
      section.style.opacity='1';
    });

    requestAnimationFrame(()=>{
      window.dispatchEvent(new Event('resize'));
      requestAnimationFrame(()=>window.dispatchEvent(new Event('scroll')));
    });
    return true;
  };

  const boot=()=>{
    if(mount())return;
    const observer=new MutationObserver(()=>{if(mount())observer.disconnect()});
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),12000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
