(()=>{
  if(document.documentElement.dataset.ctMobileFinal)return;
  document.documentElement.dataset.ctMobileFinal='1';
  const apply=()=>{
    const benefits=document.querySelector('.ct-benefit-flow');
    const benefitSection=benefits&&benefits.closest('.ad-section');
    if(benefitSection)benefitSection.classList.add('ct-benefit-section-mobile');
    const style=document.createElement('style');
    style.dataset.ctMobileFinal='1';
    style.textContent=`
      @media(max-width:900px){
        body{padding-bottom:0!important}
        .ad-sticky{display:none!important}
        .ct-benefit-section-mobile{padding-top:56px!important;padding-bottom:64px!important}
        .ct-benefit-flow{display:grid!important;grid-template-columns:1fr!important;gap:0!important;width:100%!important;max-width:100%!important;padding:0!important;overflow:visible!important}
        .ct-benefit-flow .ad-benefit{width:100%!important;min-height:132px!important;padding:22px 20px!important;border-radius:18px!important;opacity:.68!important;transform:none!important}
        .ct-benefit-flow .ad-benefit.is-active{opacity:1!important;transform:none!important;box-shadow:0 16px 38px rgba(59,111,255,.12)!important}
        .ct-benefit-flow .ad-benefit b{font-size:27px!important}.ct-benefit-flow .ad-benefit strong{margin-top:14px!important;font-size:23px!important}.ct-benefit-flow .ad-benefit p{margin-top:8px!important;font-size:11px!important}
        .ct-benefit-arrow{height:34px!important;font-size:34px!important;transform:rotate(90deg)!important}.ct-benefit-arrow.is-lit{transform:rotate(90deg)!important}
        #strengths .ad-head,#pricing .ad-head{margin-bottom:28px!important;padding:0 4px!important}
        #strengths .ad-title,#pricing .ad-title{font-size:clamp(32px,8.8vw,42px)!important;line-height:1.08!important}
        .ct-strength-grid,.ad-strengths{grid-template-columns:1fr!important;gap:10px!important;width:100%!important}
        .ct-strength-grid article,.ad-strength{width:100%!important;min-height:0!important;padding:22px 20px!important;border-radius:18px!important}
        .ct-strength-grid h3,.ad-strength h3{margin-top:15px!important;font-size:23px!important}.ct-strength-grid p,.ad-strength p{font-size:11px!important}
        .ct-suite-flow{grid-template-columns:1fr!important;gap:7px!important;width:100%!important;padding:15px!important}.ct-suite-flow i{transform:rotate(90deg)!important}.ct-suite-flow em{position:static!important;margin-top:4px!important;text-align:center!important}
        .ct-price-grid,.ad-price-grid{grid-template-columns:1fr!important;gap:11px!important;width:100%!important}
        .ct-price-card,.ad-price{width:100%!important;min-width:0!important;min-height:0!important;padding:22px 19px!important;border-radius:18px!important}
        .ct-price-card .price strong,.ad-sale strong{font-size:38px!important}.ct-price-card ul,.ad-price ul{margin-top:18px!important;margin-bottom:18px!important}.ct-price-card li,.ad-price li{font-size:10px!important}
        .ad-pricing{width:calc(100% - 24px)!important}.ad-promo{padding:15px!important;border-radius:14px!important}.ad-promo b{font-size:14px!important}.ad-promo span{font-size:10px!important}
        .ad-final{padding:72px 0!important}.ad-final h2{font-size:clamp(38px,10vw,50px)!important}.ad-final p{font-size:14px!important}
      }
      @media(max-width:420px){
        .ct-benefit-flow .ad-benefit{min-height:122px!important;padding:19px 17px!important}.ct-benefit-flow .ad-benefit strong{font-size:21px!important}
        .ct-price-card,.ad-price{padding:19px 16px!important}.ct-price-card ul,.ad-price ul{grid-template-columns:1fr!important}
      }
    `;
    document.head.append(style);
  };
  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',apply,{once:true}):apply();
})();