(()=>{
  if(document.documentElement.dataset.ctMobileHistoryFixV2)return;
  document.documentElement.dataset.ctMobileHistoryFixV2='1';

  const style=document.createElement('style');
  style.dataset.ctMobileHistoryFix='2';
  style.textContent=`
    @media(max-width:900px){
      #history{display:flex!important;flex-direction:column!important;gap:22px!important;width:100%!important;min-width:0!important;height:auto!important;min-height:0!important;overflow:visible!important}
      #history>.feature-copy{order:0!important;width:100%!important;max-width:360px!important;margin:0 auto!important;padding:0 4px!important;text-align:center!important}
      #history>.product-panel{order:1!important;width:100%!important;max-width:400px!important;height:auto!important;min-height:0!important;margin:0 auto!important;padding:14px!important;overflow:visible!important;border-radius:18px!important}
      #history .customer-detail{display:flex!important;flex-direction:column!important;width:100%!important;min-width:0!important;height:auto!important;min-height:0!important;max-height:none!important;overflow:visible!important;border-radius:15px!important}
      #history .customer-side{width:100%!important;min-width:0!important;height:auto!important;padding:22px 18px!important;border-right:0!important;border-bottom:1px solid rgba(255,255,255,.105)!important}
      #history .large-avatar{width:54px!important;height:54px!important}
      #history .customer-side h4{margin-top:14px!important;font-size:21px!important}
      #history .detail-meta{grid-template-columns:1fr!important;gap:8px!important;margin-top:20px!important}
      #history .detail-meta div{padding:12px!important}
      #history .customer-history{width:100%!important;min-width:0!important;height:auto!important;min-height:0!important;max-height:none!important;padding:22px 16px 24px!important;overflow:visible!important}
      #history .customer-history h4{font-size:19px!important}
      #history .timeline{width:100%!important;margin-top:20px!important;overflow:visible!important}
      #history .timeline-row{grid-template-columns:14px 56px minmax(0,1fr)!important;gap:10px!important;width:100%!important;min-width:0!important;min-height:0!important;height:auto!important;padding:12px 8px!important;overflow:visible!important;transform:none!important}
      #history .timeline-dot{width:9px!important;height:9px!important}
      #history .timeline-dot:after{height:calc(100% + 34px)!important}
      #history .timeline-time{min-width:0!important;font-size:10px!important;line-height:1.45!important}
      #history .timeline-content{min-width:0!important;overflow:visible!important}
      #history .timeline-content strong{font-size:13px!important;line-height:1.4!important;white-space:normal!important}
      #history .timeline-content p{margin-top:6px!important;font-size:11px!important;line-height:1.5!important;white-space:normal!important;overflow:visible!important}
      #history .timeline-content span{max-width:100%!important;margin-top:7px!important;white-space:normal!important}
    }
  `;
  document.head.append(style);

  const mobile=matchMedia('(max-width:900px)');
  const normalize=()=>{
    if(!mobile.matches)return;
    const history=document.querySelector('#history');
    if(!history)return;
    history.querySelectorAll('.product-panel,.customer-detail,.customer-side,.customer-history,.timeline,.timeline-row,.timeline-content').forEach(node=>{
      node.style.removeProperty('height');
      node.style.removeProperty('max-height');
      node.style.removeProperty('overflow');
    });
  };

  const boot=()=>{
    normalize();
    const observer=new MutationObserver(normalize);
    observer.observe(document.documentElement,{childList:true,subtree:true});
    const onResize=normalize;
    addEventListener('resize',onResize,{passive:true});
    mobile.addEventListener?.('change',normalize);
    const timers=[250,1000,2500].map(delay=>setTimeout(normalize,delay));
    const stopTimer=setTimeout(()=>{normalize();observer.disconnect();},5000);
    window.addEventListener('pagehide',()=>{timers.forEach(clearTimeout);clearTimeout(stopTimer);observer.disconnect();removeEventListener('resize',onResize);mobile.removeEventListener?.('change',normalize);},{once:true});
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();