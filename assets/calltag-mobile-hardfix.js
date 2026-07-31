(()=>{
  if(document.documentElement.dataset.ctMobileHardfix)return;
  document.documentElement.dataset.ctMobileHardfix='1';

  const apply=()=>{
    if(document.querySelector('style[data-ct-mobile-hardfix]'))return;
    const style=document.createElement('style');
    style.dataset.ctMobileHardfix='1';
    style.textContent=`
      @media(max-width:900px){
        html,body{width:100%!important;max-width:100%!important;overflow-x:hidden!important}
        body{padding-bottom:76px!important}
        .wrap{width:calc(100% - 28px)!important;max-width:none!important}
        .header,.header-inner{height:62px!important;min-height:62px!important;padding-top:0!important;padding-bottom:0!important}

        /* Fixed CTA: keep one compact button and stop covering content. */
        .ad-sticky{left:14px!important;right:14px!important;bottom:10px!important;width:auto!important;min-height:56px!important;transform:none!important;padding:7px!important;border-radius:16px!important;gap:0!important}
        .ad-sticky>div{display:none!important}
        .ad-sticky a{width:100%!important;min-height:42px!important;justify-content:center!important;padding:0 14px!important;border-radius:11px!important;font-size:13px!important}
        .scroll-top,.back-to-top,.to-top,.top-button,[data-scroll-top],button[aria-label*="위로"],button[aria-label*="맨 위"]{display:none!important}

        /* Never use desktop minimum widths inside mobile product examples. */
        .product-panel,.customer-detail,.customer-history,.customer-side,.today-content,.calendar-board,.calendar-day,.timeline,.timeline-row{min-width:0!important;max-width:100%!important}
        .product-panel{width:100%!important;padding:14px!important;overflow:hidden!important;border-radius:20px!important}

        /* Today tasks: content and three action buttons always fit the card. */
        .today-header{padding:2px 1px 15px!important}
        .today-header h4{font-size:22px!important}.today-header span{font-size:9px!important}
        .today-card{display:grid!important;grid-template-columns:54px minmax(0,1fr)!important;gap:9px!important;width:100%!important;min-height:0!important;padding:14px 12px!important;margin-top:8px!important;overflow:hidden!important;transform:none!important}
        .today-time{font-size:12px!important}.today-main{min-width:0!important}.today-main strong{font-size:14px!important;line-height:1.35!important}.today-main p{font-size:10px!important;line-height:1.45!important}
        .today-actions{grid-column:1/-1!important;display:grid!important;grid-template-columns:repeat(3,minmax(0,1fr))!important;width:100%!important;gap:6px!important;margin-top:3px!important;overflow:visible!important}
        .ui-action{width:100%!important;min-width:0!important;min-height:35px!important;justify-content:center!important;padding:0 4px!important;font-size:10px!important;white-space:nowrap!important;overflow:hidden!important}

        /* Customer detail: compact summary instead of a long desktop sidebar. */
        .customer-detail{display:block!important;min-height:0!important;overflow:hidden!important;border-radius:18px!important}
        .customer-side{display:grid!important;grid-template-columns:54px minmax(0,1fr)!important;gap:4px 12px!important;padding:18px!important;border-right:0!important;border-bottom:1px solid var(--line)!important}
        .large-avatar{grid-column:1!important;grid-row:1/5!important;width:54px!important;height:54px!important;font-size:15px!important}
        .customer-side h4{grid-column:2!important;margin:0!important;font-size:21px!important;line-height:1.2!important}
        .customer-side>p{grid-column:2!important;margin:0!important;font-size:11px!important}
        .customer-state{grid-column:2!important;width:max-content!important;margin-top:5px!important;padding:6px 9px!important;font-size:9px!important}
        .detail-meta{grid-column:1/-1!important;display:grid!important;grid-template-columns:repeat(3,minmax(0,1fr))!important;gap:6px!important;margin-top:14px!important}
        .detail-meta div{min-width:0!important;padding:9px 7px!important}.detail-meta span{font-size:8px!important}.detail-meta b{font-size:10px!important;line-height:1.35!important;word-break:break-word!important}
        .customer-history{padding:18px!important}.customer-history h4{font-size:22px!important}.timeline{margin-top:18px!important}
        .timeline-row{grid-template-columns:12px 54px minmax(0,1fr)!important;gap:8px!important;min-height:84px!important;padding:6px 0!important;transform:none!important}
        .timeline-dot{width:8px!important;height:8px!important}.timeline-dot:after{left:3px!important;height:66px!important}
        .timeline-time{font-size:8px!important}.timeline-content{min-width:0!important}.timeline-content strong{font-size:12px!important;line-height:1.35!important}.timeline-content p{font-size:10px!important;line-height:1.45!important}.timeline-content span{font-size:8px!important}

        /* Calendar: true seven-column mobile grid; no horizontal scrolling. */
        #calendar .product-panel{overflow:hidden!important}
        .calendar-head{padding-bottom:14px!important}.calendar-head h4{font-size:22px!important}.calendar-head span{font-size:9px!important}
        .calendar-board{width:100%!important;min-width:0!important;display:grid!important;grid-template-columns:repeat(7,minmax(0,1fr))!important;gap:3px!important;overflow:visible!important}
        .calendar-label{min-width:0!important;min-height:22px!important;font-size:8px!important}
        .calendar-day{width:auto!important;min-width:0!important;min-height:54px!important;aspect-ratio:auto!important;padding:5px 3px!important;border-radius:8px!important;font-size:9px!important;overflow:hidden!important;transform:none!important}
        .calendar-event{display:block!important;width:8px!important;height:8px!important;margin:7px auto 0!important;padding:0!important;border-radius:50%!important;font-size:0!important;line-height:0!important;overflow:hidden!important}
        .calendar-detail{left:10px!important;right:10px!important;bottom:10px!important;min-width:0!important;max-width:calc(100% - 20px)!important;padding:12px!important}

        /* Headings and section spacing: mobile-sized, centered and not oversized. */
        .feature-copy{padding:0 4px!important;text-align:center!important}.feature-copy h3,.ct-feature-only-title{font-size:clamp(32px,9vw,42px)!important;line-height:1.08!important;letter-spacing:-.065em!important}
        .section,.ad-section{padding:72px 0!important}
      }
      @media(max-width:420px){
        .wrap{width:calc(100% - 24px)!important}
        .product-panel{padding:11px!important}
        .today-card{grid-template-columns:47px minmax(0,1fr)!important;padding:12px 9px!important}.today-actions{gap:4px!important}.ui-action{font-size:9px!important}
        .customer-side{grid-template-columns:48px minmax(0,1fr)!important;padding:15px!important}.large-avatar{width:48px!important;height:48px!important}.detail-meta{grid-template-columns:1fr!important}
        .customer-history{padding:15px!important}.timeline-row{grid-template-columns:10px 47px minmax(0,1fr)!important;gap:7px!important}
        .calendar-day{min-height:48px!important;padding:4px 2px!important}.calendar-label{font-size:7px!important}
      }
    `;
    document.head.append(style);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});
  else apply();
})();