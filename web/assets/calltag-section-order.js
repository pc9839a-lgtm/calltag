(()=>{
  if(document.documentElement.dataset.ctLayoutCoordinatorV3)return;
  document.documentElement.dataset.ctLayoutCoordinatorV3='1';

  const unique=nodes=>[...new Set(nodes.filter(Boolean))];
  const moveAfter=(anchor,node)=>{
    if(!anchor||!node||anchor===node)return anchor;
    if(anchor.nextElementSibling!==node)anchor.insertAdjacentElement('afterend',node);
    return node;
  };

  const installBaseStyle=()=>{
    if(document.querySelector('style[data-ct-layout-coordinator]'))return;
    const style=document.createElement('style');
    style.dataset.ctLayoutCoordinator='3';
    style.textContent=`
      #app .ct-calltag-brand{margin:0 0 10px;color:var(--blue-2);font-size:18px;font-weight:950;letter-spacing:.12em;text-align:center}
      .ct-feature-only-title span{color:var(--blue-2)}
      .ct-horizontal-industries-clean .ct-horizontal-clean__copy,
      .ct-industries-static .ct-horizontal-clean__copy{display:none!important}
      @media(max-width:900px){.ct-feature-only-title{text-align:center}}
      @media(max-width:640px){#app .ct-calltag-brand{margin-bottom:8px;font-size:15px}}
    `;
    document.head.append(style);
  };

  const installBrand=()=>{
    const heading=document.querySelector('#app .hero-heading');
    const kicker=heading?.querySelector('.hero-kicker');
    if(!heading||!kicker)return;
    let brand=heading.querySelector(':scope > .ct-calltag-brand');
    if(!brand){
      brand=document.createElement('p');
      brand.className='ct-calltag-brand';
      brand.textContent='콜태그';
      heading.insertBefore(brand,kicker);
    }
  };

  const findCallStory=()=>document.querySelector('#how.ct-story-section')||
    [...document.querySelectorAll('.ct-story-section')].find(section=>{
      const text=(section.textContent||'').replace(/\s+/g,' ').trim();
      return text.includes('통화가 끝나면')&&text.includes('태그만 하세요');
    })||null;

  const applyFixedCopy=()=>{
    const titles={
      tasks:'오늘 할 일을<br><span>바로 확인하세요.</span>',
      history:'한눈에 보이는<br><span>상담이력</span>',
      calendar:'모든 일정 정리는<br><span>콜태그에서!</span>'
    };

    Object.entries(titles).forEach(([id,title])=>{
      const copyBox=document.querySelector(`#${id} .feature-copy`);
      const html=`<h3 class="ct-feature-only-title">${title}</h3>`;
      if(copyBox&&copyBox.innerHTML!==html)copyBox.innerHTML=html;
    });

    const connectHeading=document.querySelector('#ct-pagero-intro .ct-pagero-connect h2');
    const connectHeadingHtml='페이지로에서 문의를 받으면<br><span>관리는 콜태그 앱에서!</span>';
    if(connectHeading&&connectHeading.innerHTML!==connectHeadingHtml)connectHeading.innerHTML=connectHeadingHtml;

    const tagTitle=document.querySelector('.ct-journey-clean .ct-horizontal-clean__panel:nth-child(3) .ct-horizontal-clean__copy h2');
    if(tagTitle&&tagTitle.innerHTML!=='전화가 끝나면<br>태그만 하세요')tagTitle.innerHTML='전화가 끝나면<br>태그만 하세요';

    document.querySelectorAll('.ct-horizontal-industries-clean .ct-horizontal-clean__copy,.ct-industries-static .ct-horizontal-clean__copy').forEach(element=>element.remove());
  };

  const apply=()=>{
    const main=document.querySelector('main#top');
    const app=document.querySelector('#app');
    if(!main||!app)return false;

    installBaseStyle();
    installBrand();

    if(main.firstElementChild!==app)main.prepend(app);
    let cursor=app;

    const story=findCallStory();
    if(story){
      cursor=moveAfter(cursor,story);
      story.dataset.ctPinnedAfterApp='1';
    }

    const intro=document.querySelector('#ct-pagero-intro');
    const benefits=document.querySelector('.ct-benefit-section')||document.querySelector('.ad-benefits')?.closest('.ad-section');
    const calltagSections=unique([
      document.querySelector('#what'),
      document.querySelector('#tasks'),
      document.querySelector('#messages'),
      document.querySelector('#web'),
      benefits,
      document.querySelector('#targets'),
      document.querySelector('#strengths')
    ]).filter(section=>section!==story&&section!==app&&section!==intro);

    calltagSections.forEach(section=>{cursor=moveAfter(cursor,section);});

    if(intro){
      cursor=moveAfter(cursor,intro);

      const pageroOnly=intro.querySelector('.ct-horizontal-industries-clean')||intro.querySelector('.ct-v8-nocode');
      const pageroBrandIntro=intro.querySelector('.ct-pagero-brand-intro');
      const integrationHero=intro.querySelector('.ct-v8-hero');
      const connect=intro.querySelector('.ct-pagero-connect');
      const integrationJourney=intro.querySelector('.ct-journey-clean')||document.querySelector('.ct-journey-clean');

      let introCursor=null;
      unique([pageroOnly,pageroBrandIntro,integrationHero,connect,integrationJourney]).forEach(section=>{
        if(!introCursor){
          if(intro.firstElementChild!==section)intro.prepend(section);
          introCursor=section;
        }else{
          introCursor=moveAfter(introCursor,section);
        }
      });

      cursor=intro;
      unique([
        document.querySelector('#pricing'),
        document.querySelector('#faq'),
        document.querySelector('.ad-final')
      ]).filter(section=>section!==intro).forEach(section=>{cursor=moveAfter(cursor,section);});

      document.documentElement.classList.add('ct-layout-ready');
    }

    applyFixedCopy();
    return true;
  };

  const boot=()=>{
    let scheduled=false;
    const schedule=()=>{
      if(scheduled)return;
      scheduled=true;
      requestAnimationFrame(()=>{
        scheduled=false;
        apply();
      });
    };

    apply();
    const observer=new MutationObserver(schedule);
    observer.observe(document.documentElement,{childList:true,subtree:true});
    [50,150,400,900,1800,3500,7000,12000].forEach(delay=>setTimeout(apply,delay));
    setTimeout(()=>{
      apply();
      observer.disconnect();
    },15000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
