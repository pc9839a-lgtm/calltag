(()=>{
  if(document.documentElement.dataset.ctIndustryVisualV3)return;
  document.documentElement.dataset.ctIndustryVisualV3='1';

  const cards=[
    {industry:'보험',use:'상담 신청',type:'insurance',size:'xl',x:41,y:54,alpha:1,delay:.02,exit:.02,dx:-160,dy:130,rot:-7,lift:42,ex:-120,ey:-150,er:-8},
    {industry:'병원',use:'진료 예약',type:'clinic',size:'xl',x:63,y:53,alpha:.98,delay:.07,exit:.00,dx:170,dy:110,rot:7,lift:52,ex:135,ey:-165,er:9},
    {industry:'부동산',use:'매물 문의',type:'estate',size:'md',x:16,y:32,alpha:.82,delay:.16,exit:.08,dx:-180,dy:-60,rot:-9,lift:31,ex:-155,ey:-95,er:-11},
    {industry:'학원',use:'상담 예약',type:'academy',size:'sm',x:84,y:29,alpha:.67,delay:.23,exit:.11,dx:165,dy:-85,rot:10,lift:23,ex:170,ey:-110,er:12},
    {industry:'미용실',use:'시술 예약',type:'salon',size:'sm',x:10,y:72,alpha:.58,delay:.28,exit:.04,dx:-175,dy:105,rot:-12,lift:19,ex:-180,ey:-60,er:-13},
    {industry:'자동차',use:'정비 문의',type:'auto',size:'md',x:86,y:70,alpha:.8,delay:.19,exit:.07,dx:185,dy:95,rot:8,lift:35,ex:165,ey:-80,er:10},
    {industry:'법무',use:'상담 접수',type:'law',size:'sm',x:27,y:83,alpha:.61,delay:.34,exit:.13,dx:-110,dy:160,rot:-6,lift:22,ex:-95,ey:-135,er:-7},
    {industry:'쇼핑몰',use:'상품 문의',type:'shop',size:'md',x:50,y:84,alpha:.86,delay:.12,exit:.05,dx:0,dy:180,rot:2,lift:40,ex:20,ey:-185,er:4},
    {industry:'인테리어',use:'견적 신청',type:'interior',size:'sm',x:73,y:84,alpha:.69,delay:.3,exit:.1,dx:120,dy:155,rot:7,lift:26,ex:115,ey:-140,er:9},
    {industry:'여행',use:'예약 상담',type:'travel',size:'sm',x:51,y:17,alpha:.56,delay:.26,exit:.15,dx:20,dy:-155,rot:-3,lift:18,ex:-10,ey:-125,er:-5}
  ];

  const thumbs={
    insurance:`<div class="ct-mini ct-mini--insurance"><div class="ct-mini__top"><span>월 예상 보험료</span><b>84,000원</b></div><div class="ct-mini__bars"><i></i><i></i><i></i><i></i></div><div class="ct-mini__notice">보장 분석 완료</div></div>`,
    clinic:`<div class="ct-mini ct-mini--clinic"><div class="ct-mini__doctor"><i></i><span><b>김온유 원장</b><small>진료 예약</small></span></div><div class="ct-mini__calendar"><i>2</i><i class="on">3</i><i>4</i><i>5</i><i>6</i></div><div class="ct-mini__time">오전 10:30 예약 가능</div></div>`,
    estate:`<div class="ct-mini ct-mini--estate"><div class="ct-mini__house"><i></i><i></i><i></i></div><div class="ct-mini__estate-copy"><span>시티뷰 리버파크 84㎡</span><b>8억 4,000</b></div></div>`,
    academy:`<div class="ct-mini ct-mini--academy"><div class="ct-mini__people"><i></i><i></i><i></i></div><div class="ct-mini__lesson"><span>수학 상담</span><b>오늘 18:30</b></div><div class="ct-mini__line"></div></div>`,
    salon:`<div class="ct-mini ct-mini--salon"><div class="ct-mini__looks"><i></i><i></i><i></i></div><div class="ct-mini__salon-row"><span>커트 · 컬러</span><b>예약</b></div></div>`,
    auto:`<div class="ct-mini ct-mini--auto"><div class="ct-mini__car"><i></i><span></span></div><div class="ct-mini__checks"><span><i></i>엔진오일</span><span><i></i>타이어</span></div><div class="ct-mini__status">정비 접수 완료</div></div>`,
    law:`<div class="ct-mini ct-mini--law"><div class="ct-mini__doc"><i></i><i></i><i></i><b>✓</b></div><div class="ct-mini__law-copy"><span>상담서 접수</span><small>담당자 배정</small></div></div>`,
    shop:`<div class="ct-mini ct-mini--shop"><div class="ct-mini__products"><i></i><i></i><i></i></div><div class="ct-mini__order"><span>상품 문의 3건</span><b>확인</b></div></div>`,
    interior:`<div class="ct-mini ct-mini--interior"><div class="ct-mini__room"><i></i><i></i><i></i><span></span></div><div class="ct-mini__quote"><span>32평 견적</span><b>상담 요청</b></div></div>`,
    travel:`<div class="ct-mini ct-mini--travel"><div class="ct-mini__route"><i></i><i></i><i></i><b>✈</b></div><div class="ct-mini__travel-copy"><span>오사카 3박 4일</span><b>예약 상담</b></div></div>`
  };

  const style=document.createElement('style');
  style.dataset.ctIndustryVisualV3='1';
  style.textContent=`
    .ct-industry-visual-section{
      --ct-industry-progress:0;
      position:relative!important;
      height:220svh!important;
      min-height:1500px!important;
      padding:0!important;
      overflow:visible!important;
      border-top:1px solid rgba(255,255,255,.07)!important;
      border-bottom:1px solid rgba(255,255,255,.07)!important;
      background:linear-gradient(180deg,#070a13 0%,#0b1021 48%,#070a13 100%)!important;
      isolation:isolate;
    }
    .ct-industry-visual-section:before,
    .ct-industry-visual-section:after{
      content:'';
      position:absolute;
      pointer-events:none;
      border-radius:50%;
      filter:blur(70px);
      opacity:.72;
    }
    .ct-industry-visual-section:before{
      width:620px;height:620px;left:8%;top:26%;
      background:radial-gradient(circle,rgba(55,91,255,.23),transparent 68%);
    }
    .ct-industry-visual-section:after{
      width:540px;height:540px;right:7%;top:44%;
      background:radial-gradient(circle,rgba(95,66,255,.17),transparent 68%);
    }
    .ct-industry-visual__sticky{
      position:sticky;
      top:var(--ct-horizontal-header,68px);
      height:calc(100svh - var(--ct-horizontal-header,68px));
      min-height:650px;
      overflow:hidden;
      display:grid;
      grid-template-rows:auto minmax(0,1fr);
      padding:clamp(28px,4vh,48px) 0 18px;
      box-sizing:border-box;
    }
    .ct-industry-visual__head{
      position:relative;
      z-index:20;
      width:min(1180px,calc(100% - 48px));
      margin:0 auto;
      text-align:center;
      pointer-events:none;
      will-change:transform,opacity;
    }
    .ct-industry-visual__head h2{
      margin:0;
      color:#f7f8fc;
      font-size:clamp(40px,4.8vw,68px);
      line-height:1;
      letter-spacing:-.07em;
      white-space:nowrap;
    }
    .ct-industry-visual__stage{
      position:relative;
      z-index:2;
      width:min(1320px,calc(100% - 32px));
      height:100%;
      min-height:560px;
      margin:0 auto;
      perspective:1500px;
    }
    .ct-industry-visual__grid,
    .ct-industry-visual__beam{
      position:absolute;
      inset:3% 5% 5%;
      pointer-events:none;
    }
    .ct-industry-visual__grid{
      opacity:.18;
      background-image:linear-gradient(rgba(121,147,255,.08) 1px,transparent 1px),linear-gradient(90deg,rgba(121,147,255,.08) 1px,transparent 1px);
      background-size:54px 54px;
      mask-image:radial-gradient(circle at center,#000 18%,transparent 76%);
    }
    .ct-industry-visual__beam{
      background:radial-gradient(ellipse at center,rgba(76,111,255,.18),transparent 60%);
      filter:blur(20px);
      transform:scale(calc(.78 + var(--ct-industry-progress)*.28));
      opacity:calc(.35 + var(--ct-industry-progress)*.35);
    }
    .ct-industry-visual__card{
      position:absolute;
      left:var(--x);
      top:var(--y);
      z-index:var(--z,3);
      width:var(--w,240px);
      height:var(--h,184px);
      opacity:0;
      transform:translate(-50%,-50%);
      transform-style:preserve-3d;
      will-change:transform,opacity,filter;
    }
    .ct-industry-visual__card.is-xl{--w:330px;--h:250px;--z:8}
    .ct-industry-visual__card.is-md{--w:270px;--h:205px;--z:6}
    .ct-industry-visual__card.is-sm{--w:220px;--h:166px;--z:4}
    .ct-industry-visual__inner{
      position:relative;
      width:100%;
      height:100%;
      display:grid;
      grid-template-rows:minmax(0,1fr) auto;
      overflow:hidden;
      border:1px solid rgba(255,255,255,.14);
      border-radius:24px;
      background:linear-gradient(145deg,rgba(25,31,48,.96),rgba(11,15,27,.94));
      box-shadow:0 26px 78px rgba(0,0,0,.42),0 0 0 1px rgba(96,128,255,.04) inset;
      backdrop-filter:blur(18px);
      -webkit-backdrop-filter:blur(18px);
      transform-origin:center;
      transition:transform .42s cubic-bezier(.16,1,.3,1),border-color .35s ease,box-shadow .42s ease,filter .35s ease;
    }
    .ct-industry-visual__inner:after{
      content:'';
      position:absolute;
      inset:-1px;
      border-radius:inherit;
      pointer-events:none;
      background:linear-gradient(125deg,rgba(255,255,255,.12),transparent 24%,transparent 68%,rgba(103,135,255,.1));
      opacity:.68;
    }
    .ct-industry-visual__thumb{
      position:relative;
      min-height:0;
      overflow:hidden;
      border-bottom:1px solid rgba(255,255,255,.09);
      background:#0e1321;
    }
    .ct-industry-visual__meta{
      position:relative;
      z-index:2;
      display:flex;
      align-items:center;
      justify-content:space-between;
      gap:12px;
      padding:14px 16px 15px;
    }
    .ct-industry-visual__meta span{
      display:inline-flex;
      align-items:center;
      min-height:25px;
      padding:0 9px;
      border-radius:999px;
      background:rgba(80,116,255,.15);
      color:#9db0ff;
      font-size:10px;
      font-weight:900;
    }
    .ct-industry-visual__meta strong{
      color:#f3f5fb;
      font-size:clamp(16px,1.4vw,22px);
      line-height:1;
      letter-spacing:-.045em;
      white-space:nowrap;
    }
    .ct-industry-visual__card.is-settled .ct-industry-visual__inner{
      animation:ctIndustryVisualFloat var(--float-duration,6.2s) ease-in-out var(--float-delay,0s) infinite alternate;
    }
    @keyframes ctIndustryVisualFloat{
      from{translate:0 0;rotate:-.18deg}
      to{translate:0 -8px;rotate:.18deg}
    }
    @media(hover:hover) and (pointer:fine){
      .ct-industry-visual__card:hover{z-index:20!important;opacity:1!important}
      .ct-industry-visual__card:hover .ct-industry-visual__inner{
        animation-play-state:paused;
        transform:scale(1.055) translateY(-7px);
        border-color:rgba(118,148,255,.55);
        box-shadow:0 38px 110px rgba(30,56,160,.33),0 0 42px rgba(91,124,255,.15);
        filter:brightness(1.08);
      }
    }

    .ct-mini{position:absolute;inset:0;padding:14px;box-sizing:border-box;color:#fff;font-size:10px}
    .ct-mini b,.ct-mini span,.ct-mini small{position:relative;z-index:2}
    .ct-mini__top{display:flex;align-items:flex-end;justify-content:space-between}.ct-mini__top span{color:#8d96a9}.ct-mini__top b{font-size:20px;letter-spacing:-.05em}
    .ct-mini--insurance{background:linear-gradient(155deg,#172651,#0d1427)}
    .ct-mini__bars{height:64px;display:flex;align-items:flex-end;gap:7px;margin-top:17px;padding:10px;border-radius:12px;background:rgba(255,255,255,.045)}.ct-mini__bars i{flex:1;border-radius:5px 5px 2px 2px;background:linear-gradient(#7699ff,#315edc)}.ct-mini__bars i:nth-child(1){height:38%}.ct-mini__bars i:nth-child(2){height:72%}.ct-mini__bars i:nth-child(3){height:56%}.ct-mini__bars i:nth-child(4){height:88%}.ct-mini__notice{margin-top:10px;padding:8px 10px;border-radius:9px;background:rgba(73,119,255,.16);color:#aabaff;font-weight:850}
    .ct-mini--clinic{background:linear-gradient(145deg,#dff8f2,#eefcf8);color:#12352d}.ct-mini__doctor{display:flex;align-items:center;gap:9px}.ct-mini__doctor>i{width:38px;height:38px;border-radius:12px;background:linear-gradient(#65cbb8,#329a87)}.ct-mini__doctor b,.ct-mini__doctor small{display:block}.ct-mini__doctor small{margin-top:3px;color:#5b7c74}.ct-mini__calendar{display:grid;grid-template-columns:repeat(5,1fr);gap:5px;margin-top:15px}.ct-mini__calendar i{height:31px;display:grid;place-items:center;border:1px solid rgba(20,77,65,.14);border-radius:8px;background:rgba(255,255,255,.66);font-style:normal}.ct-mini__calendar i.on{background:#19a286;color:#fff}.ct-mini__time{margin-top:10px;padding:8px;border-radius:8px;background:rgba(25,162,134,.1);color:#16836e;font-weight:900}
    .ct-mini--estate{padding:0;background:#ead0af;color:#332516}.ct-mini__house{position:relative;height:70%;overflow:hidden;background:linear-gradient(#c98849,#e0b279)}.ct-mini__house:before{content:'';position:absolute;left:16%;right:16%;bottom:14%;height:46%;background:#fff1dc;clip-path:polygon(50% 0,100% 37%,90% 37%,90% 100%,10% 100%,10% 37%,0 37%)}.ct-mini__house i{position:absolute;bottom:14%;z-index:2;width:12px;height:26px;background:#b96f2e}.ct-mini__house i:nth-child(1){left:31%}.ct-mini__house i:nth-child(2){left:48%;width:21px;height:34px}.ct-mini__house i:nth-child(3){right:31%}.ct-mini__estate-copy{display:flex;align-items:center;justify-content:space-between;padding:10px 12px}.ct-mini__estate-copy span{font-weight:850}.ct-mini__estate-copy b{color:#b65f17}
    .ct-mini--academy{background:linear-gradient(145deg,#211a47,#11162d)}.ct-mini__people{display:flex;padding-top:3px}.ct-mini__people i{width:32px;height:32px;margin-right:-8px;border:3px solid #171a32;border-radius:50%;background:linear-gradient(145deg,#8b77f5,#4c42a6)}.ct-mini__lesson{display:flex;justify-content:space-between;margin-top:18px;padding:10px;border-radius:10px;background:rgba(138,118,245,.13)}.ct-mini__lesson span{color:#b9aff8}.ct-mini__line{height:7px;margin-top:12px;border-radius:999px;background:linear-gradient(90deg,#826df0 64%,rgba(255,255,255,.08) 64%)}
    .ct-mini--salon{background:linear-gradient(145deg,#3e1d32,#171321)}.ct-mini__looks{height:76%;display:grid;grid-template-columns:repeat(3,1fr);gap:6px}.ct-mini__looks i{border-radius:11px;background:linear-gradient(160deg,#ec9dbf,#6b365b)}.ct-mini__looks i:nth-child(2){background:linear-gradient(160deg,#e8c1aa,#8b5d51)}.ct-mini__looks i:nth-child(3){background:linear-gradient(160deg,#c2a4e9,#58436f)}.ct-mini__salon-row{display:flex;justify-content:space-between;margin-top:9px}.ct-mini__salon-row span{color:#e7b5cf}.ct-mini__salon-row b{color:#fff}
    .ct-mini--auto{background:linear-gradient(145deg,#18232b,#0d141a)}.ct-mini__car{position:relative;height:54px;margin-top:4px}.ct-mini__car:before{content:'';position:absolute;left:14%;right:14%;bottom:8px;height:23px;border-radius:12px 12px 7px 7px;background:linear-gradient(#72d0bd,#267f70)}.ct-mini__car:after{content:'';position:absolute;left:28%;right:28%;bottom:29px;height:18px;border-radius:10px 10px 2px 2px;background:#8de0d0}.ct-mini__car i,.ct-mini__car span{position:absolute;bottom:2px;width:14px;height:14px;border:4px solid #0b1015;border-radius:50%;background:#68747e}.ct-mini__car i{left:24%}.ct-mini__car span{right:24%}.ct-mini__checks{display:grid;grid-template-columns:1fr 1fr;gap:6px;margin-top:8px}.ct-mini__checks span{padding:7px;border-radius:8px;background:rgba(105,207,186,.09);color:#9fe2d4}.ct-mini__checks i{display:inline-block;width:5px;height:5px;margin-right:5px;border-radius:50%;background:#69cfba}.ct-mini__status{margin-top:8px;color:#6ed6c1;font-weight:900}
    .ct-mini--law{display:grid;grid-template-columns:1fr 1fr;align-items:center;background:linear-gradient(145deg,#28251e,#121416)}.ct-mini__doc{position:relative;width:62px;height:82px;padding:14px 9px;border-radius:7px;background:#ede8dc}.ct-mini__doc i{display:block;height:5px;margin-bottom:7px;border-radius:3px;background:#b7ac98}.ct-mini__doc b{position:absolute;right:-8px;bottom:-8px;width:28px;height:28px;display:grid;place-items:center;border-radius:50%;background:#c49b4a;color:#fff}.ct-mini__law-copy span,.ct-mini__law-copy small{display:block}.ct-mini__law-copy span{font-weight:900}.ct-mini__law-copy small{margin-top:6px;color:#aaa187}
    .ct-mini--shop{background:linear-gradient(145deg,#291f45,#12152a)}.ct-mini__products{height:70%;display:grid;grid-template-columns:repeat(3,1fr);gap:7px}.ct-mini__products i{border-radius:11px;background:linear-gradient(160deg,#8f72e8,#483775)}.ct-mini__products i:nth-child(2){background:linear-gradient(160deg,#7196ed,#334a86)}.ct-mini__products i:nth-child(3){background:linear-gradient(160deg,#ef8ec3,#7d3a62)}.ct-mini__order{display:flex;justify-content:space-between;margin-top:10px}.ct-mini__order span{color:#c7bafa}.ct-mini__order b{padding:3px 7px;border-radius:6px;background:#7658d6}
    .ct-mini--interior{background:linear-gradient(145deg,#342b25,#171719)}.ct-mini__room{position:relative;height:76%;overflow:hidden;border-radius:10px;background:linear-gradient(#d5c2ac 0 58%,#8e765f 58%)}.ct-mini__room i:nth-child(1){position:absolute;left:10%;bottom:18%;width:44%;height:28%;border-radius:5px;background:#654c3e}.ct-mini__room i:nth-child(2){position:absolute;right:11%;bottom:18%;width:28%;height:42%;background:#ead9c5}.ct-mini__room i:nth-child(3){position:absolute;left:21%;top:14%;width:28%;height:23%;border:5px solid #977b62}.ct-mini__room span{position:absolute;right:18%;top:12%;width:9px;height:34%;border-radius:8px;background:#78624d}.ct-mini__quote{display:flex;justify-content:space-between;margin-top:9px}.ct-mini__quote span{color:#d7c5b3}.ct-mini__quote b{color:#fff}
    .ct-mini--travel{background:linear-gradient(145deg,#17324e,#10182b)}.ct-mini__route{position:relative;height:72%;margin:0 4px}.ct-mini__route:before{content:'';position:absolute;left:8%;right:8%;top:50%;height:2px;background:linear-gradient(90deg,#5e8ff5,#5dd9c3)}.ct-mini__route i{position:absolute;top:calc(50% - 6px);width:12px;height:12px;border:3px solid #13243a;border-radius:50%;background:#6e9df8}.ct-mini__route i:nth-child(1){left:7%}.ct-mini__route i:nth-child(2){left:46%;background:#78d7c6}.ct-mini__route i:nth-child(3){right:7%;background:#f3b96a}.ct-mini__route b{position:absolute;left:58%;top:21%;font-size:20px;transform:rotate(12deg)}.ct-mini__travel-copy{display:flex;justify-content:space-between}.ct-mini__travel-copy span{color:#a9c4fa}.ct-mini__travel-copy b{color:#fff}

    @media(max-width:900px){
      .ct-industry-visual-section{height:auto!important;min-height:0!important;padding:110px 0 125px!important;overflow:hidden!important}
      .ct-industry-visual__sticky{position:relative;top:auto;height:auto;min-height:0;padding:0;overflow:visible;display:block}
      .ct-industry-visual__head{margin-bottom:46px;width:min(720px,calc(100% - 32px))}
      .ct-industry-visual__head h2{font-size:clamp(35px,8vw,48px)}
      .ct-industry-visual__stage{width:min(760px,calc(100% - 32px));height:auto;min-height:0;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;perspective:none}
      .ct-industry-visual__grid,.ct-industry-visual__beam{display:none}
      .ct-industry-visual__card,.ct-industry-visual__card.is-xl,.ct-industry-visual__card.is-md,.ct-industry-visual__card.is-sm{position:relative;left:auto;top:auto;width:100%;height:210px;transform:none}
      .ct-industry-visual__card.is-sm{height:185px}
      .ct-industry-visual__meta strong{font-size:18px}
    }
    @media(max-width:540px){
      .ct-industry-visual-section{padding:92px 0 105px!important}
      .ct-industry-visual__head{margin-bottom:34px}
      .ct-industry-visual__head h2{font-size:34px}
      .ct-industry-visual__stage{grid-template-columns:1fr;gap:14px}
      .ct-industry-visual__card,.ct-industry-visual__card.is-xl,.ct-industry-visual__card.is-md,.ct-industry-visual__card.is-sm{height:205px}
    }
    @media(prefers-reduced-motion:reduce){
      .ct-industry-visual__card{opacity:var(--alpha,1)!important;transform:translate(-50%,-50%)!important}
      .ct-industry-visual__inner{animation:none!important}
      .ct-industry-visual__head{opacity:1!important;transform:none!important}
    }
    @media(max-width:900px) and (prefers-reduced-motion:reduce){.ct-industry-visual__card{transform:none!important}}
  `;
  document.head.append(style);

  const markup=`<div class="ct-industry-visual__sticky"><div class="ct-industry-visual__head"><h2>업종별 문의 화면</h2></div><div class="ct-industry-visual__stage"><div class="ct-industry-visual__grid"></div><div class="ct-industry-visual__beam"></div>${cards.map((card,index)=>`<article class="ct-industry-visual__card is-${card.size}" style="--x:${card.x}%;--y:${card.y}%;--alpha:${card.alpha};--float-duration:${5.5+(index%4)*.55}s;--float-delay:${-((index%5)*.72)}s" data-alpha="${card.alpha}" data-delay="${card.delay}" data-exit="${card.exit}" data-dx="${card.dx}" data-dy="${card.dy}" data-rot="${card.rot}" data-lift="${card.lift}" data-ex="${card.ex}" data-ey="${card.ey}" data-er="${card.er}"><div class="ct-industry-visual__inner"><div class="ct-industry-visual__thumb">${thumbs[card.type]}</div><div class="ct-industry-visual__meta"><span>${card.industry}</span><strong>${card.use}</strong></div></div></article>`).join('')}</div></div>`;

  let section=null;
  let sticky=null;
  let title=null;
  let cardNodes=[];
  let ticking=false;
  const reduce=matchMedia('(prefers-reduced-motion:reduce)').matches;
  const mobile=matchMedia('(max-width:900px)');
  const clamp=value=>Math.max(0,Math.min(1,value));
  const easeOut=value=>1-Math.pow(1-value,3);
  const easeIn=value=>value*value*value;

  const mount=()=>{
    const target=document.querySelector('#ct-pagero-intro .ct-industry-float-section,#ct-pagero-intro .ct-industries-static,#ct-pagero-intro .ct-horizontal-industries-clean');
    if(!target)return false;
    if(target.dataset.ctIndustryVisualMounted==='1')return true;

    target.className='ct-industry-visual-section';
    target.removeAttribute('style');
    target.innerHTML=markup;
    target.dataset.ctIndustryVisualMounted='1';
    section=target;
    sticky=target.querySelector('.ct-industry-visual__sticky');
    title=target.querySelector('.ct-industry-visual__head');
    cardNodes=[...target.querySelectorAll('.ct-industry-visual__card')];

    if(reduce){
      cardNodes.forEach(card=>{card.style.opacity=card.dataset.alpha||'1';card.classList.add('is-settled')});
      title.style.opacity='1';
    }else requestRender();

    requestAnimationFrame(()=>{
      dispatchEvent(new Event('resize'));
      requestAnimationFrame(()=>dispatchEvent(new Event('scroll')));
    });
    return true;
  };

  const getProgress=()=>{
    if(!section)return 0;
    if(mobile.matches){
      const rect=section.getBoundingClientRect();
      const vh=innerHeight||document.documentElement.clientHeight;
      return clamp((vh*.9-rect.top)/(vh*.9+rect.height*.72));
    }
    const top=section.getBoundingClientRect().top+scrollY;
    const range=Math.max(1,section.offsetHeight-(sticky?.offsetHeight||innerHeight));
    return clamp((scrollY-top)/range);
  };

  const render=()=>{
    ticking=false;
    if(!section||!cardNodes.length)return;

    const progress=getProgress();
    const isMobile=mobile.matches;
    section.style.setProperty('--ct-industry-progress',progress.toFixed(4));

    const titleIn=easeOut(clamp(progress/.16));
    const titleOut=easeIn(clamp((progress-.78)/.2));
    if(title){
      title.style.opacity=(titleIn*(1-titleOut)).toFixed(3);
      title.style.transform=`translate3d(0,${((1-titleIn)*26-titleOut*38).toFixed(2)}px,0) scale(${(.96+titleIn*.04-titleOut*.035).toFixed(4)})`;
    }

    cardNodes.forEach(card=>{
      const alpha=parseFloat(card.dataset.alpha||'.8');
      const delay=parseFloat(card.dataset.delay||'0');
      const exitOffset=parseFloat(card.dataset.exit||'0');
      const dx=parseFloat(card.dataset.dx||'0');
      const dy=parseFloat(card.dataset.dy||'80');
      const rot=parseFloat(card.dataset.rot||'0');
      const lift=isMobile?14:parseFloat(card.dataset.lift||'30');
      const ex=isMobile?0:parseFloat(card.dataset.ex||'0');
      const ey=isMobile?-44:parseFloat(card.dataset.ey||'-120');
      const er=isMobile?0:parseFloat(card.dataset.er||'0');

      const enter=easeOut(clamp((progress-delay)/.34));
      const exitStart=.69+exitOffset;
      const leave=easeIn(clamp((progress-exitStart)/(1-exitStart)));
      const visibility=enter*(1-leave);
      const hold=clamp((progress-.18)/.52);
      const x=dx*(1-enter)+ex*leave;
      const y=dy*(1-enter)-lift*hold+ey*leave;
      const rotation=rot*(1-enter)+er*leave;
      const scale=(isMobile?.965:.86)+(isMobile?.035:.14)*enter-.12*leave;
      const blur=(1-enter)*2.6+leave*3.4;

      card.style.opacity=(alpha*visibility).toFixed(3);
      card.style.filter=`blur(${blur.toFixed(2)}px) brightness(${(.78+enter*.22-leave*.16).toFixed(3)})`;
      card.style.transform=isMobile
        ?`translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) rotate(${rotation.toFixed(2)}deg) scale(${scale.toFixed(4)})`
        :`translate(-50%,-50%) translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) rotate(${rotation.toFixed(2)}deg) scale(${scale.toFixed(4)})`;
      card.classList.toggle('is-settled',enter>.96&&leave<.04);
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
        if(mount())observer.disconnect();
      });
      observer.observe(document.documentElement,{childList:true,subtree:true});
      setTimeout(()=>observer.disconnect(),12000);
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
