(()=>{
  if(document.documentElement.dataset.ctHeroResponsiveV39)return;
  document.documentElement.dataset.ctHeroResponsiveV39='1';

  const normalize=()=>{
    document.querySelectorAll('#ct-pagero-intro .ct-v7-step b').forEach((el,index)=>{
      el.textContent=String(index+1).padStart(2,'0');
    });
  };

  const style=document.createElement('style');
  style.id='ct-hero-responsive-v39';
  style.textContent=`
    #ct-pagero-intro .ct-v7-hero{min-height:0!important;padding:88px 0 94px!important}
    #ct-pagero-intro .ct-v7-head{width:min(100%,1080px)!important;margin:0 auto!important;padding:0 28px!important}
    #ct-pagero-intro .ct-v7-head>p{margin-bottom:18px!important;font-size:13px!important}
    #ct-pagero-intro .ct-v7-head h1{width:100%!important;max-width:980px!important;margin:0 auto!important;font-size:clamp(54px,5.25vw,78px)!important;line-height:.96!important;letter-spacing:-.068em!important;text-wrap:balance!important}
    #ct-pagero-intro .ct-v7-head>strong{width:min(100%,760px)!important;margin:29px auto 0!important;font-size:clamp(16px,1.25vw,19px)!important;line-height:1.55!important}
    #ct-pagero-intro .ct-v7-stage{grid-template-columns:minmax(0,1.08fr) 86px minmax(350px,.92fr)!important;align-items:stretch!important;gap:28px!important;max-width:1320px!important;margin-top:62px!important;padding:34px!important;border-radius:30px!important}
    #ct-pagero-intro .ct-v7-side{display:grid!important;grid-template-rows:auto 1fr!important;gap:16px!important;align-self:stretch!important;min-width:0!important}
    #ct-pagero-intro .ct-v7-step,#ct-pagero-intro .ct-v7-phone-side .ct-v7-step{width:100%!important;min-height:58px!important;margin:0!important;padding:10px 16px!important;display:flex!important;align-items:center!important;justify-content:flex-start!important;gap:12px!important;align-self:stretch!important;border:1px solid rgba(104,140,255,.28)!important;border-radius:16px!important;background:linear-gradient(135deg,rgba(59,111,255,.14),rgba(59,111,255,.045))!important;color:#f3f6ff!important;font-size:14px!important;font-weight:800!important;letter-spacing:-.025em!important;box-sizing:border-box!important}
    #ct-pagero-intro .ct-v7-step b{flex:0 0 36px!important;width:36px!important;height:36px!important;font-size:11px!important;box-shadow:0 0 0 6px rgba(59,111,255,.1),0 8px 24px rgba(59,111,255,.2)!important}
    #ct-pagero-intro .ct-v7-step span{display:block!important;overflow:visible!important;white-space:nowrap!important}
    #ct-pagero-intro .ct-v7-phone-side{align-items:stretch!important}
    #ct-pagero-intro .ct-v7-inquiry{min-height:446px!important;display:flex!important;flex-direction:column!important;box-sizing:border-box!important;padding:28px!important}
    #ct-pagero-intro .ct-v7-inquiry dl{margin-bottom:0!important}
    #ct-pagero-intro .ct-v7-complete{margin-top:auto!important}
    #ct-pagero-intro .ct-v7-transfer{align-self:stretch!important;min-height:0!important;padding-top:74px!important}
    #ct-pagero-intro .ct-v7-phone{width:min(330px,100%)!important;margin:auto!important}
    #ct-pagero-intro .ct-v7-screen{min-height:424px!important}
    #ct-pagero-intro .ct-v7-progress{margin-top:24px!important;font-size:10px!important}
    @media(max-width:1180px){#ct-pagero-intro .ct-v7-hero{padding:76px 0 84px!important}#ct-pagero-intro .ct-v7-head{max-width:920px!important}#ct-pagero-intro .ct-v7-head h1{max-width:860px!important;font-size:clamp(49px,5.8vw,66px)!important}#ct-pagero-intro .ct-v7-stage{grid-template-columns:minmax(0,1fr) 72px minmax(320px,.9fr)!important;gap:20px!important;margin-top:54px!important;padding:28px!important}#ct-pagero-intro .ct-v7-step,#ct-pagero-intro .ct-v7-phone-side .ct-v7-step{font-size:13px!important}#ct-pagero-intro .ct-v7-inquiry{min-height:430px!important}}
    @media(max-width:900px){#ct-pagero-intro .ct-v7-hero{padding:62px 0 72px!important}#ct-pagero-intro .ct-v7-head{max-width:760px!important;padding:0 22px!important}#ct-pagero-intro .ct-v7-head h1{max-width:700px!important;font-size:clamp(43px,7vw,57px)!important;line-height:.98!important}#ct-pagero-intro .ct-v7-head>strong{margin-top:24px!important;font-size:16px!important}#ct-pagero-intro .ct-v7-stage{grid-template-columns:1fr!important;max-width:720px!important;gap:18px!important;margin-top:46px!important;padding:22px!important}#ct-pagero-intro .ct-v7-side{display:block!important}#ct-pagero-intro .ct-v7-step,#ct-pagero-intro .ct-v7-phone-side .ct-v7-step{margin-bottom:14px!important}#ct-pagero-intro .ct-v7-inquiry{min-height:0!important}#ct-pagero-intro .ct-v7-complete{margin-top:14px!important}#ct-pagero-intro .ct-v7-transfer{min-height:76px!important;padding-top:0!important}#ct-pagero-intro .ct-v7-transfer>b{transform:rotate(90deg)!important}#ct-pagero-intro .ct-v7-transfer>span{width:2px!important;height:68px!important}#ct-pagero-intro .ct-v7-phone{margin:0 auto!important}}
    @media(max-width:640px){#ct-pagero-intro .ct-v7-hero{padding:48px 0 60px!important}#ct-pagero-intro .ct-v7-head{padding:0 17px!important}#ct-pagero-intro .ct-v7-head h1{font-size:clamp(36px,10.2vw,46px)!important;line-height:1!important;letter-spacing:-.055em!important}#ct-pagero-intro .ct-v7-head>strong{max-width:390px!important;margin-top:21px!important;font-size:15px!important}#ct-pagero-intro .ct-v7-stage{margin-top:38px!important;padding:16px!important;border-radius:22px!important}#ct-pagero-intro .ct-v7-step,#ct-pagero-intro .ct-v7-phone-side .ct-v7-step{min-height:54px!important;padding:9px 13px!important;font-size:13px!important}#ct-pagero-intro .ct-v7-step b{flex-basis:32px!important;width:32px!important;height:32px!important;font-size:10px!important}#ct-pagero-intro .ct-v7-inquiry{padding:20px!important}#ct-pagero-intro .ct-v7-phone{width:min(310px,100%)!important}}
    @media(max-width:420px){#ct-pagero-intro .ct-v7-head h1{font-size:34px!important}#ct-pagero-intro .ct-v7-head>strong{font-size:14px!important}}
  `;
  document.head.append(style);
  normalize();
  const observer=new MutationObserver(normalize);
  observer.observe(document.documentElement,{childList:true,subtree:true});
  setTimeout(()=>observer.disconnect(),12000);
})();