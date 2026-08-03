(()=>{
  if(document.documentElement.dataset.ctHorizontalLiveFix)return;
  document.documentElement.dataset.ctHorizontalLiveFix='1';

  const desktop=matchMedia('(min-width:901px)');
  const clamp=value=>Math.max(0,Math.min(1,value));
  let section=null;
  let track=null;
  let panels=[];
  let bars=[];
  let raf=0;

  const cleanWrongRestore=()=>{
    document.querySelectorAll('style[data-ct-recontact-restore]').forEach(node=>node.remove());
    document.querySelectorAll('.ct-recontact-restored').forEach(node=>{
      node.classList.remove('ct-recontact-restored');
      node.style.removeProperty('transform');
      node.style.removeProperty('translate');
      node.style.removeProperty('scale');
      node.style.removeProperty('filter');
      node.style.removeProperty('opacity');
      node.style.removeProperty('animation');
      node.style.removeProperty('transition');
    });
  };

  const mount=()=>{
    cleanWrongRestore();
    section=document.querySelector('.ct-journey-clean');
    if(!section)return false;
    track=section.querySelector('.ct-horizontal-clean__track');
    panels=[...section.querySelectorAll('.ct-horizontal-clean__panel')];
    bars=[...section.querySelectorAll('.ct-horizontal-clean__progress i')];
    if(!track||panels.length!==4)return false;
    requestRender();
    return true;
  };

  const render=()=>{
    raf=0;
    if(!section||!track||!panels.length)return;

    if(!desktop.matches){
      track.style.transform='none';
      panels.forEach(panel=>panel.classList.add('is-active'));
      bars.forEach(bar=>bar.classList.add('on'));
      return;
    }

    const sticky=section.querySelector('.ct-horizontal-clean__sticky');
    const absoluteTop=section.getBoundingClientRect().top+scrollY;
    const stickyHeight=sticky?.offsetHeight||innerHeight;
    const range=Math.max(1,section.offsetHeight-stickyHeight);
    const progress=clamp((scrollY-absoluteTop)/range);
    const x=-progress*(panels.length-1)*innerWidth;
    const index=Math.min(panels.length-1,Math.max(0,Math.round(progress*(panels.length-1))));

    track.style.setProperty('transform',`translate3d(${x}px,0,0)`,'important');
    panels.forEach((panel,i)=>panel.classList.toggle('is-active',i===index));
    bars.forEach((bar,i)=>bar.classList.toggle('on',i<=index));
  };

  const requestRender=()=>{
    if(raf)return;
    raf=requestAnimationFrame(render);
  };

  const boot=()=>{
    if(!mount()){
      const observer=new MutationObserver(()=>{
        if(mount())observer.disconnect();
      });
      observer.observe(document.documentElement,{childList:true,subtree:true});
      setTimeout(()=>observer.disconnect(),12000);
    }

    addEventListener('scroll',requestRender,{passive:true});
    addEventListener('resize',requestRender,{passive:true});
    addEventListener('pageshow',requestRender,{passive:true});
    desktop.addEventListener?.('change',requestRender);

    const resizeObserver=new ResizeObserver(requestRender);
    resizeObserver.observe(document.documentElement);
    [0,100,300,700,1200,2200,4000].forEach(delay=>setTimeout(()=>{
      mount();
      requestRender();
    },delay));
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
