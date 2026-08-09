(()=>{
  if(document.documentElement.dataset.ctMobileCleanV5)return;
  document.documentElement.dataset.ctMobileCleanV5='1';
  const mobile=matchMedia('(max-width:900px)');
  const qa=(selector,root=document)=>[...root.querySelectorAll(selector)];
  if(!document.querySelector('link[data-ct-mobile-static]')){
    const link=document.createElement('link');
    link.rel='stylesheet';
    link.href='/assets/calltag-mobile.css?v=20260809-mobile1';
    link.dataset.ctMobileStatic='1';
    document.head.append(link);
  }
  const normalize=()=>{
    if(!mobile.matches)return;
    qa('.phone-stage .app-screen,.ct-story-step,.ct-story-step .ct-screen,.ct-journey-clean .ct-horizontal-clean__panel,.ct-industry-v5__card,#strengths .ad-strength,.ct-strength-grid article').forEach(node=>{
      node.style.removeProperty('opacity');
      node.style.removeProperty('transform');
      node.style.removeProperty('filter');
      node.style.removeProperty('height');
      node.style.removeProperty('min-height');
    });
    qa('.phone-stage .app-screen').forEach(node=>node.classList.add('active'));
    qa('.scroll-top,.back-to-top,.to-top,.top-button,.scrollTop,#scrollTop,.ct-scroll-top,[data-scroll-top],[aria-label*="맨 위"],[aria-label*="위로"],[title*="맨 위"],[title*="위로"]').forEach(node=>node.remove());
  };
  const apply=()=>requestAnimationFrame(normalize);
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});else apply();
  const onViewportChange=apply;
  addEventListener('resize',onViewportChange,{passive:true});
  mobile.addEventListener?.('change',onViewportChange);
  const timers=[120,500,1200,2500].map(delay=>setTimeout(apply,delay));
  window.addEventListener('pagehide',()=>{
    timers.forEach(clearTimeout);
    removeEventListener('resize',onViewportChange);
    mobile.removeEventListener?.('change',onViewportChange);
  },{once:true});
})();
