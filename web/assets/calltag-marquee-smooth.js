(()=>{
  if(document.documentElement.dataset.ctMarqueeSmooth)return;
  document.documentElement.dataset.ctMarqueeSmooth='1';

  const start=()=>{
    const viewport=document.querySelector('#targets .ct-marquee-viewport');
    const rail=viewport&&viewport.querySelector('.ct-marquee-rail');
    const firstGroup=rail&&rail.querySelector('.ct-marquee-group');
    if(!viewport||!rail||!firstGroup)return;

    if(!document.querySelector('style[data-ct-marquee-smooth]')){
      const style=document.createElement('style');
      style.dataset.ctMarqueeSmooth='1';
      style.textContent=`
        #targets .ct-marquee-rail{
          animation:none!important;
          transition:none!important;
          transform:translate3d(0,0,0);
          will-change:transform;
        }
        #targets .ct-marquee-viewport:hover .ct-marquee-rail{
          animation-play-state:running!important;
        }
      `;
      document.head.append(style);
    }

    let offset=0;
    let groupWidth=0;
    let previousTime=0;
    let frameId=0;
    const pixelsPerSecond=42;

    const measure=()=>{
      const nextWidth=firstGroup.getBoundingClientRect().width;
      if(!nextWidth)return;
      groupWidth=nextWidth;
      offset=-((Math.abs(offset)%groupWidth));
      rail.style.transform=`translate3d(${offset}px,0,0)`;
    };

    const tick=(time)=>{
      if(!previousTime)previousTime=time;
      const elapsed=Math.min((time-previousTime)/1000,.05);
      previousTime=time;

      if(groupWidth>0){
        offset-=pixelsPerSecond*elapsed;
        while(offset<=-groupWidth)offset+=groupWidth;
        rail.style.transform=`translate3d(${offset}px,0,0)`;
      }
      frameId=requestAnimationFrame(tick);
    };

    const resizeObserver=new ResizeObserver(measure);
    resizeObserver.observe(firstGroup);
    window.addEventListener('resize',measure,{passive:true});
    document.addEventListener('visibilitychange',()=>{previousTime=0;});

    measure();
    cancelAnimationFrame(frameId);
    frameId=requestAnimationFrame(tick);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(start),{once:true});
  else requestAnimationFrame(start);
})();