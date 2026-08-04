(()=>{
  if(document.documentElement.dataset.ctFooterLinksV5)return;
  document.documentElement.dataset.ctFooterLinksV5='1';

  const destinations={
    '이용약관':'/terms/',
    '개인정보처리방침':'/privacy/',
    '환불정책':'/refund/',
    '고객센터':'/support/'
  };

  const installStyle=()=>{
    if(document.querySelector('style[data-ct-footer-click-fix]'))return;
    const style=document.createElement('style');
    style.dataset.ctFooterClickFix='1';
    style.textContent=`
      .ct-wayzi-footer{
        position:relative!important;
        z-index:80!important;
        isolation:isolate!important;
        overflow:visible!important;
        pointer-events:auto!important;
        transform:none!important;
        filter:none!important;
        opacity:1!important;
      }
      .ct-wayzi-footer:before,.ct-wayzi-footer:after{pointer-events:none!important}
      .ct-wayzi-footer .wrap,
      .ct-wayzi-footer nav,
      .ct-wayzi-footer a{
        position:relative!important;
        z-index:3!important;
        pointer-events:auto!important;
      }
      .ct-wayzi-footer a{
        cursor:pointer!important;
        touch-action:manipulation!important;
        -webkit-tap-highlight-color:rgba(120,151,255,.22)!important;
      }
    `;
    document.head.append(style);
  };

  const apply=()=>{
    installStyle();
    const footer=document.querySelector('.ct-wayzi-footer,footer');
    if(!footer)return;

    footer.style.pointerEvents='auto';
    footer.style.position='relative';
    footer.style.zIndex='80';

    footer.querySelectorAll('a').forEach(anchor=>{
      const label=(anchor.textContent||'').replace(/\s+/g,' ').trim();
      if(destinations[label]){
        anchor.href=destinations[label];
        anchor.target='_self';
        anchor.dataset.ctFooterDestination=destinations[label];
      }
      if((anchor.getAttribute('href')||'').startsWith('tel:'))anchor.remove();
    });
  };

  const forceNavigate=event=>{
    const anchor=event.target.closest?.('.ct-wayzi-footer a');
    if(!anchor)return;
    const destination=anchor.dataset.ctFooterDestination;
    if(!destination)return;
    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation?.();
    window.location.assign(new URL(destination,window.location.origin).href);
  };

  document.addEventListener('click',forceNavigate,true);

  let queued=false;
  const queue=()=>{
    if(queued)return;
    queued=true;
    requestAnimationFrame(()=>{queued=false;apply()});
  };

  const boot=()=>{
    apply();
    const observer=new MutationObserver(queue);
    observer.observe(document.body||document.documentElement,{childList:true,subtree:true});
    [100,400,1000,2500].forEach(delay=>setTimeout(apply,delay));
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
