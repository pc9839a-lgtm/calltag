(()=>{
  if(document.documentElement.dataset.ctSectionOrderV3)return;
  document.documentElement.dataset.ctSectionOrderV3='1';

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

  const moveAfter=(anchor,node)=>{
    if(!anchor||!node||anchor===node)return anchor;
    if(anchor.nextElementSibling!==node)anchor.insertAdjacentElement('afterend',node);
    return node;
  };

  const apply=()=>{
    const main=document.querySelector('main#top');
    const intro=document.querySelector('#ct-pagero-intro');
    const app=document.querySelector('#app');
    if(!main||!intro||!app)return false;

    installBrand();
    if(main.firstElementChild!==intro)main.prepend(intro);

    const pageroHero=intro.querySelector('.ct-v8-hero');
    const connect=intro.querySelector('.ct-pagero-connect');
    const nocode=intro.querySelector('.ct-v8-nocode');

    let introCursor=null;
    [pageroHero,connect,nocode].filter(Boolean).forEach(section=>{
      if(!introCursor){
        if(intro.firstElementChild!==section)intro.prepend(section);
        introCursor=section;
      }else{
        introCursor=moveAfter(introCursor,section);
      }
    });

    if(introCursor)moveAfter(introCursor,app);
    else intro.append(app);

    const benefits=document.querySelector('.ct-benefit-section')||document.querySelector('.ad-benefits')?.closest('.ad-section');
    const ordered=[
      document.querySelector('#what'),
      document.querySelector('#tasks'),
      document.querySelector('#messages'),
      document.querySelector('#web'),
      benefits,
      document.querySelector('#targets'),
      document.querySelector('#strengths'),
      document.querySelector('#pricing'),
      document.querySelector('#faq'),
      document.querySelector('.ad-final')
    ].filter(Boolean);

    let cursor=intro;
    ordered.forEach(section=>{cursor=moveAfter(cursor,section);});
    return installBrand();
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
    [50,120,250,500,900,1500,2500,4000,6500,10000,15000].forEach(delay=>setTimeout(apply,delay));
    setTimeout(()=>observer.disconnect(),20000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
