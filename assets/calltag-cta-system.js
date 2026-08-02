(()=>{
  if(document.documentElement.dataset.ctCtaSystem)return;
  document.documentElement.dataset.ctCtaSystem='1';

  const APP='https://pagero.kr/app';
  const plans={
    CALL:{key:'call',label:'전화관리 7일 무료체험'},
    MESSAGE:{key:'message',label:'문자자동화 7일 무료체험'},
    PAGERO:{key:'pagero',label:'페이지로 시작하기'},
    'ALL IN ONE':{key:'all',label:'통합권 7일 무료체험'}
  };

  const makeUrl=(plan='all',position='site')=>{
    const url=new URL(APP);
    url.searchParams.set('source','calltag');
    url.searchParams.set('plan',plan);
    url.searchParams.set('utm_source','calltag_site');
    url.searchParams.set('utm_medium','cta');
    url.searchParams.set('utm_campaign','2026_launch');
    url.searchParams.set('utm_content',position);
    return url.toString();
  };

  const configureLink=(link,plan,position,label)=>{
    if(!link)return;
    link.href=makeUrl(plan,position);
    link.target='_blank';
    link.rel='noopener';
    link.dataset.ctUnifiedCta='1';
    if(label)link.textContent=label;
    link.setAttribute('aria-label',`${label||'콜태그 시작'} · 새 창`);
  };

  const addPricingButtons=()=>{
    document.querySelectorAll('#pricing .ct-plan-card').forEach(card=>{
      if(card.querySelector('.ct-plan-cta'))return;
      const code=card.querySelector(':scope>small')?.textContent.trim().toUpperCase()||'';
      const config=plans[code]||plans['ALL IN ONE'];
      const link=document.createElement('a');
      link.className='ct-plan-cta';
      if(card.classList.contains('all'))link.classList.add('primary');
      configureLink(link,config.key,`pricing_${config.key}`,config.label);
      card.append(link);
    });
  };

  const unifyExistingLinks=()=>{
    document.querySelectorAll('.hero-heading .ad-btn.primary,.ad-final .ad-btn.primary,.ad-sticky a,.price-button').forEach((link,index)=>{
      const position=link.closest('.ad-sticky')?'sticky':link.closest('.ad-final')?'final':link.closest('.hero-heading')?'hero':`legacy_${index+1}`;
      configureLink(link,'all',position,position==='sticky'?'7일 무료체험 시작':'7일 무료로 시작하기');
    });
    document.querySelectorAll('#pricing .ad-price a').forEach((link,index)=>{
      const plan=['call','message','all'][index]||'all';
      configureLink(link,plan,`legacy_pricing_${plan}`);
    });
  };

  const installStyle=()=>{
    if(document.querySelector('style[data-ct-cta-system]'))return;
    const style=document.createElement('style');
    style.dataset.ctCtaSystem='1';
    style.textContent=`
      #pricing .ct-plan-card{display:flex!important;flex-direction:column!important}
      #pricing .ct-plan-points{margin-bottom:24px!important}
      .ct-plan-cta{min-height:50px;display:flex;align-items:center;justify-content:center;margin-top:auto;padding:0 15px;border:1px solid rgba(124,153,255,.42);border-radius:12px;background:rgba(59,111,255,.09);color:#eef1ff;font-size:13px;font-weight:900;text-align:center;transition:transform .18s ease,border-color .18s ease,background .18s ease}
      .ct-plan-cta:hover,.ct-plan-cta:focus-visible{border-color:#7595ff;background:rgba(59,111,255,.2);transform:translateY(-2px);outline:none}
      .ct-plan-cta.primary{border-color:#3b6fff;background:#3b6fff;color:#fff;box-shadow:0 15px 36px rgba(59,111,255,.24)}
      .ct-plan-cta.primary:hover,.ct-plan-cta.primary:focus-visible{background:#527dff}
      @media(max-width:650px){.ct-plan-cta{min-height:48px;font-size:13px}.ct-plan-points{margin-bottom:20px!important}}
    `;
    document.head.append(style);
  };

  const apply=()=>{
    installStyle();
    addPricingButtons();
    unifyExistingLinks();
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(apply),{once:true});
  else requestAnimationFrame(apply);

  const observer=new MutationObserver(()=>requestAnimationFrame(apply));
  observer.observe(document.documentElement,{childList:true,subtree:true});
  setTimeout(()=>observer.disconnect(),12000);
})();
