(()=>{
  if(document.documentElement.dataset.ctHorizontalSingle)return;
  document.documentElement.dataset.ctHorizontalSingle='1';

  const style=document.createElement('style');
  style.dataset.ctHorizontalSingle='1';
  style.textContent=`
    :root{--ct-horizontal-header:68px}

    .ct-industries-static{
      height:auto!important;
      padding:72px 24px!important;
      overflow:hidden!important;
      background:#080a10!important;
    }
    .ct-industries-static .ct-horizontal-clean__sticky{
      position:relative!important;
      inset:auto!important;
      height:auto!important;
      min-height:0!important;
      overflow:visible!important;
      padding:0!important;
    }
    .ct-industries-static .ct-horizontal-clean__track{
      width:min(1180px,100%)!important;
      height:auto!important;
      margin:0 auto!important;
      display:grid!important;
      grid-template-columns:repeat(3,minmax(0,1fr))!important;
      gap:18px!important;
      transform:none!important;
      will-change:auto!important;
    }
    .ct-industries-static .ct-horizontal-clean__panel{
      width:auto!important;
      height:auto!important;
      min-height:0!important;
      display:grid!important;
      grid-template-columns:1fr!important;
      align-content:start!important;
      gap:22px!important;
      padding:28px 18px 22px!important;
      border:1px solid rgba(255,255,255,.1)!important;
      border-radius:22px!important;
      background:rgba(255,255,255,.025)!important;
      overflow:hidden!important;
    }
    .ct-industries-static .ct-horizontal-clean__copy{
      max-width:none!important;
      text-align:center!important;
      transform:none!important;
      opacity:1!important;
      filter:none!important;
    }
    .ct-industries-static .ct-horizontal-clean__copy h3{
      margin:0!important;
      font-size:clamp(23px,2vw,31px)!important;
      line-height:1.08!important;
      letter-spacing:-.055em!important;
    }
    .ct-industries-static .ct-horizontal-clean__visual{
      width:100%!important;
      max-width:340px!important;
      margin:0 auto!important;
      transform:none!important;
      opacity:1!important;
      filter:none!important;
    }
    .ct-industries-static .ct-industry-card{
      width:100%!important;
      max-width:340px!important;
      margin:0 auto!important;
      transform:none!important;
      opacity:1!important;
      filter:none!important;
    }
    .ct-industries-static .ct-horizontal-clean__progress{display:none!important}

    @media(min-width:901px){
      #ct-pagero-intro{overflow:visible!important;transform:none!important;filter:none!important;perspective:none!important;contain:none!important}
      .ct-journey-clean{position:relative!important;overflow:visible!important;transform:none!important;filter:none!important;opacity:1!important;contain:none!important}
      .ct-journey-clean .ct-horizontal-clean__sticky{position:sticky!important;top:var(--ct-horizontal-header)!important;height:calc(100svh - var(--ct-horizontal-header))!important;min-height:540px!important;padding-top:0!important;overflow:hidden!important;transform:none!important}
      .ct-journey-clean .ct-horizontal-clean__track{height:100%!important;align-items:stretch!important}
      .ct-journey-clean .ct-horizontal-clean__panel{height:100%!important;min-height:0!important}
    }

    @media(max-width:900px){
      .ct-industries-static{padding:48px 16px!important}
      .ct-industries-static .ct-horizontal-clean__track{grid-template-columns:1fr!important;max-width:420px!important}
      .ct-industries-static .ct-horizontal-clean__panel{padding:24px 16px!important}
    }
  `;
  document.head.append(style);

  const clearStickyBlockers=section=>{
    let node=section.parentElement;
    while(node&&node!==document.body){
      node.classList.remove('ct-motion-section','ct-motion-enter','is-inview');
      node.style.setProperty('transform','none','important');
      node.style.setProperty('filter','none','important');
      node.style.setProperty('perspective','none','important');
      node.style.setProperty('contain','none','important');
      node.style.setProperty('overflow','visible','important');
      node.style.setProperty('will-change','auto','important');
      node=node.parentElement;
    }
  };

  const mount=()=>{
    const industry=document.querySelector('.ct-horizontal-industries-clean');
    const journey=document.querySelector('.ct-journey-clean');
    if(!industry||!journey)return false;

    if(journey.previousElementSibling!==industry)industry.insertAdjacentElement('afterend',journey);

    industry.classList.add('ct-industries-static');
    industry.classList.remove('ct-horizontal-clean');
    industry.style.setProperty('height','auto','important');
    industry.querySelector('.ct-horizontal-clean__track')?.style.setProperty('transform','none','important');
    industry.querySelectorAll('.ct-horizontal-clean__panel').forEach(panel=>{
      panel.classList.remove('is-active','ct-impact-hit');
      panel.style.removeProperty('--ct-focus');
      panel.style.removeProperty('--ct-side');
    });

    clearStickyBlockers(journey);

    const header=document.querySelector('.header,.site-header,body>header,header');
    const headerHeight=Math.max(0,Math.min(96,Math.ceil(header?.getBoundingClientRect().height||68)));
    document.documentElement.style.setProperty('--ct-horizontal-header',`${headerHeight}px`);
    journey.style.height=`calc(300svh - ${headerHeight}px)`;
    journey.classList.remove('ct-motion-section','ct-motion-enter','is-inview');
    journey.style.setProperty('transform','none','important');
    journey.style.setProperty('filter','none','important');
    journey.style.setProperty('opacity','1','important');

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
    setTimeout(()=>observer.disconnect(),10000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
