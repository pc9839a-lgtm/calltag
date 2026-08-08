(()=>{
  if(document.documentElement.dataset.ctPageroIndustriesThreeCardFixV2)return;
  document.documentElement.dataset.ctPageroIndustriesThreeCardFixV2='1';

  const css=`
    #ct-pagero-intro .ct-industry-auto{display:none!important}
    @media(min-width:761px){
      #ct-pagero-intro .ct-industry-showcase{grid-template-columns:repeat(3,minmax(0,1fr))!important;align-items:stretch!important;gap:28px!important;width:100%!important;max-width:1100px!important}
      #ct-pagero-intro .ct-industry-card{min-width:0!important;width:100%!important;height:100%!important;display:flex!important;flex-direction:column!important}
      #ct-pagero-intro .ct-industry-phone{width:100%!important;height:620px!important;min-height:620px!important;display:flex!important}
      #ct-pagero-intro .ct-industry-screen{width:100%!important;height:600px!important;min-height:600px!important}
      #ct-pagero-intro .ct-hospital .ct-industry-screen{padding:0 17px 58px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-head{height:56px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-head strong{font-size:14px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-head span{width:31px!important;height:31px!important;font-size:12px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-doctor{grid-template-columns:80px 1fr!important;gap:14px!important;padding:16px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-photo{height:96px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-info small{font-size:8px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-info h3{margin:7px 0 5px!important;font-size:19px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-info p{font-size:8px!important;line-height:1.5!important}
      #ct-pagero-intro .ct-hospital .ct-hos-title{margin:21px 0 11px!important;font-size:14px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-days{gap:7px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-day{min-height:48px!important;padding:10px 2px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-day b{font-size:10px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-day small{font-size:7px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-times{gap:8px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-times span{height:39px!important;font-size:9px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-summary{min-height:44px!important;margin-top:16px!important;padding:13px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-summary span,#ct-pagero-intro .ct-hospital .ct-hos-summary b{font-size:9px!important}
      #ct-pagero-intro .ct-hospital .ct-hos-submit{height:44px!important;margin-top:12px!important;font-size:10px!important}
    }
    @media(min-width:761px) and (max-width:1080px){#ct-pagero-intro .ct-industry-showcase{gap:17px!important}#ct-pagero-intro .ct-industry-phone{height:595px!important;min-height:595px!important}#ct-pagero-intro .ct-industry-screen{height:575px!important;min-height:575px!important}}
  `;

  const apply=()=>{
    const showcase=document.querySelector('#ct-pagero-intro .ct-industry-showcase');
    if(!showcase)return false;
    showcase.querySelectorAll('.ct-industry-auto').forEach(text=>text.remove());
    if(!document.querySelector('style[data-ct-pagero-industries-three-card-fix]')){const style=document.createElement('style');style.dataset.ctPageroIndustriesThreeCardFix='2';style.textContent=css;document.head.append(style);}
    showcase.dataset.layout='three-column';
    return showcase.querySelectorAll('.ct-industry-card').length===3;
  };

  if(!apply()){
    const observer=new MutationObserver(()=>{if(apply())observer.disconnect();});
    observer.observe(document.documentElement,{childList:true,subtree:true});
    const timer=setTimeout(()=>observer.disconnect(),5000);
    window.addEventListener('pagehide',()=>{clearTimeout(timer);observer.disconnect();},{once:true});
  }
})();