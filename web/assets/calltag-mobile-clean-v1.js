(()=>{
  if(document.documentElement.dataset.ctMobileCleanV1)return;
  document.documentElement.dataset.ctMobileCleanV1='1';

  const mobile=matchMedia('(max-width:900px)');
  const q=(s,r=document)=>r.querySelector(s);
  const qa=(s,r=document)=>[...r.querySelectorAll(s)];

  const style=document.createElement('style');
  style.dataset.ctMobileCleanV1='1';
  style.textContent=`
    @media(max-width:900px){
      html,body{width:100%!important;max-width:100%!important;overflow-x:hidden!important}
      body{min-width:0!important;padding-bottom:0!important}
      .wrap{width:calc(100% - 32px)!important;max-width:520px!important;margin-inline:auto!important}

      .header,.header-inner{height:64px!important;min-height:64px!important}
      .header-inner{justify-content:space-between!important;gap:12px!important}
      .nav{display:none!important}
      .logo{font-size:18px!important}.logo-mark{width:34px!important;height:34px!important}

      .section,.ad-section,.hero-web,.ct-convert-section,.ct-auto-message-section{padding:70px 0!important}
      .hero-app{padding:94px 0 72px!important}

      .hero-heading,.web-heading-copy,.section-head,.ad-head,.feature-copy,.ct-v8-head,.ct-v8-nocode-copy,.ct-horizontal-clean__copy,.ct-story-sticky,.ct-industry-v5__head,.ad-message-copy,.ct-convert-head,.ct-auto-message-copy{text-align:center!important}
      .hero-heading>p,.web-heading-copy p,.section-copy,.ad-copy,.feature-copy p,.ct-v8-head p,.ct-v8-nocode-copy p,.ct-horizontal-clean__copy p,.ad-message-copy p,.ct-auto-message-copy p{max-width:340px!important;margin-left:auto!important;margin-right:auto!important;font-size:14px!important;line-height:1.6!important}
      .hero h1,.hero-heading h1,.web-heading-copy h2,.section-title,.ad-title,.feature-copy h3,.ct-v8-head h1,.ct-v8-nocode-copy h2,.ct-horizontal-clean__copy h2,.ct-horizontal-clean__copy h3,.ct-story-sticky h2,.ct-industry-v5__head h2{max-width:100%!important;margin-left:auto!important;margin-right:auto!important;text-align:center!important;white-space:normal!important;text-wrap:balance!important;line-height:1.06!important;letter-spacing:-.065em!important}
      .hero h1,.hero-heading h1{font-size:clamp(36px,10vw,48px)!important}
      .section-title,.ad-title,.feature-copy h3,.web-heading-copy h2,.ct-v8-nocode-copy h2,.ct-horizontal-clean__copy h2,.ct-horizontal-clean__copy h3,.ct-story-sticky h2,.ct-industry-v5__head h2{font-size:clamp(32px,8.8vw,42px)!important}

      .phone-stage{display:flex!important;flex-direction:column!important;gap:26px!important;margin-top:36px!important;padding:16px!important;border-radius:22px!important;overflow:hidden!important}
      .phone-stage>.step-panel{order:0!important;width:100%!important;padding:4px 0 0!important;text-align:center!important}
      .phone-stage>.phone-shell{order:1!important;width:100%!important;max-width:360px!important;margin:0 auto!important}
      .stage-label{justify-content:center!important}.step-title,.step-sub{text-align:center!important;margin-left:auto!important;margin-right:auto!important}
      .step-title{font-size:clamp(32px,9vw,42px)!important}.step-sub{max-width:330px!important;font-size:14px!important;line-height:1.55!important}
      .step-list{display:flex!important;gap:10px!important;width:calc(100% + 16px)!important;margin:22px -8px 0!important;padding:0 8px 8px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:8px!important;scrollbar-width:none!important}
      .step-list::-webkit-scrollbar{display:none!important}
      #app .step-item,#app .step-item:nth-child(4){display:flex!important;flex:0 0 72vw!important;max-width:260px!important;min-height:108px!important;flex-direction:column!important;justify-content:center!important;align-items:center!important;gap:8px!important;padding:14px 12px!important;border-radius:16px!important;text-align:center!important;scroll-snap-align:center!important;transform:none!important}
      #app .step-item b{margin:0 auto!important}.progress-track{width:78px!important;margin:12px auto 0!important}

      .feature-block,.feature-block.reverse{display:flex!important;flex-direction:column!important;gap:26px!important;grid-template-columns:none!important;align-items:stretch!important}
      .feature-block>.feature-copy,.feature-block.reverse>.feature-copy{order:0!important;width:100%!important;padding:0 4px!important;text-align:center!important}
      .feature-block>.product-panel,.feature-block>.feature-visual,.feature-block.reverse>.product-panel,.feature-block.reverse>.feature-visual{order:1!important;width:100%!important;max-width:100%!important;margin:0 auto!important}
      .feature-block+.feature-block{margin-top:84px!important}
      .product-panel,.feature-visual,.ct-screen,.ct-auto-message-preview,.ct-convert-stage{border-radius:20px!important;overflow:hidden!important}

      .ct-story-layout{display:block!important}.ct-story-sticky{position:relative!important;top:auto!important;min-height:0!important;padding:68px 16px 24px!important}
      .ct-story-status,.ct-story-current{justify-content:center!important;margin-left:auto!important;margin-right:auto!important}
      .ct-story-steps{display:flex!important;gap:14px!important;width:100%!important;padding:0 14px 24px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:14px!important;scrollbar-width:none!important}
      .ct-story-steps::-webkit-scrollbar{display:none!important}
      .ct-story-step{flex:0 0 86vw!important;width:86vw!important;max-width:390px!important;min-height:0!important;padding:10px 0 18px!important;scroll-snap-align:center!important;opacity:1!important;transform:none!important}
      .ct-story-step h3{text-align:center!important}.ct-screen{width:100%!important;margin:0 auto!important}

      .ct-industry-v5{height:auto!important;min-height:0!important;padding:74px 0 80px!important;overflow:hidden!important}
      .ct-industry-v5__sticky{position:relative!important;top:auto!important;height:auto!important;min-height:0!important;display:block!important;padding:0!important;overflow:visible!important}
      .ct-industry-v5__head{width:calc(100% - 32px)!important;margin:0 auto 28px!important;text-align:center!important}
      .ct-industry-v5__stage{display:flex!important;gap:14px!important;width:100%!important;height:auto!important;min-height:0!important;padding:0 16px 18px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:16px!important;scrollbar-width:none!important}
      .ct-industry-v5__stage::-webkit-scrollbar{display:none!important}.ct-industry-v5__stage:before{display:none!important}
      .ct-industry-v5__card,.ct-industry-v5__card.xl,.ct-industry-v5__card.md,.ct-industry-v5__card.sm{position:relative!important;left:auto!important;top:auto!important;flex:0 0 82vw!important;width:82vw!important;max-width:330px!important;height:230px!important;scroll-snap-align:center!important;opacity:1!important;filter:none!important;transform:none!important}
      .ct-industry-v5__inner{border-radius:20px!important}.ct-industry-v5__meta{justify-content:center!important;gap:10px!important}.ct-industry-v5__meta strong{font-size:18px!important}

      #strengths .ad-strengths,.ct-strength-grid{display:flex!important;gap:12px!important;width:calc(100% + 16px)!important;margin-right:-16px!important;padding:0 16px 14px 0!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scrollbar-width:none!important}
      #strengths .ad-strengths::-webkit-scrollbar,.ct-strength-grid::-webkit-scrollbar{display:none!important}
      #strengths .ad-strength,.ct-strength-grid article{flex:0 0 82vw!important;width:82vw!important;max-width:330px!important;min-height:240px!important;scroll-snap-align:center!important;text-align:center!important}
      #strengths .ad-strength ul,.ct-strength-grid article ul{justify-content:center!important}

      .ad-price-grid,.ct-price-grid{display:flex!important;gap:12px!important;width:100%!important;padding:0 16px 18px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:16px!important;scrollbar-width:none!important}
      .ad-price-grid::-webkit-scrollbar,.ct-price-grid::-webkit-scrollbar{display:none!important}
      .ad-price,.ct-price-card{flex:0 0 86vw!important;width:86vw!important;max-width:350px!important;min-height:0!important;scroll-snap-align:center!important;text-align:center!important}
      .ad-price ul,.ct-price-card ul{text-align:left!important}.ad-sale,.ct-price-card .price{justify-content:center!important}.ad-discount,.ad-life{margin-left:auto!important;margin-right:auto!important}

      .ct-benefit-flow{display:grid!important;grid-template-columns:1fr!important;gap:10px!important;width:100%!important;padding:0!important;overflow:visible!important}
      .ct-benefit-flow .ad-benefit{width:100%!important;min-height:150px!important;text-align:center!important;opacity:1!important;transform:none!important}
      .ct-benefit-arrow{display:none!important}

      .ct-horizontal-clean{height:auto!important;min-height:0!important;overflow:hidden!important}
      .ct-horizontal-clean__sticky{position:relative!important;top:auto!important;height:auto!important;min-height:0!important;overflow:visible!important;padding:70px 0!important}
      .ct-horizontal-clean__track{display:flex!important;width:100%!important;height:auto!important;gap:14px!important;padding:0 16px 18px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:16px!important;scrollbar-width:none!important;transform:none!important}
      .ct-horizontal-clean__track::-webkit-scrollbar{display:none!important}
      .ct-horizontal-clean__panel{flex:0 0 88vw!important;width:88vw!important;max-width:410px!important;height:auto!important;min-height:560px!important;padding:34px 18px!important;scroll-snap-align:center!important;display:flex!important;flex-direction:column!important;justify-content:center!important}
      .ct-horizontal-clean__progress,.ct-horizontal-clean__top{display:none!important}

      .ad-message,.ct-auto-message-layout,.ct-convert-stage{grid-template-columns:1fr!important;gap:24px!important}
      .ad-checks{justify-items:center!important}.ct-auto-message-point,.ct-single-callout{justify-content:center!important}

      .scroll-top,.back-to-top,.to-top,.top-button,.scrollTop,#scrollTop,.ct-scroll-top,[data-scroll-top],[aria-label*="맨 위"],[aria-label*="위로"],[title*="맨 위"],[title*="위로"]{display:none!important;visibility:hidden!important;pointer-events:none!important}

      .ct-mobile-clean-enter{opacity:0!important;transform:translateY(26px)!important;transition:opacity .55s ease,transform .55s cubic-bezier(.2,.8,.2,1)!important}
      .ct-mobile-clean-enter.is-visible{opacity:1!important;transform:none!important}
    }
    @media(max-width:520px){
      .section,.ad-section,.hero-web,.ct-convert-section,.ct-auto-message-section{padding:62px 0!important}
      #app .step-item{flex-basis:76vw!important}
      .ct-story-step{flex-basis:90vw!important;width:90vw!important}
      .ct-industry-v5__card,.ct-industry-v5__card.xl,.ct-industry-v5__card.md,.ct-industry-v5__card.sm{flex-basis:84vw!important;width:84vw!important;height:220px!important}
      #strengths .ad-strength,.ct-strength-grid article{flex-basis:84vw!important;width:84vw!important}
      .ad-price,.ct-price-card{flex-basis:88vw!important;width:88vw!important}
      .ct-horizontal-clean__panel{flex-basis:90vw!important;width:90vw!important;min-height:540px!important}
    }
    @media(prefers-reduced-motion:reduce){.ct-mobile-clean-enter{opacity:1!important;transform:none!important;transition:none!important}}
  `;
  document.head.append(style);

  const reorder=()=>{
    if(!mobile.matches)return;
    qa('.phone-stage').forEach(stage=>{
      const copy=q(':scope>.step-panel',stage);
      const visual=q(':scope>.phone-shell',stage);
      if(copy&&visual&&stage.firstElementChild!==copy)stage.insertBefore(copy,visual);
    });
    qa('.feature-block').forEach(block=>{
      const copy=q(':scope>.feature-copy',block);
      const visual=q(':scope>.product-panel,:scope>.feature-visual',block);
      if(copy&&visual&&block.firstElementChild!==copy)block.insertBefore(copy,visual);
    });
  };

  const removeTopButtons=()=>{
    qa('.scroll-top,.back-to-top,.to-top,.top-button,.scrollTop,#scrollTop,.ct-scroll-top,[data-scroll-top],[aria-label*="맨 위"],[aria-label*="위로"],[title*="맨 위"],[title*="위로"]').forEach(node=>node.remove());
    if(!mobile.matches)return;
    qa('button,a').forEach(node=>{
      const cs=getComputedStyle(node);
      if(cs.position!=='fixed')return;
      const rect=node.getBoundingClientRect();
      if(rect.width<38||rect.width>92||rect.height<38||rect.height>92)return;
      const center=Math.abs(rect.left+rect.width/2-innerWidth/2);
      const bottom=innerHeight-rect.bottom;
      if(center>90||bottom<0||bottom>130)return;
      const radius=parseFloat(cs.borderRadius)||0;
      const text=(node.textContent||'').replace(/\s+/g,'').trim();
      const label=`${node.className||''} ${node.id||''} ${node.getAttribute('aria-label')||''} ${node.getAttribute('title')||''}`.toLowerCase();
      if(radius>=rect.width*.35&&(/top|up|scroll|위로|맨위/.test(label)||['↑','▲','⌃','^',''].includes(text)))node.remove();
    });
  };

  let io=null;
  const installReveal=()=>{
    if(!mobile.matches||matchMedia('(prefers-reduced-motion:reduce)').matches)return;
    if(!io)io=new IntersectionObserver(entries=>entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('is-visible');io.unobserve(entry.target)}}),{threshold:.12,rootMargin:'0px 0px -8%'});
    qa('.feature-block,.ct-story-step,.ct-industry-v5__card,#strengths .ad-strength,.ct-strength-grid article,.ad-price,.ct-price-card').forEach(node=>{
      if(node.dataset.ctMobileCleanReveal==='1')return;
      node.dataset.ctMobileCleanReveal='1';
      node.classList.add('ct-mobile-clean-enter');
      io.observe(node);
    });
  };

  let queued=false;
  const apply=()=>{queued=false;reorder();removeTopButtons();installReveal()};
  const queue=()=>{if(queued)return;queued=true;requestAnimationFrame(apply)};
  const observer=new MutationObserver(queue);
  observer.observe(document.documentElement,{childList:true,subtree:true});
  mobile.addEventListener?.('change',queue);
  addEventListener('resize',queue,{passive:true});

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});
  else apply();
  [200,700,1500,3000,6000].forEach(delay=>setTimeout(apply,delay));
})();
