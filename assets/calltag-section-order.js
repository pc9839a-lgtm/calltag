(()=>{
  if(document.documentElement.dataset.ctSectionOrderCalltagFirstV2)return;
  document.documentElement.dataset.ctSectionOrderCalltagFirstV2='1';

  const installBrand=()=>{
    const heading=document.querySelector('#app .hero-heading');
    const kicker=heading?.querySelector('.hero-kicker');
    if(!heading||!kicker)return false;

    let brand=heading.querySelector(':scope > .ct-calltag-brand');
    if(!brand){
      brand=document.createElement('p');
      brand.className='ct-calltag-brand';
      brand.textContent='콜태그';
      heading.insertBefore(brand,kicker);
    }

    if(!document.querySelector('style[data-ct-calltag-brand]')){
      const style=document.createElement('style');
      style.dataset.ctCalltagBrand='1';
      style.textContent=`
        #app .ct-calltag-brand{margin:0 0 10px;color:var(--blue-2);font-size:18px;font-weight:950;letter-spacing:.12em;text-align:center}
        @media(max-width:640px){#app .ct-calltag-brand{margin-bottom:8px;font-size:15px}}
      `;
      document.head.append(style);
    }
    return true;
  };

  const unique=nodes=>[...new Set(nodes.filter(Boolean))];

  const moveAfter=(anchor,node)=>{
    if(!anchor||!node||anchor===node)return anchor;
    if(anchor.nextElementSibling!==node)anchor.insertAdjacentElement('afterend',node);
    return node;
  };

  const findCallStory=()=>document.querySelector('#how.ct-story-section')||
    [...document.querySelectorAll('.ct-story-section')].find(section=>{
      const text=(section.textContent||'').replace(/\s+/g,' ').trim();
      return text.includes('통화가 끝나면')&&text.includes('태그만 하세요');
    })||null;

  const apply=()=>{
    const main=document.querySelector('main#top');
    const app=document.querySelector('#app');
    const intro=document.querySelector('#ct-pagero-intro');
    if(!main||!app||!intro)return false;

    installBrand();

    if(main.firstElementChild!==app)main.prepend(app);
    let cursor=app;

    const story=findCallStory();
    if(story)cursor=moveAfter(cursor,story);

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

    const closing=unique([
      document.querySelector('#pricing'),
      document.querySelector('#faq'),
      document.querySelector('.ad-final')
    ]).filter(section=>section!==intro);

    cursor=intro;
    closing.forEach(section=>{cursor=moveAfter(cursor,section);});

    document.documentElement.classList.add('ct-layout-ready');
    return true;
  };

  const boot=()=>{
    let scheduled=false;
    const schedule=()=>{
      if(scheduled)return;
      scheduled=true;
      requestAnimationFrame(()=>{scheduled=false;apply();});
    };

    apply();
    const observer=new MutationObserver(schedule);
    observer.observe(document.documentElement,{childList:true,subtree:true});
    [20,50,100,180,320,550,900,1400,2200,3500,5500,8000,12000,18000,25000].forEach(delay=>setTimeout(apply,delay));
    setTimeout(()=>observer.disconnect(),30000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
