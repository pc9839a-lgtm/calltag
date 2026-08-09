(()=>{
  if(document.documentElement.dataset.ctHorizontalImpactV4)return;
  document.documentElement.dataset.ctHorizontalImpactV4='1';
  const desktop=matchMedia('(min-width:901px) and (prefers-reduced-motion:no-preference)');
  const clamp=(value,min=0,max=1)=>Math.min(max,Math.max(min,value));
  const style=document.createElement('style');
  style.dataset.ctHorizontalImpact='4';
  style.textContent=`
    @media(min-width:901px) and (prefers-reduced-motion:no-preference){
      .ct-horizontal-clean__panel{--ct-glow:0;--ct-copy-x:0px;--ct-copy-y:0px;--ct-copy-scale:1;--ct-copy-opacity:1;--ct-copy-blur:0px;--ct-visual-x:0px;--ct-visual-rotate:0deg;--ct-visual-scale:1;--ct-visual-opacity:1;--ct-visual-brightness:1;--ct-visual-saturation:1;--ct-visual-blur:0px;perspective:1500px}
      .ct-horizontal-clean__panel::after{content:'';position:absolute;inset:0;z-index:1;pointer-events:none;background:radial-gradient(circle at 70% 48%,rgba(112,145,255,.24),transparent 34%);opacity:var(--ct-glow);mix-blend-mode:screen}
      .ct-horizontal-clean__copy,.ct-horizontal-clean__visual{transform-style:preserve-3d;backface-visibility:hidden;will-change:transform,opacity,filter}
      .ct-horizontal-clean__copy{transform:translate3d(var(--ct-copy-x),var(--ct-copy-y),0) scale(var(--ct-copy-scale));opacity:var(--ct-copy-opacity);filter:blur(var(--ct-copy-blur))}
      .ct-horizontal-clean__visual{transform:perspective(1400px) translate3d(var(--ct-visual-x),0,0) rotateY(var(--ct-visual-rotate)) scale(var(--ct-visual-scale));opacity:var(--ct-visual-opacity);filter:brightness(var(--ct-visual-brightness)) saturate(var(--ct-visual-saturation)) blur(var(--ct-visual-blur))}
      .ct-impact-shell{width:100%;display:grid;place-items:center;transform-origin:center}
      .ct-horizontal-clean__panel.ct-impact-hit .ct-horizontal-clean__copy h2,.ct-horizontal-clean__panel.ct-impact-hit .ct-horizontal-clean__copy h3{animation:ctImpactTitle .52s cubic-bezier(.16,1,.3,1)}
      .ct-horizontal-clean__panel.ct-impact-hit .ct-impact-shell{animation:ctImpactPunch .56s cubic-bezier(.16,1,.3,1)}
      .ct-horizontal-clean__panel.ct-impact-hit::after{animation:ctImpactFlash .5s ease-out}
      .ct-horizontal-clean__progress i.on:after{box-shadow:0 0 18px rgba(120,151,255,.7)}
      @keyframes ctImpactTitle{0%{opacity:.25;transform:translate3d(-34px,18px,0) scale(.94);filter:blur(4px)}58%{opacity:1;transform:translate3d(5px,-2px,0) scale(1.015);filter:blur(0)}100%{opacity:1;transform:none;filter:blur(0)}}
      @keyframes ctImpactPunch{0%{transform:scale(.93);filter:brightness(.72)}58%{transform:scale(1.035);filter:brightness(1.12)}100%{transform:scale(1);filter:brightness(1)}}
      @keyframes ctImpactFlash{0%{opacity:0}34%{opacity:.95}100%{opacity:var(--ct-glow)}}
    }
  `;
  document.head.append(style);

  let metrics=[],raf=0;
  const hitTimers=new WeakMap();
  const propertyNames=['--ct-glow','--ct-copy-x','--ct-copy-y','--ct-copy-scale','--ct-copy-opacity','--ct-copy-blur','--ct-visual-x','--ct-visual-rotate','--ct-visual-scale','--ct-visual-opacity','--ct-visual-brightness','--ct-visual-saturation','--ct-visual-blur'];
  const addImpactShell=panel=>{const visual=panel.querySelector('.ct-horizontal-clean__visual');if(!visual||visual.firstElementChild?.classList.contains('ct-impact-shell'))return;const shell=document.createElement('div');shell.className='ct-impact-shell';while(visual.firstChild)shell.append(visual.firstChild);visual.append(shell);};
  const resetPanel=panel=>{propertyNames.forEach(name=>panel.style.removeProperty(name));panel.classList.remove('ct-impact-hit');};
  const measure=()=>{metrics=[...document.querySelectorAll('.ct-horizontal-clean')].map(section=>{const panels=[...section.querySelectorAll('.ct-horizontal-clean__panel')];panels.forEach(addImpactShell);return{section,panels,top:section.getBoundingClientRect().top+scrollY,range:Math.max(1,section.offsetHeight-innerHeight),activeIndex:-1};}).filter(item=>item.panels.length>1);render();};
  const triggerHit=(item,index)=>{const panel=item.panels[index];if(!panel)return;item.panels.forEach(node=>node.classList.remove('ct-impact-hit'));const oldTimer=hitTimers.get(panel);if(oldTimer)clearTimeout(oldTimer);void panel.offsetWidth;panel.classList.add('ct-impact-hit');hitTimers.set(panel,setTimeout(()=>panel.classList.remove('ct-impact-hit'),620));};
  const render=()=>{raf=0;if(!desktop.matches){metrics.forEach(item=>item.panels.forEach(resetPanel));return;}metrics.forEach(item=>{const progress=clamp((scrollY-item.top)/item.range);const position=progress*(item.panels.length-1);const activeIndex=Math.min(item.panels.length-1,Math.max(0,Math.round(position)));const inView=scrollY>=item.top-innerHeight*.12&&scrollY<=item.top+item.range+innerHeight*.12;item.panels.forEach((panel,index)=>{const distance=index-position;const focus=clamp(1-Math.abs(distance)*.92);const side=clamp(distance,-1.15,1.15);panel.style.setProperty('--ct-glow',(focus*.72).toFixed(4));panel.style.setProperty('--ct-copy-x',`${(side*-76).toFixed(2)}px`);panel.style.setProperty('--ct-copy-y',`${((1-focus)*24).toFixed(2)}px`);panel.style.setProperty('--ct-copy-scale',(.9+focus*.1).toFixed(4));panel.style.setProperty('--ct-copy-opacity',(.12+focus*.88).toFixed(4));panel.style.setProperty('--ct-copy-blur',`${((1-focus)*2).toFixed(2)}px`);panel.style.setProperty('--ct-visual-x',`${(side*108).toFixed(2)}px`);panel.style.setProperty('--ct-visual-rotate',`${(side*-9).toFixed(2)}deg`);panel.style.setProperty('--ct-visual-scale',(.84+focus*.16).toFixed(4));panel.style.setProperty('--ct-visual-opacity',(.22+focus*.78).toFixed(4));panel.style.setProperty('--ct-visual-brightness',(.54+focus*.46).toFixed(4));panel.style.setProperty('--ct-visual-saturation',(.72+focus*.28).toFixed(4));panel.style.setProperty('--ct-visual-blur',`${((1-focus)*1.5).toFixed(2)}px`);});if(!inView){item.activeIndex=-1;return;}if(activeIndex!==item.activeIndex){item.activeIndex=activeIndex;triggerHit(item,activeIndex);}});};
  const request=()=>{if(!raf)raf=requestAnimationFrame(render);};
  const onResize=()=>requestAnimationFrame(measure);
  addEventListener('scroll',request,{passive:true});
  addEventListener('resize',onResize,{passive:true});
  addEventListener('load',measure,{once:true});
  desktop.addEventListener?.('change',measure);
  const boot=()=>{measure();const timers=[100,350,900,1800].map(delay=>setTimeout(measure,delay));window.addEventListener('pagehide',()=>{timers.forEach(clearTimeout);removeEventListener('scroll',request);removeEventListener('resize',onResize);desktop.removeEventListener?.('change',measure);if(raf)cancelAnimationFrame(raf);},{once:true});};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();