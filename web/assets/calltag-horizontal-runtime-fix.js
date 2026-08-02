(()=>{
  if(document.documentElement.dataset.ctHorizontalRuntimeFix)return;
  document.documentElement.dataset.ctHorizontalRuntimeFix='1';

  const clamp=(value,min=0,max=1)=>Math.min(max,Math.max(min,value));
  const lerp=(a,b,t)=>a+(b-a)*t;
  const reduced=matchMedia('(prefers-reduced-motion: reduce)');

  const mount=()=>{
    const industry=document.querySelector('.ct-horizontal-industries');
    const journey=document.querySelector('.ct-journey-horizontal');
    const industryTrack=industry?.querySelector('.ct-h-track');
    const journeyTrack=journey?.querySelector('.ct-j-track');
    if(!industry||!journey||!industryTrack||!journeyTrack)return false;

    industryTrack.style.width='400vw';

    let industryCurrent=0;
    let journeyCurrent=0;
    let frame=0;

    const render=()=>{
      frame=0;
      if(innerWidth<=900){
        industryTrack.style.removeProperty('transform');
        journeyTrack.style.removeProperty('transform');
        return;
      }

      const industryTarget=clamp((scrollY-industry.offsetTop)/(industry.offsetHeight-innerHeight));
      const journeyTarget=clamp((scrollY-journey.offsetTop)/(journey.offsetHeight-innerHeight));

      industryCurrent=reduced.matches?industryTarget:lerp(industryCurrent,industryTarget,.14);
      journeyCurrent=reduced.matches?journeyTarget:lerp(journeyCurrent,journeyTarget,.14);

      const industryX=-(1+industryCurrent*2)*innerWidth;
      const journeyX=-(journeyCurrent*3)*innerWidth;

      industryTrack.style.setProperty('transform',`translate3d(${industryX}px,0,0)`,'important');
      journeyTrack.style.setProperty('transform',`translate3d(${journeyX}px,0,0)`,'important');

      const industryIndex=Math.min(2,Math.max(0,Math.round(industryCurrent*2)));
      const journeyIndex=Math.min(3,Math.max(0,Math.round(journeyCurrent*3)));

      industry.querySelectorAll('.ct-h-panel').forEach((panel,index)=>{
        panel.classList.toggle('is-active',index===industryIndex+1);
      });
      journey.querySelectorAll('.ct-j-panel').forEach((panel,index)=>{
        panel.classList.toggle('is-active',index===journeyIndex);
      });

      const count=industry.querySelector('.ct-h-count b');
      if(count)count.textContent=String(industryIndex+1).padStart(2,'0');

      journey.classList.remove('is-stage-0','is-stage-1','is-stage-2','is-stage-3');
      journey.classList.add(`is-stage-${journeyIndex}`);
      journey.querySelectorAll('.ct-j-progress i').forEach((bar,index)=>{
        bar.classList.toggle('on',index<=journeyIndex);
      });

      if(Math.abs(industryCurrent-industryTarget)>.001||Math.abs(journeyCurrent-journeyTarget)>.001)request();
    };

    const request=()=>{
      if(!frame)frame=requestAnimationFrame(render);
    };

    addEventListener('scroll',request,{passive:true});
    addEventListener('resize',request,{passive:true});
    request();
    return true;
  };

  const boot=()=>{
    if(mount())return;
    const observer=new MutationObserver(()=>{
      if(mount())observer.disconnect();
    });
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),15000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(boot),{once:true});
  else requestAnimationFrame(boot);
})();
