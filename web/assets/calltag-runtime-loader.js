(()=>{
  if(document.documentElement.dataset.ctRuntimeLoaderV2)return;
  document.documentElement.dataset.ctRuntimeLoaderV2='1';

  const scripts=[
    'calltag-enhance.js?v=20260801-37',
    'calltag-copy-fix.js?v=20260801-37',
    'calltag-section-split.js?v=20260801-37',
    'calltag-final-polish.js?v=20260801-37',
    'calltag-interaction-fix.js?v=20260801-37',
    'calltag-benefits-flow.js?v=20260804-benefits2',
    'calltag-message-simple.js?v=20260801-37',
    'calltag-final-fix.js?v=20260801-37',
    'calltag-suite-pricing.js?v=20260801-37',
    'calltag-steady-slider.js?v=20260801-37',
    'calltag-strength-animation.js?v=20260801-37',
    'calltag-pricing-redesign.js?v=20260801-37',
    'calltag-pagero-intro.js?v=20260803-intro11',
    'calltag-pagero-reveal-fix.js?v=20260802-reveal3',
    'calltag-pagero-heading-fix.js?v=20260803-heading1',
    'calltag-pagero-connect-visual.js?v=20260802-connect1',
    'calltag-pagero-start-cta.js?v=20260805-cta3',
    'calltag-pagero-industries.js?v=20260802-industries2',
    'calltag-pagero-industries-two-column.js?v=20260802-three-card1',
    'calltag-product-switcher.js?v=20260801-37',
    'calltag-cta-system.js?v=20260804-gp1',
    'calltag-stability-fix.js?v=20260802-stability1',
    'calltag-horizontal-clean.js?v=20260803-compact1',
    'calltag-horizontal-guard.js?v=20260803-pin4',
    'calltag-industry-visual-v5.js?v=20260803-v5',
    'calltag-horizontal-impact.js?v=20260803-impact2',
    'calltag-feature-copy-exact.js?v=20260803-copy3',
    'calltag-section-motion.js?v=20260803-motion2',
    'calltag-site-final-cleanup.js?v=20260804-footer2',
    'calltag-horizontal-live-fix.js?v=20260803-live1',
    'calltag-mobile-clean-v2.js?v=20260804-clean3',
    'calltag-mobile-history-fix.js?v=20260804-history1',
    'calltag-footer-links-v3.js?v=20260804-links5',
    'calltag-section-order.js?v=20260805-order9',
    'calltag-seo-runtime.js?v=20260805-seo1',
    'calltag-copy-hard-fix.js?v=20260803-hard1',
    'calltag-story-order-hard-fix.js?v=20260805-pin1',
    'calltag-pagero-light-chapter.js?v=20260805-light2'
  ];

  const mount=()=>{
    const fragment=document.createDocumentFragment();
    let remaining=scripts.length;
    const done=()=>{
      remaining-=1;
      if(remaining<=0)document.documentElement.classList.add('ct-layout-ready');
    };
    scripts.forEach(src=>{
      const script=document.createElement('script');
      script.src=`/assets/${src}`;
      script.async=false;
      script.onload=done;
      script.onerror=()=>{
        console.error('[CallTag] runtime asset failed:',src);
        done();
      };
      fragment.appendChild(script);
    });
    document.body.appendChild(fragment);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});
  else mount();
})();
