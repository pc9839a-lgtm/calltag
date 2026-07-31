(()=>{
  if(document.documentElement.dataset.ctSteadySlider)return;
  document.documentElement.dataset.ctSteadySlider='1';
  const q=(s,r=document)=>r.querySelector(s);
  const run=()=>{
    const viewport=q('#targets .ct-marquee-viewport');
    const rail=q('.ct-marquee-rail',viewport||document);
    const group=q('.ct-marquee-group',viewport||document);
    if(!viewport||!rail||!group)return;
    rail.style.animation='none';rail.style.animationPlayState='paused';rail.style.transform='translate3d(0,0,0)';
    let offset=0,loopWidth=0,last=0,paused=false,visible=true,frame=0;
    const reduced=matchMedia('(prefers-reduced-motion: reduce)');
    const speed=()=>matchMedia('(max-width:700px)').matches?28:42;
    const measure=()=>{loopWidth=group.getBoundingClientRect().width;if(loopWidth>0)offset%=loopWidth;};
    const tick=time=>{if(!last)last=time;const delta=Math.min(time-last,50);last=time;if(!paused&&visible&&!reduced.matches&&loopWidth>0){offset+=speed()*delta/1000;if(offset>=loopWidth)offset-=loopWidth;rail.style.transform=`translate3d(${-offset}px,0,0)`;}frame=requestAnimationFrame(tick);};
    viewport.addEventListener('mouseenter',()=>paused=true);viewport.addEventListener('mouseleave',()=>{paused=false;last=performance.now();});viewport.addEventListener('focusin',()=>paused=true);viewport.addEventListener('focusout',e=>{if(!viewport.contains(e.relatedTarget)){paused=false;last=performance.now();}});
    new ResizeObserver(()=>requestAnimationFrame(measure)).observe(viewport);new IntersectionObserver(e=>{visible=e[0]?.isIntersecting??true;last=performance.now();},{threshold:.01}).observe(viewport);measure();frame=requestAnimationFrame(tick);window.addEventListener('pagehide',()=>cancelAnimationFrame(frame),{once:true});
    const style=document.createElement('style');style.dataset.ctSteadySlider='1';style.textContent='#targets .ct-marquee-rail{animation:none!important;transition:none!important;will-change:transform}#targets .ct-marquee-viewport{overflow:hidden!important}@media(max-width:700px){#targets{padding:52px 0 58px!important}#targets .ad-head{margin-bottom:24px!important;padding:0 14px!important}#targets .ad-kicker{margin-bottom:10px!important;font-size:12px!important}#targets .ad-title{font-size:clamp(28px,8vw,37px)!important;white-space:nowrap!important}#targets .ct-marquee-viewport{width:100vw!important;margin-left:calc(50% - 50vw)!important;padding:0 14px!important;mask-image:linear-gradient(90deg,transparent,#000 4%,#000 96%,transparent)!important;-webkit-mask-image:linear-gradient(90deg,transparent,#000 4%,#000 96%,transparent)!important}#targets .ct-marquee-group{gap:14px!important;padding-right:14px!important}#targets .ct-marquee-group .ad-target{flex:0 0 calc(100vw - 52px)!important;width:calc(100vw - 52px)!important;max-width:410px!important;min-height:220px!important;padding:27px 24px!important;border-radius:20px!important}#targets .ct-marquee-group .ad-target h3{margin-top:16px!important;font-size:29px!important}#targets .ct-marquee-group .ad-target b{margin-top:23px!important;font-size:12px!important}}';document.head.append(style);
  };
  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(run),{once:true}):requestAnimationFrame(run);
})();