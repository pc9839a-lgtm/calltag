(()=>{
  if(document.documentElement.dataset.ctFeatureCopyExact)return;
  document.documentElement.dataset.ctFeatureCopyExact='2';

  const titles={
    tasks:'오늘 할 일을<br><span>바로 확인하세요.</span>',
    history:'한눈에 보이는<br><span>상담이력</span>',
    calendar:'모든 일정 정리는<br><span>콜태그에서!</span>'
  };

  const apply=()=>{
    Object.entries(titles).forEach(([id,title])=>{
      const copyBox=document.querySelector(`#${id} .feature-copy`);
      const html=`<h3 class="ct-feature-only-title">${title}</h3>`;
      if(copyBox&&copyBox.innerHTML!==html)copyBox.innerHTML=html;
    });

    const pageroCopy=document.querySelector('#ct-pagero-intro .ct-v8-head>strong');
    if(pageroCopy&&pageroCopy.innerHTML!=='페이지로에서 문의를 받으면<br>관리는 콜태그 앱에서!'){
      pageroCopy.innerHTML='페이지로에서 문의를 받으면<br>관리는 콜태그 앱에서!';
    }

    const tagTitle=document.querySelector('.ct-journey-clean .ct-horizontal-clean__panel:nth-child(3) .ct-horizontal-clean__copy h2');
    if(tagTitle&&tagTitle.innerHTML!=='전화가 끝나면<br>태그만 하세요'){
      tagTitle.innerHTML='전화가 끝나면<br>태그만 하세요';
    }

    document.querySelectorAll('.ct-horizontal-industries-clean .ct-horizontal-clean__copy,.ct-industries-static .ct-horizontal-clean__copy').forEach(element=>element.remove());

    if(!document.querySelector('style[data-ct-feature-copy-exact]')){
      const style=document.createElement('style');
      style.dataset.ctFeatureCopyExact='2';
      style.textContent=`
        .ct-feature-only-title span{color:var(--blue-2)}
        .ct-horizontal-industries-clean .ct-horizontal-clean__copy,
        .ct-industries-static .ct-horizontal-clean__copy{display:none!important}
        @media(max-width:900px){.ct-feature-only-title{text-align:center}}
      `;
      document.head.append(style);
    }
  };

  let timer=0;
  const queue=()=>{
    clearTimeout(timer);
    timer=setTimeout(apply,30);
  };

  const boot=()=>{
    apply();
    const observer=new MutationObserver(queue);
    observer.observe(document.body,{childList:true,subtree:true});
    setTimeout(()=>{apply();observer.disconnect()},10000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();