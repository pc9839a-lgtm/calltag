(()=>{
  if(document.documentElement.dataset.ctFinalCtaMotion)return;
  document.documentElement.dataset.ctFinalCtaMotion='1';

  const reduce=matchMedia('(prefers-reduced-motion: reduce)').matches;
  const clamp=value=>Math.max(0,Math.min(1,value));
  const easeOut=value=>1-Math.pow(1-value,3);
  const easeIn=value=>value*value*value;

  const style=document.createElement('style');
  style.dataset.ctFinalCtaMotion='1';
  style.textContent=`
    .ct-final-cta-motion{position:relative!important;overflow:hidden!important;isolation:isolate!important}
    .ct-final-cta-motion:before{content:'';position:absolute;left:50%;top:42%;width:min(920px,82vw);height:440px;z-index:-1;border-radius:50%;background:radial-gradient(circle,rgba(63,101,255,.2),transparent 68%);filter:blur(58px);opacity:var(--ct-cta-glow,0);transform:translate(-50%,-50%) scale(var(--ct-cta-glow-scale,.72));pointer-events:none}
    .ct-final-cta-motion [data-ct-final-cta-part]{will-change:transform,opacity,filter}
    @media(prefers-reduced-motion:reduce){.ct-final-cta-motion [data-ct-final-cta-part]{opacity:1!important;transform:none!important;filter:none!important}.ct-final-cta-motion:before{opacity:.28!important;transform:translate(-50%,-50%) scale(1)!important}}
  `;
  document.head.append(style);

  let section=null;
  let heading=null;
  let buttons=[];
  let raf=0;

  const findTarget=()=>{
    const headings=[...document.querySelectorAll('h1,h2,h3,strong')];
    const marker=headings.find(node=>{
      const text=(node.textContent||'').replace(/\s+/g,' ').trim();
      return text.includes('이제 전화 후 고객관리')&&text.includes('놓치지 마세요');
    })||headings.find(node=>{
      const text=(node.textContent||'').replace(/\s+/g,' ').trim();
      return text.includes('전화 후 고객관리')&&text.includes('놓치지');
    });
    if(!marker)return false;

    section=marker.closest('.ad-final,.final-cta,.cta-section,[class*="final-cta"],[class*="cta"],section,.section')||marker.parentElement?.parentElement||marker.parentElement;
    if(!section)return false;

    heading=marker;
    buttons=[...section.querySelectorAll('a,button')].filter(node=>{
      const text=(node.textContent||'').replace(/\s+/g,' ').trim();
      return text.includes('무료')||text.includes('페이지로')||text.includes('시작');
    }).slice(0,3);

    section.classList.add('ct-final-cta-motion');
    heading.dataset.ctFinalCtaPart='heading';
    buttons.forEach((button,index)=>button.dataset.ctFinalCtaPart=`button-${index+1}`);
    requestRender();
    return true;
  };

  const render=()=>{
    raf=0;
    if(!section||!heading)return;

    if(reduce){
      heading.style.opacity='1';heading.style.transform='none';heading.style.filter='none';
      buttons.forEach(button=>{button.style.opacity='1';button.style.transform='none';button.style.filter='none'});
      return;
    }

    const rect=section.getBoundingClientRect();
    const vh=innerHeight||document.documentElement.clientHeight;
    const enter=clamp((vh*.92-rect.top)/(vh*.6));
    const leave=clamp((-rect.top)/(Math.max(rect.height,vh*.72)*.72));
    const enterEase=easeOut(enter);
    const leaveEase=easeIn(leave);

    const headingIn=easeOut(clamp(enter/.7));
    const headingOpacity=headingIn*(1-leaveEase);
    const headingY=(1-headingIn)*82-leaveEase*72;
    const headingScale=.92+headingIn*.08-leaveEase*.035;
    const headingBlur=(1-headingIn)*10+leaveEase*7;

    heading.style.opacity=headingOpacity.toFixed(3);
    heading.style.transform=`translate3d(0,${headingY.toFixed(2)}px,0) scale(${headingScale.toFixed(4)})`;
    heading.style.filter=`blur(${headingBlur.toFixed(2)}px)`;

    buttons.forEach((button,index)=>{
      const start=.34+index*.13;
      const local=easeOut(clamp((enter-start)/(1-start)));
      const opacity=local*(1-leaveEase);
      const direction=index%2===0?-1:1;
      const x=(1-local)*direction*46+leaveEase*direction*34;
      const y=(1-local)*50-leaveEase*54;
      const scale=.9+local*.1-leaveEase*.04;
      const blur=(1-local)*7+leaveEase*5;
      button.style.opacity=opacity.toFixed(3);
      button.style.transform=`translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) scale(${scale.toFixed(4)})`;
      button.style.filter=`blur(${blur.toFixed(2)}px)`;
    });

    section.style.setProperty('--ct-cta-glow',(enterEase*(1-leaveEase)*.72).toFixed(3));
    section.style.setProperty('--ct-cta-glow-scale',(.72+enterEase*.34-leaveEase*.08).toFixed(3));
  };

  const requestRender=()=>{if(!raf)raf=requestAnimationFrame(render)};

  const boot=()=>{
    if(!findTarget()){
      const observer=new MutationObserver(()=>{if(findTarget())observer.disconnect()});
      observer.observe(document.documentElement,{childList:true,subtree:true});
      setTimeout(()=>observer.disconnect(),12000);
    }
    addEventListener('scroll',requestRender,{passive:true});
    addEventListener('resize',requestRender,{passive:true});
    addEventListener('pageshow',requestRender,{passive:true});
    [100,350,800,1600,3000].forEach(delay=>setTimeout(()=>{findTarget();requestRender()},delay));
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
