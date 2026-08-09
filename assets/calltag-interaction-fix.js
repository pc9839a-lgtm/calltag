(()=>{
  if(document.documentElement.dataset.ctInteractionFixV5)return;
  document.documentElement.dataset.ctInteractionFixV5='1';
  const q=(s,r=document)=>r.querySelector(s),qa=(s,r=document)=>[...r.querySelectorAll(s)];
  const run=()=>{
    const kicker=q('.hero-kicker'),heroTitle=q('.hero-heading h1'),description=q('.hero-heading > p:last-of-type');
    if(kicker)kicker.remove();
    if(heroTitle)heroTitle.innerHTML='통화 후 <span>고객관리.</span>';
    if(description)description.textContent='고객·일정·문자를 통화 직후 남기세요.';
    qa('.hero-heading .ad-actions,.hero-heading .ad-offer,.ad-copy').forEach(el=>el.remove());
    qa('.ad-target p,.ad-benefit p,.ad-strength p,.ad-plan-copy').forEach(el=>el.remove());
    qa('.section-copy').forEach((el,index)=>{if(index>0)el.remove();});
    const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);let textNode;
    while((textNode=walker.nextNode()))textNode.nodeValue=textNode.nodeValue.replaceAll('이어집니다.','').replaceAll('흐름입니다.','').replaceAll('정리됩니다.','').replaceAll('한눈에 볼 수 있습니다.','');
    const stage=q('#app .phone-stage');
    if(stage){
      const items=qa('.step-item',stage),screens=qa('.app-screen',stage),progress=q('#phoneProgress',stage),visibleCount=3;
      if(items[3]){items[3].style.display='none';items[3].setAttribute('aria-hidden','true');}
      if(screens[3]){screens[3].style.display='none';screens[3].setAttribute('aria-hidden','true');}
      let lockedIndex=-1,applying=false;
      const activate=index=>{if(index<0||index>=visibleCount)return;applying=true;items.forEach((item,i)=>item.classList.toggle('active',i===index));screens.forEach((screen,i)=>screen.classList.toggle('active',i===index));if(progress)progress.style.width=`${((index+1)/visibleCount)*100}%`;requestAnimationFrame(()=>{applying=false;});};
      items.slice(0,visibleCount).forEach((item,index)=>{item.style.cursor='pointer';item.tabIndex=0;item.addEventListener('mouseenter',()=>{lockedIndex=index;activate(index);});item.addEventListener('mouseleave',()=>{lockedIndex=-1;});item.addEventListener('focus',()=>{lockedIndex=index;activate(index);});item.addEventListener('blur',()=>{lockedIndex=-1;});item.addEventListener('click',()=>activate(index));});
      const observer=new MutationObserver(()=>{if(applying)return;if(lockedIndex>=0){activate(lockedIndex);return;}const activeIndex=screens.findIndex(screen=>screen.classList.contains('active'));if(activeIndex<0||activeIndex>=visibleCount)activate(0);});
      [...items,...screens].forEach(node=>observer.observe(node,{attributes:true,attributeFilter:['class']}));
      activate(Math.min(Math.max(items.findIndex(item=>item.classList.contains('active')),0),visibleCount-1));
      window.addEventListener('pagehide',()=>observer.disconnect(),{once:true});
    }
    qa('.ct-hover-menu,.ct-carousel-controls').forEach(el=>el.remove());
    const targets=q('#targets'),title=q('.ad-title',targets||document),viewport=q('.ad-targets',targets||document);
    if(title)title.textContent='이런 업종에 필요합니다.';
    if(targets&&viewport&&!viewport.classList.contains('ct-marquee-viewport')){
      const cards=qa('.ad-target',viewport);
      if(cards.length){viewport.className='ad-targets ct-marquee-viewport';viewport.removeAttribute('style');const rail=document.createElement('div');rail.className='ct-marquee-rail';const makeGroup=hidden=>{const group=document.createElement('div');group.className='ct-marquee-group';if(hidden)group.setAttribute('aria-hidden','true');cards.forEach(card=>group.appendChild(hidden?card.cloneNode(true):card));return group;};rail.append(makeGroup(false),makeGroup(true));viewport.replaceChildren(rail);}
    }
    const rail=viewport?.querySelector('.ct-marquee-rail'),group=rail?.querySelector('.ct-marquee-group');
    if(viewport&&rail&&group&&!rail.dataset.ctSteady){
      rail.dataset.ctSteady='1';rail.style.animation='none';rail.style.transition='none';
      let offset=0,loopWidth=0,previousTime=0,frameId=0,active=true;
      const measure=()=>{const nextWidth=group.getBoundingClientRect().width;if(!nextWidth)return;loopWidth=nextWidth;offset%=loopWidth;rail.style.transform=`translate3d(${-offset}px,0,0)`;};
      const tick=time=>{if(!active)return;if(!previousTime)previousTime=time;const elapsed=Math.min((time-previousTime)/1000,.05);previousTime=time;if(loopWidth>0){offset=(offset+38*elapsed)%loopWidth;rail.style.transform=`translate3d(${-offset}px,0,0)`;}frameId=requestAnimationFrame(tick);};
      const resizeObserver=new ResizeObserver(()=>requestAnimationFrame(measure));resizeObserver.observe(group);
      const onResize=measure,onVisibility=()=>{previousTime=0;};window.addEventListener('resize',onResize,{passive:true});document.addEventListener('visibilitychange',onVisibility);
      measure();frameId=requestAnimationFrame(tick);
      window.addEventListener('pagehide',()=>{active=false;cancelAnimationFrame(frameId);resizeObserver.disconnect();window.removeEventListener('resize',onResize);document.removeEventListener('visibilitychange',onVisibility);},{once:true});
    }
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});else requestAnimationFrame(run);
})();