(()=>{
  if(document.documentElement.dataset.ctSuitePricing)return;
  document.documentElement.dataset.ctSuitePricing='1';

  const q=(s,r=document)=>r.querySelector(s);
  const qa=(s,r=document)=>[...r.querySelectorAll(s)];
  const byText=(needles)=>qa('section').find(section=>needles.some(text=>section.textContent.includes(text)));

  const setFeature=(section,title,copy,callout)=>{
    if(!section)return;
    const heading=q('.feature-copy h3,.section-title,h2,h3',section);
    const description=q('.feature-copy>p,.section-copy',section);
    if(heading)heading.innerHTML=title;
    if(description)description.textContent=copy;
    let points=q('.feature-points',section);
    if(points){
      points.innerHTML=`<div class="ct-single-callout">${callout}</div>`;
    }else{
      const copyBox=q('.feature-copy',section);
      if(copyBox&&!q('.ct-single-callout',copyBox))copyBox.insertAdjacentHTML('beforeend',`<div class="ct-single-callout">${callout}</div>`);
    }
  };

  const run=()=>{
    setFeature(q('#tasks')||byText(['놓친 일부터','기한이 지난 재연락']),
      '놓친 일부터<br><span>먼저 보입니다.</span>',
      '기한이 지난 재연락, 오늘 연락할 고객, 보내지 않은 자료를 우선순위대로 보여줍니다.',
      '오늘 할 일을 바로 확인하세요');

    setFeature(q('#history')||byText(['고객마다','상담 이력']),
      '고객마다<br><span>기억할 필요 없습니다.</span>',
      '누구와 언제 통화했고, 상담이 어디까지 진행됐고, 다음에 무엇을 해야 하는지 고객별로 확인합니다.',
      '한눈에 보이는 상담이력');

    setFeature(q('#calendar')||byText(['일정은','달력에 모입니다']),
      '일정은<br><span>달력에 모입니다.</span>',
      '재연락, 자료 발송, 방문 약속을 날짜별로 확인하고 고객 상담 이력까지 바로 확인합니다.',
      '모든 일정 정리는 콜태그에서!');

    const strengths=q('#strengths');
    if(strengths){
      strengths.className='ad-section alt ct-suite-strengths';
      strengths.innerHTML=`
        <div class="wrap">
          <div class="ad-head">
            <p class="ad-kicker">콜태그의 강점</p>
            <h2 class="ad-title">고객을 받고,<br>놓치지 않고, 다시 연락합니다.</h2>
          </div>
          <div class="ct-strength-grid">
            <article><b>01</b><h3>통화 직후 태그</h3><p>통화 종료 화면에서 고객 구분, 상담 상태, 재연락 날짜를 바로 남깁니다.</p></article>
            <article><b>02</b><h3>고객별 상담기록</h3><p>통화, 문자, 해야 할 일과 일정을 고객 한 명 기준으로 확인합니다.</p></article>
            <article><b>03</b><h3>자동문자</h3><p>저장한 문구를 통화 직후 보내고 발송 제외와 중복 발송을 확인합니다.</p></article>
            <article class="future"><span>연동 출시 예정</span><b>04</b><h3>페이지로 고객접수 연동</h3><p>페이지로 랜딩에서 접수된 고객을 콜태그에 등록해 전화·문자 후속관리까지 연결할 예정입니다.</p></article>
          </div>
          <div class="ct-suite-flow">
            <div><small>01</small><strong>페이지로 고객접수</strong></div><i>→</i>
            <div><small>02</small><strong>콜태그 고객등록</strong></div><i>→</i>
            <div><small>03</small><strong>전화·문자 후속관리</strong></div>
            <em>연동 출시 예정</em>
          </div>
        </div>`;
    }

    const pricing=q('#pricing');
    if(pricing){
      pricing.className='ad-section ct-suite-pricing';
      pricing.innerHTML=`
        <div class="ad-pricing">
          <div class="ad-head">
            <p class="ad-kicker">4가지 요금제</p>
            <h2 class="ad-title">필요한 기능만 선택하세요.</h2>
            <p class="ad-copy">2026년 가입자는 해지 전까지 가입 가격을 유지합니다.</p>
          </div>
          <div class="ad-promo"><b>2026년 선착순 가입자 한정 · 평생 가격</b><span>2026년 12월 31일 또는 준비 인원 마감 시 종료</span></div>
          <div class="ct-price-grid">
            <article class="ct-price-card">
              <span class="plan">CALL</span><h3>전화관리</h3><p>통화 직후 고객 상태와 재연락 일정을 남깁니다.</p>
              <del>정가 월 6,900원</del><div class="price"><strong>1,900원</strong><span>/ 월</span></div>
              <ul><li>통화 종료 후 태그</li><li>상담 상태·메모</li><li>오늘 할 일·재연락</li><li>고객별 상담기록</li></ul>
            </article>
            <article class="ct-price-card">
              <span class="plan">MESSAGE</span><h3>문자자동화</h3><p>통화 상황에 맞는 문자를 즉시 또는 예약 발송합니다.</p>
              <del>정가 월 4,900원</del><div class="price"><strong>990원</strong><span>/ 월</span></div>
              <ul><li>수신·발신·부재중</li><li>예약·후속문자</li><li>문자 템플릿</li><li>발송 제외·중복방지</li></ul>
            </article>
            <article class="ct-price-card pagero">
              <span class="plan">PAGERO</span><h3>페이지로</h3><p>광고용 랜딩페이지를 만들고 고객 접수를 받습니다.</p>
              <del>정가 월 9,900원</del><div class="price"><strong>3,500원</strong><span>/ 월</span></div>
              <ul><li>랜딩페이지 제작</li><li>고객 접수 폼</li><li>모바일 최적화</li><li>접수 고객 관리</li></ul>
            </article>
            <article class="ct-price-card all">
              <span class="badge">연동 출시 예정</span><span class="plan">ALL IN ONE</span><h3>통합권</h3><p>페이지로 접수부터 콜태그 전화·문자 후속관리까지 사용합니다.</p>
              <del>정가 월 19,900원</del><div class="price"><strong>5,500원</strong><span>/ 월 예정</span></div>
              <ul><li>전화관리 전체 기능</li><li>문자자동화 전체 기능</li><li>페이지로 전체 기능</li><li>접수 고객 자동등록 예정</li></ul>
            </article>
          </div>
          <p class="ct-price-note">통합권의 연동 기능과 최종 가격은 페이지로·콜태그 연동 출시 전에 확정됩니다. 현재 페이지로 고객의 콜태그 자동등록은 제공되지 않습니다.</p>
        </div>`;
    }

    if(!q('style[data-ct-suite-pricing]')){
      const style=document.createElement('style');
      style.dataset.ctSuitePricing='1';
      style.textContent=`
        .ct-single-callout{display:inline-flex;align-items:center;margin-top:28px;padding:14px 18px;border:1px solid rgba(59,111,255,.35);border-radius:12px;background:var(--blue-soft);color:#e4e8ff;font-size:15px;font-weight:900}
        .ct-single-callout:before{content:'→';margin-right:10px;color:var(--blue-2);font-size:18px}
        .ct-suite-strengths{background:#0d0f13!important}.ct-strength-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}
        .ct-strength-grid article{position:relative;min-height:290px;padding:32px;border:1px solid var(--line);border-radius:22px;background:#11141a}
        .ct-strength-grid article>b{color:var(--blue-2);font-size:36px}.ct-strength-grid h3{margin:28px 0 0;font-size:26px;letter-spacing:-.055em}.ct-strength-grid p{margin:14px 0 0;color:var(--muted);font-size:13px;line-height:1.7}
        .ct-strength-grid article.future{border-color:rgba(59,111,255,.5);background:linear-gradient(155deg,rgba(59,111,255,.15),#11141a 48%)}.ct-strength-grid article.future>span{position:absolute;top:20px;right:20px;padding:6px 9px;border-radius:999px;background:var(--blue);font-size:9px;font-weight:900}
        .ct-suite-flow{display:grid;grid-template-columns:1fr 52px 1fr 52px 1fr;align-items:center;gap:10px;margin-top:24px;padding:24px;border:1px solid rgba(59,111,255,.35);border-radius:20px;background:rgba(59,111,255,.07);position:relative}
        .ct-suite-flow div{padding:18px;border-radius:14px;background:#11141a;text-align:center}.ct-suite-flow small{display:block;color:var(--blue-2);font-weight:900}.ct-suite-flow strong{display:block;margin-top:8px;font-size:16px}.ct-suite-flow i{color:var(--blue-2);font-size:27px;font-style:normal;text-align:center}.ct-suite-flow em{position:absolute;right:18px;top:14px;color:#c6d0ff;font-size:9px;font-style:normal;font-weight:900}
        .ct-price-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:13px}.ct-price-card{position:relative;min-height:610px;padding:28px;border:1px solid var(--line);border-radius:23px;background:#11141a;display:flex;flex-direction:column}
        .ct-price-card.pagero{border-color:rgba(124,153,255,.38)}.ct-price-card.all{border-color:rgba(59,111,255,.68);background:linear-gradient(160deg,rgba(59,111,255,.17),#11141a 38%);box-shadow:0 25px 65px rgba(59,111,255,.13)}
        .ct-price-card .badge{position:absolute;top:18px;right:18px;padding:6px 9px;border-radius:999px;background:var(--blue);font-size:8px;font-weight:900}.ct-price-card .plan{color:var(--blue-2);font-size:10px;font-weight:900}.ct-price-card h3{margin:16px 0 0;font-size:29px}.ct-price-card>p{min-height:62px;margin:12px 0 0;color:var(--muted);font-size:12px;line-height:1.65}.ct-price-card del{margin-top:26px;color:var(--muted-2);font-size:11px}.ct-price-card .price{display:flex;align-items:flex-end;gap:6px;margin-top:8px}.ct-price-card .price strong{font-size:43px;line-height:1;letter-spacing:-.07em}.ct-price-card .price span{margin-bottom:5px;color:var(--muted);font-size:10px}.ct-price-card ul{display:grid;gap:11px;margin:28px 0 0;padding:0;list-style:none}.ct-price-card li{display:flex;gap:8px;color:#d9dce3;font-size:11px;line-height:1.45}.ct-price-card li:before{content:'✓';color:var(--blue-2);font-weight:900}.ct-price-note{margin:18px auto 0;max-width:1050px;color:var(--muted-2);font-size:10px;line-height:1.7;text-align:center}
        @media(max-width:1150px){.ct-strength-grid,.ct-price-grid{grid-template-columns:repeat(2,1fr)}}
        @media(max-width:760px){.ct-strength-grid,.ct-price-grid{grid-template-columns:1fr}.ct-suite-flow{grid-template-columns:1fr}.ct-suite-flow i{transform:rotate(90deg)}.ct-price-card{min-height:auto}.ct-single-callout{font-size:13px}}
      `;
      document.head.append(style);
    }
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});
  else requestAnimationFrame(run);
})();