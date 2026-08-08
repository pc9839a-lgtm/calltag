(()=>{
  if(document.documentElement.dataset.ctHorizontalLiveFixV2)return;
  document.documentElement.dataset.ctHorizontalLiveFixV2='1';
  const desktop=matchMedia('(min-width:901px)');
  const clamp=value=>Math.max(0,Math.min(1,value));
  let section=null,track=null,panels=[],bars=[],raf=0;
  const cleanWrongRestore=()=>{document.querySelectorAll('style[data-ct-recontact-restore]').forEach(node=>node.remove());document.querySelectorAll('.ct-recontact-restored').forEach(node=>{node.classList.remove('ct-recontact-restored');['transform','translate','scale','filter','opacity','animation','transition'].forEach(name=>node.style.removeProperty(name));});};
  const mount=()=>{cleanWrongRestore();section=document.querySelector('.ct-journey-clean');if(!section)return false;track=section.querySelector('.ct-horizontal-clean__track');panels=[...section.querySelectorAll('.ct-horizontal-clean__panel')];bars=[...section.querySelectorAll('.ct-horizontal-clean__progress i')];if(!track||panels.length!==4)return false;requestRender();return true;};
  const render=()=>{raf=0;if(!section||!track||!panels.length)return;if(!desktop.matches){track.style.transform='none';panels.forEach(panel=>panel.classList.add('is-active'));bars.forEach(bar=>bar.classList.add('on'));return;}const sticky=section.querySelector('.ct-horizontal-clean__sticky');const absoluteTop=section.getBoundingClientRect().top+scrollY;const stickyHeight=sticky?.offsetHeight||innerHeight;const range=Math.max(1,section.offsetHeight-stickyHeight);const progress=clamp((scrollY-absoluteTop)/range);const x=-progress*(panels.length-1)*innerWidth;const index=Math.min(panels.length-1,Math.max(0,Math.round(progress*(panels.length-1))));track.style.setProperty('transform',`translate3d(${x}px,0,0)`,'important');panels.forEach((panel,i)=>panel.classList.toggle('is-active',i===index));bars.forEach((bar,i)=>bar.classList.toggle('on',i<=index));};
  const requestRender=()=>{if(raf)return;raf=requestAnimationFrame(render);};
  const boot=()=>{
    let fallbackObserver=null;
    if(!mount()){fallbackObserver=new MutationObserver(()=>{if(mount()){fallbackObserver.disconnect();fallbackObserver=null;}});fallbackObserver.observe(document.documentElement,{childList:true,subtree:true});setTimeout(()=>{fallbackObserver?.disconnect();fallbackObserver=null;},5000);}
    const onScroll=requestRender,onResize=requestRender,onPageShow=requestRender,onMedia=requestRender;
    addEventListener('scroll',onScroll,{passive:true});addEventListener('resize',onResize,{passive:true});addEventListener('pageshow',onPageShow,{passive:true});desktop.addEventListener?.('change',onMedia);
    const resizeObserver=new ResizeObserver(requestRender);resizeObserver.observe(document.documentElement);
    const timers=[0,100,300,700,1200,2200,4000].map(delay=>setTimeout(()=>{mount();requestRender();},delay));
    window.addEventListener('pagehide',()=>{timers.forEach(clearTimeout);fallbackObserver?.disconnect();resizeObserver.disconnect();removeEventListener('scroll',onScroll);removeEventListener('resize',onResize);removeEventListener('pageshow',onPageShow);desktop.removeEventListener?.('change',onMedia);if(raf)cancelAnimationFrame(raf);},{once:true});
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();