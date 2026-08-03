(()=>{
  if(document.documentElement.dataset.ctMobileFinalV3)return;
  document.documentElement.dataset.ctMobileFinalV3='1';

  const mobile=matchMedia('(max-width:900px)');

  const reorder=()=>{
    document.querySelectorAll('.feature-block').forEach(block=>{
      const copy=block.querySelector(':scope > .feature-copy');
      const visual=block.querySelector(':scope > .product-panel,:scope > .feature-visual');
      if(!copy||!visual)return;
      if(mobile.matches){
        copy.style.setProperty('order','0','important');
        visual.style.setProperty('order','1','important');
        block.dataset.ctMobileOrdered='1';
      }else if(block.dataset.ctMobileOrdered==='1'){
        copy.style.removeProperty('order');
        visual.style.removeProperty('order');
        delete block.dataset.ctMobileOrdered;
      }
    });

    document.querySelectorAll('.ct-horizontal-clean__panel').forEach(panel=>{
      const copy=panel.querySelector(':scope > .ct-horizontal-clean__copy');
      const visual=panel.querySelector(':scope > .ct-horizontal-clean__visual');
      if(!copy||!visual)return;
      if(mobile.matches){
        copy.style.setProperty('order','0','important');
        visual.style.setProperty('order','1','important');
      }else{
        copy.style.removeProperty('order');
        visual.style.removeProperty('order');
      }
    });
  };

  const style=document.createElement('style');
  style.dataset.ctMobileFinalV3='1';
  style.textContent=`
    @media(max-width:900px){
      html,body{width:100%!important;max-width:100%!important;overflow-x:hidden!important}
      body{min-width:0!important;padding-bottom:0!important}
      .wrap{width:calc(100% - 32px)!important;max-width:none!important;margin-inline:auto!important}
      img,svg,video,canvas{max-width:100%!important;height:auto}

      .header{height:58px!important;background:rgba(9,10,13,.94)!important;border-bottom:1px solid rgba(255,255,255,.08)!important;backdrop-filter:blur(16px)!important}
      .header-inner{height:58px!important;min-height:58px!important;width:calc(100% - 24px)!important;gap:8px!important}
      .logo{font-size:16px!important;gap:7px!important}.logo-mark{width:30px!important;height:30px!important;border-radius:9px!important}.logo-mark svg{width:18px!important;height:18px!important}
      .ct-product-switch{margin-left:auto!important;padding:3px!important;border-radius:10px!important}.ct-product-switch a{padding:7px 8px!important;font-size:9px!important}.nav{display:none!important}

      .hero-app,.hero-web{padding:92px 0 76px!important}.hero-heading,.web-heading-copy{text-align:left!important}.hero-kicker{margin-bottom:12px!important;font-size:12px!important}
      .hero h1,.hero-heading h1,.ct-v8-head h1{font-size:clamp(39px,11.5vw,52px)!important;line-height:.98!important;letter-spacing:-.075em!important;white-space:normal!important;text-wrap:balance!important}
      .hero-heading>p,.web-heading-copy p{max-width:360px!important;margin:17px 0 0!important;font-size:14px!important;line-height:1.58!important}
      .hero-heading .ad-actions{display:grid!important;grid-template-columns:1fr!important;gap:9px!important;width:100%!important;margin-top:24px!important}.hero-heading .ad-btn{width:100%!important;min-height:52px!important}

      .section,.ad-section,.audience{padding:84px 0!important}.feature-block+.feature-block{margin-top:96px!important}.section-head,.ad-head{margin-bottom:32px!important;text-align:left!important}
      .section-title,.ad-title,.feature-copy h3,.ct-feature-only-title,.ct-convert-head h2,.ct-auto-message-copy h2,.audience h2,#pricing h2,#faq h2{font-size:clamp(35px,10vw,44px)!important;line-height:1.04!important;letter-spacing:-.068em!important;text-align:left!important;text-wrap:balance!important}
      .section-copy,.ad-copy,.feature-copy p{max-width:none!important;font-size:14px!important;line-height:1.62!important;text-align:left!important}
      .feature-block,.feature-block.reverse{display:grid!important;grid-template-columns:1fr!important;gap:24px!important;align-items:start!important}
      .feature-block>.feature-copy,.feature-block.reverse>.feature-copy{order:0!important;padding:0!important;text-align:left!important}
      .feature-block>.product-panel,.feature-block>.feature-visual,.feature-block.reverse>.product-panel,.feature-block.reverse>.feature-visual{order:1!important}
      .product-panel,.feature-visual{width:100%!important;min-width:0!important;max-width:100%!important;margin:0!important;padding:13px!important;border-radius:20px!important;overflow:hidden!important}.product-panel *,.feature-visual *{max-width:100%!important}

      .phone-stage{grid-template-columns:1fr!important;gap:24px!important;margin-top:34px!important;padding:14px!important;border-radius:22px!important}.step-panel{padding:4px 0!important}.stage-label{justify-content:flex-start!important;font-size:11px!important}.step-title{margin-top:16px!important;font-size:36px!important;text-align:left!important}.step-sub{font-size:14px!important;text-align:left!important}
      .step-list{grid-template-columns:repeat(3,minmax(0,1fr))!important;gap:7px!important}.step-item{min-width:0!important;padding:9px 5px!important}.phone-shell{max-width:350px!important;min-height:620px!important;margin:0 auto!important;border-radius:38px!important}.phone-screen{min-height:604px!important}

      .ct-journey-clean{height:auto!important;overflow:visible!important}.ct-journey-clean .ct-horizontal-clean__sticky{position:relative!important;top:auto!important;height:auto!important;min-height:0!important;overflow:visible!important}
      .ct-journey-clean .ct-horizontal-clean__track{display:grid!important;height:auto!important;transform:none!important;will-change:auto!important}.ct-journey-clean .ct-horizontal-clean__panel{width:100%!important;height:auto!important;min-height:0!important;display:grid!important;grid-template-columns:1fr!important;gap:22px!important;padding:70px 16px!important;border-bottom:1px solid rgba(255,255,255,.08)!important}
      .ct-journey-clean .ct-horizontal-clean__copy{order:0!important;max-width:none!important;text-align:left!important;opacity:1!important;filter:none!important;transform:none!important}.ct-journey-clean .ct-horizontal-clean__copy h2{font-size:clamp(36px,10.5vw,44px)!important;line-height:1!important;text-align:left!important}
      .ct-journey-clean .ct-horizontal-clean__visual{order:1!important;width:100%!important;max-width:430px!important;margin:0 auto!important;opacity:1!important;filter:none!important;transform:none!important}.ct-journey-clean .ct-j-scene-clean{width:100%!important;height:auto!important;min-height:390px!important;border-radius:18px!important}
      .ct-journey-clean .ct-j-body{padding:17px!important}.ct-journey-clean .ct-j-phone{right:10px!important;width:205px!important}.ct-horizontal-clean__progress{display:none!important}

      .ct-industry-visual-section{height:auto!important;min-height:0!important;padding:88px 0!important;overflow:hidden!important}.ct-industry-visual__sticky{position:relative!important;top:auto!important;height:auto!important;min-height:0!important;display:block!important;padding:0!important;overflow:visible!important}
      .ct-industry-visual__head{width:calc(100% - 32px)!important;margin:0 auto 28px!important;text-align:left!important}.ct-industry-visual__head h2{font-size:36px!important;line-height:1!important;white-space:nowrap!important;text-align:left!important}
      .ct-industry-visual__stage{width:calc(100% - 32px)!important;height:auto!important;min-height:0!important;display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:12px!important;margin:0 auto!important}.ct-industry-visual__grid,.ct-industry-visual__beam{display:none!important}
      .ct-industry-visual__card,.ct-industry-visual__card.is-xl,.ct-industry-visual__card.is-md,.ct-industry-visual__card.is-sm{position:relative!important;left:auto!important;top:auto!important;width:100%!important;height:190px!important}.ct-industry-visual__inner{border-radius:18px!important}.ct-industry-visual__meta{padding:11px 12px 12px!important}.ct-industry-visual__meta strong{font-size:16px!important}.ct-industry-visual__meta span{font-size:9px!important}

      .ct-convert-stage,.ct-auto-message-layout,.ad-value,.ad-message,.ad-msg-main,.ad-result{grid-template-columns:1fr!important;gap:18px!important}.ct-convert-arrow,.ad-arrow{transform:rotate(90deg)!important;margin:0 auto!important}
      .ct-strength-grid,.ad-strengths,.ct-price-grid,.ad-price-grid,.faq-grid,.faq-list{grid-template-columns:1fr!important;gap:12px!important}.ct-strength-grid article,.ad-strength,.ct-price-card,.ad-price,.faq-item{width:100%!important;min-width:0!important;border-radius:18px!important}
      .ad-price{min-height:0!important;padding:22px 18px!important}.ad-pricing{width:calc(100% - 32px)!important}.ad-promo{padding:15px!important;border-radius:14px!important}

      .ad-final,.final-cta,.cta-section{padding:92px 0!important}.ad-final .wrap,.final-cta .wrap,.cta-section .wrap{width:calc(100% - 32px)!important}.ad-final h2,.final-cta h2,.cta-section h2{font-size:clamp(38px,11vw,48px)!important;line-height:1!important;letter-spacing:-.075em!important}.ad-final p,.final-cta p,.cta-section p{font-size:14px!important;line-height:1.58!important}
      .ad-final .ad-actions,.final-cta .ad-actions,.cta-section .ad-actions{display:grid!important;grid-template-columns:1fr!important;gap:9px!important;width:100%!important;margin-top:24px!important}.ad-final .ad-btn,.final-cta .ad-btn,.cta-section .ad-btn{width:100%!important;min-height:54px!important}
      .ad-sticky,.sticky-offer,.floating-offer,.bottom-offer{display:none!important}.ct-wayzi-footer{padding:38px 0 42px!important}.ct-wayzi-footer__inner{grid-template-columns:1fr!important;gap:22px!important}
    }

    @media(max-width:560px){
      .wrap{width:calc(100% - 28px)!important}.section,.ad-section,.audience{padding:76px 0!important}
      .ct-industry-visual__stage{width:calc(100% - 28px)!important;grid-template-columns:1fr!important}.ct-industry-visual__head{width:calc(100% - 28px)!important}.ct-industry-visual__card,.ct-industry-visual__card.is-xl,.ct-industry-visual__card.is-md,.ct-industry-visual__card.is-sm{height:220px!important}
      .ct-journey-clean .ct-horizontal-clean__panel{padding:64px 14px!important}.today-actions{grid-template-columns:1fr!important}.fact-strip{grid-template-columns:1fr!important}
    }
  `;
  document.head.append(style);

  reorder();
  const observer=new MutationObserver(reorder);
  observer.observe(document.documentElement,{childList:true,subtree:true});
  setTimeout(()=>observer.disconnect(),18000);
  mobile.addEventListener?.('change',reorder);
})();
