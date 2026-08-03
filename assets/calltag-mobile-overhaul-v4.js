(()=>{
  if(document.documentElement.dataset.ctMobileOverhaulV4)return;
  document.documentElement.dataset.ctMobileOverhaulV4='1';

  const mobile=matchMedia('(max-width:900px)');
  const q=(s,r=document)=>r.querySelector(s);
  const qa=(s,r=document)=>[...r.querySelectorAll(s)];

  const style=document.createElement('style');
  style.dataset.ctMobileOverhaulV4='1';
  style.textContent=`
    @media(max-width:900px){
      html,body{width:100%!important;max-width:100%!important;overflow-x:hidden!important}
      body{min-width:0!important;padding-bottom:0!important}
      .wrap{width:calc(100% - 28px)!important;max-width:none!important;margin-inline:auto!important}
      .section,.ad-section,.hero-web,.ct-convert-section,.ct-auto-message-section{padding-top:76px!important;padding-bottom:76px!important}

      .hero-heading,.web-heading-copy,.section-head,.ad-head,.feature-copy,.ct-v8-head,.ct-v8-nocode-copy,.ct-horizontal-clean__copy,.ct-story-sticky,.ct-industry-v5__head,.ad-message-copy{text-align:center!important}
      .hero-heading>p,.web-heading-copy p,.section-copy,.ad-copy,.feature-copy p,.ct-v8-head p,.ct-v8-nocode-copy p,.ct-horizontal-clean__copy p,.ad-message-copy p{margin-left:auto!important;margin-right:auto!important;max-width:340px!important}
      .hero h1,.hero-heading h1,.web-heading-copy h2,.section-title,.ad-title,.feature-copy h3,.ct-v8-head h1,.ct-v8-nocode-copy h2,.ct-horizontal-clean__copy h2,.ct-horizontal-clean__copy h3,.ct-story-sticky h2,.ct-industry-v5__head h2{max-width:100%!important;margin-left:auto!important;margin-right:auto!important;text-align:center!important;white-space:normal!important;text-wrap:balance!important}

      .phone-stage{display:flex!important;flex-direction:column!important;gap:28px!important;margin-top:38px!important;padding:16px!important;border-radius:22px!important;overflow:hidden!important}
      .phone-stage>.step-panel{order:0!important;width:100%!important;padding:6px 0 0!important;text-align:center!important}
      .phone-stage>.phone-shell{order:1!important;width:100%!important;max-width:360px!important;margin:0 auto!important}
      .stage-label{justify-content:center!important}.step-title,.step-sub{text-align:center!important;margin-left:auto!important;margin-right:auto!important}
      .step-title{font-size:clamp(34px,9.4vw,44px)!important;line-height:1.05!important}.step-sub{max-width:330px!important;font-size:14px!important;line-height:1.55!important}
      .step-list{display:flex!important;grid-template-columns:none!important;gap:10px!important;width:calc(100% + 16px)!important;margin:24px -8px 0!important;padding:0 8px 8px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:8px!important;overscroll-behavior-x:contain!important;scrollbar-width:none!important}
      .step-list::-webkit-scrollbar{display:none!important}
      #app .step-item,#app .step-item:nth-child(4){display:flex!important;flex:0 0 min(74vw,270px)!important;min-height:112px!important;flex-direction:column!important;justify-content:center!important;align-items:center!important;gap:9px!important;padding:15px 12px!important;border-radius:17px!important;text-align:center!important;scroll-snap-align:center!important;transform:none!important}
      #app .step-item b{margin:0 auto!important}.progress-track{width:84px!important;height:4px!important;margin:13px auto 0!important}

      .feature-block,.feature-block.reverse{display:flex!important;flex-direction:column!important;grid-template-columns:none!important;gap:28px!important;align-items:stretch!important}
      .feature-block>.feature-copy,.feature-block.reverse>.feature-copy{order:0!important;width:100%!important;padding:0 4px!important;text-align:center!important}
      .feature-block>.product-panel,.feature-block>.feature-visual,.feature-block.reverse>.product-panel,.feature-block.reverse>.feature-visual{order:1!important;width:100%!important;max-width:100%!important;margin:0 auto!important}
      .feature-block+.feature-block{margin-top:92px!important}

      .ct-story-layout{display:block!important}.ct-story-sticky{position:relative!important;top:auto!important;min-height:0!important;padding:70px 14px 24px!important}.ct-story-status,.ct-story-current{justify-content:center!important;margin-left:auto!important;margin-right:auto!important}
      .ct-story-steps{display:flex!important;gap:14px!important;width:100%!important;padding:0 14px 26px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:14px!important;scrollbar-width:none!important}
      .ct-story-steps::-webkit-scrollbar{display:none!important}.ct-story-step{flex:0 0 88vw!important;width:88vw!important;min-height:0!important;padding:12px 0 18px!important;scroll-snap-align:center!important;opacity:1!important;transform:none!important}.ct-story-step h3{text-align:center!important}.ct-screen{width:100%!important;max-width:100%!important;margin:0 auto!important}

      .ct-industry-v5{padding:78px 0 84px!important}.ct-industry-v5__head{width:calc(100% - 28px)!important;margin:0 auto 28px!important}.ct-industry-v5__head h2{font-size:clamp(34px,9vw,42px)!important;text-align:center!important}
      .ct-industry-v5__stage{display:flex!important;grid-template-columns:none!important;gap:14px!important;width:100%!important;padding:0 14px 20px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:14px!important;overscroll-behavior-x:contain!important;scrollbar-width:none!important}
      .ct-industry-v5__stage::-webkit-scrollbar{display:none!important}.ct-industry-v5__stage:before{display:none!important}
      .ct-industry-v5__card,.ct-industry-v5__card.xl,.ct-industry-v5__card.md,.ct-industry-v5__card.sm{flex:0 0 82vw!important;width:82vw!important;max-width:330px!important;height:230px!important;scroll-snap-align:center!important}
      .ct-industry-v5__inner{border-radius:21px!important}.ct-industry-v5__meta{justify-content:center!important;gap:10px!important}.ct-industry-v5__meta strong{font-size:18px!important}

      #strengths .ad-head,#pricing .ad-head,#targets .ad-head{margin-bottom:30px!important;text-align:center!important}
      #strengths .ad-strengths,.ct-strength-grid{display:flex!important;grid-template-columns:none!important;gap:12px!important;width:calc(100% + 14px)!important;margin-right:-14px!important;padding:0 14px 16px 0!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:0!important;scrollbar-width:none!important}
      #strengths .ad-strengths::-webkit-scrollbar,.ct-strength-grid::-webkit-scrollbar{display:none!important}
      #strengths .ad-strength,.ct-strength-grid article{flex:0 0 82vw!important;width:82vw!important;max-width:330px!important;min-height:250px!important;scroll-snap-align:center!important;text-align:center!important}
      #strengths .ad-strength ul,.ct-strength-grid article ul{justify-content:center!important}

      .ct-benefit-flow{display:flex!important;grid-template-columns:none!important;gap:12px!important;width:calc(100% + 14px)!important;margin-right:-14px!important;padding:0 14px 14px 0!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scrollbar-width:none!important}
      .ct-benefit-flow::-webkit-scrollbar{display:none!important}.ct-benefit-flow .ad-benefit{flex:0 0 78vw!important;width:78vw!important;max-width:310px!important;min-height:190px!important;scroll-snap-align:center!important;text-align:center!important;opacity:1!important;transform:none!important}.ct-benefit-arrow{display:none!important}

      .ad-price-grid,.ct-price-grid{display:flex!important;grid-template-columns:none!important;gap:12px!important;width:100%!important;padding:0 14px 18px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:14px!important;scrollbar-width:none!important}
      .ad-price-grid::-webkit-scrollbar,.ct-price-grid::-webkit-scrollbar{display:none!important}.ad-price,.ct-price-card{flex:0 0 86vw!important;width:86vw!important;max-width:350px!important;scroll-snap-align:center!important;text-align:center!important}.ad-price ul,.ct-price-card ul{text-align:left!important}.ad-sale,.ct-price-card .price{justify-content:center!important}.ad-discount,.ad-life{margin-left:auto!important;margin-right:auto!important}

      .ct-horizontal-clean{height:auto!important;min-height:0!important;overflow:hidden!important}.ct-horizontal-clean__sticky{position:relative!important;top:auto!important;height:auto!important;min-height:0!important;overflow:visible!important;padding:72px 0!important}.ct-horizontal-clean__track{display:flex!important;width:100%!important;height:auto!important;gap:14px!important;padding:0 14px 18px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:14px!important;scrollbar-width:none!important;transform:none!important}.ct-horizontal-clean__track::-webkit-scrollbar{display:none!important}.ct-horizontal-clean__panel{flex:0 0 88vw!important;width:88vw!important;height:auto!important;min-height:600px!important;padding:36px 18px!important;scroll-snap-align:center!important;display:flex!important;flex-direction:column!important;justify-content:center!important}.ct-horizontal-clean__copy{text-align:center!important}.ct-horizontal-clean__progress,.ct-horizontal-clean__top{display:none!important}

      .ad-message,.ct-auto-message-layout,.ct-convert-stage{grid-template-columns:1fr!important}.ad-message-copy,.ct-auto-message-copy,.ct-convert-head{text-align:center!important}.ad-checks{justify-items:center!important}.ct-auto-message-point,.ct-single-callout{justify-content:center!important}

      .scroll-top,.back-to-top,.to-top,.top-button,.scrollTop,#scrollTop,.ct-scroll-top,[data-scroll-top],[aria-label*="맨 위"],[aria-label*="위로"],[title*="맨 위"],[title*="위로"]{display:none!important;visibility:hidden!important;pointer-events:none!important}
    }
    @media(max-width:520px){
      .section,.ad-section,.hero-web,.ct-convert-section,.ct-auto-message-section{padding-top:66px!important;padding-bottom:66px!important}
      #app .step-item{flex-basis:76vw!important}.ct-industry-v5__card,.ct-industry-v5__card.xl,.ct-industry-v5__card.md,.ct-industry-v5__card.sm{flex-basis:84vw!important;width:84vw!important;height:220px!important}
      #strengths .ad-strength,.ct-strength-grid article{flex-basis:84vw!important;width:84vw!important}.ct-benefit-flow .ad-benefit{flex-basis:82vw!important;width:82vw!important}.ad-price,.ct-price-card{flex-basis:88vw!important;width:88vw!important}.ct-horizontal-clean__panel{flex-basis:90vw!important;width:90vw!important;min-height:560px!important}
    }
  `;
  document.head.append(style);

  const markSlider=(selector)=>qa(selector).forEach(node=>node.classList.add('ct-mobile-snap'));

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
    markSlider('.step-list,.ct-story-steps,.ct-industry-v5__stage,#strengths .ad-strengths,.ct-strength-grid,.ct-benefit-flow,.ad-price-grid,.ct-price-grid,.ct-horizontal-clean__track');
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
      if(center>80||bottom<0||bottom>120)return;
      const radius=parseFloat(cs.borderRadius)||0;
      const text=(node.textContent||'').replace(/\s+/g,'').trim();
      const label=`${node.className||''} ${node.id||''} ${node.getAttribute('aria-label')||''} ${node.getAttribute('title')||''}`.toLowerCase();
      const looksTop=/top|up|scroll|위로|맨위/.test(label)||['↑','▲','⌃','^',''].includes(text);
      if(radius>=rect.width*.35&&looksTop)node.remove();
    });
  };

  let queued=false;
  const apply=()=>{
    queued=false;
    reorder();
    removeTopButtons();
  };
  const queue=()=>{
    if(queued)return;
    queued=true;
    requestAnimationFrame(apply);
  };

  const observer=new MutationObserver(queue);
  observer.observe(document.documentElement,{childList:true,subtree:true});
  mobile.addEventListener?.('change',queue);
  addEventListener('resize',queue,{passive:true});
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});
  else apply();
  [200,700,1500,3000,6000].forEach(delay=>setTimeout(apply,delay));
})();
