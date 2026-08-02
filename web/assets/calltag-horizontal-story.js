(()=>{
  if(document.documentElement.dataset.ctHorizontalStory)return;
  document.documentElement.dataset.ctHorizontalStory='1';

  const reduce=matchMedia('(prefers-reduced-motion: reduce)');
  const clamp=(v,min=0,max=1)=>Math.min(max,Math.max(min,v));
  const lerp=(a,b,t)=>a+(b-a)*t;

  const css=`
    body.ct-horizontal-live{overflow-x:clip}
    body.ct-horizontal-live .ct-motion-progress,body.ct-horizontal-live .ct-motion-cursor{display:none!important}
    body.ct-horizontal-live .ct-motion-marquee{margin-top:-1px}

    .ct-horizontal-industries{--ct-hp:0;--ct-hi:0;position:relative;height:360vh;background:#090a0d;color:#fff;isolation:isolate}
    .ct-horizontal-industries .ct-h-sticky{position:sticky;top:0;height:100svh;min-height:720px;overflow:hidden;background:radial-gradient(circle at 70% 28%,rgba(65,104,255,.2),transparent 30%),#090a0d}
    .ct-horizontal-industries .ct-h-head{position:absolute;z-index:8;left:clamp(24px,5vw,82px);top:clamp(92px,12vh,132px);width:min(390px,34vw)}
    .ct-horizontal-industries .ct-h-kicker{margin:0 0 18px;color:#8ea6ff;font-size:12px;font-weight:900;letter-spacing:.12em}
    .ct-horizontal-industries .ct-h-head h2{margin:0;font-size:clamp(44px,5.2vw,78px);line-height:.95;letter-spacing:-.075em}
    .ct-horizontal-industries .ct-h-head h2 span{color:#86a0ff}
    .ct-horizontal-industries .ct-h-head>p:last-of-type{margin:24px 0 0;color:#aab0bd;font-size:15px;line-height:1.7}
    .ct-horizontal-industries .ct-h-count{display:flex;align-items:center;gap:12px;margin-top:32px;font-size:12px;font-weight:900}
    .ct-horizontal-industries .ct-h-count b{font-size:27px;color:#8fa7ff}.ct-horizontal-industries .ct-h-count i{width:80px;height:2px;background:#2d3340;overflow:hidden}.ct-horizontal-industries .ct-h-count i:after{content:'';display:block;width:100%;height:100%;transform-origin:left;transform:scaleX(calc((var(--ct-hi) + 1)/3));background:#7594ff}
    .ct-horizontal-industries .ct-h-track{position:absolute;inset:0;display:flex;width:300vw;transform:translate3d(calc(var(--ct-hp)*-200vw),0,0);will-change:transform}
    .ct-horizontal-industries .ct-h-panel{position:relative;flex:0 0 100vw;width:100vw;height:100%;display:grid;grid-template-columns:minmax(310px,38vw) minmax(0,1fr);align-items:center;padding:88px clamp(24px,5vw,82px) 54px;overflow:hidden}
    .ct-horizontal-industries .ct-h-panel:before{content:attr(data-step);position:absolute;right:3vw;top:7vh;color:rgba(255,255,255,.035);font-size:clamp(150px,25vw,380px);font-weight:950;line-height:.8;letter-spacing:-.1em}
    .ct-horizontal-industries .ct-h-copy{grid-column:2;align-self:end;margin:0 0 10vh clamp(40px,7vw,120px);max-width:540px;opacity:.3;transform:translateX(70px);transition:opacity .45s ease,transform .55s cubic-bezier(.16,1,.3,1)}
    .ct-horizontal-industries .ct-h-panel.is-active .ct-h-copy{opacity:1;transform:none}
    .ct-horizontal-industries .ct-h-copy small{color:#819cff;font-size:12px;font-weight:900;letter-spacing:.1em}.ct-horizontal-industries .ct-h-copy h3{margin:12px 0 0;font-size:clamp(36px,4.4vw,66px);line-height:.98;letter-spacing:-.068em}.ct-horizontal-industries .ct-h-copy p{margin:18px 0 0;color:#b0b6c2;font-size:15px;line-height:1.65}
    .ct-horizontal-industries .ct-industry-card{grid-column:2;grid-row:1;justify-self:center;width:min(340px,29vw)!important;min-width:300px!important;opacity:.35;transform:translate3d(0,40px,0) scale(.92)!important;transition:opacity .45s ease,transform .6s cubic-bezier(.16,1,.3,1)!important}
    .ct-horizontal-industries .ct-h-panel.is-active .ct-industry-card{opacity:1;transform:translate3d(0,-5vh,0) scale(1)!important}
    .ct-horizontal-industries .ct-industry-phone{height:620px!important;min-height:620px!important}.ct-horizontal-industries .ct-industry-screen{height:600px!important;min-height:600px!important}
    .ct-horizontal-industries .ct-h-panel:nth-child(2){background:radial-gradient(circle at 68% 38%,rgba(45,113,255,.25),transparent 28%)}
    .ct-horizontal-industries .ct-h-panel:nth-child(3){background:radial-gradient(circle at 70% 36%,rgba(20,152,126,.24),transparent 30%)}
    .ct-horizontal-industries .ct-h-panel:nth-child(4){background:radial-gradient(circle at 70% 36%,rgba(185,111,42,.24),transparent 30%)}

    .ct-journey-horizontal{--ct-jp:0;--ct-ji:0;position:relative;height:430vh;background:#05070b;color:#fff;isolation:isolate}
    .ct-journey-horizontal .ct-j-sticky{position:sticky;top:0;height:100svh;min-height:720px;overflow:hidden;background:linear-gradient(135deg,#070a12,#10172b);transition:background .5s ease}
    .ct-journey-horizontal.is-stage-1 .ct-j-sticky{background:linear-gradient(135deg,#07111a,#0c2630)}
    .ct-journey-horizontal.is-stage-2 .ct-j-sticky{background:linear-gradient(135deg,#0b0c11,#191423)}
    .ct-journey-horizontal.is-stage-3 .ct-j-sticky{background:linear-gradient(135deg,#09100e,#10271e)}
    .ct-journey-horizontal .ct-j-top{position:absolute;z-index:9;left:clamp(24px,5vw,80px);right:clamp(24px,5vw,80px);top:clamp(84px,10vh,118px);display:flex;align-items:center;justify-content:space-between}
    .ct-journey-horizontal .ct-j-top strong{font-size:13px;letter-spacing:.12em}.ct-journey-horizontal .ct-j-top span{color:#818a99;font-size:12px;font-weight:800}
    .ct-journey-horizontal .ct-j-track{position:absolute;inset:0;display:flex;width:400vw;transform:translate3d(calc(var(--ct-jp)*-300vw),0,0);will-change:transform}
    .ct-journey-horizontal .ct-j-panel{position:relative;flex:0 0 100vw;width:100vw;height:100%;display:grid;grid-template-columns:minmax(310px,.72fr) minmax(480px,1.28fr);gap:7vw;align-items:center;padding:120px clamp(24px,6vw,96px) 58px}
    .ct-journey-horizontal .ct-j-copy{max-width:610px;opacity:.22;transform:translateX(-80px);transition:opacity .45s ease,transform .62s cubic-bezier(.16,1,.3,1)}.ct-journey-horizontal .ct-j-panel.is-active .ct-j-copy{opacity:1;transform:none}
    .ct-journey-horizontal .ct-j-copy b{display:block;color:#7897ff;font-size:15px}.ct-journey-horizontal .ct-j-copy h2{margin:18px 0 0;font-size:clamp(54px,7vw,106px);line-height:.88;letter-spacing:-.085em}.ct-journey-horizontal .ct-j-copy p{margin:28px 0 0;color:#aeb5c2;font-size:16px;line-height:1.7;max-width:500px}
    .ct-journey-horizontal .ct-j-scene{position:relative;justify-self:end;width:min(720px,52vw);height:min(620px,68vh);min-height:480px;border:1px solid rgba(255,255,255,.13);border-radius:30px;background:rgba(13,17,25,.83);box-shadow:0 42px 120px rgba(0,0,0,.4);overflow:hidden;opacity:.3;transform:perspective(1400px) rotateY(-7deg) translateX(80px) scale(.94);transition:opacity .5s ease,transform .72s cubic-bezier(.16,1,.3,1)}
    .ct-journey-horizontal .ct-j-panel.is-active .ct-j-scene{opacity:1;transform:perspective(1400px) rotateY(0) translateX(0) scale(1)}
    .ct-j-windowbar{height:48px;display:flex;align-items:center;gap:7px;padding:0 18px;border-bottom:1px solid rgba(255,255,255,.1);background:#151923}.ct-j-windowbar i{width:8px;height:8px;border-radius:50%;background:#59606e}.ct-j-windowbar span{width:44%;height:22px;margin-left:10px;border-radius:7px;background:#202632}
    .ct-j-body{height:calc(100% - 48px);padding:30px}.ct-j-bigstatus{display:flex;align-items:flex-end;justify-content:space-between}.ct-j-bigstatus small{color:#7d8797;font-size:11px}.ct-j-bigstatus strong{font-size:34px;letter-spacing:-.06em}.ct-j-bigstatus em{padding:8px 11px;border-radius:999px;background:rgba(50,200,121,.12);color:#72dfa7;font-size:10px;font-style:normal;font-weight:900}
    .ct-j-form{display:grid;gap:12px;margin-top:28px}.ct-j-form div{min-height:60px;display:flex;align-items:center;justify-content:space-between;padding:0 18px;border:1px solid rgba(255,255,255,.1);border-radius:14px;background:#171c26}.ct-j-form span{color:#7f8897;font-size:11px}.ct-j-form b{font-size:13px}.ct-j-complete{margin-top:14px;padding:17px;border:1px solid rgba(50,200,121,.28);border-radius:14px;background:rgba(50,200,121,.09);color:#76e2aa;font-size:13px;font-weight:900}
    .ct-j-phone{position:absolute;right:38px;bottom:-35px;width:310px;padding:9px;border:1px solid rgba(255,255,255,.18);border-radius:40px;background:#020305;box-shadow:0 30px 80px rgba(0,0,0,.55)}.ct-j-phone-screen{min-height:515px;padding:34px 18px 24px;border-radius:31px;background:#10141c}.ct-j-phone-head{display:flex;justify-content:space-between;color:#7e9bff;font-size:15px;font-weight:900}.ct-j-alert{margin-top:26px;padding:17px;border:1px solid rgba(105,139,255,.24);border-radius:16px;background:#171d2a}.ct-j-alert small,.ct-j-alert b{display:block}.ct-j-alert small{color:#7d8797;font-size:9px}.ct-j-alert b{margin-top:7px;font-size:16px}.ct-j-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:13px}.ct-j-actions span{height:42px;display:grid;place-items:center;border-radius:11px;background:#222938;font-size:10px;font-weight:850}.ct-j-actions span.on{background:#3b6fff}.ct-j-timeline{display:grid;gap:10px;margin-top:24px}.ct-j-timeline div{display:grid;grid-template-columns:34px 1fr;gap:11px;align-items:center;padding:11px;border-radius:12px;background:#161b24}.ct-j-timeline i{width:34px;height:34px;display:grid;place-items:center;border-radius:10px;background:#252d3b;color:#8da5ff;font-style:normal;font-size:11px}.ct-j-timeline b,.ct-j-timeline small{display:block}.ct-j-timeline b{font-size:11px}.ct-j-timeline small{margin-top:4px;color:#717a88;font-size:8px}
    .ct-j-stats{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:28px}.ct-j-stats article{padding:18px;border:1px solid rgba(255,255,255,.1);border-radius:15px;background:#151b22}.ct-j-stats small,.ct-j-stats b{display:block}.ct-j-stats small{color:#7c8791;font-size:9px}.ct-j-stats b{margin-top:10px;font-size:25px}.ct-j-bars{display:flex;align-items:flex-end;gap:9px;height:170px;margin-top:30px;padding:22px;border-radius:18px;background:#11171b}.ct-j-bars i{flex:1;border-radius:7px 7px 2px 2px;background:linear-gradient(#65d89c,#1a6448)}.ct-j-bars i:nth-child(1){height:35%}.ct-j-bars i:nth-child(2){height:62%}.ct-j-bars i:nth-child(3){height:48%}.ct-j-bars i:nth-child(4){height:86%}.ct-j-bars i:nth-child(5){height:72%}.ct-j-bars i:nth-child(6){height:94%}
    .ct-journey-horizontal .ct-j-progress{position:absolute;z-index:10;left:clamp(24px,6vw,96px);right:clamp(24px,6vw,96px);bottom:36px;display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.ct-journey-horizontal .ct-j-progress i{height:3px;background:rgba(255,255,255,.13);overflow:hidden}.ct-journey-horizontal .ct-j-progress i:after{content:'';display:block;width:100%;height:100%;transform:scaleX(0);transform-origin:left;background:#7897ff;transition:transform .28s ease}.ct-journey-horizontal .ct-j-progress i.on:after{transform:scaleX(1)}

    @media(max-width:900px){
      .ct-horizontal-industries,.ct-journey-horizontal{height:auto!important}
      .ct-horizontal-industries .ct-h-sticky,.ct-journey-horizontal .ct-j-sticky{position:relative;height:auto;min-height:0;overflow:hidden;padding:96px 0 42px}
      .ct-horizontal-industries .ct-h-head{position:relative;left:auto;top:auto;width:auto;margin:0 22px 42px}.ct-horizontal-industries .ct-h-head h2{font-size:48px}.ct-horizontal-industries .ct-h-head>p:last-of-type{font-size:14px}.ct-horizontal-industries .ct-h-count{display:none}
      .ct-horizontal-industries .ct-h-track,.ct-journey-horizontal .ct-j-track{position:relative;inset:auto;width:auto;transform:none!important;overflow-x:auto;scroll-snap-type:x mandatory;scrollbar-width:none;padding:0 18px 28px;gap:16px}.ct-horizontal-industries .ct-h-track::-webkit-scrollbar,.ct-journey-horizontal .ct-j-track::-webkit-scrollbar{display:none}
      .ct-horizontal-industries .ct-h-panel{flex:0 0 88vw;width:88vw;height:auto;min-height:760px;display:flex;flex-direction:column;justify-content:flex-end;padding:34px 18px 34px;border:1px solid rgba(255,255,255,.09);border-radius:28px;scroll-snap-align:center}.ct-horizontal-industries .ct-h-panel:first-child{display:none}.ct-horizontal-industries .ct-h-copy{order:2;margin:24px 0 0;opacity:1;transform:none}.ct-horizontal-industries .ct-h-copy h3{font-size:38px}.ct-horizontal-industries .ct-industry-card{order:1;width:min(330px,82vw)!important;min-width:0!important;align-self:center;opacity:1;transform:none!important}.ct-horizontal-industries .ct-industry-phone{height:590px!important;min-height:590px!important}.ct-horizontal-industries .ct-industry-screen{height:570px!important;min-height:570px!important}
      .ct-journey-horizontal .ct-j-top{position:relative;left:auto;right:auto;top:auto;margin:0 22px 28px}.ct-journey-horizontal .ct-j-track{gap:16px}.ct-journey-horizontal .ct-j-panel{flex:0 0 91vw;width:91vw;height:auto;min-height:760px;display:flex;flex-direction:column;align-items:stretch;gap:34px;padding:34px 20px;scroll-snap-align:center;border:1px solid rgba(255,255,255,.1);border-radius:28px}.ct-journey-horizontal .ct-j-copy{opacity:1;transform:none}.ct-journey-horizontal .ct-j-copy h2{font-size:52px}.ct-journey-horizontal .ct-j-scene{width:100%;height:520px;min-height:0;opacity:1;transform:none}.ct-j-phone{right:18px;width:260px}.ct-journey-horizontal .ct-j-progress{display:none}
    }
    @media(prefers-reduced-motion:reduce){.ct-horizontal-industries .ct-h-track,.ct-journey-horizontal .ct-j-track{transition:none!important}.ct-horizontal-industries .ct-h-copy,.ct-horizontal-industries .ct-industry-card,.ct-journey-horizontal .ct-j-copy,.ct-journey-horizontal .ct-j-scene{opacity:1!important;transform:none!important}}
  `;

  const style=()=>{
    if(document.querySelector('style[data-ct-horizontal-story]'))return;
    const node=document.createElement('style');node.dataset.ctHorizontalStory='1';node.textContent=css;document.head.append(node);
  };

  const panelCopy=[
    ['보험 상담','상담 신청부터 고객 등록까지','보험료 진단 결과와 연락처가 한 번에 접수됩니다. 상담자는 화면을 옮겨 다니지 않고 바로 전화와 후속관리를 시작합니다.'],
    ['병원 예약','날짜와 시간까지 그대로','고객이 선택한 진료 날짜와 예약 시간이 콜태그 고객카드로 들어옵니다. 예약 누락 없이 바로 확인하고 안내할 수 있습니다.'],
    ['부동산 문의','매물 문의가 바로 다음 할 일로','관심 매물과 고객 연락처가 자동으로 묶입니다. 전화, 문자, 방문 일정까지 하나의 고객카드에서 이어집니다.']
  ];

  const buildIndustries=()=>{
    const section=document.querySelector('#ct-pagero-intro .ct-v8-nocode');
    const showcase=section?.querySelector('.ct-industry-showcase');
    const cards=showcase?[...showcase.querySelectorAll('.ct-industry-card')]:[];
    if(!section||cards.length!==3)return null;
    section.className='ct-horizontal-industries';
    section.classList.remove('ct-motion-section','ct-motion-enter','is-inview');
    section.innerHTML=`<div class="ct-h-sticky"><header class="ct-h-head"><p class="ct-h-kicker">PAGERO LANDING COLLECTION</p><h2>업종이 달라도,<br><span>문의는 한곳으로.</span></h2><p>아래로 스크롤하면 실제 랜딩페이지가 가로로 전환됩니다.</p><div class="ct-h-count"><b>01</b><i></i><span>03</span></div></header><div class="ct-h-track"><div class="ct-h-panel" data-step="00"></div></div></div>`;
    const track=section.querySelector('.ct-h-track');
    cards.forEach((card,index)=>{
      card.querySelectorAll('.ct-industry-auto').forEach(node=>node.remove());
      const panel=document.createElement('article');panel.className='ct-h-panel';panel.dataset.step=String(index+1).padStart(2,'0');
      panel.innerHTML=`<div class="ct-h-copy"><small>0${index+1} / 03 · ${panelCopy[index][0]}</small><h3>${panelCopy[index][1]}</h3><p>${panelCopy[index][2]}</p></div>`;
      panel.append(card);track.append(panel);
    });
    return section;
  };

  const journeyHtml=`<section class="ct-journey-horizontal"><div class="ct-j-sticky"><div class="ct-j-top"><strong>CALLTAG WORKFLOW</strong><span>SCROLL TO MOVE →</span></div><div class="ct-j-track">
    <article class="ct-j-panel"><div class="ct-j-copy"><b>01 / 문의 접수</b><h2>문의가<br>들어옵니다.</h2><p>페이지로 랜딩페이지에서 이름, 연락처, 상담 내용이 들어오는 순간 고객 데이터가 만들어집니다.</p></div><div class="ct-j-scene"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>새 상담 신청</small><strong>김민수 고객</strong></div><em>접수 완료</em></div><div class="ct-j-form"><div><span>연락처</span><b>010-1234-5678</b></div><div><span>문의 내용</span><b>보험 상담을 요청합니다</b></div><div><span>유입 페이지</span><b>보험료 진단 랜딩</b></div></div><div class="ct-j-complete">✓ 고객 정보가 콜태그로 전달됐습니다.</div></div></div></article>
    <article class="ct-j-panel"><div class="ct-j-copy"><b>02 / 즉시 알림</b><h2>앱에서<br>바로 뜹니다.</h2><p>새 문의 알림과 고객카드가 동시에 생성됩니다. 연락처를 다시 입력하거나 메모를 옮길 필요가 없습니다.</p></div><div class="ct-j-scene"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>콜태그 고객함</small><strong>신규 문의 1</strong></div><em>지금</em></div><div class="ct-j-phone"><div class="ct-j-phone-screen"><div class="ct-j-phone-head"><b>CALLTAG</b><span>● 1</span></div><div class="ct-j-alert"><small>페이지로 신규 문의</small><b>김민수 고객</b><small>보험 상담 요청 · 지금</small></div><div class="ct-j-actions"><span>전화</span><span>문자</span><span class="on">고객카드</span></div><div class="ct-j-timeline"><div><i>✓</i><span><b>자동 등록 완료</b><small>연락처·문의내용 저장</small></span></div><div><i>!</i><span><b>오늘 할 일 추가</b><small>신규 문의 확인</small></span></div></div></div></div></div></div></article>
    <article class="ct-j-panel"><div class="ct-j-copy"><b>03 / 통화 후 정리</b><h2>전화가 끝나면<br>정리도 끝.</h2><p>통화 종료 팝업에서 고객 구분, 상담 상태, 다음 할 일과 재연락 날짜를 한 번에 남깁니다.</p></div><div class="ct-j-scene"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>통화 종료</small><strong>김민수 고객</strong></div><em>02:18</em></div><div class="ct-j-form"><div><span>고객 구분</span><b>신규 문의</b></div><div><span>상담 상태</span><b>견적·자료 전달</b></div><div><span>다음 할 일</span><b>3일 뒤 다시 연락</b></div><div><span>메모</span><b>보장 분석표 문자 발송</b></div></div><div class="ct-j-actions"><span>문자 보내기</span><span>일정 추가</span><span class="on">저장 완료</span></div></div></div></article>
    <article class="ct-j-panel"><div class="ct-j-copy"><b>04 / 후속관리</b><h2>다시 연락할<br>고객만 보입니다.</h2><p>오늘 할 일, 재연락 일정, 상담 단계와 유입 통계가 한 화면에 쌓입니다. 고객을 놓치지 않는 업무 루프가 완성됩니다.</p></div><div class="ct-j-scene"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>오늘의 고객관리</small><strong>재연락 8건</strong></div><em>진행 중</em></div><div class="ct-j-stats"><article><small>신규 문의</small><b>24</b></article><article><small>통화 완료</small><b>17</b></article><article><small>계약 예정</small><b>6</b></article></div><div class="ct-j-bars"><i></i><i></i><i></i><i></i><i></i><i></i></div><div class="ct-j-complete">✓ 페이지로 유입부터 후속관리까지 연결됐습니다.</div></div></div></article>
    </div><div class="ct-j-progress"><i></i><i></i><i></i><i></i></div></div></section>`;

  const buildJourney=industry=>{
    if(document.querySelector('.ct-journey-horizontal'))return document.querySelector('.ct-journey-horizontal');
    industry.insertAdjacentHTML('afterend',journeyHtml);
    return industry.nextElementSibling;
  };

  const setupScroll=(industry,journey)=>{
    let industryCurrent=0,journeyCurrent=0,raf=0;
    const update=()=>{
      raf=0;
      if(innerWidth<=900)return;
      const y=scrollY;
      const ip=clamp((y-industry.offsetTop)/(industry.offsetHeight-innerHeight));
      const jp=clamp((y-journey.offsetTop)/(journey.offsetHeight-innerHeight));
      industryCurrent=reduce.matches?ip:lerp(industryCurrent,ip,.13);
      journeyCurrent=reduce.matches?jp:lerp(journeyCurrent,jp,.13);
      industry.style.setProperty('--ct-hp',industryCurrent.toFixed(4));
      journey.style.setProperty('--ct-jp',journeyCurrent.toFixed(4));
      const ii=Math.min(2,Math.max(0,Math.round(industryCurrent*2)));
      const ji=Math.min(3,Math.max(0,Math.round(journeyCurrent*3)));
      industry.style.setProperty('--ct-hi',ii);
      industry.querySelector('.ct-h-count b').textContent=String(ii+1).padStart(2,'0');
      industry.querySelectorAll('.ct-h-panel').forEach((panel,index)=>panel.classList.toggle('is-active',index===ii+1));
      journey.querySelectorAll('.ct-j-panel').forEach((panel,index)=>panel.classList.toggle('is-active',index===ji));
      journey.className=`ct-journey-horizontal is-stage-${ji}`;
      journey.querySelectorAll('.ct-j-progress i').forEach((bar,index)=>bar.classList.toggle('on',index<=ji));
      if(Math.abs(industryCurrent-ip)>.001||Math.abs(journeyCurrent-jp)>.001)request();
    };
    const request=()=>{if(!raf)raf=requestAnimationFrame(update)};
    addEventListener('scroll',request,{passive:true});addEventListener('resize',request,{passive:true});request();
  };

  const init=()=>{
    style();
    document.body.classList.add('ct-horizontal-live');
    document.querySelectorAll('.ct-motion-progress,.ct-motion-cursor').forEach(node=>node.remove());
    const industry=buildIndustries();
    if(!industry)return false;
    const journey=buildJourney(industry);
    setupScroll(industry,journey);
    return true;
  };

  const boot=()=>{if(init())return;const observer=new MutationObserver(()=>{if(init())observer.disconnect()});observer.observe(document.documentElement,{childList:true,subtree:true});setTimeout(()=>observer.disconnect(),15000)};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(boot),{once:true});else requestAnimationFrame(boot);
})();
