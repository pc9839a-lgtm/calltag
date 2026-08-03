(()=>{
  if(document.documentElement.dataset.ctHorizontalImpact)return;
  document.documentElement.dataset.ctHorizontalImpact='1';

  const desktop=matchMedia('(min-width:901px) and (prefers-reduced-motion:no-preference)');
  const clamp=(value,min=0,max=1)=>Math.min(max,Math.max(min,value));

  const style=document.createElement('style');
  style.dataset.ctHorizontalImpact='1';
  style.textContent=`
    @media(min-width:901px) and (prefers-reduced-motion:no-preference){
      .ct-horizontal-clean__panel{
        --ct-focus:0;
        --ct-side:0;
        perspective:1500px;
      }
      .ct-horizontal-clean__panel::after{
        content:'';
        position:absolute;
        inset:0;
        z-index:1;
        pointer-events:none;
        background:radial-gradient(circle at 70% 48%,rgba(112,145,255,.24),transparent 34%);
        opacity:calc(var(--ct-focus) * .72);
        mix-blend-mode:screen;
      }
      .ct-horizontal-clean__copy,
      .ct-horizontal-clean__visual{
        transform-style:preserve-3d;
        backface-visibility:hidden;
        will-change:transform,opacity,filter;
      }
      .ct-horizontal-clean__copy{
        transform:translate3d(calc(var(--ct-side) * -76px),calc((1 - var(--ct-focus)) * 24px),0) scale(calc(.9 + var(--ct-focus) * .1));
        opacity:calc(.12 + var(--ct-focus) * .88);
        filter:blur(calc((1 - var(--ct-focus)) * 2px));
      }
      .ct-horizontal-clean__visual{
        transform:perspective(1400px) translate3d(calc(var(--ct-side) * 108px),0,0) rotateY(calc(var(--ct-side) * -9deg)) scale(calc(.84 + var(--ct-focus) * .16));
        opacity:calc(.22 + var(--ct-focus) * .78);
        filter:brightness(calc(.54 + var(--ct-focus) * .46)) saturate(calc(.72 + var(--ct-focus) * .28)) blur(calc((1 - var(--ct-focus)) * 1.5px));
      }
      .ct-horizontal-clean__panel.ct-impact-hit .ct-horizontal-clean__copy h2,
      .ct-horizontal-clean__panel.ct-impact-hit .ct-horizontal-clean__copy h3{
        animation:ctImpactTitle .52s cubic-bezier(.16,1,.3,1);
      }
      .ct-horizontal-clean__panel.ct-impact-hit .ct-j-scene-clean,
      .ct-horizontal-clean__panel.ct-impact-hit .ct-industry-card{
        animation:ctImpactPunch .56s cubic-bezier(.16,1,.3,1);
      }
      .ct-horizontal-clean__panel.ct-impact-hit::after{
        animation:ctImpactFlash .5s ease-out;
      }
      .ct-horizontal-clean__progress i.on:after{
        box-shadow:0 0 18px rgba(120,151,255,.7);
      }
      @keyframes ctImpactTitle{
        0%{opacity:.25;transform:translate3d(-34px,18px,0) scale(.94);filter:blur(4px)}
        58%{opacity:1;transform:translate3d(5px,-2px,0) scale(1.015);filter:blur(0)}
        100%{opacity:1;transform:none;filter:blur(0)}
      }
      @keyframes ctImpactPunch{
        0%{transform:scale(.93);filter:brightness(.72)}
        58%{transform:scale(1.035);filter:brightness(1.12)}
        100%{transform:scale(1);filter:brightness(1)}
      }
      @keyframes ctImpactFlash{
        0%{opacity:0}
        34%{opacity:.95}
        100%{opacity:calc(var(--ct-focus) * .72)}
      }
    }
  `;
  document.head.append(style);

  let metrics=[];
  let raf=0;
  const hitTimers=new WeakMap();

  const resetPanel=panel=>{
    panel.style.removeProperty('--ct-focus');
    panel.style.removeProperty('--ct-side');
    panel.classList.remove('ct-impact-hit');
  };

  const measure=()=>{
    metrics=[...document.querySelectorAll('.ct-horizontal-clean')].map(section=>({
      section,
      panels:[...section.querySelectorAll('.ct-horizontal-clean__panel')],
      top:section.getBoundingClientRect().top+scrollY,
      range:Math.max(1,section.offsetHeight-innerHeight),
      activeIndex:-1
    })).filter(item=>item.panels.length>1);
    render();
  };

  const triggerHit=(item,index)=>{
    const panel=item.panels[index];
    if(!panel)return;
    item.panels.forEach(node=>node.classList.remove('ct-impact-hit'));
    const oldTimer=hitTimers.get(panel);
    if(oldTimer)clearTimeout(oldTimer);
    void panel.offsetWidth;
    panel.classList.add('ct-impact-hit');
    hitTimers.set(panel,setTimeout(()=>panel.classList.remove('ct-impact-hit'),620));
  };

  const render=()=>{
    raf=0;
    if(!desktop.matches){
      metrics.forEach(item=>item.panels.forEach(resetPanel));
      return;
    }

    metrics.forEach(item=>{
      const progress=clamp((scrollY-item.top)/item.range);
      const position=progress*(item.panels.length-1);
      const activeIndex=Math.min(item.panels.length-1,Math.max(0,Math.round(position)));

      item.panels.forEach((panel,index)=>{
        const distance=index-position;
        const focus=clamp(1-Math.abs(distance)*.92);
        const side=clamp(distance,-1.15,1.15);
        panel.style.setProperty('--ct-focus',focus.toFixed(4));
        panel.style.setProperty('--ct-side',side.toFixed(4));
      });

      if(activeIndex!==item.activeIndex){
        item.activeIndex=activeIndex;
        triggerHit(item,activeIndex);
      }
    });
  };

  const request=()=>{if(!raf)raf=requestAnimationFrame(render)};
  addEventListener('scroll',request,{passive:true});
  addEventListener('resize',()=>requestAnimationFrame(measure),{passive:true});
  addEventListener('load',measure,{once:true});
  desktop.addEventListener?.('change',measure);

  const boot=()=>{
    measure();
    if(metrics.length)return;
    const observer=new MutationObserver(()=>{
      measure();
      if(metrics.length)observer.disconnect();
    });
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),10000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
