(()=>{
  if(document.documentElement.dataset.ctFinalCtaMotionV2)return;
  document.documentElement.dataset.ctFinalCtaMotionV2='1';

  const reduce=matchMedia('(prefers-reduced-motion:reduce)').matches;
  const clamp=value=>Math.max(0,Math.min(1,value));
  const ease=value=>1-Math.pow(1-value,3);
  let section=null;
  let heading=null;
  let parts=[];
  let raf=0;

  const installStyle=()=>{
    if(document.querySelector('style[data-ct-final-cta-motion-v2]'))return;
    const style=document.createElement('style');
    style.dataset.ctFinalCtaMotionV2='1';
    style.textContent=`
      .ct-final-cta-motion-v2{position:relative!important;overflow:hidden!important;isolation:isolate!important}
      .ct-final-cta-motion-v2 [data-ct-final-cta-part]{will-change:transform,opacity,filter}
      .ct-final-cta-motion-v2:after{content:'';position:absolute;left:50%;top:45%;z-index:-1;width:min(900px,84vw);height:460px;border-radius:50%;background:radial-gradient(circle,rgba(70,108,255,.2),transparent 68%);filter:blur(58px);opacity:var(--ct-final-glow,0);transform:translate(-50%,-50%) scale(var(--ct-final-glow-scale,.82));pointer-events:none}
      @media(max-width:900px){.ct-final-cta-motion-v2:after{width:120vw;height:360px}.ct-final-cta-motion-v2 .ad-actions{display:grid!important;grid-template-columns:1fr!important;gap:10px!important;width:100%!important;max-width:390px!important;margin:24px auto 0!important}.ct-final-cta-motion-v2 .ad-btn{width:100%!important;min-height:54px!important}}
      @media(prefers-reduced-motion:reduce){.ct-final-cta-motion-v2 [data-ct-final-cta-part]{opacity:1!important;transform:none!important;filter:none!important}.ct-final-cta-motion-v2:after{opacity:.35!important;transform:translate(-50%,-50%) scale(1)!important}}
    `;
    document.head.append(style);
  };

  const text=node=>(node?.textContent||'').replace(/\s+/g,' ').trim();

  const findSection=()=>{
    const exact=[...document.querySelectorAll('h1,h2,h3')].find(node=>{
      const value=text(node);
      return value.includes('이제 전화 후 고객관리')||value.includes('다음 연락을 놓치기 전에')||value.includes('콜태그를 시작하세요');
    });
    if(!exact)return false;

    const target=exact.closest('.ad-final,.final-cta,.cta-section,[class*="final-cta"],section,.section')||exact.parentElement;
    if(!target)return false;
    if(target.dataset.ctFinalCtaMounted==='1')return true;

    section=target;
    heading=exact;
    const actions=section.querySelector('.ad-actions,[class*="actions"],[class*="buttons"]');
    parts=[heading];
    section.querySelectorAll('p,.ad-final-tag,a,button').forEach(node=>{
      if(!parts.includes(node))parts.push(node);
    });
    if(actions&&!parts.includes(actions))parts.push(actions);

    section.classList.add('ct-final-cta-motion-v2');
    section.dataset.ctFinalCtaMounted='1';
    parts.forEach((node,index)=>{
      node.dataset.ctFinalCtaPart=String(index);
      node.style.opacity='0';
      node.style.transform=`translate3d(0,${index===0?72:46}px,0) scale(${index===0?.94:.97})`;
      node.style.filter=`blur(${index===0?9:6}px)`;
    });
    requestRender();
    return true;
  };

  const render=()=>{
    raf=0;
    if(!section||!heading)return;
    if(reduce){
      parts.forEach(node=>{node.style.opacity='1';node.style.transform='none';node.style.filter='none'});
      return;
    }

    const rect=section.getBoundingClientRect();
    const vh=innerHeight||document.documentElement.clientHeight;
    const enter=clamp((vh*.9-rect.top)/(vh*.58));
    const leave=clamp((vh*.16-rect.bottom)/(vh*.42));
    const inEase=ease(enter);
    const outEase=leave*leave;

    parts.forEach((node,index)=>{
      const stagger=Math.min(index*.085,.34);
      const local=ease(clamp((enter-stagger)/(1-stagger)));
      const opacity=local*(1-outEase);
      const direction=index%2===0?-1:1;
      const x=index===0?0:(1-local)*direction*24+outEase*direction*18;
      const y=(1-local)*(index===0?72:42)-outEase*(index===0?54:38);
      const scale=(index===0?.94:.97)+local*(index===0?.06:.03)-outEase*.025;
      const blur=(1-local)*(index===0?9:6)+outEase*4;
      node.style.opacity=opacity.toFixed(3);
      node.style.transform=`translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) scale(${scale.toFixed(4)})`;
      node.style.filter=`blur(${blur.toFixed(2)}px)`;
    });

    section.style.setProperty('--ct-final-glow',(inEase*(1-outEase)*.72).toFixed(3));
    section.style.setProperty('--ct-final-glow-scale',(.82+inEase*.2-outEase*.08).toFixed(3));
  };

  const requestRender=()=>{if(!raf)raf=requestAnimationFrame(render)};

  installStyle();
  const observer=new MutationObserver(()=>{
    if(findSection())requestRender();
  });
  observer.observe(document.documentElement,{childList:true,subtree:true});

  const boot=()=>{
    findSection();
    requestRender();
    addEventListener('scroll',requestRender,{passive:true});
    addEventListener('resize',requestRender,{passive:true});
    addEventListener('pageshow',requestRender,{passive:true});
    setTimeout(()=>observer.disconnect(),15000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
