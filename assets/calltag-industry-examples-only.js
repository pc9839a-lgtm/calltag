(()=>{
  if(document.documentElement.dataset.ctIndustryExamplesOnly)return;
  document.documentElement.dataset.ctIndustryExamplesOnly='1';

  const style=document.createElement('style');
  style.dataset.ctIndustryExamplesOnly='1';
  style.textContent=`
    .ct-industries-static .ct-horizontal-clean__copy,
    .ct-horizontal-industries-clean .ct-horizontal-clean__copy,
    .ct-industry-label,
    .ct-industry-auto{
      display:none!important;
    }

    .ct-industries-static{
      padding:64px 24px!important;
    }

    .ct-industries-static .ct-horizontal-clean__track{
      align-items:stretch!important;
    }

    .ct-industries-static .ct-horizontal-clean__panel{
      display:flex!important;
      align-items:center!important;
      justify-content:center!important;
      min-height:0!important;
      padding:0!important;
      border:0!important;
      border-radius:0!important;
      background:transparent!important;
      box-shadow:none!important;
    }

    .ct-industries-static .ct-horizontal-clean__visual,
    .ct-industries-static .ct-industry-card{
      width:100%!important;
      max-width:340px!important;
      margin:0 auto!important;
    }

    .ct-industries-static .ct-industry-phone{
      margin:0!important;
    }

    @media(max-width:900px){
      .ct-industries-static{padding:48px 16px!important}
      .ct-industries-static .ct-horizontal-clean__track{gap:28px!important}
    }
  `;
  document.head.append(style);

  const clean=()=>{
    document.querySelectorAll(
      '.ct-horizontal-industries-clean .ct-horizontal-clean__copy,'+
      '.ct-industries-static .ct-horizontal-clean__copy,'+
      '.ct-industry-label,'+
      '.ct-industry-auto'
    ).forEach(element=>element.remove());
  };

  const boot=()=>{
    clean();
    const observer=new MutationObserver(clean);
    observer.observe(document.body,{childList:true,subtree:true});
    setTimeout(()=>{clean();observer.disconnect()},12000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();