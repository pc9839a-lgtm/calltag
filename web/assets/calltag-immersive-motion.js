(()=>{
  if(document.documentElement.dataset.ctImmersiveMotion)return;
  document.documentElement.dataset.ctImmersiveMotion='1';

  const reduced=matchMedia('(prefers-reduced-motion: reduce)');
  const coarse=matchMedia('(pointer: coarse)');
  const clamp=(value,min=0,max=1)=>Math.min(max,Math.max(min,value));
  const lerp=(a,b,t)=>a+(b-a)*t;

  const css=`
    :root{--ct-motion-progress:0;--ct-motion-velocity:0;--ct-pointer-x:50vw;--ct-pointer-y:42vh}
    html.ct-lenis,html.ct-lenis body{height:auto}
    html.ct-lenis.ct-lenis-smooth{scroll-behavior:auto!important}
    html.ct-lenis.ct-lenis-stopped{overflow:hidden}
    body.ct-immersive-ready{position:relative;overflow-x:clip}
    body.ct-immersive-ready:before{content:'';position:fixed;z-index:-3;inset:-24vh -20vw;background:radial-gradient(circle at var(--ct-pointer-x) var(--ct-pointer-y),rgba(65,108,255,.16),transparent 24%),radial-gradient(circle at 18% 32%,rgba(36,70,190,.16),transparent 30%),linear-gradient(180deg,#08090d,#090b13 42%,#07080c);pointer-events:none;transition:background .35s ease}
    body.ct-immersive-ready:after{content:'';position:fixed;z-index:-2;inset:0;opacity:.18;pointer-events:none;background-image:linear-gradient(rgba(255,255,255,.022) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.022) 1px,transparent 1px);background-size:72px 72px;mask-image:linear-gradient(to bottom,transparent 0,#000 16%,#000 82%,transparent 100%);transform:translate3d(0,calc(var(--ct-motion-progress)*-42px),0)}

    .ct-motion-progress{position:fixed;z-index:180;right:22px;top:50%;display:grid;gap:9px;transform:translateY(-50%);padding:13px 10px;border:1px solid rgba(255,255,255,.09);border-radius:999px;background:rgba(8,10,15,.66);backdrop-filter:blur(18px);box-shadow:0 14px 50px rgba(0,0,0,.22)}
    .ct-motion-progress button{width:7px;height:7px;padding:0;border:0;border-radius:50%;background:#525866;cursor:pointer;transition:height .28s ease,background .28s ease,box-shadow .28s ease}
    .ct-motion-progress button.is-active{height:25px;background:#7896ff;box-shadow:0 0 18px rgba(91,128,255,.75)}
    .ct-motion-meter{position:fixed;z-index:181;left:0;top:0;width:100%;height:3px;transform-origin:0 50%;transform:scaleX(var(--ct-motion-progress));background:linear-gradient(90deg,#3b6fff,#9bb0ff);box-shadow:0 0 18px rgba(59,111,255,.65);pointer-events:none}
    .ct-motion-cursor{position:fixed;z-index:179;left:0;top:0;width:84px;height:84px;border:1px solid rgba(134,161,255,.28);border-radius:50%;transform:translate3d(calc(var(--ct-cursor-x, -120px) - 42px),calc(var(--ct-cursor-y, -120px) - 42px),0);pointer-events:none;mix-blend-mode:screen;transition:width .24s ease,height .24s ease,opacity .24s ease,border-color .24s ease;opacity:.72}
    .ct-motion-cursor.is-hot{width:120px;height:120px;border-color:rgba(134,161,255,.56);opacity:1}

    .ct-motion-section{position:relative;isolation:isolate;--ct-section-progress:0;--ct-section-enter:0}
    .ct-motion-section:before{content:attr(data-ct-motion-index);position:absolute;z-index:-1;right:clamp(18px,4vw,68px);top:clamp(24px,5vw,86px);color:rgba(255,255,255,.026);font-size:clamp(90px,15vw,230px);font-weight:950;line-height:.75;letter-spacing:-.09em;pointer-events:none;transform:translate3d(0,calc((.5 - var(--ct-section-progress))*70px),0)}
    .ct-motion-section.ct-motion-enter{opacity:.18;transform:translate3d(0,54px,0) scale(.985);filter:blur(8px);transition:opacity .85s ease,transform 1s cubic-bezier(.16,1,.3,1),filter .85s ease}
    .ct-motion-section.ct-motion-enter.is-inview{opacity:1;transform:none;filter:none}
    .ct-motion-section[data-ct-motion-side='left'].ct-motion-enter{transform:translate3d(-46px,38px,0) scale(.986)}
    .ct-motion-section[data-ct-motion-side='right'].ct-motion-enter{transform:translate3d(46px,38px,0) scale(.986)}
    .ct-motion-section[data-ct-motion-side='left'].ct-motion-enter.is-inview,.ct-motion-section[data-ct-motion-side='right'].ct-motion-enter.is-inview{transform:none}

    .ct-motion-marquee{position:relative;z-index:8;overflow:hidden;border-top:1px solid rgba(255,255,255,.12);border-bottom:1px solid rgba(255,255,255,.12);background:#3b6fff;color:#fff;transform:rotate(-1.15deg) scale(1.025);box-shadow:0 22px 70px rgba(30,62,180,.23)}
    .ct-motion-marquee-track{display:flex;width:max-content;gap:34px;padding:18px 0;font-size:clamp(18px,2.25vw,34px);font-weight:950;letter-spacing:-.055em;text-transform:uppercase;animation:ctMarquee 24s linear infinite;will-change:transform}
    .ct-motion-marquee-track span{display:flex;align-items:center;gap:34px;white-space:nowrap}
    .ct-motion-marquee-track span:after{content:'✦';font-size:.6em;color:#cdd7ff}
    @keyframes ctMarquee{to{transform:translateX(-50%)}}

    #ct-pagero-intro .ct-v8-head{transform:translate3d(0,calc(var(--ct-v8-progress,0)*-30px),0)}
    #ct-pagero-intro .ct-v8-head h1{transform-origin:50% 50%;transform:scale(calc(1 - var(--ct-v8-progress,0)*.055));letter-spacing:calc(-.078em + var(--ct-v8-progress,0)*.012em)}
    #ct-pagero-intro .ct-v8-head h1 span{display:inline-block;background:linear-gradient(95deg,#7f9cff,#c2ceff,#668cff);background-size:220% 100%;-webkit-background-clip:text;background-clip:text;color:transparent;animation:ctHeadlineFlow 5s ease-in-out infinite}
    @keyframes ctHeadlineFlow{0%,100%{background-position:0 50%}50%{background-position:100% 50%}}
    #ct-pagero-intro .ct-v8-stage{transform:perspective(1500px) rotateX(calc(2deg - var(--ct-v8-progress,0)*2deg)) rotateY(calc((.5 - var(--ct-v8-progress,0))*2deg)) translate3d(0,calc((.5 - var(--ct-v8-progress,0))*30px),0);transform-origin:50% 20%;will-change:transform}
    #ct-pagero-intro .ct-v8-inquiry{transform:translate3d(calc(var(--ct-v8-progress,0)*-14px),calc((.5 - var(--ct-v8-progress,0))*12px),0)}
    #ct-pagero-intro .ct-v8-phone{transform:translate3d(calc(var(--ct-v8-progress,0)*14px),calc((var(--ct-v8-progress,0) - .5)*18px),0)}
    #ct-pagero-intro .ct-v8-push{animation:ctPushFloat 3.8s ease-in-out infinite}
    @keyframes ctPushFloat{0%,100%{transform:translate3d(-50%,0,0)}50%{transform:translate3d(-50%,-10px,0)}}

    #ct-pagero-intro .ct-industry-card{--ct-card-shift:0;transform:translate3d(0,calc(var(--ct-card-shift)*-22px),0) rotateX(calc(var(--ct-card-shift)*1.4deg));transform-origin:50% 100%;transition:filter .35s ease,opacity .35s ease,transform .22s linear}
    #ct-pagero-intro .ct-industry-card.is-ct-focus{filter:drop-shadow(0 28px 40px rgba(42,76,210,.2))}
    #ct-pagero-intro .ct-industry-phone{transition:border-color .35s ease,box-shadow .35s ease,transform .35s cubic-bezier(.2,.8,.2,1)}
    #ct-pagero-intro .ct-industry-card.is-ct-focus .ct-industry-phone{border-color:rgba(117,151,255,.68);box-shadow:0 38px 100px rgba(25,50,145,.28);transform:translateY(-8px)}

    .ct-motion-kinetic{overflow:hidden}
    .ct-motion-kinetic .hero-heading,.ct-motion-kinetic .web-heading-copy,.ct-motion-kinetic .ad-head{transform:translate3d(calc(var(--ct-motion-velocity)*-.08px),0,0)}
    .ct-motion-kinetic h1,.ct-motion-kinetic h2{will-change:transform}

    .ct-motion-magnetic{--ct-mx:0px;--ct-my:0px;transform:translate3d(var(--ct-mx),var(--ct-my),0);transition:transform .22s cubic-bezier(.2,.8,.2,1),box-shadow .25s ease}
    .ct-motion-magnetic:hover{box-shadow:0 16px 38px rgba(44,81,210,.24)}

    @media(max-width:900px){
      .ct-motion-progress,.ct-motion-cursor{display:none}
      .ct-motion-section:before{font-size:110px;right:14px;top:28px}
      .ct-motion-marquee{transform:rotate(-.7deg) scale(1.015)}
      .ct-motion-marquee-track{padding:14px 0;gap:24px;animation-duration:18s}
      #ct-pagero-intro .ct-v8-stage,#ct-pagero-intro .ct-v8-inquiry,#ct-pagero-intro .ct-v8-phone{transform:none!important}
      #ct-pagero-intro .ct-industry-card{transform:none!important}
    }
    @media(prefers-reduced-motion:reduce){
      html{scroll-behavior:auto!important}
      .ct-motion-section.ct-motion-enter,.ct-motion-section[data-ct-motion-side].ct-motion-enter{opacity:1!important;transform:none!important;filter:none!important;transition:none!important}
      .ct-motion-marquee-track,#ct-pagero-intro .ct-v8-head h1 span,#ct-pagero-intro .ct-v8-push{animation:none!important}
      .ct-motion-cursor,.ct-motion-progress{display:none!important}
      #ct-pagero-intro .ct-v8-stage,#ct-pagero-intro .ct-v8-inquiry,#ct-pagero-intro .ct-v8-phone,#ct-pagero-intro .ct-industry-card{transform:none!important}
    }
  `;

  const addStyle=()=>{
    if(document.querySelector('style[data-ct-immersive-motion]'))return;
    const style=document.createElement('style');
    style.dataset.ctImmersiveMotion='1';
    style.textContent=css;
    document.head.append(style);
  };

  const addMarquee=()=>{
    if(document.querySelector('.ct-motion-marquee'))return;
    const anchor=document.querySelector('#ct-pagero-intro .ct-v8-hero')||document.querySelector('.hero');
    if(!anchor)return;
    const marquee=document.createElement('div');
    marquee.className='ct-motion-marquee';
    marquee.setAttribute('aria-hidden','true');
    const phrase='문의 접수 · 고객 등록 · 통화 관리 · 자동 문자 · 재연락 일정 · 실시간 통계 · ';
    marquee.innerHTML=`<div class="ct-motion-marquee-track"><span>${phrase}</span><span>${phrase}</span><span>${phrase}</span><span>${phrase}</span></div>`;
    anchor.insertAdjacentElement('afterend',marquee);
  };

  const collectSections=()=>{
    const candidates=[
      ...document.querySelectorAll('#ct-pagero-intro > section'),
      ...document.querySelectorAll('body > section'),
      ...document.querySelectorAll('main > section'),
      ...document.querySelectorAll('section[id]'),
      document.querySelector('.hero')
    ].filter(Boolean);
    return [...new Set(candidates)].filter(section=>!section.closest('.ct-industry-screen,.ct-v8-screen,.phone-screen')&&section.getBoundingClientRect().height>220);
  };

  const setupSections=sections=>{
    sections.forEach((section,index)=>{
      section.classList.add('ct-motion-section','ct-motion-enter');
      section.dataset.ctMotionIndex=String(index+1).padStart(2,'0');
      section.dataset.ctMotionSide=index%3===1?'left':index%3===2?'right':'center';
    });
    const observer=new IntersectionObserver(entries=>entries.forEach(entry=>entry.target.classList.toggle('is-inview',entry.isIntersecting)),{rootMargin:'-8% 0px -12%',threshold:.08});
    sections.forEach(section=>observer.observe(section));
  };

  const setupProgress=sections=>{
    const meter=document.createElement('div');
    meter.className='ct-motion-meter';
    meter.setAttribute('aria-hidden','true');
    document.body.append(meter);
    if(innerWidth<=900)return;
    const nav=document.createElement('nav');
    nav.className='ct-motion-progress';
    nav.setAttribute('aria-label','페이지 구간 이동');
    sections.forEach((section,index)=>{
      const button=document.createElement('button');
      button.type='button';
      button.setAttribute('aria-label',`${index+1}번째 구간으로 이동`);
      button.addEventListener('click',()=>{
        if(window.ctLenis)window.ctLenis.scrollTo(section,{offset:-74,duration:1.15});
        else section.scrollIntoView({behavior:reduced.matches?'auto':'smooth',block:'start'});
      });
      nav.append(button);
    });
    document.body.append(nav);
  };

  const setupCursor=()=>{
    if(coarse.matches||innerWidth<=900||reduced.matches)return null;
    const cursor=document.createElement('div');
    cursor.className='ct-motion-cursor';
    cursor.setAttribute('aria-hidden','true');
    document.body.append(cursor);
    let x=-120,y=-120,tx=-120,ty=-120;
    addEventListener('pointermove',event=>{tx=event.clientX;ty=event.clientY;document.documentElement.style.setProperty('--ct-pointer-x',`${tx}px`);document.documentElement.style.setProperty('--ct-pointer-y',`${ty}px`);},{passive:true});
    document.addEventListener('pointerover',event=>cursor.classList.toggle('is-hot',Boolean(event.target.closest('a,button,.ct-industry-phone,.ct-v8-phone'))));
    const tick=()=>{x=lerp(x,tx,.17);y=lerp(y,ty,.17);cursor.style.setProperty('--ct-cursor-x',`${x}px`);cursor.style.setProperty('--ct-cursor-y',`${y}px`);requestAnimationFrame(tick)};
    tick();
    return cursor;
  };

  const setupMagnetic=()=>{
    if(coarse.matches||reduced.matches)return;
    document.querySelectorAll('a[class*="btn"],button[class*="btn"],.cta-button,.save-bar,.ct-hos-submit,.ct-est-submit,.ct-ins-submit').forEach(element=>{
      element.classList.add('ct-motion-magnetic');
      element.addEventListener('pointermove',event=>{
        const rect=element.getBoundingClientRect();
        element.style.setProperty('--ct-mx',`${(event.clientX-rect.left-rect.width/2)*.12}px`);
        element.style.setProperty('--ct-my',`${(event.clientY-rect.top-rect.height/2)*.12}px`);
      });
      element.addEventListener('pointerleave',()=>{element.style.setProperty('--ct-mx','0px');element.style.setProperty('--ct-my','0px')});
    });
  };

  const setupLenis=()=>{
    if(reduced.matches||coarse.matches||innerWidth<=900)return;
    const start=()=>{
      if(!window.Lenis||window.ctLenis)return;
      document.documentElement.classList.add('ct-lenis','ct-lenis-smooth');
      window.ctLenis=new window.Lenis({autoRaf:true,anchors:{offset:-72},lerp:.085,wheelMultiplier:.92,smoothWheel:true,allowNestedScroll:true});
    };
    if(window.Lenis){start();return;}
    const script=document.createElement('script');
    script.src='https://unpkg.com/lenis@1.3.23/dist/lenis.min.js';
    script.async=true;
    script.onload=start;
    document.head.append(script);
  };

  const setupScroll=sections=>{
    let last=scrollY;
    let velocity=0;
    let ticking=false;
    const update=()=>{
      const y=scrollY;
      velocity=lerp(velocity,y-last,.18);
      last=y;
      const limit=Math.max(1,document.documentElement.scrollHeight-innerHeight);
      const progress=clamp(y/limit);
      document.documentElement.style.setProperty('--ct-motion-progress',progress.toFixed(4));
      document.documentElement.style.setProperty('--ct-motion-velocity',Math.max(-40,Math.min(40,velocity)).toFixed(2));

      let activeIndex=0;
      let closest=Infinity;
      sections.forEach((section,index)=>{
        const rect=section.getBoundingClientRect();
        const sectionProgress=clamp((innerHeight-rect.top)/(innerHeight+rect.height));
        section.style.setProperty('--ct-section-progress',sectionProgress.toFixed(4));
        const distance=Math.abs(rect.top-innerHeight*.34);
        if(distance<closest){closest=distance;activeIndex=index}
      });
      document.querySelectorAll('.ct-motion-progress button').forEach((button,index)=>button.classList.toggle('is-active',index===activeIndex));

      const v8=document.querySelector('#ct-pagero-intro .ct-v8-hero');
      if(v8){const rect=v8.getBoundingClientRect();v8.style.setProperty('--ct-v8-progress',clamp(-rect.top/Math.max(1,rect.height-innerHeight*.35)).toFixed(4))}

      const cards=[...document.querySelectorAll('#ct-pagero-intro .ct-industry-card')];
      cards.forEach((card,index)=>{
        const rect=card.getBoundingClientRect();
        const focus=clamp(1-Math.abs(rect.top+rect.height*.5-innerHeight*.53)/(innerHeight*.7));
        card.style.setProperty('--ct-card-shift',focus.toFixed(3));
        card.classList.toggle('is-ct-focus',focus>.62);
      });
      ticking=false;
    };
    const requestUpdate=()=>{if(!ticking){ticking=true;requestAnimationFrame(update)}};
    addEventListener('scroll',requestUpdate,{passive:true});
    addEventListener('resize',requestUpdate,{passive:true});
    requestUpdate();
  };

  const init=()=>{
    addStyle();
    document.body.classList.add('ct-immersive-ready','ct-motion-kinetic');
    addMarquee();
    const sections=collectSections();
    setupSections(sections);
    setupProgress(sections);
    setupCursor();
    setupMagnetic();
    setupLenis();
    setupScroll(sections);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(init),{once:true});
  else requestAnimationFrame(init);
})();
