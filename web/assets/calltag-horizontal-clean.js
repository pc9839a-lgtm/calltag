(()=>{
  if(document.documentElement.dataset.ctHorizontalClean)return;
  document.documentElement.dataset.ctHorizontalClean='1';

  const mq=matchMedia('(min-width:901px)');
  const clamp=(v,min=0,max=1)=>Math.min(max,Math.max(min,v));

  const css=`
    body{overflow-x:hidden}
    .ct-horizontal-clean{position:relative;background:#080a10;color:#fff;isolation:isolate}
    .ct-horizontal-clean__sticky{position:sticky;top:0;height:100svh;min-height:680px;overflow:hidden;padding-top:68px;box-sizing:border-box}
    .ct-horizontal-clean__track{height:100%;display:flex;will-change:transform;transform:translate3d(0,0,0)}
    .ct-horizontal-clean__panel{position:relative;flex:0 0 100vw;width:100vw;height:100%;display:grid;grid-template-columns:minmax(320px,.78fr) minmax(520px,1.22fr);gap:clamp(34px,6vw,110px);align-items:center;padding:52px clamp(28px,5.2vw,90px) 60px;box-sizing:border-box;overflow:hidden}
    .ct-horizontal-clean__panel:before{content:attr(data-step);position:absolute;right:3vw;top:9vh;color:rgba(255,255,255,.035);font-size:clamp(150px,23vw,350px);font-weight:950;line-height:.8;letter-spacing:-.1em;pointer-events:none}
    .ct-horizontal-clean__copy{position:relative;z-index:2;min-width:0;max-width:650px}
    .ct-horizontal-clean__copy small{display:block;color:#7898ff;font-size:13px;font-weight:900;letter-spacing:.07em}
    .ct-horizontal-clean__copy h2,.ct-horizontal-clean__copy h3{margin:15px 0 0;font-size:clamp(48px,6vw,96px);line-height:.92;letter-spacing:-.08em;word-break:keep-all}
    .ct-horizontal-clean__copy p{max-width:560px;margin:24px 0 0;color:#aeb5c3;font-size:16px;line-height:1.72;word-break:keep-all}
    .ct-horizontal-clean__visual{position:relative;z-index:2;min-width:0;justify-self:stretch}
    .ct-horizontal-clean__top{position:absolute;z-index:8;left:clamp(28px,5.2vw,90px);right:clamp(28px,5.2vw,90px);top:88px;display:flex;justify-content:space-between;color:#8c95a7;font-size:12px;font-weight:900;letter-spacing:.08em;pointer-events:none}
    .ct-horizontal-clean__progress{position:absolute;z-index:8;left:clamp(28px,5.2vw,90px);right:clamp(28px,5.2vw,90px);bottom:32px;display:grid;gap:8px}
    .ct-horizontal-clean__progress i{height:3px;background:rgba(255,255,255,.14);overflow:hidden}
    .ct-horizontal-clean__progress i:after{content:'';display:block;width:100%;height:100%;background:#7595ff;transform:scaleX(0);transform-origin:left;transition:transform .18s ease}
    .ct-horizontal-clean__progress i.on:after{transform:scaleX(1)}

    .ct-horizontal-industries-clean .ct-horizontal-clean__sticky{background:radial-gradient(circle at 76% 36%,rgba(63,100,255,.18),transparent 30%),#080a10}
    .ct-horizontal-industries-clean .ct-horizontal-clean__panel:nth-child(2){background:radial-gradient(circle at 76% 36%,rgba(32,160,132,.16),transparent 30%)}
    .ct-horizontal-industries-clean .ct-horizontal-clean__panel:nth-child(3){background:radial-gradient(circle at 76% 36%,rgba(208,123,44,.15),transparent 30%)}
    .ct-horizontal-industries-clean .ct-industry-card{width:min(620px,46vw)!important;min-width:0!important;max-width:100%!important;margin:0 auto!important;opacity:1!important;transform:none!important;filter:none!important}
    .ct-horizontal-industries-clean .ct-industry-phone{height:min(680px,70vh)!important;min-height:0!important;transform:none!important}
    .ct-horizontal-industries-clean .ct-industry-screen{height:100%!important;min-height:0!important}

    .ct-journey-clean .ct-horizontal-clean__sticky{background:linear-gradient(135deg,#080c17,#10182b)}
    .ct-journey-clean .ct-horizontal-clean__panel:nth-child(2){background:linear-gradient(135deg,#07131a,#0d2730)}
    .ct-journey-clean .ct-horizontal-clean__panel:nth-child(3){background:linear-gradient(135deg,#0b0d12,#191424)}
    .ct-journey-clean .ct-horizontal-clean__panel:nth-child(4){background:linear-gradient(135deg,#09110f,#10281f)}
    .ct-j-scene-clean{width:min(760px,49vw);height:min(620px,66vh);min-height:500px;margin-left:auto;border:1px solid rgba(255,255,255,.14);border-radius:28px;background:#111722;overflow:hidden;box-shadow:0 30px 80px rgba(0,0,0,.28)}
    .ct-j-windowbar{height:48px;display:flex;align-items:center;gap:7px;padding:0 18px;border-bottom:1px solid rgba(255,255,255,.1);background:#171c27}.ct-j-windowbar i{width:8px;height:8px;border-radius:50%;background:#5d6574}.ct-j-windowbar span{width:44%;height:22px;margin-left:10px;border-radius:7px;background:#242b38}
    .ct-j-body{height:calc(100% - 48px);padding:30px;box-sizing:border-box}.ct-j-bigstatus{display:flex;align-items:flex-end;justify-content:space-between;gap:20px}.ct-j-bigstatus small{display:block;color:#7f8998;font-size:11px}.ct-j-bigstatus strong{display:block;margin-top:6px;font-size:34px;letter-spacing:-.06em}.ct-j-bigstatus em{padding:8px 11px;border-radius:999px;background:rgba(50,200,121,.12);color:#72dfa7;font-size:10px;font-style:normal;font-weight:900;white-space:nowrap}
    .ct-j-form{display:grid;gap:12px;margin-top:28px}.ct-j-form div{min-height:60px;display:flex;align-items:center;justify-content:space-between;gap:18px;padding:0 18px;border:1px solid rgba(255,255,255,.1);border-radius:14px;background:#181e29}.ct-j-form span{color:#808a99;font-size:11px}.ct-j-form b{font-size:13px;text-align:right}.ct-j-complete{margin-top:14px;padding:17px;border:1px solid rgba(50,200,121,.28);border-radius:14px;background:rgba(50,200,121,.09);color:#76e2aa;font-size:13px;font-weight:900}
    .ct-j-phone{position:absolute;right:36px;bottom:-34px;width:305px;padding:9px;border:1px solid rgba(255,255,255,.18);border-radius:38px;background:#020305;box-shadow:0 26px 70px rgba(0,0,0,.48)}.ct-j-phone-screen{min-height:500px;padding:32px 18px 22px;border-radius:29px;background:#10141c;box-sizing:border-box}.ct-j-phone-head{display:flex;justify-content:space-between;color:#7e9bff;font-size:15px;font-weight:900}.ct-j-alert{margin-top:25px;padding:17px;border:1px solid rgba(105,139,255,.24);border-radius:16px;background:#171d2a}.ct-j-alert small,.ct-j-alert b{display:block}.ct-j-alert small{color:#7d8797;font-size:9px}.ct-j-alert b{margin-top:7px;font-size:16px}.ct-j-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:13px}.ct-j-actions span{height:42px;display:grid;place-items:center;border-radius:11px;background:#222938;font-size:10px;font-weight:850}.ct-j-actions span.on{background:#3b6fff}.ct-j-timeline{display:grid;gap:10px;margin-top:24px}.ct-j-timeline div{display:grid;grid-template-columns:34px 1fr;gap:11px;align-items:center;padding:11px;border-radius:12px;background:#161b24}.ct-j-timeline i{width:34px;height:34px;display:grid;place-items:center;border-radius:10px;background:#252d3b;color:#8da5ff;font-style:normal;font-size:11px}.ct-j-timeline b,.ct-j-timeline small{display:block}.ct-j-timeline b{font-size:11px}.ct-j-timeline small{margin-top:4px;color:#717a88;font-size:8px}
    .ct-j-stats{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:28px}.ct-j-stats article{padding:18px;border:1px solid rgba(255,255,255,.1);border-radius:15px;background:#151b22}.ct-j-stats small,.ct-j-stats b{display:block}.ct-j-stats small{color:#7c8791;font-size:9px}.ct-j-stats b{margin-top:10px;font-size:25px}.ct-j-bars{display:flex;align-items:flex-end;gap:9px;height:170px;margin-top:30px;padding:22px;border-radius:18px;background:#11171b}.ct-j-bars i{flex:1;border-radius:7px 7px 2px 2px;background:linear-gradient(#65d89c,#1a6448)}.ct-j-bars i:nth-child(1){height:35%}.ct-j-bars i:nth-child(2){height:62%}.ct-j-bars i:nth-child(3){height:48%}.ct-j-bars i:nth-child(4){height:86%}.ct-j-bars i:nth-child(5){height:72%}.ct-j-bars i:nth-child(6){height:94%}

    @media(max-width:900px){
      .ct-horizontal-clean{height:auto!important}
      .ct-horizontal-clean__sticky{position:relative;top:auto;height:auto;min-height:0;padding-top:0;overflow:visible}
      .ct-horizontal-clean__top,.ct-horizontal-clean__progress{display:none}
      .ct-horizontal-clean__track{display:grid;transform:none!important;height:auto;will-change:auto}
      .ct-horizontal-clean__panel{width:100%;height:auto;min-height:0;display:grid;grid-template-columns:1fr;gap:28px;padding:70px 16px;border-bottom:1px solid rgba(255,255,255,.1)}
      .ct-horizontal-clean__panel:before{font-size:120px;top:32px;right:12px}
      .ct-horizontal-clean__copy h2,.ct-horizontal-clean__copy h3{font-size:clamp(38px,11vw,54px)}
      .ct-horizontal-clean__copy p{font-size:14px}
      .ct-horizontal-clean__visual{width:100%}
      .ct-horizontal-industries-clean .ct-industry-card{width:min(100%,390px)!important}
      .ct-horizontal-industries-clean .ct-industry-phone{height:auto!important;min-height:0!important}
      .ct-j-scene-clean{width:100%;height:auto;min-height:520px;margin:0;border-radius:22px}
      .ct-j-phone{right:16px;width:250px}
    }
  `;

  const addStyle=()=>{
    const node=document.createElement('style');
    node.dataset.ctHorizontalClean='1';
    node.textContent=css;
    document.head.append(node);
  };

  const industryCopy=[
    ['보험 상담','상담 신청부터 고객 등록까지','보험료 진단 결과와 연락처가 한 번에 접수됩니다. 상담자는 화면을 옮겨 다니지 않고 바로 전화와 후속관리를 시작합니다.'],
    ['병원 예약','날짜와 시간까지 그대로','고객이 선택한 진료 날짜와 예약 시간이 콜태그 고객카드로 들어옵니다. 예약 누락 없이 바로 확인하고 안내할 수 있습니다.'],
    ['부동산 문의','매물 문의가 바로 다음 할 일로','관심 매물과 고객 연락처가 자동으로 묶입니다. 전화, 문자, 방문 일정까지 하나의 고객카드에서 이어집니다.']
  ];

  const makeProgress=(count)=>`<div class="ct-horizontal-clean__progress" style="grid-template-columns:repeat(${count},1fr)">${Array.from({length:count},()=>'<i></i>').join('')}</div>`;

  const buildIndustries=()=>{
    const section=document.querySelector('#ct-pagero-intro .ct-v8-nocode');
    const cards=section?[...section.querySelectorAll('.ct-industry-showcase .ct-industry-card')]:[];
    if(!section||cards.length!==3)return null;

    section.className='ct-horizontal-clean ct-horizontal-industries-clean';
    section.style.height='300vh';
    section.innerHTML=`<div class="ct-horizontal-clean__sticky"><div class="ct-horizontal-clean__top"><strong>PAGERO LANDING COLLECTION</strong><span>SCROLL TO MOVE →</span></div><div class="ct-horizontal-clean__track"></div>${makeProgress(3)}</div>`;
    const track=section.querySelector('.ct-horizontal-clean__track');

    cards.forEach((card,index)=>{
      card.querySelectorAll('.ct-industry-auto').forEach(node=>node.remove());
      const panel=document.createElement('article');
      panel.className='ct-horizontal-clean__panel';
      panel.dataset.step=String(index+1).padStart(2,'0');
      panel.innerHTML=`<div class="ct-horizontal-clean__copy"><small>0${index+1} / 03 · ${industryCopy[index][0]}</small><h3>${industryCopy[index][1]}</h3><p>${industryCopy[index][2]}</p></div><div class="ct-horizontal-clean__visual"></div>`;
      panel.querySelector('.ct-horizontal-clean__visual').append(card);
      track.append(panel);
    });
    return section;
  };

  const journeyMarkup=`<section class="ct-horizontal-clean ct-journey-clean"><div class="ct-horizontal-clean__sticky"><div class="ct-horizontal-clean__top"><strong>CALLTAG WORKFLOW</strong><span>SCROLL TO MOVE →</span></div><div class="ct-horizontal-clean__track">
    <article class="ct-horizontal-clean__panel" data-step="01"><div class="ct-horizontal-clean__copy"><small>01 / 문의 접수</small><h2>문의가<br>들어옵니다.</h2><p>페이지로 랜딩페이지에서 이름, 연락처, 상담 내용이 들어오는 순간 고객 데이터가 만들어집니다.</p></div><div class="ct-horizontal-clean__visual"><div class="ct-j-scene-clean"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>새 상담 신청</small><strong>김민수 고객</strong></div><em>접수 완료</em></div><div class="ct-j-form"><div><span>연락처</span><b>010-1234-5678</b></div><div><span>문의 내용</span><b>보험 상담을 요청합니다</b></div><div><span>유입 페이지</span><b>보험료 진단 랜딩</b></div></div><div class="ct-j-complete">✓ 고객 정보가 콜태그로 전달됐습니다.</div></div></div></div></article>
    <article class="ct-horizontal-clean__panel" data-step="02"><div class="ct-horizontal-clean__copy"><small>02 / 즉시 알림</small><h2>앱에서<br>바로 뜹니다.</h2><p>새 문의 알림과 고객카드가 동시에 생성됩니다. 연락처를 다시 입력하거나 메모를 옮길 필요가 없습니다.</p></div><div class="ct-horizontal-clean__visual"><div class="ct-j-scene-clean" style="position:relative"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>콜태그 고객함</small><strong>신규 문의 1</strong></div><em>지금</em></div><div class="ct-j-phone"><div class="ct-j-phone-screen"><div class="ct-j-phone-head"><b>CALLTAG</b><span>● 1</span></div><div class="ct-j-alert"><small>페이지로 신규 문의</small><b>김민수 고객</b><small>보험 상담 요청 · 지금</small></div><div class="ct-j-actions"><span>전화</span><span>문자</span><span class="on">고객카드</span></div><div class="ct-j-timeline"><div><i>✓</i><span><b>자동 등록 완료</b><small>연락처·문의내용 저장</small></span></div><div><i>!</i><span><b>오늘 할 일 추가</b><small>신규 문의 확인</small></span></div></div></div></div></div></div></div></article>
    <article class="ct-horizontal-clean__panel" data-step="03"><div class="ct-horizontal-clean__copy"><small>03 / 통화 후 정리</small><h2>전화가 끝나면<br>정리도 끝.</h2><p>통화 종료 팝업에서 고객 구분, 상담 상태, 다음 할 일과 재연락 날짜를 한 번에 남깁니다.</p></div><div class="ct-horizontal-clean__visual"><div class="ct-j-scene-clean"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>통화 종료</small><strong>김민수 고객</strong></div><em>02:18</em></div><div class="ct-j-form"><div><span>고객 구분</span><b>신규 문의</b></div><div><span>상담 상태</span><b>견적·자료 전달</b></div><div><span>다음 할 일</span><b>3일 뒤 다시 연락</b></div><div><span>메모</span><b>보장 분석표 문자 발송</b></div></div><div class="ct-j-actions"><span>문자 보내기</span><span>일정 추가</span><span class="on">저장 완료</span></div></div></div></div></article>
    <article class="ct-horizontal-clean__panel" data-step="04"><div class="ct-horizontal-clean__copy"><small>04 / 후속관리</small><h2>다시 연락할<br>고객만 보입니다.</h2><p>오늘 할 일, 재연락 일정, 상담 단계와 유입 통계가 한 화면에 쌓입니다. 고객을 놓치지 않는 업무 루프가 완성됩니다.</p></div><div class="ct-horizontal-clean__visual"><div class="ct-j-scene-clean"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>오늘의 고객관리</small><strong>재연락 8건</strong></div><em>진행 중</em></div><div class="ct-j-stats"><article><small>신규 문의</small><b>24</b></article><article><small>통화 완료</small><b>17</b></article><article><small>계약 예정</small><b>6</b></article></div><div class="ct-j-bars"><i></i><i></i><i></i><i></i><i></i><i></i></div><div class="ct-j-complete">✓ 페이지로 유입부터 후속관리까지 연결됐습니다.</div></div></div></div></article>
    </div>${makeProgress(4)}</div></section>`;

  const buildJourney=(industry)=>{
    let journey=document.querySelector('.ct-journey-clean');
    if(journey)return journey;
    industry.insertAdjacentHTML('afterend',journeyMarkup);
    journey=industry.nextElementSibling;
    journey.style.height='400vh';
    return journey;
  };

  const setup=(sections)=>{
    let metrics=[];
    let raf=0;
    const measure=()=>{
      metrics=sections.map(section=>({
        section,
        track:section.querySelector('.ct-horizontal-clean__track'),
        panels:[...section.querySelectorAll('.ct-horizontal-clean__panel')],
        bars:[...section.querySelectorAll('.ct-horizontal-clean__progress i')],
        top:section.getBoundingClientRect().top+scrollY,
        range:Math.max(1,section.offsetHeight-innerHeight)
      }));
      render();
    };
    const render=()=>{
      raf=0;
      metrics.forEach(item=>{
        if(!mq.matches){
          item.track.style.transform='none';
          item.bars.forEach(bar=>bar.classList.add('on'));
          return;
        }
        const progress=clamp((scrollY-item.top)/item.range);
        const max=(item.panels.length-1)*innerWidth;
        item.track.style.transform=`translate3d(${-progress*max}px,0,0)`;
        const index=Math.min(item.panels.length-1,Math.round(progress*(item.panels.length-1)));
        item.bars.forEach((bar,i)=>bar.classList.toggle('on',i<=index));
      });
    };
    const request=()=>{if(!raf)raf=requestAnimationFrame(render)};
    addEventListener('scroll',request,{passive:true});
    addEventListener('resize',()=>{requestAnimationFrame(measure)},{passive:true});
    addEventListener('load',measure,{once:true});
    measure();
    setTimeout(measure,600);
    setTimeout(measure,1600);
  };

  const init=()=>{
    const industry=buildIndustries();
    if(!industry)return false;
    const journey=buildJourney(industry);
    setup([industry,journey]);
    return true;
  };

  addStyle();
  const boot=()=>{
    if(init())return;
    const observer=new MutationObserver(()=>{
      if(init())observer.disconnect();
    });
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),12000);
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(boot),{once:true});
  else requestAnimationFrame(boot);
})();
