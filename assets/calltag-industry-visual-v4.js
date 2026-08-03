(()=>{
  if(document.documentElement.dataset.ctIndustryVisualV4)return;
  document.documentElement.dataset.ctIndustryVisualV4='1';

  const items=[
    {industry:'보험',title:'상담 신청',type:'insurance',size:'xl',x:38,y:53,alpha:1,delay:.00,dx:-180,dy:120,rot:-8,ex:-120,ey:-150,er:-8},
    {industry:'병원',title:'진료 예약',type:'clinic',size:'xl',x:63,y:52,alpha:.98,delay:.06,dx:180,dy:105,rot:8,ex:130,ey:-165,er:9},
    {industry:'부동산',title:'매물 문의',type:'estate',size:'md',x:15,y:31,alpha:.82,delay:.14,dx:-170,dy:-80,rot:-9,ex:-145,ey:-95,er:-10},
    {industry:'학원',title:'상담 예약',type:'academy',size:'sm',x:84,y:28,alpha:.68,delay:.22,dx:160,dy:-90,rot:10,ex:155,ey:-110,er:11},
    {industry:'미용실',title:'시술 예약',type:'salon',size:'sm',x:10,y:72,alpha:.62,delay:.28,dx:-170,dy:115,rot:-11,ex:-165,ey:-75,er:-12},
    {industry:'자동차',title:'정비 문의',type:'auto',size:'md',x:86,y:70,alpha:.82,delay:.18,dx:185,dy:105,rot:9,ex:160,ey:-90,er:10},
    {industry:'쇼핑몰',title:'상품 문의',type:'shop',size:'md',x:49,y:83,alpha:.88,delay:.11,dx:0,dy:185,rot:2,ex:18,ey:-180,er:4},
    {industry:'인테리어',title:'견적 신청',type:'interior',size:'sm',x:73,y:83,alpha:.7,delay:.3,dx:120,dy:160,rot:7,ex:115,ey:-135,er:8}
  ];

  const thumbs={
    insurance:`<div class="ct-v4-mini insurance"><div class="top"><span>월 예상 보험료</span><b>84,000원</b></div><div class="bars"><i></i><i></i><i></i><i></i></div><div class="notice">보장 분석 완료</div></div>`,
    clinic:`<div class="ct-v4-mini clinic"><div class="doctor"><i></i><span><b>김온유 원장</b><small>진료 예약</small></span></div><div class="dates"><i>2</i><i class="on">3</i><i>4</i><i>5</i><i>6</i></div><div class="time">오전 10:30 예약 가능</div></div>`,
    estate:`<div class="ct-v4-mini estate"><div class="house"><i></i><i></i><i></i></div><div class="estate-copy"><span>시티뷰 리버파크 84㎡</span><b>8억 4,000</b></div></div>`,
    academy:`<div class="ct-v4-mini academy"><div class="people"><i></i><i></i><i></i></div><div class="lesson"><span>수학 상담</span><b>오늘 18:30</b></div><div class="line"></div></div>`,
    salon:`<div class="ct-v4-mini salon"><div class="looks"><i></i><i></i><i></i></div><div class="salon-row"><span>커트 · 컬러</span><b>예약</b></div></div>`,
    auto:`<div class="ct-v4-mini auto"><div class="car"><i></i><span></span></div><div class="checks"><span><i></i>엔진오일</span><span><i></i>타이어</span></div><div class="status">정비 접수 완료</div></div>`,
    shop:`<div class="ct-v4-mini shop"><div class="products"><i></i><i></i><i></i></div><div class="order"><span>상품 문의 3건</span><b>확인</b></div></div>`,
    interior:`<div class="ct-v4-mini interior"><div class="room"><i></i><i></i><i></i><span></span></div><div class="quote"><span>32평 견적</span><b>상담 요청</b></div></div>`
  };

  const style=document.createElement('style');
  style.dataset.ctIndustryVisualV4='1';
  style.textContent=`
    .ct-industry-v4{position:relative!important;height:205svh!important;min-height:1420px!important;padding:0!important;overflow:visible!important;background:linear-gradient(180deg,#070a13 0%,#0b1021 50%,#070a13 100%)!important;border-block:1px solid rgba(255,255,255,.07)!important;isolation:isolate}
    .ct-industry-v4:before,.ct-industry-v4:after{content:'';position:absolute;width:560px;height:560px;border-radius:50%;filter:blur(76px);pointer-events:none;opacity:.68}.ct-industry-v4:before{left:7%;top:27%;background:radial-gradient(circle,rgba(53,91,255,.24),transparent 68%)}.ct-industry-v4:after{right:6%;top:45%;background:radial-gradient(circle,rgba(98,68,255,.18),transparent 68%)}
    .ct-industry-v4__sticky{position:sticky;top:var(--ct-horizontal-header,68px);height:calc(100svh - var(--ct-horizontal-header,68px));min-height:640px;display:grid;grid-template-rows:auto minmax(0,1fr);padding:34px 0 18px;overflow:hidden;box-sizing:border-box}
    .ct-industry-v4__head{position:relative;z-index:30;width:min(1180px,calc(100% - 48px));margin:0 auto;text-align:center;will-change:transform,opacity}.ct-industry-v4__head h2{margin:0;color:#f7f8fc;font-size:clamp(42px,4.8vw,68px);line-height:1;letter-spacing:-.07em;white-space:nowrap}
    .ct-industry-v4__stage{position:relative;width:min(1320px,calc(100% - 32px));height:100%;min-height:550px;margin:0 auto;perspective:1500px}.ct-industry-v4__stage:before{content:'';position:absolute;inset:5% 7%;background:radial-gradient(ellipse at center,rgba(74,110,255,.19),transparent 64%);filter:blur(28px);pointer-events:none}
    .ct-industry-v4__card{position:absolute;left:var(--x);top:var(--y);z-index:var(--z,4);width:var(--w,250px);height:var(--h,190px);opacity:0;transform:translate(-50%,-50%);will-change:transform,opacity,filter;transform-style:preserve-3d}.ct-industry-v4__card.is-xl{--w:330px;--h:250px;--z:9}.ct-industry-v4__card.is-md{--w:270px;--h:205px;--z:6}.ct-industry-v4__card.is-sm{--w:220px;--h:166px;--z:4}
    .ct-industry-v4__inner{width:100%;height:100%;display:grid;grid-template-rows:minmax(0,1fr) auto;overflow:hidden;border:1px solid rgba(255,255,255,.14);border-radius:24px;background:linear-gradient(145deg,rgba(25,31,48,.97),rgba(11,15,27,.95));box-shadow:0 27px 78px rgba(0,0,0,.43);backdrop-filter:blur(18px);transition:transform .4s cubic-bezier(.16,1,.3,1),border-color .3s ease,box-shadow .4s ease,filter .3s ease}.ct-industry-v4__thumb{position:relative;min-height:0;overflow:hidden;border-bottom:1px solid rgba(255,255,255,.09);background:#0e1321}.ct-industry-v4__meta{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:13px 15px 14px}.ct-industry-v4__meta span{padding:6px 9px;border-radius:999px;background:rgba(80,116,255,.15);color:#9db0ff;font-size:10px;font-weight:900}.ct-industry-v4__meta strong{color:#f3f5fb;font-size:clamp(16px,1.4vw,22px);line-height:1;letter-spacing:-.045em;white-space:nowrap}
    .ct-industry-v4__card.is-settled .ct-industry-v4__inner{animation:ctV4Float var(--float-duration,6s) ease-in-out var(--float-delay,0s) infinite alternate}@keyframes ctV4Float{from{translate:0 0;rotate:-.16deg}to{translate:0 -8px;rotate:.16deg}}
    @media(hover:hover) and (pointer:fine){.ct-industry-v4__card:hover{z-index:30!important;opacity:1!important}.ct-industry-v4__card:hover .ct-industry-v4__inner{animation-play-state:paused;transform:translateY(-7px) scale(1.055);border-color:rgba(118,148,255,.58);box-shadow:0 40px 112px rgba(30,56,160,.34);filter:brightness(1.08)}}

    .ct-v4-mini{position:absolute;inset:0;padding:14px;box-sizing:border-box;color:#fff;font-size:10px}.ct-v4-mini b,.ct-v4-mini span,.ct-v4-mini small{position:relative;z-index:2}.ct-v4-mini .top{display:flex;align-items:flex-end;justify-content:space-between}.ct-v4-mini .top span{color:#8d96a9}.ct-v4-mini .top b{font-size:20px}.ct-v4-mini.insurance{background:linear-gradient(155deg,#172651,#0d1427)}.ct-v4-mini .bars{height:64px;display:flex;align-items:flex-end;gap:7px;margin-top:17px;padding:10px;border-radius:12px;background:rgba(255,255,255,.045)}.ct-v4-mini .bars i{flex:1;border-radius:5px 5px 2px 2px;background:linear-gradient(#7699ff,#315edc)}.ct-v4-mini .bars i:nth-child(1){height:38%}.ct-v4-mini .bars i:nth-child(2){height:72%}.ct-v4-mini .bars i:nth-child(3){height:56%}.ct-v4-mini .bars i:nth-child(4){height:88%}.ct-v4-mini .notice{margin-top:9px;padding:8px 10px;border-radius:9px;background:rgba(73,119,255,.16);color:#aabaff;font-weight:850}
    .ct-v4-mini.clinic{background:linear-gradient(155deg,#e8fbf7,#c9f1e8);color:#17342f}.ct-v4-mini .doctor{display:flex;gap:9px;align-items:center}.ct-v4-mini .doctor>i{width:38px;height:38px;border-radius:12px;background:linear-gradient(#63c7b5,#3fa995)}.ct-v4-mini .doctor b,.ct-v4-mini .doctor small{display:block}.ct-v4-mini .doctor small{margin-top:3px;color:#557a72}.ct-v4-mini .dates{display:grid;grid-template-columns:repeat(5,1fr);gap:5px;margin-top:15px}.ct-v4-mini .dates i{height:29px;display:grid;place-items:center;border-radius:8px;background:rgba(255,255,255,.7);font-style:normal}.ct-v4-mini .dates i.on{background:#16a085;color:#fff}.ct-v4-mini .time{margin-top:9px;padding:8px;border-radius:8px;background:rgba(255,255,255,.6);font-weight:850}
    .ct-v4-mini.estate{background:linear-gradient(#d9a66f 0 59%,#f4eee6 59%);color:#4a2e1d}.ct-v4-mini .house{position:relative;height:82px}.ct-v4-mini .house i:first-child{position:absolute;left:20%;right:20%;bottom:11px;height:48px;background:#fff0db}.ct-v4-mini .house i:nth-child(2){position:absolute;left:15%;right:15%;bottom:56px;height:35px;background:#fff0db;clip-path:polygon(50% 0,100% 100%,0 100%)}.ct-v4-mini .house i:nth-child(3){position:absolute;left:47%;bottom:11px;width:18px;height:32px;background:#bd7539}.ct-v4-mini .estate-copy{display:flex;justify-content:space-between;gap:8px;margin-top:11px}.ct-v4-mini .estate-copy span{font-weight:800}.ct-v4-mini .estate-copy b{color:#b76b2b}
    .ct-v4-mini.academy{background:linear-gradient(145deg,#27214b,#14182c)}.ct-v4-mini .people{display:flex;justify-content:center;gap:8px;margin-top:12px}.ct-v4-mini .people i{width:30px;height:30px;border-radius:50%;background:#7569d8}.ct-v4-mini .people i:nth-child(2){width:38px;height:38px;background:#9b8cf4}.ct-v4-mini .lesson{display:flex;justify-content:space-between;margin-top:18px}.ct-v4-mini .lesson b{color:#b9adff}.ct-v4-mini .line{height:7px;margin-top:12px;border-radius:999px;background:linear-gradient(90deg,#836ff0 65%,rgba(255,255,255,.09) 65%)}
    .ct-v4-mini.salon{background:linear-gradient(150deg,#4b2137,#221322)}.ct-v4-mini .looks{display:flex;gap:7px;height:76px}.ct-v4-mini .looks i{flex:1;border-radius:12px;background:linear-gradient(160deg,#e090b8,#7b3459)}.ct-v4-mini .looks i:nth-child(2){background:linear-gradient(160deg,#f0b389,#8f4e45)}.ct-v4-mini .looks i:nth-child(3){background:linear-gradient(160deg,#c987dc,#603974)}.ct-v4-mini .salon-row{display:flex;justify-content:space-between;margin-top:12px}.ct-v4-mini .salon-row b{color:#ffb9d8}
    .ct-v4-mini.auto{background:linear-gradient(145deg,#172329,#0d151a)}.ct-v4-mini .car{position:relative;height:64px}.ct-v4-mini .car>i{position:absolute;left:15%;right:15%;top:22px;height:25px;border-radius:13px 13px 7px 7px;background:#56b79e}.ct-v4-mini .car>span{position:absolute;left:27%;right:27%;top:8px;height:25px;border-radius:20px 20px 4px 4px;background:#376f63}.ct-v4-mini .checks{display:flex;gap:7px}.ct-v4-mini .checks span{flex:1;padding:7px;border-radius:8px;background:rgba(255,255,255,.055)}.ct-v4-mini .checks i{display:inline-block;width:6px;height:6px;margin-right:4px;border-radius:50%;background:#69d7b4}.ct-v4-mini .status{margin-top:8px;color:#79dfbd;font-weight:850}
    .ct-v4-mini.shop{background:linear-gradient(150deg,#281b48,#15152a)}.ct-v4-mini .products{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;height:74px}.ct-v4-mini .products i{border-radius:10px;background:linear-gradient(#8b73e6,#49358e)}.ct-v4-mini .products i:nth-child(2){background:linear-gradient(#f090bc,#85405e)}.ct-v4-mini .products i:nth-child(3){background:linear-gradient(#6cb7ea,#315e8a)}.ct-v4-mini .order{display:flex;justify-content:space-between;margin-top:12px}.ct-v4-mini .order b{color:#b9a8ff}
    .ct-v4-mini.interior{background:linear-gradient(150deg,#40352b,#211b17)}.ct-v4-mini .room{position:relative;height:74px;border-radius:10px;background:#c9b49c;overflow:hidden}.ct-v4-mini .room i:first-child{position:absolute;left:10px;bottom:9px;width:45%;height:24px;background:#7f6653}.ct-v4-mini .room i:nth-child(2){position:absolute;right:12px;bottom:9px;width:28%;height:38px;background:#e2d3c1}.ct-v4-mini .room i:nth-child(3){position:absolute;left:12px;top:10px;width:34%;height:14px;background:#eadfce}.ct-v4-mini .quote{display:flex;justify-content:space-between;margin-top:12px}.ct-v4-mini .quote b{color:#e3c29d}

    @media(max-width:900px){.ct-industry-v4{height:auto!important;min-height:0!important;padding:88px 0!important;overflow:hidden!important}.ct-industry-v4__sticky{position:relative!important;top:auto!important;height:auto!important;min-height:0!important;display:block!important;padding:0!important;overflow:visible!important}.ct-industry-v4__head{width:calc(100% - 32px)!important;margin:0 auto 28px!important;text-align:left!important}.ct-industry-v4__head h2{font-size:36px!important;text-align:left!important}.ct-industry-v4__stage{width:calc(100% - 32px)!important;height:auto!important;min-height:0!important;display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:12px!important}.ct-industry-v4__stage:before{display:none!important}.ct-industry-v4__card,.ct-industry-v4__card.is-xl,.ct-industry-v4__card.is-md,.ct-industry-v4__card.is-sm{position:relative!important;left:auto!important;top:auto!important;width:100%!important;height:190px!important;transform:none}.ct-industry-v4__inner{border-radius:18px!important}.ct-industry-v4__meta{padding:11px 12px 12px!important}.ct-industry-v4__meta strong{font-size:16px!important}.ct-industry-v4__meta span{font-size:9px!important}}
    @media(max-width:560px){.ct-industry-v4__head,.ct-industry-v4__stage{width:calc(100% - 28px)!important}.ct-industry-v4__stage{grid-template-columns:1fr!important}.ct-industry-v4__card,.ct-industry-v4__card.is-xl,.ct-industry-v4__card.is-md,.ct-industry-v4__card.is-sm{height:220px!important}}
    @media(prefers-reduced-motion:reduce){.ct-industry-v4__card{opacity:var(--alpha,1)!important;transform:translate(-50%,-50%)!important}.ct-industry-v4__inner{animation:none!important}.ct-industry-v4__head{opacity:1!important;transform:none!important}}
    @media(max-width:900px) and (prefers-reduced-motion:reduce){.ct-industry-v4__card{transform:none!important}}
  `;
  document.head.append(style);

  const markup=`<div class="ct-industry-v4__sticky"><div class="ct-industry-v4__head"><h2>업종별 문의 화면</h2></div><div class="ct-industry-v4__stage">${items.map((item,index)=>`<article class="ct-industry-v4__card is-${item.size}" style="--x:${item.x}%;--y:${item.y}%;--alpha:${item.alpha};--float-duration:${5.5+(index%4)*.55}s;--float-delay:${-((index%5)*.72)}s" data-alpha="${item.alpha}" data-delay="${item.delay}" data-dx="${item.dx}" data-dy="${item.dy}" data-rot="${item.rot}" data-ex="${item.ex}" data-ey="${item.ey}" data-er="${item.er}"><div class="ct-industry-v4__inner"><div class="ct-industry-v4__thumb">${thumbs[item.type]}</div><div class="ct-industry-v4__meta"><span>${item.industry}</span><strong>${item.title}</strong></div></div></article>`).join('')}</div></div>`;

  let section=null;
  let sticky=null;
  let head=null;
  let cards=[];
  let raf=0;
  const mobile=matchMedia('(max-width:900px)');
  const reduce=matchMedia('(prefers-reduced-motion:reduce)').matches;
  const clamp=value=>Math.max(0,Math.min(1,value));
  const easeOut=value=>1-Math.pow(1-value,3);
  const easeIn=value=>value*value*value;

  const mount=()=>{
    const target=document.querySelector('#ct-pagero-intro .ct-industry-visual-section,#ct-pagero-intro .ct-industry-float-section,#ct-pagero-intro .ct-industries-static,#ct-pagero-intro .ct-horizontal-industries-clean');
    if(!target)return false;
    if(target.dataset.ctIndustryV4Mounted==='1')return true;
    target.className='ct-industry-v4';
    target.removeAttribute('style');
    target.innerHTML=markup;
    target.dataset.ctIndustryV4Mounted='1';
    section=target;
    sticky=target.querySelector('.ct-industry-v4__sticky');
    head=target.querySelector('.ct-industry-v4__head');
    cards=[...target.querySelectorAll('.ct-industry-v4__card')];
    if(reduce){
      cards.forEach(card=>{card.style.opacity=card.dataset.alpha||'1';card.classList.add('is-settled')});
      head.style.opacity='1';
    }else requestRender();
    dispatchEvent(new Event('resize'));
    return true;
  };

  const progress=()=>{
    if(!section)return 0;
    const rect=section.getBoundingClientRect();
    const vh=innerHeight||document.documentElement.clientHeight;
    if(mobile.matches)return clamp((vh*.92-rect.top)/(vh*.92+rect.height*.72));
    const top=rect.top+scrollY;
    const range=Math.max(1,section.offsetHeight-(sticky?.offsetHeight||innerHeight));
    return clamp((scrollY-top)/range);
  };

  const render=()=>{
    raf=0;
    if(!section||!cards.length)return;
    const p=progress();
    const isMobile=mobile.matches;
    const titleIn=easeOut(clamp(p/.16));
    const titleOut=easeIn(clamp((p-.8)/.18));
    head.style.opacity=(titleIn*(1-titleOut)).toFixed(3);
    head.style.transform=`translate3d(0,${((1-titleIn)*28-titleOut*38).toFixed(2)}px,0) scale(${(.96+titleIn*.04-titleOut*.03).toFixed(4)})`;

    cards.forEach(card=>{
      const alpha=parseFloat(card.dataset.alpha||'.8');
      const delay=parseFloat(card.dataset.delay||'0');
      const dx=parseFloat(card.dataset.dx||'0');
      const dy=parseFloat(card.dataset.dy||'80');
      const rot=parseFloat(card.dataset.rot||'0');
      const ex=parseFloat(card.dataset.ex||'0');
      const ey=parseFloat(card.dataset.ey||'-120');
      const er=parseFloat(card.dataset.er||'0');
      const enter=easeOut(clamp((p-delay)/.34));
      const leave=easeIn(clamp((p-.72)/.28));
      const visibility=enter*(1-leave);
      const lift=clamp((p-.2)/.48)*(isMobile?12:34);
      const x=(isMobile?0:dx*(1-enter)+ex*leave);
      const y=(isMobile?(1-enter)*36-leave*42:dy*(1-enter)-lift+ey*leave);
      const rotation=isMobile?0:rot*(1-enter)+er*leave;
      const startScale=isMobile?.97:.86;
      const scale=startScale+(1-startScale)*enter-.1*leave;
      const blur=(1-enter)*2.4+leave*3.2;
      card.style.opacity=(alpha*visibility).toFixed(3);
      card.style.filter=`blur(${blur.toFixed(2)}px) brightness(${(.8+enter*.2-leave*.14).toFixed(3)})`;
      card.style.transform=isMobile
        ?`translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) scale(${scale.toFixed(4)})`
        :`translate(-50%,-50%) translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) rotate(${rotation.toFixed(2)}deg) scale(${scale.toFixed(4)})`;
      card.classList.toggle('is-settled',enter>.96&&leave<.04);
    });
  };

  const requestRender=()=>{if(!raf&&!reduce)raf=requestAnimationFrame(render)};
  const boot=()=>{
    if(!mount()){
      const observer=new MutationObserver(()=>{if(mount())observer.disconnect()});
      observer.observe(document.documentElement,{childList:true,subtree:true});
      setTimeout(()=>observer.disconnect(),14000);
    }
    addEventListener('scroll',requestRender,{passive:true});
    addEventListener('resize',requestRender,{passive:true});
    addEventListener('pageshow',requestRender,{passive:true});
    mobile.addEventListener?.('change',requestRender);
    [100,350,800,1600,3000].forEach(delay=>setTimeout(()=>{mount();requestRender()},delay));
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
