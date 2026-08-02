(()=>{
  if(document.documentElement.dataset.ctMobileSystem)return;
  document.documentElement.dataset.ctMobileSystem='1';

  const style=document.createElement('style');
  style.dataset.ctMobileSystem='1';
  style.textContent=`
    @media(max-width:900px){
      html,body{width:100%!important;max-width:100%!important;overflow-x:hidden!important}
      body{min-width:0!important;padding-bottom:calc(84px + env(safe-area-inset-bottom))!important}
      .wrap{width:calc(100% - 28px)!important;max-width:none!important;margin-inline:auto!important}
      img,svg,video,canvas{max-width:100%}

      .header,.header-inner{height:62px!important;min-height:62px!important}
      .header-inner{gap:14px!important}.logo{font-size:18px!important}.logo-mark{width:32px!important;height:32px!important}.nav{display:none!important}

      .hero-app{padding:90px 0 72px!important}.hero-web{padding:84px 0!important}
      .hero-heading,.web-heading-copy{max-width:100%!important}
      .hero-kicker{margin-bottom:12px!important;font-size:13px!important}
      .hero h1,.hero-heading h1{font-size:clamp(32px,10vw,46px)!important;line-height:1.04!important;letter-spacing:-.07em!important;white-space:normal!important;text-wrap:balance}
      .hero-heading>p,.web-heading-copy p{max-width:350px!important;margin-top:16px!important;font-size:14px!important;line-height:1.6!important}

      .phone-stage{grid-template-columns:1fr!important;gap:28px!important;margin-top:38px!important;padding:15px!important;border-radius:22px!important}
      .step-panel{padding:6px 0!important}.stage-label{justify-content:center!important;font-size:12px!important}
      .step-title{margin-top:18px!important;font-size:clamp(34px,9.4vw,45px)!important;text-align:center!important}.step-sub{margin-top:12px!important;font-size:15px!important;text-align:center!important}
      .step-list{grid-template-columns:repeat(3,minmax(0,1fr))!important;gap:7px!important;margin-top:24px!important}
      #app .step-item{display:flex!important;min-width:0!important;min-height:76px!important;flex-direction:column!important;justify-content:center!important;gap:7px!important;padding:10px 5px!important;border-radius:13px!important;text-align:center!important;transform:none!important}
      #app .step-item:nth-child(4){display:none!important}#app .step-item b{width:31px!important;height:31px!important;margin:0 auto!important;border-radius:9px!important;font-size:11px!important}
      #app .step-item strong{font-size:12px!important;line-height:1.25!important}#app .step-item small{display:none!important}.progress-track{margin-top:12px!important}
      .phone-shell{width:100%!important;max-width:360px!important;min-height:650px!important;margin:0 auto!important;padding:7px!important;border-radius:38px!important}
      .phone-screen{min-height:634px!important;border-radius:31px!important}.phone-status{height:36px!important;padding:0 19px!important}.app-screen{inset:36px 0 0!important;padding:10px 18px 22px!important}
      .app-title{font-size:24px!important}.app-sub{margin:8px 0 18px!important;font-size:12px!important}.option{min-height:49px!important;padding:0 13px!important}.save-bar{min-height:50px!important}.done-ring{margin-top:48px!important}

      .section,.ad-section{padding:76px 0!important}.ad-head{margin-bottom:34px!important}
      .section-title,.ad-title{font-size:clamp(34px,9.2vw,45px)!important;line-height:1.08!important;letter-spacing:-.065em!important;text-wrap:balance}
      .section-copy,.ad-copy,.feature-copy p{font-size:14px!important;line-height:1.65!important}
      .feature-block,.feature-block.reverse{grid-template-columns:1fr!important;gap:32px!important}.feature-block+.feature-block{margin-top:86px!important}.feature-block.reverse .feature-copy{order:0!important}
      .feature-copy{padding:0 4px!important;text-align:center!important}.feature-copy h3,.ct-feature-only-title{font-size:clamp(34px,9.2vw,44px)!important;line-height:1.08!important;letter-spacing:-.065em!important}
      .feature-copy p{margin:15px auto 0!important}.feature-points{margin-top:20px!important}.ct-single-callout{justify-content:center!important;margin-top:20px!important;padding:12px 14px!important;font-size:13px!important}

      .product-panel{width:100%!important;min-width:0!important;max-width:100%!important;padding:14px!important;overflow:hidden!important;border-radius:20px!important}
      .today-content,.customer-detail,.customer-history,.customer-side,.calendar-board,.calendar-day,.timeline,.timeline-row{min-width:0!important;max-width:100%!important}
      .today-header{align-items:flex-start!important;gap:8px!important;padding:2px 1px 15px!important}.today-header h4{font-size:22px!important}.today-header span{font-size:10px!important;text-align:right!important}
      .today-card{display:grid!important;grid-template-columns:54px minmax(0,1fr)!important;gap:9px!important;width:100%!important;min-height:0!important;padding:14px 12px!important;margin-top:8px!important;overflow:hidden!important;transform:none!important}
      .today-time{font-size:12px!important}.today-main{min-width:0!important}.today-main strong{font-size:14px!important;line-height:1.35!important}.today-main p{font-size:11px!important;line-height:1.5!important}
      .today-actions{grid-column:1/-1!important;display:grid!important;grid-template-columns:repeat(3,minmax(0,1fr))!important;width:100%!important;gap:6px!important;margin-top:4px!important;overflow:visible!important}
      .ui-action{width:100%!important;min-width:0!important;min-height:44px!important;justify-content:center!important;padding:0 5px!important;font-size:11px!important;white-space:nowrap!important;overflow:hidden!important}
      .task-toast,.calendar-detail{left:10px!important;right:10px!important;bottom:10px!important;min-width:0!important;max-width:calc(100% - 20px)!important;padding:12px!important}

      .customer-detail{display:block!important;min-height:0!important;overflow:hidden!important;border-radius:18px!important}
      .customer-side{display:grid!important;grid-template-columns:54px minmax(0,1fr)!important;gap:4px 12px!important;padding:18px!important;border-right:0!important;border-bottom:1px solid var(--line)!important}
      .large-avatar{grid-column:1!important;grid-row:1/5!important;width:54px!important;height:54px!important;font-size:15px!important}.customer-side h4{grid-column:2!important;margin:0!important;font-size:21px!important;line-height:1.2!important}
      .customer-side>p{grid-column:2!important;margin:0!important;font-size:11px!important}.customer-state{grid-column:2!important;width:max-content!important;margin-top:5px!important;padding:6px 9px!important;font-size:10px!important}
      .detail-meta{grid-column:1/-1!important;display:grid!important;grid-template-columns:repeat(3,minmax(0,1fr))!important;gap:6px!important;margin-top:14px!important}.detail-meta div{min-width:0!important;padding:9px 7px!important}.detail-meta span{font-size:9px!important}.detail-meta b{font-size:10px!important;line-height:1.35!important;word-break:break-word!important}
      .customer-history{padding:18px!important}.customer-history h4{font-size:22px!important}.timeline{margin-top:18px!important}.timeline-row{grid-template-columns:12px 54px minmax(0,1fr)!important;gap:8px!important;min-height:84px!important;padding:6px 0!important;transform:none!important}
      .timeline-dot{width:8px!important;height:8px!important}.timeline-dot:after{left:3px!important;height:66px!important}.timeline-time{font-size:9px!important}.timeline-content{min-width:0!important}.timeline-content strong{font-size:12px!important;line-height:1.35!important}.timeline-content p{font-size:10px!important;line-height:1.45!important}.timeline-content span{font-size:9px!important}

      #calendar .product-panel{overflow:hidden!important}.calendar-head{padding-bottom:14px!important}.calendar-head h4{font-size:22px!important}.calendar-head span{font-size:10px!important}
      .calendar-board{width:100%!important;min-width:0!important;display:grid!important;grid-template-columns:repeat(7,minmax(0,1fr))!important;gap:3px!important;overflow:visible!important}
      .calendar-label{min-width:0!important;min-height:22px!important;font-size:9px!important}.calendar-day{width:auto!important;min-width:0!important;min-height:52px!important;aspect-ratio:auto!important;padding:5px 3px!important;border-radius:8px!important;font-size:10px!important;overflow:hidden!important;transform:none!important}
      .calendar-event{display:block!important;width:8px!important;height:8px!important;margin:7px auto 0!important;padding:0!important;border-radius:50%!important;font-size:0!important;line-height:0!important;overflow:hidden!important}

      .ct-convert-section,.ct-auto-message-section{padding:76px 0!important}.ct-convert-head h2,.ct-auto-message-copy h2{font-size:clamp(36px,9.8vw,47px)!important;line-height:1.06!important}.ct-convert-head p{margin-bottom:13px!important;font-size:13px!important}
      .ct-convert-stage,.ct-auto-message-layout{grid-template-columns:1fr!important;gap:20px!important;margin-top:36px!important}.ct-record-card,.ct-customer-card{padding:22px!important;border-radius:19px!important;transform:none!important}.ct-record-card strong{margin-top:20px!important;font-size:29px!important}
      .ct-convert-arrow{width:54px!important;height:54px!important;margin:0 auto!important;transform:rotate(90deg)!important}.ct-customer-meta{grid-template-columns:1fr!important}.ct-customer-meta div{padding:13px!important}
      .ct-auto-message-copy{text-align:center!important}.ct-auto-message-point{justify-content:center!important;margin-top:21px!important;font-size:13px!important}.ct-auto-message-preview{width:100%!important;padding:19px!important;border-radius:21px!important}.ct-auto-bubble{max-width:100%!important;margin-top:22px!important;padding:17px!important;font-size:14px!important}.ct-auto-result{align-items:flex-start!important;flex-direction:column!important;padding:15px!important}

      .ct-story-layout{grid-template-columns:1fr!important;gap:0!important}.ct-story-sticky{position:static!important;min-height:auto!important;padding:76px 0 20px!important;text-align:center!important}.ct-story-sticky h2{font-size:clamp(39px,10.2vw,50px)!important}.ct-story-status{justify-content:center!important;margin-top:26px!important}.ct-story-status strong{font-size:42px!important}.ct-story-current{margin:10px auto 0!important;font-size:14px!important}
      .ct-story-steps{padding:0 0 58px!important}.ct-story-step{min-height:auto!important;padding:34px 0!important;opacity:1!important;transform:none!important}.ct-story-step h3{margin:11px 0 20px!important;font-size:clamp(31px,8.6vw,41px)!important}.ct-screen{min-height:auto!important;padding:16px!important;border-radius:20px!important;transform:none!important}.ct-screen-grid.three,.ct-screen-task{grid-template-columns:1fr!important}

      .ct-benefit-flow{display:grid!important;grid-template-columns:1fr!important;gap:0!important;width:100%!important;max-width:100%!important;padding:0!important;overflow:visible!important}.ct-benefit-flow .ad-benefit{width:100%!important;min-height:132px!important;padding:22px 20px!important;border-radius:18px!important;opacity:.72!important;transform:none!important}.ct-benefit-flow .ad-benefit.is-active{opacity:1!important;transform:none!important;box-shadow:0 16px 38px rgba(59,111,255,.12)!important}.ct-benefit-flow .ad-benefit b{font-size:27px!important}.ct-benefit-flow .ad-benefit strong{margin-top:14px!important;font-size:23px!important}.ct-benefit-flow .ad-benefit p{margin-top:8px!important;font-size:12px!important}.ct-benefit-arrow{height:34px!important;font-size:34px!important;transform:rotate(90deg)!important}

      #targets .ad-head{text-align:center!important}#targets .ad-title{font-size:clamp(28px,8.2vw,39px)!important;white-space:normal!important}#targets .ct-marquee-viewport{margin-left:-14px!important;width:calc(100% + 28px)!important;mask-image:linear-gradient(90deg,transparent,#000 5%,#000 95%,transparent)!important;-webkit-mask-image:linear-gradient(90deg,transparent,#000 5%,#000 95%,transparent)!important}
      #targets .ct-marquee-group{gap:12px!important;padding-right:12px!important}#targets .ct-marquee-group .ad-target{flex-basis:80vw!important;width:80vw!important;min-height:220px!important;padding:25px 22px!important;border-radius:20px!important}#targets .ct-marquee-group .ad-target h3{margin-top:16px!important;font-size:27px!important}#targets .ct-marquee-group .ad-target b{margin-top:24px!important;font-size:12px!important}

      .ct-strength-grid,.ad-strengths{grid-template-columns:1fr!important;gap:10px!important;width:100%!important}.ct-strength-grid article,.ad-strength{width:100%!important;min-height:0!important;padding:22px 20px!important;border-radius:18px!important}.ct-strength-grid article>b,.ad-strength>b{font-size:28px!important}.ct-strength-grid h3,.ad-strength h3{margin-top:15px!important;font-size:23px!important}.ct-strength-grid p,.ad-strength p{font-size:12px!important}
      .ct-suite-flow{grid-template-columns:1fr!important;gap:7px!important;width:100%!important;padding:15px!important}.ct-suite-flow i{transform:rotate(90deg)!important}.ct-suite-flow em{position:static!important;margin-top:4px!important;text-align:center!important}
      .ct-price-grid,.ad-price-grid{grid-template-columns:1fr!important;gap:11px!important;width:100%!important}.ct-price-card,.ad-price{width:100%!important;min-width:0!important;min-height:0!important;padding:22px 19px!important;border-radius:18px!important}.ct-price-card>p{min-height:0!important}.ct-price-card .price strong,.ad-sale strong{font-size:38px!important}.ct-price-card ul,.ad-price ul{grid-template-columns:1fr!important;gap:9px!important;margin:18px 0!important}.ct-price-card li,.ad-price li{font-size:11px!important}.ad-pricing{width:calc(100% - 24px)!important}.ad-promo{padding:15px!important;border-radius:14px!important}.ad-promo b{font-size:14px!important}.ad-promo span{font-size:11px!important}

      .fact-strip{grid-template-columns:repeat(2,1fr)!important}.fact{min-height:92px!important;padding:17px!important}.fact b{font-size:18px!important}.fact span{font-size:11px!important}.faq-grid,.faq-list{grid-template-columns:1fr!important}.footer-inner{flex-direction:column!important;align-items:flex-start!important;gap:18px!important}

      .ad-sticky{display:flex!important;position:fixed!important;left:12px!important;right:12px!important;bottom:calc(10px + env(safe-area-inset-bottom))!important;z-index:999!important;width:auto!important;min-height:58px!important;transform:none!important;padding:7px!important;border-radius:17px!important;gap:0!important}
      .ad-sticky>div{display:none!important}.ad-sticky a{display:flex!important;width:100%!important;min-height:44px!important;align-items:center!important;justify-content:center!important;padding:0 16px!important;border-radius:12px!important;font-size:14px!important;font-weight:900!important}
      .scroll-top,.back-to-top,.to-top,.top-button,[data-scroll-top],button[aria-label*="위로"],button[aria-label*="맨 위"]{display:none!important}
    }

    @media(max-width:520px){
      .wrap{width:calc(100% - 24px)!important}.hero-app{padding-top:82px!important}.hero h1,.hero-heading h1{font-size:clamp(30px,9.8vw,40px)!important}.phone-stage{padding:12px!important}.step-title{font-size:36px!important}.step-list{gap:5px!important}
      #app .step-item{min-height:70px!important;padding:8px 3px!important}#app .step-item strong{font-size:11px!important}.phone-shell{max-width:336px!important;min-height:626px!important}.phone-screen{min-height:612px!important}.app-screen{padding-left:15px!important;padding-right:15px!important}
      .section,.ad-section{padding:70px 0!important}.feature-copy h3,.section-title,.ad-title{font-size:34px!important}.product-panel{padding:11px!important}.today-card{grid-template-columns:47px minmax(0,1fr)!important;padding:12px 9px!important}.today-actions{gap:4px!important}.ui-action{font-size:10px!important;padding:0 3px!important}
      .customer-side{grid-template-columns:48px minmax(0,1fr)!important;padding:15px!important}.large-avatar{width:48px!important;height:48px!important}.detail-meta{grid-template-columns:1fr!important}.customer-history{padding:15px!important}.timeline-row{grid-template-columns:10px 47px minmax(0,1fr)!important;gap:7px!important}
      .calendar-day{min-height:48px!important;padding:4px 2px!important}.calendar-label{font-size:8px!important}.ct-auto-message-preview{padding:16px!important}.ct-auto-bubble{padding:15px!important;font-size:13px!important}
      #targets .ct-marquee-group .ad-target{flex-basis:84vw!important;width:84vw!important;min-height:210px!important;padding:23px 20px!important}#targets .ct-marquee-group .ad-target h3{font-size:26px!important}
      .ct-price-card,.ad-price{padding:19px 16px!important}.ct-price-card .price strong,.ad-sale strong{font-size:37px!important}
    }
  `;
  document.head.append(style);
})();
