(()=>{
  if(document.documentElement.dataset.ctPageroIndustriesTwoColumn)return;
  document.documentElement.dataset.ctPageroIndustriesTwoColumn='1';

  const css=`
    @media(min-width:761px){
      #ct-pagero-intro .ct-industry-showcase{
        grid-template-columns:minmax(0,.96fr) minmax(0,1.08fr)!important;
        align-items:stretch!important;
        gap:52px!important;
        width:min(100%,1000px)!important;
        max-width:1000px!important;
      }
      #ct-pagero-intro .ct-industry-card{
        min-width:0!important;
        height:100%!important;
        display:flex!important;
        flex-direction:column!important;
      }
      #ct-pagero-intro .ct-industry-phone{
        width:100%!important;
        flex:1 1 auto!important;
        display:flex!important;
      }
      #ct-pagero-intro .ct-industry-screen{
        width:100%!important;
        min-height:650px!important;
      }
      #ct-pagero-intro .ct-industry-auto{
        min-height:22px!important;
        margin-top:16px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-industry-screen{
        padding:0 20px 66px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-head{
        height:60px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-head strong{
        font-size:15px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-head span{
        width:34px!important;
        height:34px!important;
        font-size:13px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-doctor{
        grid-template-columns:92px 1fr!important;
        gap:16px!important;
        padding:18px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-photo{
        height:108px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-info small{
        font-size:9px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-info h3{
        margin:7px 0 5px!important;
        font-size:22px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-info p{
        font-size:9px!important;
        line-height:1.55!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-title{
        margin:24px 0 13px!important;
        font-size:16px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-days{
        gap:8px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-day{
        min-height:54px!important;
        padding:11px 3px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-day b{
        font-size:11px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-day small{
        font-size:8px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-times{
        gap:9px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-times span{
        height:43px!important;
        font-size:10px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-summary{
        min-height:48px!important;
        margin-top:18px!important;
        padding:14px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-summary span,
      #ct-pagero-intro .ct-hospital .ct-hos-summary b{
        font-size:10px!important;
      }
      #ct-pagero-intro .ct-hospital .ct-hos-submit{
        height:50px!important;
        margin-top:13px!important;
        font-size:11px!important;
      }
    }

    @media(min-width:761px) and (max-width:1080px){
      #ct-pagero-intro .ct-industry-showcase{
        gap:30px!important;
        width:min(100%,900px)!important;
      }
      #ct-pagero-intro .ct-industry-screen{
        min-height:620px!important;
      }
    }
  `;

  const apply=()=>{
    const showcase=document.querySelector('#ct-pagero-intro .ct-industry-showcase');
    if(!showcase)return false;

    showcase.querySelectorAll('.ct-industry-card.ct-estate').forEach(card=>card.remove());

    if(!document.querySelector('style[data-ct-pagero-industries-two-column]')){
      const style=document.createElement('style');
      style.dataset.ctPageroIndustriesTwoColumn='1';
      style.textContent=css;
      document.head.append(style);
    }

    showcase.dataset.layout='two-column';
    return showcase.querySelectorAll('.ct-industry-card').length===2;
  };

  if(!apply()){
    const observer=new MutationObserver(()=>{
      if(apply())observer.disconnect();
    });
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),15000);
  }
})();
