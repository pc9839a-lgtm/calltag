(()=>{
  if(document.documentElement.dataset.ctMobileOptimize)return;
  document.documentElement.dataset.ctMobileOptimize='1';
  const apply=()=>{
    if(document.querySelector('style[data-ct-mobile-optimize]'))return;
    const style=document.createElement('style');
    style.dataset.ctMobileOptimize='1';
    style.textContent=`
      @media(max-width:900px){
        html,body{max-width:100%;overflow-x:hidden!important}
        body{min-width:0!important}
        .wrap{width:calc(100% - 32px)!important;max-width:none!important}
        .header{height:60px!important}
        .header-inner{gap:14px!important}
        .logo{font-size:18px!important}.logo-mark{width:32px!important;height:32px!important}
        .nav{display:none!important}
        .hero-app{padding:92px 0 72px!important}.hero-web{padding:88px 0!important}
        .hero-heading,.web-heading-copy{max-width:100%!important}
        .hero-kicker{margin-bottom:12px!important;font-size:13px!important}
        .hero h1{font-size:clamp(31px,9vw,42px)!important;line-height:1.04!important;letter-spacing:-.07em!important;white-space:nowrap!important}
        .hero-heading>p{max-width:340px!important;margin-top:16px!important;font-size:14px!important;line-height:1.55!important}
        .phone-stage{grid-template-columns:1fr!important;gap:28px!important;margin-top:38px!important;padding:16px!important;border-radius:22px!important}
        .step-panel{padding:6px 0!important}.stage-label{justify-content:center!important;font-size:12px!important}
        .step-title{margin-top:18px!important;font-size:clamp(34px,9.4vw,45px)!important;text-align:center!important}.step-sub{margin-top:12px!important;font-size:15px!important;text-align:center!important}
        .step-list{grid-template-columns:repeat(3,minmax(0,1fr))!important;gap:7px!important;margin-top:24px!important}
        #app .step-item{display:flex!important;min-width:0!important;min-height:76px!important;flex-direction:column!important;justify-content:center!important;gap:7px!important;padding:10px 5px!important;border-radius:13px!important;text-align:center!important;transform:none!important}
        #app .step-item:nth-child(4){display:none!important}
        #app .step-item b{width:31px!important;height:31px!important;margin:0 auto!important;border-radius:9px!important;font-size:11px!important}
        #app .step-item strong{font-size:12px!important;line-height:1.25!important}#app .step-item small{display:none!important}
        .progress-track{margin-top:12px!important}
        .phone-shell{width:100%!important;max-width:360px!important;min-height:650px!important;margin:0 auto!important;padding:7px!important;border-radius:38px!important}
        .phone-screen{min-height:634px!important;border-radius:31px!important}.phone-status{height:36px!important;padding:0 19px!important}
        .app-screen{inset:36px 0 0!important;padding:10px 18px 22px!important}.app-title{font-size:24px!important}.app-sub{margin:8px 0 18px!important;font-size:12px!important}
        .option{min-height:49px!important;padding:0 13px!important}.save-bar{min-height:50px!important}.done-ring{margin-top:48px!important}
        .ct-convert-section{padding:82px 0!important}.ct-convert-head h2{font-size:clamp(35px,9.4vw,45px)!important;line-height:1.08!important}
        .ct-convert-head p{margin-bottom:13px!important;font-size:12px!important}.ct-convert-stage{grid-template-columns:1fr!important;gap:17px!important;margin-top:38px!important}
        .ct-record-card,.ct-customer-card{padding:22px!important;border-radius:19px!important;transform:none!important}.ct-record-card strong{margin-top:20px!important;font-size:29px!important}
        .ct-convert-arrow{width:54px!important;height:54px!important;margin:0 auto!important;transform:rotate(90deg)!important}.ct-convert-arrow:before{right:15px!important}.ct-convert-arrow b{font-size:25px!important}
        .ct-customer-meta{grid-template-columns:1fr!important}.ct-customer-meta div{padding:13px!important}
        .ct-story-layout{grid-template-columns:1fr!important;gap:0!important}.ct-story-sticky{position:static!important;min-height:auto!important;padding:82px 0 22px!important;text-align:center!important}
        .ct-story-sticky h2{font-size:clamp(40px,10.5vw,52px)!important}.ct-story-status{justify-content:center!important;margin-top:27px!important}.ct-story-status strong{font-size:43px!important}.ct-story-current{margin:10px auto 0!important;font-size:14px!important}
        .ct-story-steps{padding:0 0 62px!important}.ct-story-step{min-height:auto!important;padding:38px 0!important;opacity:1!important;transform:none!important}
        .ct-story-step h3{margin:11px 0 22px!important;font-size:clamp(32px,8.7vw,42px)!important}.ct-screen{min-height:auto!important;padding:17px!important;border-radius:20px!important;transform:none!important}
        .ct-screen-grid.three{grid-template-columns:1fr!important}.ct-screen-task{grid-template-columns:1fr!important}.ct-date-card{padding:22px!important}.ct-date-card strong{font-size:35px!important}
        .ct-message-tabs{grid-template-columns:repeat(2,1fr)!important}.ct-message-preview{padding:18px!important}.ct-message-preview p{font-size:13px!important;line-height:1.65!important}
        .section,.ad-section{padding:88px 0!important}.section-title,.ad-title{font-size:clamp(35px,9.3vw,46px)!important;line-height:1.08!important;letter-spacing:-.065em!important}
        .section-copy,.ad-copy{font-size:15px!important;line-height:1.65!important}.ad-head{margin-bottom:38px!important}
        .feature-block,.feature-block.reverse{grid-template-columns:1fr!important;gap:34px!important}.feature-block+.feature-block{margin-top:92px!important}.feature-block.reverse .feature-copy{order:0!important}
        .feature-copy{text-align:center!important}.feature-copy h3{font-size:clamp(36px,9.5vw,46px)!important;line-height:1.08!important}.feature-copy p{margin:16px auto 0!important;font-size:15px!important;line-height:1.65!important}
        .feature-points{margin-top:22px!important}.ct-single-callout{justify-content:center!important;margin-top:21px!important;padding:12px 14px!important;font-size:13px!important}
        .product-panel{padding:16px!important;border-radius:20px!important;overflow-x:auto!important;scrollbar-width:none!important}.product-panel::-webkit-scrollbar{display:none}
        .today-header{align-items:flex-start!important;gap:8px!important}.today-header h4{font-size:23px!important}.today-header span{font-size:10px!important;text-align:right!important}
        .today-card{grid-template-columns:54px minmax(0,1fr)!important;gap:10px!important;min-height:auto!important;padding:14px!important;margin-top:8px!important;transform:none!important}
        .today-time{font-size:12px!important}.today-main strong{font-size:14px!important}.today-main p{font-size:11px!important}.today-actions{grid-column:1/-1!important;display:grid!important;grid-template-columns:repeat(3,1fr)!important;gap:6px!important}.ui-action{justify-content:center!important;padding:0 7px!important}
        .task-toast,.calendar-detail{right:14px!important;bottom:14px!important;left:14px!important;min-width:0!important}
        .customer-detail{grid-template-columns:1fr!important;min-height:0!important}.customer-side{padding:20px!important;border-right:0!important;border-bottom:1px solid var(--line)!important}.customer-history{padding:20px!important}
        .detail-meta{grid-template-columns:repeat(2,1fr)!important}.timeline-row{grid-template-columns:15px 67px minmax(0,1fr)!important;gap:9px!important;min-height:94px!important;padding:8px 2px!important;transform:none!important}.timeline-time{font-size:9px!important}.timeline-content strong{font-size:12px!important}.timeline-content p{font-size:11px!important}
        .calendar-head h4{font-size:23px!important}.calendar-board{min-width:610px!important;gap:6px!important}.calendar-day{min-height:78px!important;padding:8px!important}.calendar-event{margin-top:9px!important;padding:5px!important}
        .ct-auto-message-section{padding:88px 0!important}.ct-auto-message-layout{grid-template-columns:1fr!important;gap:31px!important;max-width:760px!important}.ct-auto-message-copy{text-align:center!important}.ct-auto-message-copy h2{font-size:clamp(40px,10vw,52px)!important;line-height:1.04!important}.ct-auto-message-point{justify-content:center!important;margin-top:21px!important;font-size:13px!important}
        .ct-auto-message-preview{width:100%!important;padding:20px!important;border-radius:21px!important}.ct-auto-customer{margin-top:19px!important}.ct-auto-bubble{max-width:100%!important;margin-top:23px!important;padding:18px!important;font-size:14px!important}.ct-auto-result{align-items:flex-start!important;flex-direction:column!important;padding:15px!important}
        .ct-benefit-flow{max-width:100%!important}.ct-benefit-flow .ad-benefit{min-height:155px!important;padding:24px 22px!important;transform:scale(.97)!important}.ct-benefit-flow .ad-benefit.is-active{transform:scale(1.015)!important}.ct-benefit-flow .ad-benefit b{font-size:30px!important}.ct-benefit-flow .ad-benefit strong{margin-top:18px!important;font-size:24px!important}.ct-benefit-arrow{height:42px!important;font-size:40px!important}
        #targets .ad-head{text-align:center!important}#targets .ad-title{font-size:clamp(25px,7.2vw,32px)!important;white-space:nowrap!important;letter-spacing:-.06em!important}
        #targets .ct-marquee-viewport{margin-left:-16px!important;width:calc(100% + 32px)!important;mask-image:linear-gradient(90deg,transparent,#000 5%,#000 95%,transparent)!important;-webkit-mask-image:linear-gradient(90deg,transparent,#000 5%,#000 95%,transparent)!important}
        #targets .ct-marquee-group{gap:12px!important;padding-right:12px!important}#targets .ct-marquee-group .ad-target{flex-basis:78vw!important;width:78vw!important;min-height:230px!important;padding:27px 24px!important;border-radius:20px!important}
        #targets .ct-marquee-group .ad-target h3{margin-top:16px!important;font-size:29px!important}#targets .ct-marquee-group .ad-target b{margin-top:25px!important;font-size:12px!important}
        .ct-strength-grid{grid-template-columns:1fr!important;gap:11px!important}.ct-strength-grid article{min-height:auto!important;padding:24px!important;border-radius:18px!important}.ct-strength-grid article>b{font-size:29px!important}.ct-strength-grid h3{margin-top:18px!important;font-size:24px!important}.ct-strength-grid p{font-size:12px!important}
        .ct-suite-flow{grid-template-columns:1fr!important;padding:17px!important}.ct-suite-flow i{transform:rotate(90deg)!important;font-size:23px!important}.ct-suite-flow em{position:static!important;text-align:center!important}
        .ct-price-grid{grid-template-columns:1fr!important;gap:12px!important}.ct-price-card{min-height:auto!important;padding:23px!important;border-radius:19px!important}.ct-price-card>p{min-height:0!important}.ct-price-card .price strong{font-size:39px!important}.ct-price-card ul{grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:9px 12px!important;margin-top:22px!important}.ct-price-card li{font-size:10px!important}
        .fact-strip{grid-template-columns:repeat(2,1fr)!important}.fact{min-height:92px!important;padding:17px!important}.fact b{font-size:18px!important}.fact span{font-size:11px!important}
        .faq-grid,.faq-list{grid-template-columns:1fr!important}.footer-inner{flex-direction:column!important;align-items:flex-start!important;gap:18px!important}
        img,svg,video,canvas{max-width:100%}
      }
      @media(max-width:520px){
        .wrap{width:calc(100% - 28px)!important}
        .hero-app{padding-top:82px!important}.hero h1{font-size:clamp(29px,8.8vw,38px)!important}
        .phone-stage{padding:12px!important}.step-title{font-size:36px!important}.step-list{gap:5px!important}#app .step-item{min-height:70px!important;padding:8px 3px!important}#app .step-item strong{font-size:11px!important}
        .phone-shell{max-width:336px!important;min-height:626px!important}.phone-screen{min-height:612px!important}.app-screen{padding-left:15px!important;padding-right:15px!important}
        .ct-convert-head h2,.ct-story-sticky h2,.ct-auto-message-copy h2{font-size:38px!important}.ct-story-step h3{font-size:34px!important}
        .section,.ad-section{padding:76px 0!important}.feature-copy h3,.section-title,.ad-title{font-size:36px!important}
        .product-panel{padding:13px!important}.today-card{grid-template-columns:45px minmax(0,1fr)!important;padding:12px!important}.today-actions{grid-template-columns:1fr!important}.ui-action{min-height:36px!important}
        .customer-side,.customer-history{padding:16px!important}.detail-meta{grid-template-columns:1fr!important}.timeline-row{grid-template-columns:13px 55px minmax(0,1fr)!important}
        .ct-auto-message-preview{padding:16px!important}.ct-auto-avatar{width:42px!important;height:42px!important}.ct-auto-bubble{padding:15px!important;font-size:13px!important}
        .ct-price-card ul{grid-template-columns:1fr!important}.ct-price-card .price strong{font-size:37px!important}
        #targets .ct-marquee-group .ad-target{flex-basis:84vw!important;width:84vw!important;min-height:215px!important;padding:24px 21px!important}#targets .ct-marquee-group .ad-target h3{font-size:27px!important}
      }
    `;
    document.head.append(style);
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});else apply();
})();