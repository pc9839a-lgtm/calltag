(()=>{
  if(document.documentElement.dataset.ctIndustryVisualV5)return;
  document.documentElement.dataset.ctIndustryVisualV5='1';

  const data=[
    ['보험','상담 신청','insurance','xl',38,54,1,0,-170,120,-8,-120,-145,-8],
    ['병원','진료 예약','clinic','xl',63,53,.98,.06,175,110,8,130,-155,9],
    ['부동산','매물 문의','estate','md',15,31,.84,.14,-165,-75,-9,-145,-90,-10],
    ['학원','상담 예약','academy','sm',84,28,.68,.22,160,-85,10,150,-105,11],
    ['미용실','시술 예약','salon','sm',10,72,.62,.28,-165,110,-11,-160,-70,-12],
    ['자동차','정비 문의','auto','md',86,70,.82,.18,180,100,9,155,-85,10],
    ['쇼핑몰','상품 문의','shop','md',49,83,.88,.11,0,180,2,18,-175,4],
    ['인테리어','견적 신청','interior','sm',73,83,.7,.3,115,155,7,110,-130,8]
  ];

  const ui={
    insurance:'<div class="v5-ui insurance"><div class="v5-row"><span>월 예상 보험료</span><b>84,000원</b></div><div class="v5-bars"><i></i><i></i><i></i><i></i></div><em>보장 분석 완료</em></div>',
    clinic:'<div class="v5-ui clinic"><div class="v5-profile"><i></i><span><b>김온유 원장</b><small>진료 예약</small></span></div><div class="v5-days"><i>2</i><i class="on">3</i><i>4</i><i>5</i><i>6</i></div><em>오전 10:30 예약 가능</em></div>',
    estate:'<div class="v5-ui estate"><div class="v5-house"><i></i><i></i><i></i></div><div class="v5-row"><span>시티뷰 리버파크</span><b>8억 4,000</b></div></div>',
    academy:'<div class="v5-ui academy"><div class="v5-avatars"><i></i><i></i><i></i></div><div class="v5-row"><span>수학 상담</span><b>18:30</b></div><em>상담 일정 확정</em></div>',
    salon:'<div class="v5-ui salon"><div class="v5-tiles"><i></i><i></i><i></i></div><div class="v5-row"><span>커트 · 컬러</span><b>예약</b></div></div>',
    auto:'<div class="v5-ui auto"><div class="v5-car"><i></i><span></span></div><div class="v5-checks"><span>● 엔진오일</span><span>● 타이어</span></div><em>정비 접수 완료</em></div>',
    shop:'<div class="v5-ui shop"><div class="v5-tiles"><i></i><i></i><i></i></div><div class="v5-row"><span>상품 문의 3건</span><b>확인</b></div></div>',
    interior:'<div class="v5-ui interior"><div class="v5-room"><i></i><i></i><i></i></div><div class="v5-row"><span>32평 견적</span><b>상담 요청</b></div></div>'
  };

  const style=document.createElement('style');
  style.dataset.ctIndustryVisualV5='1';
  style.textContent=`
    .ct-industry-v5{position:relative!important;height:205svh!important;min-height:1420px!important;padding:0!important;overflow:visible!important;background:linear-gradient(180deg,#070a13,#0b1021 50%,#070a13)!important;border-block:1px solid rgba(255,255,255,.07)!important;isolation:isolate}
    .ct-industry-v5:before,.ct-industry-v5:after{content:'';position:absolute;width:560px;height:560px;border-radius:50%;filter:blur(76px);pointer-events:none;opacity:.68}.ct-industry-v5:before{left:7%;top:27%;background:radial-gradient(circle,rgba(53,91,255,.24),transparent 68%)}.ct-industry-v5:after{right:6%;top:45%;background:radial-gradient(circle,rgba(98,68,255,.18),transparent 68%)}
    .ct-industry-v5__sticky{position:sticky;top:var(--ct-horizontal-header,68px);height:calc(100svh - var(--ct-horizontal-header,68px));min-height:640px;display:grid;grid-template-rows:auto minmax(0,1fr);padding:34px 0 18px;overflow:hidden;box-sizing:border-box}
    .ct-industry-v5__head{position:relative;z-index:30;width:min(1180px,calc(100% - 48px));margin:0 auto;text-align:center;will-change:transform,opacity}.ct-industry-v5__head h2{margin:0;color:#f7f8fc;font-size:clamp(42px,4.8vw,68px);line-height:1;letter-spacing:-.07em;white-space:nowrap}
    .ct-industry-v5__stage{position:relative;width:min(1320px,calc(100% - 32px));height:100%;min-height:550px;margin:0 auto;perspective:1500px}.ct-industry-v5__stage:before{content:'';position:absolute;inset:5% 7%;background:radial-gradient(ellipse at center,rgba(74,110,255,.19),transparent 64%);filter:blur(28px);pointer-events:none}
    .ct-industry-v5__card{position:absolute;left:var(--x);top:var(--y);z-index:var(--z,4);width:var(--w,250px);height:var(--h,190px);opacity:0;transform:translate(-50%,-50%);will-change:transform,opacity,filter}.ct-industry-v5__card.xl{--w:330px;--h:250px;--z:9}.ct-industry-v5__card.md{--w:270px;--h:205px;--z:6}.ct-industry-v5__card.sm{--w:220px;--h:166px;--z:4}
    .ct-industry-v5__inner{width:100%;height:100%;display:grid;grid-template-rows:minmax(0,1fr) auto;overflow:hidden;border:1px solid rgba(255,255,255,.14);border-radius:24px;background:linear-gradient(145deg,rgba(25,31,48,.97),rgba(11,15,27,.95));box-shadow:0 27px 78px rgba(0,0,0,.43);backdrop-filter:blur(18px);transition:transform .4s cubic-bezier(.16,1,.3,1),border-color .3s ease,box-shadow .4s ease,filter .3s ease}.ct-industry-v5__thumb{position:relative;min-height:0;overflow:hidden;border-bottom:1px solid rgba(255,255,255,.09);background:#0e1321}.ct-industry-v5__meta{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:13px 15px 14px}.ct-industry-v5__meta span{padding:6px 9px;border-radius:999px;background:rgba(80,116,255,.15);color:#9db0ff;font-size:10px;font-weight:900}.ct-industry-v5__meta strong{color:#f3f5fb;font-size:clamp(16px,1.4vw,22px);line-height:1;letter-spacing:-.045em;white-space:nowrap}
    .ct-industry-v5__card.settled .ct-industry-v5__inner{animation:v5Float var(--float-duration,6s) ease-in-out var(--float-delay,0s) infinite alternate}@keyframes v5Float{from{translate:0 0;rotate:-.16deg}to{translate:0 -8px;rotate:.16deg}}
    @media(hover:hover) and (pointer:fine){.ct-industry-v5__card:hover{z-index:30!important;opacity:1!important}.ct-industry-v5__card:hover .ct-industry-v5__inner{animation-play-state:paused;transform:translateY(-7px) scale(1.055);border-color:rgba(118,148,255,.58);box-shadow:0 40px 112px rgba(30,56,160,.34);filter:brightness(1.08)}}

    .v5-ui{position:absolute;inset:0;padding:14px;color:#fff;font-size:10px;box-sizing:border-box}.v5-ui b,.v5-ui span,.v5-ui small{position:relative;z-index:2}.v5-row{display:flex;align-items:flex-end;justify-content:space-between;gap:8px}.v5-ui em{display:block;margin-top:9px;padding:8px 10px;border-radius:9px;background:rgba(255,255,255,.07);font-style:normal;font-weight:850}.insurance{background:linear-gradient(155deg,#172651,#0d1427)}.insurance .v5-row span{color:#8d96a9}.insurance .v5-row b{font-size:20px}.v5-bars{height:64px;display:flex;align-items:flex-end;gap:7px;margin-top:17px;padding:10px;border-radius:12px;background:rgba(255,255,255,.045)}.v5-bars i{flex:1;border-radius:5px 5px 2px 2px;background:linear-gradient(#7699ff,#315edc)}.v5-bars i:nth-child(1){height:38%}.v5-bars i:nth-child(2){height:72%}.v5-bars i:nth-child(3){height:56%}.v5-bars i:nth-child(4){height:88%}
    .clinic{background:linear-gradient(155deg,#e8fbf7,#c9f1e8);color:#17342f}.v5-profile{display:flex;gap:9px;align-items:center}.v5-profile>i{width:38px;height:38px;border-radius:12px;background:linear-gradient(#63c7b5,#3fa995)}.v5-profile b,.v5-profile small{display:block}.v5-profile small{margin-top:3px;color:#557a72}.v5-days{display:grid;grid-template-columns:repeat(5,1fr);gap:5px;margin-top:15px}.v5-days i{height:29px;display:grid;place-items:center;border-radius:8px;background:rgba(255,255,255,.7);font-style:normal}.v5-days i.on{background:#16a085;color:#fff}.clinic em{background:rgba(255,255,255,.62)}
    .estate{background:linear-gradient(#d9a66f 0 60%,#f4eee6 60%);color:#4a2e1d}.v5-house{position:relative;height:82px}.v5-house i:first-child{position:absolute;left:20%;right:20%;bottom:11px;height:48px;background:#fff0db}.v5-house i:nth-child(2){position:absolute;left:15%;right:15%;bottom:56px;height:35px;background:#fff0db;clip-path:polygon(50% 0,100% 100%,0 100%)}.v5-house i:nth-child(3){position:absolute;left:47%;bottom:11px;width:18px;height:32px;background:#bd7539}.estate .v5-row{margin-top:11px}.estate .v5-row b{color:#b76b2b}
    .academy{background:linear-gradient(145deg,#27214b,#14182c)}.v5-avatars{display:flex;justify-content:center;gap:8px;margin-top:12px}.v5-avatars i{width:30px;height:30px;border-radius:50%;background:#7569d8}.v5-avatars i:nth-child(2){width:38px;height:38px;background:#9b8cf4}.academy .v5-row{margin-top:18px}.academy .v5-row b{color:#b9adff}
    .salon{background:linear-gradient(150deg,#4b2137,#221322)}.v5-tiles{display:flex;gap:7px;height:76px}.v5-tiles i{flex:1;border-radius:12px;background:linear-gradient(160deg,#e090b8,#7b3459)}.v5-tiles i:nth-child(2){background:linear-gradient(160deg,#f0b389,#8f4e45)}.v5-tiles i:nth-child(3){background:linear-gradient(160deg,#c987dc,#603974)}.salon .v5-row,.shop .v5-row,.interior .v5-row{margin-top:12px}.salon .v5-row b{color:#ffb9d8}
    .auto{background:linear-gradient(145deg,#172329,#0d151a)}.v5-car{position:relative;height:64px}.v5-car>i{position:absolute;left:15%;right:15%;top:22px;height:25px;border-radius:13px 13px 7px 7px;background:#56b79e}.v5-car>span{position:absolute;left:27%;right:27%;top:8px;height:25px;border-radius:20px 20px 4px 4px;background:#376f63}.v5-checks{display:flex;gap:7px}.v5-checks span{flex:1;padding:7px;border-radius:8px;background:rgba(255,255,255,.055)}
    .shop{background:linear-gradient(150deg,#281b48,#15152a)}.shop .v5-tiles i{background:linear-gradient(#8b73e6,#49358e)}.shop .v5-tiles i:nth-child(2){background:linear-gradient(#f090bc,#85405e)}.shop .v5-tiles i:nth-child(3){background:linear-gradient(#6cb7ea,#315e8a)}.shop .v5-row b{color:#b9a8ff}
    .interior{background:linear-gradient(150deg,#40352b,#211b17)}.v5-room{position:relative;height:76px;border-radius:10px;background:#c9b49c;overflow:hidden}.v5-room i:first-child{position:absolute;left:10px;bottom:9px;width:45%;height:24px;background:#7f6653}.v5-room i:nth-child(2){position:absolute;right:12px;bottom:9px;width:28%;height:38px;background:#e2d3c1}.v5-room i:nth-child(3){position:absolute;left:12px;top:10px;width:34%;height:14px;background:#eadfce}.interior .v5-row b{color:#e3c29d}

    @media(max-width:900px){.ct-industry-v5{height:auto!important;min-height:0!important;padding:88px 0!important;overflow:hidden!important}.ct-industry-v5__sticky{position:relative!important;top:auto!important;height:auto!important;min-height:0!important;display:block!important;padding:0!important;overflow:visible!important}.ct-industry-v5__head{width:calc(100% - 32px)!important;margin:0 auto 28px!important;text-align:left!important}.ct-industry-v5__head h2{font-size:36px!important;text-align:left!important}.ct-industry-v5__stage{width:calc(100% - 32px)!important;height:auto!important;min-height:0!important;display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:12px!important}.ct-industry-v5__stage:before{display:none!important}.ct-industry-v5__card,.ct-industry-v5__card.xl,.ct-industry-v5__card.md,.ct-industry-v5__card.sm{position:relative!important;left:auto!important;top:auto!important;width:100%!important;height:190px!important}.ct-industry-v5__inner{border-radius:18px!important}.ct-industry-v5__meta{padding:11px 12px 12px!important}.ct-industry-v5__meta strong{font-size:16px!important}.ct-industry-v5__meta span{font-size:9px!important}}
    @media(max-width:560px){.ct-industry-v5__head,.ct-industry-v5__stage{width:calc(100% - 28px)!important}.ct-industry-v5__stage{grid-template-columns:1fr!important}.ct-industry-v5__card,.ct-industry-v5__card.xl,.ct-industry-v5__card.md,.ct-industry-v5__card.sm{height:220px!important}}
    @media(prefers-reduced-motion:reduce){.ct-industry-v5__card{opacity:var(--alpha,1)!important;transform:translate(-50%,-50%)!important}.ct-industry-v5__inner{animation:none!important}.ct-industry-v5__head{opacity:1!important;transform:none!important}}
    @media(max-width:900px) and (prefers-reduced-motion:reduce){.ct-industry-v5__card{transform:none!important}}
  `;
  document.head.append(style);

  const markup=`<div class="ct-industry-v5__sticky"><div class="ct-industry-v5__head"><h2>업종별 문의 화면</h2></div><div class="ct-industry-v5__stage">${data.map((item,index)=>`<article class="ct-industry-v5__card ${item[3]}" style="--x:${item[4]}%;--y:${item[5]}%;--alpha:${item[6]};--float-duration:${5.5+(index%4)*.55}s;--float-delay:${-((index%5)*.72)}s" data-alpha="${item[6]}" data-delay="${item[7]}" data-dx="${item[8]}" data-dy="${item[9]}" data-rot="${item[10]}" data-ex="${item[11]}" data-ey="${item[12]}" data-er="${item[13]}"><div class="ct-industry-v5__inner"><div class="ct-industry-v5__thumb">${ui[item[2]]}</div><div class="ct-industry-v5__meta"><span>${item[0]}</span><strong>${item[1]}</strong></div></div></article>`).join('')}</div></div>`;

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
    const target=document.querySelector('#ct-pagero-intro .ct-industry-v4,#ct-pagero-intro .ct-industry-visual-section,#ct-pagero-intro .ct-industry-float-section,#ct-pagero-intro .ct-industries-static,#ct-pagero-intro .ct-horizontal-industries-clean');
    if(!target)return false;
    if(target.dataset.ctIndustryV5Mounted==='1')return true;
    target.className='ct-industry-v5';
    target.removeAttribute('style');
    target.innerHTML=markup;
    target.dataset.ctIndustryV5Mounted='1';
    section=target;
    sticky=target.querySelector('.ct-industry-v5__sticky');
    head=target.querySelector('.ct-industry-v5__head');
    cards=[...target.querySelectorAll('.ct-industry-v5__card')];
    if(reduce){
      cards.forEach(card=>{card.style.opacity=card.dataset.alpha||'1';card.classList.add('settled')});
      head.style.opacity='1';
    }else requestRender();
    dispatchEvent(new Event('resize'));
    return true;
  };

  const getProgress=()=>{
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
    const p=getProgress();
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
      const x=isMobile?0:dx*(1-enter)+ex*leave;
      const y=isMobile?(1-enter)*36-leave*42:dy*(1-enter)-lift+ey*leave;
      const rotation=isMobile?0:rot*(1-enter)+er*leave;
      const startScale=isMobile ? .97 : .86;
      const scale=startScale+(1-startScale)*enter-.1*leave;
      const blur=(1-enter)*2.4+leave*3.2;
      card.style.opacity=(alpha*visibility).toFixed(3);
      card.style.filter=`blur(${blur.toFixed(2)}px) brightness(${(.8+enter*.2-leave*.14).toFixed(3)})`;
      card.style.transform=isMobile
        ?`translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) scale(${scale.toFixed(4)})`
        :`translate(-50%,-50%) translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) rotate(${rotation.toFixed(2)}deg) scale(${scale.toFixed(4)})`;
      card.classList.toggle('settled',enter>.96&&leave<.04);
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
