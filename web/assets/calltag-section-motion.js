(()=>{
  if(document.documentElement.dataset.ctSectionMotion)return;
  document.documentElement.dataset.ctSectionMotion='1';

  const reduce=matchMedia('(prefers-reduced-motion: reduce)').matches;
  const style=document.createElement('style');
  style.dataset.ctSectionMotion='1';
  style.textContent=`
    html.ct-section-motion [data-ct-reveal]{opacity:0;translate:0 34px;transition:opacity .72s cubic-bezier(.22,1,.36,1),translate .82s cubic-bezier(.22,1,.36,1);transition-delay:var(--ct-delay,0ms)}
    html.ct-section-motion [data-ct-reveal].is-visible{opacity:1;translate:0 0}
    html.ct-section-motion [data-ct-reveal="heading"]{clip-path:inset(0 0 24% 0);translate:0 24px;transition:opacity .72s cubic-bezier(.22,1,.36,1),translate .82s cubic-bezier(.22,1,.36,1),clip-path .82s cubic-bezier(.22,1,.36,1)}
    html.ct-section-motion [data-ct-reveal="heading"].is-visible{clip-path:inset(0 0 0 0)}
    html.ct-section-motion .ct-motion-card{transition:translate .35s cubic-bezier(.22,1,.36,1),border-color .35s ease,box-shadow .35s ease}
    @media(hover:hover) and (pointer:fine){html.ct-section-motion .ct-motion-card:hover{translate:0 -6px;box-shadow:0 22px 54px rgba(0,0,0,.16)}}
    @media(min-width:901px) and (prefers-reduced-motion:no-preference){
      html.ct-section-motion .phone-stage .phone-shell,html.ct-section-motion #ct-pagero-intro .ct-v8-phone{animation:ctSoftFloat 6.8s ease-in-out infinite alternate}
      html.ct-section-motion #ct-pagero-intro .ct-v8-inquiry{animation:ctSoftFloatReverse 7.4s ease-in-out infinite alternate}
    }
    @keyframes ctSoftFloat{to{translate:0 -8px}}
    @keyframes ctSoftFloatReverse{to{translate:0 7px}}
    @media(max-width:900px){html.ct-section-motion [data-ct-reveal]{translate:0 22px;transition-duration:.62s}}
    @media(prefers-reduced-motion:reduce){html.ct-section-motion [data-ct-reveal]{opacity:1!important;translate:none!important;clip-path:none!important;transition:none!important}.phone-stage .phone-shell,#ct-pagero-intro .ct-v8-phone,#ct-pagero-intro .ct-v8-inquiry{animation:none!important}}
  `;
  document.head.append(style);
  document.documentElement.classList.add('ct-section-motion');

  const selectors=[
    '.hero-heading','.web-heading-copy','.phone-stage',
    '.section-title','.ad-title','.feature-copy h3','.ct-convert-head h2','.ct-auto-message-copy h2',
    '.feature-block','.product-panel','.ct-convert-stage','.ct-auto-message-layout','.ct-benefit-flow',
    '.ct-strength-grid > *','.ad-strengths > *','.ct-price-card','.ad-price','.faq-item','.fact',
    '.ct-suite-flow','.footer-inner'
  ];
  const headingSelector='.section-title,.ad-title,.feature-copy h3,.ct-convert-head h2,.ct-auto-message-copy h2';
  const cardSelector='.product-panel,.ct-strength-grid > *,.ad-strengths > *,.ct-price-card,.ad-price,.faq-item,.fact';
  const seen=new WeakSet();
  const observer=reduce?null:new IntersectionObserver(entries=>{
    entries.forEach(entry=>{
      if(!entry.isIntersecting)return;
      entry.target.classList.add('is-visible');
      observer.unobserve(entry.target);
    });
  },{threshold:.1,rootMargin:'0px 0px -8%'});

  const register=()=>{
    document.querySelectorAll(selectors.join(',')).forEach(element=>{
      if(seen.has(element)||element.closest('.ct-horizontal-clean'))return;
      seen.add(element);
      element.dataset.ctReveal=element.matches(headingSelector)?'heading':'item';
      if(element.matches(cardSelector))element.classList.add('ct-motion-card');
      const siblings=[...element.parentElement?.children||[]];
      const order=Math.max(0,siblings.indexOf(element));
      element.style.setProperty('--ct-delay',`${Math.min(order,4)*65}ms`);
      if(reduce)element.classList.add('is-visible');else observer.observe(element);
    });
  };

  let timer=0;
  const queue=()=>{
    clearTimeout(timer);
    timer=setTimeout(register,40);
  };
  register();
  const mutation=new MutationObserver(queue);
  mutation.observe(document.body,{childList:true,subtree:true});
  setTimeout(()=>{register();mutation.disconnect()},9000);
})();
