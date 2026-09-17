(()=>{
  if(document.documentElement.dataset.ctAppOnlyV1)return;
  document.documentElement.dataset.ctAppOnlyV1='1';

  let applying=false;
  const replacements=[
    ['오늘 할 일과 웹 캘린더에 추가됨','오늘 할 일과 일정에 추가됨'],
    ['앱과 PC에서 고객과 일정 확인','앱에서 고객과 일정 확인'],
    ['고객별 상담 이력과 웹 캘린더','고객별 상담 이력과 일정 관리'],
    ['Android 앱 · PC 웹 연동','Android 앱 지원'],
    ['웹 화면은 PC와 모바일 브라우저에서 확인할 수 있습니다.',''],
    ['휴대폰에서 기록, 큰 화면에서 확인','앱에서 바로 기록하고 확인']
  ];

  const patchText=()=>{
    const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
    let node;
    while((node=walker.nextNode())){
      const parent=node.parentElement;
      if(!parent||['SCRIPT','STYLE','NOSCRIPT','TEXTAREA'].includes(parent.tagName))continue;
      let next=node.nodeValue;
      replacements.forEach(([from,to])=>{if(next.includes(from))next=next.replaceAll(from,to);});
      if(next!==node.nodeValue)node.nodeValue=next;
    }
  };

  const hideLegacyWeb=()=>{
    const web=document.getElementById('web');
    if(web){
      web.hidden=true;
      web.setAttribute('aria-hidden','true');
      web.style.setProperty('display','none','important');
    }
    document.querySelectorAll('a[href="#web"]').forEach(link=>link.remove());
    document.querySelectorAll('.ct-story-step[data-number="04"],.ct-app-web-roles').forEach(node=>node.remove());
  };

  const patchBenefits=()=>{
    document.querySelectorAll('.ad-benefit').forEach(card=>{
      if(!(card.textContent||'').includes('PC'))return;
      const strong=card.querySelector('strong');
      const copy=card.querySelector('p');
      if(strong)strong.textContent='앱에서 고객관리';
      if(copy)copy.textContent='앱에서 고객과 일정, 다음 할 일을 확인';
    });
  };

  const patchFaq=()=>{
    document.querySelectorAll('.faq-item').forEach(item=>{
      const question=item.querySelector('.faq-question');
      const answer=item.querySelector('.faq-answer p');
      const q=(question?.textContent||'').trim();
      if(q.includes('웹에서는')||q.includes('PC에서')){item.remove();return;}
      if(q.includes('아이폰')&&answer){
        answer.textContent='현재 콜태그 앱은 Android 전용입니다.';
      }
    });
  };

  const apply=()=>{
    if(applying)return;
    applying=true;
    try{hideLegacyWeb();patchBenefits();patchFaq();patchText();}
    finally{applying=false;}
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});else apply();
  [80,220,500,900,1500,2400,3600,5200,7200].forEach(delay=>setTimeout(apply,delay));
  const observer=new MutationObserver(()=>requestAnimationFrame(apply));
  observer.observe(document.documentElement,{subtree:true,childList:true,characterData:true,attributes:true,attributeFilter:['hidden','aria-hidden','class']});
  setTimeout(()=>observer.disconnect(),9000);
})();
