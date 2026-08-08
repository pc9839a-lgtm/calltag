(()=>{
  if(document.documentElement.dataset.ctInteractionFixV3)return;
  document.documentElement.dataset.ctInteractionFixV3='1';
  const q=(s,r=document)=>r.querySelector(s);
  const qa=(s,r=document)=>[...r.querySelectorAll(s)];

  const run=()=>{
    const kicker=q('.hero-kicker');
    const heroTitle=q('.hero-heading h1');
    const description=q('.hero-heading > p:last-of-type');
    if(kicker)kicker.remove();
    if(heroTitle)heroTitle.innerHTML='통화 후 <span>고객관리.</span>';
    if(description)description.textContent='고객·일정·문자를 통화 직후 남기세요.';
    qa('.hero-heading .ad-actions,.hero-heading .ad-offer').forEach(el=>el.remove());
    qa('.ad-copy').forEach(el=>el.remove());
    qa('.ad-target p,.ad-benefit p,.ad-strength p,.ad-plan-copy').forEach(el=>el.remove());
    qa('.section-copy').forEach((el,index)=>{if(index>0)el.remove();});

    const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
    let textNode;
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

    if(!q('style[data-ct-interaction-fix]')){
      const style=document.createElement('style');style.dataset.ctInteractionFix='3';
      style.textContent=`
        .hero-app{padding:118px 0 72px!important}.hero h1{font-size:clamp(54px,7.4vw,96px)!important;line-height:.96!important}.hero-heading>p{margin-top:20px!important;font-size:clamp(16px,1.4vw,19px)!important}.ad-section{padding:88px 0!important}.ad-head{margin-bottom:34px!important}.ad-target{min-height:150px!important}.ad-benefit{min-height:135px!important}.ad-strength{min-height:175px!important}
        .hero-heading h1{white-space:nowrap!important}.hero-heading .ad-actions,.hero-heading .ad-offer{display:none!important}
        #app .step-item:nth-child(4),#app .app-screen[data-screen="3"]{display:none!important}#app .step-item{cursor:pointer}#app .step-item:hover,#app .step-item:focus-visible{border-color:rgba(59,111,255,.58);background:var(--blue-soft);transform:translateX(8px);outline:none}#app .step-item:hover b,#app .step-item:focus-visible b{background:var(--blue);color:#fff;box-shadow:0 0 0 8px rgba(59,111,255,.11)}
        #targets .ad-head{max-width:none!important}#targets .ad-title{white-space:nowrap!important;font-size:clamp(42px,5.2vw,76px)!important}#targets .ct-marquee-viewport{position:relative!important;display:block!important;width:100%!important;max-width:none!important;margin:0!important;overflow:hidden!important;scroll-snap-type:none!important;scrollbar-width:none!important;mask-image:linear-gradient(90deg,transparent 0,#000 7%,#000 93%,transparent 100%);-webkit-mask-image:linear-gradient(90deg,transparent 0,#000 7%,#000 93%,transparent 100%)}#targets .ct-marquee-rail{display:flex;width:max-content;will-change:transform;animation:ctIndustryFlow 34s linear infinite}#targets .ct-marquee-viewport:hover .ct-marquee-rail{animation-play-state:paused}#targets .ct-marquee-group{display:flex;flex:none;gap:20px;padding-right:20px}#targets .ct-marquee-group .ad-target{flex:0 0 clamp(420px,42vw,650px)!important;width:clamp(420px,42vw,650px)!important;min-height:330px!important;padding:48px 52px!important;border-radius:27px!important;display:flex!important;flex-direction:column!important;justify-content:center!important;scroll-snap-align:none!important}#targets .ct-marquee-group .ad-target span{font-size:13px!important}#targets .ct-marquee-group .ad-target h3{margin-top:24px!important;font-size:clamp(32px,3.5vw,48px)!important;line-height:1.08!important}#targets .ct-marquee-group .ad-target b{margin-top:36px!important;font-size:15px!important}
        @keyframes ctIndustryFlow{from{transform:translate3d(0,0,0)}to{transform:translate3d(-50%,0,0)}}
        @media(max-width:700px){.hero-app{padding:98px 0 58px!important}.hero h1{font-size:52px!important}.hero-heading h1{white-space:normal!important}#targets .ad-title{font-size:clamp(29px,9vw,43px)!important;letter-spacing:-.065em!important}#targets .ct-marquee-group{gap:14px;padding-right:14px}#targets .ct-marquee-group .ad-target{flex-basis:82vw!important;width:82vw!important;min-height:290px!important;padding:34px 28px!important}#targets .ct-marquee-group .ad-target h3{font-size:34px!important}#targets .ct-marquee-rail{animation-duration:28s}}
        @media(prefers-reduced-motion:reduce){#targets .ct-marquee-viewport{overflow-x:auto!important;mask-image:none;-webkit-mask-image:none}#targets .ct-marquee-rail{animation:none!important}}
      `;
      document.head.append(style);
    }
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});else requestAnimationFrame(run);
})();