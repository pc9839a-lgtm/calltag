(()=>{
  if(document.documentElement.dataset.ctSteadySlider)return;
  document.documentElement.dataset.ctSteadySlider='1';

  const run=()=>{
    const viewport=document.querySelector('#targets .ct-marquee-viewport');
    const rail=viewport&&viewport.querySelector('.ct-marquee-rail');
    const group=rail&&rail.querySelector('.ct-marquee-group');
    if(!viewport||!rail||!group)return;

    rail.style.animation='none';
    rail.style.animationPlayState='paused';
    rail.style.transition='none';
    rail.style.transform='translate3d(0,0,0)';

    /* The old paged-carousel interval still calls scrollTo every 3 seconds.
       Neutralize it so only the continuous transform controls movement. */
    try{viewport.scrollTo=()=>{};}catch(error){}
    viewport.scrollLeft=0;

    let offset=0;
    let loopWidth=0;
    let previousTime=0;
    let frameId=0;
    const pixelsPerSecond=38;

    const measure=()=>{
      const nextWidth=group.getBoundingClientRect().width;
      if(!nextWidth)return;
      loopWidth=nextWidth;
      offset%=loopWidth;
      rail.style.transform=`translate3d(${-offset}px,0,0)`;
    };

    const tick=time=>{
      if(!previousTime)previousTime=time;
      const elapsed=Math.min((time-previousTime)/1000,.05);
      previousTime=time;

      if(loopWidth>0){
        if(viewport.scrollLeft!==0)viewport.scrollLeft=0;
        offset=(offset+pixelsPerSecond*elapsed)%loopWidth;
        rail.style.transform=`translate3d(${-offset}px,0,0)`;
      }
      frameId=requestAnimationFrame(tick);
    };

    const resizeObserver=new ResizeObserver(()=>requestAnimationFrame(measure));
    resizeObserver.observe(group);
    window.addEventListener('resize',measure,{passive:true});
    document.addEventListener('visibilitychange',()=>{previousTime=0;});
    window.addEventListener('pagehide',()=>cancelAnimationFrame(frameId),{once:true});

    if(!document.querySelector('style[data-ct-steady-slider]')){
      const style=document.createElement('style');
      style.dataset.ctSteadySlider='1';
      style.textContent=`
        #targets .ct-marquee-rail{
          animation:none!important;
          animation-play-state:paused!important;
          transition:none!important;
          will-change:transform;
        }
        #targets .ct-marquee-viewport{
          overflow:hidden!important;
          scroll-behavior:auto!important;
          scroll-snap-type:none!important;
          overscroll-behavior-x:none!important;
        }
        #targets .ct-marquee-viewport:hover .ct-marquee-rail{
          animation-play-state:paused!important;
        }
        @media(max-width:700px){
          #targets{padding:52px 0 58px!important}
          #targets .ad-head{margin-bottom:24px!important;padding:0 14px!important}
          #targets .ad-kicker{margin-bottom:10px!important;font-size:12px!important}
          #targets .ad-title{font-size:clamp(28px,8vw,37px)!important;white-space:nowrap!important}
          #targets .ct-marquee-viewport{width:100vw!important;margin-left:calc(50% - 50vw)!important;padding:0 14px!important;mask-image:linear-gradient(90deg,transparent,#000 4%,#000 96%,transparent)!important;-webkit-mask-image:linear-gradient(90deg,transparent,#000 4%,#000 96%,transparent)!important}
          #targets .ct-marquee-group{gap:14px!important;padding-right:14px!important}
          #targets .ct-marquee-group .ad-target{flex:0 0 calc(100vw - 52px)!important;width:calc(100vw - 52px)!important;max-width:410px!important;min-height:220px!important;padding:27px 24px!important;border-radius:20px!important}
          #targets .ct-marquee-group .ad-target h3{margin-top:16px!important;font-size:29px!important}
          #targets .ct-marquee-group .ad-target b{margin-top:23px!important;font-size:12px!important}
        }
      `;
      document.head.append(style);
    }

    measure();
    frameId=requestAnimationFrame(tick);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(run),{once:true});
  else requestAnimationFrame(run);
})();