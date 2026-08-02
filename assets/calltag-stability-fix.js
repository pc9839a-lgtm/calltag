(()=>{
  if(document.documentElement.dataset.ctStabilityFix)return;
  document.documentElement.dataset.ctStabilityFix='1';

  const installStyles=()=>{
    if(document.querySelector('style[data-ct-stability-fix]'))return;
    const style=document.createElement('style');
    style.dataset.ctStabilityFix='1';
    style.textContent=`
      .ct-horizontal-industries .ct-h-track,
      .ct-journey-horizontal .ct-j-track{backface-visibility:hidden;transform-style:preserve-3d}
      .ct-horizontal-industries .ct-h-panel,
      .ct-journey-horizontal .ct-j-panel{contain:layout paint style}

      @media(max-width:900px){
        html,body{overflow-x:hidden!important}
        body{padding-bottom:calc(82px + env(safe-area-inset-bottom))!important}
        .ad-sticky{display:flex!important;position:fixed!important;left:12px!important;right:12px!important;bottom:calc(10px + env(safe-area-inset-bottom))!important;z-index:999!important;width:auto!important;min-height:58px!important;padding:7px!important;border:1px solid rgba(255,255,255,.14)!important;border-radius:17px!important;background:rgba(13,15,20,.94)!important;box-shadow:0 18px 54px rgba(0,0,0,.46)!important;backdrop-filter:blur(18px)!important;transform:none!important}
        .ad-sticky>div{display:none!important}
        .ad-sticky a{display:flex!important;width:100%!important;min-height:44px!important;align-items:center!important;justify-content:center!important;padding:0 16px!important;border-radius:12px!important;font-size:14px!important;font-weight:900!important}

        .section-copy,.ad-copy,.feature-copy p,.ct-j-copy p,.ct-h-copy p,.ct-auto-message-copy p{font-size:14px!important;line-height:1.65!important}
        .today-main p,.timeline-content p,.ct-price-card li,.ad-price li,.price-list div{font-size:12px!important;line-height:1.5!important}
        .today-main strong,.timeline-content strong{font-size:13px!important;line-height:1.4!important}
        .ui-action{min-height:44px!important;font-size:12px!important;padding:0 8px!important}
        .calendar-label{font-size:10px!important}
        .calendar-day{min-height:52px!important;font-size:11px!important}
        .calendar-event{width:9px!important;height:9px!important;margin-top:7px!important}
        .detail-meta span,.summary-cell span{font-size:10px!important}
        .detail-meta b,.summary-cell b{font-size:11px!important}
        .ct-price-card li,.ad-price li{min-height:18px!important}

        .ct-horizontal-industries .ct-h-track,
        .ct-journey-horizontal .ct-j-track{scroll-behavior:smooth;overscroll-behavior-x:contain;-webkit-overflow-scrolling:touch}
        .ct-horizontal-industries .ct-h-panel,
        .ct-journey-horizontal .ct-j-panel{scroll-snap-stop:always}
      }

      @media(max-width:420px){
        .section-copy,.ad-copy,.feature-copy p,.ct-j-copy p,.ct-h-copy p{font-size:13px!important}
        .today-main p,.timeline-content p,.ct-price-card li,.ad-price li{font-size:11px!important}
        .ui-action{font-size:11px!important}
        .calendar-label{font-size:9px!important}
        .calendar-day{font-size:10px!important}
      }
    `;
    document.head.append(style);
  };

  const cleanPlaceholderReviews=()=>{
    const reviews=document.querySelector('#reviews');
    if(reviews&&/베타 테스트 전|실제 후기 공개 예정|후기를 준비하고 있습니다/.test(reviews.textContent||'')){
      reviews.remove();
      document.querySelectorAll('a[href="#reviews"]').forEach(link=>link.remove());
    }
  };

  const stabilizeCtas=()=>{
    const label='7일 무료체험 시작';
    const ariaLabel='콜태그 7일 무료체험 시작';
    document.querySelectorAll('.ad-sticky a').forEach(link=>{
      if(link.textContent.trim()!==label)link.textContent=label;
      if(link.getAttribute('aria-label')!==ariaLabel)link.setAttribute('aria-label',ariaLabel);
    });
  };

  const apply=()=>{
    installStyles();
    cleanPlaceholderReviews();
    stabilizeCtas();
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});
  else apply();

  const observer=new MutationObserver(apply);
  observer.observe(document.documentElement,{childList:true,subtree:true});
  setTimeout(()=>observer.disconnect(),12000);
})();
