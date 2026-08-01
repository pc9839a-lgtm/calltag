(()=>{
  if(document.documentElement.dataset.ctHeroResponsiveV38)return;
  document.documentElement.dataset.ctHeroResponsiveV38='1';

  const apply=()=>{
    if(document.getElementById('ct-hero-responsive-v38'))return;
    const style=document.createElement('style');
    style.id='ct-hero-responsive-v38';
    style.textContent=`
      #ct-pagero-intro .ct-signal-hero{
        min-height:0!important;
        padding:72px 0 82px!important;
      }
      #ct-pagero-intro .ct-signal-head{
        width:min(100%,1120px)!important;
        max-width:1120px!important;
        margin:0 auto!important;
        padding:0 32px!important;
      }
      #ct-pagero-intro .ct-signal-kicker{
        margin:0 0 17px!important;
        font-size:13px!important;
        line-height:1!important;
      }
      #ct-pagero-intro .ct-signal-head h1{
        width:100%!important;
        max-width:1050px!important;
        margin:0 auto!important;
        font-size:clamp(50px,5.35vw,82px)!important;
        line-height:.96!important;
        letter-spacing:-.068em!important;
        text-wrap:balance!important;
      }
      #ct-pagero-intro .ct-signal-head h1 .ct-line{
        white-space:nowrap!important;
      }
      #ct-pagero-intro .ct-signal-head>strong{
        width:min(100%,760px)!important;
        margin:31px auto 0!important;
        font-size:clamp(16px,1.28vw,19px)!important;
        line-height:1.55!important;
        letter-spacing:-.025em!important;
      }
      #ct-pagero-intro .ct-signal-stage{
        margin-top:58px!important;
      }
      #ct-pagero-intro .ct-stage-badge{
        top:25px!important;
      }
      #ct-pagero-intro .ct-stage-badge.left{
        left:56px!important;
      }
      #ct-pagero-intro .ct-stage-badge.right{
        right:clamp(76px,10vw,150px)!important;
      }

      @media(max-width:1200px){
        #ct-pagero-intro .ct-signal-hero{padding:66px 0 74px!important}
        #ct-pagero-intro .ct-signal-head{max-width:940px!important}
        #ct-pagero-intro .ct-signal-head h1{max-width:900px!important;font-size:clamp(47px,5.55vw,67px)!important;letter-spacing:-.062em!important}
        #ct-pagero-intro .ct-signal-head>strong{margin-top:27px!important;font-size:17px!important}
        #ct-pagero-intro .ct-signal-stage{margin-top:52px!important}
      }

      @media(max-width:900px){
        #ct-pagero-intro .ct-signal-hero{padding:58px 0 66px!important}
        #ct-pagero-intro .ct-signal-head{max-width:760px!important;padding:0 24px!important}
        #ct-pagero-intro .ct-signal-kicker{margin-bottom:15px!important;font-size:12px!important}
        #ct-pagero-intro .ct-signal-head h1{max-width:720px!important;font-size:clamp(43px,7vw,57px)!important;line-height:.98!important;letter-spacing:-.058em!important}
        #ct-pagero-intro .ct-signal-head>strong{max-width:620px!important;margin-top:25px!important;font-size:16px!important}
        #ct-pagero-intro .ct-signal-stage{margin-top:46px!important}
        #ct-pagero-intro .ct-stage-badge.left{left:28px!important}
        #ct-pagero-intro .ct-stage-badge.right{right:28px!important}
      }

      @media(max-width:640px){
        #ct-pagero-intro .ct-signal-hero{padding:48px 0 58px!important}
        #ct-pagero-intro .ct-signal-head{padding:0 18px!important}
        #ct-pagero-intro .ct-signal-head h1{max-width:520px!important;font-size:clamp(36px,10.5vw,48px)!important;line-height:1!important;letter-spacing:-.052em!important}
        #ct-pagero-intro .ct-signal-head h1 .ct-line{white-space:normal!important}
        #ct-pagero-intro .ct-signal-head>strong{max-width:390px!important;margin-top:22px!important;font-size:15px!important;line-height:1.6!important}
        #ct-pagero-intro .ct-signal-stage{margin-top:38px!important}
      }

      @media(max-width:420px){
        #ct-pagero-intro .ct-signal-hero{padding-top:42px!important}
        #ct-pagero-intro .ct-signal-head h1{font-size:34px!important;line-height:1.02!important;letter-spacing:-.048em!important}
        #ct-pagero-intro .ct-signal-head>strong{font-size:14px!important;margin-top:19px!important}
      }
    `;
    document.head.append(style);
  };

  apply();
  const observer=new MutationObserver(apply);
  observer.observe(document.documentElement,{childList:true,subtree:true});
  setTimeout(()=>observer.disconnect(),12000);
})();