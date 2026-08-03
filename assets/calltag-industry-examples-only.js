(()=>{
  if(document.documentElement.dataset.ctIndustryFloatV2)return;
  document.documentElement.dataset.ctIndustryFloatV2='1';

  const cards=[
    {industry:'보험',use:'상담 신청',size:'xl',x:39,y:46,alpha:.95,delay:.00,rise:125,lift:42},
    {industry:'병원',use:'진료 예약',size:'xl',x:63,y:50,alpha:.91,delay:.08,rise:108,lift:50},
    {industry:'부동산',use:'매물 문의',size:'md',x:17,y:30,alpha:.79,delay:.18,rise:92,lift:34},
    {industry:'학원',use:'상담 예약',size:'sm',x:83,y:28,alpha:.56,delay:.27,rise:78,lift:26},
    {industry:'미용실',use:'시술 예약',size:'sm',x:11,y:68,alpha:.48,delay:.34,rise:70,lift:24},
    {industry:'자동차',use:'정비 문의',size:'md',x:85,y:70,alpha:.74,delay:.22,rise:96,lift:38},
    {industry:'법무',use:'상담 접수',size:'sm',x:29,y:79,alpha:.53,delay:.41,rise:74,lift:28},
    {industry:'인테리어',use:'견적 신청',size:'sm',x:72,y:82,alpha:.61,delay:.31,rise:84,lift:30},
    {industry:'쇼핑몰',use:'상품 문의',size:'md',x:50,y:83,alpha:.83,delay:.13,rise:104,lift:44},
    {industry:'여행',use:'예약 상담',size:'sm',x:51,y:20,alpha:.46,delay:.38,rise:68,lift:22}
  ];

  const style=document.createElement('style');
  style.dataset.ctIndustryFloatV2='1';
  style.textContent=`
    .ct-industry-float-section{
      position:relative!important;
      height:auto!important;
      min-height:0!important;
      padding:clamp(150px,12vw,220px) 0!important;
      overflow:hidden!important;
      border-top:1px solid rgba(255,255,255,.075)!important;
      border-bottom:1px solid rgba(255,255,255,.075)!important;
      background:
        radial-gradient(circle at 35% 42%,rgba(61,102,255,.16),transparent 31%),
        radial-gradient(circle at 68% 54%,rgba(84,126,255,.12),transparent 30%),
        linear-gradient(180deg,#080a10,#090c15)!important;
      isolation:isolate;
    }
    .ct-industry-float-section:before{
      content:'';
      position:absolute;
      inset:13% 14%;
      z-index:-1;
      border-radius:50%;
      background:radial-gradient(circle,rgba(73,112,255,.13),transparent 67%);
      filter:blur(55px);
      pointer-events:none;
    }
    .ct-industry-float__stage{
      position:relative;
      width:min(1180px,calc(100% - 48px));
      height:720px;
      margin:0 auto;
    }
    .ct-industry-float__title{
      position:absolute!important;
      width:1px!important;
      height:1px!important;
      padding:0!important;
      margin:-1px!important;
      overflow:hidden!important;
      clip:rect(0,0,0,0)!important;
      white-space:nowrap!important;
      border:0!important;
    }
    .ct-industry-float__card{
      position:absolute;
      left:var(--x);
      top:var(--y);
      z-index:var(--z,2);
      width:var(--w,210px);
      min-height:var(--h,112px);
      opacity:0;
      transform:translate3d(-50%,var(--rise,90px),0) scale(.91);
      will-change:transform,opacity;
      pointer-events:auto;
    }
    .ct-industry-float__inner{
      position:relative;
      display:flex;
      flex-direction:column;
      justify-content:space-between;
      width:100%;
      min-height:inherit;
      padding:18px 20px;
      overflow:hidden;
      border:1px solid rgba(255,255,255,.11);
      border-radius:20px;
      background:linear-gradient(145deg,rgba(24,29,43,.92),rgba(11,15,25,.88));
      box-shadow:0 22px 64px rgba(0,0,0,.28),inset 0 1px 0 rgba(255,255,255,.055);
      backdrop-filter:blur(16px);
      -webkit-backdrop-filter:blur(16px);
      transition:border-color .35s ease,box-shadow .35s ease,background .35s ease;
    }
    .ct-industry-float__inner:before{
      content:'';
      position:absolute;
      inset:-45% auto auto -20%;
      width:150px;
      height:150px;
      border-radius:50%;
      background:radial-gradient(circle,rgba(101,139,255,.2),transparent 68%);
      pointer-events:none;
    }
    .ct-industry-float__card span{
      position:relative;
      display:inline-flex;
      align-items:center;
      align-self:flex-start;
      min-height:28px;
      padding:0 10px;
      border-radius:999px;
      background:rgba(83,121,255,.14);
      color:#9bb0ff;
      font-size:11px;
      font-weight:900;
      letter-spacing:-.02em;
    }
    .ct-industry-float__card strong{
      position:relative;
      display:block;
      margin-top:18px;
      color:#f7f8fc;
      font-size:clamp(20px,1.7vw,27px);
      line-height:1.08;
      letter-spacing:-.055em;
    }
    .ct-industry-float__card.is-xl{--w:290px;--h:158px;--z:4}
    .ct-industry-float__card.is-md{--w:226px;--h:126px;--z:3}
    .ct-industry-float__card.is-sm{--w:178px;--h:104px;--z:2}
    .ct-industry-float__card.is-sm strong{font-size:20px}
    .ct-industry-float__card.is-settled .ct-industry-float__inner{
      animation:ctIndustrySoftFloat var(--float-duration,6s) ease-in-out var(--float-delay,0s) infinite alternate;
    }
    @media(hover:hover) and (pointer:fine){
      .ct-industry-float__card:hover{z-index:10!important;opacity:1!important}
      .ct-industry-float__card:hover .ct-industry-float__inner{
        animation-play-state:paused;
        border-color:rgba(111,145,255,.5);
        background:linear-gradient(145deg,rgba(31,39,62,.97),rgba(13,18,31,.95));
        box-shadow:0 32px 90px rgba(34,67,176,.26),inset 0 1px 0 rgba(255,255,255,.1);
        transform:translateY(-8px) scale(1.025);
      }
    }
    @keyframes ctIndustrySoftFloat{
      from{transform:translate3d(0,0,0) rotate(-.12deg)}
      to{transform:translate3d(0,-9px,0) rotate(.12deg)}
    }
    @media(max-width:900px){
      .ct-industry-float-section{padding:120px 0!important}
      .ct-industry-float__stage{
        width:min(720px,calc(100% - 32px));
        height:auto;
        display:grid;
        grid-template-columns:repeat(2,minmax(0,1fr));
        gap:14px;
      }
      .ct-industry-float__card,
      .ct-industry-float__card.is-xl,
      .ct-industry-float__card.is-md,
      .ct-industry-float__card.is-sm{
        position:relative;
        left:auto;
        top:auto;
        width:100%;
        min-height:118px;
        transform:translate3d(0,var(--rise,48px),0) scale(.97);
      }
      .ct-industry-float__inner{min-height:118px;border-radius:17px}
      .ct-industry-float__card strong,.ct-industry-float__card.is-sm strong{font-size:21px}
    }
    @media(max-width:540px){
      .ct-industry-float-section{padding:96px 0!important}
      .ct-industry-float__stage{grid-template-columns:1fr;gap:12px}
      .ct-industry-float__card,
      .ct-industry-float__card.is-xl,
      .ct-industry-float__card.is-md,
      .ct-industry-float__card.is-sm{min-height:104px}
      .ct-industry-float__inner{min-height:104px;padding:16px 18px}
    }
    @media(prefers-reduced-motion:reduce){
      .ct-industry-float__card{opacity:var(--alpha,1)!important;transform:translate3d(-50%,0,0) scale(1)!important}
      .ct-industry-float__card .ct-industry-float__inner{animation:none!important}
    }
    @media(max-width:900px) and (prefers-reduced-motion:reduce){
      .ct-industry-float__card{transform:none!important}
    }
  `;
  document.head.append(style);

  const markup=`<h2 class="ct-industry-float__title">업종별 활용 예시</h2><div class="ct-industry-float__stage">${cards.map((card,index)=>`<article class="ct-industry-float__card is-${card.size}" style="--x:${card.x}%;--y:${card.y}%;--alpha:${card.alpha};--rise:${card.rise}px;--z:${card.size==='xl'?4:card.size==='md'?3:2};--float-duration:${5.4+(index%4)*.55}s;--float-delay:${(index%5)*-.7}s" data-alpha="${card.alpha}" data-delay="${card.delay}" data-rise="${card.rise}" data-lift="${card.lift}"><div class="ct-industry-float__inner"><span>${card.industry}</span><strong>${card.use}</strong></div></article>`).join('')}</div>`;

  let section=null;
  let cardNodes=[];
  let maxProgress=0;
  let ticking=false;
  const reduce=matchMedia('(prefers-reduced-motion:reduce)').matches;
  const mobile=matchMedia('(max-width:900px)');
  const clamp=value=>Math.max(0,Math.min(1,value));

  const mount=()=>{
    const target=document.querySelector('#ct-pagero-intro .ct-industries-static,#ct-pagero-intro .ct-horizontal-industries-clean');
    if(!target)return false;
    if(target.dataset.ctIndustryFloatMounted==='1')return true;

    target.className='ct-industry-float-section';
    target.removeAttribute('style');
    target.innerHTML=markup;
    target.dataset.ctIndustryFloatMounted='1';
    section=target;
    cardNodes=[...target.querySelectorAll('.ct-industry-float__card')];

    if(reduce){
      cardNodes.forEach(card=>{card.style.opacity=card.dataset.alpha||'1';card.classList.add('is-settled')});
      return true;
    }

    requestRender();
    return true;
  };

  const getProgress=()=>{
    if(!section)return 0;
    const rect=section.getBoundingClientRect();
    const viewport=innerHeight||document.documentElement.clientHeight;
    const start=viewport*.94;
    const end=-rect.height*.18;
    return clamp((start-rect.top)/(start-end));
  };

  const render=()=>{
    ticking=false;
    if(!section||!cardNodes.length)return;

    const progress=getProgress();
    maxProgress=Math.max(maxProgress,progress);
    const isMobile=mobile.matches;
    const startScale=isMobile ? .97 : .91;

    cardNodes.forEach((card,index)=>{
      const alpha=parseFloat(card.dataset.alpha||'.8');
      const delay=parseFloat(card.dataset.delay||'0');
      const rise=isMobile?48:parseFloat(card.dataset.rise||'90');
      const lift=isMobile?12:parseFloat(card.dataset.lift||'30');
      const local=clamp((maxProgress-delay)/(1-delay));
      const eased=1-Math.pow(1-local,3);
      const scale=startScale+(1-startScale)*eased;
      const y=(1-eased)*rise-progress*lift;
      const x=isMobile?'0':'-50%';

      card.style.opacity=(alpha*eased).toFixed(3);
      card.style.transform=`translate3d(${x},${y.toFixed(2)}px,0) scale(${scale.toFixed(4)})`;
      if(local>.94)card.classList.add('is-settled');
      card.style.setProperty('--float-delay',`${-((index%5)*.63)}s`);
    });
  };

  const requestRender=()=>{
    if(ticking||reduce)return;
    ticking=true;
    requestAnimationFrame(render);
  };

  const boot=()=>{
    if(!mount()){
      const observer=new MutationObserver(()=>{
        if(mount()){
          observer.disconnect();
          requestRender();
        }
      });
      observer.observe(document.documentElement,{childList:true,subtree:true});
      setTimeout(()=>observer.disconnect(),12000);
    }
    addEventListener('scroll',requestRender,{passive:true});
    addEventListener('resize',requestRender,{passive:true});
    mobile.addEventListener?.('change',requestRender);
    setTimeout(requestRender,250);
    setTimeout(requestRender,900);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
