(()=>{
  if(document.documentElement.dataset.ctMobileCleanV2)return;
  document.documentElement.dataset.ctMobileCleanV2='1';

  const mobile=matchMedia('(max-width:900px)');
  const qa=(selector,root=document)=>[...root.querySelectorAll(selector)];

  const style=document.createElement('style');
  style.dataset.ctMobileCleanV2='1';
  style.textContent=`
    @media(max-width:900px){
      html,body{width:100%!important;max-width:100%!important;overflow-x:hidden!important}
      body{min-width:0!important;padding-bottom:0!important}
      .wrap{width:calc(100% - 32px)!important;max-width:520px!important;margin-inline:auto!important}
      section,.section,.ad-section,.hero,.hero-web,.hero-app,.ct-story-section,.ct-convert-section{min-height:0!important}
      .section,.ad-section,.hero-web,.ct-convert-section,.ct-auto-message-section{padding:62px 0!important}
      .hero-app{padding:92px 0 66px!important}
      .hero-heading,.web-heading-copy,.section-head,.ad-head,.feature-copy,.ct-v8-head,.ct-v8-nocode-copy,.ct-horizontal-clean__copy,.ct-industry-v5__head,.ad-message-copy,.ct-convert-head,.ct-auto-message-copy{text-align:center!important}
      .hero-heading>p,.web-heading-copy p,.section-copy,.ad-copy,.feature-copy p,.ct-v8-head p,.ct-v8-nocode-copy p,.ct-horizontal-clean__copy p,.ad-message-copy p,.ct-auto-message-copy p{max-width:330px!important;margin-left:auto!important;margin-right:auto!important;font-size:14px!important;line-height:1.55!important}
      .hero h1,.hero-heading h1,.web-heading-copy h2,.section-title,.ad-title,.feature-copy h3,.ct-v8-head h1,.ct-v8-nocode-copy h2,.ct-horizontal-clean__copy h2,.ct-horizontal-clean__copy h3,.ct-industry-v5__head h2{max-width:100%!important;margin-left:auto!important;margin-right:auto!important;text-align:center!important;white-space:normal!important;text-wrap:balance!important;line-height:1.05!important;letter-spacing:-.065em!important}
      .hero h1,.hero-heading h1{font-size:clamp(36px,10vw,47px)!important}
      .section-title,.ad-title,.feature-copy h3,.web-heading-copy h2,.ct-v8-nocode-copy h2,.ct-horizontal-clean__copy h2,.ct-horizontal-clean__copy h3,.ct-industry-v5__head h2{font-size:clamp(31px,8.7vw,41px)!important}
      .phone-stage{display:block!important;margin-top:30px!important;padding:0!important;border:0!important;border-radius:0!important;background:none!important;box-shadow:none!important;overflow:visible!important}
      .phone-stage>.step-panel,.phone-stage .step-list,.phone-stage .progress-track,.phone-stage .stage-label,.phone-stage .step-title,.phone-stage .step-sub{display:none!important}
      .phone-stage>.phone-shell{width:100%!important;max-width:420px!important;min-height:0!important;margin:0 auto!important;padding:0!important;border:0!important;border-radius:0!important;background:none!important;box-shadow:none!important}
      .phone-stage .phone-screen{min-height:0!important;overflow:visible!important;border-radius:0!important;background:none!important}.phone-stage .phone-status{display:none!important}
      .phone-stage .app-screen{position:relative!important;inset:auto!important;display:block!important;min-height:0!important;margin:0 0 14px!important;padding:20px 16px 22px!important;opacity:1!important;transform:none!important;filter:none!important;pointer-events:auto!important;border:1px solid rgba(255,255,255,.11)!important;border-radius:20px!important;background:#101218!important;box-shadow:none!important}
      .phone-stage .app-screen:last-child{margin-bottom:0!important}.phone-stage .app-title{font-size:24px!important;text-align:left!important}.phone-stage .app-sub{font-size:12px!important;margin-bottom:18px!important}.phone-stage .option{min-height:50px!important}.phone-stage .save-bar{min-height:52px!important}
      .ct-story-section{padding:62px 0!important;overflow:hidden!important}.ct-story-layout{display:block!important;width:100%!important}.ct-story-sticky{display:none!important}.ct-story-steps{display:block!important;width:100%!important;padding:0 16px!important;overflow:visible!important}.ct-story-step{display:block!important;width:100%!important;min-height:0!important;margin:0 0 18px!important;padding:0!important;opacity:1!important;transform:none!important;filter:none!important}.ct-story-step:last-child{margin-bottom:0!important}.ct-story-step .ct-step-label,.ct-story-step>h3{display:none!important}.ct-story-step .ct-screen{width:100%!important;min-height:0!important;margin:0!important;padding:18px!important;border-radius:20px!important;opacity:1!important;transform:none!important;filter:none!important;box-shadow:0 18px 50px rgba(0,0,0,.24)!important}
      .ct-screen-grid.three{grid-template-columns:1fr!important}.ct-screen-grid div{padding:14px!important}.ct-screen-task{display:grid!important;grid-template-columns:1fr!important;gap:10px!important}.ct-date-card{padding:20px!important}.ct-date-card strong{font-size:32px!important}.ct-message-tabs{grid-template-columns:repeat(4,minmax(0,1fr))!important;gap:5px!important}.ct-message-tabs span{padding:10px 4px!important;font-size:10px!important}.ct-web-body{grid-template-columns:72px minmax(0,1fr)!important}.ct-web-body aside{padding:12px 7px!important}.ct-web-body main{padding:14px!important}
      .feature-block,.feature-block.reverse{display:flex!important;flex-direction:column!important;gap:24px!important;grid-template-columns:none!important;align-items:stretch!important}.feature-block>.feature-copy,.feature-block.reverse>.feature-copy{order:0!important;width:100%!important;padding:0 4px!important;text-align:center!important}.feature-block>.product-panel,.feature-block>.feature-visual,.feature-block.reverse>.product-panel,.feature-block.reverse>.feature-visual{order:1!important;width:100%!important;max-width:420px!important;margin:0 auto!important}.feature-block+.feature-block{margin-top:66px!important}.product-panel,.feature-visual,.ct-screen,.ct-auto-message-preview,.ct-convert-stage{border-radius:19px!important;overflow:hidden!important}
      #strengths{padding:48px 0 62px!important;min-height:0!important}#strengths .ad-head{margin-bottom:24px!important}#strengths .ad-strengths,.ct-strength-grid{display:flex!important;gap:12px!important;width:calc(100% + 16px)!important;margin-right:-16px!important;padding:0 16px 12px 0!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scrollbar-width:none!important}#strengths .ad-strengths::-webkit-scrollbar,.ct-strength-grid::-webkit-scrollbar{display:none!important}#strengths .ad-strength,.ct-strength-grid article{flex:0 0 78vw!important;width:78vw!important;max-width:305px!important;min-height:205px!important;padding:28px 20px!important;scroll-snap-align:center!important;text-align:center!important}#strengths .ad-strength h3,.ct-strength-grid article h3{font-size:25px!important;margin-top:15px!important}#strengths .ad-strength p,.ct-strength-grid article p{font-size:12px!important;line-height:1.55!important}#strengths .ad-strength ul,.ct-strength-grid article ul{justify-content:center!important}
      .ct-industry-v5{height:auto!important;min-height:0!important;padding:64px 0 70px!important;overflow:hidden!important}.ct-industry-v5__sticky{position:relative!important;top:auto!important;height:auto!important;min-height:0!important;display:block!important;padding:0!important;overflow:visible!important}.ct-industry-v5__head{width:calc(100% - 32px)!important;margin:0 auto 24px!important;text-align:center!important}.ct-industry-v5__stage{display:flex!important;gap:12px!important;width:100%!important;height:auto!important;min-height:0!important;padding:0 16px 16px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:16px!important;scrollbar-width:none!important}.ct-industry-v5__stage::-webkit-scrollbar{display:none!important}.ct-industry-v5__stage:before{display:none!important}.ct-industry-v5__card,.ct-industry-v5__card.xl,.ct-industry-v5__card.md,.ct-industry-v5__card.sm{position:relative!important;left:auto!important;top:auto!important;flex:0 0 80vw!important;width:80vw!important;max-width:320px!important;height:218px!important;scroll-snap-align:center!important;opacity:1!important;filter:none!important;transform:none!important}.ct-industry-v5__inner{border-radius:19px!important}.ct-industry-v5__meta{justify-content:center!important;gap:9px!important}.ct-industry-v5__meta strong{font-size:17px!important}
      .ad-price-grid,.ct-price-grid{display:flex!important;gap:12px!important;width:100%!important;padding:0 16px 16px!important;overflow-x:auto!important;scroll-snap-type:x mandatory!important;scroll-padding-inline:16px!important;scrollbar-width:none!important}.ad-price-grid::-webkit-scrollbar,.ct-price-grid::-webkit-scrollbar{display:none!important}.ad-price,.ct-price-card{flex:0 0 86vw!important;width:86vw!important;max-width:350px!important;min-height:0!important;scroll-snap-align:center!important;text-align:center!important}.ad-price ul,.ct-price-card ul{text-align:left!important}.ad-sale,.ct-price-card .price{justify-content:center!important}
      .ct-benefit-flow{display:grid!important;grid-template-columns:1fr!important;gap:10px!important;width:100%!important;padding:0!important;overflow:visible!important}.ct-benefit-flow .ad-benefit{width:100%!important;min-height:138px!important;text-align:center!important;opacity:1!important;transform:none!important}.ct-benefit-arrow{display:none!important}
      .scroll-top,.back-to-top,.to-top,.top-button,.scrollTop,#scrollTop,.ct-scroll-top,[data-scroll-top],[aria-label*="맨 위"],[aria-label*="위로"],[title*="맨 위"],[title*="위로"]{display:none!important;visibility:hidden!important;pointer-events:none!important}
    }
    @media(max-width:520px){.section,.ad-section,.hero-web,.ct-convert-section,.ct-auto-message-section{padding:56px 0!important}.ct-story-section{padding:56px 0!important}#strengths .ad-strength,.ct-strength-grid article{flex-basis:82vw!important;width:82vw!important}.ct-industry-v5__card,.ct-industry-v5__card.xl,.ct-industry-v5__card.md,.ct-industry-v5__card.sm{flex-basis:84vw!important;width:84vw!important;height:214px!important}}
  `;
  document.head.append(style);

  const normalize=()=>{
    if(!mobile.matches)return;
    qa('.phone-stage .app-screen,.ct-story-step,.ct-story-step .ct-screen,.ct-industry-v5__card,#strengths .ad-strength,.ct-strength-grid article').forEach(node=>{node.style.removeProperty('opacity');node.style.removeProperty('transform');node.style.removeProperty('filter')});
    qa('.phone-stage .app-screen').forEach(node=>node.classList.add('active'));
  };

  const removeTopButtons=()=>{
    qa('.scroll-top,.back-to-top,.to-top,.top-button,.scrollTop,#scrollTop,.ct-scroll-top,[data-scroll-top],[aria-label*="맨 위"],[aria-label*="위로"],[title*="맨 위"],[title*="위로"]').forEach(node=>node.remove());
    if(!mobile.matches)return;
    qa('button,a,div').forEach(node=>{
      const cs=getComputedStyle(node);
      if(!['fixed','sticky','absolute'].includes(cs.position))return;
      const rect=node.getBoundingClientRect();
      if(rect.width<42||rect.width>100||rect.height<42||rect.height>100||Math.abs(rect.width-rect.height)>12)return;
      const center=Math.abs(rect.left+rect.width/2-innerWidth/2),bottom=innerHeight-rect.bottom;
      if(center>85||bottom<0||bottom>150)return;
      const radius=parseFloat(cs.borderRadius)||0;if(radius<rect.width*.35)return;
      const text=(node.textContent||'').replace(/\s+/g,'').trim();
      const label=`${node.className||''} ${node.id||''} ${node.getAttribute('aria-label')||''} ${node.getAttribute('title')||''}`.toLowerCase();
      const light=/rgb\(2[0-5][0-9],\s*2[0-5][0-9],\s*2[0-5][0-9]\)/.test(cs.backgroundColor);
      if(light||/top|up|scroll|위로|맨위/.test(label)||['↑','▲','⌃','^',''].includes(text))node.remove();
    });
  };

  let queued=false;const apply=()=>{queued=false;normalize();removeTopButtons()};const queue=()=>{if(queued)return;queued=true;requestAnimationFrame(apply)};
  const observer=new MutationObserver(queue);observer.observe(document.documentElement,{childList:true,subtree:true});mobile.addEventListener?.('change',queue);addEventListener('resize',queue,{passive:true});
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});else apply();[100,350,800,1600,3200,6000].forEach(delay=>setTimeout(apply,delay));
})();
